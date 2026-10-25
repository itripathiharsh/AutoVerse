# Performance Profiles (Phase 9)

Phase 9 turns the previously cosmetic *Performance profile* setting into an
observable behavior contract. Each profile maps to a `PerformanceSpec` — a
pure, table-tested set of rendering knobs — and the dashboard, transitions,
media artwork, clock, and diagnostics all read their behavior from it. A
manual *Prefer smoothness* switch forces the Constrained workload without
changing the stored profile.

Implementation:
`app/src/main/java/com/autoverse/launcher/core/performance/PerformanceSpec.kt`
Tests: `app/src/test/java/com/autoverse/launcher/core/performance/PerformanceSpecTest.kt`

## Behavior contract

`PerformanceSpec` fields, by profile (Balanced is the pre-Phase-9 behavior
byte for byte):

| Knob | Constrained | Balanced (default) | Enhanced |
|---|---|---|---|
| Screen transitions | None | Fade | Fade + slide |
| Surfaces (screen + cards) | Flat fill | Gradient | Gradient |
| Dashboard FPS chip | Hidden | Shown | Shown |
| Clock tick | Minute-aligned | Every second | Every second |
| Artwork decode target (longest edge) | 128 px | 256 px | 256 px |
| Artwork crossfade | Off | Off | On |
| Status chips cap | Reduced | Full | Full |

Definition of each knob:

- **Screen transitions** — `AnimatedContent` between HOME / drawer / settings /
  diagnostics / map (AppRoot). `None` skips animation entirely; fade uses the
  motion-token durations; fade + slide adds a small vertical slide-in
  (`height / 32`).
- **Flat surfaces** — `ScreenScaffold`, `CockpitCard` and the dashboard
  background draw a single `backgroundTop` fill instead of a vertical
  gradient. Cheap on weak GPUs, visually identical in structure.
- **FPS chip** — the live FPS status chip on the dashboard. When hidden the
  chip row does not render it, so no frame data appears anywhere off
  Diagnostics.
- **Minute-aligned clock** — the Constrained ticker wakes exactly at the next
  wall-clock minute boundary (`millisUntilNextMinute`, clamped to
  1..60_000 ms) instead of waking every second and recomposing. The clock is
  still exact to the second after any resume.
- **Artwork target** — longest-edge decode budget for media artwork
  (`sampleSizeFor`, power-of-two `inSampleSize`). Cache entries are keyed
  `uri@target`, so a profile change can never serve an oversized bitmap.
- **Artwork crossfade** — `Crossfade` when artwork appears, changes or
  disappears in the media card and compact media row.
- **Status chips cap** — `minOf(layoutPolicy.statusChips, spec cap)`; the
  Constrained cap keeps the theme chip (plus the FPS chip when shown) and
  drops the profile/tier chips.

## Prefer smoothness

`effectiveWorkloadProfile(profile, preferSmoothness)` resolves the override:
when the switch is on, every profile runs the Constrained workload. The
stored profile is untouched, so turning the switch off restores the profile
the user picked. The dashboard's profile chip shows the *effective* profile;
Diagnostics shows Selected vs Effective, both labelled.

## Selection policy

- The stored profile is manual (Settings → PERFORMANCE PROFILE), default
  Balanced.
- Missing or unknown stored values fall back to Balanced
  (`profileFromStorage`) — the conservative default, matching the decision
  that unknown head units start safe.
- **No automatic runtime switching.** The PO decisions require stable
  thresholds and cooldowns before auto-switch may ship; adding sampling-based
  switching without them risks oscillation. `DeviceProfile` is still detected
  and shown in Diagnostics.
- The detected `PerformanceTier` is informational; it is not wired to the
  profile (PRD §6.3–6.4 forbids unreliable CPU/GPU inference).

## Sampling and diagnostics

- `FrameTimeTracker` is Choreographer-driven and runs **only while it is
  wanted**: the diagnostics screen, or the dashboard when the active profile
  shows the FPS chip. It also stops on `ON_PAUSE` (`FrameSamplingEffect` in
  AppRoot), so it never samples a backgrounded or off-screen surface.
- The pure math lives in `FrameStatsAccumulator`: one-second windows, FPS
  published when a window closes, cumulative frame/jank counters. A frame is
  janky when the gap to its predecessor is strictly longer than 33 ms
  (two 60 Hz deadlines). Android-free, fully unit-tested.
- Diagnostics → WORKLOAD PROFILE shows the effective knobs (transitions,
  surfaces, FPS readout, artwork max edge) so any profile claim can be
  verified on-device without a frame capture.

## Memory pressure

There is no custom `Application` class; `MainViewModel` registers
`ComponentCallbacks2` and on `TRIM_MEMORY_RUNNING_LOW` or above (and
`onLowMemory`) calls `AppRepository.trimMemory()` (app icon cache) and
`MediaSessionRepository.trimMemory()` (artwork cache). Both caches evict;
bitmaps are never recycled.

## Measurement methodology (results pending device)

FPS is **measured, never promised**. Target methodology for a head unit:

1. Install the debug build; open Diagnostics; let sampling run ~60 s of
   steady interaction (scroll the drawer, open/close settings).
