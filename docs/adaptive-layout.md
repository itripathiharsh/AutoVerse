# Adaptive Layout Engine (Phase 3)

Phase 3 replaces the width-only layout bucket with a deterministic policy
that decides the dashboard structure from the **actual window space**,
orientation, and font scale. Every decision is a pure function of its
inputs — same inputs, same layout — so behaviour is testable and previewable
without hardware.

Implementation: `app/src/main/java/com/autoverse/launcher/core/design/LayoutPolicy.kt`
Tests: `app/src/test/java/com/autoverse/launcher/core/design/HomeLayoutPolicyTest.kt`
Previews: `app/src/debug/java/com/autoverse/launcher/core/design/previews/DesignPreviews.kt`

## Inputs

`HomeScreen` measures the dashboard with `BoxWithConstraints`, then subtracts
the `WindowInsets.safeDrawing` insets (dp, from `LocalDensity`) and passes the
result to:

```
homeLayoutPolicyFor(widthDp: Int, heightDp: Int, fontScale: Float = 1f)
```

- `widthDp` / `heightDp` — window space actually available to the dashboard,
  after insets; clamped to ≥1 (degenerate windows resolve to `MINIMAL`).
- `fontScale` — `density.fontScale`; >1.15 disables split archetypes.

Orientation is not an input: a 1280×800 landscape and a 800×1280 portrait
window are both handled by the same width/height rules, and the policy is
recomputed on every configuration change (`configChanges` in the manifest
already covers orientation/size/`uiMode`/`density`, so the activity is never
recreated and preferences are untouched).

## Decision table

`HomeLayoutRules`:

| Constant | Value | Meaning |
|---|---|---|
| `MEDIUM_WIDTH_DP` | 600 | compact → split threshold |
| `EXPANDED_WIDTH_DP` | 840 | split → rail threshold |
| `MIN_COMPACT_HEIGHT_DP` | 240 | below → `MINIMAL` |
| `MIN_SPLIT_HEIGHT_DP` | 380 | minimum height for any split |
| `RAIL_FULL_HEIGHT_DP` | 480 | full density / chips gate (rail, scroll) |
| `SPLIT_FULL_HEIGHT_DP` | 560 | full density / chips gate (split) |
| `CHIPS_REDUCED_WIDTH_DP` | 720 | below → no chips |
| `CHIPS_FULL_WIDTH_DP` | 1100 | full chip set threshold |
| `MAX_SPLIT_FONT_SCALE` | 1.15 | above → scroll-safe column |

Archetype, in order:

1. `height < 240` → `MINIMAL`
2. `fontScale > 1.15` → `COMPACT_SCROLL`
3. `width ≥ 840 && height ≥ 380` → `RAIL_SPLIT`
4. `width ≥ 600 && height ≥ 380` → `SPLIT_WITH_ACTIONS`
5. otherwise → `COMPACT_SCROLL`

Navigation density (`FULL` hero card vs `COMPACT` row): `COMPACT` for
`MINIMAL`, for any large font scale, and below the archetype's full-height
gate (480dp for rail/scroll, 560dp for split); `FULL` at or above it.

Status chips: `COMPACT_SCROLL` and `MINIMAL` never show chips. Split
archetypes show them only at their full-height gate; then `width < 720` →
`NONE`, `width ≥ 1100` → `FULL` (theme, profile, tier, FPS), else `REDUCED`
(theme, FPS).

## Why the height gates

The audit measured the fixed-height content the dashboard must place:

- Navigation hero card (icon chip 36dp + two text rows + 84dp hero chip +
  two 56dp actions) ≈ 336dp; the `COMPACT` density row is 52dp + action.
- Media card ≈ 300dp in its full state; `dense` drops the hint and footer.
- Header (brand plate, link indicators, clock, diagnostics), dock row and
  rail ≈ 64 + 96 + 56dp.

380dp is the smallest height at which a split can honour the nav + media
column without clipping (dense media + compact nav); 480/560dp are where the
full-density variants fit with the chip strip; 240dp is the smallest height
in which the `MINIMAL` action row is still usable. All fixed children keep
`touchMin = 48dp` targets at every supported size.

## Supported matrix (documented, self-declared)

