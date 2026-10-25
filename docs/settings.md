# Settings Page (Phase 16)

Phase 16 rebuilds Settings to the reference design: a dark automotive
surface with seven real categories, an adaptive three-pane layout, and a
live dashboard preview. Every visible control changes real state and is
persisted through the existing `SettingsRepository` (DataStore); nothing on
the page is simulated.

Implementation:

- Screen: `app/src/main/java/com/autoverse/launcher/feature/settings/SettingsScreen.kt`
- Pane policy: `app/src/main/java/com/autoverse/launcher/core/design/SettingsPanePolicy.kt`
- Search catalog: `app/src/main/java/com/autoverse/launcher/feature/settings/SettingsCatalog.kt`
- Wired from `AppRoot.kt` (density override, notification-access state,
  `DashboardPreview` host)
- Unit tests: `SettingsPanePolicyTest` (8), `SettingsCatalogTest` (8),
  extended `AppSettingsTest` (3) and `SettingsMappingTest` (13)
- Instrumented pins: `SettingsScreenTest` (18 cases) — **compile-checked
  only, never executed** (no device)
- Previews: six `SettingsPreview` variants in `DesignPreviews.kt`
  (wide Night/Day/Neon Pulse, medium, compact Night/Day)

## Layout modes

`settingsPanePolicyFor(widthDp, heightDp, fontScale)` in
`core/design/SettingsPanePolicy.kt` — pure, table-tested:

| Constant | Value | Meaning |
|---|---|---|
| `MEDIUM_WIDTH_DP` | 600 | compact → medium threshold |
| `WIDE_WIDTH_DP` | 1000 | medium → wide threshold |
| `MIN_PANEL_HEIGHT_DP` | 360 | below → compact (short windows) |
| `MAX_SPLIT_FONT_SCALE` | 1.15 | above → compact (large text) |

Rules, in order: `fontScale > 1.15` → `COMPACT`; `height < 360` →
`COMPACT`; `width ≥ 1000` → `WIDE`; `width ≥ 600` → `MEDIUM`; otherwise
`COMPACT`. `COMPACT` is the universal fallback — constrained or unusual
windows degrade to one scrollable column, never clip.

| Mode | Structure |
|---|---|
| `WIDE` | Navigation rail + category pane (search + 7 entries, 260dp) + center controls (weight) + secondary pane (340dp: dashboard preview card + reset card) |
| `MEDIUM` | Shared top bar + category pane (240dp) + center controls with the preview (280dp) and reset cards appended below |
| `COMPACT` | Shared top bar + one scroll column: search field, horizontal category chips, category content, preview (280dp), reset |

All modes keep `touchMin = 48dp` targets and respect
`WindowInsets.safeDrawing`. The search field sits in the category pane
(WIDE/MEDIUM) or at the top of the scroll column (COMPACT).

## Categories

`SettingsCategory` (declaration order = display order): Appearance,
Navigation, Media & Audio, Apps & Shortcuts, Performance, Notifications,
About. Selection never touches unrelated state; it survives configuration
changes (the selected category is a `rememberSaveable` name, not an index).

| Category | Real controls |
|---|---|
| Appearance | Theme (5 swatch rows), Day/Night/System, text size slider (0.85×–1.30×, live % label, "Reset text size" when scaled), Adaptive layout (read-only "Automatic (Recommended)" — the real engine decides), 24-hour clock, Reduce motion |
| Navigation | Provider rows (System default, Google Maps, Waze, discovered apps), stale-preference notice when the saved package is gone. The experimental embedded-map switch was removed with the MapLibre prototype in 0.20.0 |
| Media & Audio | Notification-listener access state, explanation, route to Android settings; media-card description. No fake EQ or players |
| Apps & Shortcuts | Real persisted favorites (icon, launch, star = remove via the same store the drawer uses), empty-state hint, "Open app drawer" |
| Performance | Constrained / Balanced / Enhanced profiles, "Prefer smoothness" switch, Selected vs Effective workload rows, diagnostics route |
| Notifications | The same real notification-access block as Media & Audio |
| About | Brand plate, `BuildConfig.VERSION_NAME/VERSION_CODE` (never hardcoded), build type, stage, licenses pointer (`docs/third-party-licenses.md`), privacy summary, parked-use safety note |

## Text size

`AppSettings.textScale` (default 1.0f) is persisted as a float and clamped
to 0.85×–1.30× on both read and write (`SettingsMapping.textScaleFromStorage`,
`SettingsRepository.setTextScale`; malformed values fall back to 1.0f like
every other key).

`AppRoot` applies it once, app-wide:

```kotlin
CompositionLocalProvider(
    LocalPerformanceSpec provides performanceSpec,
    LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * settings.textScale),
)
```

Density scaling scales text without touching dp geometry, so the authoring
matrix (spacing, touch targets) stays intact. The layout engines receive
the *effective* font scale — past 1.15 the home dashboard switches to its
scroll-safe archetype and this page switches to `COMPACT`, exactly as they
do for an OS-level large-font setting.

## Dashboard preview

The right pane (WIDE) / appended card (MEDIUM, COMPACT) hosts the real
dashboard composition: `DashboardPreview` in `AppRoot.kt` renders
`HomeScreenContent` inside a `BoxWithConstraints` with the live
`homeLayoutPolicyFor(width, height, fontScale)`, the real theme/appearance
context, real favorites, and inert callbacks (no accidental launch from
inside settings). It updates the moment a theme, appearance, or text-size
change is committed — it is never a screenshot or static mock.

Frame sampling stays limited to HOME/DIAGNOSTICS screens: the miniature is
deliberately small, so status chips would not be legible there, and the
preview performs no map or media work.

## Search

`SettingsCatalog` lists every setting with its owning category;
`searchSettings(query)` matches title, category label, and subtitle
case-insensitively. A query swaps the center column for a results card;
tapping a result selects the owning category and clears the query. No
matches renders an explicit "NO MATCHES" card with a shorter-word hint.

## Reset

"Reset settings to defaults" opens a confirmation dialog (title "Reset
settings?"). Confirm calls `MainViewModel.resetSettings()`, which restores
every preference to defaults **and keeps favorites** (favorites live in
their own store and are never touched by the reset path). Cancel leaves
everything untouched. The card copy states the favorites guarantee before
the user commits.

## Notification-access state

`notificationAccessGranted` is read from
`isNotificationAccessEnabled(context)` on entry and re-read on every
`ON_RESUME` (the existing lifecycle observer in `AppRoot`), so returning
from Android's Notification access screen reflects the new state without a
restart. If `notificationAccessSettingsIntent()` cannot be opened, the
failure is surfaced inline instead of being swallowed. AutoVerse requests
no new permissions for any of this.

## Persistence keys

All keys live in the existing `SettingsRepository`: theme id, appearance
mode, reduced motion, performance profile, prefer-smoothness, 24-hour
clock, text scale (new in Phase 16), navigation package — plus the
separate favorites store. The `embedded_map` key stopped being read in
0.20.0 (prototype removed); stale values in existing installs are simply
ignored. Unknown or malformed stored values fall
back to app defaults per key (Phase 10 recovery behavior, extended and
unit-tested in Phase 16).

## Verification status

Unit + policy suites: 265 JVM tests, 0 failures (Phase 16 adds 20). Lint:
0 errors / 33 warnings. Previews render six settings variants. The 18
instrumented pins exist and compile (`assembleDebugAndroidTest`) but have
**never executed** — no device or emulator is available. Real insets,
touch ergonomics, and on-device DataStore round trips remain UNVERIFIED
(open Phase 0 hardware item).
