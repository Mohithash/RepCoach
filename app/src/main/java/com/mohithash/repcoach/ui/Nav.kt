@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.mohithash.repcoach.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mohithash.repcoach.ui.screens.HistoryScreen
import com.mohithash.repcoach.ui.screens.OnboardingScreen
import com.mohithash.repcoach.ui.screens.PlanScreen
import com.mohithash.repcoach.ui.screens.RecordsScreen
import com.mohithash.repcoach.ui.screens.SettingsScreen
import com.mohithash.repcoach.ui.screens.WorkoutScreen

enum class Tab(val route: String, val label: String, val icon: ImageVector, val selected: ImageVector) {
    PLAN("plan", "Plan", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    HISTORY("history", "History", Icons.Outlined.History, Icons.Filled.History),
    RECORDS("records", "Records", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
}

@Composable
fun Nav(vm: AppViewModel) {
    val p by vm.profile.collectAsState()
    if (!p.onboarded) { OnboardingScreen(vm); return }
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val current = back?.destination
    val showBar = Tab.entries.any { t -> current?.hierarchy?.any { it.route == t.route } == true }
    Scaffold(bottomBar = {
        if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Tab.entries.forEach { tab ->
                val sel = current?.hierarchy?.any { it.route == tab.route } == true
                NavigationBarItem(selected = sel, onClick = { nav.navigate(tab.route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(if (sel) tab.selected else tab.icon, tab.label) }, label = { Text(tab.label) })
            }
        }
    }) { pad ->
        NavHost(nav, Tab.PLAN.route, Modifier.padding(bottom = pad.calculateBottomPadding())) {
            composable(Tab.PLAN.route) { PlanScreen(vm, onWorkout = { nav.navigate("workout") }, onSettings = { nav.navigate("settings") }) }
            composable(Tab.HISTORY.route) { HistoryScreen(vm, onOpen = { nav.navigate("workout") }) }
            composable(Tab.RECORDS.route) { RecordsScreen(vm) }
            composable("workout") { WorkoutScreen(vm, onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}
