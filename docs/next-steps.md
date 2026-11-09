# AutoVerse: status and next steps (2026-11-10)

## Shipped
- **1.7.0 phone connection** (optional, Car → Phone):
  - AutoVerse Remote: a QR code opens a web remote on the phone, from `core/remote/` and `assets/remote/index.html`.
  - A Bluetooth pairing guide.
  - A Spotify Connect tip.
- **1.6.0 premium redesign:**
  - Look: Inter font, graphite palette, borderless panels, four themes, and the clock moved into the rail.
  - Map: a new night map (`core/map/MapStyleJson.kt`).
  - Navigation: a tilted driving camera with an arrow puck, a turn card and ETA bar, and a map-only Maps screen.
  - Share from Google Maps → AutoVerse (`core/routing/SharedLocation.kt`).
- Test APKs are at https://github.com/itripathiharsh/For-App-share.
- 549 unit tests pass (`gradlew :app:testDebugUnitTest`).

## Ideas for later
- Field test on real head units in India with 5 drivers, and note anything that feels slow or confusing.
- Long-press on the map → "Navigate here".
- Street names in turn instructions; lane guidance.
- Remote: show cover art and allow choosing a song from the queue.
