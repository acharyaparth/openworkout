package com.workout.tracker.ui.exercise

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.workout.tracker.AppContainer
import com.workout.tracker.data.Exercise
import com.workout.tracker.data.HistorySession
import com.workout.tracker.data.PersonalRecords
import com.workout.tracker.data.Repository
import com.workout.tracker.data.SettingsStore
import com.workout.tracker.data.WeightUnit
import com.workout.tracker.data.computeRecords
import com.workout.tracker.data.groupHistory
import com.workout.tracker.data.trimWeight
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.formatDate
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurface
import com.workout.tracker.ui.theme.AppSurfaceHigh
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(private val repo: Repository, settings: SettingsStore, val exerciseId: Long) : ViewModel() {
    val exercise = MutableStateFlow<Exercise?>(null)
    val history = repo.historyForExercise(exerciseId)
    val unit = settings.unit

    init { viewModelScope.launch { exercise.value = repo.getExercise(exerciseId) } }
}

@Composable
fun ExerciseDetailScreen(exerciseId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "exdetail-$exerciseId") { c: AppContainer ->
        ExerciseDetailViewModel(c.repository, c.settings, exerciseId)
    }
    val exercise by vm.exercise.collectAsState()
    val historyRaw by vm.history.collectAsState(initial = emptyList())
    val unit by vm.unit.collectAsState(initial = WeightUnit.LBS)
    val context = LocalContext.current

    var tab by remember { mutableIntStateOf(0) } // 0 history, 1 records
    val sessions = remember(historyRaw) { groupHistory(historyRaw) }
    val records = remember(historyRaw) { computeRecords(historyRaw) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppGreen)
            }
            Text("Exercise history", color = AppMuted, style = MaterialTheme.typography.titleSmall)
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val url = exercise?.youtubeUrl.orEmpty()
                if (url.isNotBlank()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppSurface)
                            .clickableNoRipple {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = AppGreen, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Watch demo on YouTube", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Text(
                    exercise?.name ?: "Exercise",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                TabSwitch(tab = tab, onSelect = { tab = it })
            }

            if (tab == 0) {
                if (sessions.isEmpty()) {
                    item { Text("No logged sessions yet.", color = AppMuted, modifier = Modifier.padding(vertical = 12.dp)) }
                } else {
                    items(sessions.size) { idx ->
                        HistoryCard(sessions[idx], unit)
                    }
                }
            } else {
                item { RecordsCard(records, unit) }
            }
        }
    }
}

@Composable
private fun TabSwitch(tab: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(AppSurface)
            .padding(4.dp)
    ) {
        listOf("History", "Records").forEachIndexed { i, label ->
            val selected = i == tab
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) AppGreen else Color.Transparent)
                    .clickableNoRipple { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color(0xFF06110B) else AppMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(session: HistorySession, unit: WeightUnit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppSurface).padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(session.workoutName, color = AppGreen, fontWeight = FontWeight.SemiBold)
            Text(formatDate(session.date), color = AppGreen, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        session.sets.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Set ${s.setIndex}", color = AppMuted, modifier = Modifier.width(64.dp))
                Text("${unit.fromKg(s.weightKg).trimWeight()} ${unit.label}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(90.dp))
                Text("×", color = AppMuted, modifier = Modifier.width(24.dp))
                Text("${s.reps} reps", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun RecordsCard(records: PersonalRecords, unit: WeightUnit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppSurface).padding(16.dp)
    ) {
        Text("PERSONAL BEST", color = AppMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(12.dp))
        if (!records.hasAny) {
            Text("No records yet — finish a workout to set one.", color = AppMuted)
        } else {
            RecordRow(
                label = "Heaviest set",
                value = records.topSetKg?.let { "${unit.fromKg(it).trimWeight()} ${unit.label}" } ?: "—",
                date = records.topSetDate
            )
            Spacer(Modifier.height(10.dp))
            RecordRow(
                label = "Highest volume",
                value = records.topVolumeKg?.let { "${unit.fromKg(it).trimWeight()} ${unit.label}" } ?: "—",
                date = records.topVolumeDate
            )
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String, date: Long?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppSurfaceHigh).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (date != null) Text(formatDate(date), color = AppMuted, style = MaterialTheme.typography.labelMedium)
        }
        Text(value, color = AppGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

