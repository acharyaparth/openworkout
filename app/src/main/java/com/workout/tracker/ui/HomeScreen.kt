package com.workout.tracker.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.workout.tracker.ui.programs.ProgramsScreen
import com.workout.tracker.ui.schedule.ScheduleScreen
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurface

@Composable
fun HomeScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = AppSurface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Today") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text("Programs") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                    label = { Text("Progress") },
                    colors = navColors()
                )
            }
        }
    ) { inner ->
        androidx.compose.foundation.layout.Box(Modifier.padding(inner)) {
            when (tab) {
                0 -> ScheduleScreen(
                    onOpenWorkout = { nav.navigate(Routes.overview(it)) },
                    onStartWorkout = { nav.navigate(Routes.player(it)) },
                )
                1 -> ProgramsScreen(
                    onOpenProgram = { nav.navigate(Routes.workouts(it)) },
                    onEditProgram = { nav.navigate(Routes.editProgram(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
                else -> com.workout.tracker.ui.progress.ProgressScreen()
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AppGreen,
    selectedTextColor = AppGreen,
    indicatorColor = MaterialTheme.colorScheme.background,
    unselectedIconColor = AppMuted,
    unselectedTextColor = AppMuted,
)
