package com.workout.tracker.ui.player

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.AppContainer
import com.workout.tracker.data.Exercise
import com.workout.tracker.data.LogType
import com.workout.tracker.data.Repository
import com.workout.tracker.data.SectionWithContent
import com.workout.tracker.data.SetLog
import com.workout.tracker.data.SettingsStore
import com.workout.tracker.data.WeightUnit
import com.workout.tracker.data.WorkoutSession
import com.workout.tracker.data.WorkoutWithContent
import com.workout.tracker.data.trimWeight
import com.workout.tracker.ui.common.GreenCheck
import com.workout.tracker.ui.common.PillButton
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppDivider
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurface
import com.workout.tracker.ui.theme.AppSurfaceHigh
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SetRowState(
    var id: Long,
    weight: String,
    reps: String,
    done: Boolean,
) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
    var done by mutableStateOf(done)
}

/** Reopen the same workout within this window and it resumes instead of restarting. */
private const val RESUME_WINDOW_MS = 12L * 60 * 60 * 1000

class PlayerViewModel(
    private val repo: Repository,
    private val settings: SettingsStore,
    val workoutId: Long,
) : ViewModel() {

    var content by mutableStateOf<WorkoutWithContent?>(null); private set
    var sessionId by mutableStateOf(0L); private set
    var startedAtMillis by mutableStateOf(0L); private set
    val unitFlow = settings.unit

    // exerciseId -> observable list of set rows
    val rows = mutableStateMapOf<Long, SnapshotStateList<SetRowState>>()

    init {
        viewModelScope.launch {
            val w = repo.getWorkout(workoutId) ?: return@launch
            val unit = settings.unit.first()
            repo.deleteStaleUnfinished(System.currentTimeMillis() - RESUME_WINDOW_MS)
            // Resume a recent in-progress session for this workout, or start a fresh one.
            val existing = repo.activeSession(workoutId)
            val resuming = existing != null &&
                System.currentTimeMillis() - existing.startedAt < RESUME_WINDOW_MS
            if (resuming) {
                sessionId = existing!!.id
                startedAtMillis = existing.startedAt
            } else {
                sessionId = repo.startSession(w)
                startedAtMillis = System.currentTimeMillis()
            }
            val c = repo.workoutContentOnce(workoutId)
            content = c
            val logsByExercise =
                if (resuming) repo.setLogsForSession(sessionId).groupBy { it.exerciseId } else emptyMap()
            c?.sections?.forEach { sec ->
                sec.groups.forEach { g ->
                    g.exercises.forEach { ex ->
                        if (!ex.tracked) return@forEach   // "just for fun" items aren't logged
                        val saved = logsByExercise[ex.id].orEmpty().sortedBy { it.setIndex }
                        val baseN = if (ex.logType == LogType.NONE) 1 else ex.targetSets.coerceAtLeast(1)
                        val n = maxOf(baseN, saved.maxOfOrNull { it.setIndex } ?: 0)
                        val list = mutableStateListOf<SetRowState>()
                        for (i in 1..n) {
                            val log = saved.find { it.setIndex == i }
                            if (log != null) {
                                val wStr = if (log.weightKg > 0.0) unit.fromKg(log.weightKg).trimWeight() else ""
                                val rStr = if (log.reps > 0) log.reps.toString() else ""
                                list.add(SetRowState(log.id, wStr, rStr, log.done))
                            } else {
                                list.add(SetRowState(0L, "", "", false))
                            }
                        }
                        rows[ex.id] = list
                    }
                }
            }
        }
    }

    fun setUnit(u: WeightUnit) = viewModelScope.launch { settings.setUnit(u) }

    fun addSet(exercise: Exercise) {
        rows[exercise.id]?.add(SetRowState(0L, "", "", false))
    }

    fun toggleDone(exercise: Exercise, row: SetRowState, unit: WeightUnit) {
        row.done = !row.done
        persist(exercise, row, unit)
    }

    fun onEdited(exercise: Exercise, row: SetRowState, unit: WeightUnit) {
        // persist every edit so nothing is memory-only (survives the app being killed)
        persist(exercise, row, unit)
    }

    private fun persist(exercise: Exercise, row: SetRowState, unit: WeightUnit) {
        val weightVal = row.weight.toDoubleOrNull() ?: 0.0
        val repsVal = row.reps.toIntOrNull() ?: 0
        val idx = (rows[exercise.id]?.indexOf(row) ?: 0) + 1
        viewModelScope.launch {
            val log = SetLog(
                id = row.id,
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setIndex = idx,
                weightKg = unit.toKg(weightVal),
                reps = repsVal,
                done = row.done,
            )
            val newId = repo.upsertSetLog(log)
            if (row.id == 0L) row.id = newId
        }
    }

    fun finish(onDone: (FinishSummary) -> Unit) {
        viewModelScope.launch {
            val unit = settings.unit.first()
            var volumeKg = 0.0
            var setsDone = 0
            content?.sections?.forEach { sec ->
                sec.groups.forEach { g ->
                    g.exercises.forEach { ex ->
                        rows[ex.id]?.forEachIndexed { i, row ->
                            val hasData = row.weight.isNotBlank() || row.reps.isNotBlank() || row.done
                            if (hasData) {
                                val weightKg = unit.toKg(row.weight.toDoubleOrNull() ?: 0.0)
                                val reps = row.reps.toIntOrNull() ?: 0
                                val log = SetLog(
                                    id = row.id, sessionId = sessionId, exerciseId = ex.id,
                                    exerciseName = ex.name, setIndex = i + 1,
                                    weightKg = weightKg, reps = reps, done = row.done,
                                )
                                val newId = repo.upsertSetLog(log)
                                if (row.id == 0L) row.id = newId
                                if (row.done) {
                                    setsDone++
                                    if (ex.tracksWeight) volumeKg += weightKg * reps
                                }
                            }
                        }
                    }
                }
            }
            val finishedAt = System.currentTimeMillis()
            repo.finishSession(sessionId, finishedAt)
            onDone(FinishSummary(
                durationSec = ((finishedAt - startedAtMillis) / 1000).coerceAtLeast(0),
                volumeKg = volumeKg,
                setsDone = setsDone,
            ))
        }
    }

    /** Abandon the in-progress workout: remove the unfinished session (and its logged sets). */
    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            if (sessionId != 0L) repo.deleteSession(sessionId)
            onDone()
        }
    }

    fun doneCount(): Pair<Int, Int> {
        var total = 0; var done = 0
        rows.values.forEach { list -> list.forEach { total++; if (it.done) done++ } }
        return done to total
    }
}

