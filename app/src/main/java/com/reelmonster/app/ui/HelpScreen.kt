package com.reelmonster.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.reelmonster.app.overlay.hasUsageAccess

@Composable
fun HelpScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val tick = vm.tick
    val overlay = remember(tick) { ctx.canOverlay() }
    val a11y = remember(tick) { ctx.accessibilityEnabled() }
    val usage = remember(tick) { hasUsageAccess(ctx) }
    val notif = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Why we need this", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(
            "Manual counting needs only the overlay permission. Everything else is optional or Level 2 only. " +
                "No internet permission, no accounts, no analytics.",
            style = MaterialTheme.typography.bodyMedium
        )

        InfoCard(
            "Display over other apps (required)",
            "Lets the monster float above Instagram and YouTube. Without it there is no bubble to tap.",
            "Open overlay settings", { ctx.openOverlaySettings() },
            status = if (overlay) "✅ Granted" else "❌ Not granted"
        )
        InfoCard(
            "Notification + foreground service (required to keep running)",
            "Android requires a visible notification for anything that keeps running in the background. " +
                "Ours has Stop and Hide/Show buttons. On Android 13+ you must allow notifications; the monster " +
                "still works if you refuse, but the system may stop it sooner.",
            if (!notif) "Allow notifications" else null,
            if (!notif && Build.VERSION.SDK_INT >= 33) ({ notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) else null,
            status = if (notif) "✅ Allowed" else "❌ Not allowed"
        )
        InfoCard(
            "Start after reboot (automatic)",
            "The boot-completed permission lets the monster come back after a restart or app update if it was running. " +
                "Some phones also need \"Autostart\" enabled (see battery section below)."
        )
        InfoCard(
            "Usage access (optional)",
            "Only used for \"Only show in Instagram and YouTube\", to learn which app is in front. Never reads what you do inside apps.",
            "Open usage access", { ctx.openUsageAccessSettings() },
            status = if (usage) "✅ Granted" else "Not granted (optional)"
        )
        InfoCard(
            "Accessibility (Level 2 only, experimental)",
            "Needed only for automatic counting. Reel Monster watches scroll events inside Instagram and YouTube only, " +
                "and never collects, stores or sends text. Manual mode works without it.",
            "Open accessibility settings", { ctx.openAccessibilitySettings() },
            status = if (a11y) "✅ On" else "Off (default)"
        )
        InfoCard(
            "Battery optimisation and background kills",
            "Phone makers often kill background apps. If the monster disappears, whitelist Reel Monster:\n" +
                "• Any phone: Battery settings → set Reel Monster to Unrestricted / Don't optimise.\n" +
                "• Xiaomi/Redmi/POCO: Settings → Apps → Manage apps → Reel Monster → Autostart ON, Battery saver → No restrictions; lock the app in Recents.\n" +
                "• Oppo/Realme/OnePlus: Battery → App battery management → Reel Monster → Allow background activity; enable Auto-launch.\n" +
                "• Vivo/iQOO: Battery → Background power consumption → Allow high background power; enable Autostart.\n" +
                "• Samsung: Battery → Background usage limits → Never sleeping apps → add Reel Monster.\n" +
                "Menu names vary by Android version and skin.",
            "Open battery settings", { ctx.openBatterySettings() }
        )
        InfoCard(
            "Android 13+ \"Restricted settings\"",
            "For sideloaded apps Android may grey out the Accessibility switch. Fix: Settings → Apps → Reel Monster → " +
                "⋮ (top-right menu) → Allow restricted settings → then enable Accessibility. Wording may vary by device. " +
                "(The ⋮ menu only appears after you've tried to enable it once.)",
            "Open App info", { ctx.openAppInfo() }
        )
        InfoCard(
            "Installing outside the Play Store",
            "Google Play Protect may warn about an unknown app. Only install an APK from someone you trust, tap " +
                "\"More details\" → \"Install anyway\". Google Play also restricts apps that use the Accessibility API " +
                "for non-accessibility purposes, so this app is intended for personal sideloading."
        )
        InfoCard(
            "Updates can break Level 2",
            "Instagram and YouTube change their screens often. Automatic counting is experimental and may stop working " +
                "or miscount after an update. Manual counting is unaffected."
        )
    }
}
