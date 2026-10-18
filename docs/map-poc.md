# Embedded Map Proof of Concept (Phase 7)

Status: **evaluated, implemented behind a disabled-by-default feature flag,
not shipped**. Recommendation at the bottom: keep it off until a device
evaluation and a licensed tile strategy exist.

## 1. Purpose & scope

Phase 7 asked whether an embedded map is viable in a head-unit launcher
without mandatory paid services, secrets in source control, or giving up
the Phase 6 navigation handoff. The output is a bounded prototype:

- One screen (`Screen.MAP_POC`, "Map preview") reachable only from
  Settings → EXPERIMENTAL while the new `embeddedMapEnabled` setting is on.
- Default state of the setting: **off** — normal builds and normal user
  flows never instantiate a map or load map data.
- The navigation handoff (Phase 6) remains the product's map behavior and
  is reused inside the prototype as the fallback whenever the map cannot
  render.
- No location permission is requested; the camera starts at a fixed demo
  position (Vienna, 48.2082 / 16.3738, zoom 11).

## 2. Provider evaluation & decision

| Criterion | MapLibre Native 11.13.5 (chosen) | Google Maps SDK for Android |
| --- | --- | --- |
| License / cost | BSD-2-Clause; no account, no API key, no billing | Free tier then billing; Dynamic Maps 10,000 loads/mo free, then ~$7.00/1,000 |
| Secrets | None in source or build | Requires API key (must be injected and rotated) |
| Platform deps | Plain Android; no Google Play services | Requires Google Play services on the head unit — unverifiable in Phase 0 audit |
| Offline / caching | Can render local/self-hosted styles and tiles | Terms restrict storing/caching map content (narrow exceptions: `place_id`, `pano_ID`, `video_ID`; Geocoder lat/lng ≤ 30 days) — disqualifying for offline head-unit scenarios |
| Routing / ETA | None (handoff stays the answer) | Directions API exists but adds billing + key surface |

Chosen: **MapLibre Native Android 11.13.5** (`org.maplibre.gl:android-sdk`).
Rationale for pinning 11.x instead of the newest 13.7.1 line: 11.13.5 ships
the OpenGL ES renderer (safer on older head-unit GPUs) and a
kotlin-stdlib 2.0.20 dependency compatible with this project's Kotlin
2.0.21. The 13.x line defaults to Vulkan and Kotlin 2.2 metadata.

## 3. Tile data strategy

The prototype uses the MapLibre **demo style**
(`https://demotiles.maplibre.org/style.json`) — the project explicitly
labels it demo/dev data, not a production tile service, and the screen
carries a visible "Prototype only" caption. Production would require one
of: a licensed provider (e.g. MapTiler free tier — 100k requests / 5k
sessions per month, documented as PoC/testing use only, Flex from $25/mo),
a community service like OpenFreeMap (no SLA, no cap), or self-hosted
tiles. Public OSM community tile servers must never be used as an assumed
production service.

## 4. Architecture

Files (all under `app/src/main/java/com/autoverse/launcher/`):

| File | Role |
| --- | --- |
| `feature/map/MapPocConfig.kt` | Pure constants: style URL, 15 s load timeout, fixed demo camera |
| `feature/map/MapPocState.kt` | `Loading / Ready / Empty / Error(failure) / Unavailable(reason)` + `showsFallback` + user-facing failure copy |
| `feature/map/MapPocController.kt` | Pure Kotlin state machine (`StateFlow`) — no Android types, fully JVM-testable |
| `feature/map/MapPocScreen.kt` | Screen, testable `MapPocContent` chrome (surface injected), real `MapPocSurface` with `MapView` |
| `core/model/AppSettings.kt` | `embeddedMapEnabled: Boolean = false` |
| `core/preferences/SettingsRepository.kt` | `KEY_EMBEDDED_MAP = booleanPreferencesKey("embedded_map")` + setter |
| `MainViewModel.kt` | `setEmbeddedMapEnabled(enabled)` |
| `feature/settings/SettingsScreen.kt` | EXPERIMENTAL section: switch + "Open map preview" action |
| `AppRoot.kt` | `Screen.MAP_POC` route, guarded: renders only while the flag is on, otherwise redirects to Settings |

State machine:

```
Loading ──style loaded, >0 sources──▶ Ready
   │──style loaded, 0 sources──▶ Empty ──reload()──▶ Loading
   │──style failure / 15 s timeout──▶ Error ──reload()──▶ Loading
   │        ▲ (a late style success still promotes to Ready/Empty)
Ready ──renderer failure──▶ Error
any ──surface could not start──▶ Unavailable (terminal, reason shown)
```

Lifecycle: the `MapView` is created inside the screen's composition,
`onCreate` on construction; an activity-lifecycle observer forwards
START/RESUME/PAUSE/STOP/DESTROY; `onDispose` calls `mapView.onDestroy()`.
Leaving the screen destroys the map — no map work continues on other
screens, and the flag being off means the screen can never be reached.

