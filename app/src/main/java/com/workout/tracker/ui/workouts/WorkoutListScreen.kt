package com.workout.tracker.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.AppContainer
import com.workout.tracker.data.Program
import com.workout.tracker.data.Repository
import com.workout.tracker.data.Workout
import com.workout.tracker.ui.common.EmptyState
import com.workout.tracker.ui.common.AppCard
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import kotlinx.coroutines.launch

class WorkoutListViewModel(private val repo: Repository, val programId: Long) : ViewModel() {
    val workouts = repo.workouts(programId)
    var program by androidx.compose.runtime.mutableStateOf<Program?>(null)
        private set

    init { refreshProgram() }
    fun refreshProgram() = viewModelScope.launch { program = repo.getProgram(programId) }

    fun addWorkout(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val id = repo.addWorkout(programId, name)
        onCreated(id)
    }
    fun delete(workout: Workout) = viewModelScope.launch { repo.deleteWorkout(workout) }
    suspend fun countFor(workoutId: Long) = repo.exerciseCount(workoutId)
}

@Composable
fun WorkoutListScreen(
    programId: Long,
    onBack: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    onEditWorkout: (Long?) -> Unit,
    onEditProgram: () -> Unit,
) {
    val vm = appViewModel(key = "workouts-$programId") { c: AppContainer ->
        WorkoutListViewModel(c.repository, programId)
    }
    val workouts by vm.workouts.collectAsState(initial = emptyList())
    LaunchedEffect(Unit) { vm.refreshProgram() }
    var pendingDelete by remember { mutableStateOf<Workout?>(null) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditWorkout(null) },
                containerColor = AppGreen,
                contentColor = Color(0xFF06110B),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Workout", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppGreen)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        vm.program?.name ?: "Program",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                TextButton(onClick = onEditProgram) { Text("Rename", color = AppMuted) }
            }

            if (workouts.isEmpty()) {
                EmptyState("No workouts yet.\nTap “Workout” to add one (it starts with Warm-up, Main Exercise and Cooldown).")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(workouts, key = { it.id }) { workout ->
                        val count by produceState(0, workout.id) { value = vm.countFor(workout.id) }
                        WorkoutCard(
                            workout = workout,
                            count = count,
                            onOpen = { onOpenWorkout(workout.id) },
                            onDelete = { pendingDelete = workout },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("“${w.name}” and its logged sets will be removed.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(w); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    count: Int,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    workout.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text("$count exercises", color = AppMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Delete",
                color = AppMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickableNoRipple(onDelete)
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppGreen, modifier = Modifier.size(24.dp))
        }
    }
}

