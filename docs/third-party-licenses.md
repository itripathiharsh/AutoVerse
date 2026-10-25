# Third-Party Licenses & Notices (Phase 11, refreshed Phase 12)

Phase 11 verified every third-party component that ends up in the shipped
APK, its license, and the obligations that follow for distribution. The
dependency set below was taken from the resolved `releaseRuntimeClasspath`
(what actually ships), not just the declared list. Phase 12 re-checked the
shipped set against the packaged release APK; no dependency versions were
changed in Phase 12, so the table below stands as verified.

## Shipped dependencies and licenses

### First-level runtime dependencies

| Component | Version | License |
| --- | --- | --- |
| `org.jetbrains.kotlin:kotlin-stdlib` (+ `-jdk7`/`-jdk8`/`-common`, parcelize runtime) | 2.0.21 / 1.9.x (resolved) | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` (+ `-core`) | 1.8.1 | Apache-2.0 |
| `org.jetbrains:annotations` | 23.0.0 | Apache-2.0 |
| `androidx.core:core-ktx` / `core` | 1.13.1 | Apache-2.0 |
| `androidx.activity:activity(-compose/-ktx)` | 1.9.2 | Apache-2.0 |
| `androidx.lifecycle:*` (runtime, runtime-ktx, runtime-compose, viewmodel, viewmodel-compose, viewmodel-savedstate, livedata, process) | 2.8.6 | Apache-2.0 |
| `androidx.compose.ui:*` (ui, ui-graphics, ui-text, ui-unit, ui-tooling-preview, geometry, util) | 1.7.5 (Compose BOM 2024.10.01) | Apache-2.0 |
| `androidx.compose.animation:animation` (+ `-core`) | 1.7.5 | Apache-2.0 |
| `androidx.compose.foundation:foundation` (+ `-layout`) | 1.7.5 | Apache-2.0 |
| `androidx.compose.runtime:*` (runtime, runtime-saveable) | 1.7.5 | Apache-2.0 |
| `androidx.compose.material3:material3` | 1.3.1 | Apache-2.0 |
| `androidx.compose.material:material-icons-extended` (+ `material-icons-core`, `material-ripple`) | 1.7.5 | Apache-2.0 |
| `androidx.datastore:datastore(-preferences)(-core)(-okio)` | 1.1.1 | Apache-2.0 |
| `com.squareup.okio:okio(-jvm)` | 3.6.0 (transitive of DataStore) | Apache-2.0 |
| `org.maplibre.gl:*` (native map stack) | — | **Removed in 0.20.0** with the flag-off map prototype; see `docs/map-poc.md` |
| Leaflet 1.9.4 (bundled cockpit WebView asset: `leaflet.js`, `leaflet.css`) | 1.9.4 | BSD-2-Clause |
| `com.android.tools:desugar_jdk_libs` | 2.1.2 | GPL-2.0 WITH Classpath-exception-2.0 |

Remaining resolved AndroidX transitives (`collection`, `annotation`,
`savedstate`, `emoji2`, `profileinstaller`, `tracing`, `startup`,
`interpolator`, `concurrent-futures`, `versionedparcelable`, `autofill`,
`loader`, `viewpager`, `customview`, `graphics-path`, `fragment`,
`arch.core`, `guava:listenablefuture`) are all Apache-2.0.

Every runtime component is Apache-2.0, BSD-2-Clause, or
GPL-2.0-with-Classpath-exception (the desugar library back-ports JDK
runtime classes, standard for Android apps; the classpath exception creates
no distribution obligation for app code). **No component ships telemetry,
analytics, advertising, or crash-reporting functionality** — verified
against each artifact's documented purpose above.

### Test-only and build-only (not shipped in the APK)

| Component | Version | License |
| --- | --- | --- |
| `junit:junit` (unit tests) | 4.13.2 | EPL-1.0 |
| `androidx.test.ext:junit`, `androidx.test.espresso:espresso-core`, `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` (instrumented tests) | 2.1/1.2.1/3.6.1/Compose BOM | Apache-2.0 |
| Gradle, Android Gradle Plugin, Kotlin compiler | 8.9 / 8.5.2 / 2.0.21 | Apache-2.0 |

## Obligations for distribution (Phase 13 checklist)

- **Apache-2.0** (majority): include the license text; retain copyright and
  NOTICE files where the library ships one. Handled by bundling a
  consolidated open-source notices screen/file at packaging time.
- **BSD-2-Clause** (Leaflet, bundled in the cockpit WebView assets):
  retain the copyright notice and disclaimer; Leaflet's license text ships
  in the source tree beside the bundled files.
- **GPL-2.0 WITH Classpath-exception-2.0** (`desugar_jdk_libs`): no source
  disclosure obligation for the app (classpath exception); ship the license
  text with notices.
- **EPL-1.0** (JUnit, test only): not distributed in the APK.

The consolidated notice file itself is a Phase 13 packaging task, tracked
as checklist item 8 in `docs/release.md` (which now exists); the licenses
and obligations above stand as verified (Phase 11), and the shipped
dependency set was unchanged in Phase 12.

## Map data, tiles and attribution (0.20.0 state)

- The native embedded-map prototype (MapLibre) was **removed in 0.20.0**
  together with its flag and dependency (see `docs/map-poc.md`). The
  Compose launcher renders no map of its own; the navigation **hand-off**
  (launching an installed maps app) requires no map data, license, or
  network access from AutoVerse.
- The cockpit WebView (in-app dashboard UI) embeds **Leaflet 1.9.4** and
  fetches raster map tiles and Nominatim geocoding over HTTPS when the
  cockpit is used with location granted. Tile-provider licensing and
  attribution for that cockpit map is an open pre-production item: before
  distribution, the tile source must be confirmed as licensed for this use
  (the current raster tiles are not a contracted production provider), and
  "© OpenStreetMap contributors" attribution must be visible wherever
  OSM-derived data is shown.
