# Reel Monster 👾

A cute floating monster that counts the Instagram Reels and YouTube Shorts you watch, plus a dashboard of daily usage, time spent and history. Works fully offline: no login, no accounts, no analytics, **no INTERNET permission**.

> **Honest status (Phase 1):** the complete source project is written, but it has **not been compiled or run yet**. It was authored in an environment with no Android SDK and no network, so no APK was produced there and nothing has been tested on a device. See `TESTING.md`. The first build may need small compile fixes; the quickest way to find out is Option A below.

## Get the APK

### Option A: GitHub Actions (no Android Studio needed)
1. Create a new GitHub repository and upload everything in this project (keep the `.github` folder).
2. Open the **Actions** tab → **Build APK** → it runs on every push (or press **Run workflow**).
3. When it finishes, open the run and download the artifact **ReelMonster-debug-apk** (a zip containing `ReelMonster.apk`).
4. Share that file. If the build fails, open the failed step and send me the log.

The debug APK is signed with the standard Android debug key. That is fine for personal testing and sharing with friends.

### Option B: Android Studio
1. Install Android Studio (Koala or newer) and open this folder.
2. Let Gradle sync (it downloads the Android SDK 34 pieces and dependencies). If it asks about a Gradle wrapper, accept the default or use Gradle 8.7.
3. **Build → Build APK(s)**. The file is at `app/build/outputs/apk/debug/app-debug.apk`.

### Option C: Command line
With JDK 17, Android SDK (platform 34) and Gradle 8.7 installed:
```
gradle wrapper --gradle-version 8.7     # one time, creates ./gradlew
./gradlew :app:assembleDebug
```

### Release signing (optional)
Release builds fall back to the debug key unless you provide a keystore. For GitHub Actions add these repository secrets: `KEYSTORE_BASE64` (output of `base64 -w0 your.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The workflow then also uploads `ReelMonster-release.apk`. Locally, export `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` and run `assembleRelease`. Never commit a keystore.

## Installing the APK safely
1. Send the APK to the phone, open it, and allow "Install unknown apps" for the app you opened it from.
2. Google Play Protect may warn: tap **More details → Install anyway** (only for APKs from someone you trust).
3. Open Reel Monster and follow the **Help** tab.

## First run
1. **Home → Show floating monster** → allow "Display over other apps" → (Android 13+) allow notifications.
2. Open Instagram or YouTube. The monster floats there.
3. Pick **Instagram Reel** or **YouTube Short** on the Home tab (or from the monster's long-press menu) and **tap the monster** for each video.

## Using the monster
- **Tap:** +1 (animation, haptic, saved to the database).
- **Long-press:** Undo last · Switch type (Reel/Short) · Open dashboard · Hide.
- **Drag:** move it; it snaps to the nearest screen edge.
- **Notification:** Stop, and Hide/Show monster. (Once stopped, start it again from the app or the Home switch.)
- **Evolution:** 6 stages by today's count (0–9, 10–24, 25–49, 50–99, 100–199, 200+). Resets at local midnight; history is kept.

## Level 2: automatic counting (experimental, untested)
Off by default. Turn it on in the **Auto** tab after reading the consent text, then enable the Accessibility service. It counts a scroll in Instagram/YouTube while a view whose ID contains a configured keyword is on screen, with a 2 s debounce and an in-memory same-content check. The default keywords are generic **guesses**, not verified view IDs. Use the debug log and **Snapshot** to see what your installed versions actually expose, then edit the keywords. Do not rely on it until it has been verified on real devices (Phase 2).

## Troubleshooting
- **Monster vanishes after a while:** battery optimisation. See the Help tab and `PERMISSIONS.md` (Xiaomi, Oppo, Vivo, Samsung steps).
- **Can't turn on the overlay switch:** open Settings → Apps → Reel Monster → Display over other apps.
- **Accessibility switch greyed out (Android 13+):** App info → ⋮ → Allow restricted settings, then retry.
- **"Only show in Instagram/YouTube" does nothing:** grant Usage access (Settings tab); without it the monster stays visible everywhere.
- **Auto counting off or wrong:** expected to be fragile; use manual mode and Undo.

## Data & privacy
Room (SQLite) stores timestamp, source and mode per count. Settings live in DataStore. Everything stays on the device, backups are disabled, and CSV export writes only where you choose. Day boundaries use the current local timezone at query time, so timezone and DST changes are handled; the app also re-evaluates "today" on date/time/timezone broadcasts.

## Project layout
```
app/src/main/java/com/reelmonster/app/
  MainActivity.kt            single activity + navigation
  data/                      Room, DataStore, stats, midnight ticker
  monster/                   stages, 42 messages, Canvas drawing
  overlay/                   foreground service, bubble/menu windows, boot receiver
  auto/                      AccessibilityService, debug log
  ui/                        theme, dashboard, settings, auto, help
```
Stack: Kotlin 1.9.24, Compose BOM 2024.06 + Material 3, Room 2.6.1 (KSP), DataStore, AGP 8.5.2, Gradle 8.7, minSdk 26, compileSdk/targetSdk 34.