| Window | Policy |
|---|---|
| 411×891 portrait (baseline phone) | `COMPACT_SCROLL`, FULL density |
| 700×900, 800×600 (medium landscape) | `SPLIT_WITH_ACTIONS`, FULL / REDUCED chips as height allows |
| 800×400 (short medium) | `SPLIT_WITH_ACTIONS`, COMPACT density, no chips |
| 1280×800, 1024×600 (wide) | `RAIL_SPLIT` (+ chips per width/height gates) |
| ≤239dp tall (windowed / split-screen stress) | `MINIMAL` |
| Any size at fontScale > 1.15 | `COMPACT_SCROLL` |

`COMPACT_SCROLL` and `MINIMAL` scroll vertically and never split
nav/media side-by-side, so they cannot clip at any size: this is the
guaranteed fallback. Unknown or extreme aspect ratios therefore degrade to a
scroll-safe layout rather than overlapping controls (FR-HOME-003/004).

## Guarantees

- No overlapping controls or icon/text overlap at any supported size — the
  invariant sweep in `HomeLayoutPolicyTest` asserts the structural
  conditions (MINIMAL only below 240dp; splits require ≥600×380; FULL
  density and chips require their height gates; large fonts never split).
- No horizontal scrolling for the main dashboard (primary and minimal
  layouts use full-width columns; the compact scroll uses one column).
- Text that could still overrun its box is ellipsized via `maxLines` +
  `TextOverflow.Ellipsis` (nav title/body, clock, media title/hint, chip
  labels, section labels, card subtitles).
- Layout changes never reset preferences: state lives in `SettingsRepository`
  / `MainViewModel`, and the manifest handles configuration changes without
  recreation.

## Hardware caveat

All Phase 3 verification is via unit tests and Compose previews; **no target
head unit was available**, so frame timing, real insets, and physical touch
ergonomics on the declared matrix remain unverified on hardware (open
Phase 0 audit item). The declared matrix above is the reference to validate
against when a device exists.

## Phase 14 refresh — home dashboard structure

The redesign kept every policy input and gate unchanged; only the
composition inside each archetype changed:

- The full-width header (brand plate, Wi-Fi/Bluetooth indicators, clock,
  diagnostics) sits above all layouts; the `RAIL_SPLIT` rail starts below
  it and uses the `railWidth 96` token.
- `RAIL_SPLIT` and `SPLIT_WITH_ACTIONS` gained a bottom row: quick actions
  and the DRIVE MODE selector (the real performance profiles) at 1:1 width.
- The DRIVE MODE card is ~230dp tall, so at the smallest split heights
  (380–480dp) the nav/media row above it is squeezed; `COMPACT` navigation
  density applies there and the row keeps its weight-based split. This is
  accepted pending hardware review.
- `COMPACT_SCROLL` places the drive-mode selector at the end of the scroll
  column; `MINIMAL` is unchanged.

## Phase 15 refresh — app drawer policy

The drawer has its own pure policy, `appDrawerLayoutPolicyFor(widthDp,
heightDp, fontScale)` in `core/design/DrawerLayoutPolicy.kt`, table-tested
by `DrawerLayoutPolicyTest` with the same boundary+sweep discipline as the
home policy. Rules, in order:

1. `fontScale > 1.15` → `COMPACT` (large text: single column, no rail).
2. `height < 360` → `COMPACT` (short strips: central panel only).
3. `width >= 1000` → `EXPANDED` (rail + app grid + favorites side panel,
   300dp fixed right column).
4. `width >= 600` → `MEDIUM` (rail + app grid).
5. otherwise → `COMPACT` (grid with top navigation, back affordance in
   the shared `DashboardTopBar`).

`COMPACT` is the universal fallback, so constrained or unusual windows
degrade to a single scrollable column instead of clipping. Grid columns
are `GridCells.Adaptive(124.dp)` in every mode; the drawer does not add a
second grid policy. The rail in MEDIUM/EXPANDED is the same
`AppNavigationRail` component the home dashboard uses.

Like the home policy, this is verified by unit tests and Compose previews
only — real insets and touch ergonomics on the target head unit remain
unverified (no hardware; open Phase 0 audit item).

