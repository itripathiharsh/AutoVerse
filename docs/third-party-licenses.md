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
| `org.maplibre.gl:android-sdk` | 11.13.5 | BSD-2-Clause |
| `org.maplibre.gl:android-sdk-turf` | 6.0.1 | BSD-2-Clause |
| `org.maplibre.gl:android-sdk-geojson` | 6.0.1 | BSD-2-Clause |
| `org.maplibre.gl:maplibre-android-gestures` | 0.0.4 | BSD-2-Clause |
| `com.squareup.okhttp3:okhttp` | 4.12.0 (transitive of MapLibre) | Apache-2.0 |
| `com.google.code.gson:gson` | 2.10.1 (transitive of MapLibre) | Apache-2.0 |
| `com.jakewharton.timber:timber` | 5.0.1 (transitive of MapLibre) | Apache-2.0 |
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
- **BSD-2-Clause** (MapLibre family): retain the copyright notice and
  disclaimer in binary distributions. MapLibre's license text must ship with
  the APK's notices.
- **GPL-2.0 WITH Classpath-exception-2.0** (`desugar_jdk_libs`): no source
  disclosure obligation for the app (classpath exception); ship the license
  text with notices.
- **EPL-1.0** (JUnit, test only): not distributed in the APK.

The consolidated notice file itself is a Phase 13 packaging task, tracked
as checklist item 8 in `docs/release.md` (which now exists); the licenses
and obligations above stand as verified (Phase 11), and the shipped
dependency set was unchanged in Phase 12.

## Map data, tiles and attribution

- The embedded map is an **experimental prototype, off by default**, loaded
  with MapLibre's public demo style/tiles. The demo tile service is for
  development only: it must not be treated as a production tile source, and
  it carries no availability or data-licensing guarantee.
- MapLibre Native itself renders the tiles; MapLibre attribution requirements
  (visible attribution control) must be satisfied by any production map view.
- For production use, a tile/data provider with explicit licensing must be
  selected (e.g. a commercial vector-tile provider, or self-hosted
  OpenMapTiles with OpenStreetMap data). OpenStreetMap-derived data requires
  "© OpenStreetMap contributors" attribution under ODbL, and each style has
  its own license. This decision is deliberately deferred and gated behind
  the experimental flag; the in-app text already states "Map data: MapLibre
  demo tiles. Prototype only — production needs a licensed tile provider."
- Because the prototype is off by default, no map requests or attribution
  claims reach users in the default configuration. The navigation
  **hand-off** (launching an installed maps app) requires no map data,
  license, or network access from AutoVerse.
