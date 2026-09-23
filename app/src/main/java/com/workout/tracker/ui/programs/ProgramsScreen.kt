package com.workout.tracker.ui.programs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.AppContainer
import com.workout.tracker.data.Program
import com.workout.tracker.data.Repository
import com.workout.tracker.ui.common.EmptyState
import com.workout.tracker.ui.common.AppCard
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurfaceHigh
import kotlinx.coroutines.launch

class ProgramsViewModel(private val repo: Repository) : ViewModel() {
    val programs = repo.programs()
    fun countFlow(programId: Long) = repo.programExerciseCount(programId)
    fun delete(program: Program) = viewModelScope.launch { repo.deleteProgram(program) }
    fun toggleActive(program: Program) =
        viewModelScope.launch { repo.updateProgram(program.copy(active = !program.active)) }
}

@Composable
fun ProgramsScreen(
    onOpenProgram: (Long) -> Unit,
    onEditProgram: (Long?) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val vm = appViewModel { c: AppContainer -> ProgramsViewModel(c.repository) }
    val programs by vm.programs.collectAsState(initial = emptyList())
    var pendingDelete by remember { mutableStateOf<Program?>(null) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditProgram(null) },
                containerColor = AppGreen,
                contentColor = androidx.compose.ui.graphics.Color(0xFF06110B),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Program", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(
                        com.workout.tracker.R.string.app_name
                    ),
                    color = AppGreen,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppMuted)
                }
            }
            Text(
                "Programs",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )

            if (programs.isEmpty()) {
                EmptyState("No programs yet.\nTap “Program” to create your first block.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(programs, key = { it.id }) { program ->
                        val count by vm.countFlow(program.id).collectAsState(initial = 0)
                        ProgramCard(
                            program = program,
                            exerciseCount = count,
                            onOpen = { onOpenProgram(program.id) },
                            onEdit = { onEditProgram(program.id) },
                            onToggleActive = { vm.toggleActive(program) },
                            onDelete = { pendingDelete = program },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { prog ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete program?") },
            text = { Text("“${prog.name}” and all its workouts and logged sets will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(prog); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProgramCard(
    program: Program,
    exerciseCount: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(onClick = onOpen) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    program.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(active = program.active, onClick = onToggleActive)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$exerciseCount exercises", color = AppMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit program",
                    tint = AppMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppSurfaceHigh)
                        .padding(6.dp)
                        .height(20.dp)
                        .clickableNoRipple(onEdit)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Delete",
                    color = AppMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickableNoRipple(onDelete)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean, onClick: () -> Unit) {
    val label = if (active) "Active" else "Expired"
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) AppGreen else AppSurfaceHigh)
            .clickableNoRipple(onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) androidx.compose.ui.graphics.Color(0xFF06110B) else AppMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

