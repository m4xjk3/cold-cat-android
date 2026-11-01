# Cold Cat Android — progress snapshot

**Updated:** 2026-11-01, after the first successful signed release build.
**Repo root:** `C:\Users\Maks\Desktop\cold-cat\cold-cat-android`
**GitHub account (from Git Credential Manager):** `m4xjk3`
**Build logs:** `%TEMP%\coldcat-build6.log` (debug), `%TEMP%\coldcat-release4.log` (release)

## Status: BUILD WORKS ✅

- `.\gradlew.bat testDebugUnitTest` — pass.
- `.\gradlew.bat assembleDebug` — pass.
- `.\gradlew.bat assembleRelease` — pass, signed with the project release key.
- Artifacts in `dist\`:
  - `ColdCat-0.1.0-arm64-v8a.apk` (98.9 MB)
  - `ColdCat-0.1.0-armeabi-v7a.apk` (96.1 MB)
- Signature verified with `apksigner verify --print-certs`:
  - V2 signer, DN `CN=Cold Cat, OU=Android, O=Cold Cat, L=Internet, ST=Internet, C=RU`
  - SHA-256 `585b302803df9ca0d027cd92c573e0b72701150bd4a13427085a9e601d848ca7`
  - SHA-1 `80124260da640b1ef08c881c0a1e7411184527f6`
- APK badging: package `com.coldcat.browser`, versionCode 1, versionName 0.1.0,
  minSdk 26, targetSdk 37, label `Cold Cat`.

## Toolchain that actually builds

| Component | Version |
| --- | --- |
| JDK | Temurin 17.0.20.101 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`) |
| Gradle (wrapper) | 9.6.0 |
| Android Gradle Plugin | 9.5.0 |
| Kotlin compiler | AGP built-in (2.2.10) |
| Android SDK platform | `platforms/android-37.2` (API 37, minor 2) |
| Build tools | 37.0.0 |
| GeckoView | `org.mozilla.geckoview:geckoview:159.0.20261022150525` (Mozilla Maven) |

Local env for every build:

```powershell
$sdk="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_HOME=$sdk; $env:ANDROID_SDK_ROOT=$sdk
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
cd C:\Users\Maks\Desktop\cold-cat\cold-cat-android
.\gradlew.bat assembleRelease --console=plain --no-daemon
```

## Build problems solved (chronological)

1. **AGP 8.13 / compileSdk 36 too old.** GeckoView 159 requires
   `compileSdk >= 37.2` and AGP >= 9.1. Upgraded to AGP 9.5.0 + Gradle 9.6.0.
2. **AGP 9 built-in Kotlin vs Kotlin 2.4 metadata.** GeckoView depends on
   `kotlin-stdlib 2.4.20`; AGP 9.5's built-in compiler is Kotlin 2.2.10 and
   rejected the metadata ("compiled with an incompatible version of Kotlin").
   Opting out of built-in Kotlin (`android.builtInKotlin=false`) is not viable
   because it forces `android.newDsl=false`, which in turn makes the whole
   `android {}` DSL deprecated and fails script compilation.
   **Solution:** keep built-in Kotlin and add
   `-Xskip-metadata-version-check` via `kotlin.compilerOptions.freeCompilerArgs`.
3. **`compileSdk = 37` resolved to API 37.0.** GeckoView wants 37.2 →
   `compileSdkMinor = 2` (new AGP DSL property) and
   `sdkmanager "platforms/android-37.2"`.
4. **`minSdk 24` rejected** — GeckoView 159 requires API 26. Raised to 26
   (README/CHANGELOG updated: Android 8.0+).
5. **Kotlin nullability fixes** in `AddonsActivity.kt`: GeckoResult callbacks
   deliver nullable `Throwable?` / `List<WebExtension>?` / `WebExtension?`;
   `EditText.length` → `length()`.
6. **APK was 338 MB universal.** Added ABI splits (arm64-v8a, armeabi-v7a) and
   `useLegacyPackaging = true` to compress GeckoView's native libs:
   arm64 APK 98.9 MB, armv7 APK 96.1 MB (fits GitHub's 100 MB file limit).

## Project contents (all done)

- GeckoView browser: tabs, URL/search bar (DuckDuckGo), home page, back/forward/
  reload/stop, desktop-site toggle, share, copy link, about dialog.
- Firefox extension manager: curated AMO catalog (uBlock Origin, Dark Reader,
  Bitwarden, Privacy Badger, ClearURLs, SponsorBlock), install from `.xpi` URL,
  installed list, uninstall, permission prompts.
- Strict Enhanced Tracking Protection by default.
- English + Russian UI; only these locales shipped (`androidResources.localeFilters`).
- Branding from `C:\Users\Maks\Downloads\logo.jpg`; `branding/make-icons.ps1`
  regenerates all mipmaps and splash assets.
- `README.md` (Russian), `LICENSE` (MPL-2.0), `NOTICE.md`, `CHANGELOG.md`,
  `.gitignore`, `.gitattributes`, `.editorconfig`, `keystore.properties.example`,
  GitHub Actions workflow (`.github/workflows/android.yml`, installs SDK 37.2,
  runs unit tests, builds debug APK).
- Release key **outside the repo**:
  - keystore `C:\Users\Maks\.coldcat\coldcat-release.jks`
  - alias `coldcat`
  - password/backup note `C:\Users\Maks\.coldcat\README.txt`
  - `keystore.properties` in repo root is gitignored.
  - **Back up the keystore and password.** Without them updates cannot be
    installed over an existing Cold Cat install.

## Remaining work

1. ~~Git init + commit~~ / publish (see below — next action).
2. Publish to GitHub as `m4xjk3/cold-cat-android` and create release `v0.1.0`
   with both APKs as assets (APKs stay out of git; `dist/` is gitignored).
3. Smoke test on a real ARM device / emulator:
   - first launch, browsing, tabs, desktop mode;
   - extension install from the catalog and from an `.xpi` URL;
   - home page (`resource://android/assets/home.html` — fallback
     `file:///android_asset/home.html` if blank).
   No x86_64 APK is produced (ABI splits only arm64 + armv7), so the emulator
   would need an ARM image or a temporary x86_64 build.
4. Later: PC version in a separate repository.

## Publish commands (when `gh` is authenticated as m4xjk3)

```powershell
cd C:\Users\Maks\Desktop\cold-cat\cold-cat-android
git init -b main
git add -A
git commit -m "Cold Cat 0.1.0: GeckoView browser with Firefox extensions"
gh repo create m4xjk3/cold-cat-android --public --source . --remote origin --push
gh release create v0.1.0 `
  dist/ColdCat-0.1.0-arm64-v8a.apk `
  dist/ColdCat-0.1.0-armeabi-v7a.apk `
  --title "Cold Cat 0.1.0" --notes-file CHANGELOG.md
```

## Known risks / not yet tested

- No device/emulator run yet.
- GeckoView extension install/AMO redirects and permission prompt need a real
  smoke test.
- `resource://android/assets/home.html` is the intended GeckoView asset URL;
  switch to `file:///android_asset/home.html` if the home page is blank.
- `-Xskip-metadata-version-check` is a workaround for the AGP-bundled Kotlin
  2.2.10 compiler; revisit when AGP ships Kotlin 2.4+ (then remove the flag).
- `useLegacyPackaging = true` compresses native libs: smaller download, slightly
  larger install and slower first start.
