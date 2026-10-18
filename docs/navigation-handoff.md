# AutoVerse Navigation Handoff (Phase 6)

The dashboard navigation surface hands a validated destination to the user's
preferred navigation app through documented platform intents — no embedded
map, no location permission, no backend. Nothing is invented: no route, no
ETA, no traffic, no "current location".

- **Version:** `0.6.0-phase6` (versionCode 6).
- **Key rule:** every provider keeps its own documented URI format; AutoVerse
  never assumes two navigation apps share one. Where no documented hand-off
  exists, the app can only be opened — and the UI says so instead of
  attempting a silent, unsupported launch.

## 1. Scope

Included: the provider model (`SystemDefault`, `GoogleMaps`, `Waze`,
discovered `OpenOnly` apps), discovery, the persisted preferred-provider
setting and its resolution, live destination validation, documented URI
construction, launch-outcome classification with honest failure copy, the
settings section and the dashboard card/bar states.

Deliberately excluded: embedded map rendering, `ACCESS_FINE_LOCATION` (or any
location permission), route/ETA/traffic computation, tracking a hand-off after
launch, and historical destination storage — the destination lives in the
card's text field only and is never persisted.

## 2. Architecture

All in `core/navigation/`:

| File | Contents |
|---|---|
| `NavigationProvider.kt` | Sealed interface (`SystemDefault`, `GoogleMaps`, `Waze`, `OpenOnly`), `supportsOpenApp` extension, per-provider URI builders, `ResolvedNavigationProvider`, `resolveNavigationProvider` |
| `NavigationDestination.kt` | `NavigationDestination.Query` / `.Coordinates`, `parseNavigationDestination`, `destinationFailureMessage`, coordinate formatting, 512-char cap |
| `NavigationUriEncoding.kt` | Pure-Kotlin RFC 3986 percent-encoding (no `android.net.Uri`, unit-testable on the JVM) |
| `NavigationOutcome.kt` | `NavigationOutcome`, exception classification, `navigationFailureMessage` |
| `NavigationRepository.kt` | Interface: `availableProviders()`, `launch(destination?, provider)` |
| `SystemNavigationRepository.kt` | Platform implementation (package manager + `Intent` dispatch) |

Wiring: `MainViewModel` owns the repository; `navigationProviders` is a
`StateFlow` refreshed alongside the app list (failures keep the last known
list); `navigationProvider` combines it with the persisted preference via
`resolveNavigationProvider`. `AppRoot` collects both and passes them to
`HomeScreen` (card + callbacks) and `SettingsScreen` (provider list + setter).

## 3. Providers & discovery

| Provider | `packageName` | Destination hand-off |
|---|---|---|
| System default | `null` (Android resolves the `geo:` intent) | yes, via `geo:` |
| Google Maps | `com.google.android.apps.maps` | yes, `google.navigation:` |
| Waze | `com.waze` | yes, `waze://` |
| `OpenOnly` | discovered package | no — open only |

Discovery: `SystemNavigationRepository.availableProviders()` queries the
package manager for activities handling a `geo:` `ACTION_VIEW` intent, keeps
one entry per package, removes AutoVerse itself and the two known providers,
and lists the remainder as `OpenOnly(appLabel)` sorted by label. The system
default is always first. All discovery runs off the main thread inside the
repository and never throws; the view model additionally keeps the previous
list if a refresh fails.

Package visibility (API 30+), declared in the manifest `<queries>`:
`ACTION_VIEW` + `geo` scheme, plus explicit
`com.google.android.apps.maps` and `com.waze` package entries. No
`QUERY_ALL_PACKAGES`.

## 4. Destination validation

Typing is parsed live (`parseNavigationDestination` on every keystroke):

1. Trim whitespace; blank → `Empty` (Navigate disabled).
2. Longer than 512 chars → `Invalid(TOO_LONG)`.
3. Exactly two comma-separated parts, each matching
   `^[+-]?(\d+(\.\d*)?|\.\d+)$` (no scientific notation), with latitude in
   −90…90 and longitude in −180…180 → `Valid(Coordinates)`.
4. Anything else → `Valid(Query)` — an address or place name, passed through
   as entered.

The Navigate control is enabled only for a `Valid` result; invalid input
shows the reason inline (`destinationFailureMessage`). Coordinate numbers are
formatted with `BigDecimal().stripTrailingZeros().toPlainString()` so
`48.8584` stays `48.8584` and `48.0` becomes `48`.

Encoding (`encodeUriComponent`): RFC 3986 unreserved characters pass through;
everything else is UTF-8 percent-encoded with uppercase hex. Spaces are `%20`,
never `+` (Maps' documentation shows `+`, but both decode identically and one
consistent encoder is easy to test).

