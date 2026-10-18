# Safety & Accessibility Audit (Phase 11)

Phase 11 reviewed the launcher for driving-safety, accessibility and
reduced-motion behavior across every screen, all four layout archetypes
(minimal / compact-scroll / split / rail-split) and all eight themes. This
document records what was found, the exact fixes applied, the automated
tests that now guard the fixes, and what still requires a physical device.

Implementation touched: `core/design/tokens/AutoVerseMotion.kt`,
`core/design/components/SettingRows.kt`, `core/design/components/Buttons.kt`,
`core/design/components/CockpitCard.kt`, `MainViewModel.kt`, `AppRoot.kt`,
`feature/settings/SettingsScreen.kt`.
Tests touched: `core/design/ContrastTest.kt`, `core/design/MotionTest.kt`.

## Findings and fixes

**F1 — Custom controls had no screen-reader roles.** All interactive
components built on `Modifier.clickable` announced as generic clicks;
row-style selectors and switches announced as plain text. Fixed:

- `SettingRows.kt` — `SelectRow` now uses `Modifier.selectable(...)` with
  `role = Role.RadioButton` and enforces `AutoVerseSpacing.touchMin`; the
  inner `RadioButton` is a passive visual (single merged semantics node).
  `SwitchRow` now makes the whole row `Modifier.toggleable(...)` with
  `role = Role.Switch` (the `Switch` child is display-only), so both the
  label and the switch are one large, correctly-announced target.
- `Buttons.kt` — `TransportButton` and `TextAction` set
  `role = Role.Button`.
- `CockpitCard.kt` — the card's `clickable` sets `role = Role.Button`.

**F2 — Reduced motion ignored the system-wide setting.** The app honored
its own `reducedMotion` preference, but not Android's global
"remove animations" developer/OEM setting. Fixed with
`effectiveReducedMotion(userSetting, systemAnimationsDisabled)` in
`AutoVerseMotion.kt` (union of both), backed by a `ContentObserver` on
`Settings.Global.ANIMATOR_DURATION_SCALE` owned by `MainViewModel`
(registered in `init`, unregistered in `onCleared`, failure-safe: an
unreadable setting is treated as "animations enabled") and wired through
`AppRoot`. `MotionTest` now locks the four-case truth table.

**F3 — No safety notice (FR-SAFE-006).** Settings now opens with a SAFETY
card: "Configure while parked — set themes, shortcuts, and destinations
before you set off. Do not interact with the screen while driving, and
obey local traffic laws." The ABOUT section identifies the build stage.

**F4 — Contrast policy gap for state colors used as text.** The existing
policy covered state colors as *markers* (3:1) against page backgrounds,
but `TextAction`, `NavigationCard` status text and error rows render
`accent` / `warning` / `danger` as small text on card surfaces. The
contrast test now asserts accent/warning/danger ≥4.5:1 on `surface` and
`surfaceRaised` in all eight palettes (success stays a ≥3:1 marker, as it
is only used as a status dot).

## Touch targets

Tokens (`AutoVerseSpacing`): `touchMin` 48dp, `touchComfort` 56dp,
`transportPrimary` 64dp (play/pause), `transportSecondary` 52dp (skip),
plus `iconChip`/`heroChip` icon geometry. Every custom tappable control
either uses these tokens directly or is a Material component whose minimum
touch target is 48dp. `SpacingTest` enforces the scale relationships and
car-scale minimums mechanically; Phase 11 additionally enforced
`touchMin` on `SelectRow` rows and whole-row toggling for `SwitchRow`.
Status chips and dots are display-only (not interactive).

## Contrast

Policy (now fully test-enforced in `ContrastTest` across all eight
palettes, day and night variants): primary text ≥7:1 and secondary/tertiary
text ≥4.5:1 on every background and card surface; accent `onAccent` ≥4.5:1;
state colors as text on card surfaces ≥4.5:1 (new); state colors as page
markers ≥3:1. Known intentional margin: `success` in the north-light day
theme measures 4.38:1 against white, which is why it is restricted to
≥3:1 marker usage (status dot) — it never renders as text on a card, and
the test asserts exactly that boundary.

## Text scaling, truncation and orientation

- Font scale above 1.15 forces the COMPACT_SCROLL single-column layout
  (`homeLayoutPolicyFor(width, height, fontScale)`), and every screen
  scrolls (`verticalScroll` in the shared screen scaffold), so enlarged
  text cannot paint outside the window.
- Single-line texts (wordmark, chips, clock/date lines, card titles) use
  `maxLines` + ellipsis; bulk text wraps. All type sizes come from
  theme typography tokens measured in sp, so they scale with the user's
  font scale.
- Rotations and density changes are declared in `configChanges`, and the
  layout is chosen from actual window metrics, so orientation changes
  re-layout rather than restart. Verified by policy and unit tests; visual
  verification on a device is pending (Phase 13 device checklist; no
  hardware available).

## Motion audit

Complete animation inventory: the screen-transition `AnimatedContent`
(fade using `motion.fastMs`/`standardMs`) and two artwork crossfades
(home and media cards, gated on `spec.artworkCrossfade`). There are no
looping, flashing or attention-only animations anywhere in the app, and
the constrained performance profile disables screen transitions entirely
(`ScreenTransitionStyle.NONE`). Under reduced motion every duration token
collapses to zero, making all of the above instant; the remaining
crossfade call sites were deliberately left in place because a
zero-duration crossfade is a direct swap — removing the call sites would
add code churn without changing behavior.

## Driving-safety review

- Browsing, launching apps, favorites and media control require **no
  typing**: the app drawer's search field is optional, and the navigation
  destination entry is the only other text input — both are configuration
  surfaces covered by the parked-use safety notice.
- No speed, fuel or vehicle readings exist anywhere in the app because no
  real data source exists to back them (FR-SAFE-005); nothing simulates
  vehicle data.
- Error and permission states are explained, non-blocking (inline card
  text / snackbar), and recoverable: media access missing → direct link to
  system settings; missing navigation app → explained fallback; failed
  app launches → classified and surfaced; settings reset → confirmation
  dialog that keeps favorites; corrupt settings → safe defaults (Phase 10).
- The app makes no safety-certification or distraction claims; the safety
  notice asks the user to comply with local law.

## Privacy

See `docs/permissions-and-privacy.md` (inventory, rationale, data
behavior) and `docs/third-party-licenses.md` (dependency licenses, map
data obligations). No analytics, location, telemetry or credential
storage exists.

## What remains unverified (no device available)

TalkBack traversal order and announcement phrasing, real font-scale
rendering at extreme values, glanceability in a car, OEM variants of the
notification-access settings screen, and MapLibre's rendered attribution
UI. These are scheduled for first-device verification in Phases 12–13.

## Exit criteria

The safety, accessibility, privacy and license review is documented with
no unresolved critical (P0/P1) issues identified in the static audit. All
fixes above are covered by unit tests where the behavior is testable on
the JVM; device-dependent verification is explicitly deferred and
tracked.
