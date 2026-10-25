# Settings & Release Resilience Audit (Phase 10)

Phase 10 audited the launcher's persistence, lifecycle cleanup, optional
module isolation, manifest surface, and release configuration, then fixed
every gap that could corrupt user choices or crash the launcher. This
document records the confirmed behavior, the exact fixes, and what remains
unverified without hardware.

Implementation touched:
`core/preferences/SettingsRepository.kt`, `SettingsMapping.kt`,
`core/launcher/FavoritesRepository.kt`, `AppRepository.kt`,
`MainViewModel.kt`, `feature/settings/SettingsScreen.kt`, `AppRoot.kt`,
`app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`.
New tests: `app/src/test/java/com/autoverse/launcher/core/preferences/SettingsSchemaTest.kt`

## Confirmed recovery behavior

**Decode contract (settings).** Every stored value is decoded by name with a
per-key fallback: missing key → default, unknown/blank enum name → default,
wrong type → ignored (DataStore throws nothing for unrelated keys). Decoding
never writes storage, so a malformed or unreadable value can not silently
overwrite a valid choice — the file is only rewritten when the user actually
changes something.

**File corruption.** Both DataStores (`autoverse_settings`,
`autoverse_favorites`) now install
`ReplaceFileCorruptionHandler { emptyPreferences() }`, and both read flows
catch `IOException` and emit defaults. A corrupt file degrades to factory
defaults (empty favorites) instead of an unhandled exception in an eagerly
started `stateIn` — the previous crash-loop gap. Recovery is read-side only;
no "repair" write is issued.

**Schema versioning.** A `schema_version` int is stamped into the settings
file. Files without a stamp are schema 1 (the Phases 1–9 layout — every key
was already decoded with a safe default, so no transformation was needed).
`migratePreferences()` is a pure function: it runs registered migrations in
ascending order and stamps `CURRENT_SETTINGS_SCHEMA`. It is applied once at
startup from `MainViewModel.init` and only rewrites the file when an upgrade
is actually due. A file written by a *newer* app version is never stamped
down; unknown future keys are ignored and the file stays usable.

**Reset to defaults (FR-SET-003).** Settings → RESET → "Reset settings to
defaults" opens a confirmation dialog and clears the settings DataStore.
Favorites live in a separate DataStore and are user content, so they are
intentionally kept.

**Favorites self-healing.** The codec trims, drops blanks/duplicates, and
tolerates stray separators and CRLF. `pruneStale` runs only after a
successful app-list refresh, so a transient package-manager failure can not
drop favorites.

## Lifecycle & resource audit

- **Media observation** — `callbackFlow` registers the notification listener
  and session callbacks only while collected; `awaitClose` unregisters both.
  `WhileSubscribed(5_000)` stops observation shortly after the UI leaves.
  `ON_RESUME` → `refreshMediaSession()` re-observes from scratch, which
  covers returning from the notification-listener permission screen and
  session changes caused by other apps. `onSessionDestroyed` unbinds the
  controller. Revoked access surfaces as `AccessRequired`; failures as a
  retryable `Error`.
- **Package-change receiver** — registered `NOT_EXPORTED` with the
  composition and disposed with it. Refreshes keep the previous list on
  failure, so package updates never blank the drawer.
- **Caches** — icon LRU (64 entries) and artwork LRU (8 MB) are evicted via
  a single `ComponentCallbacks2` registered in `MainViewModel.init` and
  unregistered in `onCleared` (`onTrimMemory` / `onLowMemory`).
- **Cancellation hygiene** — fixed `AppRepository.loadIcon` to rethrow
  `CancellationException` (previously swallowed by `runCatching`) and to
  catch `Exception` rather than `Throwable`, so cancellation propagates and
  `Error`s are not masked.
- **Process recreation** — the navigation screen is transient by design: a
  fresh process opens HOME while settings and favorites restore from disk.
  Drawer search text survives via `rememberSaveable`. Device behavior is
  listed under remaining risks.

## Optional-module failure isolation

- The embedded-map prototype was **removed in 0.20.0** (feature files,
  flag, dependency and tests); navigation hand-off never depended on it.
  The cockpit WebView dashboard is the only map surface in the app.
- Media states (`AccessRequired`, `NoSession`, `Connecting`, `Error`) all
  render factual text inside the dashboard card; no media failure can blank
  the dashboard.
- Navigation failures are returned as typed outcomes (`NO_PROVIDER`,
  `NOT_FOUND`, `NO_DESTINATION`, ...) and shown as inline states. A stored
  preferred package that is no longer installed resolves to the system
  default with an explanation in Settings.
- No boot receiver and no persistent background service; the launcher does
  nothing while closed (Phase 10 task 10).

