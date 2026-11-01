# Unexpected Keyboard

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android Min SDK](https://img.shields.io/badge/Android-5.0%2B%20(API%2021)-green.svg)](build.gradle.kts)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-brightgreen.svg)](build.gradle.kts)
[![Translation status](https://hosted.weblate.org/widget/unexpected-keyboard/svg-badge.svg)](https://hosted.weblate.org/engage/unexpected-keyboard/)

[English](README.md) | [සිංහල (Sinhala)](README.si.md)

**Unexpected Keyboard** is a lightweight, privacy-focused virtual keyboard (Input Method Editor) for Android. It is designed around fast corner-swipe gestures, allowing users to type numbers, punctuation, navigation keys, and symbols without switching keyboard layouts.

This enhanced fork builds upon the original work by [Julow](https://github.com/Julow/Unexpected-Keyboard) by integrating a full **Sinhala phonetic (Singlish) transliteration suite**, context-aware word prediction, a **Gboard-style theme gallery** with real-time previews, squircle key styling, and hardened security controls.

---

## Screenshots

| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Keyboard Default View" width="220" /> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Keyboard Symbols & Gestures" width="220" /> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Theme Gallery Preview" width="220" /> |
| :---: | :---: | :---: |
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Sinhala Transliteration" width="220" /> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Suggestions & Autocomplete" width="220" /> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" alt="Settings & Customization" width="220" /> |

---

## Key Features

- **Corner Swipe Gestures:** Swipe toward any key corner to trigger alternate characters, symbols, and actions without switching pages or layers.
- **Sinhala Input Suite:**
  - **Singlish Phonetic Transliteration:** Type Latin letters to produce Sinhala script in real time (e.g., `ka` &rarr; `ක`, `sinhala` &rarr; `සිංහල`, `kramaya` &rarr; `ක්‍රමය`).
  - **Context-Aware Next-Word Prediction:** Predicts upcoming words based on preceding Sinhala context.
  - **Dictionary Autocomplete & Suggestions:** Fast, deterministic prefix-based suggestions backed by local dictionary files.
  - **Dedicated Physical Layout:** Includes the generated `sinhala_phonetic.xml` layout for direct Sinhala character placement.
- **Gboard-Style Theme Gallery:**
  - Visual theme picker categorized into Default, Light, Dark, Amoled, and System themes.
  - Live interactive keyboard preview before applying.
  - Key borders toggle with customized border width and squircle key shape support.
  - Dynamic system theme support (Material You / DayNight matching).
- **Clipboard History & Smart Snippets:**
  - Optional in-memory clipboard history with pinning support.
  - Expandable text snippets for repetitive input.
- **Privacy & Security by Design:**
  - All keystroke processing and transliteration occur 100% on-device.
  - No analytics, telemetry, trackers, or advertising SDKs.
  - Automatically disables word suggestions and transliteration in sensitive, password, or private input fields (`EditorInfo.TYPE_TEXT_VARIATION_PASSWORD`, `IME_FLAG_NO_PERSONALIZED_LEARNING`).
  - `allowBackup="false"` with strict backup extraction rules to prevent credential extraction.
  - Direct Boot aware (`android:directBootAware="true"`) to function securely before device decryption after restart.
- **Power Tools:**
  - Compose key sequence support for entering rare symbols and diacritics.
  - Integrated quick calculator and emoji selector.
  - Support for custom user layouts imported via XML.

---

## Privacy & Network Permissions

Unexpected Keyboard requests the following Android permissions:
- `android.permission.VIBRATE`: For haptic feedback on key presses.
- `android.permission.INTERNET`: **Strictly for optional, user-requested dictionary downloads.**
  - When you download a language dictionary from the in-app Dictionary Manager (`DictionariesActivity`), the app retrieves the compressed `.dict` file from the repository via HTTPS.
  - Decompressed downloads are strictly validated and capped at 64 MiB to prevent resource exhaustion attacks.
  - The keyboard **never** connects to the internet during regular typing or transliteration.

---

## Requirements

| Component | Minimum Version | Recommended / Target |
|---|---|---|
| **Android OS** | Android 5.0 (API level 21) | Android 16 (API level 36) |
| **JDK** | JDK 17 | JDK 17 or JDK 21 |
| **Android SDK** | Platforms;android-36 | Build-Tools 35.0.0+ |
| **Python** | Python 3.10 | Python 3.11+ (uses `StrEnum` in generators) |
| **FontForge** *(optional)* | Required only when modifying vector icons in `srcs/special_font/` | `fontforge` CLI |

---

## Quick Start & Installation

### Option 1: Install Pre-built APK

Download the latest APK release from GitHub Releases, then install via `adb`:

```sh
adb install -r Unexpected-Keyboard-release.apk
```

### Option 2: Enable Keyboard on Android

After installation:
1. Open Android **Settings** &rarr; **System** &rarr; **Languages & input** (or **General management** &rarr; **Keyboard list and default**).
2. Tap **Manage on-screen keyboards** and toggle on **Unexpected Keyboard**.
3. Select **Unexpected Keyboard** as your default input method.
4. Open the keyboard settings from your app launcher or swipe down-left on the bottom-left key.

---

## Building from Source

### 1. Clone the Repository

```sh
git clone https://github.com/Lakmal2078/Unexpected-Keyboard.git
cd Unexpected-Keyboard
```

### 2. Configure Environment

Ensure Android SDK path is specified in `local.properties`:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

Make sure the Gradle wrapper script is executable:

```sh
chmod +x gradlew
```

### 3. Build Debug APK

```sh
./gradlew assembleDebug
```

- **Output:** `build/outputs/apk/debug/Unexpected-Keyboard-debug.apk`
- The build also copies the output to `app/build/outputs/apk/debug/app-debug.apk` for compatibility with deployment scripts.

### 4. Build Signed Release APK

Release builds require signing credentials passed via environment variables:

| Variable | Description | Required |
|---|---|---|
| `RELEASE_KEYSTORE` | Path to release `.keystore` or `.jks` file | Yes |
| `RELEASE_KEYSTORE_PASSWORD` | Password for the keystore | Yes |
| `RELEASE_KEY_ALIAS` | Key alias name | Yes |
| `RELEASE_KEY_PASSWORD` | Password for the key alias | Yes |

#### Option A: Using Environment Variables

```sh
export RELEASE_KEYSTORE="/path/to/release.keystore"
export RELEASE_KEYSTORE_PASSWORD="your-keystore-password"
export RELEASE_KEY_ALIAS="your-key-alias"
export RELEASE_KEY_PASSWORD="your-key-password"

./gradlew assembleRelease
```

#### Option B: Using `.env` and `build_release.sh`

Create a local environment file from the safe template:

```sh
cp .env.example .env
chmod 600 .env
```

Edit `.env` and set the four signing values. The keystore path may be absolute or relative to the repository root. Then build:

```sh
./build_release.sh
```

- **Output:** `build/outputs/apk/release/Unexpected-Keyboard-release.apk`
- A build summary and size report is automatically generated at `build/reports/apk-release-report.txt`.

The script sources `.env` as a trusted, shell-compatible file. Only put values you control in it; do not download or source an untrusted env file.

### GitHub Actions release build

The existing `.github/workflows/make-apk.yml` workflow reads these repository Actions secrets:

| GitHub Actions secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Create each under **Repository → Settings → Secrets and variables → Actions → New repository secret**. For the keystore value, encode the binary file locally with `base64 -w 0 release.keystore` (Linux) and paste the output directly into the secret. Never add these values to workflow YAML, commits, issue comments, or chat.

The workflow builds on a `v*` tag or manual dispatch. A tagged run publishes the APK and its SHA-256 checksum to GitHub Releases.

> [!CAUTION]
> Never commit `.env` or production `.keystore` files to version control. Rotate any signing credentials that may have appeared in repository history before configuring new Actions secrets.

### 5. Building Inside Termux / PRoot (Android)

If building directly on an Android device using Termux or PRoot Ubuntu:
1. Set the AAPT2 binary override in `~/.gradle/gradle.properties`:
   ```properties
   android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
   ```
2. Build the APK:
   ```sh
   ./gradlew assembleDebug
   ```
3. Copy the APK to shared internal storage:
   ```sh
   # Inside Termux
   cp build/outputs/apk/debug/Unexpected-Keyboard-debug.apk ~/storage/shared/Download/

   # Inside PRoot
   cp build/outputs/apk/debug/Unexpected-Keyboard-debug.apk /sdcard/Download/
   ```

---

## Testing & Quality Verification

Run the test suites and code-verification scripts locally before submitting changes:

### 1. JVM Unit Tests

Executes all Android and transliteration unit tests:

```sh
./gradlew test
```

### 2. Sinhala Python Test Suite

Tests Sinhala phonetic transliteration rules, layout generation, dictionary safety, and malformed cdict parsing:

```sh
python3 -m unittest discover -s test/sinhala -p 'test_*.py'
```

### 3. Layout Integrity & Verification

Verifies all XML keyboard layout definitions against valid key codes:

```sh
python3 check_layout.py
```

### 4. Theme Contrast Verification (WCAG Compliance)

Checks foreground and background color combinations across all themes to ensure legibility and accessibility:

```sh
python3 check_theme_contrast.py
```

### 5. Sinhala Layout Drift Check

Ensure generated Sinhala phonetic layout XML matches the generator script:

```sh
python3 gen_sinhala_phonetic_layout.py
git diff --exit-code -- srcs/layouts/sinhala_phonetic.xml
```

---

## Project Structure

```text
Unexpected-Keyboard/
├── AndroidManifest.xml         # App manifest (IME service, permissions, activities)
├── build.gradle.kts            # Project build configuration and custom tasks
├── build_release.sh            # Release build helper script (reads .env)
├── check_layout.py             # Validator for XML keyboard layouts
├── check_theme_contrast.py     # WCAG contrast validator for themes
├── gen_sinhala_dict.py         # Sinhala dictionary compiler
├── gen_sinhala_phonetic_layout.py # Sinhala phonetic XML layout generator
├── assets/                     # Packaged app assets (special_font.ttf, dict/)
├── doc/                        # Layout documentation and key reference guides
│   ├── Custom-layouts.md       # Guide to writing custom XML layouts
│   └── Possible-key-values.md  # Key codes and symbol specifications
├── docs/                       # Architecture documentation and audit reports
│   ├── SINHALA_ARCHITECTURE.md # Detailed transliteration pipeline architecture
│   └── REPOSITORY_AUDIT.md     # Codebase security and performance audit
├── fastlane/                   # Metadata and screenshots
├── res/                        # Android resources (drawables, layouts, styles, themes)
├── srcs/
│   ├── compose/                # Compose key sequence tables and compiler
│   ├── java/juloo/keyboard2/   # Main application source code
│   │   ├── dict/               # Dictionary management and download logic
│   │   ├── prefs/              # Settings UI preferences and color pickers
│   │   ├── suggestions/        # Candidate view and suggestion logic
│   │   ├── Keyboard2.java      # Android InputMethodService entry point
│   │   ├── SinhalaTransliterator.java # Singlish transliteration engine
│   │   └── ThemePicker.java    # Gboard-style theme gallery implementation
│   ├── layouts/                # XML layout definitions for supported languages
│   └── special_font/           # Vector SVGs and FontForge script for keyboard glyphs
└── test/                       # Unit tests (JVM & Sinhala test suites)
```

---

## Troubleshooting

- **`bash: ./gradlew: Permission denied`**  
  The wrapper script lost its execute permission. Run `chmod +x gradlew` to fix it.
- **`AAPT2 process failed` on Termux / ARM**  
  Ensure `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` is set in your `~/.gradle/gradle.properties`.
- **`Cannot run program "fontforge"`**  
  FontForge is only needed if you edit icons in `srcs/special_font/`. Install it via your package manager (`sudo apt-get install fontforge` or `pkg install fontforge`), or avoid touching the special font sources during standard code edits.
- **`Release keystore does not exist`**  
  Verify the path passed to `RELEASE_KEYSTORE`. Use absolute paths or run via `./build_release.sh` with a valid `.env` file.
- **Transliteration not appearing in certain apps**  
  PasswordField or inputs specifying `IME_FLAG_NO_PERSONALIZED_LEARNING` disable suggestion bars and transliteration automatically to guarantee privacy and security.

---

## Documentation & References

- [Sinhala Documentation (සිංහල)](README.si.md): Native Sinhala build guide and privacy notes.
- [Sinhala Architecture Guide](docs/SINHALA_ARCHITECTURE.md): Deep dive into Singlish transliteration, character mappings, and layout generation.
- [Repository Audit Report](docs/REPOSITORY_AUDIT.md): Security, performance, and memory analysis report.
- [Custom Layouts Guide](doc/Custom-layouts.md): How to create and customize keyboard layouts.
- [Possible Key Values](doc/Possible-key-values.md): Complete list of supported key symbols and actions.

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
- For UI translations, contribute via [Weblate](https://hosted.weblate.org/engage/unexpected-keyboard/).
- Ensure all tests pass (`./gradlew test` and Sinhala Python tests) before submitting PRs.

---

## License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0). See [LICENSE](LICENSE) for full details.
Original project created by [Julow](https://github.com/Julow/Unexpected-Keyboard). Enhanced Sinhala and theme gallery additions by [Lakmal2078](https://github.com/Lakmal2078/Unexpected-Keyboard).
