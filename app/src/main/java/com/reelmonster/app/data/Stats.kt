package com.reelmonster.app.data

import java.time.Instant
import java.time.LocalDate

data class DayStat(val date: LocalDate, val reels: Int, val shorts: Int) {
    val total: Int get() = reels + shorts
}

data class DashState(
    val today: DayStat,
    val week: List<DayStat>,
    val streak: Int,
    val best: DayStat?,
    val worst: DayStat?,
    val settings: AppSettings
) {
    fun seconds(d: DayStat): Int = d.reels * settings.reelSec + d.shorts * settings.shortSec
}

fun buildDash(events: List<ViewEvent>, day: Day, s: AppSettings): DashState {
    val reels = HashMap<LocalDate, Int>()
    val shorts = HashMap<LocalDate, Int>()
    for (e in events) {
        val d = Instant.ofEpochMilli(e.timestamp).atZone(day.zone).toLocalDate()
        if (e.source == Source.REEL) reels[d] = (reels[d] ?: 0) + 1 else shorts[d] = (shorts[d] ?: 0) + 1
    }
    fun stat(d: LocalDate) = DayStat(d, reels[d] ?: 0, shorts[d] ?: 0)

    val week = (6 downTo 0).map { stat(day.date.minusDays(it.toLong())) }
    val withData = (reels.keys + shorts.keys).map { stat(it) }.filter { it.total > 0 }
    val best = withData.maxByOrNull { it.total }
    val worst = withData.minByOrNull { it.total }

    var streak = 0
    var d = if (stat(day.date).total > 0) day.date else day.date.minusDays(1)
    while (stat(d).total > 0) {
        streak++
        d = d.minusDays(1)
    }
    return DashState(stat(day.date), week, streak, best, worst, s)
}

fun fmtDuration(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        sec > 0 -> "${sec}s"
        else -> "0m"
    }
}
