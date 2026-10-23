# Unexpected Keyboard

Unexpected Keyboard is a lightweight, open-source Android input method (IME) focused on gesture-based typing, customizable layouts, and privacy. This repository also includes Sinhala phonetic transliteration and dictionary-based suggestions.

## Features

- **Gesture typing:** Swipe toward a key's corners to enter additional characters and actions.
- **Sinhala support:** Phonetic Singlish-to-Sinhala transliteration, word completion, and dictionary suggestions.
- **Custom layouts and themes:** Switch layouts and personalize keyboard appearance.
- **Clipboard and snippets:** Optional clipboard history and text-expansion shortcuts.
- **Privacy-focused design:** Text processing is performed on-device. The app requests `INTERNET` permission for optional dictionary downloads; review the in-app settings and source code for details.
- **Android integration:** Direct-boot-aware input service and modifier-key support.

## Requirements

- JDK 17 or newer
- Android SDK Platform 36
- Python 3
- FontForge (for building the special keyboard font)
- Android SDK command-line/build tools configured in your environment

The application supports Android 5.0 (API 21) and newer. The current Gradle configuration targets API 36.

## Build

Clone the repository and enter the project directory:

```sh
git clone https://github.com/Lakmal2078/Unexpected-Keyboard.git
cd Unexpected-Keyboard
```

Build a debug APK:

```sh
./gradlew assembleDebug
```

Output: `build/outputs/apk/debug/Unexpected-Keyboard-debug.apk`. The build also copies it to `app/build/outputs/apk/debug/app-debug.apk`.

Build and run unit tests:

```sh
./gradlew test
```

Build a release APK:

```sh
export RELEASE_KEYSTORE="/absolute/path/to/release.keystore"
export RELEASE_KEYSTORE_PASSWORD="..."
export RELEASE_KEY_ALIAS="..."
export RELEASE_KEY_PASSWORD="..."
./gradlew assembleRelease
```

Keep signing credentials and keystore files private; do not commit them. Release builds without signing credentials can produce an unsigned APK. The release output is `build/outputs/apk/release/Unexpected-Keyboard-release.apk`.

For local builds that store signing settings in `.env`, `./build_release.sh` loads that file and runs `clean assembleRelease`. Treat `.env` as a secret and never share its contents.

## Development

- Main Java sources: `srcs/java/`
- Android resources: `res/`
- Keyboard layouts and compose sequences: `srcs/layouts/`, `srcs/compose/`
- App assets and dictionaries: `assets/`
- Unit tests: `test/`
- Android instrumentation tests: `src/androidTest/`
- Build and generated-file scripts: root-level `*.py` and `scripts/`
- CI workflows: `.github/workflows/`

Generated files are updated by the Gradle tasks and scripts used in CI. Run `./gradlew test` before submitting changes and inspect `git status` to ensure generated changes are intentional.

## License

See [LICENSE](LICENSE) for the project's license.

## Sinhala documentation

සිංහල භාෂාවෙන් build කිරීම, privacy සහ clipboard ආරක්ෂාව පිළිබඳ වැඩිදුර තොරතුරු සඳහා [README.si.md](README.si.md) බලන්න.
