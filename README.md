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

The transliterator in `srcs/java/juloo/keyboard2/SinhalaTransliterator.java` provides Singlish-to-Sinhala word candidates and next-word suggestions. Next-word suggestions use a fixed list keyed by the previous word, covering a limited set of common Sinhala phrases. They do not learn from typed text or use broader sentence context, so suggestions may be missing or unsuitable for other wording.

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
- **Android NDK**: `27.2.12479018`. Build both native dictionary libraries from
  source before assembling APKs; see [native build instructions](CONTRIBUTING.md#building-the-app).
- **Git submodules**: Required for dictionary/spell-checking (`vendor/cdict`). Initialize with:
  ```sh
  git submodule update --init --recursive
  ```
- **Python 3.x**: Required for generator scripts (`gen_layouts.py`, `gen_method_xml.py`, `check_layout.py`, compose compiler).
- **fontforge**: Required if compiling the special keyboard icon font (`buildKeyboardFont` task).
- **keytool**: (Bundled with JDK) used to generate local development debug keystores.
- **Signing configuration**: See `.env.example` for required environment variables.

### Building & Testing

#### 1. Build Debug APK
The debug build automatically generates a local debug keystore if one does not exist:
```sh
./gradlew assembleDebug
```
- **Output APK**: `build/outputs/apk/debug/Unexpected-Keyboard-debug.apk`
- Also copied to: `app/build/outputs/apk/debug/app-debug.apk`

#### 2. Build Release APK (Signed)
The release build requires signing environment variables (`RELEASE_KEYSTORE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).

If you have a `.env` file configured:
```sh
# Load signing secrets from .env
set -a && source .env && set +a

# Assemble optimized and signed release APK
./gradlew assembleRelease
```
- **Output APK**: `build/outputs/apk/release/Unexpected-Keyboard-release.apk`
- Automatically generates a size and file report (`build/reports/apk-release-report.txt`).

#### 3. Building in Termux / PRoot (Android)
If building directly on Android via Termux or a PRoot Linux environment:
1. Ensure the AAPT2 override is configured in `~/.gradle/gradle.properties`:
   ```properties
   android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
   ```
2. Build the APK variant:
   ```sh
   # Debug APK
   ./gradlew assembleDebug

   # Release APK
   set -a && source .env && set +a
   ./gradlew assembleRelease
   ```
3. Copy the APK to your phone storage / Downloads folder:
   ```sh
   # From PRoot environment:
   cp build/outputs/apk/release/Unexpected-Keyboard-release.apk /sdcard/Download/

   # From Termux native environment:
   cp build/outputs/apk/release/Unexpected-Keyboard-release.apk ~/storage/shared/Download/
   ```

#### 4. Additional Tasks
```sh
# Generate APK size and output file count report
./gradlew generateReleaseApkReport

# Run unit tests
./gradlew test
```

### Building Release APK with GitHub Actions

The repository includes automated GitHub Actions workflows to build and release APKs directly on GitHub:

- **`.github/workflows/build-apk.yml`**: Automatically builds a debug APK on every push to `main`/`master`, pull request, or manual trigger (`workflow_dispatch`).
- **`.github/workflows/make-apk.yml`**: Builds an optimized, signed release APK and publishes a GitHub Release when a version tag (`v*`) is pushed or manually triggered.

#### 1. Generate a Release Keystore

If you do not already have an Android release signing keystore, create one with `keytool` (included with JDK):

```sh
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias my-key-alias \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "your_keystore_password" \
  -keypass "your_key_password"
```

#### 2. Convert the Keystore to Base64

GitHub Actions secrets store plain text strings. Convert your binary `.keystore` file to Base64:

- **Linux / Git Bash**:
  ```sh
  base64 -w 0 release.keystore > keystore_base64.txt
  ```
- **macOS**:
  ```sh
  base64 -i release.keystore -o keystore_base64.txt
  ```
- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -Encoding ascii keystore_base64.txt
  ```

#### 3. Configure GitHub Repository Secrets

To enable automated release signing, add the following secrets to your GitHub repository:

1. In your repository on GitHub, navigate to **Settings** &gt; **Secrets and variables** &gt; **Actions**.
2. Click **New repository secret** for each required variable:

| Secret Name | Description | Example / Format |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | The Base64 string of your `.keystore` / `.jks` file | Entire content of `keystore_base64.txt` (one continuous line) |
| `RELEASE_KEYSTORE_PASSWORD` | Password used to unlock the keystore file | Value provided for `-storepass` |
| `RELEASE_KEY_ALIAS` | Alias name for the signing key entry | Value provided for `-alias` (e.g., `my-key-alias`) |
| `RELEASE_KEY_PASSWORD` | Password used to unlock the specific key entry | Value provided for `-keypass` |

#### 4. Triggering the Release Workflow

Once the secrets are configured, you can build the signed release APK using either of the following methods:

- **Option A: Push a Version Tag (Recommended)**
  Tag your commit with a `v` prefix and push it to GitHub:
  ```sh
  git tag v2.1.2
  git push origin v2.1.2
  ```
  The workflow compiles the code, signs the APK with your release key, generates ProGuard/R8 optimizations, and creates a new entry on your repository's **Releases** page with `Unexpected-Keyboard-release.apk` attached.

- **Option B: Manual Workflow Run via GitHub UI**
  1. Open the repository on GitHub and click the **Actions** tab.
  2. In the left workflow list, select **Build and release signed APK**.
  3. Click **Run workflow**, choose your target branch (e.g. `main`), and confirm.
  4. Once finished, download the signed APK from the **Artifacts** section of the run summary.


## Privacy & Clipboard Security

Unexpected Keyboard follows strict privacy and data protection principles:
- **Zero Logging & Telemetry**: Keystrokes, passwords, and user input are never logged or transmitted.
- **In-Memory Clipboard History**: The clipboard history is strictly **opt-in** (disabled by default in Settings). When enabled, clips are held only in temporary RAM and are **never** persisted to disk or database.
- **Clipboard Quick Paste**: The clipboard preview in the candidate bar is disabled by default. Enable it in Settings to show the current clipboard item for one-tap pasting on Android 7+ (API 24+). Previews are unavailable on older versions without clipboard sensitivity metadata.
- **Sensitive Data Safeguard**: On Android 7+ (API 24+), clips flagged with `android.content.extra.IS_SENSITIVE` (`ClipDescription.EXTRA_IS_SENSITIVE` on API 33+) are excluded from previews and history.
- **Private Editors**: Password fields and editors requesting `IME_FLAG_NO_PERSONALIZED_LEARNING` disable suggestions, text tracking, and translation. Host `TYPE_TEXT_FLAG_NO_SUGGESTIONS` requests override all candidate sources. Next-word suggestions use fixed data and do not learn from input.
- **Auto-Expiration & Instant Clear**: Clipboard history automatically expires according to the user-selected TTL, and unchecking clipboard history immediately purges all stored items from memory.

## Contributing

For instructions on building the application, see
[Contributing](CONTRIBUTING.md).

## Acknowledgement

The [NLnet foundation](https://nlnet.nl/) funded the work on the spell checking feature.
