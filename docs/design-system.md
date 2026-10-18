# AutoVerse Design System (Phase 2)

The single source of truth for AutoVerse's visual language. Everything in
`app/src/main/java/com/autoverse/launcher/core/design/` implements this
document; screens must consume tokens and components listed here instead of
hard-coding values.

- **Themes:** four value sets over one token schema — Obsidian (flagship:
  graphite surfaces, warm amber accent), Neon Pulse (electric cyan on
  midnight blue), Atelier (warm neutrals, clay accent), Apex (motorsport
  orange/red). Flat by design in every theme (no shadows; hierarchy comes
  from surface tone + hairline borders).
- **Day & night:** every token set ships a night palette and a daylight-legible
  day palette. Switching is a token swap, never a redesign.
- **Car-first ergonomics:** large touch targets (48dp minimum, 52–64dp for
  media transport), arm's-length readability, calm motion budgets.

## 1. Token architecture

| Layer | File | Contents |
|---|---|---|
| Color | `tokens/AutoVerseColors.kt` | `AutoVerseColors` data class, `ObsidianDarkColors`, `ObsidianDayColors` |
| Typography | `tokens/AutoVerseTypography.kt` | M3 `Typography` with cockpit scale |
| Spacing | `tokens/AutoVerseSpacing.kt` | Base-4 scale, semantic spacing, touch targets |
| Shapes | `tokens/AutoVerseShapes.kt` | `AutoVerseShapes` + `AutoVerseBorders` |
| Icons | `tokens/AutoVerseIcons.kt` | Icon size scale + status dots |
| Motion | `tokens/AutoVerseMotion.kt` | `MotionSpec`, `motionSpecFor`, `AutoVerseEasing` |
| Engine | `ThemeSpec.kt`, `AutoVerseTheme.kt`, `LayoutPolicy.kt` | Data-driven theme spec, composition locals, archetype policy |

Screens and components read colors via `LocalAutoVerseColors` and motion via
`LocalAutoVerseMotion`. **Color literals exist only in the token files.**

## 2. Color

### Obsidian Night (flagship, dark)

| Token | Hex | Usage |
|---|---|---|
| `backgroundTop` | `#05070B` | Screen gradient start |
| `backgroundBottom` | `#0C1018` | Screen gradient end |
| `surface` | `#0F141C` | Card gradient end, chips |
| `surfaceRaised` | `#141B26` | Card gradient start, controls |
| `surfaceHighlight` | `#1B2432` | Icon chips, pressed secondary |
| `outline` | `#232E3E` | Hairline borders |
| `outlineStrong` | `#2F3D51` | Emphasis borders |
| `accent` | `#F2B857` | Primary actions, active icons |
| `accentSoft` | `#33F2B857` | Hero chip fill (translucent at night) |
| `onAccent` | `#201607` | Text/icons on accent |
| `textPrimary` | `#E9EFF7` | Titles, values |
| `textSecondary` | `#9DACC0` | Body copy, labels |
| `textFaint` | `#7E8CA0` | Footnotes, captions |
| `disabledContainer` | `#1A2230` | Disabled fills |
| `disabledContent` | `#5A6678` | Disabled content |
| `success` | `#6FCF97` | Healthy status |
| `warning` | `#F2C94C` | Caution status |
| `danger` | `#EB6E63` | Fault status |

### Obsidian Day

| Token | Hex | Notes |
|---|---|---|
| `backgroundTop` | `#F4F6F9` | |
| `backgroundBottom` | `#E8ECF2` | |
| `surface` | `#FFFFFF` | |
| `surfaceRaised` | `#F7F9FC` | |
| `surfaceHighlight` | `#EDF1F7` | |
| `outline` | `#D8DFE8` | |
| `outlineStrong` | `#C3CDDA` | |
| `accent` | `#8F5E0F` | Deepened bronze for daylight legibility |
| `accentSoft` | `#F5E8D0` | Pre-blended (non-alpha) to keep compositing cheap |
| `onAccent` | `#FFFFFF` | |
| `textPrimary` | `#101721` | |
| `textSecondary` | `#48566B` | |
| `textFaint` | `#556577` | |
| `disabledContainer` | `#E3E8EF` | |
| `disabledContent` | `#98A2B3` | |
| `success` | `#1F8A4C` | |
| `warning` | `#966800` | Darkened in Phase 11 so warning text clears 4.5:1 on raised cards |
| `danger` | `#C0392B` | |

