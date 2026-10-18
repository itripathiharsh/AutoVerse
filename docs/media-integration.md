# AutoVerse Media Integration (Phase 5)

The dashboard media surface is bound to the device's real media sessions
through platform APIs (`MediaSessionManager` + `MediaController`) — no media3,
no playback of its own, no backend. Every displayed value comes from the
session or the system; nothing is invented.

- **Version:** `0.5.0-phase5` (versionCode 5).
- **Key rule:** controls are enabled only when the session's action mask and
  current state actually support them — unsupported controls are disabled,
  never hidden and never faked.

## 1. Scope

Included: session discovery and selection, metadata (title / artist / album /
artwork), playback status, transport commands (play / pause / next / previous),
artwork loading, the notification-access flow, and graceful handling of access
denial, session churn and controller failures.

Deliberately excluded (per Phase 5 directives): audio playback, credentials,
any backend, queue/repeat/shuffle, scrubbing, and launching the source app.

## 2. Access model

Android gates `MediaSessionManager.getActiveSessions()` behind a bound
"notification listener" component. AutoVerse declares one:

- `core/media/AutoVerseMediaListenerService` — an intentionally empty
  `NotificationListenerService` subclass. It exists **only** to satisfy the
  binding requirement; it never reads, stores, or forwards notifications.
- Manifest: `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`,
  label `@string/media_access_service_label` ("AutoVerse media access") so the
  user recognises it in system settings.
- **No extra permissions:** no `MEDIA_CONTENT_CONTROL`, no
  `QUERY_ALL_PACKAGES`. The Phase 4 `<queries>` block (MAIN/LAUNCHER intent)
  remains the only package-visibility declaration.

### Access check

`core/media/MediaAccess.kt` reads the canonical `Settings.Secure`
`"enabled_notification_listeners"` value (the same source
`NotificationManagerCompat` uses internally; works on every supported API
level). While access is off, every emission is `MediaUiState.AccessRequired`.

### Grant flow

1. Dashboard shows the access-required card: *"Allow media access in system
   settings to see and control playback from other apps. AutoVerse never reads
   your notifications."*
2. **Open system settings** launches
   `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` via a guarded helper
   (`openNotificationAccessSettings` in `AppRoot.kt`) that reports failure
   instead of crashing when no activity handles the intent.
3. The user enables AutoVerse in the app list; on return, `ON_RESUME` refresh
   (see §3) picks the grant up automatically — no restart needed.

### Revocation

Grant removed while in use: session reads throw `SecurityException` → state
falls back to `AccessRequired`; transport commands classify as `ACCESS_LOST`
with the snackbar *"Media access was lost. Re-enable it in system settings."*

## 3. Lifecycle & update model

No polling, no wake locks, no background work beyond the required binding.

- `SystemMediaSessionRepository.observe()` is a `callbackFlow` that registers
  `OnActiveSessionsChangedListener` **only while collected**, plus a
  `MediaController.Callback` (playback state, metadata, session destroyed) on
  the selected controller. Everything is unregistered when the flow closes.
- `MainViewModel` folds this into one shared state:
  `mediaRefreshTrigger.flatMapLatest { repo.observe() }.stateIn(WhileSubscribed(5 s), Connecting)`;
  screens collect it with `collectAsStateWithLifecycle`.
- `AppRoot` calls `refreshMediaSession()` on every `ON_RESUME`, so returning
  from settings — or from any app — shows current data immediately.
- Session destroyed → automatic re-scan; nothing selectable → `NoSession`.

### Featured-session selection

Deterministic order over the system's list (ties keep list order):

1. a playing session
2. a paused/buffering session that has metadata
3. any paused/buffering session
4. a session whose state is anything other than `STATE_NONE`
5. the first session

## 4. Data fidelity

- Metadata fields stay `null` when absent; values are trimmed, and blank or
  literal `"<unknown>"` strings are discarded (`cleanText`).
- Artist falls back to album artist; artwork URI priority:
  `ALBUM_ART_URI` → `ART_URI` → `DISPLAY_ICON_URI`.
- Missing app label falls back to a humanised package segment
  (`sessionDisplayName`), never a fabricated name.
- Headline falls back title → app label → package name.

### Artwork

- Embedded bitmaps (`ALBUM_ART`, then `ART`) are preferred and memoised per
  metadata instance.
