@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.repcoach.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.mohithash.repcoach.ui.AppViewModel
import com.mohithash.repcoach.ui.EmptyState
import com.mohithash.repcoach.ui.HeroCard
import com.mohithash.repcoach.ui.Job
import com.mohithash.repcoach.ui.Label
import com.mohithash.repcoach.ui.ShapeIcon
import com.mohithash.repcoach.ui.StatCard
import com.mohithash.repcoach.ui.theme.Brand

@Composable
fun PlanScreen(vm: AppViewModel, onWorkout: () -> Unit, onSettings: () -> Unit) {
    val plan by vm.plan.collectAsState()
    val job by vm.planJob.collectAsState()
    val ai by vm.ai.collectAsState()
    val p by vm.profile.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val cs = MaterialTheme.colorScheme
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lastDay = sessions.firstOrNull { it.endedAt > 0 }?.dayName
    val nextIdx = plan.days.indexOfFirst { it.name == lastDay }.let { if (it < 0) 0 else (it + 1) % maxOf(1, plan.days.size) }
    Scaffold(modifier = Modifier.nestedScroll(scroll.nestedScrollConnection), topBar = {
        MediumFlexibleTopAppBar(title = { Text(plan.title.ifBlank { "Your programme" }) }, subtitle = { Text("${p.goal} · ${p.daysPerWeek}×/week · ${p.equipment}") },
            actions = { IconButton(onSettings) { Icon(Icons.Default.Settings, null) } }, scrollBehavior = scroll, colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface, scrolledContainerColor = cs.surface))
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (plan.days.isEmpty()) HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Cookie9Sided) {
                    val on = cs.onPrimary
                    Label("Step 1", on.copy(alpha = 0.8f)); Text("Generate your week", style = MaterialTheme.typography.headlineMedium, color = on)
                    Text("Built around your goal, equipment, schedule and any limitations. Regenerate any time.", color = on.copy(alpha = 0.9f))
                    if (!ai.configured) Text("Add your API key in Settings first.", color = cs.secondaryContainer, style = MaterialTheme.typography.labelLarge)
                    Button({ vm.generatePlan() }, enabled = ai.configured && job != Job.Loading, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.buttonColors(containerColor = cs.secondary, contentColor = cs.onSecondary), modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp)) {
                        if (job == Job.Loading) { LoadingIndicator(Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Programming…") } else { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.size(8.dp)); Text("Build plan") }
                    }
                } else HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Cookie9Sided) {
                    val on = cs.onPrimary
                    Label("Up next", on.copy(alpha = 0.8f))
                    Text(plan.days[nextIdx].name, style = MaterialTheme.typography.headlineMedium, color = on)
                    Text(plan.days[nextIdx].focus, color = on.copy(alpha = 0.9f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Button({ vm.startWorkout(plan.days[nextIdx], onWorkout) }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.buttonColors(containerColor = cs.secondary, contentColor = cs.onSecondary), modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.Default.PlayArrow, null); Text("Start") }
                        FilledTonalButton({ vm.generatePlan() }, enabled = ai.configured && job != Job.Loading, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(48.dp)) { if (job == Job.Loading) LoadingIndicator(Modifier.size(18.dp)) else Text("Regenerate") }
                    }
                }
                (job as? Job.Failed)?.let { Text(it.message, color = cs.error, modifier = Modifier.padding(top = 6.dp)) }
            }
            if (plan.rationale.isNotBlank()) item { Text(plan.rationale, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) }
            if (plan.days.isEmpty() && job != Job.Loading) item { EmptyState(Icons.Default.FitnessCenter, "No plan yet", "Your week appears here once generated.") }
            itemsIndexed(plan.days) { i, d ->
                StatCard(Modifier.clip(MaterialTheme.shapes.extraLarge).clickable { vm.startWorkout(d, onWorkout) }, container = if (i == nextIdx) cs.secondaryContainer else cs.surfaceContainerLow) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ShapeIcon(Icons.Default.FitnessCenter, cs.primaryContainer, cs.onPrimaryContainer, MaterialShapes.Sunny)
                        Column(Modifier.weight(1f)) { Text(d.name, style = MaterialTheme.typography.titleMedium); Text(d.focus, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
                        Text("${d.exercises.size} ex", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    }
                    d.exercises.forEach { e -> Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(e.name, style = MaterialTheme.typography.bodyMedium); Text("${e.sets}×${e.reps}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) } }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