### Neon Pulse — night / day

Restrained electric cyan on deep midnight blue; the day variant deepens the
accent to teal so white-on-accent and accent-on-light stay legible.

| Token | Night | Day |
|---|---|---|
| `backgroundTop` | `#04070C` | `#F1F7FA` |
| `backgroundBottom` | `#0A1018` | `#E4EDF3` |
| `surface` | `#0C141E` | `#FFFFFF` |
| `surfaceRaised` | `#101B28` | `#F4F9FC` |
| `surfaceHighlight` | `#16273A` | `#E2EEF5` |
| `outline` | `#1E3040` | `#CFDDE7` |
| `outlineStrong` | `#2A4256` | `#B5C7D3` |
| `accent` | `#35E0FF` | `#0B6E8A` |
| `accentSoft` | `#3335E0FF` | `#D6EBF2` |
| `onAccent` | `#041E26` | `#FFFFFF` |
| `textPrimary` | `#E8F6FC` | `#0B1620` |
| `textSecondary` | `#A8C0D0` | `#40536A` |
| `textFaint` | `#8AA2B4` | `#4F6478` |
| `disabledContainer` | `#131E2B` | `#DCE7EF` |
| `disabledContent` | `#5A6E80` | `#8FA2B2` |
| `success` | `#5BD99F` | `#17804F` |
| `warning` | `#F2C94C` | `#8A6500` |
| `danger` | `#EB6E63` | `#C0392B` |

### Atelier — night / day

Warm charcoal and clay at night; warm paper with a deep clay accent by day.
Quiet surfaces for calm, low-glare reading.

| Token | Night | Day |
|---|---|---|
| `backgroundTop` | `#100D0A` | `#FAF6F0` |
| `backgroundBottom` | `#181410` | `#F1EAE0` |
| `surface` | `#1C1712` | `#FFFDFA` |
| `surfaceRaised` | `#231D16` | `#F7F1E8` |
| `surfaceHighlight` | `#2C241B` | `#EDE3D6` |
| `outline` | `#3A2F24` | `#E0D4C4` |
| `outlineStrong` | `#4C3E30` | `#CBBBA6` |
| `accent` | `#E0A87E` | `#9C5A2E` |
| `accentSoft` | `#33E0A87E` | `#F2E2CC` |
| `onAccent` | `#2A1A0C` | `#FFFFFF` |
| `textPrimary` | `#F4EDE4` | `#241C14` |
| `textSecondary` | `#C0B1A1` | `#5C5044` |
| `textFaint` | `#9C8C7B` | `#6B5E50` |
| `disabledContainer` | `#261F18` | `#E9E0D3` |
| `disabledContent` | `#6B5D4E` | `#A79A88` |
| `success` | `#76C79B` | `#1F7A48` |
| `warning` | `#E8C15A` | `#8A6200` |
| `danger` | `#D97A6C` | `#B23B2E` |

### Apex — night / day

Near-black asphalt with motorsport orange; day keeps the racing red on warm
white. Danger shifts to crimson so it never reads as the accent hue.

