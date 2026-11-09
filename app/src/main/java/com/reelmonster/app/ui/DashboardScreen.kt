package com.reelmonster.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.reelmonster.app.data.DashState
import com.reelmonster.app.data.DayStat
import com.reelmonster.app.data.K
import com.reelmonster.app.data.Source
import com.reelmonster.app.data.fmtDuration
import com.reelmonster.app.monster.MonsterView
import com.reelmonster.app.monster.STAGES
import com.reelmonster.app.monster.STAGE_MESSAGES
import com.reelmonster.app.monster.stageIndex
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val dash by vm.dash.collectAsState()
    var showOverlayDialog by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.startMonster() // the monster works even if notifications are denied
    }

    fun toggle(on: Boolean) {
        if (!on) { vm.stopMonster(); return }
        if (!ctx.canOverlay()) { showOverlayDialog = true; return }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.startMonster()
    }

    val d: DashState = dash ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val today = d.today
    val stageIdx = stageIndex(today.total)
    val stage = STAGES[stageIdx]
    var seed by remember { mutableIntStateOf(0) }
    val msg = remember(stageIdx, seed) { STAGE_MESSAGES[stageIdx].random() }
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero
        Card(
            Modifier.fillMaxWidth().clickable { seed++ },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MonsterView(count = today.total, modifier = Modifier.size(150.dp))
                Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stage.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Stage ${stageIdx + 1}/6 · ${stage.range} today", style = MaterialTheme.typography.labelMedium)
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Floating monster control
        SectionCard("Floating monster", container = MaterialTheme.colorScheme.secondaryContainer) {
            SwitchRow(
                "Show floating monster",
                if (s.serviceEnabled) "Running. Tap it to count, long-press for the menu."
                else "Off. Turn on to float above other apps.",
                s.serviceEnabled
            ) { toggle(it) }
            Text("Counting as:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.currentType == Source.REEL,
                    onClick = { vm.edit { it[K.type] = Source.REEL } },
                    label = { Text("Instagram Reel") }
                )
                FilterChip(
                    selected = s.currentType == Source.SHORT,
                    onClick = { vm.edit { it[K.type] = Source.SHORT } },
                    label = { Text("YouTube Short") }
                )
            }
        }

        // Today stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("📸 Reels today", today.reels.toString(), null, Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer)
            StatCard("▶️ Shorts today", today.shorts.toString(), null, Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
        }
        SectionCard("Today in total", container = MaterialTheme.colorScheme.tertiaryContainer) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(today.total.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Text("videos")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmtDuration(d.seconds(today)), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Text("estimated watch time")
                }
            }
            if (s.goal > 0) {
                Text("Daily goal: ${today.total} / ${s.goal}")
                LinearProgressIndicator(
                    progress = { (today.total / s.goal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                )
                if (s.goalWarning && today.total >= s.goal) {
                    Text("You've reached today's goal 🎯 A stretch break could feel great.", fontWeight = FontWeight.Medium)
                }
            }
        }

        // Last 7 days chart
        SectionCard("Last 7 days") {
            WeekChart(d.week)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(MaterialTheme.colorScheme.secondary, "Reels")
                Legend(MaterialTheme.colorScheme.primary, "Shorts")
            }
        }

        // Daily time
        SectionCard("Daily time spent") {
            val maxSec = d.week.maxOf { d.seconds(it) }.coerceAtLeast(1)
            d.week.reversed().forEach { day ->
                val sec = d.seconds(day)
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(day.date.format(dateFmt), style = MaterialTheme.typography.bodyMedium)
                        Text("${fmtDuration(sec)} · ${day.total} videos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    LinearProgressIndicator(
                        progress = { sec / maxSec.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
            Text(
                "Estimates use ${s.reelSec}s per Reel and ${s.shortSec}s per Short (change in Settings).",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Streak, best, worst
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "🔥 Streak", "${d.streak}", if (d.streak == 1) "day in a row" else "days in a row",
                Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer
            )
            StatCard(
                "🏆 Best day", d.best?.total?.toString() ?: "–", d.best?.date?.format(dateFmt),
                Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer
            )
            StatCard(
                "🌱 Lowest day", d.worst?.total?.toString() ?: "–", d.worst?.date?.format(dateFmt),
                Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer
            )
        }
        Text(
            "Best/lowest are taken from days with at least one recorded video.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = { Text("Allow display over other apps") },
            text = {
                Text(
                    "The monster is a small window drawn on top of Instagram and YouTube. Android needs your " +
                        "permission for that. On the next screen, find Reel Monster and switch on " +
                        "\"Allow display over other apps\", then come back and turn the monster on."
                )
            },
            confirmButton = {
                TextButton(onClick = { showOverlayDialog = false; ctx.openOverlaySettings() }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { showOverlayDialog = false }) { Text("Not now") } }
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, sub: String?, modifier: Modifier, container: Color) {
    Card(
        modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WeekChart(week: List<DayStat>) {
    val max = week.maxOf { it.total }.coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(190.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEachIndexed { i, day ->
            val frac by animateFloatAsState(day.total / max.toFloat(), tween(600), label = "bar")
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(day.total.toString(), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(26.dp).height(120.dp * frac).clip(RoundedCornerShape(10.dp))) {
                    Column(Modifier.fillMaxSize()) {
                        if (day.shorts > 0) Box(Modifier.weight(day.shorts.toFloat()).fillMaxWidth().background(MaterialTheme.colorScheme.primary))
                        if (day.reels > 0) Box(Modifier.weight(day.reels.toFloat()).fillMaxWidth().background(MaterialTheme.colorScheme.secondary))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == week.lastIndex) FontWeight.Black else FontWeight.Normal
                )
            }
        }
    }
}
