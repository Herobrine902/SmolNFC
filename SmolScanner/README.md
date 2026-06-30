# Smol Scanner

A multi-tool signal scanner. Five tabs:

- **NFC** — tap a card to read/catalog it; emulate NDEF tags over HCE.
- **BT** — list Bluetooth devices broadcasting nearby, signal strength, vendor.
- **WiFi** — networks around you: name, signal, band, security. Names and signal only, never passwords.
- **Metal** — magnetometer as a metal/magnet detector (Earth's field is ~25–65 µT; metal spikes it).
- **Tracker** — Bluetooth devices sorted by how long they've stayed near you, as a rough "is something following me" hint.

## Build

GitHub Actions builds the APK automatically on push (`.github/workflows/build.yml`). Grab it from the repo's Actions tab → latest run → Artifacts → SmolNFC-debug.

Local build also works with Android Studio (open the folder, sync, Run) or `gradle wrapper && ./gradlew assembleDebug`.

## Permissions

The scanners need nearby-devices, Bluetooth, and location permission (location is required by Android to return WiFi/BLE scan results — it's not used for anything else here). Each tab has a "Grant permission" button. WiFi scanning also needs Location turned on in system settings.

## What it does NOT do

- No WiFi password reading/cracking. WPA2/WPA3 passwords are never transmitted, so nothing can read them out of the air. Anything claiming to is fake.
- No intercepting other people's messages/calls, no tracking a specific person. The tracker tab is defensive only and can't reliably identify a tracker (they rotate IDs) — use Android's built-in Unknown Tracker Alerts for that.
- NFC: still can't emulate UID-checked door fobs, Ultralight C, or payment cards. Only NDEF payloads emulate.

Everything here only observes signals already being broadcast in the open, or reads the phone's own sensors.
