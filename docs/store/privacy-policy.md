# AutoVerse — Privacy Policy

_Last updated: 9 November 2026 · Applies to AutoVerse 1.2.0 and later_

AutoVerse is a car launcher for Android. It is built to work without an
account and without tracking. **We (the developer) do not collect, store,
sell or share any of your personal data.** We run no servers, no analytics
and no advertising, and AutoVerse contains no tracking or advertising SDKs.

This page explains what AutoVerse uses on your device, what — if anything —
leaves your device, and why.

## What stays on your device

| Data | Why AutoVerse uses it | Where it goes |
|---|---|---|
| **Precise location (GPS)** — only while AutoVerse is on screen | Speed readout, your position on the live map, trip distance | Stays on the device. Never stored long-term, never sent to us. (See weather below for the one rounded exception.) |
| **List of installed apps** | Showing and launching your apps | Stays on the device |
| **Apps you open from AutoVerse** (recently used) | The "Recently Used" list | Stored only on the device; you can clear it anytime (Apps → Recently Used → Clear) |
| **Destinations you navigate to / save** | Recent and saved destinations | Stored only on the device; clear or remove them anytime (Maps → Destinations) |
| **Your routes** (start, destination, the route line, turn-by-turn progress) | In-app navigation | Calculated on the device by the built-in routing engine. Never sent anywhere |
| **Last car position** | Opening the map where the car is | Stored only on the device |
| **Music files** on this unit, an SD card or a USB drive (audio only) — optional | AutoVerse's own music player and the Music page library | Read on the device only. Never uploaded or copied |
| **Other music apps' libraries** (lists of playlists/songs those apps choose to share) | Browsing and starting songs from the Music page | Read on the device only, only while the Music page is open |
| **Now-playing media** (title, artist, artwork, queue) — requires the "notification access" you grant | Music controls on Home | Read on the device only. Notifications themselves are never read, stored or sent |
| **Phone call state** (ringing / in call, and the caller number only if Android provides it) — optional, only if you enable "Show incoming calls here" | Temporarily showing the call on Home | Stays on the device, never stored |
| **Bluetooth OBD-II adapter data** (speed, RPM, temperatures, fault codes) — only if you connect an adapter | The OBD screen | Stays on the device |
| **Settings** (theme, layout choices) | Remembering your preferences | Stored only on the device |

## What leaves your device (and why)

AutoVerse talks to these third-party services directly from your device. We
never see this traffic.

1. **Map tiles — OpenFreeMap (openfreemap.org).** To draw the live map,
   AutoVerse downloads map tiles for the area being shown. Like any web
   request, the tile server can see your device's IP address and which map
   area was requested. Your GPS position itself is not sent. Map data ©
   OpenStreetMap contributors.
2. **Weather — Open-Meteo (open-meteo.com).** To show the current weather,
   AutoVerse sends your location **rounded to about 1 km** (two decimal
   places). Your exact position is never sent.
3. **Album artwork.** If a music app provides an artwork web address,
   AutoVerse downloads that image from the address the music app supplied.
4. **Place search — Photon by komoot (photon.komoot.io).** When you type a
   destination, AutoVerse sends the text you typed and your location
   **rounded to about 1 km** (so nearby places come first). Your exact
   position is never sent. Search data © OpenStreetMap contributors.
5. **Road data for navigation — BRouter (brouter.de).** The first time you
   navigate in an area, AutoVerse downloads that area's road data (a 5°×5°
   region file). The server sees your IP address and which region file was
   requested — never your position, route or destination. Routes are then
   calculated on the device.
6. **Navigation apps (optional).** If you choose "Google Maps" or "Open Google
   Maps", AutoVerse passes the destination to that app on your device. That
   app's own privacy policy applies from then on.

## Permissions

At first setup AutoVerse asks for **precise location** (with the GPS switched
on) and **notification access for music**, because the map, speed, navigation
and music features cannot work without them. Everything else is optional.

- **Location** — speed, live-map position, navigation, weather.
- **Notification access** (media) — music controls only.
- **Phone state** — only for the optional incoming-call panel.
- **Bluetooth connect** — only for an OBD-II adapter you choose to connect.
- **Music and audio files** — only for playing songs from USB / SD card / this unit.
- **Foreground service (media playback)** — keeps AutoVerse's music playing while you use other apps.
- **Internet / network state** — map tiles, weather, place search, road-data downloads, artwork; network status icons.

## Children

AutoVerse is not directed at children and collects no data from anyone.

## Your choices

Deny or revoke any permission in Android settings at any time. Clear recent
apps and recent destinations from inside AutoVerse. Uninstalling AutoVerse
deletes everything it stored.

## Changes

If this policy changes, the new version will be posted at this address with
a new "Last updated" date.

## Contact

[YOUR NAME OR COMPANY] — [YOUR CONTACT EMAIL]