## Phase 16 refresh — settings pane policy

Settings has its own pure policy, `settingsPanePolicyFor(widthDp,
heightDp, fontScale)` in `core/design/SettingsPanePolicy.kt`, table-tested
by `SettingsPanePolicyTest` with the same boundary discipline as the home
and drawer policies. Rules, in order:

1. `fontScale > 1.15` → `COMPACT` (large text: one scroll column).
2. `height < 360` → `COMPACT` (short strips).
3. `width >= 1000` → `WIDE` (rail + category pane + controls + 340dp
   preview/reset pane).
4. `width >= 600` → `MEDIUM` (category pane + controls with the preview
   and reset cards appended below).
5. otherwise → `COMPACT` (top bar + search + horizontal category chips +
   single scroll column).

`COMPACT` is the universal fallback, so constrained or unusual windows
degrade to a single scrollable column instead of clipping. Unlike the home
and drawer policies, the input `fontScale` is the *effective* scale — the
Phase 16 app-wide text-size setting multiplies the density font scale in
`AppRoot`, so a large in-app text size degrades the settings page and the
dashboard consistently. Verified by unit tests and Compose previews only —
real insets and touch ergonomics on the target head unit remain unverified
(no hardware; open Phase 0 audit item).

## Phase 17 refresh — diagnostics pane policy

Diagnostics has its own pure policy, `diagnosticsPanePolicyFor(widthDp,
heightDp, fontScale)` in `core/design/DiagnosticsLayoutPolicy.kt`,
table-tested by `DiagnosticsLayoutPolicyTest` with the same boundary
discipline as the home, drawer and settings policies. Rules, in order:

1. `fontScale > 1.15` → `COMPACT` (large text: one scroll column).
2. `height < 360` → `COMPACT` (short strips).
3. `width >= 1000` → `WIDE` (navigation rail + performance/system column
   + components/logs column).
4. `width >= 600` → `MEDIUM` (single scroll column; the reference
   two-column body appears only at `WIDE`).
5. otherwise → `COMPACT` (top bar + summary cards + panels in one
   scrollable column).

`COMPACT` is the universal fallback, so constrained or unusual windows
degrade to a single scrollable column instead of clipping. The input
`fontScale` is the *effective* scale (the Phase 16 app-wide text-size
setting multiplies the density font scale in `AppRoot`), so a large
in-app text size degrades the diagnostics page consistently with settings
and the dashboard. Verified by unit tests and Compose previews only — real
insets and touch ergonomics on the target head unit remain unverified (no
hardware; open Phase 0 audit item).

## Phase 18 refresh — navigation pane policy

The navigation page has its own pure policy, `navigationPlacesLayoutFor(
widthDp, heightDp, fontScale)` in `core/design/NavigationLayoutPolicy.kt`,
table-tested by `NavigationLayoutPolicyTest` with the same boundary
discipline as the home, drawer, settings and diagnostics policies. Rules,
in order:

1. `fontScale > 1.15` → `COMPACT` (large text: stacked body, no side
   panel).
2. `height < 360` → `COMPACT` (short strips).
3. `width >= 1000` → `WIDE` (map dominates; search surface separate; the
   Recents + Saved Places panel is a 340dp side panel next to the map).
4. `width >= 600` → `MEDIUM` (map with a compact 280dp side panel).
5. otherwise → `COMPACT` (map above, places panel stacked below).

In every mode the places panel carries a stacked-height budget of
`(heightDp * 0.45).coerceIn(180, 320)` dp so the map keeps priority when
the panel is visible. The panel's hide/show toggle reflows the map into the
freed space — the map surface itself is not recreated by the toggle, by a
query change or by the mode change (it lives in one full-size box whose
constraints change). `COMPACT` is the universal fallback, so constrained or
unusual windows stack instead of clipping. The input `fontScale` is the
*effective* scale (app-wide text-size setting applied in `AppRoot`),
consistent with settings, diagnostics and the dashboard. Verified by unit
tests (`NavigationLayoutPolicyTest`, 14 cases) and Compose previews only —
real insets and touch ergonomics on the target head unit remain unverified
(no hardware; open Phase 0 audit item).
