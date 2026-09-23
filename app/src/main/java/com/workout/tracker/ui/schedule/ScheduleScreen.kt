package com.workout.tracker.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.workout.tracker.AppContainer
import com.workout.tracker.data.Repository
import com.workout.tracker.data.Workout
import com.workout.tracker.data.WorkoutSession
import com.workout.tracker.data.trimWeight
import com.workout.tracker.ui.common.PillButton
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.formatDate
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppDivider
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurface
import com.workout.tracker.ui.theme.AppSurfaceHigh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(private val repo: Repository, settings: com.workout.tracker.data.SettingsStore) : ViewModel() {
    val workouts = repo.scheduledProgram().flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else repo.workouts(p.id)
    }
    val finished = repo.finishedSessions()
    val totalVolumeKg = repo.totalVolumeKg()
    val unit = settings.unit
    suspend fun exerciseCount(id: Long) = repo.exerciseCount(id)
}

@Composable
fun ScheduleScreen(
    onOpenWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
) {
    val vm = appViewModel { c: AppContainer -> ScheduleViewModel(c.repository, c.settings) }
    val workouts by vm.workouts.collectAsState(initial = emptyList())
    val finished by vm.finished.collectAsState(initial = emptyList())
    val totalVolumeKg by vm.totalVolumeKg.collectAsState(initial = 0.0)
    val unit by vm.unit.collectAsState(initial = com.workout.tracker.data.WeightUnit.LBS)

    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(today) }

    // completions grouped by the local date they were finished on
    val completionsByDate = remember(finished) {
        finished.groupBy {
            Instant.ofEpochMilli(it.finishedAt ?: it.startedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val completedCount = finished.size
    val upNext = if (workouts.isEmpty()) null else workouts[completedCount % workouts.size]

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Today", color = AppGreen, style = MaterialTheme.typography.titleLarge)
            Text(today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM")),
                color = AppMuted, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            UpNextCard(upNext, completedCount, workouts.size, vm, onStartWorkout, onOpenWorkout)
        }
        item {
            val totalSec = finished.sumOf { ((it.finishedAt ?: it.startedAt) - it.startedAt) } / 1000
            StatsRow(
                workouts = finished.size,
                totalSeconds = totalSec,
                volume = "${unit.fromKg(totalVolumeKg).trimWeight()} ${unit.label}",
            )
        }
        item {
            MonthCalendar(
                month = month,
                today = today,
                selected = selected,
                completedDates = completionsByDate.keys,
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                onSelect = { selected = it },
            )
        }
        item {
            DayDetail(
                date = selected,
                sessions = selected?.let { completionsByDate[it] }.orEmpty(),
                onOpenWorkout = onOpenWorkout,
            )
        }
    }
}

@Composable
private fun UpNextCard(
    upNext: Workout?,
    completedCount: Int,
    total: Int,
    vm: ScheduleViewModel,
    onStart: (Long) -> Unit,
    onOpen: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppSurface).padding(18.dp)) {
        if (upNext == null) {
            Text("No program yet.", color = AppMuted)
            return@Column
        }
        val count by produceState(0, upNext.id) { value = vm.exerciseCount(upNext.id) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("UP NEXT", color = AppMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (total > 0) Text(
                "Workout ${(completedCount % total) + 1} of $total",
                color = AppMuted, style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(upNext.name, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text("$count exercises · $completedCount completed", color = AppMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Start workout", onClick = { onStart(upNext.id) })
            PillButton("View", onClick = { onOpen(upNext.id) }, filled = false)
        }
    }
}

@Composable
private fun StatsRow(workouts: Int, totalSeconds: Long, volume: String) {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("WORKOUTS", workouts.toString(), Modifier.weight(1f))
        StatTile("TIME", timeStr, Modifier.weight(1f))
        StatTile("VOLUME", volume, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(AppSurface).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = AppGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = AppMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    completedDates: Set<LocalDate>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppSurface).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = AppGreen)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = AppGreen)
            }
        }
        Spacer(Modifier.height(6.dp))
        // weekday headers, Monday-first
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, color = AppMuted, style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        // build cells (Monday-first)
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value + 6) % 7
        val cells = buildList<LocalDate?> {
            repeat(lead) { add(null) }
            for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
            while (size % 7 != 0) add(null)
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                completed = date in completedDates,
                                isToday = date == today,
                                isSelected = date == selected,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    completed: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (completed) AppGreen else Color.Transparent
    var m = Modifier.fillMaxSize().clip(CircleShape).background(bg)
    if (isToday && !completed) m = m.border(2.dp, AppGreen, CircleShape)
    else if (isSelected && !completed) m = m.border(1.dp, AppMuted, CircleShape)
    Box(m.clickableNoRipple(onClick), contentAlignment = Alignment.Center) {
        if (completed) {
            Icon(Icons.Default.Check, contentDescription = "Completed",
                tint = Color(0xFF06110B), modifier = Modifier.size(18.dp))
        } else {
            Text(date.dayOfMonth.toString(),
                color = if (isToday) AppGreen else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun DayDetail(date: LocalDate?, sessions: List<WorkoutSession>, onOpenWorkout: (Long) -> Unit) {
    if (date == null) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppSurface).padding(16.dp)) {
        Text(date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
            color = AppGreen, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (sessions.isEmpty()) {
            Text("No workout logged on this day.", color = AppMuted, style = MaterialTheme.typography.bodyMedium)
        } else {
            sessions.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppSurfaceHigh)
                        .clickableNoRipple { onOpenWorkout(s.workoutId) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AppGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(s.workoutName, color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
