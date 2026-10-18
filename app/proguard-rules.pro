# AutoVerse R8 keep rules — Phase 13 release candidate.
#
# Minification is ENABLED for release builds (isMinifyEnabled = true in
# app/build.gradle.kts). There are no keep rules in this file: the app
# code uses no reflection and no JNI, and every reflection-heavy
# dependency ships its own consumer rules, which R8 merges automatically.
#
# Consumer rules merged automatically from the resolved artifacts
# (verified in the Phase 12 audit and re-checked for the Phase 13 RC in
# app/build/outputs/mapping/release/configuration.txt):
#   - org.maplibre.gl:android-sdk 11.13.5 ships proguard.txt with Gson
#     reflection keeps, org.maplibre.geojson keeps, native-entry-point
#     keeps, enum value/valueOf keeps, and dontwarn/dontnote for optional
#     dependencies.
#   - okhttp 4.12.0 (META-INF/proguard/okhttp3.pro) and gson 2.10.1 ship
#     their own rules.
#   - Compose/androidx artifacts ship their own consumer rules.
#
# Phase 12 experiment (2026-10-17, same flags as now):
#   - BUILD SUCCESSFUL; lintVitalRelease clean; no missing-class
#     diagnostics and no missing_rules.txt emitted.
#   - Size: 51,795,751 B minified vs 62,118,062 B unminified
#     (-10,322,311 B, about -16.6%; native libmaplibre.so is untouched).
#
# Phase 13 status: the minified RUNTIME behavior is UNVERIFIED — no device
# or emulator is available, so no automated test executes the R8 output.
# Before treating the minified build as validated, install the signed RC
# on the target head unit and run the release smoke tests (embedded-map
# toggle, media session access, navigation hand-off — the callback- and
# reflection-heavy paths R8 can affect).
#
# Add rules below only if an R8 run reports a missing class or a device
# smoke test finds a break. Avoid broad keeps that disable shrinking.
