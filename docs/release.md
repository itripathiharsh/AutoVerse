# Release: Signing, Versioning, Installation & Rollback (Phase 13)

Phase 13 (Packaging, Signing and Release) configured secure release
signing, enabled R8 minification on the strength of the Phase 12 audit,
built a signed release candidate (RC), and re-ran the full automated
verification against it. This document records the signing setup (no
secret values), the exact build/test commands and results, the RC
artifact identity, size, SHA-256 and certificate fingerprints, the R8
status with its verified/unverified boundary, the device-test checklist
(UNVERIFIED — no hardware available), and the release verdict.

## Versioning

| Setting | Value (Phase 13 RC) |
|---|---|
| `applicationId` / namespace | `com.autoverse.launcher` |
| `versionCode` | `13` |
| `versionName` | `0.13.0-phase13` |
| `minSdk` | 23 |
| `targetSdk` / `compileSdk` | 34 |

Scheme: `versionCode` increments once per completed phase; `versionName`
is `major.minor.patch-phaseNN`. Upgrades rely on a strictly increasing
`versionCode` **and** the same application ID + signing certificate. The
app is pre-1.0 with no released baseline, so no upgrade-migration
obligations exist yet; DataStore schema versioning and migration handling
are already implemented (Phase 10).

## Reproducing the Phase 13 RC build

Toolchain: JDK 17 (`D:\dev\jdk`), Gradle 8.9 (`D:\dev\gradle\gradle-8.9`),
Android Gradle Plugin 8.5.2, Kotlin 2.0.21, Android SDK platform 34 /
build-tools 34.0.0. All run from the workspace root. Release builds need
the signing configuration (see "Signing"); without it Gradle falls back
to the debug key and prints a warning.

```powershell
$env:JAVA_HOME = "D:\dev\jdk"
$env:ANDROID_HOME = "D:\dev\android-sdk"
$env:ANDROID_USER_HOME = "D:\dev\.android"
$env:GRADLE_USER_HOME = "D:\dev\cache\gradle"
$env:TEMP = "D:\dev\Temp\opencode"; $env:TMP = "D:\dev\Temp\opencode"
& "D:\dev\gradle\gradle-8.9\bin\gradle.bat" --no-daemon --console=plain `
  clean :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest `
  :app:assembleRelease :app:lintDebug
```

Byte-for-byte reproducibility of APKs across machines is not claimed.
Note: `META-INF/version-control-info.textproto` in the RC records
`generate_error_reason: NO_SUPPORTED_VCS_FOUND` — this project has no
VCS (no git repository), so the RC is identified by its
`versionCode` / `versionName`, the source state at build time, and the
SHA-256 below.

## Build outputs and Phase 13 RC checksums

Clean build 2026-10-17 from the final Phase 13 sources with the command
above (BUILD SUCCESSFUL in 5m 9s):

| Artifact | Size (bytes) | SHA-256 | Notes |
|---|---|---|---|
| `app\build\outputs\apk\release\app-release.apk` | 51,828,237 | `11788DCE892BC02CB25D529252B41C1AAFE2B7DC922CCB052AF7EA5A7CF76E3E` | **signed RC**: release key + R8-minified |
| `app\build\outputs\apk\debug\app-debug.apk` | 69,274,087 | `51302F36D8A030DBC1B588840791B77CF6E19ECEDD12558BB4E86A7AF3ED43FC` | debug-signed, testing only |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,029,978 | `63D35D3D23E392E951C95669A561BBF61EF288D83C7AC1BA81653F2EC01B7C24` | not distributable |

No `app-release-unsigned.apk` is produced anymore: configuring the
release signing config changes the output to the signed
`app-release.apk`.

### RC signing identity (recorded with the artifact)

| Property | Value |
|---|---|
| Certificate DN | `CN=AutoVerse Release, OU=Android Launcher, O=AutoVerse, C=US` |
| Certificate SHA-256 | `29:C3:76:1C:48:D8:12:61:BD:89:CA:DF:AE:80:F4:ED:52:67:57:A8:4B:8A:1B:37:14:CD:68:8C:EE:A6:1B:86` |
| Certificate SHA-1 | `94:5C:CB:F9:AC:37:99:91:AA:85:96:A5:9E:C8:57:31:5E:C1:1F:4A` |
| Key | RSA 4096, SHA256withRSA, self-signed, valid 2026-10-17 → 2054-03-04 |
| Schemes verified | v1 (JAR) **true**, v2 (APK Signature Scheme v2) **true** (v3/v4 not used at minSdk 23) |