- URI artwork is decoded off the main thread by `MediaArtworkCache`: an
  `LruCache` capped at 8 MiB keyed by URI, bounds-first decode then the
  smallest power-of-two sample that stays ≥ 256 px on the longest edge
  (`ARTWORK_TARGET_PX`, tested by `MediaArtworkSamplingTest`). Failed or
  oversized decodes are dropped rather than cached; evicted bitmaps are left
  to the GC (never recycled).

### Progress

- Shown only when the track duration is known; otherwise no bar exists (no
  invented progress).
- While playing, the position is extrapolated **at emission time**
  (reported position + speed × elapsed since `lastPositionUpdateTime`),
  clamped to `[0, duration]`. Nothing animates between emissions.
- `MediaProgress.fraction` clamps to 0..1.
- The card shows elapsed / total labels under the bar (`formatTrackTime`:
  `m:ss`, `h:mm:ss` from an hour up, negatives clamp to `0:00`) whenever
  the duration is known and the card is not dense (Phase 14).

## 5. Transport & capability gating

- The session's action mask is expanded (`ACTION_PLAY_PAUSE` → PLAY+PAUSE)
  and combined with the current status into `MediaControls`
  (`canTogglePlayPause = canPlay || canPause`, etc.).
- The card always renders all three transport buttons; unsupported actions
  render **disabled**, so the layout never jumps.
- Toggle semantics: sends PAUSE while playing/buffering, PLAY otherwise.
- Outcomes (`MediaCommandOutcome`):

| Outcome | Snackbar |
|---|---|
| `SENT` | none; state refreshes |
| `NOT_SUPPORTED` | "That control isn't available for this player" |
| `ACCESS_LOST` | "Media access was lost. Re-enable it in system settings." |
| `FAILED` | "Couldn't reach the media app. Try again." |

Failure classes: `UnsupportedOperationException` → `NOT_SUPPORTED`,
`SecurityException` → `ACCESS_LOST`, no bound controller or anything else →
`FAILED`. `CancellationException` is rethrown, never swallowed.

## 6. Integration points

| File | Role |
|---|---|
| `core/media/` (11 files) | State model, selection, presentation copy, access check, repository, artwork cache, listener service |
| `MainViewModel.kt` | `mediaState` StateFlow; `sendMediaCommand`, `loadMediaArtwork`, `refreshMediaSession` |
| `AppRoot.kt` | `ON_RESUME` refresh; `openMediaAccessSettings` guarded intent |
| `feature/home/HomeScreen.kt` | `MediaSection` wiring, failure snackbar, `CompactMediaRow` for the MINIMAL archetype |
| `core/design/components/MediaCard.kt` | All card states incl. `dense` mode; `mediaProgress` test tag |

## 7. Testing & build results

- **JVM suites (45 tests, all green):** `PlaybackStatusTest` (4 mapping /
  `isPlaying`), `MediaActionTest` (6 mask expansion + gating),
  `MediaSessionSelectorTest` (7 selection order + ties),
  `MediaPresentationTest` (13 copy + fallbacks + `cleanText`),
  `MediaProgressTest` (3 extrapolation + clamping + unknown duration),
  `MediaCommandTest` (3 failure classification + wording),
  `MediaAccessParsingTest` (5 listener-settings parsing),
  `MediaArtworkSamplingTest` (4 sample-size math).
- **Instrumentation:** `MediaCardTest` (11 cases — all card states,
  disabled-not-hidden transport, progress tag, dense layout). Compiles via
  `assembleDebugAndroidTest`; requires a device to actually run.
- **Build:** `gradle :app:testDebugUnitTest :app:assembleDebug
  :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL;
  **20 suites / 112 tests / 0 failures / 0 errors** (project total).
- **Artifacts (2026-10-17 03:22):**
  `app/build/outputs/apk/debug/app-debug.apk` (18 504 462 bytes) and
  `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
  (1 020 333 bytes).

## 8. Known limitations (unverified until hardware)

- No physical head unit / device was available: real session enumeration,
  artwork from real players, transport delivery and OEM notification-access
  screens are verified by unit tests and compilation only.
- Apps that never publish a `MediaSession` cannot appear (platform rule, not
  an AutoVerse omission).
- Only play/pause/next/previous are supported in this phase; seek, queue,
  repeat and shuffle are out of scope by directive.
- Sessions without a duration never show a progress bar.
- Content-URI artwork that AutoVerse cannot read (permissions on the source
  app side) falls back to no artwork rather than a placeholder image.
- OEM notification-listener settings screens differ visually; the card's
  footer ("Turn on AutoVerse in the app list, then come back.") is the
  generic guidance.
