@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.repcoach.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mohithash.repcoach.ui.AnimatedNumber
import com.mohithash.repcoach.ui.AppViewModel
import com.mohithash.repcoach.ui.EmptyState
import com.mohithash.repcoach.ui.HeroCard
import com.mohithash.repcoach.ui.Label
import com.mohithash.repcoach.ui.ShapeIcon
import com.mohithash.repcoach.ui.TrendChart
import com.mohithash.repcoach.ui.theme.Brand

@Composable
fun RecordsScreen(vm: AppViewModel) {
    val bests by vm.bests.collectAsState()
    val sets by vm.allSets.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val cs = MaterialTheme.colorScheme
    val totalVolume = sets.sumOf { it.weight * it.reps }
    val volumeSeries = sessions.filter { it.endedAt > 0 }.sortedBy { it.startedAt }.map { s -> sets.filter { it.sessionId == s.id }.sumOf { it.weight * it.reps }.toFloat() }
    Scaffold(topBar = { TopAppBar(title = { Text("Records") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface)) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                HeroCard(colors = listOf(cs.primary, Brand.heroDeep), blobShape = MaterialShapes.Sunny) {
                    val on = cs.onPrimary
                    Label("Lifetime volume", on.copy(alpha = 0.8f)); AnimatedNumber(totalVolume.toInt(), MaterialTheme.typography.displaySmall, on, " kg")
                    Text("${sets.size} sets · ${sessions.count { it.endedAt > 0 }} sessions", color = on.copy(alpha = 0.85f))
                    if (volumeSeries.size >= 2) TrendChart(volumeSeries, Modifier.padding(top = 8.dp), line = cs.secondary)
                }
            }
            if (bests.isEmpty()) item { EmptyState(Icons.Default.EmojiEvents, "No records yet", "Heaviest set per exercise appears here.") }
            items(bests, key = { it.exercise }) { b ->
                ListItem(leadingContent = { ShapeIcon(Icons.Default.EmojiEvents, cs.secondaryContainer, cs.onSecondaryContainer, MaterialShapes.Sunny) }, headlineContent = { Text(b.exercise) },
                    trailingContent = { Text("${if (b.weight % 1.0 == 0.0) b.weight.toInt() else b.weight} kg × ${b.reps}", style = MaterialTheme.typography.titleMedium) },
                    colors = ListItemDefaults.colors(containerColor = cs.surfaceContainerLow), modifier = Modifier.clip(MaterialTheme.shapes.large))
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
