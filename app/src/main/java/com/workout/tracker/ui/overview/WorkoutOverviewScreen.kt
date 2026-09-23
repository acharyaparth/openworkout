package com.workout.tracker.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.AppContainer
import com.workout.tracker.data.ExerciseGroup
import com.workout.tracker.data.GroupWithExercises
import com.workout.tracker.data.Repository
import com.workout.tracker.data.Section
import com.workout.tracker.data.SectionWithContent
import com.workout.tracker.data.WorkoutWithContent
import com.workout.tracker.ui.common.PillButton
import com.workout.tracker.ui.common.SectionHeader
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppDivider
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurface
import com.workout.tracker.ui.theme.AppSurfaceHigh
import kotlinx.coroutines.launch

class OverviewViewModel(private val repo: Repository, val workoutId: Long) : ViewModel() {
    val content = repo.workoutContent(workoutId)

    fun addSection(name: String) = viewModelScope.launch { repo.addSection(workoutId, name) }
    fun deleteSection(section: Section) = viewModelScope.launch { repo.deleteSection(section) }
    fun addGroup(sectionId: Long, label: String, scheme: String, note: String) =
        viewModelScope.launch { repo.addGroup(sectionId, label, scheme, note) }
    fun updateGroup(group: ExerciseGroup) = viewModelScope.launch { repo.updateGroup(group) }
    fun deleteGroup(group: ExerciseGroup) = viewModelScope.launch { repo.deleteGroup(group) }
    fun deleteExercise(exerciseId: Long) = viewModelScope.launch {
        repo.getExercise(exerciseId)?.let { repo.deleteExercise(it) }
    }
}

@Composable
fun WorkoutOverviewScreen(
    workoutId: Long,
    editable: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onEditWorkout: () -> Unit,
    onEditExercise: (groupId: Long, exerciseId: Long?) -> Unit,
) {
    val vm = appViewModel(key = "overview-$workoutId") { c: AppContainer ->
        OverviewViewModel(c.repository, workoutId)
    }
    val content by vm.content.collectAsState(initial = null)
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }

    // dialogs
    var showAddSection by remember { mutableStateOf(false) }
    var groupDialog by remember { mutableStateOf<GroupDialogState?>(null) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (content != null) {
                PillButtonFab(onStart)
            }
        }
    ) { inner ->
        val c = content
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppGreen)
                }
                Text(
                    c?.workout?.name ?: "Workout",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (editable) {
                    IconButton(onClick = onEditWorkout) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename workout", tint = AppMuted)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            if (c == null) {
                Text("Loading…", color = AppMuted, modifier = Modifier.padding(20.dp))
            } else {
                val sections = c.sections.sortedBy { it.section.position }
                sections.forEach { sc ->
                    val open = expanded[sc.section.id] ?: true
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SectionHeader(
                            title = sc.section.name,
                            expanded = open,
                            onToggle = { expanded[sc.section.id] = !open },
                        )
                        if (open) {
                            Spacer(Modifier.height(8.dp))
                            SectionBody(
                                sc = sc,
                                editable = editable,
                                onOpenExercise = onOpenExercise,
                                onEditExercise = onEditExercise,
                                onDeleteExercise = { vm.deleteExercise(it) },
                                onAddGroup = { groupDialog = GroupDialogState(sectionId = sc.section.id) },
                                onEditGroup = { g ->
                                    groupDialog = GroupDialogState(
                                        sectionId = sc.section.id, existing = g,
                                        label = g.label, scheme = g.scheme, note = g.note
                                    )
                                },
                                onDeleteGroup = { vm.deleteGroup(it) },
                                onDeleteSection = { vm.deleteSection(sc.section) },
                            )
                        }
                    }
                }
                if (editable) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "+ Add section",
                        color = AppGreen,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickableNoRipple { showAddSection = true }
                    )
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showAddSection) {
        TextPromptDialog(
            title = "New section",
            label = "Section name",
            initial = "",
            onDismiss = { showAddSection = false },
            onConfirm = { name -> if (name.isNotBlank()) vm.addSection(name.trim()); showAddSection = false }
        )
    }

    groupDialog?.let { st ->
        GroupDialog(
            state = st,
            onDismiss = { groupDialog = null },
            onConfirm = { label, scheme, note ->
                val existing = st.existing
                if (existing == null) vm.addGroup(st.sectionId, label, scheme, note)
                else vm.updateGroup(existing.copy(label = label, scheme = scheme, note = note))
                groupDialog = null
            }
        )
    }
}

