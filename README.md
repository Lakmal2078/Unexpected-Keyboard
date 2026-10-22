# Unexpected Keyboard

> A lightweight, privacy-focused, and developer-friendly Android virtual keyboard designed for speed, customization, and multilingual typing.

Unexpected Keyboard replaces traditional multi-page keyboard layouts with intuitive swipe gestures on individual keys. Swipe up, down, or towards corners to instantly access numbers, punctuation, special symbols, and modifier keys without switching screens.

---

## Key Features

- **Gesture-Based Keypad**: Access numbers, symbols, and navigation keys with quick corner and edge swipes.
- **Privacy first**: Sinhala transliteration runs on-device. The app requests `INTERNET` only for optional, user-initiated dictionary downloads; source review found no analytics/telemetry SDKs or code path that uploads typed text.
- **Singlish to Sinhala Auto-Transliteration**: Built-in phonetic engine (`SinhalaTransliterator`) backed by a lightweight binary dictionary (`si.dict`) for fast, offline Sinhala typing.
- **Unicode-aware input**: Sinhala input uses Android's `InputConnection` composing/commit APIs and includes regression coverage for Unicode vowel signs and conjuncts.
- **Smart Snippets & Calculator**: Instant text snippets (e.g., `//shrug` → `¯\_(ツ)_/¯`, `//sinhala` → `සිංහල`) and inline mathematical calculations.
- **Customizable Themes**: Multiple built-in light, dark, AMOLED, and high-contrast accessibility themes.

> [!NOTE]
> Unexpected Keyboard is engineered for privacy and low memory footprint. It contains no telemetry, analytics, or background tracking services.

---

## Quick Snippets

Unexpected Keyboard supports quick text expansions typed directly into any input field:

| Trigger | Output |
| :--- | :--- |
| `//shrug` | `¯\_(ツ)_/¯` |
| `//lenny` | `( ͡° ͜ʖ ͡°)` |
| `//peace` | `✌️` |
| `//sinhala` | `සිංහල` |
| `//lk` | `ශ්‍රී ලංකා` |
| `//ayubowan` | `ආයුබෝවන්!` |

---

## Building from Source

### Prerequisites

- Android SDK (API Level 34+)
- JDK 17 or higher
- Python 3.x (for layout and dictionary generation scripts)

### Local Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Lakmal2078/Unexpected-Keyboard.git
   cd Unexpected-Keyboard
   ```

2. Make the Gradle wrapper executable:
   ```bash
   chmod +x gradlew
   ```

3. Build the Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

The compiled APK will be available in `build/outputs/apk/debug/app-debug.apk`.

> [!TIP]
> To run localized dictionary compilation scripts independently, run `python3 gen_sinhala_dict.py`.

---

## Usage & Customization

1. Enable **Unexpected Keyboard** in Android System Settings under **Languages & Input > On-screen Keyboard**.
2. Select Unexpected Keyboard as your active Input Method Editor (IME).
3. Long-press the **Spacebar** or tap the **Language Toggle** key to switch layouts or toggle Sinhala transliteration on/off.
