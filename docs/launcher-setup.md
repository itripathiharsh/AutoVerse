# AutoVerse Launcher Setup (Phase 4; drawer rebuilt Phase 15)

How AutoVerse discovers, launches, and favorites apps on a head unit, plus
the device-side steps for using it as the Home app. Everything here maps to
user directives for Phase 4; no backend, account, or telemetry is involved.

## 1. What Phase 4 adds

- **App drawer** — every launchable app on the device, with live search.
- **Favorites** — starred apps appear in a drawer section and in the home
  dock (up to 3). Persisted locally on the device.
- **Launch handling** — apps open through the platform-standard MAIN/LAUNCHER
  intent; missing or blocked apps produce a snackbar instead of a crash.
- **Refresh** — the app list re-queries on launch and when packages are
  installed, removed, or updated while AutoVerse is running.

Recents (recently used apps) are intentionally **not** implemented; see §6.

## 2. Permissions and package visibility

- **No runtime permissions are requested.** AutoVerse declares no
  `uses-permission` entries.
- **Package visibility (Android 11+ / API 30+):** `AndroidManifest.xml`
  declares a scoped `<queries>` block:

  ```xml
  <queries>
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent>

      <!-- Phase 15: nine MAIN + CATEGORY_APP_* intents (maps, music,
           messaging, email, contacts, calendar, browser, calculator,
           gallery) for category classification -->
  </queries>
  ```

  This exposes exactly the apps that declare a launcher activity — the same
  set a Home screen must show; the category intents never grant visibility
  beyond category-hinted launcher activities. `QUERY_ALL_PACKAGES` is
  deliberately **not** used: it is unnecessary and triggers Play policy
  review.
- **Package change listener:** a dynamically registered, `NOT_EXPORTED`
  broadcast receiver (system `PACKAGE_ADDED/REMOVED/REPLACED/CHANGED`
  broadcasts) refreshes the list. No manifest-declared receiver, no
  background awareness beyond the app's own lifetime.

## 3. Using AutoVerse as the Home app

AutoVerse declares the `HOME` + `DEFAULT` categories on its main activity,
so Android lists it as a Home app option — it **never forces itself** as the
default.

1. Install the APK (`app-debug.apk`).
2. Open **Settings → Apps → Default apps → Home app** (wording varies by
   OEM and Android version) and pick **AutoVerse**.
3. Press Home — AutoVerse becomes the launcher.

### Returning to the factory launcher

Same screen: choose the OEM launcher (e.g. "Launcher3", "Car Launcher") as
the Home app. Depending on the head unit, you may need to hold Home to see
the chooser, or use the OEM's own settings. AutoVerse also remains available
as a normal app (LAUNCHER category), so nothing is locked behind the Home
role.

### Fallback: dashboard-only mode

If the head unit pins a single factory launcher and cannot change the Home
app, AutoVerse still works as a regular app: its full dashboard, settings,
diagnostics, and app drawer are usable when opened from the factory
launcher's app list. The app makes no assumptions about holding the Home
role.

## 4. Favorites

- Tap the **star** on a drawer tile to add/remove a favorite. Favorites show
  in a FAVORITES section (when not searching) and in the home dock —
  the first three favorites, plus a permanent **Apps** tile that opens the
  drawer.
- Stored locally in the app's own DataStore file (`autoverse_favorites`);
  format is one `package/activity` key per line with blank/duplicate entries
  dropped on read. No favorites leave the device.
- If an app is uninstalled, its favorite entry is pruned on the next
  successful refresh; launching a missing app also removes it immediately.

## 5. Compatibility notes

- `minSdk 23` (Android 6.0) through `targetSdk 34`. On API < 30 the
  `<queries>` block is ignored by the platform and all installed apps are
  visible, as before.
- Launch intents handle the standard failure modes: an app with no launcher
  activity never appears in the list; an app removed between refresh and tap
  shows "… is no longer installed" (and is auto-unfavorited); security
  blocks show "Android blocked launching …".
- Icons load asynchronously (off the main thread) with a bounded in-memory
  LRU cache (64 entries at 96px); a vector placeholder shows until an icon
  arrives, so large app lists never block the UI thread.

## 6. Why no recents list

Reliable recents require either `UsageStatsManager` (needs the special
`PACKAGE_USAGE_STATS` app-op permission, which is inappropriate to request
for a launcher) or the deprecated/restricted `getRecentTasks` API. Phase 4
therefore ships favorites + full drawer instead. Recents can be revisited
if a supported, permission-clean API becomes available.

## 7. Unverified on hardware (carried forward)

No physical device or head unit was available during Phase 4 development.
Still to verify on hardware:

- Real icon rendering and cache behavior across a typical head-unit app
  set (>40 apps).
- Drawer scroll performance and frame pacing with a large list.
- Behavior of the OEM default-Home chooser and factory-launcher rollback
  (OEM-specific wording/flows in §3).
- Insets and touch ergonomics in the actual dash position.

## 8. Phase 15 refresh — drawer rebuild

Phase 15 rebuilt the drawer around the shared brand header and navigation
rail without changing the Phase 4 foundations above (same queries model,
same launch handling, same favorites file).

- **Search + category chips** — the search field filters live over label
  and package name; seven chips (All Apps, Navigation, Music & Audio,
  Communication, Video, Tools, Others) filter by category and combine
  with the query.
- **Classification** — an app's declared `ApplicationInfo.category` wins
  when present; otherwise nine `CATEGORY_APP_*` intent probes map apps
  whose metadata lacks a declared category. Gallery is deliberately
  mapped to **Others** (storing photos is not consuming video), and
  anything unmatched falls back to **Others**; the classifier never
  returns the All Apps sentinel. Many OEM apps declare no category and
  answer no category intent, so large "Others" groups are expected and
  documented rather than worked around with broad package queries.
- **Grid / list** — a titled toggle switches view mode; the current
  query and chip survive the switch, and item keys are stable across
  modes.
- **Favorites edit mode** — Edit stages removals in a draft; Save
  persists the new order to `autoverse_favorites`, Cancel discards. The
  stored favorites survive process death and are pruned against the
  live app list on entry.
- **Newly Installed** — the top of the side content lists up to 5 real
  apps by `firstInstallTime` (descending, zeroed entries excluded). No
  recents/usage history (§6 still applies).
- **Adaptive modes** — a table-tested policy
  (`core/design/DrawerLayoutPolicy.kt`) picks COMPACT (grid + top
  navigation), MEDIUM (rail + grid) or EXPANDED (rail + grid + favorites
  side panel, 300dp) from width/height/font scale; large font scale or
  very short windows fall back to COMPACT.

### Additional unverified on hardware (Phase 15)

- Category accuracy against a real head-unit app set (OEM metadata is
  sparse — expect large "Others" groups).
- Search keyboard behavior on a physical dash display (soft keyboard
  remains untested; no hardware).
- Toggle and edit flows under real touch latency.