| Token | Night | Day |
|---|---|---|
| `backgroundTop` | `#0A0708` | `#FAF5F2` |
| `backgroundBottom` | `#140C0D` | `#F1E8E2` |
| `surface` | `#170E10` | `#FFFFFF` |
| `surfaceRaised` | `#1E1215` | `#F8F2ED` |
| `surfaceHighlight` | `#28181B` | `#EFE2DA` |
| `outline` | `#382226` | `#E4D4CB` |
| `outlineStrong` | `#4A2D33` | `#D0B8AB` |
| `accent` | `#FF6B2C` | `#C2320F` |
| `accentSoft` | `#33FF6B2C` | `#F7DCCF` |
| `onAccent` | `#2A0A05` | `#FFFFFF` |
| `textPrimary` | `#F7EEEA` | `#20140F` |
| `textSecondary` | `#C9ADA3` | `#5A4740` |
| `textFaint` | `#A68C80` | `#68534B` |
| `disabledContainer` | `#241518` | `#EADCD4` |
| `disabledContent` | `#70564F` | `#A8958A` |
| `success` | `#63D08C` | `#1F7A48` |
| `warning` | `#F2B84C` | `#8A5A00` |
| `danger` | `#E5484D` | `#B3261E` |

### Cobalt — night / day

The flagship palette (Phase 14): deep charcoal surfaces with the restrained
electric blue drawn from the brand mark; day keeps the blue deepened on cool
near-white. First entry in the settings picker and the default for new
installs.

| Token | Night | Day |
|---|---|---|
| `backgroundTop` | `#090C11` | `#F4F7FB` |
| `backgroundBottom` | `#0C1017` | `#E9EEF6` |
| `surface` | `#11151D` | `#FFFFFF` |
| `surfaceRaised` | `#161B25` | `#F2F5FA` |
| `surfaceHighlight` | `#1C2330` | `#E3EAF4` |
| `outline` | `#262E3C` | `#D2DAE6` |
| `outlineStrong` | `#354054` | `#B9C4D4` |
| `accent` | `#4A90F7` | `#1E63E9` |
| `accentSoft` | `#334A90F7` | `#DCE7FB` |
| `onAccent` | `#071523` | `#FFFFFF` |
| `textPrimary` | `#E8EDF4` | `#0B1526` |
| `textSecondary` | `#9AA7B8` | `#3E4C61` |
| `textFaint` | `#78879C` | `#55647A` |
| `disabledContainer` | `#1B2230` | `#E2E8F2` |
| `disabledContent` | `#5C6978` | `#94A1B3` |
| `success` | `#57D3A1` | `#0E7A4B` |
| `warning` | `#E8B04B` | `#8A5B00` |
| `danger` | `#F0716A` | `#C33B32` |

### Contrast policy (enforced by `ContrastTest`)

WCAG 2.x relative-luminance ratios, checked for every registered palette
(currently 10 — five themes × night/day):

- `textPrimary` on all four backdrop surfaces ≥ **7:1**
- `textSecondary`, `textFaint` on all four backdrops ≥ **4.5:1**
- `onAccent` on `accent` ≥ **4.5:1**
- `accent`, `success`, `warning`, `danger` on `backgroundTop` and
  `accent` on `surfaceHighlight` ≥ **3:1** (iconography / non-text)

Disabled tokens are intentionally low-contrast (they signal non-interactivity)
and are exempt — never use them for readable content.

## 3. Typography

Material 3 `Typography` overridden in `AutoVerseTypography`. Key styles:

| Style | Size / weight | Usage |
|---|---|---|
| `displayLarge` | 64sp Light, tabular | (reserved, media/lock clock) |
| `displayMedium` | 44sp Light, tabular | Clock |
| `headlineMedium` | 26sp SemiBold | Screen titles |
| `titleLarge` | 20sp SemiBold | Card headlines |
| `titleMedium` | 16sp Medium, tabular | Labels, values, rows |
| `bodyLarge` | 15sp Regular | Reading copy |
| `bodyMedium` | 13sp Regular | Supporting copy |
| `labelLarge` | 13sp Medium, +1.2sp tracking | Section labels, wordmark |
| `labelMedium` | 11sp Medium, +1.4sp tracking | Chips, footnotes |

Numeric styles carry `fontFeatureSettings = "tnum"` so clocks, FPS and
diagnostics never jitter as digits change.

## 4. Spacing & touch targets

Base unit **4dp**. Scale: `xxs 4 · xs 8 · sm 12 · md 16 · lg 20 · xl 24 · xxl 32`.

