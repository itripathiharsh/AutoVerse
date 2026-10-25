<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/autoverse-banner.png">
    <source media="(prefers-color-scheme: light)" srcset="app/src/main/res/drawable-nodpi/autoverse_logo.png">
    <img src="docs/assets/autoverse-banner.png" width="420" alt="AutoVerse logo">
  </picture>
</p>

<h1 align="center">AutoVerse</h1>

<p align="center">A premium adaptive launcher and cockpit dashboard for standalone Android head units and phones.</p>

<p align="center">
  <a href="releases"><img src="https://img.shields.io/badge/platform-Android%206.0%2B%20%28API%2023%29-3DDC84?logo=android&logoColor=white" alt="Platform: Android 6.0+ (API 23)"></a>
  <img src="https://img.shields.io/badge/release-0.25.1--alignment-orange" alt="Release 0.25.0-theme-engine">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.0.21">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose BOM 2024.10.01">
  <img src="https://img.shields.io/badge/unit%20tests-417%20passing-brightgreen" alt="417 unit tests passing">
  <img src="https://img.shields.io/badge/themes-12%20%287%20premium%29-8B5CF6" alt="12 themes, 7 premium">
</p>

---

## Overview

AutoVerse replaces the stock home screen with an adaptive, glanceable cockpit: live media, navigation hand-off, apps, engine data, voice, and system status in one driving-safe surface. It is built with Kotlin and Jetpack Compose around a hardware-accelerated WebView cockpit for the dashboard, reshapes itself to the screen it lands on and the performance tier it runs on, and behaves honestly — every surface tells you what is actually happening instead of faking data. The whole thing ships as a **single ~8 MB APK** with no accounts, no backend, and no paid services.

## Features

**Cockpit dashboard**

- Hardware-accelerated dashboard surface: live map, real media-session playback with artwork and working seek, driving-mode workload controls, favourites, and a status bar with Bluetooth / Wi-Fi / island notifications.
- Everything visual is rendered in code — CSS gradients, SVG silhouettes, code-built theme previews — **zero bundled image assets** in the UI.
- **Driving mode** engages automatically from live GPS speed (enter above 8 km/h, exit after 30 s calm): bigger touch targets, decorative animation stops, a Driving indicator. Never blocks function.
- **Performance tiering**: the app's Constrained / Balanced / Enhanced workload profiles re-tier the cockpit live — the Constrained profile drops the GPU-heavy blur and ambient effects, measuring 20–40% faster interactions on weak-hardware simulation.

**Dynamic theme engine**

- **12 themes, 7 premium**: Cobalt (flagship), Obsidian, Neon Pulse, Atelier, Apex, Sakura, Neon Tokyo, Shonen, Gotham, Hot Rod, Fortress, Web Rush — each a complete visual identity (palette pair, cockpit backdrop, icon language), not a color filter.
- **Dynamic icon engine**: the same logical icon renders in the active theme's container language — angular shields, reactor rings, hexagonal neon, sport angular, precision squares — while real installed-app artwork is masked, never replaced. Selected states are structural (edge / ring / indicator), never color-only.
- Themes switch instantly without a restart, persist across boots, and premium themes unlock through a local entitlement (early-access builds include everything free).

**Voice assistant**

- Tap-to-talk through the platform speech recognizer: "open Spotify", "navigate to India Gate", play / pause / next.
- **Offline wake word** (opt-in): say "Hey AutoVerse" with zero connectivity — Vosk recognition runs fully on-device; the ~40 MB voice model downloads once on first enable.

**Live engine data (OBD2)**

- Pair any ELM327 Bluetooth adapter and the cockpit shows real RPM, speed, coolant, and fuel level. Unsupported sensors render an em dash — never a fabricated number.

**Offline maps**

- Map tiles you have viewed are mirrored on-device, so recent areas render without connectivity; a truthful offline banner and a real cache-clear control back it up.

**Navigation**

- Validated destination hand-off to Google Maps, Waze, OsmAnd, or any installed geo app, with strict URI encoding, saved places, recents, and truthful failure messages.

**Launcher**

- Category-classified app drawer (declared categories + intent probes + keyword fallback) with favorites and honest launch-failure reporting. Scoped `<queries>` — no `QUERY_ALL_PACKAGES`.
- First-run onboarding: theme pick, launcher chooser, media access, location, and voice-service checks.
- Diagnostics with live readings and component health; settings with schema validation, reset, and recovery paths.

## Download

Grab the latest signed, R8-minified release APK from the [`releases/`](releases) folder:

