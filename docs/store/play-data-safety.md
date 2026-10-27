# AutoVerse 1.0.0 — Play Console "Data safety" answers

Derived from the code (network calls: OpenFreeMap tiles, Open-Meteo weather,
media-artwork URLs supplied by players; no analytics/ads SDKs). Review before
submitting — you are legally responsible for this form.

## Data collection and security
- Does your app collect or share any of the required user data types? **Yes**
  (only because weather sends an approximate location to Open-Meteo).
- Is all user data encrypted in transit? **Yes** (all requests are HTTPS).
- Do you provide a way for users to request data deletion? **No data is held
  by the developer.** Local data is removed by clearing it in the app or
  uninstalling — state this in the form's explanation.

## Data types
| Data type | Collected | Shared | Optional | Purpose | Notes |
|---|---|---|---|---|---|
| Location → **Approximate location** | Yes | Yes (Open-Meteo) | Yes | App functionality (weather) | Rounded to ~1 km, processed ephemerally, not stored by the developer |
| Location → Precise location | No | No | — | — | Used on-device only (speed, map position); never leaves the device |
| App activity → Installed apps | No | No | — | — | On-device only (launcher list) |
| Personal info, messages, contacts, photos, audio, files, calendar, health, financial, web history, device IDs | No | No | — | — | Not accessed (no microphone permission in 1.0.0) |

Notes for the reviewer field:
- "Collected" in Play's sense means transmitted off the device. GPS, installed
  apps, recent apps, destinations, media metadata, call state and OBD data are
  processed on-device only.
- Map tiles are requested from OpenFreeMap; the request contains no location
  from the device beyond the map area being displayed (and the IP address, as
  with any web request).

## Permissions declarations Play may ask about
- **READ_PHONE_STATE** — optional incoming-call panel; requested only when the
  user taps "Show incoming calls here". (Not a restricted SMS/Call Log
  permission.)
- **Notification listener (BIND_NOTIFICATION_LISTENER_SERVICE)** — media
  session access for the music panel; notifications are not read.
- **Location (foreground only)** — no background location, so no
  background-location declaration is needed.
- **QUERY_ALL_PACKAGES** — not used (scoped `<queries>`).
