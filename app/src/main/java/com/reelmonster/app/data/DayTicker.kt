package com.reelmonster.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** A local calendar day in a specific zone. Range boundaries are computed per-day, so DST days (23/25h) are correct. */
data class Day(val date: LocalDate, val zone: ZoneId) {
    val start: Long get() = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end: Long get() = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    fun startOf(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        fun now(): Day {
            val z = ZoneId.systemDefault()
            return Day(LocalDate.now(z), z)
        }
    }
}

/**
 * Emits the current [Day] and re-emits at local midnight, and whenever the system date, time or
 * timezone changes (broadcasts). Handles timezone/DST changes because the zone is part of [Day].
 */
fun dayTicker(ctx: Context): Flow<Day> = callbackFlow {
    val trigger = Channel<Unit>(Channel.CONFLATED)
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            trigger.trySend(Unit)
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    ContextCompat.registerReceiver(ctx.applicationContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    val job = launch {
        while (true) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            send(Day(now.toLocalDate(), zone))
            val next = now.toLocalDate().plusDays(1).atStartOfDay(zone)
            val wait = Duration.between(now, next).toMillis().coerceAtLeast(1000L)
            withTimeoutOrNull(wait) { trigger.receive() }
        }
    }
    awaitClose {
        job.cancel()
        ctx.applicationContext.unregisterReceiver(receiver)
    }
}.distinctUntilChanged()
