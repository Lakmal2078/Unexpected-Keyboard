# Unexpected Keyboard [<img src="https://hosted.weblate.org/widget/unexpected-keyboard/svg-badge.svg" alt="État de la traduction" />](https://hosted.weblate.org/engage/unexpected-keyboard/)

---

⚠️ Google wants to kill the open-source Android community.

See [keepandroidopen.org](https://keepandroidopen.org/) and the [F-Droid blog](https://f-droid.org/en/2025/09/29/google-developer-registration-decree.html).

---

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/juloo.keyboard2/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=juloo.keyboard2)

[සිංහල අනුවාදය](README.si.md)

Lightweight and privacy-conscious virtual keyboard for Android.

https://github.com/Julow/Unexpected-Keyboard/assets/2310568/28f8f6fe-ac13-46f3-8c5e-d62443e16d0d

The main feature is that you can type more characters by swiping the keys towards the corners.

This application was originally designed for programmers using Termux.
Now perfect for everyday use.

This application contains no ads and is open source.

## Sinhala suggestions

The transliterator in `srcs/juloo.keyboard2/SinhalaTransliterator.java` provides Singlish-to-Sinhala word candidates and next-word suggestions. Next-word suggestions use a fixed list keyed by the previous word, covering a limited set of common Sinhala phrases. They do not learn from typed text or use broader sentence context, so suggestions may be missing or unsuitable for other wording.

Usage: to apply the symbols located in the corners of each key, slide your finger in the direction of the symbols. For example, the Settings are opened by sliding in the left down corner.

| <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Screenshot-1" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Screenshot-2"/> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Screenshot-3"/> |
| --- | --- | --- |
| <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Screenshot-4" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Screenshot-5" /> | <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" alt="Screenshot-6" /> |

## Help translate the application

Improve the application translations [using Weblate](https://hosted.weblate.org/engage/unexpected-keyboard/).

[<img src="https://hosted.weblate.org/widget/unexpected-keyboard/multi-auto.svg" alt="État de la traduction" />](https://hosted.weblate.org/engage/unexpected-keyboard/)

## Build Prerequisites

To build and test Unexpected Keyboard locally:
- **OpenJDK 17** (or compatible JDK)
- **Android SDK**: Build tools and platform `android-36`
- **Git submodules**: Required for dictionary/spell-checking (`vendor/cdict`). Initialize with:
  ```sh
  git submodule update --init --recursive
  ```
- **Python 3.x**: Required for generator scripts (`gen_layouts.py`, `gen_method_xml.py`, `check_layout.py`, compose compiler).
- **fontforge**: Required if compiling the special keyboard icon font (`buildKeyboardFont` task).
- **keytool**: (Bundled with JDK) used to generate local development debug keystores.
- **Signing configuration**: See `.env.example` for required environment variables.

### Building & Testing
```sh
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

## Privacy & Clipboard Security

Unexpected Keyboard follows strict privacy and data protection principles:
- **Zero Logging & Telemetry**: Keystrokes, passwords, and user input are never logged or transmitted.
- **In-Memory Clipboard History**: The clipboard history is strictly **opt-in** (disabled by default in Settings). When enabled, clips are held only in temporary RAM and are **never** persisted to disk or database.
- **Clipboard Quick Paste**: The clipboard preview in the candidate bar is disabled by default. Enable it in Settings to show the current clipboard item for one-tap pasting.
- **Sensitive Data Safeguard**: On Android 13+ (API 33+), clips flagged as `ClipDescription.EXTRA_IS_SENSITIVE` (such as passwords, tokens, and 2FA OTPs) are discarded immediately and never added to history.
- **Auto-Expiration & Instant Clear**: Clipboard history automatically expires according to the user-selected TTL, and unchecking clipboard history immediately purges all stored items from memory.

## Contributing

For instructions on building the application, see
[Contributing](CONTRIBUTING.md).

## Acknowledgement

The [NLnet foundation](https://nlnet.nl/) funded the work on the spell checking feature.
