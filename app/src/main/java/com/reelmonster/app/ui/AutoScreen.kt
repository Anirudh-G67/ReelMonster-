package com.reelmonster.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelmonster.app.auto.AutoBridge
import com.reelmonster.app.auto.DebugLog
import com.reelmonster.app.data.DEFAULT_IG_KEYWORDS
import com.reelmonster.app.data.DEFAULT_YT_KEYWORDS
import com.reelmonster.app.data.K
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
fun AutoScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val log by DebugLog.entries.collectAsState()
    val a11yOn = remember(vm.tick) { ctx.accessibilityEnabled() }
    var consent by remember { mutableStateOf(false) }
    var ig by remember(s.igKeywords) { mutableStateOf(s.igKeywords) }
    var yt by remember(s.ytKeywords) { mutableStateOf(s.ytKeywords) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Automatic counting", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

        SectionCard("⚠️ Experimental / Untested", container = MaterialTheme.colorScheme.errorContainer) {
            Text(
                "Instagram and YouTube updates may break detection. This has not been verified on real devices. " +
                    "Manual counting (tap the monster) always works without any of this.",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        SectionCard("Enable") {
            SwitchRow(
                "Count Reels and Shorts automatically",
                if (s.autoEnabled) "On. Counting only inside Instagram and YouTube." else "Off (default).",
                s.autoEnabled
            ) { on ->
                if (on) consent = true else {
                    vm.edit { it[K.autoEnabled] = false }
                    AutoBridge.service?.disableSelf()
                }
            }
            Text(
                "Accessibility service: " + if (a11yOn) "ON" else "OFF",
                fontWeight = FontWeight.Bold,
                color = if (a11yOn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            if (s.autoEnabled && !a11yOn) {
                Text(
                    "Consent saved, but Android's Accessibility service is still off. Open Accessibility settings and " +
                        "enable Reel Monster. If the switch is greyed out on Android 13+: App info → ⋮ menu → " +
                        "Allow restricted settings (wording varies), then try again. See the Help tab.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ctx.openAccessibilitySettings() }) { Text("Accessibility settings") }
                OutlinedButton(onClick = { ctx.openAppInfo() }) { Text("App info") }
            }
        }

        SectionCard("Detection rules") {
            Text(
                "A scroll in the app, while a view whose ID contains one of these keywords is on screen, counts as one " +
                    "new Reel/Short. Keywords are matched against view-ID names only, never text. The defaults are generic " +
                    "starting guesses, not verified IDs. Edit them using the debug log.",
                style = MaterialTheme.typography.bodySmall
            )
            SliderSetting("Debounce window", s.debounceMs.toFloat(), 500f..5000f, 8, { "%.1f s".format(it / 1000f) }) { v ->
                vm.edit { it[K.debounce] = v.roundToInt() }
            }
            OutlinedTextField(
                ig, { ig = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Instagram keywords (comma-separated)") }
            )
            OutlinedTextField(
                yt, { yt = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("YouTube keywords (comma-separated)") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.edit { it[K.igKw] = ig.trim(); it[K.ytKw] = yt.trim() } }) { Text("Save rules") }
                OutlinedButton(onClick = {
                    ig = DEFAULT_IG_KEYWORDS; yt = DEFAULT_YT_KEYWORDS
                    vm.edit { it[K.igKw] = DEFAULT_IG_KEYWORDS; it[K.ytKw] = DEFAULT_YT_KEYWORDS }
                }) { Text("Reset") }
            }
        }

        SectionCard("Debug log") {
            Text(
                "Shows event types, class names and view-ID names, never text content. Kept in memory only and " +
                    "cleared when the app process ends.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val svc = AutoBridge.service
                    if (svc == null) Toast.makeText(ctx, "Accessibility service isn't running", Toast.LENGTH_SHORT).show()
                    else {
                        svc.captureSnapshotDelayed(8000)
                        Toast.makeText(ctx, "Switch to Instagram or YouTube now (8 s)", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Snapshot (8 s)") }
                OutlinedButton(onClick = {
                    val text = log.joinToString("\n") { fmt(it.time, it.pkg, it.type, it.note) }
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Reel Monster debug log", text))
                    Toast.makeText(ctx, "Log copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
                TextButton(onClick = { DebugLog.clear() }) { Text("Clear") }
            }
            Text(
                "Phase 2 tip: swipe a few Reels/Shorts, optionally tap Snapshot (8 s) and switch to the app, then come back, tap Copy and send me the log.",
                style = MaterialTheme.typography.bodySmall
            )
            if (log.isEmpty()) Text("No events yet.", style = MaterialTheme.typography.bodySmall)
            log.takeLast(60).reversed().forEach {
                Text(fmt(it.time, it.pkg, it.type, it.note), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        SectionCard("Limitations") {
            Text(
                "• Detection relies on scroll events plus view-ID keywords, which Instagram/YouTube can rename at any time.\n" +
                    "• Same-content detection uses an in-memory hash and may be unavailable (no text on the matched views).\n" +
                    "• May over- or under-count. Use Undo in the monster menu to correct.\n" +
                    "• Accessibility apps may be blocked from Google Play; this app is meant for sideloading.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (consent) {
        AlertDialog(
            onDismissRequest = { consent = false },
            title = { Text("Turn on experimental auto-counting?") },
            text = {
                Text(
                    "Reel Monster will use Android's Accessibility API only inside Instagram and YouTube to notice when " +
                        "you swipe to a new Reel or Short.\n\nIt does not collect, store or send messages, comments, " +
                        "names or any personal text, and the app has no internet permission, so nothing can leave " +
                        "your phone.\n\nThis is experimental and may miscount. You can turn it off any time here."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    consent = false
                    vm.edit { it[K.autoEnabled] = true }
                    ctx.openAccessibilitySettings()
                }) { Text("I understand, enable") }
            },
            dismissButton = { TextButton(onClick = { consent = false }) { Text("Cancel") } }
        )
    }
}

private fun fmt(time: Long, pkg: String, type: String, note: String): String {
    val t = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(timeFmt)
    return "$t $pkg $type $note"
}
