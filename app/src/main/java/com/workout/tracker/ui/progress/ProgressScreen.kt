package com.workout.tracker.ui.progress

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.tracker.AppContainer
import com.workout.tracker.data.Measurement
import com.workout.tracker.data.Repository
import com.workout.tracker.data.SessionStat
import com.workout.tracker.data.WeightUnit
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
import kotlinx.coroutines.launch

data class MetricDef(val name: String, val unit: String, val usesWeightUnit: Boolean = false)

val BODY_METRICS = listOf(
    MetricDef("Weight", "", usesWeightUnit = true),
    MetricDef("Body Fat", "%"),
    MetricDef("Waist", "in"),
    MetricDef("Chest", "in"),
    MetricDef("Hips", "in"),
    MetricDef("Bicep", "in"),
    MetricDef("Thigh", "in"),
)

class ProgressViewModel(private val repo: Repository) : ViewModel() {
    val measurements = repo.measurements()
    val sessionStats = repo.sessionStats()
    fun add(metric: String, unit: String, value: Double) =
        viewModelScope.launch { repo.addMeasurement(metric, unit, value) }
}

@Composable
fun ProgressScreen() {
    val vm = appViewModel { c: AppContainer -> ProgressViewModel(c.repository) }
    val container = com.workout.tracker.ui.common.appContainer()
    val unit by container.settings.unit.collectAsState(initial = WeightUnit.LBS)
    val measurements by vm.measurements.collectAsState(initial = emptyList())
    val stats by vm.sessionStats.collectAsState(initial = emptyList())

    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text("Progress", color = AppGreen, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp))
        TabSwitch(tab) { tab = it }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (tab == 0) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Body Measurements", color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickableNoRipple { showAdd = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = AppGreen)
                            Text("Add", color = AppGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                val byMetric = measurements.groupBy { it.metric }
                if (byMetric.isEmpty()) {
                    item { Text("No measurements yet. Tap “Add” to log your first.", color = AppMuted,
                        modifier = Modifier.padding(vertical = 8.dp)) }
                } else {
                    // preset order first, then any custom metrics
                    val order = BODY_METRICS.map { it.name }
                    val metrics = byMetric.keys.sortedWith(compareBy({ order.indexOf(it).let { i -> if (i < 0) 999 else i } }, { it }))
                    items(metrics.size) { i ->
                        val name = metrics[i]
                        val entries = byMetric.getValue(name).sortedBy { it.recordedAt }
                        MeasurementCard(name, entries)
                    }
                }
            } else {
                item { TrainingTrends(stats, unit) }
            }
        }
    }

    if (showAdd) AddMeasurementDialog(unit, onDismiss = { showAdd = false }) { metric, u, v ->
        vm.add(metric, u, v); showAdd = false
    }
}

