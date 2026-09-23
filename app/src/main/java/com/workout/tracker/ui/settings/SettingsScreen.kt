package com.workout.tracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.workout.tracker.data.BackupManager
import com.workout.tracker.data.SettingsStore
import com.workout.tracker.data.WeightUnit
import com.workout.tracker.ui.common.PillButton
import com.workout.tracker.ui.common.clickableNoRipple
import com.workout.tracker.ui.common.appViewModel
import com.workout.tracker.ui.theme.AppGreen
import com.workout.tracker.ui.theme.AppMuted
import com.workout.tracker.ui.theme.AppSurface
import com.workout.tracker.ui.theme.AppSurfaceHigh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = com.workout.tracker.ui.common.appContainer()
    val settings = container.settings
    val backup = container.backup
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val unit by settings.unit.collectAsState(initial = WeightUnit.LBS)
    var status by remember { mutableStateOf<String?>(null) }

    val today = java.time.LocalDate.now().toString().replace("-", "")
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = backup.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
            }.onSuccess { status = "Backup saved." }
                .onFailure { status = "Backup failed: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("Could not read file")
                backup.import(json)
            }.onSuccess { status = "Backup restored." }
                .onFailure { status = "Restore failed: ${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppGreen)
            }
            Text("Settings", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Backup
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppSurface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Backup", color = AppGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Save a copy of all your workouts and logged sessions to a file (Drive, Files, email — your choice). Keep it off-device so a lost or reset phone doesn't lose your history.",
                    color = AppMuted, style = MaterialTheme.typography.bodyMedium
                )
                PillButton("Back up my data", onClick = {
                    exportLauncher.launch("workout-backup-$today.json")
                }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(2.dp))
                Text("Restore replaces everything currently in the app with the backup's contents.",
                    color = AppMuted, style = MaterialTheme.typography.labelMedium)
                PillButton("Restore from backup", onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }, filled = false, modifier = Modifier.fillMaxWidth())
                status?.let {
                    Text(it, color = AppGreen, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Units
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppSurface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Units", color = AppGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightUnit.entries.forEach { u ->
                        val sel = u == unit
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(if (sel) AppGreen else AppSurfaceHigh)
                                .clickableNoRipple { scope.launch { settings.setUnit(u) } }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(u.label.uppercase(), color = if (sel) Color(0xFF06110B) else AppMuted,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