Semantic tokens: `screenHorizontal 20`, `screenVertical 16`,
`cardPadding 20`, `cardPaddingDense 16`, `stackGap 12`, `gridGap 12`,
`inlineGap 8`, `chipHorizontal 12`, `chipVertical 8`, `railWidth 96`
(home navigation rail, Phase 14).

Touch targets: `touchMin 48`, `touchComfort 56`, `transportPrimary 64`,
`transportSecondary 52`, `iconChip 36`, `heroChip 84`.

## 5. Shapes & borders

`card 24dp · tile 20dp · row 14dp · small 12dp · pill`. Borders:
`hairline 1dp`, `emphasis 2dp`. **No elevation tokens** — flat hierarchy.

## 6. Icons

Sizes: `inline 16 · compact 20 · standard 24 · prominent 32 · hero 40`;
status dots `dot 8 · dotLarge 10`. Section labels use accent-tinted
`inline` icons; status chips color their icon/dot by tone.

## 7. Motion

| Token | Default | Reduced |
|---|---|---|
| `fastMs` | 120 | 0 |
| `standardMs` | 180 | 0 |
| `emphasizedMs` | 220 | 0 |

Easing: `AutoVerseEasing.standard` (FastOutSlowIn) and `.linear`. Total
motion budget stays ≤250ms — calm cabin, no attention-grabbing effects.
`reducedMotion` flows from settings → `motionSpecFor()` → `LocalAutoVerseMotion`;
screen transitions in `AppRoot` already honor it (fade instead of slide).

## 8. Theme engine

- `ThemeSpec(id, displayName, darkColors, lightColors)` is a pure data object.
- `AutoVerseThemeSpecs` is the registry of all themes in settings-picker
  order; `themeSpecFor(themeId)` resolves one and `colorsFor(darkTheme)`
  picks the palette. `ThemeSpecTest` fails if an enum value is unregistered
  or two themes share a palette.
- `isDarkTheme(appearanceMode, systemInDark)` resolves `SYSTEM / DAY / NIGHT`.
- `AutoVerseTheme(themeId, appearanceMode, reducedMotion)` provides
  `LocalAutoVerseColors` + `LocalAutoVerseMotion` and the M3 color scheme.
  Changing any input only recomposes the subtree — the activity is never recreated.
- Themes differ only in color values — same spacing, typography, shapes, icon
  sizes, motion budgets and component set. No theme introduces glow, particles
  or per-theme effects, and all five honor `reducedMotion`.

## 9. Layout policy

`layoutClassFor(widthDp)`: `<600 COMPACT · 600–839 MEDIUM · ≥840 EXPANDED`.
Kept only as a width bucket for diagnostics; it no longer drives the
dashboard.

The dashboard policy is width **and** height **and** font-scale aware:
`homeLayoutPolicyFor(widthDp, heightDp, fontScale)` — full rules,
supported display matrix and clipping guarantees in
[`adaptive-layout.md`](adaptive-layout.md).

| Archetype | When | Navigation | Status chips |
|---|---|---|---|
| `MINIMAL` | height < 240dp | compact action row only | hidden |
| `COMPACT_SCROLL` | width < 600dp, short height, or large font | stacked, scrollable | hidden |
| `SPLIT_WITH_ACTIONS` | ≥600 × 380dp | nav + media split | from 560dp tall, progressive |
| `RAIL_SPLIT` | ≥840 × 380dp | rail + media/dock column | from 480dp tall, progressive |

Navigation density follows height: the `FULL` hand-off card (hero marker +
destination field) from 480dp (RAIL/scroll) or 560dp (split), the denser
`COMPACT` card below that or on large font scales; `MINIMAL` uses the
single-row `CompactNavigationRow`. The `COMPACT_SCROLL` and `MINIMAL`
archetypes are scroll-safe, so no supported size can clip the dashboard.

## 10. Component catalog

All in `core/design/components/`:

