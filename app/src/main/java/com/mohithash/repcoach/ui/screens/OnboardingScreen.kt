@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.mohithash.repcoach.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.mohithash.repcoach.domain.Profile
import com.mohithash.repcoach.ui.AppViewModel
import com.mohithash.repcoach.ui.Label
import com.mohithash.repcoach.ui.StatCard

@Composable
fun ProfileForm(initial: Profile, onChange: (Profile) -> Unit) {
    var goal by remember { mutableStateOf(initial.goal) }
    var exp by remember { mutableStateOf(initial.experience) }
    var days by remember { mutableStateOf(initial.daysPerWeek.toFloat()) }
    var equip by remember { mutableStateOf(initial.equipment) }
    var mins by remember { mutableStateOf(initial.minutes.toFloat()) }
    var lim by remember { mutableStateOf(initial.limitations) }
    fun emit() = onChange(initial.copy(goal = goal, experience = exp, daysPerWeek = days.toInt(), equipment = equip, minutes = mins.toInt(), limitations = lim))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label("Goal"); FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("strength", "hypertrophy", "fat loss", "general").forEach { g -> FilterChip(selected = goal == g, onClick = { goal = g; emit() }, label = { Text(g) }) } }
        Label("Experience"); FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("beginner", "intermediate", "advanced").forEach { g -> FilterChip(selected = exp == g, onClick = { exp = g; emit() }, label = { Text(g) }) } }
        Label("Equipment"); FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("full gym", "dumbbells", "bodyweight").forEach { g -> FilterChip(selected = equip == g, onClick = { equip = g; emit() }, label = { Text(g) }) } }
        Label("Days per week: ${days.toInt()}"); Slider(days, { days = it; emit() }, valueRange = 2f..6f, steps = 3)
        Label("Session length: ${mins.toInt()} min"); Slider(mins, { mins = it; emit() }, valueRange = 30f..90f, steps = 3)
        OutlinedTextField(lim, { lim = it; emit() }, label = { Text("Injuries / limitations (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large)
    }
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var draft by remember { mutableStateOf(Profile()) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LargeFlexibleTopAppBar(title = { Text("Train with a plan") }, subtitle = { Text("A programme built for you, set‑by‑set logging, and a coach that tells you what to lift next time.") }, scrollBehavior = scroll) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard { ProfileForm(Profile()) { draft = it } }
            Button({ vm.saveProfile(draft) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Build my plan", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
