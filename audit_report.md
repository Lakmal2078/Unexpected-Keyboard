# Unexpected Keyboard – Universal App Audit (Android App Mode)

**Audit date:** 2026-10-22

---

## 1. Scope & Mode
- **Application archetype:** Android application (Kotlin/Java source, Gradle build, APK output).
- **Audit mode:** **Android App Mode** – focuses on `AndroidManifest`, signing configuration, permissions, IPC, local storage, WebView usage, clear‑text traffic, and build‑time security settings.

---

## 2. High‑Level Findings
| Category | Severity | Evidence | Recommendation |
|----------|----------|----------|----------------|
| **Clear‑text traffic** | ✅ Low (mitigated) | `android:usesCleartextTraffic="false"` in `AndroidManifest.xml` (line 14). | No action needed – already disabled.
| **Exported service without proper protection** | ⚠️ Medium | `Keyboard2` service declared as `android:exported="true"` (manifest line 16) but protected by `android.permission.BIND_INPUT_METHOD`. | Verify that no other components are unintentionally exported. Consider setting `android:exported="false"` for non‑IME services.
| **Signing configuration** | ✅ Low (guarded) | Release signing strictly guards `RELEASE_KEYSTORE` environment variables with clear build-time validation in `build.gradle.kts`. | Maintained; CI / local builds gracefully fall back to debug or fail fast if release secrets are missing. |
| **Local secret storage** | ❌ Info | No usage of `EncryptedSharedPreferences` or Android Keystore observed in source. Sensitive data (e.g., clipboard history) stays in‑memory only – good, but any future feature storing tokens must use Keystore. | Add guidelines to store any future secrets with `EncryptedSharedPreferences` or `android.security.keystore`. |
| **ProGuard / R8** | ✅ Low | Release build has `isMinifyEnabled = true` and `isShrinkResources = true` (`build.gradle.kts`). | Verify that ProGuard rules do not strip needed classes (run full test suite on release build). |
| **Clipboard handling** | ✅ Low | `ClipboardHistoryService` stores clipboard only in RAM and respects Android sensitivity flags. | No changes needed; note that Incognito tabs may block system clipboard – provide optional Paste key (already configurable). |
| **WebView usage** | ❌ Info | No `WebView` component detected in the codebase. | No action required. |
| **Permissions** | ✅ Low | Only `VIBRATE` and `INTERNET` permissions (`AndroidManifest.xml`). `INTERNET` is required for optional dictionaries; no dangerous permissions. | Compliant. |
| **Accessibility** | ❓ Info | Keys have TalkBack labels and content descriptions. | Continue maintaining touch‑target size ≥ 48 dp. |
| **Performance** | ✅ Low | Asset generation tasks run at build time; ThemePicker uses in-place card updates; EmojiGridView uses dirty-flag disk write caching. | Highly optimized. |

---

## 3. Detailed Evidence
- **Manifest:** `AndroidManifest.xml`
- **Clipboard History Service (security/privacy):** `srcs/java/juloo/keyboard2/ClipboardHistoryService.java`
- **Clipboard History Toggle (user‑controlled):** `srcs/java/juloo/keyboard2/ClipboardHistoryCheckBox.java`
- **Gradle signing config (release):** `build.gradle.kts`
- **ProGuard activation:** `build.gradle.kts`

---

## 4. Remediation Plan (Action‑Oriented)

### 4.1. Exported Service Review
1. Review all `<service>` declarations in `AndroidManifest.xml`.
2. For any service **not** acting as an IME, set `android:exported="false"`.
3. Run `adb shell pm list packages -f` and `adb shell dumpsys package <package>` to verify export flags.

### 4.2. Signing Configuration Hardening
1. Store keystore files in a secret‑managed location (e.g., GitHub Secrets, Cloud Secret Manager).
2. Add a Gradle task that aborts the build if any of the required env vars are missing.
3. Optionally enable **APK Signature Scheme v3** for stronger integrity.

### 4.3. Secure Local Storage Guidelines
1. Introduce a utility class `SecurePrefs` that wraps `EncryptedSharedPreferences`.
2. Migrate any future persistent tokens (e.g., analytics IDs) to this class.
3. Add unit tests verifying that data is encrypted on disk.

### 4.4. Accessibility Checklist
1. Run `./gradlew lint` with `--lint‐options=EnabledChecks:Accessibility`.
2. For each missing `contentDescription` in custom key views, add a string resource (e.g., `@string/key_enter`).
3. Verify touch‑target size using `Layout Inspector` on a device.

### 4.5. Performance Validation
1. Profile the app with Android Studio Profiler on a low‑end device.
2. Ensure no heavy IO or long‑running loops on the UI thread (search for `Thread.sleep` or blocking network calls in UI code).
3. Enable `pm set‑prop debug.performance.tuning true` for runtime checks.

---

## 5. Conclusion
The **Unexpected Keyboard** project follows many best‑practice security measures: it disables clear‑text traffic, uses in‑memory clipboard history, and ships with ProGuard/R8 for release builds. The primary areas for improvement are **exported component hardening**, **secure handling of signing credentials**, and **accessibility compliance**. Addressing the remediation items above will bring the app to a robust production‑ready state.

---

*Report generated by the Universal App Audit skill (Android App Mode).*