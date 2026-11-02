# Cold Cat Android — progress snapshot

**Updated:** 2026-11-02.
**Repo root:** `C:\Users\Maks\Desktop\cold-cat\cold-cat-android`
**GitHub:** <https://github.com/m4xjk3/cold-cat-android>
**Releases:** <https://github.com/m4xjk3/cold-cat-android/releases>

## Current version: 0.1.1 (Android-first)

Android-only product. Desktop version is a later, separate repository.

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

## Next Android-first steps

1. Bottom address bar (Fenix/Waterfox-style, one-hand use).
2. Private tabs, downloads, history.
3. Pull-to-refresh, swipe between tabs.
4. Settings: search engine, home page, theme.
5. PWA / add to home screen.
6. On-device smoke test of 0.1.1 (icons, share sheet, extensions, home page).

## Signing

- Keystore `C:\Users\Maks\.coldcat\coldcat-release.jks`, alias `coldcat`,
  password backup `C:\Users\Maks\.coldcat\README.txt`.
- `keystore.properties` is local and gitignored. **Back up the keystore.**
