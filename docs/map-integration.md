# Google Maps Integration — Audit (0.28.0 Phase 0)

> Status 0.28.5: **the live map is MapLibre Native + OpenFreeMap** (replaces
> 0.28.3's osmdroid + CARTO). Why: osmdroid's repository is archived (last
> release Aug 2024) and draws raster tiles on the CPU; CARTO's basemap FAQ says
> commercial use needs an Enterprise licence. MapLibre renders vector tiles on
> the GPU (OpenGL ES), is BSD-licensed and maintained; OpenFreeMap serves
> vector tiles free, keyless, unlimited, commercial use allowed (attribution
> required, shown on the map). Style URLs live in core/map/MapStyle.kt so the
> host can be swapped (self-hosted OpenFreeMap / bundled PMTiles) later.
> Pinned to MapLibre 13.0.2: 13.1+ needs Kotlin 2.2, which this AGP 8.5.2 lint
> cannot read; upgrading AGP/Kotlin unlocks 13.7.x.

> Status 0.28.3: **a live map now ships in the map regions** — osmdroid
> rendering free OpenStreetMap (CARTO dark) tiles with the GPS puck. No API
> key, no billing, offline tile cache. It is an OpenStreetMap map, not the
> Google Maps app; turn-by-turn still hands off to Google. The 0.28.0 audit
> below remains accurate for *Google* embedding specifically.

## 1. What the product wanted

A live Google Maps view rendered directly inside AutoVerse layouts — Home
(60% region), Apps (70%), Maps (70%) — with no click-to-open step, within
these constraints: no API key/billing without explicit approval, no
recurring cost, no scraping, no unsupported embedding, no fake maps.

## 2. Options investigated

| Option | Embeds a live map? | Supported? | Cost / requirements | Verdict |
| --- | --- | --- | --- | --- |
| Google Maps SDK for Android (`com.google.android.gms:play-services-maps`) | Yes — interactive Google-styled map in-app | Yes, documented | API key + **billing-enabled Google Cloud project**; Play services on device; network at render time | **Blocked on approval** (see §3) |
| Google Navigation SDK for Android | Yes, with turn-by-turn UI | Restricted — partner eligibility only | Commercial agreement with Google | Not available to this project |
| Intents / Maps URLs (`google.navigation:q=`, `geo:`, https deep links) | No — launches the Maps app | Yes, documented | None | **Shipped** (handoff) |
| Embedding the installed Maps app (Activity embedding, overlays, screen capture) | N/A | No — no platform support; unsafe | — | Forbidden by constraint |
| WebView of google.com/maps | Renders a web map | No — violates Google ToS; scraping-adjacent; fragile | — | Forbidden by constraint |
| OSM-based SDKs (osmdroid / MapLibre) | Yes, but not Google Maps | Yes | Zero cost, self-supplied tiles | Rejected: user asked for Google Maps; Leaflet PoC already removed by user (0.20.0) |

Important distinction confirmed: **Android intents and Maps URLs launch Maps
externally; they do not provide an embeddable view.** No public API exposes
the Maps app's internal speedometer either (see `docs/performance-profiles.md`
and the Speedometer source note: AutoVerse uses GPS-derived speed, labelled).

## 3. Requirements if embedding is later approved

1. Create a Google Cloud project with **billing enabled** (a credit card on
   the project; the Maps SDK has a free monthly allowance — historically
   10,000 Dynamic-Map loads/month per SKU pricing — but billing cannot be
   skipped).
2. Create a **Maps SDK for Android API key**, restricted to the app's
   package name + release signing certificate SHA-1 (and debug SHA-1 during
   development).
3. Deliver the key OUT of source control: `local.properties` /
   `gradleLocalProperties` manifest placeholder injection (never committed).
4. Add the `play-services-maps` dependency (+ Play services availability
   handling on generic head units — many have none, so a graceful fallback
   stays required).
5. Review the Google Maps Platform Terms (caching/offline restrictions apply;
   the current offline-tile story does not exist for the SDK).

None of steps 1–2 can be performed by the agent: creating a cloud project and
enabling billing is the project owner's action. **Nothing was purchased,
registered, or configured.**

## 4. What ships instead (0.28.0)

- `GoogleMapsRegion` (`core/design/components/GoogleMapsRegion.kt`) — one
  honest, shared launch surface used by Home (60%), Apps (70%) and Maps
  (70%): prominent "Open Google Maps" action, destination entry with live
  validation handed to the provider, provider-availability messaging, and an
  explicit one-line disclosure that embedded maps need an API key + billing,
  so no fake map is drawn.
- Turn-by-turn stays a package-pinned handoff (`SystemNavigationRepository`);
  failures surface as typed outcomes (no silently launched apps).
- Map regions expand/collapse instantly (no animation) when side panels are
  minimized, per the no-animation constraint.

## 5. Honest limitations

- The Maps region is a launch surface, **not** an embedded map. AutoVerse
  does not claim otherwise anywhere in the UI.
- "Open Google Maps" launches the installed app; if it is missing, the
  system `geo:` resolver is used and the UI says the preferred app is
  missing. If no resolver exists, the handoff fails with an explicit message.
- Speed data is GPS-derived (`SpeedometerManager`), labelled "GPS" — never
  presented as Google Maps' reading.
