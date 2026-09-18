package com.mohithash.repcoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohithash.repcoach.App
import com.mohithash.repcoach.ai.AiSettings
import com.mohithash.repcoach.data.Session
import com.mohithash.repcoach.data.SetLog
import com.mohithash.repcoach.domain.Debrief
import com.mohithash.repcoach.domain.Plan
import com.mohithash.repcoach.domain.PlanDay
import com.mohithash.repcoach.domain.Profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate

sealed interface Job<out T> {
    data object Idle : Job<Nothing>
    data object Loading : Job<Nothing>
    data class Done<T>(val value: T) : Job<T>
    data class Failed(val message: String) : Job<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val app: App) : ViewModel() {
    private val db = app.db
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val client get() = app.client

    val ai: StateFlow<AiSettings> = app.store.flow("ai", AiSettings.serializer(), AiSettings())
    val profile: StateFlow<Profile> = app.store.flow("profile", Profile.serializer(), Profile())
    val plan: StateFlow<Plan> = app.store.flow("plan", Plan.serializer(), Plan())
    fun saveAi(a: AiSettings) = app.store.set("ai", AiSettings.serializer(), a)
    fun saveProfile(p: Profile) = app.store.set("profile", Profile.serializer(), p.copy(onboarded = true))

    val sessions = db.sessions().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSets = db.sets().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bests = db.sets().bests().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The session being logged right now (or reviewed). */
    val active = MutableStateFlow<Session?>(null)
    val activeDay = MutableStateFlow<PlanDay?>(null)
    val activeSets: StateFlow<List<SetLog>> = active.flatMapLatest { s -> if (s == null) flowOf(emptyList()) else db.sets().forSession(s.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    /** Previous sets per exercise for the active day, to show "last time". */
    val previous = MutableStateFlow<Map<String, List<SetLog>>>(emptyMap())

    private val _planJob = MutableStateFlow<Job<Plan>>(Job.Idle)
    val planJob: StateFlow<Job<Plan>> = _planJob
    private val _debrief = MutableStateFlow<Job<Debrief>>(Job.Idle)
    val debrief: StateFlow<Job<Debrief>> = _debrief
    private val _howTo = MutableStateFlow<Job<String>>(Job.Idle)
    val howTo: StateFlow<Job<String>> = _howTo

    fun generatePlan() {
        _planJob.value = Job.Loading
        viewModelScope.launch { _planJob.value = runCatching { app.coach.plan(ai.value, profile.value) }.fold({ app.store.set("plan", Plan.serializer(), it); Job.Done(it) }, { Job.Failed(it.message ?: "Failed") }) }
    }

    fun startWorkout(day: PlanDay, onReady: () -> Unit) = viewModelScope.launch {
        val id = db.sessions().insert(Session(date = LocalDate.now().toString(), dayName = day.name))
        val s = Session(id = id, date = LocalDate.now().toString(), dayName = day.name)
        active.value = s; activeDay.value = day; _debrief.value = Job.Idle
        previous.value = day.exercises.associate { it.name to db.sets().lastFor(it.name, id) }
        onReady()
    }
    fun logSet(exercise: String, weight: Double, reps: Int) = viewModelScope.launch {
        val s = active.value ?: return@launch
        val idx = activeSets.value.count { it.exercise == exercise } + 1
        db.sets().insert(SetLog(sessionId = s.id, exercise = exercise, setIndex = idx, weight = weight, reps = reps))
    }
    fun deleteSet(id: Long) = viewModelScope.launch { db.sets().delete(id) }

    fun finishWorkout(notes: String) = viewModelScope.launch {
        val s = active.value ?: return@launch
        val done = s.copy(endedAt = System.currentTimeMillis(), notes = notes)
        db.sessions().update(done); active.value = done
        if (ai.value.configured) {
            _debrief.value = Job.Loading
            _debrief.value = runCatching { app.coach.debrief(ai.value, profile.value, s.dayName, db.sets().listForSession(s.id), previous.value) }.fold({ d ->
                db.sessions().update(done.copy(debrief = json.encodeToString(Debrief.serializer(), d))); Job.Done(d)
            }, { Job.Failed(it.message ?: "Failed") })
        }
    }
    fun openSession(s: Session) { active.value = s; activeDay.value = plan.value.days.firstOrNull { it.name == s.dayName }; _debrief.value = decodeDebrief(s)?.let { Job.Done(it) } ?: Job.Idle }
    fun decodeDebrief(s: Session): Debrief? = s.debrief.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(Debrief.serializer(), it) }.getOrNull() }
    fun deleteSession(s: Session) = viewModelScope.launch { db.sets().deleteSession(s.id); db.sessions().delete(s.id); if (active.value?.id == s.id) active.value = null }
    fun discardActive() = viewModelScope.launch { active.value?.let { if (it.endedAt == 0L) deleteSession(it) } }

    fun askHowTo(exercise: String) { _howTo.value = Job.Loading; viewModelScope.launch { _howTo.value = runCatching { app.coach.howTo(ai.value, exercise) }.fold({ Job.Done(it) }, { Job.Failed(it.message ?: "Failed") }) } }
    fun clearHowTo() { _howTo.value = Job.Idle }
}