Manifest: `INTERNET` permission was added to the main manifest with a
comment; if the prototype is removed (no-ship decision), remove the
permission with it.

## 5. Tests & verification

`gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`
→ **BUILD SUCCESSFUL**. **27 suites / 162 tests / 0 failures / 0 errors**
(Phase 6: 25/147; +2 suites, +15 tests).

- `MapPocControllerTest` (JVM, 13 tests): every transition — ready/empty
  split, retryable style failure, timeout, late-success recovery, stale
  failure ignored after ready, runtime failure, reload semantics,
  Unavailable terminality, fallback recommendation flags.
- `MapPocConfigTest` (JVM, 2 tests): style URL is HTTPS on the MapLibre
  demo host (never an OSM tile server), timeout within a sane budget.
- `AppSettingsTest` (extended): `embeddedMapEnabled` defaults to `false`.
- `MapPocScreenTest` (androidTest, 6 tests): loading overlay + surface,
  ready shows surface and no fallback, error shows message/retry/fallback
  and fires the handoff callback, empty message, unavailable hides the
  surface and retry but keeps the fallback, retry callback. Includes the
  `mainClock.autoAdvance = false` workaround for the indefinite progress
  animation. **Compiles only — no device connected in this environment.**

## 6. Build artifacts & size impact

| Artifact | Phase 6 | Phase 7 |
| --- | --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | 18,537,340 B | **69,229,680 B** |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | 1,029,225 B | 1,029,954 B |

The +50.7 MB is MapLibre's native `libmaplibre.so` across ABIs (no ABI
splits in debug). A production decision must include an ABI-split / bundle
size budget plan; this alone is a material product cost.

## 7. Measurement plan (UNVERIFIED — no device)

All figures must be collected on a real head unit before any ship call:

1. Launcher cold start: unchanged expectation — the map is only created on
   demand; verify no MapLibre class loading on the home path.
2. Map screen open latency: Settings → toggle on → Map preview; time to
   first drawn frame (target: within the 15 s timeout budget on wifi).
3. Memory: PSS delta while the map screen is open vs closed; no growth
   across repeated open/close cycles.
4. Frame timing during pan/zoom gestures (jank % via `dumpsys gfxinfo`).
5. Navigation away/back and app backgrounding: map destroyed on leave,
   no residual CPU/network, no leaked activity.
6. Airplane mode: expect Loading → timeout → Error with the handoff
   fallback visible.

## 8. Risks & unknowns

- 50 MB APK growth without ABI splits (see §6).
- Real head-unit renderer support (EGL/OpenGL ES drivers, GPU) unverified.
- Demo tiles are explicitly not for production; a commercial tile
  provider contract (or self-hosting) is an open product/cost decision.
- No routing/ETA/traffic: the handoff remains the only route answer.
- Network is required; the PoC has no offline map story, which is exactly
  where the Google ToS restrictions were disqualifying.

## 9. Recommendation

**Do not ship yet.** Keep `embeddedMapEnabled` default-off (it is), keep
the flag as the only entry point, and run the §7 device evaluation before
deciding. If the prototype graduates, it needs: a licensed tile provider
decision, an offline/network-failure product story, an ABI-split size
plan, and a UI pass by the design system owner. If it does not graduate,
delete `feature/map/`, the EXPERIMENTAL section, the `INTERNET`
permission, the MapLibre dependency, and `embeddedMapEnabled` — all
removal points are isolated and documented here.

## Phase 18 refresh — navigation page integration

The Phase 18 navigation page consumes the prototype strictly as an
optional surface; the ship recommendation above is unchanged.

- `MapPocScreen.kt` exposes `EmbeddedMapSurface(controller, reloadToken,
  modifier)` publicly; the navigation page accepts an injectable
  `embeddedMapSurface` slot (tests inject a stub; production passes the
  real surface when the flag is on).
- The page **never depends** on the embedded map: with
  `embeddedMapEnabled = false` (the default) the body is an intentional
  handoff surface — provider label, honest explanation, "Open navigation
  app" action — never a fake or placeholder map.
- With the flag on, the page hosts the prototype's `MapPocController`
  states unchanged (loading overlay, empty style, retryable error,
  terminal unavailable) and keeps the handoff as the fallback action.
- Hiding/showing the places panel or editing the query never recreates the
  map surface; leaving the page disposes it exactly as the Map preview
  screen does (§4 lifecycle).
- Demo tiles (`demotiles.maplibre.org`) are still demo/dev data only, and
  remain acceptable solely because the feature is off by default (§3).
- `DesignPreviews.kt` gained 8 navigation-page previews (63 total),
  covering wide/medium/compact, day theme, panel hidden, empty places and
  invalid-coordinate states — all with the embedded map off.

Still unverified: real rendering, GPU/EGL behaviour and the §7 measurement
plan on a head unit (no hardware available).
