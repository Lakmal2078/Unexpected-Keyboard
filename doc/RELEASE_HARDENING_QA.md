# Release hardening QA (AND-ACC-001 through AND-CFG-001)

## Automated checks

Run:

```sh
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

The release build must be signed using the project's configured release signing credentials. Do not distribute a locally generated debug-signed APK as a production release.

## AND-ACC-001 — TalkBack (manual device test required)

- Enable TalkBack and open a text field.
- Swipe through the keyboard: each physical key should be exposed as a focusable virtual button with a non-empty label.
- Explore by touch over keys; focus bounds should match the drawn key.
- Double-tap a character, Shift, Backspace, Space, and at least one action key.
- Verify the expected input/action occurs once and that ordinary multi-touch typing and swipe gestures still work with TalkBack disabled.
- Test portrait, landscape, numeric/special layouts, and at least one RTL or non-Latin layout.

## AND-PERF-001 — Rendering/touch performance

- Run a representative rapid typing session and capture Android Studio CPU/Memory profiler traces.
- Compare allocations and GC pauses before/after this change.
- Verify Paint instances remain owned/cached by Theme.Computed and RectF used by drawKeyFrame is reused.
- Do not claim zero-allocation until the profiler trace proves it; Android framework and text/accessibility internals can allocate independently.

## AND-SEC-001 — Clipboard history

- Confirm clipboard entries exist only in the service's in-memory list; do not persist clip contents to preferences, files, or databases.
- Verify Android 13+ clips marked EXTRA_IS_SENSITIVE are not added.
- Enable history, copy a non-sensitive string, destroy/recreate the IME, and verify the in-memory history is purged on teardown.
- Disable history and verify the list is cleared.
- Reboot the device and verify no previous history returns. Older Android versions cannot reliably detect the Android 13 sensitive-clip marker; keep history opt-in and volatile.
- Never place clipboard contents in logs or test failure messages.

## AND-CFG-001 — R8/minified release

- Run assembleRelease with release signing credentials.
- Install the minified APK and verify the IME can be enabled, opened, type text, invoke Settings, and handle keyboard switching.
- Inspect the R8 mapping and APK size; retain only rules justified by component entry points or actual reflection.
