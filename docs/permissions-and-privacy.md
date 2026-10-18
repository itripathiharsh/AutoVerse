# Permissions & Privacy (Phase 11)

Phase 11 inventoried every permission, exported component, intent filter
and package-visibility declaration in the shipped manifest, and restated the
app's data behavior against the code. This document is the reference for
what AutoVerse can access, why, and what happens if access is denied or
removed. It is deliberately specific; anything not listed here is not
requested or used.

## Final manifest inventory (release merge)

### Permissions

| Permission | Origin | Why it exists | Runtime prompt |
| --- | --- | --- | --- |
| `INTERNET` | Declared in `AndroidManifest.xml` | Only for the feature-flagged embedded-map prototype (default **off**): MapLibre fetches its style document and vector tiles over HTTPS when the map screen is opened. The launcher itself, the app drawer and the navigation hand-off work fully offline. | No (install-time) |
| `ACCESS_NETWORK_STATE` | Declared in `AndroidManifest.xml` (also merged by MapLibre) | Two uses: it feeds the dashboard Wi-Fi indicator from the current network's capabilities (Phase 14; read on-device only), and MapLibre's internal download management. | No (install-time) |
| `ACCESS_WIFI_STATE` | Merged by MapLibre | Same as above. | No (install-time) |
| `BLUETOOTH` | Declared in `AndroidManifest.xml`, capped at `maxSdkVersion="30"` | Reads the adapter on/off state for the dashboard Bluetooth indicator on downlevel devices only. From Android 12 the app never requests `BLUETOOTH_CONNECT`; the indicator simply stays hidden there. | No (install-time) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Merged by AndroidX Core | Protects the app's own dynamically registered receivers. | No |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Declared by MapLibre, **removed at merge** | MapLibre declares them for an optional user-location puck that AutoVerse never uses. `tools:node="remove"` keeps the shipped manifest free of any location access. No location API exists anywhere in the app. | n/a — not present |

### Components

| Component | Exported | Notes |
| --- | --- | --- |
| `MainActivity` | Yes (required) | Carries the `MAIN`/`LAUNCHER`/`HOME`/`DEFAULT` intent filter — a launcher must be exported to be selectable as the home screen. `singleTask`, `resizeableActivity`, and `configChanges` for orientation/density; holds no data. |
| `AutoVerseMediaListenerService` | Yes, guarded by `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | The platform only binds notification listeners to services protected by that system permission; nothing else can bind it. The class is intentionally **empty** — it never reads, stores, logs or forwards notification content. |

There are no other exported components, no manifest-declared receivers, and
no background services. The package-change receiver used by the app drawer is
registered at runtime with `RECEIVER_NOT_EXPORTED`, so only the system can
deliver to it.

### Package visibility (`<queries>`)

Scoped to exactly what the launcher needs; `QUERY_ALL_PACKAGES` is not used:

- `MAIN`/`LAUNCHER` intent — resolves the app drawer and favorites.
- Nine `MAIN` + `CATEGORY_APP_*` intents (maps, music, messaging, email,
  contacts, calendar, browser, calculator, gallery) — probed once per
  refresh to classify apps whose declared `ApplicationInfo.category` is
  missing or unmapped (games, images, news); they never grant visibility
  beyond category-hinted launcher activities (Phase 15).
- `VIEW` with `geo:` scheme — destination discovery for the navigation
  hand-off.
- `com.google.android.apps.maps`, `com.waze` — the two providers with
  documented URI formats that are probed by package name.

## Feature-to-permission rationale (and the MVP without it)

| Capability | Needs | Without it |
| --- | --- | --- |
| Home screen, app drawer, launching apps, favorites | Nothing | — |
| Navigation hand-off to a maps app | Nothing (uses installed apps) | Hand-off unavailable only if no nav app is installed — the UI explains and falls back to the system default. |
| Media card (play/pause/skip, metadata) | Notification-listener access via system settings | The card shows an explained "Media access required" state with a direct link to the system settings page. No crash, no silent failure. |
| Dashboard Wi-Fi / Bluetooth indicators | `ACCESS_NETWORK_STATE`; `BLUETOOTH` (≤ API 30, install-time only) | Indicators stay hidden (`UNKNOWN`) and the header stays clean — no prompt, no error state. |
| Embedded map prototype (experimental, off by default) | `INTERNET` (+ the merged network-state permissions) | The rest of the launcher is unaffected; the map screen cannot be reached while the toggle is off. |

If the embedded-map prototype is dropped before release, `INTERNET` and the
MapLibre dependency (and with it the two merged network permissions) should
be removed together — the launcher has no other network use.

## Notification-listener disclosure

AutoVerse asks for notification-listener access for exactly one reason:
Android gates media-session access (reading other apps' playback state and
sending transport commands) behind that binding. Consequences, as
implemented:

- **User-initiated**: access is only offered from the media card or when a
  media action fails — never on first launch, never as a blocker.
- **Explained in place**: the card states "…playback from other apps.
  AutoVerse never reads your notifications."
- **Empty implementation**: the service subclass has no code; notification
  content is never read, stored, logged or forwarded.
- **Revocable at any time**: access is re-checked on every resume and on
  every media action; revocation degrades to the explained access-required
  state.

## Data behavior

- **No analytics, telemetry, crash reporting or advertising SDKs** exist in
  the dependency set (see `docs/third-party-licenses.md`) and no code sends
  usage data anywhere.
- **No location history**: no location permission is present in the merged
  manifest and no location API is called.
- **No connectivity tracking**: the dashboard reads the current Wi-Fi and
  Bluetooth on/off state locally, only while it is visible, to draw the
  header indicators; the values are never stored or transmitted.
- **No credential storage**: there are no accounts, tokens, or passwords to
  store.
- **No background recording or persistent services**: nothing samples or
  records beyond the visible screen; the notification listener is bound by
  the system only while access is granted, and the app performs no work in
  the background.
- **On-device storage** is limited to two DataStore files:
  `autoverse_settings` (theme, appearance, motion, performance profile,
  clock, navigation choice, map toggle) and `autoverse_favorites` (favorite
  app component keys). Nothing in them is personal data.
  `android:allowBackup` is left at its default, so these two files can be
  included in the user's own Android Auto Backup; they contain no
  identifiers beyond the user's chosen favorite app packages.
- **Diagnostics are on-screen only**: the diagnostics screen shows device
  capability information (model, Android version, RAM, cores, display
  metrics, frame statistics) for the user standing in front of the device.
  Nothing is written to disk or transmitted, and no device identifiers
  (serial, IMEI, advertising ID, MAC) are shown or read.
- **Network use** happens only when the user enables the experimental map
  and opens it; those requests go to the MapLibre demo tile service, which
  is marked in-app as a prototype that must be replaced by a licensed
  provider before production use (see `docs/third-party-licenses.md`).
- **App-list scanning stays local and minimal**: the drawer reads each
  launchable app's label, icon, declared category, the category intents
  it answers, and its first-install timestamp from the local
  PackageManager only; the results live in memory for the visible session
  and are never written to disk or transmitted. There is no
  `PACKAGE_USAGE_STATS`, no accessibility service, and no usage history
  of any kind — the "Newly Installed" panel uses only
  `firstInstallTime` (apps that report zero are excluded).
- **App icons and media artwork** are read from the local device
  (PackageManager and the media session) and never uploaded.

## What remains unverified

No Android device has been available (Phases 11–15), so the following
are verified by code inspection only: OEM-specific variants of the
notification-access settings screen, and revocation timing across OEM
notification managers. First-device verification remains pending — see
the device checklist in `docs/release.md`.
