# AutoVerse Theme System — architecture, dynamic icons, and roadmap

Phase A deliverable of the dynamic-theme PRD: what exists, how the six
PRD identities map onto it, the dynamic icon engine design, and the
implementation plan with status.

## 1. Existing architecture (audited, not assumed)

| Layer | What exists | State |
| --- | --- | --- |
| Color tokens | `AutoVerseColors` (18 semantic tokens × dark/day per theme), verified by `ContrastTest` | Complete, 12 themes |
| Theme registry | `AutoVerseThemeSpecs` (data-driven `ThemeSpec`), `themeSpecFor`, unknown-ID fallback via `SettingsMapping` | Complete |
| Switching | Recomposition-only (no activity recreation), persisted in DataStore, survives restart | Complete |
| Cockpit themes | CSS custom properties per theme + static flair backdrops + profile tiering | Complete |
| Premium gating | `premiumUnlocked` local entitlement, PRO chips, early-access unlock | Complete |
| Icon rendering | **ThemeIconSpec engine** (Phase B): per-theme container shape/border/background/glow/selected-state, `ThemedAppIcon` wraps real app bitmaps in dock + drawer; Material glyphs remain on other surfaces (token-tinted, no hardcoded colors) | Implemented (dock/drawer/dock-rail coverage; other surfaces = next) |
| Hardcoded colors | Only `BrandPlate` (brand constants) + WebView background | Clean |
| Gallery | Cockpit: visual carousel with code-built previews + PRO chips. Compose: text rows with small swatches; live dashboard preview exists in Settings | Partial → Phase C |
| Sounds | None | Phase E |
| Wallpapers | Cockpit: per-theme CSS flair backdrops (static). Compose: background token gradients | Partial |

## 2. PRD identity mapping (six themes)

The PRD's six identities map onto the existing theme set as follows —
distinct visual identities all exist or are added; names are working names
(reviewed for trademark suitability per the PRD):

| PRD identity | AutoVerse theme | Status |
| --- | --- | --- |
| Dark Knight (stealth, amber/gold, angular shields) | **Gotham** | Exists — gains the shield icon language in Phase B |
| Stark Mode (reactor cyan, circular radial) | **Hot Rod** sibling → *Stark* container language applied via icon spec; Hot Rod keeps its red identity, a cyan-accented reactor variant ships as **Web Rush's sibling** below if needed | Partial — circular-radial icon language added in Phase B |
| Fortress (light, sky blue, soft rounded) | **Fortress** — new theme (the dedicated light identity) | Added (0.25.0) |
| Neo Tokyo (indigo/cyan/magenta, hex neon) | **Neon Tokyo** | Exists — gains hexagonal icon language |
| Web Rush (racing red + electric blue, sport angular) | **Web Rush** — new theme | Added (0.25.0) |
| Apex (professional graphite/blue, precision squares) | **Apex** (the original performance identity) | Exists — precision-square icon language |
| Original AutoVerse theme | **Cobalt** (flagship) — always available | Exists |

Premium placement: Fortress and Web Rush join the premium set (PRD §6 does
not forbid it; the free tier keeps Cobalt/Apex/Obsidian/Neon Pulse/Atelier).

## 3. Dynamic icon engine (PRD §4)

### 3.1 Separation of identity and appearance

`ThemeIconId` — a logical catalog (home, navigation, apps, phone, music,
media, settings, diagnostics, search, back, play, pause, previous, next,
favorite, bluetooth, wifi, theme, edit, saved, recent, …) independent of
styling. Installed third-party apps keep their **real PackageManager icons**;
themes style the **container** around them (shape mask, border, background,
glow, selection treatment) and never replace or recolor the artwork itself.

### 3.2 ThemeIconSpec

A data model on `ThemeSpec` (one per theme, no scattered `when(theme)`):

- `containerShape`: one of `ANGULAR_SHIELD`, `CIRCULAR_RADIAL`,
  `SOFT_ROUNDED`, `HEXAGONAL_NEON`, `SPORT_ANGULAR`, `PRECISION_SQUARE`
- `borderWidthDp`, `borderColorRole` (accent / outline / none)
- `backgroundStyle`: `TINTED` (accentSoft), `RAISED`, `INK` (near-black)
- `glow`: none / restrained / luminous (maps to shadow/elevation, honored
  only on Enhanced profile)
- `iconScale` (0.9–1.1 of base) and `selectedState`: `EDGE` / `RING` /
  `INDICATOR` (never color-only — PRD §4.7)

Shape rendering is a small, closed set of Compose `Shape` factories
(corner-radius treatments + `GenericShape` outlines for shield/hex/sport) —
data-driven, no per-theme drawing code.

### 3.3 ThemeIcon + installed-app containers

- `ThemeIcon(id, state)` — for the logical catalog: resolves the Material
  glyph + themed container + state treatment; respects reduced motion and
  the performance profile; contentDescription from the catalog.
- `ThemedAppIcon(packageKey, bitmap, state)` — wraps real app bitmaps in the
  theme container (clip shape, border, background, selected ring). Reuses
  the existing bounded icon cache and async loading; package IDs and launch
  intents untouched; missing icons fall back to the catalog glyph.
- Cockpit (WebView): the same language via CSS — per-theme
  `border-radius`/`clip-path` + border + box-shadow on `.apple-squircle`,
  favourite tiles and dock tiles, applied through the existing
  `body.theme-*` classes. Real app artwork (base64 from the bridge) is
  masked, never replaced.

### 3.4 Sizing rules

Container sizes stay fixed per surface (dock 64 dp, drawer 96 dp tiles,
rail 24 dp glyphs, status 16 dp); themes change shape/border, never touch
targets. Verified against the existing layout policy tests.

## 4. Sounds (PRD §8)

`ThemeSoundManager` — synthesized via `ToneGenerator`/`AudioTrack` short
tones (original by construction, zero bundled assets, zero size cost),
disabled by default, global mute, three events max (theme applied, panel
opened, action succeeded), never fires on repeated interactions, respects
audio focus (ducked stream), no mic permission. Cockpit: WebAudio oscillator
equiv turned off by default.

## 5. Plan and status (PRD §12 order)

| Phase | Scope | Status |
| --- | --- | --- |
| A | This audit + spec | Done |
| B | ThemeIconSpec + ThemedAppIcon (dock + drawer) + 5-shape factory set + tests; cockpit CSS icon language (clip-path masks + glows, profile-gated) | **Done (0.25.0)** |
| C | Gallery: cockpit carousel (12 visual cards, PRO chips, live switching) | Cockpit done; Compose gallery screen with preview-vs-applied split = next |
| D | Identities: Fortress + Web Rush added (12 themes total); icon languages for all | **Done (0.25.0)** |
| E | Sounds: ToneGenerator (Compose) + WebAudio (cockpit), off by default, rate-limited, Settings toggle | **Done (0.25.0, minimal)** |
| E+ | Wallpaper picker, accent variants, density, icon-pack import | Deferred (next pass) |
| F | Tests/build/sign + honest report | This pass |

Explicitly deferred with reasons: OS launcher-icon switching (PRD §4.8
requires explicit approval — separate feature), runtime downloads (forbidden
by PRD §11), bundled artwork wallpapers (APK-size impact must be measured;
CSS flair + token gradients cover the identity for now), full
ThemeCustomizationScreen (accent variants land with the wallpaper pass).