## 5. URI formats (documented per provider)

| Provider | Coordinates | Query | Documentation |
|---|---|---|---|
| System default | `geo:lat,lng?q=lat,lng` | `geo:0,0?q=<encoded>` | [Common intents: maps](https://developer.android.com/guide/components/intents-common#ViewMap) |
| Google Maps | `google.navigation:q=lat,lng` | `google.navigation:q=<encoded>` | [Google Maps intents](https://developer.android.com/guide/components/google-maps-intents) |
| Waze | `waze://?ll=lat,lng&navigate=yes` | `waze://?q=<encoded>&navigate=yes` | [Waze deep links](https://developers.google.com/waze/deeplinks) |

Launch mechanics: `ACTION_VIEW` with `FLAG_ACTIVITY_NEW_TASK`, dispatched
from the application context. Maps and Waze launches are package-pinned
(`setPackage`) so the intent goes to exactly the chosen app; the system
default is left unpinned so Android resolves the `geo:` intent with the user's
own default.

## 6. Outcomes & failure copy

| Outcome | When | Snackbar copy |
|---|---|---|
| `OPENED` | Intent dispatched | — (no snackbar) |
| `NO_PROVIDER` | `SystemDefault` (no package to pin) and nothing handles the intent | "No navigation app is available" |
| `ACTIVITY_NOT_FOUND` | Target package is installed but no activity accepts the intent | "<App> couldn't open that destination" |
| `NOT_SUPPORTED` | `OpenOnly` provider asked to receive a destination | "<App> can't receive destinations from AutoVerse" |
| `LAUNCH_FAILED` | Any other launch failure | "Couldn't open <App>. Try again." |

Classification maps exceptions (`ActivityNotFoundException` →
`ACTIVITY_NOT_FOUND`, everything else → `LAUNCH_FAILED`) and the two
structural cases above; `CancellationException` is always rethrown. Failures
surface as a dismissible snackbar on the dashboard — never a blank panel or a
crash.

## 7. Preferences & settings

- Preference: `AppSettings.navigationPackage: String?` (null = system
  default), persisted through `SettingsRepository`.
- Settings → NAVIGATION lists "System default" plus every installed provider;
  `OpenOnly` entries carry "Opens the app only — no documented destination
  format". If the stored preference is not installed, a warning row appears
  and the effective provider falls back to the system default **without
  clearing the preference** — a reinstall restores the choice.

## 8. Privacy

No location permission is requested or used. The destination text exists only
in the card's field (remembered for the composition, never persisted, never
sent anywhere except the navigation app the user chose). Provider discovery
reads only local package metadata. Nothing is logged or transmitted.

## 9. Limitations

- **No dev hardware:** intent dispatch, URI construction and failure mapping
  are unit-tested, but real resolution (which OEM app receives `geo:`, the
  system chooser, Maps/Waze behavior) needs a device — see §10 and
  `launcher-setup.md` §7.
- The platform has **no completion callback** for this pattern: once handed
  off, route and navigation state belong to the other app. AutoVerse cannot
  (and does not claim to) show progress.
- Only Maps and Waze have publicly documented destination formats; every
  other navigation-capable app is `OpenOnly` by design.
- The 512-character destination cap is a deliberate URL-length guard; longer
  text is rejected with a visible reason, not truncated.

## 10. Tests

JVM (`app/src/test/java/com/autoverse/launcher/core/navigation/`, 35 tests):

| Suite | Tests | Covers |
|---|---|---|
| `NavigationDestinationParsingTest` | 11 | Empty/query/coordinate classification, ranges, the 512 cap, coordinate formatting |
| `NavigationUriEncodingTest` | 7 | RFC 3986 passthrough, UTF-8, `%20` spaces, punctuation, Unicode |
| `NavigationProviderUriTest` | 10 | Per-provider URIs for queries and coordinates, OpenOnly → null |
| `NavigationProviderResolutionTest` | 4 | Preference match, stale preference → system default + warning flag |
| `NavigationOutcomeTest` | 3 | Exception classification and exact snackbar wording |

UI (`app/src/androidTest/.../components/NavigationCardTest.kt`, 7 cases):
destination field validation, Navigate enablement, open-only state,
preferred-missing warning. Compiles via `assembleDebugAndroidTest`; not run —
no device attached.

## 11. Verification

- **Build:** `gradle :app:testDebugUnitTest :app:assembleDebug
  :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL; **25 suites / 147 tests
  / 0 failures** (Phase 5: 20/112; +5 suites, +35 tests).
- **APKs:** `app/build/outputs/apk/debug/app-debug.apk` (18,537,340 bytes)
  and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
  (1,029,225 bytes).
- Previews: `DesignPreviews` NAVIGATION CARD STATES gallery section
  (Maps full, Waze dense, preferred-missing warning, open-only, compact
  rows) plus two home rail previews (Maps selected, preferred app missing).
- Unverified until hardware: everything in §9 — real intent resolution and
  on-road ergonomics.

## Phase 18 refresh — the navigation page

Phase 18 adds a dedicated navigation page (`Screen.NAVIGATION`, reached from
the rail's Navigation destination) on top of the unchanged handoff stack.
Every API in §2–§6 is reused as-is: the page never re-implements validation,
URI construction or outcome classification.

**Launch target.** `navigationPageProvider(resolved, providers)`
(`core/navigation/NavigationPageProvider.kt`) picks the page's provider: a
non-default preference passes through unchanged (including `OpenOnly`); when
the preference is the system default and Google Maps is installed, the page
launches through the Google Maps provider — clearly labelled in the UI —
while keeping `preferredMissing` semantics and never rewriting the stored
preference. Settings → NAVIGATION remains the only place the preference is
changed.

**Destination field & handoff.** One field accepts free text or
coordinates; parsing/validation is the same `parseNavigationDestination`
(§4) live on every keystroke, invalid input shows the same
`destinationFailureMessage` reason, and Navigate stays disabled until the
result is `Valid`. Navigate hands the parsed destination to the repository;
failures surface the same snackbar copy (§6). No geocoding: the typed text
is passed through exactly as entered.

**Search suggestions.** `navigationSuggestions(query, savedPlaces,
recentDestinations, limit)` (`core/navigation/NavigationSearch.kt`) returns
genuine local matches only — saved places first, then recents, deduplicated
by canonical destination text, an exact echo of the query skipped,
case-insensitive substring match on label or destination text, capped at
`MAX_NAVIGATION_SUGGESTIONS = 5`. Tapping a suggestion fills the query via
`navigationDestinationText` (round-trips through the parser). With no local
match the page says so and points at the handoff instead of inventing
suggestions, POIs or addresses.

**Saved places & recents (local only).**

| Model | File | Rules |
|---|---|---|
| `SavedPlace` | `SavedPlace.kt` | label trimmed/clamped to 60 chars, blank label falls back to the destination text, max 12, replace-in-place for an existing destination |
| `SavedPlaceCodec` | `SavedPlaceCodec.kt` | line per place, `<escaped label>` TAB `<escaped destination text>`; malformed lines skipped, never fail the whole set |
| `RecentDestination` | `RecentDestination.kt` | recorded only on a confirmed `OPENED` outcome (never on attempt/failure), newest first, dedup by destination text, max 8 |

Storage: DataStore Preferences (`autoverse_navigation`) in
`NavigationPlacesRepository.kt`, with a corruption handler and
IOException-tolerant flows. Both codecs escape control characters so
newlines/tabs in user text survive a round trip. There is no location
history and no fabricated coordinates anywhere: a recents/saved entry shows
exactly what the user typed or entered.

**Places panel.** Recents + Saved Places live in one collapsible panel with
an accessible toggle ("Hide places panel" / "Show places panel", icon +
selected state). Hiding it reflows the map into the freed space; the panel
is a child of the body layout and the map surface is never recreated by the
toggle or by query changes. Panel visibility and the query survive
configuration changes (`rememberSaveable`).

**Embedded map.** Still off by default, and the page does not depend on it:
when the flag is off the body is an intentional handoff surface (provider
name, honest explanation, "Open navigation app" action), never a fake map.
When the flag is on, the page renders the injected `EmbeddedMapSurface` and
handles its loading/error/empty/fallback states through the existing
`MapPocController`.

**Tests (Phase 18).** 73 new JVM tests: `SavedPlaceTest` (12),
`SavedPlaceCodecTest` (13), `RecentDestinationTest` (12),
`NavigationSearchTest` (10), `NavigationPageProviderTest` (6),
`NavigationDestinationTextTest` (6), plus `NavigationLayoutPolicyTest`
(14, `core/design`). Instrumented `NavigationScreenTest` (20 cases: field
validation/clear, suggestions, panel toggle, reflow keeps the map surface
alive, save/remove flows, failure snackbar copy, stale preferred app,
`OpenOnly` routing note, empty hints) compiles via
`assembleDebugAndroidTest` only — no device attached, never executed.

**UNVERIFIED on hardware:** everything in §9 still applies, plus the page's
on-device keyboard behaviour, panel reflow ergonomics, suggestion tap
targets and the embedded-map states on a real head unit.
