# Tier 2 — OBD live engine data + offline map support (0.22.0)

## OBD live engine data (`core/obd/`)

Reads real engine PIDs from a **paired ELM327-compatible Bluetooth adapter**
(Serial Port Profile) and surfaces them in the cockpit's Cockpit Controls
card.

- **Protocol core** (`ObdProtocol.kt`, pure Kotlin, fully unit-tested): the
  ATZ/ATE0/ATL0/ATSP0 handshake, the round-robin poll order (speed → RPM →
  coolant → fuel → load → throttle), and the response parser. The parser
  tolerates echoes, `SEARCHING...`/`NO DATA`/`CAN ERROR` noise, multi-line
  frames and the `>` prompt. Formulas: RPM `((A·256)+B)/4`, speed `A`,
  coolant `A−40`, fuel/load/throttle `A·100/255`.
- **Honesty rules**: a PID the vehicle does not answer parses to null and
  the cockpit renders an em dash — never a zero, never an extrapolation.
- **Hardware layer** (`SystemObdRepository.kt`): finds the first bonded
  device whose name matches common adapters (OBD/ELM/VLINK/Veepeak/…),
  opens the SPP socket (`00001101-…`), handshakes with one reset retry,
  polls at ~150 ms per PID round-robin, closes the socket on stop or IO
  failure. **UNVERIFIED until tested with a real adapter** — the protocol
  layer it feeds is covered by `ObdProtocolTest` (13 cases).
- **Permissions**: `BLUETOOTH_CONNECT` (API 31+, runtime) requested only
  when the user taps Connect; API ≤30 uses the install-time legacy
  `BLUETOOTH` permission. No scanning — bonded devices only.
- **Cockpit UI**: the ENGINE · OBD strip shows real states only —
  Disconnected (Connect button), Permission (Allow button), Connecting,
  Connected (RPM / Speed / Coolant / Fuel with em dashes for unsupported
  PIDs), Error (reason + Retry). Hidden entirely in browser preview.
- **Privacy**: adapter traffic stays on-device; nothing is stored or sent.

## Offline map support (cockpit)

- **Passive tile mirror**: every tile the user actually views is mirrored
  into IndexedDB (`autoverse_tiles`). No bulk region downloads — that
  violates open tile-server fair-use policies and needs a licensed
  provider (a Pro-tier item). Last-visited areas therefore render without
  connectivity.
- **Offline banner** on the Maps screen when the device is offline.
- **Offline search honesty**: text search tells the user it matches saved &
  recent places only when offline (Nominatim needs a connection).
- **"Clear Offline Map Cache"** in Settings › Navigation now genuinely
  clears the tile store (it previously showed a fabricated size toast).

## Offline wake-word voice (0.23.0)

- **"Hey AutoVerse"** wake word + a 6-second free-form command window, then
  the transcript routes through the same cockpit voice router as
  tap-to-talk (open apps, navigate to places, media transport).
- Engine: Vosk 0.3.47 (Apache-2.0) + JNA, `core/voice/`. The ~40 MB small
  English model downloads ONCE from the Vosk project's own hosting on
  first enable (progress shown in the cockpit and Settings), unzipped
  flattened into app-private storage. The APK carries only the ~6 MB
  (compressed) vosk native library.
- **Opt-in**: Settings › Spatial Audio & Media › "Offline Voice". Off by
  default; listening runs only while the app is foregrounded (stops on
  ON_PAUSE); RECORD_AUDIO is a runtime permission requested at enable
  time. Audio never leaves the device.
- Pure logic (wake detection, wake-phrase stripping, zip-flatten safety,
  model completeness) is unit-tested in `OfflineVoiceRulesTest` (8 cases).
  Live microphone recognition is UNVERIFIED until run on a device.
- Costs: APK 2.3 MB → 8.2 MB (compressed vosk native libs for both
  ABIs); continuous recognition is a real battery cost when enabled, which
  is why it is foreground-only and opt-in.

## What Tier 2 deliberately does not include yet

- **Embedded offline turn-by-turn routing**: needs a routing engine and
  licensed map data — the flagship Pro feature. Until then, offline
  navigation is the handoff (geo:/Maps/Waze/OsmAnd all work offline on the
  receiving side) plus one-tap saved/recents handoff.
- **Reverse camera / steering-wheel controls**: requires real head-unit
  hardware to develop against.
