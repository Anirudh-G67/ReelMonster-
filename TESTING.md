# TESTING checklist

Legend: ✅ pass · ❌ fail · ⬜ **UNTESTED**

No Android SDK or device was available when this project was written, so **nothing below has been executed yet**. Rows will be updated as you report results.

## Build and static checks
| Check | Status | Notes |
|---|---|---|
| Project compiles (`assembleDebug`) | ⬜ UNTESTED | First build via GitHub Actions or Android Studio; report any errors |
| Manifest, services, receiver, resources cross-referenced by reading | ✅ done by manual review | Not a substitute for a build |
| Brace/parenthesis balance in all Kotlin files | ✅ | Syntax sanity only |
| APK installs on a device | ⬜ UNTESTED | |
| No INTERNET permission in the final APK | ⬜ UNTESTED | Verify with `aapt dump permissions ReelMonster.apk` |

## Level 1: manual (real device)
| Test | Status |
|---|---|
| Overlay permission flow, then monster appears | ⬜ UNTESTED |
| Notification permission (Android 13+) | ⬜ UNTESTED |
| Monster shows above Instagram | ⬜ UNTESTED |
| Monster shows above YouTube | ⬜ UNTESTED |
| Tap = +1, animation, haptic | ⬜ UNTESTED |
| Long-press menu: Undo / Switch type / Open dashboard / Hide | ⬜ UNTESTED |
| Drag and edge snapping; position kept after restart | ⬜ UNTESTED |
| Notification Stop and Hide/Show actions | ⬜ UNTESTED |
| "Only in Instagram/YouTube" with Usage access | ⬜ UNTESTED |
| Counts survive app close, reboot, app update | ⬜ UNTESTED |
| Service restarts after reboot (and on OEM skins) | ⬜ UNTESTED |
| Evolution through all 6 stages; resets at midnight, history kept | ⬜ UNTESTED |
| Timezone change / DST day | ⬜ UNTESTED |

## Dashboard and settings
| Test | Status |
|---|---|
| Today reels/shorts/total, estimated time | ⬜ UNTESTED |
| 7-day chart, daily time, streak, best/lowest | ⬜ UNTESTED |
| Duration sliders change estimates | ⬜ UNTESTED |
| Size/opacity apply live | ⬜ UNTESTED |
| Goal and gentle warning | ⬜ UNTESTED |
| CSV export opens correctly | ⬜ UNTESTED |
| Clear all data | ⬜ UNTESTED |
| Light and dark mode; small and large screens | ⬜ UNTESTED |

## Level 2: automatic (experimental)
| Test | Status |
|---|---|
| Consent dialog, enable/disable, `disableSelf` | ⬜ UNTESTED |
| Accessibility enable incl. Android 13+ restricted settings | ⬜ UNTESTED |
| Events only from the two target apps | ⬜ UNTESTED |
| Instagram Reel detection with default keywords | ⬜ UNTESTED (defaults are guesses) |
| YouTube Short detection with default keywords | ⬜ UNTESTED (defaults are guesses) |
| 2 s debounce / same-content check | ⬜ UNTESTED |
| Debug log + Snapshot useful for tuning rules | ⬜ UNTESTED |

## Phase 2 input needed
Level 1 results from your phone (model, Android version), and the Auto-tab debug log (**Copy**) after swiping a few Reels/Shorts, ideally with a **Snapshot**.
