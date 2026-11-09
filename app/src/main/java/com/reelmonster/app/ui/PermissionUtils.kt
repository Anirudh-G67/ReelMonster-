package com.reelmonster.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.reelmonster.app.auto.ReelAccessibilityService

fun Context.canOverlay(): Boolean = Settings.canDrawOverlays(this)

fun Context.accessibilityEnabled(): Boolean {
    val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val me = ComponentName(this, ReelAccessibilityService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
}

private fun Context.launch(i: Intent) {
    runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun Context.openOverlaySettings() =
    launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

fun Context.openAppInfo() =
    launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

fun Context.openAccessibilitySettings() = launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

fun Context.openUsageAccessSettings() = launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

fun Context.openBatterySettings() = launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