| Component | Purpose | States |
|---|---|---|
| `CockpitCard` | Base surface (gradient + hairline border) | static / pressed (border → accent) / disabled |
| `PrimaryAction` | Filled accent action, 56dp min | enabled / pressed / disabled |
| `TransportButton` (`PRIMARY 64dp` / `SECONDARY 52dp`) | Media transport | enabled / pressed / disabled |
| `StatusChip` (`StatusTone`) | Pill status indicator | neutral / success / warning / danger |
| `WidgetContainer` | Titled section card | with optional subtitle / icon / click |
| `SectionLabel` | Accent icon + tracked label | — |
| `AppDockItem` | Vertical shortcut tile: icon chip above a centered label (vector or real app icon bitmap) | enabled / pressed / disabled |
| `BrandPlate` | The AutoVerse brand mark on its live-color plate (`#141B22`, theme-independent by design) | fixed |
| `DriveModeCard` | DRIVE MODE selector — the real performance profiles as radio rows (Phase 14) | selected / pressed |
| `AppTile` | App-drawer grid tile: icon (bitmap or fallback), 2-line label, star favorite toggle | normal / favorite / no-icon |
| `AppListRow` | App-drawer list row: icon + label + star favorite toggle (Phase 15) | normal / favorite / no-icon |
| `DashboardTopBar` | Shared home/drawer top bar: brand plate, clock/date, Wi-Fi/Bluetooth indicators, diagnostics, optional back affordance, optional status chips (Phase 15) | compact / full / back-affordance / chips NONE·REDUCED·FULL |
| `AppNavigationRail` | Shared navigation rail (home + drawer MEDIUM/EXPANDED): Home / Apps / Navigation / Settings entries (Phase 15) | selected (tab) |
| `MediaCard` | Media surface for the featured media session (Phase 5; time labels Phase 14) | access-required / no-session / connecting / error / active (playing, paused, buffering, stopped, playback error) |
| `NavigationCard` / `CompactNavigationRow` | Navigation hand-off: live-validated destination field + Navigate/Open actions (Phase 6) | supports-destination / open-only / preferred-missing warning / dense |
| `KeyValueRow` / `SelectRow` / `SwitchRow` | Settings & diagnostics rows | selected / enabled / disabled |
| `AutoVerseScreen` / `ScreenHeader` | Gradient scaffold + back header | — |

Rules: components take no color parameters — they always read
`LocalAutoVerseColors`. New UI must be built from this catalog; if something
is missing, add a component here rather than styling inline.

## 11. Previews

`app/src/debug/java/.../design/previews/DesignPreviews.kt` (debug build only):

- Palette boards — Night & Day swatch sheets.
- Component gallery — every component with all states, Night & Day.
- Home dashboard — one preview per archetype × appearance: Compact
  (411×891), Split full (800×600), Split short (800×400), Rail (1280×800)
  and Minimal (800×220), all in Night and Day (except the split-short
  stress case, Night only); the dock row shows sample favorites, the
  header shows sample Wi-Fi/Bluetooth indicators, and the drive-mode
  selector appears wherever the layout carries it.
- App drawer (Phase 4, rebuilt Phase 15) — Ready (rail + compact, Night &
  Day), Loading, Empty, Error, grid, list, no-favorites and
  filtered-empty states with clearly fictional sample apps; the previews
  drive the same view/category/query state the screen owns.
- Media (Phase 5) — MEDIA CARD STATES section in the component gallery
  (active sample session + access-required, Night & Day) and a home rail
  preview with an active media session.
- Navigation (Phase 6) — NAVIGATION CARD STATES section in the component
  gallery (Google Maps full, Waze dense, preferred-missing warning,
  open-only, compact rows) and two home rail previews (Maps selected,
  preferred app missing).
- Themes (Phase 8, extended Phase 14) — palette boards for Cobalt (the
  flagship), Neon Pulse, Atelier and Apex (Night & Day), plus component
  gallery, home rail and app-drawer previews per theme, including a
  Cobalt rail preview.

## 12. Tests & guardrails

