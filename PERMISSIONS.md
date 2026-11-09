# Permissions: why Reel Monster asks for each one

Reel Monster does **not** request `INTERNET`. It cannot send data anywhere.

| Permission | Needed for | Required? |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` (Display over other apps) | Drawing the floating monster above Instagram/YouTube (`TYPE_APPLICATION_OVERLAY`) | Yes, for the monster |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Keeping the overlay alive with a persistent notification (Android 14 requires a service type) | Yes |
| `POST_NOTIFICATIONS` (Android 13+) | Showing that notification (Stop / Hide actions). If denied, the service still runs but is easier for the system to stop | Recommended |
| `RECEIVE_BOOT_COMPLETED` | Restarting the monster after reboot or app update if it was on | Optional |
| `VIBRATE` | Haptic feedback on each count | Optional |
| `PACKAGE_USAGE_STATS` (Usage access) | Only for "Only show in Instagram/YouTube": knowing which app is in front | Optional |
| Accessibility service | **Level 2 only**: noticing swipes to new Reels/Shorts, restricted to the two apps | Optional, experimental |

Manual mode works with just the overlay permission (and notifications, ideally).

## Overlay permission
Settings → Apps → Reel Monster → *Display over other apps* → allow. The app's Home switch and Help tab take you there.

## Foreground service and notification
Android requires a notification for long-running background work. On Android 13+ allow notifications when asked, or Settings → Apps → Reel Monster → Notifications.

## Boot completed
Lets the monster return after a restart. Many phones additionally need "Autostart" (see below).

## Accessibility (Level 2 only)
Reel Monster inspects window events **only** from `com.instagram.android` and `com.google.android.youtube` (enforced in the service config), and does nothing until you give consent inside the app. It matches **view-ID names**, never reads messages or personal text for storage, and keeps a transient in-memory hash purely to avoid double counting. The debug log holds event types, class names and view-ID names in memory only.

### Android 13+ "Restricted settings"
Sideloaded apps may have the Accessibility toggle greyed out. Fix:
1. Settings → Apps → Reel Monster (App info).
2. Tap the **⋮** menu (top right) → **Allow restricted settings** (wording varies; the menu appears after you try to enable the service once).
3. Settings → Accessibility → Reel Monster → turn on.

## Battery optimisation and background kills
Menu names vary by Android version and skin.
- **All phones:** Settings → Apps → Reel Monster → Battery → *Unrestricted* / *Don't optimise*.
- **Xiaomi / Redmi / POCO (MIUI, HyperOS):** Apps → Manage apps → Reel Monster → Autostart ON; Battery saver → *No restrictions*; open Recents, long-press the app card and **lock** it.
- **Oppo / Realme / OnePlus (ColorOS, OxygenOS):** Battery → App battery management → Reel Monster → *Allow background activity*; enable *Auto-launch*.
- **Vivo / iQOO (Funtouch, OriginOS):** Battery → Background power consumption → Reel Monster → *Allow high background power*; enable *Autostart*.
- **Samsung (One UI):** Battery → Background usage limits → *Never sleeping apps* → add Reel Monster; also remove it from "Sleeping apps".
- **Others (Huawei, Honor, Nothing, Motorola…):** look for "App launch / Autostart / Protected apps" and set Reel Monster to manage manually or allow background.

## Play Protect and safe sideloading
Play Protect may warn about apps not from the Play Store. Install only APKs you built yourself or got from someone you trust: **More details → Install anyway**. Check the APK came from your own GitHub Actions run if you can.

## Google Play publication limits
Google Play restricts use of the Accessibility API to apps that genuinely help users with disabilities, and requires prominent disclosure. A Reel counter using Accessibility is unlikely to be approved, so plan to distribute by APK. Overlay and foreground-service use also need justification in a Play listing. A Play-friendly build would be manual-only (Level 1) with Level 2 removed.

## Instagram and YouTube updates
Both apps change their layouts and internal IDs without notice. Automatic detection can stop working or miscount after any update. That is why it is experimental, configurable and off by default.
