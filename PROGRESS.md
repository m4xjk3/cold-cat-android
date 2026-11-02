# Cold Cat Android — progress snapshot

**Updated:** 2026-11-02.
**Repo root:** `C:\Users\Maks\Desktop\cold-cat\cold-cat-android`
**GitHub:** <https://github.com/m4xjk3/cold-cat-android>
**Releases:** <https://github.com/m4xjk3/cold-cat-android/releases>

## Current version: 0.1.4 (safe mode + search picker)

Android-only product. Desktop version is a later, separate repository.

### 0.1.4 — safe mode, search engine picker, startup checkpoints

0.1.3's exit report said `REASON_SIGNALED` with no tombstone: the process was
killed by the system / a child-process failure, not a catchable Java error.

- **Safe mode** (`ColdCatApp.decideSafeMode`): a new crash timestamp (from
  `ApplicationExitInfo`, main process) turns on a conservative engine setup:
  `extensionsProcessEnabled(false)`, `appZygoteProcessEnabled(false)`,
  `isolatedProcessEnabled(false)`, ETP `DEFAULT`. Menu item disables it again.
- **Search engine picker** (`SearchEngine`): Яндекс (default for `ru` locale),
  DuckDuckGo, Google, Bing; persisted in `coldcat_prefs`. Home page search form
  posts to `coldcat://search`, handled in `onLoadRequest` with the chosen engine.
- **Startup checkpoints** (`CrashLog.checkpoint`): `app:onCreate`,
  `engine:create …`, `activity:onCreate`, `session:open`, `session:load`,
  `page:start/stop`, `home:search` — written to `files/checkpoints.txt`.
  Shown in the exit report and diagnostics.
- `ExitInfo.Report.status` is now reported (signal number for signal kills).

### 0.1.3 — native crash report

User report from 0.1.2 (Pixel 7 Pro, Android 17/API 37, arm64-v8a): no Java
exception and no startup error, only "previous run ended without a normal
shutdown". That means the process died natively (GeckoView) or was killed.

- `ExitInfo.kt`: reads `ActivityManager.getHistoricalProcessExitReasons()` and
  the system-provided tombstone (`ApplicationExitInfo.traceInputStream`) on
  API 30+; shows the reason (native crash / signal / ANR / low memory) on the
  next launch.
- Diagnostics now lists recent exits of every Cold Cat process (main +
  extensions) with full traces, so a child-process crash is visible too.
- Report dialog: Copy + Share (any app) + Close; on-screen text capped at 6000
  chars, the clipboard/share payload is complete.
- The pre-API-30 "unclean session" guess is no longer shown on API 30+, where
  swiping the app away produced false positives.

### 0.1.2 — diagnostics build (crash investigation)

The 0.1.1 APK is reported by the user as crashing on launch / doing nothing
(extensions and search dead). No device or crash log was available, so 0.1.2
adds on-device diagnostics instead of guessing:

- Menu → **Diagnostics**: app version, device, Android version, ABIs, GeckoView
  version, runtime state, tab count, last error — with a Copy button.
- `CrashLog`: records the last uncaught Java/Kotlin exception to
  `files/last-crash.txt` and shows it on the next launch.
- GeckoView runtime creation is wrapped in `runCatching`; a failure sets
  `ColdCatApp.startupError` and shows a report instead of killing the process.
- Session tracking: if the previous run did not shut down normally (native
  GeckoView crash or process kill), the next launch reports that, which
  distinguishes native crashes from Java ones.
- Add-ons screen and tab creation are guarded when the engine is unavailable.

### 0.1.1 — new transparent logo + Android share target

- Logo replaced with the new transparent-background artwork
  (`C:\Users\Maks\Desktop\cold-cat\logo.png` → `branding/logo-source.png`).
  Regenerated:
  - `branding/logo.png` (README, 1024×1024, transparent);
  - `app/src/main/assets/logo.png` (home page);
  - `mipmap-*/ic_launcher.png` + `ic_launcher_round.png` (legacy, transparent);
  - `mipmap-*/ic_launcher_foreground.png` (adaptive icons + splash, art in 66–72% safe zone);
  - `branding/logo-source.jpg` and unused `branding/logo.jpg` removed.
  `branding/make-icons.ps1` now auto-detects the artwork bounding box from the
  alpha channel (no circular crop, no baked background) and only trims margins.
- Android share sheet: `ACTION_SEND` (text/plain) opens the first `http(s)` link
  from shared text, or searches the text in DuckDuckGo. Unit-tested
  (`UrlUtils.firstHttpUrl`).