**[Download AutoVerse 0.25.1-alignment](https://github.com/itripathiharsh/AutoVerse/raw/main/releases/AutoVerse-0.25.1-alignment-release.apk)** — SHA-256 in [`releases/SHA256SUMS.txt`](releases/SHA256SUMS.txt)

### Install

AutoVerse is a home-screen replacement and registers the `HOME` intent. It needs **Android 6.0 (API 23) or newer**.

1. Copy the APK to the device, or sideload it:
   ```powershell
   adb install AutoVerse-0.25.1-alignment-release.apk
   ```
2. Open **Settings → Apps → Default apps → Home app** and select AutoVerse (or press Home and pick it).
3. The first launch walks you through theme, media-access, location, and voice setup.

## Build from source

**Requirements:** JDK 17, Android SDK with **platform 34** and **build-tools 34.0.0** (`ANDROID_HOME` set), or open the project in Android Studio. The Gradle wrapper (8.9) is committed.

```powershell
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Release builds are signed when a `keystore.properties` file exists at the repo root, or when the `AUTOVERSE_RELEASE_STORE_FILE`, `AUTOVERSE_RELEASE_STORE_PASSWORD`, `AUTOVERSE_RELEASE_KEY_ALIAS`, and `AUTOVERSE_RELEASE_KEY_PASSWORD` environment variables are set (see [`app/build.gradle.kts`](app/build.gradle.kts)). The offline voice model downloads on first use from the Vosk project's own hosting — nothing is bundled or fetched at build time.

## Testing

```powershell
.\gradlew.bat :app:testDebugUnitTest      # 417 JVM unit tests
.\gradlew.bat :app:lintDebug              # Android lint
.\gradlew.bat :app:connectedDebugAndroidTest  # Compose UI tests (needs a device or emulator)
```

## Project structure

```
app/
├─ src/main/java/com/autoverse/launcher/
│  ├─ core/        design tokens + theme/icon engine, app catalog, media, navigation,
│  │               OBD2 (core/obd), offline voice (core/voice), sounds, bridge, diagnostics, preferences
│  └─ feature/     cockpit (WebView dashboard), home, app drawer, navigation, onboarding, settings, diagnostics
├─ src/main/assets/cockpit/   the dashboard UI (HTML/CSS/JS + Leaflet), shipped in the APK
├─ src/test/       417 JVM unit tests
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
| [map-poc.md](docs/map-poc.md) | Former embedded map prototype: evaluation history and 0.20.0 removal record |
| [media-integration.md](docs/media-integration.md) | Media-session binding and now-playing behavior |
| [navigation-handoff.md](docs/navigation-handoff.md) | Destination hand-off architecture and the navigation page |
| [performance-profiles.md](docs/performance-profiles.md) | Constrained / Balanced / Enhanced and their runtime effects |
| [permissions-and-privacy.md](docs/permissions-and-privacy.md) | Full permission inventory and rationale |
| [release.md](docs/release.md) | Signing, versioning, installation, rollback, artifact records |
| [resilience-and-release-audit.md](docs/resilience-and-release-audit.md) | Persistence, lifecycle, and optional-feature audit |
| [safety-accessibility-audit.md](docs/safety-accessibility-audit.md) | Driving-safety and accessibility review |
| [settings.md](docs/settings.md) | Settings page design and preference schema |
| [theme-system.md](docs/theme-system.md) | Dynamic theme system: identities, icon engine, gallery plan |
| [tier2-obd-offline.md](docs/tier2-obd-offline.md) | OBD2 engine data, offline maps, offline wake-word voice |
| [third-party-licenses.md](docs/third-party-licenses.md) | Third-party notices (AndroidX, Kotlin, Leaflet, Vosk) |

## Permissions & privacy

- **Location is cockpit-only.** The cockpit's live GPS (map position, recenter, speed readout) is the only location use; it is runtime-granted, foreground-only, and never stored or transmitted. The navigation hand-off never calls a location API.
- **Microphone is opt-in.** `RECORD_AUDIO` is requested only if you enable offline voice; recognition runs on-device (Vosk) and audio never leaves the phone. Tap-to-talk uses the system recognizer instead — no permission needed.
- **Bluetooth** is used to talk to a bonded ELM327 adapter you explicitly connect (`BLUETOOTH_CONNECT`, requested on Connect; legacy `BLUETOOTH` ≤ Android 11 is install-time). No scanning.
- **No `QUERY_ALL_PACKAGES`.** App visibility is scoped to launcher activities via a minimal `<queries>` block.
- **No telemetry, accounts, or backend.** Favorites, saved places, recents, and settings live on-device in DataStore.
- The media integration binds a notification-listener service *solely* because Android gates media-session access behind that binding — notifications are never read, stored, or forwarded.
- `INTERNET` is used only by the cockpit (map tiles, geocoding, media artwork URLs) and the one-time offline-voice model download. The launcher and navigation hand-off work fully offline.

## Status

Current release: **0.25.1-alignment** — 417 unit tests passing, signed and R8-minified (~8.2 MB universal APK). The cockpit UI is verified across head-unit and phone viewports with CPU-throttled performance profiling; behaviour on real head-unit hardware (wake word, ELM327 session, GPU headroom) is **unverified until a device pass is done**. Known open work: theme gallery screen for the Compose side, wallpaper picker, and accent variants (see [`docs/theme-system.md`](docs/theme-system.md)).

## Contributing

Issues and pull requests are welcome. For larger changes, please open an issue first so the direction can be agreed on.

## License

No license has been chosen for this project yet; until one is added, all rights are reserved.