## Manifest & permissions (final merged set)

| Permission | Source | Decision |
|---|---|---|
| `INTERNET` | Declared | Keep — cockpit WebView only (Leaflet tiles, Nominatim geocoding, media artwork URLs); the launcher and hand-off work offline |
| `ACCESS_NETWORK_STATE` | Declared | Keep — feeds the dashboard Wi-Fi indicator |
| `ACCESS_WIFI_STATE` | MapLibre merge | **Gone in 0.20.0** — removed together with the MapLibre dependency |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Declared (cockpit GPS) | Runtime-requested for the cockpit WebView's live position/speed only; never stored or transmitted (0.20.0 state) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Auto-injected (AndroidX) | Kept — signature-level self-permission generated by the toolchain for targetSdk 33+ dynamic receivers |

Package visibility: `<queries>` is scoped to the launcher intent, the `geo:`
scheme, and the two named navigation packages; no `QUERY_ALL_PACKAGES`. The
only exported components are `MainActivity` (launcher/home intent) and the
intentionally empty `AutoVerseMediaListenerService` (platform requirement
for media-session access; guarded by `BIND_NOTIFICATION_LISTENER_SERVICE`).
`allowBackup="true"` remains the default; the stores contain no sensitive
data, so it is left unchanged and noted here.

## Release configuration

- **No logging**: zero `Log.`/`println`/`System.out`/`printStackTrace` calls
  exist in main sources. `BuildConfig` is referenced only for the About
  section's version/build labels.
- **No secrets**: `MapPocConfig` stores no keys; no API keys or tokens exist
  in the codebase; the debug keystore is Android's standard public debug
  keystore (password `android`, not a secret).
- **Minify/R8 is enabled (Phase 13)** — comments-only
  `proguard-rules.pro`; no keep rules were required. Build-time evidence
  and mapping outputs are recorded in `docs/release.md`. The runtime
  behavior of the minified APK remains UNVERIFIED without a device.
- **Release signing is configured (Phase 13)** — dedicated RSA-4096 key
  outside the project (`D:\dev\autoverse-signing\`), credentials in the
  gitignored `keystore.properties`; the signed RC is
  `app\build\outputs\apk\release\app-release.apk` (checksum and
  certificate fingerprints in `docs/release.md`).
- `lintVitalRelease` passes as part of `assembleRelease`.

## Remaining risks / release-readiness

1. **Device verification pending** — no device was attached (`adb devices`
   empty). Unverified: process death and restore, low-memory behavior,
   device reboot, the notification-listener round trip, live media session
   changes, and package install/uninstall while running. All unit-testable
   parts of these paths are covered.
2. **R8 enabled but runtime-unverified** — the minified RC must pass the
   device smoke tests (checklist in `docs/release.md`) before any
   hardware rendering claim is made.
3. **Distribution blocked on device validation** — the RC is signed, but
   no install/upgrade/rollback evidence exists yet.
4. **Map-conditioned permissions** — `INTERNET` +
   `ACCESS_NETWORK_STATE`/`ACCESS_WIFI_STATE` are tied to the map PoC's
   ship decision (Phase 7); removal steps are one manifest edit.
5. **`allowBackup`** — left at the default; revisit if settings ever gain
   sensitive fields.

## Verification evidence (2026-10-17)

- Unit tests: **32 suites / 199 tests / 0 failures** (Phase 9: 31/191;
  +8 cases from `SettingsSchemaTest`).
- Builds: `:app:testDebugUnitTest :app:assembleDebug
  :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL (1m12s);
  `:app:assembleRelease` → BUILD SUCCESSFUL (1m29s incl. `lintVitalRelease`).
- APKs: `app-debug.apk` **69,273,488 B**; `app-release-unsigned.apk`
  **62,110,942 B** (−52 B after the location-permission removal);
  `app-debug-androidTest.apk` 1,029,954 B (unchanged).
- Badging: `versionCode=10`, `versionName=0.10.0-phase10`.

## Phase 13 addendum (2026-10-17)

- Release signing configured; a signed, R8-minified release candidate
  (versionCode 13 / `0.13.0-phase13`) was produced. Artifact path, size,
  SHA-256 and certificate fingerprints are recorded in `docs/release.md`.
- R8 enabled with build-time evidence only (no missing-class diagnostics,
  no `missing_rules.txt`); minified runtime behavior is UNVERIFIED — no
  device or emulator is available.
- Final verification: **32 suites / 204 tests / 0 failures / 0 errors**;
  lint **0 errors / 33 warnings**; `lintVitalRelease` clean on the signed
  minified build.
- Device install, upgrade, media, navigation, map, and rollback tests
  remain outstanding and are tracked in the Phase 13 device checklist in
  `docs/release.md`.