- Docs: Android-first positioning; README explains the relation to
  [Waterfox for Android](https://github.com/BrowserWorks/waterfox-android)
  (Fenix/mozilla-central fork) and why Cold Cat uses the same GeckoView engine
  with its own Android UI instead of forking Fenix.

### APK 0.1.4 (signed, V2, `CN=Cold Cat`)

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `ColdCat-0.1.4-arm64-v8a.apk` | 98 963 932 | `0F788217A0BE3059DA109A8274C70C597BDBAF589D2C955AB10FFB91F74D9248` |
| `ColdCat-0.1.4-armeabi-v7a.apk` | 96 187 922 | `F2FAE4230B684A70B5C6C46A4A753FB24F60236BD1D2E579CE563503CF9B2832` |

`versionCode = 5`, `versionName = 0.1.4`. Unit tests: 7 passed.

### APK 0.1.3 (signed, V2, `CN=Cold Cat`)

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `ColdCat-0.1.3-arm64-v8a.apk` | 98 958 228 | `BFBAD8FEF4AD8D247A3DA646ADCA42EFA1620252FE6824EC3B3CAF127423551E` |
| `ColdCat-0.1.3-armeabi-v7a.apk` | 96 182 218 | `075C618249CCDD020E05B333DCA5354A6E6C3334F94778B1ACB6E18C66B1FF2E` |

`versionCode = 4`, `versionName = 0.1.3`. Unit tests: 7 passed.

Published: <https://github.com/m4xjk3/cold-cat-android/releases/tag/v0.1.3>

### APK 0.1.2 (signed, V2, `CN=Cold Cat`)

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `ColdCat-0.1.2-arm64-v8a.apk` | 98 955 284 | `41AEF133F9C8DA8C3EBE2EA5FC2170149B1EB1C0E473F86B4509B6A5F1C5E152` |
| `ColdCat-0.1.2-armeabi-v7a.apk` | 96 179 274 | `B6F0596D2FC7BE119C62C408917E46018F8BF137861D25AFEF4CB01227F84832` |

`versionCode = 3`, `versionName = 0.1.2`, package `com.coldcat.browser`,
minSdk 26, targetSdk 37, ABIs arm64-v8a / armeabi-v7a. Unit tests: 7 passed.

Published: <https://github.com/m4xjk3/cold-cat-android/releases/tag/v0.1.2>

### APK 0.1.1 (signed, V2, `CN=Cold Cat`)

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `ColdCat-0.1.1-arm64-v8a.apk` | 98 951 408 | `053580447DE7B127AB259335E6834A19B2E03588AC9D4C852D12000169528535` |
| `ColdCat-0.1.1-armeabi-v7a.apk` | 96 175 398 | `057E9A8BE4267E347ACA761A94D6BBA5C1DA83A400FB98886B697C2CEA28AF8A` |

`versionCode = 2`, `versionName = 0.1.1`, package `com.coldcat.browser`,
minSdk 26, targetSdk 37, ABIs arm64-v8a / armeabi-v7a.

## Toolchain

| Component | Version |
| --- | --- |
| JDK | Temurin 17.0.20.101 |
| Gradle | 9.6.0 (wrapper) |
| AGP | 9.5.0 (built-in Kotlin 2.2.10) |
| Android SDK | platform 37.2 (`platforms;android-37.2`), build-tools 37.0.0 |
| GeckoView | 159.0.20261022150525 (Mozilla Maven) |

Build:

```powershell
$sdk="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_HOME=$sdk; $env:ANDROID_SDK_ROOT=$sdk
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
cd C:\Users\Maks\Desktop\cold-cat\cold-cat-android
.\gradlew.bat testDebugUnitTest assembleRelease --console=plain --no-daemon
```

## Known workarounds

- `-Xskip-metadata-version-check`: GeckoView 159 is built with Kotlin 2.4, AGP's
  built-in compiler is 2.2.10. Remove when AGP bundles Kotlin 2.4+.
- `useLegacyPackaging = true`: compressed native libs → ~95 MB download instead
  of ~200 MB.
- CI installs `cmdline-tools;latest` first: the runner's preinstalled
  cmdline-tools 16.0 cannot see SDK minor versions; package id must be
  `platforms;android-37.2` (semicolon).

## Blockers / crash investigation

- 0.1.3 report: `REASON_SIGNALED`, status unknown at the time, no tombstone,
  main process `com.coldcat.browser`, Pixel 7 Pro / Android 17 / arm64.
  Interpretation: system kill or a GeckoView child-process failure, not a Java
  exception. 0.1.4 answers it with safe mode + checkpoints.
- Still no local device: `adb devices` empty; emulator unusable (no package,
  VT-x off). USB debugging + `adb logcat -b crash` would give the exact signal
  and stack immediately if the user can connect the phone.
- If safe mode fixes startup, the cause is a GeckoView helper process; then
  re-enable features one by one (extensions process → app-zygote).
- If it still dies, `checkpoints.txt` shows the last step reached.

## Next Android-first steps

1. Bottom address bar (Fenix/Waterfox-style, one-hand use).
2. Private tabs, downloads, history.
3. Pull-to-refresh, swipe between tabs.
4. Settings: search engine, home page, theme.
5. PWA / add to home screen.
6. Fix the reported startup failure once a 0.1.4 report (checkpoints / safe mode) arrives.

## Signing

- Keystore `C:\Users\Maks\.coldcat\coldcat-release.jks`, alias `coldcat`,
  password backup `C:\Users\Maks\.coldcat\README.txt`.
- `keystore.properties` is local and gitignored. **Back up the keystore.**