2. Read `Frames per second` (last closed 1 s window) and
   `Janky frames (>33 ms)`; repeat per profile by switching the stored
   profile.
3. For frame-time percentiles (p50/p90/p95) use
   `adb shell dumpsys gfxinfo com.autoverse.launcher framestats` after a
   scripted trace; the in-app readout intentionally reports per-second FPS and
   jank counts only.
4. Memory: `adb shell dumpsys meminfo com.autoverse.launcher` before/after
   media artwork loading per profile target (128 vs 256 px).

**Status: UNVERIFIED — no device/head unit was available in the Phase 9
environment (`adb devices` empty).** No numbers above may be read as
measured results; they are the protocol for the hardware pass.

## APK size findings (measured, 2026-10-17)

Built with `gradle :app:assembleDebug :app:assembleRelease`:

| Artifact | Bytes | Notes |
|---|---|---|
| `app-debug.apk` | 69,262,459 | Phase 8: 69,246,087 → **+16,372 B** for all of Phase 9 |
| `app-release-unsigned.apk` | 62,110,994 | release variant (unsigned; not installable) |
| `app-debug-androidTest.apk` | 1,029,954 | unchanged (androidTest sources untouched) |

*(Historical Phase 7–9 measurements — the MapLibre payload below was removed entirely in 0.20.0; the release APK is now 2.4 MB.)*

The dominant payload is unchanged from the Phase 7 report: **MapLibre
native libraries, 47.06 MB compressed (~68% of the APK)**, shipped for all
four ABIs even though the embedded-map flag defaults off:

| `lib/*/libmaplibre.so` | Compressed |
|---|---|
| x86 | 12.76 MB |
| x86_64 | 12.73 MB |
| arm64-v8a | 12.49 MB |
| armeabi-v7a | 9.08 MB |

Implications if the map PoC is removed or split: ABI-filtered or split APKs
would ship ~12.5 MB (arm64) instead of ~47 MB of native libs; removing the
dependency entirely would take the debug APK to roughly the low 20 MB range.
Decision remains with the Phase 7 recommendation (do not ship until device
evaluation). Phase 9 itself adds ~16 KB — one small class file, no new
assets, no new dependency.

## Tests

Phase 9 adds 24 tests (Phase 8: 28 suites / 167 tests → **31 suites / 191
tests / 0 failures**):

- `PerformanceSpecTest` (8) — the per-profile knob table, `Default =
  Balanced`, override behavior for every profile, artwork targets are
  powers of two.
- `FrameStatsAccumulatorTest` (6) — window close math (50 frames × 20 ms =
  50 fps), null until a window closes, strict >33 ms jank threshold, window
  restart, `resetWindow` keeps counters.
- `ClockTickerTest` (6) — exact-boundary, half-minute, one-second and
  one-nanosecond cases, bounded 1..60_000 sweep, midnight wrap.
- `SettingsMappingTest` (+3) — profile round-trip, missing/unknown fall back
  to Balanced.
- `MediaArtworkSamplingTest` (+1) — the 128 px target samples more
  aggressively.
- `AppSettingsTest` — default `preferSmoothness = false` asserted.

## Device-validation risks (open)

- Per-profile FPS/jank differences on real hardware (the gate for any claim).
- Reduced-motion × profile interaction: reduced motion already collapses
  motion tokens to 0 ms; Constrained adds `.None` transitions — both paths
  are instant, no conflict expected but untested on hardware.
- Minute-aligned clock behavior across suspend/resume and timezone changes.
- Artwork 128 vs 256 px legibility in a cabin at typical screen sizes.
- `ComponentCallbacks2` trim efficacy under real memory pressure.

## Phase 17 refresh — frame sessions and the diagnostics readout

The diagnostics page consumes the existing frame tracker; the profiles
themselves are unchanged, and no profile-switching controls were added to
diagnostics (the Performance Profile card only reports the stored profile
against the effective one and explains divergence).

- `FrameTimeTracker` now exposes `Snapshot(sessionId, atMillis)` in
  addition to the fps/frame/jank counters, a `running` StateFlow, and an
  injectable `clock`. Each `start()` opens a new session id, and every
  published one-second window carries a wall-clock timestamp, so the
  diagnostics page can chart a bounded history and detect stale sessions.
- New `core\diagnostics\FrameSampleHistory.kt`: a 60-sample FIFO of
  `FrameSample(fps, atMillis)` (oldest-first). A ViewModel collector
  bridges published windows into it — at most one sample per second — and
  clears it whenever the tracker session id changes, so the sparkline
  never mixes sessions. The chart scales its y-axis to at least 60 FPS and
  shows an honest empty state until samples exist.
- Sampling remains resume-scoped: the tracker starts/stops with visible
  frame-showing screens (Phase 9 rules), and the FPS readout shows
  "Tracking disabled" when not sampling and "Measuring…" until the first
  window closes — never a stale number.
- Coverage: `FrameSampleHistoryTest` (4 cases — FIFO bound, order,
  clearing) plus the unchanged `FrameStatsAccumulatorTest` (window math,
  jank threshold). The tracker's new session fields are compile-verified
  only; real frame behavior on hardware remains UNVERIFIED (no hardware).
