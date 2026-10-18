# Diagnostics Page (Phase 17)

Phase 17 rebuilds Diagnostics to the reference design (`image.png`) with
one hard rule: **nothing on the page is fabricated.** Every reading comes
from a real API or an already-observed app state, every nullable reading
renders an honest "Not readable" instead of a placeholder zero, and
sampling only runs while the screen is visible.

Source: `feature\diagnostics\DiagnosticsScreen.kt`,
`feature\diagnostics\DiagnosticsSummary.kt`, `core\diagnostics\*`,
`core\design\DiagnosticsLayoutPolicy.kt`, `MainViewModel.kt`.

## Layout

Three pane modes via `diagnosticsPanePolicyFor(widthDp, heightDp,
fontScale)` (thresholds and rules in
[`adaptive-layout.md`](adaptive-layout.md)):

- **WIDE** (`width >= 1000dp`): navigation rail + two columns —
  summary, performance and system information left; app components and
  recent logs right.
- **MEDIUM / COMPACT**: one scrollable column (top bar → summary cards →
  panels); the two-column split appears only at WIDE.
- `fontScale > 1.15` or `height < 360dp` degrade to COMPACT (the
  universal fallback), so constrained or unusual windows scroll instead
  of clipping.

## Panels and data sources

| Panel / card | Source |
|---|---|
| App status card | `appHealthFor(settingsStoreReadable, appListState)` — settings-store readability + installed-apps scan state |
| Performance profile card | stored `AppSettings.performanceProfile` vs effective `LocalPerformanceSpec`; divergence explained as "Selected: …" |
| Theme card | stored `AppSettings.themeId` + appearance mode |
| Permissions card | `buildAccessReport(Build.VERSION.SDK_INT, INTERNET, notificationAccessGranted)` + `summarizePermissions` / `grantedAccessCount` |
| FPS | `FrameTimeTracker.Snapshot.fps` (running-gated) |
| CPU | `SystemSnapshot.processCpuPercentOfCore` — process CPU as % of one core (`/proc/self/stat` deltas vs clock ticks) |
| App memory | `processMemoryPssMb` — `Debug.MemoryInfo` total PSS (process, not device) |
| Device memory | `deviceMemoryAvailableMb` / `deviceMemoryTotalMb` — `ActivityManager.MemoryInfo` |
| Storage | `storageAvailableMb` — `StatFs` on the data partition |
| Sample history | `FrameSampleHistory` — 60-sample FIFO, one 1-second window per sample |
| System information | `DeviceProfile` (manufacturer, model, release, API level, RAM, cores, density, dp size, tier) |
| App components | six tiles from the `ComponentHealth` builders over already-observed state (app drawer, media session, navigation handoff, settings store, frame tracking, embedded map) |
| Recent logs | `DiagnosticLog.entries`, newest first |

## Honesty rules

- Nullable readings render "Not readable" — never 0, never a guess; the
  four resource rows (CPU, app memory, device memory, storage) do this
  independently.
- FPS shows "Tracking disabled" when the tracker is not sampling and
  "Measuring…" until the first one-second window closes.
- The sparkline shows an honest empty state ("History builds while this
  screen samples.") until samples exist; its caption states the true
  count ("N samples · 1-second windows · newest right").
- Component tiles show real statuses, including LOADING ("Reading
  installed apps…" / "Reading media session state…") and NOT_CHECKED
  ("Checked during startup.") — not fabricated "OK" rows.
- The embedded map tile reports DISABLED with "Off by default; the
  preview is an experimental prototype." (it stays off — the Phase 7
  decision).
- "Refresh All" re-scans installed apps, re-reads the media session and
  takes one fresh resource sample; it logs "Manual refresh requested"
  and then "Refresh complete" (or WARN "Refresh finished with errors")
  and never toggles settings, launches anything or changes access.
- The page never calls `android.util.Log`; the bounded in-memory log is
  the only log.

## Diagnostics log

`core\diagnostics\DiagnosticLog.kt` — a 120-event ring, newest first,
monotonically increasing ids, messages sanitized (truncated to 160
characters + "…"). "Clear log" empties it and is deliberately not itself
recorded — an empty log means exactly that. The Recent logs panel
previews six rows (`LOG_PREVIEW_COUNT`) and "View all" opens the
full-screen in-app log detail with the event count, per-row level labels
(INFO / WARN / ERROR), "Clear log" and a Back affordance.

Logged events include app-list load state, manual refreshes, settings /
media / navigation transitions, and notification-access changes
(transition-only: the first observation on resume is state, not an
event).

## Lifecycle

- The 1-second resource-sampling loop (`setDiagnosticsSampling`) is
  driven by a resume-scoped effect in `AppRoot` — it runs only while the
  diagnostics screen is visible, and the last reading survives so the
  panel still shows it while paused. Frame tracking follows the Phase 9
  visibility rules (started only while a visible screen shows frame
  data, stopped otherwise).
- Each tracker `start()` opens a new session id; the ViewModel drops
  stale samples when the session changes, so the sparkline never mixes
  sessions.
- Notification-access changes are observed on resume and logged only on
  transitions.

## Tests

- Unit (JVM, all passing): `DiagnosticLogTest` (8),
  `FrameSampleHistoryTest` (4), `SystemSamplerTest` (8),
  `AccessReportTest` (7), `ComponentHealthTest` (10), `AppHealthTest`
  (4), `DiagnosticsLayoutPolicyTest` (8), `DiagnosticsSummaryTest` (12),
  plus 2 `formatLogTimestamp` cases in `TimeFormattersTest`.
- Instrumented: `DiagnosticsScreenTest` — 21 cases pinning layout
  (wide/compact), refresh behavior and the refreshing state, healthy and
  degraded summaries, the divergence note, permission gaps, honest
  "Not readable" readings, FPS rounding, sparkline empty/caption states,
  the log empty state, the six-row preview cap + View all detail, clear,
  detail Back (closes the overlay without firing the screen back),
  card-action routing, live component tiles, media permission gap,
  system-information values and level labels. Compile-checked via
  `assembleDebugAndroidTest` only — never executed.

## Verification status

Everything above is build-verified (328 unit tests green, lint 0
errors). On-device behavior is UNVERIFIED (no hardware): real
CPU/PSS/RAM readings, sampling cadence under load, sparkline smoothness,
log-detail ergonomics and notification-access flows require a device
session.
