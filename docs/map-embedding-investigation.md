# Native Google Maps Display Without an API — Investigation (0.28.1)

> Verdict: on standard Android, the installed Google Maps app **cannot be
> kept permanently visible inside AutoVerse's layout regions through any
> supported, key-free mechanism**. The app now ships a built-in **embedding
> probe** (Diagnostics → "Run embedding probe") that verifies this on the
> actual head unit and live-tests every candidate approach. The Maps SDK
> (API key + billing) remains the only supported embedded path and is
> deliberately not enabled.

## 1. Target device

The head unit is not attached to this workstation (`adb devices` empty), so
static facts come from the project (minSdk 23, target/compile SDK 34) and the
live facts — Android version, manufacturer/firmware, freeform support, Maps
resizeability — are collected **on-device by the probe screen**
(`MapEmbedProbeScreen`, `core/map/MapEmbedProbe.kt`). This is deliberate:
the spec forbids assuming support from generic documentation, and the probe
reports the actual unit instead.

## 2. Approaches investigated (in the required order)

### A. Native split-screen / multi-window
`FLAG_ACTIVITY_LAUNCH_ADJACENT` is documented as a **request, honoured only
while the device is already in split-screen mode**; the user enters
split-screen manually (recents → split) and the system/user own the
proportions. A launcher cannot command 60/20/20 or 70/30, cannot enter split
mode programmatically on stock Android, and AutoVerse-as-HOME is not a split
participant the system offers a placement for. The probe (button 1) tests
adjacency on the real unit and logs whether the system entered
multi-window and whether AutoVerse stayed visible.

### B. Task / activity embedding
Activity embedding (Android 12L+, Jetpack WindowManager) splits activities
opened **into the embedding app's own task**, and the embedded app must opt
in. Google Maps launches `singleTask` into its own task and does not opt
in, so a third-party launcher cannot embed it — ruled out by platform rules,
not by lack of trying. The probe's button 3 (launch without `NEW_TASK`)
demonstrates the consequence live: Maps opens its own task fullscreen and
covers AutoVerse.

### C. OEM automotive capabilities
Aftermarket head units ship AOSP forks without documented multi-window APIs
for launchers; Android Automotive (AAOS) multi-display/multi-window support
is system-signed and unavailable to sideloaded apps. Per the restriction
against undocumented hacks, nothing OEM-specific was attempted. The probe
does surface the two public system features that could indicate OEM
windowing (`FEATURE_FREEFORM_WINDOW_MANAGEMENT`,
`FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS`) so real hardware gets a
factual answer.

Freeform bounds launch (probe button 2) is supported **iff** the system
feature exists; even then the launcher is not a freeform participant, so
Maps gains a resizable window while AutoVerse remains covered — a resizable
Maps window, not a persistent 60/20/20 dock.

## 3. Answer to the core question

**No.** Without the Maps SDK there is no supported mechanism that keeps the
Maps app's live UI pinned inside AutoVerse's regions. The full chain of
gatekeepers: task model (Maps owns its task) → split-screen entry
(user-only) → proportions (system-owned) → embedding (same-task + opt-in) →
AAOS (system apps only).

## 4. Permissions / privileges / OEM dependencies
None were added beyond what existed. The probe adds **no permissions** (geo
VIEW was already declared for handoff). Its `force_resizable_activities`
read is a read-only peek at a developer option, used only to refine the
adjacency verdict.

## 5. Proof of concept
`feature/probe/MapEmbedProbeScreen.kt` — reachable from **Diagnostics →
"Run embedding probe"**: device facts, Maps facts (installed/version/
resizeable), per-approach verdicts from pure tested logic
(`MapEmbedProbeTest`), three live launch probes, and a bounded on-screen log
that records the multi-window state observed on every return. No shipped
screen changed; no animation added.

## 6. Closest viable alternatives (no key)
1. **Handoff surface (shipped)** — the GoogleMapsRegion launch regions.
2. **Manual split-screen** — user enters split via system UI; AutoVerse and
   Maps share the screen at system-chosen proportions. The probe's adjacent
   launch can restore this arrangement faster once the user has entered it.
3. **Maps SDK** — documented decision gate in `docs/map-integration.md`;
   requires the owner's cloud project + billing; not configured.

## 7. Evidence
No physical device is available here, so screenshots/logs cannot be produced
from this workstation. The probe itself is the evidence collector: run it on
the head unit and its log shows exactly what each launch did. All logic
around the evidence is unit-tested (463 tests, 0 failures).

## 8. Build results (0.28.1, versionCode 34)
`testDebugUnitTest` → 62 suites / 463 tests / 0 failures; `lintDebug` →
0 errors; `assembleDebug` + `assembleRelease` → BUILD SUCCESSFUL (release
signed, badging verified). Released as `0.28.1-embed-probe`.

## Sources
- [Support multi-window mode — Android Developers](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode)
- [Activity embedding — Android Developers](https://developer.android.com/develop/ui/views/layout/activity-embedding)
- [Leverage Multi-Window and Activity Embedding — Android Developers (Medium)](https://medium.com/androiddevelopers/leverage-multi-window-and-activity-embedding-4951f44a8992)
- [Activity Embedding in Android — ProAndroidDev](https://proandroiddev.com/activity-embedding-in-android-b07da14b715)
- [Launching another app in multi-window — Droids on Roids](https://www.thedroidsonroids.com/blog/multi-window-simple-examples-part-3-launching-another-app)
- [Split-screen launch behavior discussion — Stack Overflow](https://stackoverflow.com/questions/59068510/my-app-not-launching-in-split-screen-why)


---

## Addendum — 0.28.4: virtual-display via Shizuku (the user's proposal)

The one remaining idea — scrcpy-style virtual display with shell privileges —
**is feasible without root and without an API key**, using Shizuku (shell
privileges via wireless debugging). The app now ships it as a probe:
Diagnostics → **"Map display (Shizuku)"**.

How it works (all proven by scrcpy):
1. A Shizuku user service (`MapDisplayService`, runs as shell uid 2000)
   creates a virtual display rendering into AutoVerse's own SurfaceView.
2. `am start --display <id> -a VIEW -d <geo>` launches the installed Google
   Maps onto that display (documented adb option).
3. Touches on our region are scaled and injected via
   `IInputManager.injectInputEvent` (shell may inject).

Requirements: the Shizuku app installed and started (wireless debugging),
then a one-time grant. Fallback stays the free OSM map.

Honest limits: hidden-API dependence (can shift on firmware updates),
performance unmeasured until it runs on the unit, pre-API-30 devices inject
to the focused window rather than by display id, and it is one-time setup
rather than zero-setup.
