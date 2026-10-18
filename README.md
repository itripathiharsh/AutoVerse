# AutoVerse

Premium adaptive launcher / dashboard for standalone Android automotive head units.

Current state: Phase 18 (navigation page) — a dedicated navigation page
with a map-dominant layout: real local search over saved places and recent
handoffs (genuine suggestions only, no geocoding), a collapsible
Recents + Saved Places panel that reflows the map without ever recreating
it, and honest handoff surfaces: Google Maps is the primary launch target,
the embedded MapLibre map stays off by default, and failures surface the
existing honest copy. Saved places and recents are local-only (DataStore),
recorded recents only after a confirmed handoff. A signed, R8-minified
release candidate (versionCode 18 / `0.18.0-phase18`) exists with recorded
SHA-256 and certificate fingerprints; features: five themes, adaptive
dashboard archetypes, app drawer with favorites, media-session integration,
navigation handoff + navigation page, an off-by-default MapLibre map
prototype, settings with reset/recovery, performance profiles, and the
diagnostics surface. Device tests are pending (no hardware available) and
explicitly UNVERIFIED. See `docs/release.md`, `docs/navigation-handoff.md`
and `CONTEXT.md` for details.

## Toolchain (kept on the D: drive)

- JDK 17 (Temurin): `D:\dev\jdk`
- Gradle 8.9: `D:\dev\gradle\gradle-8.9`
- Android SDK: `D:\dev\android-sdk` (platform-tools, platforms;android-34, build-tools;34.0.0)
- Gradle caches: `D:\dev\cache\gradle` (set `GRADLE_USER_HOME`)

## Build

```powershell
$env:JAVA_HOME = "D:\dev\jdk"
$env:ANDROID_HOME = "D:\dev\android-sdk"
$env:GRADLE_USER_HOME = "D:\dev\cache\gradle"
.\gradlew.bat :app:assembleDebug
```

APK output: `app\build\outputs\apk\debug\app-debug.apk`

## Test

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Status

Phase 18 release candidate — versionCode 18, `0.18.0-phase18`; 401 unit
tests passing (73 added in Phase 18), lint 0 errors / 33 warnings (all
dependency-version suggestions with documented hold decisions — see
`docs/release.md`); debug and androidTest builds green. The release APK
is signed with a dedicated release key (outside source control) and
R8-minified; artifact path, size, SHA-256 and certificate fingerprints
are recorded in `docs/release.md`. Device behaviour — including all
minified-runtime smoke tests — is untested (no hardware available) and
explicitly UNVERIFIED; do not distribute without device validation and
explicit authorization. See `plan of work\PHASES.md`, `CONTEXT.md` and
`docs/` for specifics.