data class FinishSummary(val durationSec: Long, val volumeKg: Double, val setsDone: Int)

@Composable
fun PlayerScreen(
    workoutId: Long,
    onClose: () -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val vm = appViewModel(key = "player-$workoutId") { c: AppContainer ->
        PlayerViewModel(c.repository, c.settings, workoutId)
    }
    val content = vm.content
    val unit by vm.unitFlow.collectAsState(initial = WeightUnit.LBS)
    var sectionIndex by remember { mutableIntStateOf(0) }
    var showCancel by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<FinishSummary?>(null) }

    // live workout timer
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vm.startedAtMillis) {
        while (true) { nowMillis = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
    }
    val elapsedSec = if (vm.startedAtMillis == 0L) 0L
        else ((nowMillis - vm.startedAtMillis) / 1000).coerceAtLeast(0)

    val sections = content?.sections?.sortedBy { it.section.position } ?: emptyList()
    val (done, total) = vm.doneCount()
    val progress = if (total == 0) 0f else done.toFloat() / total

    Column(Modifier.fillMaxSize()) {
        // top bar
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                UnitToggle(unit = unit, onSelect = { vm.setUnit(it) })
            }
            Text(clockFmt(elapsedSec), color = AppGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { showCancel = true }) {
                Icon(Icons.Default.Close, contentDescription = "Cancel workout", tint = AppMuted)
            }
        }
        // progress
        val title = content?.workout?.name?.uppercase() ?: ""
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = AppMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", color = AppGreen, style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { progress },
            color = AppGreen,
            trackColor = AppDivider,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (content == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Please wait while we load your workout…", color = AppMuted)
            }
            return@Column
        }

        val current = sections.getOrNull(sectionIndex)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (current == null) {
                Text("No sections in this workout.", color = AppMuted)
            } else {
                Text(
                    current.section.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))
                SectionPlayer(
                    section = current,
                    unit = unit,
                    rowsFor = { vm.rows[it] ?: mutableStateListOf() },
                    onToggle = { ex, row -> vm.toggleDone(ex, row, unit) },
                    onEdited = { ex, row -> vm.onEdited(ex, row, unit) },
                    onAddSet = { vm.addSet(it) },
                    onOpenExercise = onOpenExercise,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // nav buttons
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sectionIndex > 0) {
                PillButton(
                    text = "Previous",
                    onClick = { sectionIndex-- },
                    filled = false,
                    modifier = Modifier.weight(1f)
                )
            }
            val isLast = sectionIndex >= sections.lastIndex
            PillButton(
                text = if (isLast) "Finish" else "Next",
                onClick = {
                    if (isLast) vm.finish { summary = it } else sectionIndex++
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("Cancel this workout?") },
            text = { Text("You haven't finished yet. Leaving now discards this session — it won't be logged or count toward your streak.") },
            confirmButton = {
                TextButton(onClick = { showCancel = false; vm.discard(onClose) }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep going") } }
        )
    }

    summary?.let { s ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false
            )
        ) {
            FinishCelebration(s, unit) { summary = null; onClose() }
        }
    }
}

