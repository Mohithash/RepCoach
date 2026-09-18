@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.repcoach.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mohithash.repcoach.ui.AppViewModel
import com.mohithash.repcoach.ui.BarChart
import com.mohithash.repcoach.ui.EmptyState
import com.mohithash.repcoach.ui.Label
import com.mohithash.repcoach.ui.ShapeIcon
import com.mohithash.repcoach.ui.StatCard
import com.mohithash.repcoach.ui.prettyDate
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun HistoryScreen(vm: AppViewModel, onOpen: () -> Unit) {
    val sessions by vm.sessions.collectAsState()
    val sets by vm.allSets.collectAsState()
    val cs = MaterialTheme.colorScheme
    val done = sessions.filter { it.endedAt > 0 }
    val volumeBySession = sets.groupBy { it.sessionId }.mapValues { (_, l) -> l.sumOf { it.weight * it.reps } }
    val wf = WeekFields.of(Locale.getDefault())
    val weeks = (0 until 6).map { LocalDate.now().minusWeeks(5L - it) }.map { d ->
        val wk = d.get(wf.weekOfWeekBasedYear()); val yr = d.get(wf.weekBasedYear())
        "W$wk" to done.filter { s -> LocalDate.parse(s.date).let { it.get(wf.weekOfWeekBasedYear()) == wk && it.get(wf.weekBasedYear()) == yr } }.size
    }
    Scaffold(topBar = { TopAppBar(title = { Text("History") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface)) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { StatCard { Label("Sessions per week"); BarChart(weeks, goal = 3, modifier = Modifier.padding(top = 8.dp), base = cs.primaryContainer, hit = cs.primary) } }
            if (done.isEmpty()) item { EmptyState(Icons.Default.History, "No sessions yet", "Finished workouts show up here with their coach review.") }
            items(done, key = { it.id }) { s ->
                val vol = volumeBySession[s.id] ?: 0.0
                val n = sets.count { it.sessionId == s.id }
                ListItem(
                    leadingContent = { ShapeIcon(Icons.Default.FitnessCenter, cs.primaryContainer, cs.onPrimaryContainer, MaterialShapes.Cookie6Sided) },
                    headlineContent = { Text(s.dayName) },
                    supportingContent = { Text("${s.date.prettyDate()} · $n sets · ${vol.toInt()} kg" + if (s.debrief.isNotBlank()) " · reviewed" else "") },
                    trailingContent = { IconButton({ vm.deleteSession(s) }) { Icon(Icons.Default.Delete, null, tint = cs.onSurfaceVariant) } },
                    colors = ListItemDefaults.colors(containerColor = cs.surfaceContainerLow), modifier = Modifier.clip(MaterialTheme.shapes.large).clickable { vm.openSession(s); onOpen() },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
