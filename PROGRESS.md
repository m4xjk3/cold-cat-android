# Cold Cat Android — progress snapshot

**Updated:** 2026-11-02.
**Repo root:** `C:\Users\Maks\Desktop\cold-cat\cold-cat-android`
**GitHub:** <https://github.com/m4xjk3/cold-cat-android>
**Releases:** <https://github.com/m4xjk3/cold-cat-android/releases>

## Current version: 0.1.2 (diagnostics build)

Android-only product. Desktop version is a later, separate repository.

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

- No Android device is attached (`adb devices` empty) and the emulator is not
  usable on this machine (no `emulator` package, VT-x disabled:
  `VirtualizationFirmwareEnabled = False`), so crashes cannot be reproduced
  locally.
- 0.1.2 exists to obtain a real report from the user's phone. Awaiting:
  diagnostics text, phone model, Android version, installed ABI.
- Candidate causes if it is a native crash on `GeckoRuntime.create`:
  1. Kotlin 2.4 metadata read by the 2.2.10 compiler
     (`-Xskip-metadata-version-check`) producing wrong runtime calls → move to
     AGP 8.13 + KGP 2.4.20 (compileSdk 37.2) or pin a GeckoView built with
     Kotlin ≤ 2.2.
  2. Device-specific GeckoView failure (OS/ABI/libxul); check report ABI and
     Android version.
  3. Extract/loading of compressed native libs on the device — fallback to
     `useLegacyPackaging = false`.

## Next Android-first steps

1. Bottom address bar (Fenix/Waterfox-style, one-hand use).
2. Private tabs, downloads, history.
3. Pull-to-refresh, swipe between tabs.
4. Settings: search engine, home page, theme.
5. PWA / add to home screen.
6. Fix the reported startup failure once the 0.1.2 diagnostics report arrives.

## Signing

- Keystore `C:\Users\Maks\.coldcat\coldcat-release.jks`, alias `coldcat`,
  password backup `C:\Users\Maks\.coldcat\README.txt`.
- `keystore.properties` is local and gitignored. **Back up the keystore.**