| Test | Enforces |
|---|---|
| `ContrastTest` | WCAG thresholds above, every registered palette (10: five themes × night/day) |
| `SpacingTest` | Scale is ascending / base-4; semantic mapping; touch ordering |
| `MotionTest` | Duration ordering, ≤250ms budget, reduced motion zeroes, selection |
| `ThemeSpecTest` | Registration covers every theme exactly once; id/display-name match; no shared palettes; palette selection, appearance-mode logic |
| `HomeLayoutPolicyTest` | Table-driven archetype/density/chip rules: width, height and font-scale boundaries, unusual aspect ratios, degenerate windows, invariant sweep |
| `AppCatalogTest` | Drawer list rules: exclude self, one tile per package, case-insensitive sort, search (label + package), category filter, combined query+category, newly-installed ordering/limits, favorites resolution/pruning order |
| `AppCategoryTest` | Chip labels and declaration order pinned; new apps default to a concrete category (never the ALL sentinel) |
| `AppCategoryClassifierTest` | Declared-category mapping, intent-category fallback, multi-match priority, gallery→OTHERS, never ALL, probe order pinned |
| `DrawerLayoutPolicyTest` | Drawer width/height/font-scale boundaries, unusual aspect ratios, degenerate windows, threshold constants, invariant sweep |
| `FavoritesCodecTest` | Favorites persistence format: order, round trip, self-healing from blanks/dupes/CRLF |
| `LaunchOutcomeTest` | Launch failure classification and snackbar wording |
| `PlaybackStatusTest` | Playback-state → status mapping and `isPlaying` definition |
| `MediaActionTest` | Action-mask expansion, capability gating, toggle selection |
| `MediaSessionSelectorTest` | Featured-session selection order and tie behavior |
| `MediaPresentationTest` | Media card copy for every state; metadata fallbacks |
| `MediaProgressTest` | Position extrapolation, clamping, unknown-duration handling |
| `MediaCommandTest` | Command-failure classification and snackbar wording |
| `MediaAccessParsingTest` | Notification-access settings string parsing |
| `MediaArtworkSamplingTest` | Artwork downsample sizing (power-of-two, ≥256 px) |
| `NavigationDestinationParsingTest` | Destination parsing: coordinates vs query, ranges, 512-char cap, formatting |
| `NavigationUriEncodingTest` | RFC 3986 percent-encoding (UTF-8, `%20` spaces, reserved passthrough) |
| `NavigationProviderUriTest` | Per-provider documented URI construction (geo, Maps, Waze) |
| `NavigationProviderResolutionTest` | Preferred-provider resolution; stale preference → system default + warning |
| `NavigationOutcomeTest` | Launch-failure classification and snackbar wording |
| `AppSettingsTest` | Defaults (Cobalt, System, motion on, Balanced, 24h, system-default navigation); five-theme label list |
| `SettingsMappingTest` | Persisted theme id round-trip; missing/unknown/case-mismatched values fall back to the app default |
| `LayoutClassTest` | Breakpoints |
| `ConnectivityStatusTest` | Bluetooth readability gate (≥ API 31 needs the never-requested `BLUETOOTH_CONNECT`); link states default to hidden `UNKNOWN` |
| `TimeFormattersTest` | Clock/date formats and `formatTrackTime` (`m:ss`, `h:mm:ss` from 1h, negatives clamp to `0:00`) |

## 13. Extending

- **New theme:** add the palette pair to `AutoVerseColors.kt`, register a
  `ThemeSpec` in `AutoVerseThemeSpecs`, and add the enum value — `ContrastTest`
  and `ThemeSpecTest` then hold the new theme to every rule automatically.
- **New component:** live in `components/`, take no colors, expose explicit
  pressed/disabled states, use spacing/shape/icon tokens, add a preview row.
- **New motion:** add to `MotionSpec` and justify against the ≤250ms budget.

## 14. Known limitations

- Themes are color-only by design (no per-theme layouts or decorative
  effects); palette legibility is contrast-tested, but real cabin-light
  legibility on the target head unit is unverified (no hardware).
- No screenshot-based visual regression tests yet (added in a later phase).
- On-device performance (frame timing on the target head unit) is unverified —
  no hardware available during Phase 2.
