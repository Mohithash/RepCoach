@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.repcoach.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mohithash.repcoach.data.SetLog
import com.mohithash.repcoach.domain.PlannedExercise
import com.mohithash.repcoach.ui.AppViewModel
import com.mohithash.repcoach.ui.HeroCard
import com.mohithash.repcoach.ui.Job
import com.mohithash.repcoach.ui.Label
import com.mohithash.repcoach.ui.ShapeIcon
import com.mohithash.repcoach.ui.StatCard
import com.mohithash.repcoach.ui.theme.Brand

@Composable
fun WorkoutScreen(vm: AppViewModel, onBack: () -> Unit) {
    val session by vm.active.collectAsState()
    val s = session ?: run { onBack(); return }
    val day by vm.activeDay.collectAsState()
    val sets by vm.activeSets.collectAsState()
    val prev by vm.previous.collectAsState()
    val debrief by vm.debrief.collectAsState()
    val howTo by vm.howTo.collectAsState()
    val cs = MaterialTheme.colorScheme
    val finished = s.endedAt > 0
    var notes by remember { mutableStateOf(s.notes) }
    var howToFor by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val exercises = day?.exercises ?: sets.map { it.exercise }.distinct().map { PlannedExercise(it) }
    val planned = exercises.sumOf { it.sets }
    val volume = sets.sumOf { it.weight * it.reps }

    Scaffold(topBar = {
        TopAppBar(title = { Column { Text(s.dayName); Text(if (finished) s.date else "${sets.size}/$planned sets · ${volume.toInt()} kg volume", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface),
            navigationIcon = { IconButton({ if (finished) onBack() else confirmDiscard = true }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!finished) item { LinearWavyProgressIndicator(progress = { if (planned == 0) 0f else (sets.size.toFloat() / planned).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
            items(exercises, key = { it.name }) { ex ->
                ExerciseCard(ex, sets.filter { it.exercise == ex.name }, prev[ex.name].orEmpty(), finished,
                    onLog = { w, r -> vm.logSet(ex.name, w, r) }, onDelete = { vm.deleteSet(it) }, onHowTo = { howToFor = ex.name; vm.askHowTo(ex.name) })
            }
            item {
                if (!finished) StatCard {
                    OutlinedTextField(notes, { notes = it }, label = { Text("Session notes") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large)
                    Button({ vm.finishWorkout(notes) }, enabled = sets.isNotEmpty(), shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Finish & get coach review") }
                } else when (val d = debrief) {
                    Job.Loading -> Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { LoadingIndicator(); Spacer(Modifier.size(12.dp)); Text("Coach is reviewing…", color = cs.onSurfaceVariant) }
                    is Job.Failed -> StatCard(container = cs.errorContainer) { Text(d.message, color = cs.onErrorContainer) }
                    is Job.Done -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Sunny) { Label("Coach review", cs.onPrimary.copy(alpha = 0.8f)); Text(d.value.summary, style = MaterialTheme.typography.titleLarge, color = cs.onPrimary) }
                        if (d.value.highlights.isNotEmpty()) StatCard(container = cs.secondaryContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { ShapeIcon(Icons.Default.EmojiEvents, cs.secondary, cs.onSecondary, MaterialShapes.Sunny); Text("Highlights", style = MaterialTheme.typography.titleMedium, color = cs.onSecondaryContainer) }
                            d.value.highlights.forEach { Text("•  $it", color = cs.onSecondaryContainer) }
                        }
                        d.value.advice.forEach { a -> StatCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { ShapeIcon(Icons.Default.TrendingUp, cs.primaryContainer, cs.onPrimaryContainer, MaterialShapes.Arrow); Column { Text(a.change, style = MaterialTheme.typography.titleMedium); Text(a.why, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) } }
                            a.next_targets.forEach { Text("→ $it", style = MaterialTheme.typography.bodyLarge, color = cs.primary, modifier = Modifier.padding(start = 52.dp)) }
                        } }
                        if (d.value.recovery_tip.isNotBlank()) StatCard(container = cs.tertiaryContainer) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { ShapeIcon(Icons.Default.Bedtime, cs.tertiary, cs.onTertiary, MaterialShapes.Pill); Text(d.value.recovery_tip, color = cs.onTertiaryContainer) } }
                    }
                    Job.Idle -> if (s.notes.isNotBlank()) StatCard { Label("Notes"); Text(s.notes) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    howToFor?.let { name ->
        AlertDialog(onDismissRequest = { howToFor = null; vm.clearHowTo() }, title = { Text(name) },
            text = { when (val h = howTo) { Job.Loading -> Row(verticalAlignment = Alignment.CenterVertically) { LoadingIndicator(); Spacer(Modifier.size(10.dp)); Text("Asking coach…") }; is Job.Done -> Text(h.value); is Job.Failed -> Text(h.message, color = cs.error); Job.Idle -> Text("") } },
            confirmButton = { TextButton({ howToFor = null; vm.clearHowTo() }) { Text("Got it") } })
    }
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Leave workout?") }, text = { Text("Unfinished sessions are discarded.") },
        confirmButton = { TextButton({ confirmDiscard = false; vm.discardActive(); onBack() }) { Text("Discard") } }, dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep going") } })
}

@Composable
private fun ExerciseCard(ex: PlannedExercise, done: List<SetLog>, prev: List<SetLog>, finished: Boolean, onLog: (Double, Int) -> Unit, onDelete: (Long) -> Unit, onHowTo: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val lastW = done.lastOrNull()?.weight ?: prev.firstOrNull()?.weight
    var w by remember(ex.name, done.size) { mutableStateOf(lastW?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var r by remember(ex.name, done.size) { mutableStateOf((done.lastOrNull()?.reps ?: prev.firstOrNull()?.reps)?.toString() ?: "") }
    StatCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                Text("${ex.sets} × ${ex.reps}${if (ex.rest_s > 0) " · rest ${ex.rest_s}s" else ""}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                if (ex.cue.isNotBlank()) Text("💡 ${ex.cue}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            IconButton(onHowTo) { Icon(Icons.Default.Info, "How to", tint = cs.primary) }
        }
        if (prev.isNotEmpty()) Text("Last time: " + prev.sortedBy { it.setIndex }.joinToString("  ") { "${it.weight.fmt()}×${it.reps}" }, style = MaterialTheme.typography.labelMedium, color = cs.tertiary)
        done.forEach { sl ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Set ${sl.setIndex}", Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Text("${sl.weight.fmt()} kg × ${sl.reps}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                if (!finished) IconButton({ onDelete(sl.id) }) { Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant) }
            }
        }
        if (!finished) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(w, { w = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("kg") }, singleLine = true, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(r, { r = it.filter(Char::isDigit) }, label = { Text("reps") }, singleLine = true, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            FilledTonalButton({ onLog(w.toDoubleOrNull() ?: 0.0, r.toInt()) }, enabled = (r.toIntOrNull() ?: 0) > 0, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(56.dp)) { Icon(Icons.Default.Add, null); Text("Set ${done.size + 1}") }
        }
    }
}

private fun Double.fmt() = if (this % 1.0 == 0.0) toInt().toString() else toString()