@Composable
private fun FinishCelebration(summary: FinishSummary, unit: WeightUnit, onDone: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC05100A)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiOverlay()
        Column(
            Modifier.padding(28.dp).clip(RoundedCornerShape(20.dp)).background(AppSurface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Workout complete 💪", color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SummaryLine("Duration", clockFmt(summary.durationSec))
            SummaryLine("Sets completed", summary.setsDone.toString())
            SummaryLine("Volume", "${unit.fromKg(summary.volumeKg).trimWeight()} ${unit.label}")
            Spacer(Modifier.height(20.dp))
            PillButton("Done", onClick = onDone)
        }
    }
}

@Composable
private fun ConfettiOverlay() {
    val colors = listOf(
        AppGreen, Color(0xFF3B82F6), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )
    val parts = remember {
        List(110) {
            ConfettiPiece(
                x = kotlin.random.Random.nextFloat(),
                delay = kotlin.random.Random.nextFloat() * 0.5f,
                vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.35f,
                color = colors[kotlin.random.Random.nextInt(colors.size)],
                size = 8f + kotlin.random.Random.nextFloat() * 12f,
                rot = kotlin.random.Random.nextFloat() * 360f,
                spin = (kotlin.random.Random.nextFloat() - 0.5f) * 900f,
            )
        }
    }
    var t by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (t < 4.5f) {
            val now = withFrameNanos { it }
            t = (now - start) / 1_000_000_000f
        }
    }
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        parts.forEach { p ->
            val lt = (t - p.delay).coerceAtLeast(0f)
            val y = (-0.06f + 0.22f * lt + 0.5f * 0.85f * lt * lt) * size.height
            val x = (p.x + p.vx * lt) * size.width
            if (y in -40f..(size.height + 40f)) {
                rotate(
                    degrees = p.rot + p.spin * lt,
                    pivot = androidx.compose.ui.geometry.Offset(x, y)
                ) {
                    drawRect(
                        color = p.color,
                        topLeft = androidx.compose.ui.geometry.Offset(x - p.size / 2, y - p.size / 2),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val x: Float, val delay: Float, val vx: Float,
    val color: Color, val size: Float, val rot: Float, val spin: Float,
)

private fun clockFmt(totalSec: Long): String {
    val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppMuted)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UnitToggle(unit: WeightUnit, onSelect: (WeightUnit) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(AppSurface)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeightUnit.entries.forEach { u ->
            val selected = u == unit
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) AppGreen else Color.Transparent)
                    .clickableNoRipple { onSelect(u) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    u.label,
                    color = if (selected) Color(0xFF06110B) else AppMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionPlayer(
    section: SectionWithContent,
    unit: WeightUnit,
    rowsFor: (Long) -> SnapshotStateList<SetRowState>,
    onToggle: (Exercise, SetRowState) -> Unit,
    onEdited: (Exercise, SetRowState) -> Unit,
    onAddSet: (Exercise) -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val groups = section.groups.sortedBy { it.group.position }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { gwe ->
            val g = gwe.group
            val header = buildString {
                if (g.label.isNotBlank()) append(g.label.trim()).append(". ")
                append(g.scheme.ifBlank { "Block" })
            }
            Text(header, color = AppMuted, style = MaterialTheme.typography.titleSmall)
            if (g.note.isNotBlank()) {
                Text(g.note, color = AppMuted, style = MaterialTheme.typography.bodyMedium)
            }
            gwe.exercises.sortedBy { it.position }.forEach { ex ->
                ExercisePlayerCard(
                    exercise = ex,
                    unit = unit,
                    rows = rowsFor(ex.id),
                    onToggle = { row -> onToggle(ex, row) },
                    onEdited = { row -> onEdited(ex, row) },
                    onAddSet = { onAddSet(ex) },
                    onOpen = { onOpenExercise(ex.id) },
                )
            }
        }
    }
}

@Composable
private fun ExercisePlayerCard(
    exercise: Exercise,
    unit: WeightUnit,
    rows: SnapshotStateList<SetRowState>,
    onToggle: (SetRowState) -> Unit,
    onEdited: (SetRowState) -> Unit,
    onAddSet: () -> Unit,
    onOpen: () -> Unit,
) {
    val showWeight = exercise.logType == LogType.WEIGHT_REPS
    val amountLabel = if (exercise.logType == LogType.TIME) "SECS" else "REPS"
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .padding(14.dp)
    ) {
        val title = buildString {
            if (exercise.label.isNotBlank()) append(exercise.label.trim()).append(". ")
            append(exercise.name)
        }
        Text(
            title,
            color = AppGreen,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickableNoRipple(onOpen)
        )
        if (exercise.prescription.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(exercise.prescription, color = AppMuted, style = MaterialTheme.typography.bodyMedium)
        }

        if (!exercise.tracked) {
            // "just for fun" note — no logging, no done-gating
            Spacer(Modifier.height(6.dp))
            Text("✨ just for fun", color = AppGreen, style = MaterialTheme.typography.labelMedium)
            return@Column
        }
        Spacer(Modifier.height(10.dp))

        if (exercise.logType == LogType.NONE) {
            // mobility / cardio / warm-up — just mark it done
            val row = rows.firstOrNull()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (row?.done == true) "Done" else "Mark done",
                    color = if (row?.done == true) AppGreen else AppMuted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                GreenCheck(done = row?.done == true, onClick = { row?.let { onToggle(it) } })
            }
            return@Column
        }

        // header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SET", color = AppMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
            if (showWeight) {
                Text(unit.label.uppercase(), color = AppMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(18.dp))
            }
            Text(amountLabel, color = AppMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(34.dp))
        }
        Spacer(Modifier.height(4.dp))

        rows.forEachIndexed { i, row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${i + 1}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(36.dp))
                if (showWeight) {
                    NumberField(
                        value = row.weight,
                        onChange = { row.weight = it; onEdited(row) },
                        modifier = Modifier.weight(1f)
                    )
                    Text("×", color = AppMuted, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                }
                NumberField(
                    value = row.reps,
                    onChange = { row.reps = it; onEdited(row) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                GreenCheck(done = row.done, onClick = { onToggle(row) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = AppGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add set", color = AppGreen, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickableNoRipple(onAddSet))
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 6 && new.all { it.isDigit() || it == '.' }) onChange(new) },
        singleLine = true,
        placeholder = { Text("0", color = AppMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppGreen,
            unfocusedBorderColor = AppDivider,
            focusedContainerColor = AppSurfaceHigh,
            unfocusedContainerColor = AppSurfaceHigh,
            cursorColor = AppGreen,
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        modifier = modifier
    )
}

