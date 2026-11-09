package com.reelmonster.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.reelmonster.app.data.settingsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Restarts the floating monster after reboot / app update if it was enabled before. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val s = app.settingsFlow().first()
                if (s.serviceEnabled && Settings.canDrawOverlays(app)) OverlayService.start(app)
            } finally {
                pending.finish()
            }
        }
    }
}
