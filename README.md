# Smol NFC

Reads and catalogs NFC cards, and emulates the ones that can actually be emulated.

## Build

Needs Android Studio (or a local JDK 17 + Android SDK + Gradle).

- **Android Studio:** open the project folder, let it sync, Run on the S22 Ultra.
- **CLI:** generate the wrapper once, then assemble:
  ```
  gradle wrapper
  ./gradlew assembleDebug
  ```
  APK lands in `app/build/outputs/apk/debug/`.

The wrapper jar isn't bundled, so `gradle wrapper` (or an Android Studio sync) has to run first. Android Studio may prompt to bump AGP/Gradle — accept it if so.

## What it does

- Tap any card to the upper back of the phone → it stores UID, ATQA, SAK, tech list, an IC-type guess, and a full readable memory dump (Ultralight page-by-page, Classic block-by-block with default-key auth).
- Each stored card is flagged emulable or not, with the reason.
- Cards carrying an NDEF payload can be re-presented over HCE as a Type 4 tag (open the card → "Emulate this card", hold to a reader).

## What it can't do

- Emulate UID-checked door fobs or Ultralight/Classic access cards. Stock Android HCE is Type 4 / ISO-DEP only and won't set the UID, so the one thing those locks check is the one thing the phone can't fake.
- Touch payment cards. Those live in the secure element and aren't readable.

For a physical spare of a UID-only or Classic fob, the route is writing the dump to a magic card with a Flipper/Proxmark, not the phone.