@Composable
private fun PillButtonFab(onStart: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(AppGreen)
            .clickableNoRipple(onStart)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF06110B))
        Spacer(Modifier.width(6.dp))
        Text("Start workout", color = Color(0xFF06110B), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionBody(
    sc: SectionWithContent,
    editable: Boolean,
    onOpenExercise: (Long) -> Unit,
    onEditExercise: (Long, Long?) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (ExerciseGroup) -> Unit,
    onDeleteGroup: (ExerciseGroup) -> Unit,
    onDeleteSection: () -> Unit,
) {
    val groups = sc.groups.sortedBy { it.group.position }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEach { gwe ->
            GroupBlock(
                gwe = gwe,
                editable = editable,
                onOpenExercise = onOpenExercise,
                onEditExercise = onEditExercise,
                onDeleteExercise = onDeleteExercise,
                onEditGroup = { onEditGroup(gwe.group) },
                onDeleteGroup = { onDeleteGroup(gwe.group) },
            )
        }
        if (editable) {
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "+ Add block",
                    color = AppGreen,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickableNoRipple(onAddGroup)
                )
                Text(
                    "Delete section",
                    color = AppMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickableNoRipple(onDeleteSection)
                )
            }
        }
    }
}

@Composable
private fun GroupBlock(
    gwe: GroupWithExercises,
    editable: Boolean,
    onOpenExercise: (Long) -> Unit,
    onEditExercise: (Long, Long?) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
) {
    val group = gwe.group
    val exercises = gwe.exercises.sortedBy { it.position }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .padding(14.dp)
    ) {
        // group header: "A. 5 Rounds"
        Row(verticalAlignment = Alignment.CenterVertically) {
            val header = buildString {
                if (group.label.isNotBlank()) append(group.label.trim()).append(". ")
                append(group.scheme.ifBlank { "Block" })
            }
            Text(
                header,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (editable) {
                Icon(
                    Icons.Default.Edit, contentDescription = "Edit block", tint = AppMuted,
                    modifier = Modifier.size(18.dp).clickableNoRipple(onEditGroup)
                )
                Spacer(Modifier.width(14.dp))
                Icon(
                    Icons.Default.Delete, contentDescription = "Delete block", tint = AppMuted,
                    modifier = Modifier.size(18.dp).clickableNoRipple(onDeleteGroup)
                )
            }
        }
        if (group.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(group.note, color = AppMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))

        exercises.forEach { ex ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppSurfaceHigh)
                    .clickableNoRipple { onOpenExercise(ex.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    val title = buildString {
                        if (ex.label.isNotBlank()) append(ex.label.trim()).append(". ")
                        append(ex.name)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = AppGreen,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                        if (ex.youtubeUrl.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Outlined.OndemandVideo, contentDescription = "Has demo video",
                                tint = AppMuted, modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (ex.prescription.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(ex.prescription, color = AppMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (editable) {
                    Icon(
                        Icons.Default.Edit, contentDescription = "Edit exercise", tint = AppMuted,
                        modifier = Modifier.size(18.dp).clickableNoRipple { onEditExercise(group.id, ex.id) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Delete, contentDescription = "Delete exercise", tint = AppMuted,
                        modifier = Modifier.size(18.dp).clickableNoRipple { onDeleteExercise(ex.id) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (editable) {
            Text(
                "+ Add exercise",
                color = AppGreen,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickableNoRipple { onEditExercise(group.id, null) }
            )
        }
    }
}

// ---- dialogs ----

private data class GroupDialogState(
    val sectionId: Long,
    val existing: ExerciseGroup? = null,
    val label: String = "",
    val scheme: String = "",
    val note: String = "",
)

@Composable
private fun GroupDialog(
    state: GroupDialogState,
    onDismiss: () -> Unit,
    onConfirm: (label: String, scheme: String, note: String) -> Unit,
) {
    var label by remember { mutableStateOf(state.label) }
    var scheme by remember { mutableStateOf(state.scheme) }
    var note by remember { mutableStateOf(state.note) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.existing == null) "New block" else "Edit block") },
        text = {
            Column {
                AppField("Label (e.g. A)", label) { label = it }
                Spacer(Modifier.height(8.dp))
                AppField("Scheme (e.g. 5 Rounds)", scheme) { scheme = it }
                Spacer(Modifier.height(8.dp))
                AppField("Note (optional)", note) { note = it }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(label.trim(), scheme.trim(), note.trim()) }) {
                Text("Save", color = AppGreen)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TextPromptDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AppField(label, value) { value = it } },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(value) }) { Text("Save", color = AppGreen) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AppField(label: String, value: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = label.startsWith("Label") || label.startsWith("Scheme") || label.startsWith("Section"),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppGreen,
            unfocusedBorderColor = AppDivider,
            focusedLabelColor = AppGreen,
            cursorColor = AppGreen,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