@Composable
private fun TabSwitch(tab: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(50))
            .background(AppSurface).padding(4.dp)
    ) {
        listOf("Metrics", "Training").forEachIndexed { i, label ->
            val sel = i == tab
            Box(Modifier.weight(1f).clip(RoundedCornerShape(50))
                .background(if (sel) AppGreen else Color.Transparent)
                .clickableNoRipple { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center) {
                Text(label, color = if (sel) Color(0xFF06110B) else AppMuted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MeasurementCard(name: String, entries: List<Measurement>) {
    val unitLabel = entries.last().unit
    val latest = entries.last().value
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppSurface).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (unitLabel.isBlank()) name else "$name ($unitLabel)",
                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Text("${latest.trimWeight()}${if (unitLabel.isBlank()) "" else " $unitLabel"}",
                color = AppGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        TrendChart(entries.map { it.recordedAt to it.value.toFloat() })
    }
}

@Composable
private fun TrainingTrends(stats: List<SessionStat>, unit: WeightUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (stats.isEmpty()) {
            Text("Finish some workouts and your training trends will show up here.",
                color = AppMuted, modifier = Modifier.padding(vertical = 8.dp))
            return@Column
        }
        ChartCard("Volume per workout (${unit.label})",
            stats.map { it.finishedAt to unit.fromKg(it.volumeKg).toFloat() })
        ChartCard("Duration per workout (min)",
            stats.map { it.finishedAt to (it.durationMs / 60000f) })
    }
}

@Composable
private fun ChartCard(title: String, points: List<Pair<Long, Float>>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppSurface).padding(16.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        TrendChart(points)
    }
}

/** Minimal line+area trend chart drawn with Canvas (no chart dependency). */
@Composable
private fun TrendChart(points: List<Pair<Long, Float>>) {
    if (points.isEmpty()) return
    val ys = points.map { it.second }
    var minY = ys.minOrNull()!!; var maxY = ys.maxOrNull()!!
    if (minY == maxY) { minY -= 1f; maxY += 1f }
    val ts = points.map { it.first }
    val minT = ts.minOrNull()!!; val maxT = ts.maxOrNull()!!
    Row(Modifier.fillMaxWidth().height(160.dp)) {
        // y-axis labels
        Column(Modifier.width(44.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(maxY.trimAxis(), color = AppMuted, style = MaterialTheme.typography.labelMedium)
            Text(((minY + maxY) / 2f).trimAxis(), color = AppMuted, style = MaterialTheme.typography.labelMedium)
            Text(minY.trimAxis(), color = AppMuted, style = MaterialTheme.typography.labelMedium)
        }
        Canvas(Modifier.weight(1f).fillMaxSize().padding(vertical = 8.dp)) {
            val w = size.width; val h = size.height
            fun px(t: Long) = if (maxT == minT) w / 2f else ((t - minT).toFloat() / (maxT - minT)) * w
            fun py(v: Float) = h - ((v - minY) / (maxY - minY)) * h
            // gridlines
            listOf(0f, 0.5f, 1f).forEach { f ->
                val y = f * h
                drawLine(AppDivider, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            val pts = points.map { Offset(px(it.first), py(it.second)) }
            // area
            if (pts.size >= 2) {
                val area = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h); close()
                }
                drawPath(area, AppGreen.copy(alpha = 0.15f))
                val line = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(line, AppGreen, style = Stroke(width = 4f))
            }
            pts.forEach { drawCircle(AppGreen, radius = 5f, center = it) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 44.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDate(minT), color = AppMuted, style = MaterialTheme.typography.labelMedium)
        if (maxT != minT) Text(formatDate(maxT), color = AppMuted, style = MaterialTheme.typography.labelMedium)
    }
}

private fun Float.trimAxis(): String {
    val r = (this * 10).toInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddMeasurementDialog(unit: WeightUnit, onDismiss: () -> Unit, onAdd: (String, String, Double) -> Unit) {
    var selected by remember { mutableStateOf(BODY_METRICS.first()) }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add measurement") },
        text = {
            Column {
                Text("Metric", color = AppMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                // metric chips
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BODY_METRICS.forEach { m ->
                        val sel = m == selected
                        Box(Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) AppGreen else AppSurfaceHigh)
                            .clickableNoRipple { selected = m }
                            .padding(horizontal = 12.dp, vertical = 7.dp)) {
                            Text(m.name, color = if (sel) Color(0xFF06110B) else AppMuted,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val unitLabel = if (selected.usesWeightUnit) unit.label else selected.unit
                OutlinedTextField(
                    value = value,
                    onValueChange = { new -> if (new.count { it == '.' } <= 1 && new.all { it.isDigit() || it == '.' }) value = new },
                    label = { Text("Value ${if (unitLabel.isBlank()) "" else "($unitLabel)"}") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppGreen, unfocusedBorderColor = AppDivider,
                        focusedLabelColor = AppGreen, cursorColor = AppGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val v = value.toDoubleOrNull()
            TextButton(enabled = v != null, onClick = {
                val unitLabel = if (selected.usesWeightUnit) unit.label else selected.unit
                onAdd(selected.name, unitLabel, v!!)
            }) { Text("Save", color = if (v != null) AppGreen else AppMuted) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
