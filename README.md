# Unexpected Keyboard

An open-source, lightweight, and privacy-focused Android Input Method Editor (IME) keyboard application optimized for performance, gestures, custom layouts, and Sinhala transliteration.

---

## 🌟 Features

* **🔒 Privacy First & Offline:** Works 100% offline with zero network permissions (`android.permission.INTERNET` is neither requested nor used).
* **🇱🇰 Sinhala Transliteration & Predictions:** Real-time phonetic Sinhala transliteration, intelligent word completion, and dictionary-based next-word predictions.
* **⚡ High Performance & Low Latency:** Optimized key event handling, low Garbage Collection (GC) pressure, and smooth gesture responsiveness.
* **🎨 Customizable Themes & Layouts:** Supports dynamic layout switching, custom XML layouts, and personalized color themes.
* **📋 Clipboard & Snippets:** Embedded clipboard history manager and smart snippet expansion (`//` and `..` shortcuts).
* **🔐 Direct Boot Aware:** Operates seamlessly even before the device is unlocked after a reboot using `DirectBootAwarePreferences`.
* **⌨️ Advanced Modifiers & Gestures:** Comprehensive support for Ctrl, Alt, Meta, Shift modifiers, cursor movement sliders, and macro execution.

---

## 🛠️ Tech Stack & Requirements

* **Language:** Java / Kotlin / C (JNI `cdict` dictionary library)
* **Build System:** Gradle 8.x with Kotlin DSL (`build.gradle.kts`)
* **Minimum SDK:** Android 5.0 (API Level 21)
* **Target SDK:** Android 14/15 (API Level 34/35)
* **JDK Version:** Java 17 or higher

---

## 🚀 Building & Installation

### Prerequisites
Ensure Android SDK and Java Development Kit (JDK 17+) are configured in your environment.

### 1. Clone the Repository
```bash
git clone https://github.com/Lakmal2078/Unexpected-Keyboard.git
cd Unexpected-Keyboard
```

### 2. Run Unit Tests
Validate the build and run all unit test suites (transliterator, layout, key handlers):
```bash
./gradlew test
```

### 3. Build Debug APK
Generate an unsigned debug APK for local testing:
```bash
./gradlew assembleDebug
```
The resulting APK will be saved to:
`app/build/outputs/apk/debug/Unexpected-Keyboard-debug.apk`

### 4. Build Release APK
Generate an optimized production release APK:
```bash
./gradlew assembleRelease
```
The output APK will be saved to:
`app/build/outputs/apk/release/Unexpected-Keyboard-release.apk`

---

## 🧪 Testing Coverage

The codebase includes test coverage across key components:
* **Key Handling & Gestures:** Event dispatch, macro loops, and latency tests.
* **Sinhala Transliterator:** Phonetic mapping, vowel/consonant combination rules, and dictionary safety.
* **Clipboard & Privacy:** History retention and direct boot safety policies.

---

## 📄 License

This project is licensed under the open-source license specified in the `LICENSE` file.