Inspect the signature yourself:

```powershell
$env:JAVA_HOME = "D:\dev\jdk"
& "D:\dev\android-sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs `
  "app\build\outputs\apk\release\app-release.apk"
```

`apksigner` also prints informational warnings that `META-INF/*.version`
metadata entries are not covered by the **v1** signature. This is
standard for AGP-produced APKs: the v2 scheme covers the whole APK on
Android 7+, and the metadata files carry no executable content on any
API level.

## Signing (Phase 13)

- **Separate identities**: debug builds keep using the conventional
  repo-local `keystore/debug.keystore` (public test key, never for
  distribution). Release builds use a dedicated RSA-4096 release key.
- **Key location**: `D:\dev\autoverse-signing\autoverse-release.jks`
  (PKCS12) — **outside the project folder and outside any future VCS
  working tree**.
- **Credentials are never committed**: `app/build.gradle.kts` reads
  `keystore.properties` from the project root. The file is listed in
  `.gitignore` (together with `*.jks` and `*.keystore`) and contains the
  keystore path, alias and passwords; its values are not reproduced in
  any document or build output.
- **Environment-variable alternative** (used when no properties file
  exists): `AUTOVERSE_RELEASE_STORE_FILE`,
  `AUTOVERSE_RELEASE_STORE_PASSWORD`, `AUTOVERSE_RELEASE_KEY_ALIAS`,
  `AUTOVERSE_RELEASE_KEY_PASSWORD`. Only the variable names are
  documented here, never values.
- **No-secret fallback**: if neither source is present,
  `assembleRelease` signs with the debug key and prints
  `AUTOVERSE: release signing is not configured ... Do not distribute.`
  so local builds keep working without risking a silent release-key
  absence.
- **Key generation** (already executed): `keytool -genkeypair
  -storetype PKCS12 -keystore autoverse-release.jks -alias
  autoverse-release -keyalg RSA -keysize 4096 -sigalg SHA256withRSA
  -validity 10000` with a random 32-character password; the password is
  stored only in `keystore.properties`.
- **Backup and recovery (open action — not yet executed)**: copy
  `D:\dev\autoverse-signing\autoverse-release.jks` **and**
  `keystore.properties` to at least two secure, offline/encrypted
  locations (e.g. an encrypted archive in a password manager plus an
  offline drive). Never place them in cloud repositories or anywhere the
  project itself is copied. **If this key is lost, the installed app can
  never be updated in place** — Android only accepts updates signed with
  the same certificate, so a replacement key would require a different
  application ID, or uninstalling and reinstalling the app on every
  unit (losing its settings).

## R8 minification (Phase 13 — enabled)

`isMinifyEnabled = true` for release with
`proguard-android-optimize.txt` + `app/proguard-rules.pro`
(comments-only). Evidence on the final RC build (2026-10-17):

- `:app:minifyReleaseWithR8` BUILD SUCCESSFUL; `lintVitalRelease` clean;
  **no R8 missing-class diagnostics and no `missing_rules.txt`**.
- Consumer rules merged (verified in
  `app\build\outputs\mapping\release\configuration.txt`, 26,441 B):
  MapLibre `android-sdk` 11.13.5 keeps (Gson reflection,
  `org.maplibre.geojson`, native entry points, enum `value`/`valueOf`,
  dontwarn/dontnote) plus OkHttp/Gson/Compose consumer rules.
- Mapping outputs retained for crash deobfuscation:
  `mapping.txt` 19,883,052 B, `seeds.txt` 357,438 B,
  `usage.txt` 4,284,893 B (`app\build\outputs\mapping\release\`).
- Size: signed minified RC **51,828,237 B** vs the unminified Phase 12
  artifact (62,118,062 B) — approximately **−16.6 %**;
  `libmaplibre.so` is native and untouched by R8. The Phase 12
  experiment produced 51,795,751 B unsigned; the RC adds the APK
  signature blocks and signing metadata.
- Keep rules: **none added**. The app has no reflection/JNI and every
  reflection-heavy dependency ships its own consumer rules; broad keeps
  that would blunt shrinking are deliberately avoided.
- **R8 runtime status: UNVERIFIED.** No device or emulator exists in
  this environment (`adb devices` empty; no emulator installed), so no
  automated test executes the minified artifact — the 204 JVM tests run
  unminified classes and the instrumented suite requires a device. R8 is
  therefore **enabled with build-time evidence only**; the minified RC
  must pass the device smoke tests below before it is described as
  validated.

## Automated verification (Phase 13 RC, final source)

- **Unit tests**: 32 suites / **204 tests / 0 failures / 0 errors / 0
  skipped**.
- **Lint**: **0 errors / 33 warnings** — all dependency-version
  suggestions with documented hold decisions (Phase 12 audit table;
  unchanged, intentionally still visible).
- **Release build**: signed + minified APK produced;
  `lintVitalRelease` clean; `validateSigningRelease` green.

## Release artifact audit (Phase 13, on the signed minified RC)

Audited with `apkanalyzer` directly against the signed `app-release.apk`
(2026-10-17):

- **Identity**: package `com.autoverse.launcher`, versionCode `13`,
  versionName `0.13.0-phase13`, minSdk 23, targetSdk 34; no
  `android:debuggable`.
- **Permissions** (exactly): `INTERNET`, `ACCESS_NETWORK_STATE`,
  `ACCESS_WIFI_STATE`, plus the AndroidX auto-added
  `com.autoverse.launcher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
  **No location permissions**; `uses-feature wifi required="false"`.
- **Components**: `MainActivity` (exported,
  MAIN/LAUNCHER/HOME/DEFAULT), `AutoVerseMediaListenerService`
  (exported, gated by `BIND_NOTIFICATION_LISTENER_SERVICE`),
  `androidx.startup.InitializationProvider` (exported=false),
  `androidx.profileinstaller.ProfileInstallReceiver` (standard library
  component, `DUMP`-permission gated). No test/debug components, no boot
  receivers, no background services.
- **Dex**: the minified release dex contains **no debug-only
  `ui-tooling` classes** and no instrumented-test code (only standard
  release artifacts such as `androidx.compose.ui.platform.TestTagElement`
  from the Compose runtime).
- **Native libraries**: 4 ABIs (`arm64-v8a`, `armeabi-v7a`, `x86`,
  `x86_64`) × {`libmaplibre.so`, `libdatastore_shared_counter.so`,
  `libandroidx.graphics.path.so`}.
- **Embedded map**: still **off by default**
  (`embeddedMapEnabled = false`,
  `app\src\main\java\com\autoverse\launcher\core\model\AppSettings.kt:32`),
  so a default install makes no map/tile requests and scatters no
  location data. MapLibre's built-in attribution UI becomes relevant only
  when the map is enabled (device checklist).
- **Licenses**: the packaged dependency set is unchanged since Phase 11;
  the `docs/third-party-licenses.md` table and obligations still match
  the packaged artifacts. The consolidated notices file and any
  attribution display for the embedded map remain **deferred until
  external distribution is authorized** (see checklist).

## Compatibility and recovery

The Phase 12 review stands; the RC adds no API-conditional code.

- No `VERSION_CODES` branches / `@RequiresApi` in app code (the only
  `Build.VERSION` use records `RELEASE`/`SDK_INT` in `DeviceProfile`
  telemetry).
- Core library desugaring enabled for API 23+.
- Automated coverage: DataStore schema migration (Phase 10),
  appearance-mode decoding (Phase 12), settings reset path.
- Hardware-only items remain unverified — see the device checklist.

## Installation (UNVERIFIED — no device available)

Precondition: the target head unit must allow a third-party launcher to
be selected as the default Home app (Phase 0 check; not yet verified).

- Signed RC install (device attached, USB debugging enabled):

  ```powershell
  & "D:\dev\android-sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\release\app-release.apk"
  ```

  (`-r` is for upgrade installs; downgrades need `-d` and are not an
  expected flow.)
- Selecting AutoVerse as Home: Android Settings → Apps → Default apps →
  Home app (exact vendor path varies by head unit).
- The embedded map stays off until explicitly enabled in Settings.

All of the above is **UNVERIFIED** — no Android device or emulator has
been available (Phases 11–13).

## Rollback and recovery

- Switch back to the factory launcher at any time via Settings → Apps →
  Default apps → Home app. AutoVerse adds no boot receivers and runs no
  persistent background services, so switching the Home app away fully
  reverts it.
- Uninstall: `adb uninstall com.autoverse.launcher` or Settings → Apps →
  Uninstall. Uninstalling removes DataStore preferences; the in-app
  reset-to-defaults path (keeps favorites; confirmation dialog) is
  available without uninstalling.
- If any state appears stuck, resetting settings recovers safely without
  reinstall (Phase 10 behavior, unit-tested).

Device-level rollback/uninstall flows are **UNVERIFIED** (no device).

## Device test checklist (Phase 13 — all UNVERIFIED)

No device or emulator is available (`adb devices` empty; no emulator
installed). Every item below is **UNVERIFIED**:

1. Fresh install of `app-release.apk`; launch; select AutoVerse as Home;
   confirm the choice persists across reboot.
2. First-run UI: themes, dashboard archetypes, app drawer, favorites.
3. Settings persistence across relaunch and reboot; reset-to-defaults.
4. Upgrade behavior: the previous distributable was *unsigned*, so the
   signed RC is the first installable release — a **debug→release
   upgrade on the same device fails by design** (same package ID,
   different signing certificate: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`)
   and requires uninstall first. Future release→release upgrades must use
   the same package ID + certificate with a monotonically increasing
   `versionCode`; verify one such upgrade (and DataStore preservation)
   on hardware once a second signed build exists.
5. Permissions: enable the notification listener for media cards;
   confirm the service binds and the app requests nothing unavailable.
6. Media controls: play/pause/skip against real media apps via the
   media card.
7. Navigation hand-off: `geo:` intents reach installed maps apps.
8. Embedded map ON: MapLibre renders, attribution UI visible; OFF: no
   map/tile requests.
9. Rollback: switch Home back to the factory launcher; uninstall;
   reinstall.
10. Minified-RC smoke: run items 1–9 on the R8 build — the specific
    purpose of enabling R8 in Phase 13.

## Release verdict (Phase 13)

**Signed release candidate built and verified at build time; ready for
controlled device testing — not for production or distribution.** The
signed, minified RC passes every automated check (204 unit tests, lint
0 errors / 33 warnings, `lintVitalRelease`, R8 build) and its identity,
checksum and certificate fingerprints are recorded above. All
device-dependent behavior — including every minified-runtime smoke test
— remains UNVERIFIED pending hardware; no production-readiness claim is
made, and no external distribution should occur without explicit
authorization (device matrix, consolidated notices, and signing-key
backup are still open).

## Remaining checklist

- [x] Secure release signing configured; key outside source control;
      credentials never committed; debug/release identities separate.
- [x] R8 enabled after the Phase 12 audit; build/mapping/APK evidence
      recorded; runtime explicitly marked UNVERIFIED.
- [x] RC verified: identity, permissions, components, debug-code
      exclusion, map-off default, license table.
- [x] RC SHA-256 + certificate fingerprints + artifact path/size
      recorded (above).
- [ ] Device and upgrade tests (checklist above) — no hardware.
- [ ] Back up the signing key + `keystore.properties` to two secure
      offline locations (procedure above).
- [ ] Bundle the consolidated open-source notices and finalize the
      supported-device matrix before any distribution.
- [ ] Keep `mapping.txt` for the distributed build (crash
      deobfuscation).

## Phase 14 refresh (2026-10-18)

Phase 14 (home-dashboard redesign around the brand mark, the Cobalt
flagship theme as the new default, real connectivity indicators, and the
drive-mode selector) rebuilt the release candidate on the same signing
identity. Every procedure and caveat above still applies verbatim; this
section only refreshes the recorded artifact.

| Property | Value |
|---|---|
| `versionCode` | 14 |
| `versionName` | `0.14.0-phase14` |
| Release APK | `app\build\outputs\apk\release\app-release.apk` |
| Size (bytes) | 51,999,475 |
| SHA-256 | `E8F8CF21F7AEDAB1FA2A47C90CD7BB46B24D817C2E9A2BC845E95A4E6670E9FC` |
| Certificate SHA-256 | unchanged from Phase 13: `29:C3:76:1C:48:D8:12:61:BD:89:CA:DF:AE:80:F4:ED:52:67:57:A8:4B:8A:1B:37:14:CD:68:8C:EE:A6:1B:86` |
| Signature schemes | v1 (JAR) true, v2 true |

Companion artifacts from the same clean build (not distributable):

| Artifact | Size (bytes) | SHA-256 |
|---|---|---|
| `app\build\outputs\apk\debug\app-debug.apk` | 69,588,892 | `92B89955E4EBE2A4BC2B5B5263319F892F93BB21F7EF7E5BDB74BFFEE25D27A6` |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,030,009 | `6FBEAA15C2D700188FEC813E6C6BA1455144BD2BE4584ED2EF1473E921D9F2ED` |

Automated state at build time (clean build 2026-10-18): 210 unit tests,
0 failures/errors; lint 0 errors / 33 warnings (all dependency-version
suggestions, unchanged from Phase 13); `lintVitalRelease` passed; debug,
androidTest and R8-minified release APKs all produced.

The device checklist above — including the release→release upgrade case
(this artifact is the vc13 → vc14 upgrade candidate) — remains
unchecked/UNVERIFIED: no hardware was available.

## Phase 15 refresh (2026-10-18)

Phase 15 (app-drawer rebuild: search + category chips, grid/list toggle,
persisted favorites with explicit edit mode, "Newly Installed" panel from
real first-install timestamps, shared-brand header and navigation rail,
adaptive drawer policy) rebuilt the release candidate on the same signing
identity. Every procedure and caveat above still applies verbatim; this
section only refreshes the recorded artifact.

| Property | Value |
|---|---|
| `versionCode` | 15 |
| `versionName` | `0.15.0-phase15` |
| Release APK | `app\build\outputs\apk\release\app-release.apk` |
| Size (bytes) | 52,032,598 |
| SHA-256 | `81709499280093B5D11ED06652FFACCBC797812A8E78CD31DFCF607DE9610FA8` |
| Certificate SHA-256 | unchanged from Phase 13: `29:C3:76:1C:48:D8:12:61:BD:89:CA:DF:AE:80:F4:ED:52:67:57:A8:4B:8A:1B:37:14:CD:68:8C:EE:A6:1B:86` |
| Signature schemes | v1 (JAR) true, v2 true |

Companion artifacts from the same clean build (not distributable):

| Artifact | Size (bytes) | SHA-256 |
|---|---|---|
| `app\build\outputs\apk\debug\app-debug.apk` | 69,654,806 | `C50AB59E08F40457683463A12F4B5DA4E45A6593B2547AB70D99E53C6D3B0679` |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,038,844 | `64EF843215DA9E9E2BE98F32F0B2BB859E18CC3B76B56529329CB75ED4212359` |

Automated state at build time (clean build 2026-10-18): 245 unit tests,
0 failures/errors; lint 0 errors / 33 warnings (all dependency-version
suggestions, unchanged from Phase 14); `lintVitalRelease` passed; debug,
androidTest and R8-minified release APKs all produced. The instrumented
drawer UI tests (15 `AppDrawerScreenTest` cases) are compile-checked via
`assembleDebugAndroidTest` only — they have never been executed.

The device checklist above — including the release→release upgrade case
(this artifact is the vc14 → vc15 upgrade candidate) — remains
unchecked/UNVERIFIED: no hardware was available.

## Phase 16 refresh (2026-10-18)

Phase 16 (settings rebuild: seven real categories, adaptive three-pane
layout with a live dashboard preview, real catalog search, persisted
app-wide text size, favorites-preserving reset) rebuilt the release
candidate on the same signing identity. Every procedure and caveat above
still applies verbatim; this section only refreshes the recorded artifact.

| Property | Value |
|---|---|
| `versionCode` | 16 |
| `versionName` | `0.16.0-phase16` |
| Release APK | `app\build\outputs\apk\release\app-release.apk` |
| Size (bytes) | 52,065,372 |
| SHA-256 | `5DFBC235D0BE747792F6B224F50CA0385F60439EA994CD4A0FA9DF9A24A92CC5` |
| Certificate SHA-256 | unchanged from Phase 13: `29:C3:76:1C:48:D8:12:61:BD:89:CA:DF:AE:80:F4:ED:52:67:57:A8:4B:8A:1B:37:14:CD:68:8C:EE:A6:1B:86` |
| Signature schemes | v1 (JAR) true, v2 true |

Companion artifacts from the same clean build (not distributable):

| Artifact | Size (bytes) | SHA-256 |
|---|---|---|
| `app\build\outputs\apk\debug\app-debug.apk` | 69,736,718 | `36E1D5420DFCD4B1703AB56BEB76460C49F7EAA0528923E71B2BC253B84CDDC5` |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,056,092 | `CF916C43DEA691F1622FFE4DF3C7D27E1E7B0617F1B278761F36644BF0C2FED8` |

Automated state at build time (clean build 2026-10-18): 265 unit tests,
0 failures/errors (Phase 16 adds 20: settings pane policy, settings
catalog search, extended settings mapping/app-settings suites); lint
0 errors / 33 warnings (all dependency-version suggestions, unchanged from
Phase 15); `lintVitalRelease` passed; debug, androidTest and R8-minified
release APKs all produced. The instrumented settings UI pins (18
`SettingsScreenTest` cases) are compile-checked via
`assembleDebugAndroidTest` only — they have never been executed.

The device checklist above — including the release→release upgrade case
(this artifact is the vc15 → vc16 upgrade candidate) — remains
unchecked/UNVERIFIED: no hardware was available.

## Phase 17 refresh (2026-10-18)

Phase 17 (diagnostics rebuild: four live summary cards, honest
performance readings with a one-second sample-history sparkline, System
Information, App Components, bounded Recent Logs with a full in-app log
detail) rebuilt the release candidate on the same signing identity. Every
procedure and caveat above still applies verbatim; this section only
refreshes the recorded artifact.

| Property | Value |
|---|---|
| `versionCode` | 17 |
| `versionName` | `0.17.0-phase17` |
| Release APK | `app\build\outputs\apk\release\app-release.apk` |
| Size (bytes) | 52,098,151 |
| SHA-256 | `983516270AAD1FF0683B81D4955E69ED1E8849C814F614483EA186C61A8EB6AA` |
| Certificate SHA-256 | unchanged from Phase 13: `29:C3:76:1C:48:D8:12:61:BD:89:CA:DF:AE:80:F4:ED:52:67:57:A8:4B:8A:1B:37:14:CD:68:8C:EE:A6:1B:86` |
| Signature schemes | v1 (JAR) true, v2 true (re-verified with `apksigner` on this artifact) |

The packaged `versionName`/`versionCode` were re-read from the APK with
`apkanalyzer` (`0.17.0-phase17` / 17) — the App status card reads
`BuildConfig.VERSION_NAME`, so the shipped RC reports its own version.

Companion artifacts from the same clean build (not distributable):

| Artifact | Size (bytes) | SHA-256 |
|---|---|---|
| `app\build\outputs\apk\debug\app-debug.apk` | 69,835,029 | `C3CBCA3FC87CAD43B221425417CFAD0F334CD5821D048889ECF060F0F435DD48` |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,073,151 | `147DF007C50F439C7BD1804EEA6208C45D5E36692BF66E7D48339FD2C135AF42` |

Automated state at build time (clean build 2026-10-18): 328 unit tests,
0 failures/errors (Phase 17 adds 63: `DiagnosticLogTest` 8,
`FrameSampleHistoryTest` 4, `SystemSamplerTest` 8, `AccessReportTest` 7,
`ComponentHealthTest` 10, `AppHealthTest` 4,
`DiagnosticsLayoutPolicyTest` 8, `DiagnosticsSummaryTest` 12, plus 2
`formatLogTimestamp` cases); lint 0 errors / 33 warnings (all
dependency-version suggestions, unchanged from Phase 16);
`lintVitalRelease` passed; debug, androidTest and R8-minified release
APKs all produced (`BUILD SUCCESSFUL in 4m 3s`). The instrumented
diagnostics UI pins (21 `DiagnosticsScreenTest` cases) are compile-checked
via `assembleDebugAndroidTest` only — they have never been executed.

The device checklist above — including the release→release upgrade case
(this artifact is the vc16 → vc17 upgrade candidate) — remains
unchecked/UNVERIFIED: no hardware was available.

## Phase 18 refresh (2026-10-18)

Phase 18 (navigation page: map-dominant layout, real local search with
saved-places/recents suggestions, collapsible places panel that reflows
without recreating the map, embedded map still disabled by default,
adaptive COMPACT/MEDIUM/WIDE policy) rebuilt the release candidate on the
same signing identity. Every procedure and caveat above still applies
verbatim; this section only refreshes the recorded artifact.

| Property | Value |
|---|---|
| `versionCode` | 18 |
| `versionName` | `0.18.0-phase18` |
| Release APK | `app\build\outputs\apk\release\app-release.apk` |
| Size (bytes) | 52,114,527 |
| SHA-256 | `905C974D1A0899C69DFDD8C6F98E58DC1935D18B07D28A66ACA08AB82586FF35` |
| Certificate SHA-256 | unchanged from Phase 13: `29:C3:76:1C:48:D8:12:61:BD:89:CA:DF:AE:80:F4:ED:52:67:57:A8:4B:8A:1B:37:14:CD:68:8C:EE:A6:1B:86` |
| Signature schemes | v1 (JAR) true, v2 true (re-verified with `apksigner` on this artifact) |

The packaged `versionName`/`versionCode` were re-read from the APK with
`apkanalyzer` (`0.18.0-phase18` / 18).

Companion artifacts from the same clean build (not distributable):

| Artifact | Size (bytes) | SHA-256 |
|---|---|---|
| `app\build\outputs\apk\debug\app-debug.apk` | 69,917,083 | `EB6590D2AF2DFFF766F914B8BB406ECC4AFA1C6B009A9858E4E31D655672D54D` |
| `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,089,467 | `6584519FB3301FA456F62908F020A0C25F31C5A4C10A7CB1E409C826A07BECEC` |

Automated state at build time (clean build 2026-10-18): 401 unit tests,
0 failures/errors (Phase 18 adds 73: `SavedPlaceTest` 12,
`SavedPlaceCodecTest` 13, `RecentDestinationTest` 12,
`NavigationSearchTest` 10, `NavigationPageProviderTest` 6,
`NavigationDestinationTextTest` 6, `NavigationLayoutPolicyTest` 14);
lint 0 errors / 33 warnings (all dependency-version suggestions, unchanged
from Phase 17); `lintVitalRelease` passed; debug, androidTest and
R8-minified release APKs all produced (`BUILD SUCCESSFUL in 3m 45s`). The
instrumented navigation page pins (20 `NavigationScreenTest` cases) are
compile-checked via `assembleDebugAndroidTest` only — they have never been
executed.

The device checklist above — including the release→release upgrade case
(this artifact is the vc17 → vc18 upgrade candidate) — remains
unchecked/UNVERIFIED: no hardware was available.


## 0.20.0-vector-cockpit (2026-10-24) — no-image rebuild + map prototype removal

- **Cockpit is image-free**: the 7 bundled PNGs (album art fallback, car
  photo, 5 theme thumbnails) were replaced by code — a CSS/SVG artwork
  placeholder with the real MediaSession artwork overlaid when present,
  an inline SVG coupe silhouette, and CSS theme mini-previews. The APK
  contains no image outside the required `mipmap` launcher icons.
- **MapLibre prototype removed** (see `docs/map-poc.md` banner): no
  `libmaplibre.so`, no `ACCESS_WIFI_STATE`, okhttp/gson/timber dropped
  from the dex (verified via apkanalyzer).
- **Bug fixes**: WebView destroyed + JS interface removed on dispose;
  bridge holder survives recomposition (ON_RESUME refresh worked only by
  luck before); media artwork decode/compress moved off the main thread
  with a 512 px cap; stale artwork cleared when a session ends; real
  SEEK through `MediaController.transportControls.seekTo` (cockpit
  scrubber was a dead control).
- **Artifacts**: `app-release.apk` **2,364,911 B** (vc20 /
  `0.20.0-vector-cockpit`; prior universal release 25.6 MB — −91%),
  signed with the AutoVerse Release cert (SHA-256 `29C3761C…1B86`);
  `app-debug.apk` 19,671,345 B; 385 unit tests / 0 failures (401 before;
  16 map-PoC tests removed with the feature).
- Minified release dex contains no okhttp/gson/timber/maplibre classes
  (apkanalyzer check); `assets/cockpit/` is 424 KB of pure code + Leaflet.


## 0.21.0-tier1-premium (2026-10-25) — Tier 1 + premium themes

- **Onboarding**: 4-step first-run flow (welcome / theme pick / real setup
  checks / done) shown while `onboardingComplete` is false; replayable from
  Settings › About. Setup checks are real: system home-app chooser (never
  forces), notification-listener state, runtime location grant, speech
  service resolution (manifest `<queries>` extended for
  `RecognizerIntent`).
- **Driving mode**: cockpit-only, engaged from live GPS speed with
  hysteresis (enter >8 km/h, exit after 30 s below 5 km/h). Pauses
  decorative animation, enlarges dock/chips/capsules, shows a Driving chip.
  Never blocks function.
- **Premium themes**: Sakura, Neon Tokyo, Shonen, Gotham, Hot Rod registered
  end-to-end (Compose palettes in `AutoVerseColorsPremium.kt` + registry +
  bridge + cockpit CSS with static signature flair backdrops). Gated by
  `premiumUnlocked` (local entitlement, default locked); the unlock is an
  honest early-access grant — no payment claims. All 10 palettes pass the
  ContrastTest rules by construction (computed before writing).
- Performance: flair backdrops are static GPU-composited gradients (no
  images, no per-frame work); WebView renderer priority policy set for
  low-RAM units (API 25+).
- Artifacts: `AutoVerse.apk` / `app-release.apk` **2,275,156 B**, vc23 /
  `0.21.0-tier1-premium`, release cert unchanged; 392 unit tests / 0
  failures. FE verification: 7/7 playwright checks (premium lock/unlock,
  flair per theme, driving mode engage/exit, settings scroll).


## 0.22.0-tier2-obd-offline (2026-10-25) — Tier 2 part 1

- **OBD live engine data**: pure ELM327 protocol core (13 unit tests) +
  Bluetooth SPP repository + cockpit ENGINE · OBD strip with honest
  states (em dashes for unsupported PIDs, never fabricated values).
  `BLUETOOTH_CONNECT` added (runtime, API 31+). Socket behaviour
  UNVERIFIED until a real adapter is paired.
- **Offline map support**: passive viewed-tile mirror (IndexedDB), offline
  banner, offline search honesty, and the Settings "clear offline map
  cache" control now really clears (previously a fabricated toast).
- Artifacts: `AutoVerse.apk` **2,293,873 B**, vc24 /
  `0.22.0-tier2-obd-offline`, cert unchanged; **405 unit tests / 0
  failures**. FE verification 8/8 playwright checks.


## 0.23.0-offline-voice (2026-10-25) — Tier 2 part 2

- **Offline wake-word voice**: Vosk-based "Hey AutoVerse" + 6 s command
  window feeding the existing cockpit voice router. Opt-in toggle
  (Settings › Spatial Audio & Media), model download-on-first-enable
  (~40 MB, progress surfaced), RECORD_AUDIO runtime permission, foreground-
  only listening. New deps: vosk-android 0.3.47 + JNA 5.13.0 (@aar, vosk's
  own jna excluded to avoid duplicate classes); non-Android JNA natives
  excluded; jniLibs useLegacyPackaging=true.
- APK cost: 2.3 MB → **8,235,093 B** compressed (libvosk.so both ABIs).
- Artifacts: vc25 / `0.23.0-offline-voice`, cert unchanged; **412 unit
  tests / 0 failures** (53 suites; +7 OfflineVoiceRulesTest). FE checks:
  toggle render, download % status, capture-state orb, cleared on disable.
  On-device voice recognition UNVERIFIED (needs the phone + mic).


## 0.24.0-perf-tiering (2026-10-25) — cockpit performance tiering

- The cockpit now consumes the app's workload profile: bridge
  `getWorkloadProfile()` + live `pushWorkloadProfile` (on profile change),
  applied as body classes. **Constrained** drops the always-on GPU hogs:
  100px-blur ambient orbs, starfield, theme flair layer, vinyl disc, all 19
  backdrop-filter surfaces (solid fills keep legibility), and decorative
  infinite animations. Balanced = unchanged default; Enhanced = full look.
  Driving mode rules unchanged and stacked with the profile.
- Leaflet + app.js load with `defer` (parse off the critical path; init
  order preserved by document order).
- Measured (playwright + CDP CPU throttle, median of 3 runs; ms, lower is
  better; head-unit 1024x600 viewport):
  - 8x throttle: Settings tab 1547→934 (−40%), theme switch 880→536
    (−39%), home tab 570→457, apps tab 521→397.
  - 4x throttle: Settings tab 468→321, theme switch 369→271, home
    314→222, apps 325→235.
  - Balanced/Enhanced tiers keep the full visual set. Heap steady ~9.8 MB.
- Artifacts: vc26 / `0.24.0-perf-tiering`, 8,237,285 B, cert unchanged;
  412 unit tests / 0 failures.


## 0.25.0-theme-engine (2026-10-25) — dynamic theme system (PRD phases A/B/D/E)

- **Dynamic icon engine**: `ThemeIconSpec` (closed 6-shape set incl.
  GenericShape shields/hex/sport, border/background/glow/selected enums) +
  per-theme specs + `ThemedAppIcon` (real PackageManager bitmaps masked by
  the theme container in dock + drawer; artwork never recolored; fallback
  glyph; profile-gated glow). Cockpit CSS mirrors the language via
  clip-path masks + drop-shadow glows, disabled on Constrained.
- **Two new identities**: Fortress (light, sky blue) and Web Rush (racing
  red + electric blue) — contrast-verified palettes, registry, bridge
  mappings, cockpit cards/flairs. 12 themes total (7 premium).
- **Theme sounds**: ToneGenerator (Compose) / WebAudio (cockpit), off by
  default, rate-limited, Settings toggle + bridge methods.
- Tests: +5 ThemeIconSpecTest, updated theme-count tests; **417 unit tests
  / 0 failures** (54 suites). Cockpit checks: 12 cards / 7 premium /
  unlocked, per-theme clip-path icon languages verified.
- Artifacts: vc27 / `0.25.0-theme-engine`, 8,245,743 B, cert unchanged.
- Deferred (documented in docs/theme-system.md): Compose theme-gallery
  screen with preview-vs-applied split, wallpaper picker, accent variants,
  OS launcher-icon switching (needs explicit approval), device-verified
  icon legibility.