- Media session behavior (discovery, artwork, transport) is unit-test-verified
  only — real players and OEM notification-access flows need hardware
  (see [`media-integration.md`](media-integration.md) §8).
- Navigation hand-off is unit-tested only; the platform offers no completion
  callback, and real intent resolution (geo: chooser, OEM nav apps) needs
  hardware (see [`navigation-handoff.md`](navigation-handoff.md) §9).

## 15. Phase 16 refresh — settings page

The settings page was rebuilt to the reference design without changing the
theme engine, tokens, or motion budgets.

- `SettingRows.kt` gained `SliderRow` (label, live value display, inline
  slider — used by text size) alongside the existing `KeyValueRow` /
  `SelectRow` / `SwitchRow`; `SelectRow` gained an optional leading slot,
  used by the page-local theme swatch (28dp accent dot on the theme
  background, hairline border).
- The page composes the existing catalog only: `CockpitCard`,
  `WidgetContainer`, `SectionLabel`, `BrandPlate`, `DashboardTopBar`,
  `AppNavigationRail` and the settings rows. No new colors or inline
  styling; the pane policy lives in `core/design/SettingsPanePolicy.kt`.
- An app-wide text-size setting (0.85×–1.30×, persisted, clamped, reset
  affordance when scaled) scales density font scale once in `AppRoot`
  (`LocalDensity` override) — text scales; dp geometry (spacing, touch
  targets, icons) does not.
- New guardrail tests: `SettingsPanePolicyTest` (pane boundaries,
  constants, invariant sweep) and `SettingsCatalogTest` (catalog coverage,
  search semantics). Full rules in [`settings.md`](settings.md).
- Previews: six `SettingsPreview` variants — wide 1280×800 Night / Day /
  Neon Pulse, medium 800×600 Night, compact 411×891 Night / Day — each
  hosting the live `HomeScreenContent` mini-preview on real
  `homeLayoutPolicyFor` output.
- Verification status: `SettingsScreenTest` (18 cases) is compile-checked
  via `assembleDebugAndroidTest` only — never executed (no device); real
  insets, touch ergonomics and DataStore round trips on hardware remain
  UNVERIFIED.

## 16. Phase 17 refresh — diagnostics page

The diagnostics page was rebuilt to the reference design using existing
system components only; no new colors, type roles, spacing steps or
motion tokens were introduced.

- Composition: `WidgetContainer`, `KeyValueRow`, `StatusChip`,
  `TextAction`, `PrimaryAction`, `AppNavigationRail` and the shared
  `AutoVerseScreen` / `ScreenHeader` / `DashboardTopBar` chrome. The
  sample-history sparkline is page-local `Canvas` drawing in theme colors
  (outline/status tokens), not a new component type.
- Status and copy mapping is pure text in
  `feature/diagnostics/DiagnosticsSummary.kt` — card labels, reading
  formatters (null → "Not readable", never 0) and the profile-divergence
  note ("Selected: …" when the stored profile differs from the effective
  one).
- The pane policy lives in `core/design/DiagnosticsLayoutPolicy.kt`
  (`diagnosticsPanePolicyFor`), mirroring `SettingsPanePolicy` thresholds
  (600/1000dp width, 360dp height, 1.15 max split font scale). Full rules
  in [`adaptive-layout.md`](adaptive-layout.md).
- New guardrail tests: `DiagnosticsLayoutPolicyTest` (pane boundaries,
  constants, invariant sweep) and `DiagnosticsSummaryTest` (12 cases over
  labels and formatters). Page rules in
  [`diagnostics.md`](diagnostics.md).
- Previews: eight `DiagnosticsPreview` variants — wide 1280×800 Night /
  Day / Neon Pulse, medium 800×600, compact 411×891 Night / Day, plus
  empty-log and first-run states — all on fictional sample data.
- Verification status: `DiagnosticsScreenTest` (21 cases) is
  compile-checked via `assembleDebugAndroidTest` only — never executed
  (no device); real readings, sampling cadence and touch ergonomics on
  hardware remain UNVERIFIED.
