package com.reelmonster.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelmonster.app.data.K
import com.reelmonster.app.overlay.hasUsageAccess
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    val usageOk = remember(vm.tick) { hasUsageAccess(ctx) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri) { ok ->
            Toast.makeText(ctx, if (ok) "History exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

        SectionCard("Time estimates") {
            SliderSetting("Average Reel length", s.reelSec.toFloat(), 5f..120f, 22, { "${it.roundToInt()} s" }) { v ->
                vm.edit { it[K.reelSec] = v.roundToInt() }
            }
            SliderSetting("Average Short length", s.shortSec.toFloat(), 5f..120f, 22, { "${it.roundToInt()} s" }) { v ->
                vm.edit { it[K.shortSec] = v.roundToInt() }
            }
        }

        SectionCard("Floating monster") {
            SliderSetting("Bubble size", s.sizeDp.toFloat(), 48f..140f, 0, { "${it.roundToInt()} dp" }) { v ->
                vm.edit { it[K.sizeDp] = v.roundToInt() }
            }
            SliderSetting("Bubble opacity", s.opacity, 0.3f..1f, 0, { "${(it * 100).roundToInt()}%" }) { v ->
                vm.edit { it[K.opacity] = v }
            }
            SwitchRow(
                "Only show in Instagram and YouTube",
                "Off = always visible. Needs Usage access to know which app is open.",
                s.onlyTargetApps
            ) { on -> vm.edit { it[K.onlyTarget] = on } }
            if (s.onlyTargetApps && !usageOk) {
                Text(
                    "Usage access isn't granted, so the monster stays visible everywhere until you allow it.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = { ctx.openUsageAccessSettings() }) { Text("Grant usage access") }
            }
            SwitchRow("Haptic feedback", "A small buzz on every count.", s.haptics) { on ->
                vm.edit { it[K.haptics] = on }
            }
        }

        SectionCard("Daily goal") {
            SliderSetting(
                "Daily viewing goal", s.goal.toFloat(), 0f..500f, 49,
                { if (it.roundToInt() == 0) "Off" else "${it.roundToInt()} videos" }
            ) { v -> vm.edit { it[K.goal] = v.roundToInt() } }
            SwitchRow("Gentle warning at goal", "A friendly nudge when you reach it.", s.goalWarning) { on ->
                vm.edit { it[K.goalWarning] = on }
            }
        }

        SectionCard("Your data (stored only on this phone)") {
            Button(onClick = { export.launch("reelmonster_history.csv") }) { Text("Export history as CSV") }
            Button(
                onClick = { confirmClear = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Clear all tracking data") }
            Text(
                "Reel Monster has no internet permission, no accounts and no analytics. Nothing leaves your device.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text("Reel Monster 1.0.0", style = MaterialTheme.typography.labelSmall)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently deletes every recorded Reel and Short. Settings are kept. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
