# AutoVerse 1.0.0 — Launch checklist

Everything a build machine can verify is done (see "Done"). What remains needs
a real head unit or your Play Console account. **Do not publish until section
A passes on at least one cheap head unit** — that is the only proof of the
"smooth, no lag" promise.

Artifacts:
- Upload to Play: `releases/AutoVerse-1.0.0.aab` (signed with your upload key)
- Sideload / test: `releases/AutoVerse-1.0.0-release.apk` (same build) — also `AutoVerse.apk`

## Done (verified on the build machine)
- [x] Targets Android 16 / API 36 (Play requirement for new apps since 31 Aug 2026); minSdk 23
- [x] All 64-bit native libraries 16 KB page-aligned (Play requirement for API 35+ targets)
- [x] Signed release APK + AAB with the release key (`D:\dev\autoverse-signing\` — back it up!)
- [x] R8 minified, resource-shrunk; release APK 10.8 MB
- [x] Unit tests, lint (0 errors), device-test compilation
- [x] No microphone permission, no ads/analytics SDKs, no API keys, no paid services
- [x] Privacy policy, store listing and Data-safety drafts in `docs/store/`

## A. Device test (go / no-go) — ~20 minutes, needs a head unit
Install `AutoVerse-1.0.0-release.apk`. Uninstall any older AutoVerse first if
the install fails (signature or version conflict).

1. **First launch**: welcome → themes → setup → done. No system dialog before
   the welcome screen. Set AutoVerse as the home app from the setup page.
2. **Home appears instantly**; the map fills its panel within ~1–2 s.
3. **Location**: grant it in setup (or "Allow location" on the speed panel).
   Within a minute outdoors the speed panel says "GPS · live".
4. **Smoothness (the key test)**: drive (or walk briskly) 2–3 minutes with Home
   open, and pan/zoom the map with a finger for 30 s. Then open
   Diagnostics → Performance → **Live map**.
   - PASS: "smooth" (≥ 25 fps while moving, worst frame ≤ 50 ms).
   - If "below target": switch Settings → Performance to Constrained (30 fps cap
     + lite map) and retest. Report the numbers if it still fails.
5. **Restart test**: reboot the unit. Home must show "GPS · live" again on its
   own (no visit to the Speed page needed).
6. **Map follow**: the map follows the car heading-up while driving; pan away →
   "Recenter" appears and works.
7. **Screens**: Home ↔ Apps ↔ Maps ↔ Speed — instant switches, the map is not
   re-loaded when returning to Home/Maps.
8. **Music**: play Spotify/YouTube Music → title, artwork, play/pause/skip work;
   queue button shows the player's queue where the player provides one.
9. **Google Maps**: "Open Google Maps" opens Maps full screen; Home key returns.
   Type a destination → Navigate starts Google Maps directions.
10. **Apps**: all apps listed; search works; opened apps appear in Recently Used.
11. **Calls** (units with a SIM/phone link only): enable "Show incoming calls
    here", receive a call → the speed panel shows the call, then returns.
12. **Offline**: turn off internet, go back to an area you already viewed → map
    still draws; new areas show blank tiles without crashing.
13. **Back key / gesture on Home**: nothing happens (stays on Home).

Capture screenshots for the store while doing this (Home, Apps, Maps, Speed,
two themes).

## B. Play Console (your account)
1. Create the app → default language → **Paid** → set price.
2. App content: privacy-policy URL (host `docs/store/privacy-policy.md`
   publicly, e.g. GitHub Pages), ads = **No**, content rating questionnaire,
   target audience 18+, Data safety from `play-data-safety.md`.
3. Store listing from `play-store-listing.md` + graphics.
4. Enrol in **Play App Signing** (recommended; your key becomes the upload key).
5. Upload `AutoVerse-1.0.0.aab` to **Internal testing** first, install from
   Play on the head unit, repeat section A steps 2–9, then promote to
   Production.
6. Personal developer accounts created after Nov 2023 must run a closed test
   with at least 12 testers for 14 days before production access — plan for it.

## Known limits to state honestly (also in the listing)
- The live map is OpenStreetMap-based, not Google's map. Turn-by-turn voice
  directions run in Google Maps/Waze (full screen), not inside AutoVerse.
- Incoming-call display needs a unit with telephony (most head units only have
  it via a phone link).
- Diagnostics → "Map display (Shizuku)" is an experimental power-user tool,
  hidden in Diagnostics; it is not part of the advertised features.
