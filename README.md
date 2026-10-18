<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/autoverse-banner.png">
    <source media="(prefers-color-scheme: light)" srcset="app/src/main/res/drawable-nodpi/autoverse_logo.png">
    <img src="docs/assets/autoverse-banner.png" width="420" alt="AutoVerse logo">
  </picture>
</p>

<h1 align="center">AutoVerse</h1>

<p align="center">A premium adaptive launcher and dashboard for standalone Android head units.</p>

<p align="center">
  <a href="releases"><img src="https://img.shields.io/badge/platform-Android%206.0%2B%20%28API%2023%29-3DDC84?logo=android&logoColor=white" alt="Platform: Android 6.0+ (API 23)"></a>
  <img src="https://img.shields.io/badge/release-0.18.0--phase18-orange" alt="Release 0.18.0-phase18">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.0.21">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose BOM 2024.10.01">
  <img src="https://img.shields.io/badge/unit%20tests-401%20passing-brightgreen" alt="401 unit tests passing">
  <img src="https://img.shields.io/badge/lint-0%20errors-yellowgreen" alt="Lint: 0 errors">
</p>

---

## Overview

AutoVerse replaces the stock home screen on an Android head unit with an adaptive, glanceable dashboard: live media, navigation hand-off, apps, and system status in one driving-safe surface. It is built entirely with Kotlin and Jetpack Compose, reshapes itself to the screen it lands on (and the performance tier it runs on), and behaves honestly — every surface tells you what is actually happening instead of faking data.

## Features

**Adaptive by design**

- `COMPACT` / `MEDIUM` / `WIDE` layout archetypes driven by window size, height, and font scale, with cutout and inset handling.
- Performance profiles (Constrained / Balanced / Enhanced) that change real runtime behaviour — animation budgets, layout density, effect levels — not just cosmetics.
- Five built-in themes on a full design-token system: color, type, spacing, motion, and shape tokens shared by every screen.

**Dashboard**

- Media card bound to the device's real media sessions — artwork, transport controls, progress — and it degrades with honest copy when a source reports nothing.
- Navigation card: a validated destination hand-off to Google Maps, Waze, or any installed geo app, with strict URI encoding and truthful failure messages.
- Clock/status bar with network and Bluetooth indicators.

**Navigation page**

- Map-dominant layout with a collapsible Recents / Saved Places panel; the map surface is never recreated when the panels around it move.
- Local search with real suggestions drawn from your saved places and recent hand-offs — no geocoding calls, no fabricated results.
- Saved places and recents stay on-device, and a hand-off is only recorded once it is confirmed.
- An optional embedded MapLibre map surface exists behind a flag that is **off by default**; see [docs/map-poc.md](docs/map-poc.md) for the evaluation and ship recommendation.

**App drawer**

- Category-classified app list with favorites. Package visibility is scoped to launcher activities through a minimal `<queries>` block — no `QUERY_ALL_PACKAGES`.
- Launch reporting that tells the truth when an app fails to open.

**Diagnostics & settings**

- Diagnostics: live frame-rate, CPU, memory, and storage readings, component health, and a bounded recent-log list.
- Settings: schema-validated, with per-category reset and full recovery paths.

## Download

Grab the latest signed, R8-minified release APK from the [`releases/`](releases) folder:

