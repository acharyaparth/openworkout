package com.workout.tracker.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workout.tracker.data.Exercise
import com.workout.tracker.data.Program
import com.workout.tracker.data.Repository
import com.workout.tracker.data.Workout
import com.workout.tracker.ui.common.PillButton
import com.workout.tracker.ui.common.appContainer
import com.workout.tracker.ui.theme.AppDivider
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import kotlinx.coroutines.launch

@Composable
private fun EditScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppGreen)
            }
            Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppGreen,
            unfocusedBorderColor = AppDivider,
            focusedLabelColor = AppGreen,
            cursorColor = AppGreen,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------- Program ----------

@Composable
fun EditProgramScreen(programId: Long?, onDone: () -> Unit) {
    val repo = appContainer().repository
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<Program?>(null) }
    var name by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    LaunchedEffect(programId) {
        if (programId != null) {
            repo.getProgram(programId)?.let {
                loaded = it; name = it.name; active = it.active
            }
        }
    }

    EditScaffold(if (programId == null) "New program" else "Edit program", onDone) {
        Field("Program name", name) { name = it }
        SwitchRow("Active", active) { active = it }
        Spacer(Modifier.height(8.dp))
        PillButton("Save", onClick = {
            scope.launch {
                if (programId == null) {
                    repo.addProgram(name)
                } else {
                    loaded?.let { repo.updateProgram(it.copy(name = name.trim().ifBlank { it.name }, active = active)) }
                }
                onDone()
            }
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}

// ---------- Workout ----------

@Composable
fun EditWorkoutScreen(programId: Long, workoutId: Long?, onDone: () -> Unit) {
    val repo = appContainer().repository
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<Workout?>(null) }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(workoutId) {
        if (workoutId != null) {
            repo.getWorkout(workoutId)?.let { loaded = it; name = it.name }
        }
    }

    EditScaffold(if (workoutId == null) "New workout" else "Edit workout", onDone) {
        Field("Workout name", name) { name = it }
        if (workoutId == null) {
            Text(
                "Starts with Warm-up, Main Exercise and Cooldown sections you can fill in next.",
                color = AppMuted, style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        PillButton("Save", onClick = {
            scope.launch {
                if (workoutId == null) {
                    repo.addWorkout(programId, name)
                } else {
                    loaded?.let { repo.updateWorkout(it.copy(name = name.trim().ifBlank { it.name })) }
                }
                onDone()
            }
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}

// ---------- Exercise ----------

@Composable
fun EditExerciseScreen(groupId: Long, exerciseId: Long?, onDone: () -> Unit) {
    val repo = appContainer().repository
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<Exercise?>(null) }
    var label by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var youtube by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var timeBased by remember { mutableStateOf(false) }
    var tracksWeight by remember { mutableStateOf(true) }
    var tracked by remember { mutableStateOf(true) }

    LaunchedEffect(exerciseId) {
        if (exerciseId != null) {
            repo.getExercise(exerciseId)?.let {
                loaded = it
                label = it.label; name = it.name; prescription = it.prescription
                youtube = it.youtubeUrl; sets = it.targetSets.toString(); timeBased = it.timeBased
                tracksWeight = it.tracksWeight; tracked = it.tracked
            }
        }
    }

    EditScaffold(if (exerciseId == null) "New exercise" else "Edit exercise", onDone) {
        Field("Label (e.g. A1)", label) { label = it }
        Field("Exercise name", name) { name = it }
        Field("Prescription (e.g. 10 each side)", prescription, singleLine = false) { prescription = it }
        Field("YouTube link (optional)", youtube, keyboardType = KeyboardType.Uri) { youtube = it }
        Field("Default sets", sets, keyboardType = KeyboardType.Number) { new -> sets = new.filter { it.isDigit() } }
        SwitchRow("Tracks weight (uncheck for bodyweight / mobility)", tracksWeight) { tracksWeight = it }
        SwitchRow("Time-based (hold in seconds)", timeBased) { timeBased = it }
        SwitchRow("Tracked (uncheck for a 'just for fun' item)", tracked) { tracked = it }
        Spacer(Modifier.height(8.dp))
        PillButton("Save", onClick = {
            scope.launch {
                val setCount = sets.toIntOrNull() ?: 3
                if (exerciseId == null) {
                    repo.addExercise(groupId, label.trim(), name.trim(), prescription.trim(), youtube.trim(), setCount, timeBased, tracksWeight, tracked)
                } else {
                    loaded?.let {
                        repo.updateExercise(
                            it.copy(
                                label = label.trim(), name = name.trim().ifBlank { it.name },
                                prescription = prescription.trim(), youtubeUrl = youtube.trim(),
                                targetSets = setCount.coerceIn(1, 12), timeBased = timeBased,
                                tracksWeight = tracksWeight, tracked = tracked
                            )
                        )
                    }
                }
                onDone()
            }
        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color(0xFF06110B),
                checkedTrackColor = AppGreen,
                uncheckedTrackColor = AppDivider,
            )
        )
    }
}
