package com.reelmonster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

const val PKG_IG = "com.instagram.android"
const val PKG_YT = "com.google.android.youtube"

// Starting heuristics only. They are generic keywords matched against view-ID names (never text) and have NOT
// been verified on real devices. They are user-editable on the Auto screen. See README "Level 2".
const val DEFAULT_IG_KEYWORDS = "clips,reel"
const val DEFAULT_YT_KEYWORDS = "reel,shorts"

data class AppSettings(
    val reelSec: Int = 30,
    val shortSec: Int = 35,
    val sizeDp: Int = 72,
    val opacity: Float = 1f,
    val onlyTargetApps: Boolean = false,
    val goal: Int = 0,
    val goalWarning: Boolean = true,
    val haptics: Boolean = true,
    val serviceEnabled: Boolean = false,
    val currentType: String = Source.REEL,
    val autoEnabled: Boolean = false,
    val debounceMs: Int = 2000,
    val igKeywords: String = DEFAULT_IG_KEYWORDS,
    val ytKeywords: String = DEFAULT_YT_KEYWORDS,
    val posX: Int = -1,
    val posY: Int = -1
)

object K {
    val reelSec = intPreferencesKey("reel_sec")
    val shortSec = intPreferencesKey("short_sec")
    val sizeDp = intPreferencesKey("size_dp")
    val opacity = floatPreferencesKey("opacity")
    val onlyTarget = booleanPreferencesKey("only_target")
    val goal = intPreferencesKey("goal")
    val goalWarning = booleanPreferencesKey("goal_warning")
    val haptics = booleanPreferencesKey("haptics")
    val serviceEnabled = booleanPreferencesKey("service_enabled")
    val type = stringPreferencesKey("current_type")
    val autoEnabled = booleanPreferencesKey("auto_enabled")
    val debounce = intPreferencesKey("debounce_ms")
    val igKw = stringPreferencesKey("ig_keywords")
    val ytKw = stringPreferencesKey("yt_keywords")
    val posX = intPreferencesKey("pos_x")
    val posY = intPreferencesKey("pos_y")
}

fun Context.settingsFlow(): Flow<AppSettings> = applicationContext.dataStore.data.map { p ->
    val d = AppSettings()
    AppSettings(
        reelSec = p[K.reelSec] ?: d.reelSec,
        shortSec = p[K.shortSec] ?: d.shortSec,
        sizeDp = p[K.sizeDp] ?: d.sizeDp,
        opacity = p[K.opacity] ?: d.opacity,
        onlyTargetApps = p[K.onlyTarget] ?: d.onlyTargetApps,
        goal = p[K.goal] ?: d.goal,
        goalWarning = p[K.goalWarning] ?: d.goalWarning,
        haptics = p[K.haptics] ?: d.haptics,
        serviceEnabled = p[K.serviceEnabled] ?: d.serviceEnabled,
        currentType = p[K.type] ?: d.currentType,
        autoEnabled = p[K.autoEnabled] ?: d.autoEnabled,
        debounceMs = p[K.debounce] ?: d.debounceMs,
        igKeywords = p[K.igKw] ?: d.igKeywords,
        ytKeywords = p[K.ytKw] ?: d.ytKeywords,
        posX = p[K.posX] ?: d.posX,
        posY = p[K.posY] ?: d.posY
    )
}

suspend fun Context.editSettings(block: (MutablePreferences) -> Unit) {
    applicationContext.dataStore.edit { block(it) }
}