**[Download AutoVerse 0.18.0-phase18](https://github.com/itripathiharsh/AutoVerse/raw/main/releases/AutoVerse-0.18.0-phase18-release.apk)** — SHA-256 in [`releases/SHA256SUMS.txt`](releases/SHA256SUMS.txt)

### Install

AutoVerse is a home-screen replacement and registers the `HOME` intent. It needs **Android 6.0 (API 23) or newer**.

1. Copy the APK to the device, or sideload it:
   ```powershell
   adb install AutoVerse-0.18.0-phase18-release.apk
   ```
2. Open **Settings → Apps → Default apps → Home app** and select AutoVerse (or press Home and pick it).

## Build from source

**Requirements:** JDK 17, Android SDK with **platform 34** and **build-tools 34.0.0** (`ANDROID_HOME` set), or open the project in Android Studio. The Gradle wrapper (8.9) is committed.

```powershell
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Release builds are signed when a `keystore.properties` file exists at the repo root, or when the `AUTOVERSE_RELEASE_STORE_FILE`, `AUTOVERSE_RELEASE_STORE_PASSWORD`, `AUTOVERSE_RELEASE_KEY_ALIAS`, and `AUTOVERSE_RELEASE_KEY_PASSWORD` environment variables are set (see [`app/build.gradle.kts`](app/build.gradle.kts)).

## Testing

```powershell
.\gradlew.bat :app:testDebugUnitTest      # 401 JVM unit tests
.\gradlew.bat :app:lintDebug              # Android lint
.\gradlew.bat :app:connectedDebugAndroidTest  # Compose UI tests (needs a device or emulator)
```

## Project structure

```
app/
├─ src/main/java/com/autoverse/launcher/
│  ├─ core/        design tokens, app catalog, media, navigation, performance, diagnostics, preferences
│  └─ feature/     home dashboard, app drawer, navigation, map prototype, settings, diagnostics
├─ src/test/       401 JVM unit tests
└─ src/androidTest/ Compose UI tests (device required)
docs/              engineering documentation (see below)
releases/          verified release artifacts (APK + SHA-256)
```

## Documentation

| Doc | Contents |
| --- | --- |
| [adaptive-layout.md](docs/adaptive-layout.md) | Responsive layout policy: width buckets, performance tiers, cutouts |
| [design-system.md](docs/design-system.md) | Visual language: tokens for color, type, spacing, motion, shapes |
| [diagnostics.md](docs/diagnostics.md) | Diagnostics page design and live readings |
| [launcher-setup.md](docs/launcher-setup.md) | App discovery, launch, favorites, drawer behavior |
| [map-poc.md](docs/map-poc.md) | Embedded MapLibre map prototype: evaluation and recommendation |
| [media-integration.md](docs/media-integration.md) | Media-session binding and now-playing behavior |
| [navigation-handoff.md](docs/navigation-handoff.md) | Destination hand-off architecture and the navigation page |
| [performance-profiles.md](docs/performance-profiles.md) | Constrained / Balanced / Enhanced and their runtime effects |
| [permissions-and-privacy.md](docs/permissions-and-privacy.md) | Full permission inventory and rationale |
| [release.md](docs/release.md) | Signing, versioning, installation, rollback, artifact records |
| [resilience-and-release-audit.md](docs/resilience-and-release-audit.md) | Persistence, lifecycle, and optional-feature audit |
| [safety-accessibility-audit.md](docs/safety-accessibility-audit.md) | Driving-safety and accessibility review |
| [settings.md](docs/settings.md) | Settings page design and preference schema |
| [third-party-licenses.md](docs/third-party-licenses.md) | Third-party notices (MapLibre, AndroidX, Kotlin) |

## Permissions & privacy

- **No location access.** MapLibre's merged location permissions are explicitly removed at manifest-merge time, and nothing in the app calls a location API.
- **No `QUERY_ALL_PACKAGES`.** App visibility is scoped to launcher activities via a minimal `<queries>` block.
- **No telemetry, accounts, or backend.** Favorites, saved places, recents, and settings live on-device in DataStore.
- The media integration binds a notification-listener service *solely* because Android gates media-session access behind that binding — notifications are never read, stored, or forwarded.
- `INTERNET` is declared only for the optional embedded-map prototype (off by default). The launcher and navigation hand-off work fully offline.

## Status

Current release: **0.18.0-phase18** (release candidate) — 401 unit tests passing, 0 lint errors (33 documented dependency-version warnings), signed and R8-minified APK in [`releases/`](releases).

Behaviour on real hardware has not been validated yet — treat on-device behaviour as unverified until a device pass is done. Planned work includes device validation, screenshots, and layout polish for real head-unit screens.

## Contributing

Issues and pull requests are welcome. For larger changes, please open an issue first so the direction can be agreed on.

## License

No license has been chosen for this project yet; until one is added, all rights are reserved.
