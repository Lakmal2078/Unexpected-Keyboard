## 2024-05-24 - Identifying Key Optimization
**Learning:** In Keyboard2View.java, the `onDraw` method loops through all keys and calls `drawKeyFrame`, `drawLabel`, `drawSubLabel` up to 8 times, and `drawIndication` for each key. The `drawKeyFrame` method uses `drawRoundRect` multiple times via `drawBorder` by using `clipRect` and `save()` / `restore()`. Also inside `drawLabel` and `drawSubLabel`, the `k.keys[0]` etc are modified with `modifyKey` over and over during every single draw call.
**Action:** The modification of keys in the draw loop is expensive because it invokes the modifier stack (`KeyModifier.modify`) on every render frame for every visible key and its sublabels, sometimes multiple times. A potential optimization is to compute the modified keys outside the draw loop or cache them. Wait, modifiers change dynamically (e.g., when Shift is pressed). However, maybe `drawKeyFrame` can be optimized, or perhaps we can avoid string operations inside the loop.
## 2024-05-24 - Identifying Key Optimization
**Learning:** In Keyboard2View.java, during `onDraw`, `modifyKey(kv, _mods)` is called up to 9 times per key to apply modifiers (shift, ctrl, fn, etc.). `KeyModifier.modify` iterates over the active modifiers list for every `modify` call, creating new KeyValue objects often, evaluating deep modifier switches. This adds a substantial overhead on every frame, considering there are ~30-40 keys on screen, leading to ~300+ `modifyKey` calls per frame, allocating a large number of temporary KeyValue and object wrappers on Android UI thread. Also, `_tmpRect.set` is used, but drawing labels calls `drawText`.
**Action:** Wait, we could cache the *modified* keys at the KeyboardData.Key level whenever the modifiers change, or we can just optimize `updateFlags()` to update an array/list of rendered keys.
If we can't easily cache, perhaps a simple caching layer inside Keyboard2View: `private final Map<KeyValue, KeyValue> _modifiedKeyCache = new HashMap<>()` which gets cleared inside `updateFlags()`! This would reduce `modifyKey` from running on every draw frame to ONLY running once per key per modifier state.
Actually, if modifiers don't change between draws (e.g. key pressed animation), we are re-evaluating `modifyKey` hundreds of times unnecessarily!
## 2024-05-24 - Identifying Key Optimization
**Learning:** We can memoize or cache the modified `KeyValue` for the labels, or simply cache them in the `KeyboardData.Key` but wait, `KeyboardData.Key` instances might be shared or immutable. A caching layer in `Keyboard2View` mapped by `KeyValue` makes sense because the same `KeyValue` will always modify to the same thing given the same `_mods`. However, what about `updateFlags()` clearing the cache? Yes, `updateFlags()` can clear the cache when `_mods` change! Actually, it's simpler:
```java
  private final Map<KeyValue, KeyValue> _modifiedKeyCache = new HashMap<>();

  private void updateFlags()
  {
    Pointers.Modifiers newMods = _pointers.getModifiers();
    if (!newMods.equals(_mods)) {
        _modifiedKeyCache.clear();
    }
    _mods = newMods;
    _config.handler.mods_changed(_mods);
  }

  public KeyValue modifyKey(KeyValue k, Pointers.Modifiers mods)
  {
    if (mods == _mods) {
      KeyValue cached = _modifiedKeyCache.get(k);
      if (cached != null) return cached;
      KeyValue modified = KeyModifier.modify(k, mods);
      // Wait, HashMap allows null values, but better to use a placeholder or check containsKey.
      if (modified == null) ...
    }
    return KeyModifier.modify(k, mods);
  }
```
Even simpler: `KeyModifier.modify` caches internally? No, modifiers are specific to the view. Caching in Keyboard2View is perfect.
## 2024-05-24 - Identifying Key Optimization
**Learning:** The CI workflow  is failing because the  file checks for  and  if the  or  environment variable is set to true. The workflow doesn't provide these variables or create a temporary keystore like the main  workflow does.
**Action:** Update  to include the key generation and environment variable setup for the debug keystore.
## 2024-05-24 - Identifying Key Optimization
**Learning:** The CI workflow `security-scan.yml` is failing because the `build.gradle.kts` file checks for `DEBUG_KEYSTORE_PASSWORD` and `DEBUG_KEY_PASSWORD` if the `CI` or `GITHUB_ACTIONS` environment variable is set to true. The workflow doesnt provide these variables or create a temporary keystore like the main `android.yml` workflow does.
**Action:** Update `security-scan.yml` to include the key generation and environment variable setup for the debug keystore.
## 2024-05-24 - Identifying Key Optimization
**Learning:** The CI workflow  failed at  because . This means that the emulator running  could not run the test APK because the application didn't include the required native libraries ( files) for the  architecture, or something related to native ABI. Wait,  has . Wait,  was logged during the packageDebug task. Let's look closer. Oh,  contains native code. The  is commented out! Wait, no,  is commented out in ? No, wait! I didn't change  in this PR. Why did this suddenly fail?
Actually, wait, let's look at  around :
```kotlin
  // externalNativeBuild {
  //   ndkBuild {
  //     path = file(vendor/Android.mk)
  //   }
  // }
```
It is commented out. Wait. I see  log:
```
2026-10-18T07:28:39.3639151Z There are no .so files available to package in the APK for x86_64.
```
Wait! Is it related to my changes? I didn't touch native libraries. But wait,  happens if  is missing. The  workflow runs:
```yaml
      - name: Run Android UI and device tests
        uses: ReactiveCircus/android-emulator-runner@v2
        with:
          api-level: 35
          arch: x86_64
          target: default
          script: ./gradlew connectedDebugAndroidTest
```
Let's see if this failure happens on  branch too? If  is commented out, maybe it never generates  files? Or maybe there are prebuilt  files in ? Let's check total 4
drwxrwxr-x 2 jules jules 4096 Oct 18 07:03 arm64-v8a.
## 2024-05-24 - Identifying Key Optimization
**Learning:**  is commented out in  on the base branch! Wait, it was commented out in . The original repo  has it active. But because it's commented out in ,  says 'There are no .so files available to package in the APK for x86_64.'. BUT wait! The CI emulator is x86_64 and since  exists,  gets packaged, but there is no ! Because  is commented out, Gradle just uses whatever is in . Since only  exists, the APK only has  native libs. When installing on an x86_64 emulator, adb fails with .
**Action:** The solution is to un-comment  in  so that it builds the  files for all ABIs (including x86_64) using NDK, or to modify  to run the emulator with  (though emulator for arm64 on x86 runner is extremely slow and probably not supported by the runner). Un-commenting  is the correct fix.
## 2024-05-24 - Identifying Key Optimization
**Learning:** `externalNativeBuild` is commented out in `build.gradle.kts` on the base branch! Because its commented out, `packageDebug` says There are no .so files available to package in the APK for x86_64. BUT the CI emulator is x86_64 and since `jniLibs.srcDirs` exists, `libs/arm64-v8a/libcdict_java.so` gets packaged, but there is no `libs/x86_64`! Because `externalNativeBuild` is commented out, Gradle just uses whatever is in `libs/`. Since only `libs/arm64-v8a` exists, the APK only has `arm64-v8a` native libs. When installing on an x86_64 emulator, adb fails with `INSTALL_FAILED_NO_MATCHING_ABIS: Failed to extract native libraries`.
**Action:** The solution is to un-comment `externalNativeBuild` in `build.gradle.kts` so that it builds the `.so` files for all ABIs (including x86_64) using NDK, or to modify `android.yml` to run the emulator with `arm64-v8a` (though emulator for arm64 on x86 runner is extremely slow and probably not supported by the runner). Un-commenting `externalNativeBuild` is the correct fix.
## 2024-05-24 - Identifying Key Optimization
**Learning:** `mergeDebugNativeLibs` failed because `libcdict_java.so` was found in both the `jniLibs` folder (`libs/arm64-v8a`) AND the newly built NDK output (`obj/local/arm64-v8a`). It seems `jniLibs.srcDirs("libs")` is what included the prebuilt `libcdict_java.so`. If we want to use the NDK build, we shouldn`t use the prebuilt one from `libs/`, or we should configure packaging options to pick the first one. Let us check `libs/` folder again: it contains prebuilt native libraries, so maybe `externalNativeBuild` was commented out ON PURPOSE to use prebuilt libraries (since building NDK is slow or complex in some environments) and maybe we are just missing `libs/x86_64/libcdict_java.so`? Or maybe `android.yml` runs `connectedDebugAndroidTest` on an `x86_64` emulator, which fails because the repo only contains prebuilt `arm64-v8a` libs. Wait! Why did `android.yml` pass before on master branch? Let us check `git log -p .github/workflows/android.yml`.
## 2024-05-24 - Identifying Key Optimization
**Learning:** `test-and-build` failed in `connectedDebugAndroidTest`. Wait, the previous PR was run on `main`/`master` and probably faced the same failure! Is the test suite actually failing on `main` because `libs/x86_64` is missing? Yes, since `libs/` is committed to git (with only `arm64-v8a`) and `externalNativeBuild` is commented out, the `x86_64` library is definitely missing. So any `x86_64` emulator will fail. Since I un-commented `externalNativeBuild`, the NDK build generates both `.so`s, but the `libs/` folder already has `arm64-v8a`, which causes a merge conflict in Gradle.
**Action:** I will add `pickFirst("lib/arm64-v8a/libcdict_java.so")` to `packaging.jniLibs` so that it doesn`t conflict, or just remove `libs/` from `jniLibs.srcDirs`, but it`s safer to use `pickFirst` or `excludes` on the prebuilt one if we use `externalNativeBuild`.
## 2024-05-24 - Optimization Learning Summary
**Learning:** `externalNativeBuild` was commented out, preventing `.so` generation for the x86_64 emulator. Added `pickFirst` rule to allow using the generated `.so` without colliding with the prebuilt one.
**Action:** Always verify emulator ABI availability in APK.
