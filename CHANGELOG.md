# Changelog

## 0.1.4 — safe mode, search engine picker, checkpoints

- **Safe mode**: if the previous run was killed or crashed, Cold Cat starts with a conservative engine setup (no app-zygote, no isolated content process, no separate extension process, standard tracking protection). Leave it via menu → «Disable safe mode».
- **Search engine picker** (menu → «Search engine»): Яндекс, DuckDuckGo, Google, Bing. Default is Яндекс for Russian system locale, DuckDuckGo otherwise. The home page search form now uses the selected engine (`coldcat://search`).
- **Checkpoints**: startup steps are written to `checkpoints.txt` before they run, so even a silent kill shows exactly how far the engine got. Included in the exit report and diagnostics.
- Exit reports now include the raw `status` (signal number for signal kills).

## 0.1.3 — native crash report

- Added `ApplicationExitInfo` integration: the app now reads Android's own process-exit history, including the native tombstone for `REASON_CRASH_NATIVE`, and shows it on the next launch.
- Diagnostics now include the last system exits for every Cold Cat process (main and extensions), with full traces.
- Report dialogs can now be shared to any app, not only copied.
- Fixed a false "unclean shutdown" report when the app was simply swiped away from recents.

## 0.1.2 — diagnostics build

- Added a diagnostics screen (menu → Diagnostics) with device, Android, ABI and GeckoView versions.
- The app now records the last uncaught exception and shows it on the next launch with a copy button.
- Browser engine startup failures are caught and reported instead of killing the process silently.

## 0.1.1 — new transparent logo

- Updated branding with the new logo (transparent background, crisp at any size).
- Re-generated adaptive/legacy launcher icons, splash asset, README logo and the home-page logo.
- Android share sheet: Cold Cat accepts shared text and opens the first link in it.
- Documentation now states the Android-first focus and the relationship to Waterfox for Android.

## 0.1.0 — first Android build

- Cold Cat browser shell on GeckoView (Android 8.0+).
- Tabs, address/search bar, home page, desktop-site switch, share and copy link.
- Firefox WebExtension support: curated AMO catalog, install from `.xpi` URL, remove add-ons.
- Strict Enhanced Tracking Protection by default.
- Cold Cat branding, adaptive icon and splash screen.
- English and Russian UI.

Planned: downloads, history, private tabs, sync, tablet/desktop layout, PC build.
