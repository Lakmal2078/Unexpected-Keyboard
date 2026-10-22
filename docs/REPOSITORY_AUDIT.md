# Repository Audit Report

**Audited repository:** `Lakmal2078/Unexpected-Keyboard`  
**Base commit inspected:** `a57e2d10fcc3f1bf4ea53c296957f871168c560a`  
**Scope:** source, Android manifest/build scripts, generator, generated Sinhala layout, tests, workflows, and native-library integration. The local Gradle/Android build was not executed in this audit environment; CI remains the authoritative compile/device-test check.

## Executive summary

The audit found no confirmed Critical or High severity issue from the inspected source. Three confirmed defects/inefficiencies were fixed in this patch: the README's “zero network permissions” statement contradicted the manifest and dictionary download flow; suggestion lookup sorted and allocated the whole common-word list on every eligible keystroke; and the Sinhala layout generator silently ignored invalid escape-list configuration. Dictionary downloads are also now closed deterministically and capped at 64 MiB decompressed size.

This repository already had meaningful Sinhala, keyboard-input and private-editor tests. The gap was not “no tests”; rather, Sinhala tests were all kept in the general Java test package, the physical Sinhala layout generator had no generated-output CI check, and dedicated input-integration/performance smoke coverage was limited.

## Findings

### Critical

None confirmed.

### High

None confirmed.

### Medium

| ID | Type | Location (base-commit lines) | Root cause | Remediation |
|---|---|---|---|---|
| AUD-01 | Bug | `README.md:12`; `AndroidManifest.xml:4`; `DictionaryListView.java:166-214` | README claimed INTERNET permission was omitted, but the manifest requests it and the user can explicitly download optional dictionaries from a fixed HTTPS endpoint. | README now describes local transliteration and optional user-initiated dictionary downloads accurately. |
| AUD-02 | Performance Issue | `SinhalaTransliterator.java:850-867` | `getSuggestions` allocated a list and sorted every `COMMON_WORDS` entry for every prefix query, including repeated calls during typing. | Prefix order is precomputed once during static initialization, preserving length-then-lexical ordering. |
| AUD-03 | Risk | `DictionaryListView.java:183-210` | Gzip-expanded dictionary data was read without an application-level upper bound and streams were not consistently closed using try-with-resources. | Downloads and bundled fallback assets now use a 64 MiB decompressed-data ceiling and deterministic stream closure. |

### Low

| ID | Type | Location (base-commit lines) | Root cause | Remediation |
|---|---|---|---|---|
| AUD-04 | Bug | `gen_sinhala_phonetic_layout.py:263-274` | The invalid-`ESCAPE_LIST` branch constructed a `TypeError` but never raised it, allowing invalid configuration to be silently treated as “not escaped.” | The branch now raises the exception; regression test added. |
| AUD-05 | Maintainability Issue | `README.md:14`; `SinhalaInputHandler.java:12-65` | A helper advertises browser-specific grapheme deletion, but repository code search found no production call site. The main input path instead uses `KeyEventHandler`'s `InputConnection` composing/commit APIs. | README no longer promises an unconnected browser-specific optimization. Keep the helper unadvertised until an explicit integration and editor/device tests exist. |
| AUD-06 | Maintainability Issue | `build.gradle.kts:35,45,93`; `.github/workflows/android.yml`; `.github/workflows/make-apk.yml` | The app-side `juloo.cdict.Cdict` implementation is pure Java; no JNI native declaration or `System.loadLibrary` call was found. CI nevertheless built `libcdict_java.so` and Gradle configured native library packaging. | Removed unused ABI/JNI packaging configuration and NDK build steps from CI. The vendor submodule and makefile remain in the tree for now; deleting vendored source is a separate repository-history decision. |
| AUD-07 | Risk | `.github/workflows/check-generated.yml` | The generated-file workflow checked the layout list, compose data and method XML, but not the physical Sinhala layout emitted by `gen_sinhala_phonetic_layout.py`. | Added a Python unit-test step and a checked-in-output drift check. |
| AUD-08 | Risk | `srcs/java/juloo/keyboard2/SinhalaInputHandler.java` | Java `BreakIterator` grapheme boundaries can differ by Android/runtime version for complex script sequences; the helper does not have a production integration/device-level test. | Do not route production replacement through it without API-level tests for ZWJ, combining marks, selection ranges, and browsers. |

## Architecture review

- The main path is `KeyEventHandler.send_text → SinhalaTransliterator.transliterateWord → InputConnection.setComposingText`, followed by commit on separators or suggestion selection.
- `Suggestions` requests Sinhala candidates when transliteration is enabled and the editor is not marked sensitive.
- The Sinhala transliteration class is separate from the keyboard event-handler class, but it remains in the same Gradle application module/package. It is not yet a standalone Gradle module.
- The physical key layout (`srcs/layouts/sinhala_phonetic.xml`) and Singlish-to-Unicode runtime parser are different mapping domains. The former maps physical key/modifier positions; the latter parses sequences such as `ka`, `kaa`, and conjuncts. Combining them into one JSON file without separate schema sections would be misleading.
- Suggested future structure: a versioned JSON schema with separate `physicalLayout` and `transliteration` sections, generating physical XML and Java runtime tables, only after a golden corpus and deterministic-generator migration tests exist. This patch deliberately does not introduce an unverified mapping migration.

## Sinhala transliteration review

Existing `SinhalaTransliteratorTest.java` already covers the requested baseline (`ka → ක`, `kaa → කා`, `ki → කි`, `ku → කු`, `ke → කෙ`, `ko → කො`), standalone vowels, multiple consonant forms, conjuncts, punctuation, casing, malformed/empty input, prefix-suggestion stability, and regression vocabulary.

The parser's vowel/consonant arrays are ordered longest-first in the relevant cases (e.g. `aae` precedes `ae`, `chh` precedes `ch`). The source still intentionally supports several spelling aliases, so multiple Latin inputs producing the same Sinhala output are not automatically duplicates/bugs. No mapping corrections were made without a reliable oracle for colloquial Singlish; tests were added around current documented behavior rather than changing output semantics.

## Security and privacy

- `INTERNET` permission is real and needed by the optional dictionary downloader. The inspected downloader targets a fixed HTTPS GitHub raw-content URL, sets connect/read timeouts, validates bytes with `Cdict.of_bytes`, and does not send text-field contents.
- Source/Gradle inspection found no analytics, crash-reporting or advertising SDK dependency and no observed typed-text upload path. This is a bounded source audit, not proof about every downstream-built APK or dependency at runtime.
- The IME service is exported with Android's `BIND_INPUT_METHOD` permission, as required for an input method. Launcher is exported for its launcher intent; Settings and Dictionaries activities are not exported. No content provider or broadcast receiver is declared in the manifest.
- `allowBackup=false`; backup and device-transfer rules also exclude application data. This is conservative and consistent with a keyboard's sensitive-data surface.
- Sensitive editor handling is present in `EditorConfig`, `CurrentlyTypedWord`, `KeyEventHandler` and `Suggestions`; existing tests verify that password/private editor typing is not read for suggestions or transliterated.
- Clipboard history is intentionally in-memory, opt-in and capped; pinned clipboard content is a distinct user action and should be disclosed/documented separately from transient clipboard history.
- `no ads` is consistent with the inspected source/dependency tree, but only the shipped APK and all resolved dependencies can fully confirm release behavior.

## Native library review

- `vendor/cdict` is pinned to commit `4e29981795f6d143337511108f4e030269f3fab2`; its `LICENSE` at that commit is MIT.
- `vendor/Android.mk` builds `libcdict_java` from the C dictionary implementation and JNI wrapper, uses Android API 21 and sets a 16 KiB maximum ELF page size.
- The repository's app-side `srcs/java/juloo/cdict/Cdict.java` describes itself as a pure-Java reader and contains no native declaration or library loading. No app-source `System.loadLibrary` reference was found. The tracked Git tree does not contain `libs/**/*.so`; CI had been generating native binaries for packaging.
- Native source has a pinned source origin, but the runtime package does not appear to need it. This patch stops building/packaging the library rather than claiming to have audited a generated binary that is not in the source tree.
- Supported runtime ABIs now follow Android/Gradle defaults because this application no longer relies on JNI. Validate the built APK with `apkanalyzer files list` or `unzip -l` before release to confirm no `.so` remains.

## Build and test review

- Android Gradle Plugin 8.13.2, JDK 17 in CI, Android API 36, minSdk 21, targetSdk 36, JUnit 4 and Mockito were observed.
- Existing CI includes JVM unit tests, emulator instrumentation, debug APK build, lint and generated-file checks.
- `security-scan.yml` runs dependency reporting and Android lint but does not invoke a dedicated vulnerability scanner; treat the workflow name as broader than the checks it currently performs.
- New tests are located under `test/sinhala/` (Java files retain package `juloo.keyboard2` to match existing test conventions) plus a Python generator test. CI runs the Python tests and checks generated XML drift.

## Performance

A numeric baseline was not measured because this environment could inspect and edit GitHub sources but could not run the Android Gradle build or execute the project test suite locally. The suggestion change removes per-query creation of a full entry list and O(n log n) sort; each query still scans the pre-sorted list for prefix candidates. The download cap prevents malformed/oversized compressed assets from expanding without a bound.

## Remediation migration plan for a future JSON single source of truth

1. Preserve current output fixtures for both runtime phonetics and physical layout XML.
2. Define versioned `srcs/sinhala/sinhala_phonetic.json` with separate `transliteration`, `commonWords`, `predictions`, and `physicalLayout` sections (these are semantically different rule sets).
3. Generate a Java mapping class and `srcs/layouts/sinhala_phonetic.xml` from the JSON using Python 3.11+; do not parse JSON or sort maps during keystrokes.
4. Validate schema, unique input keys within each rule table, longest-prefix precedence, code points, XML parseability and deterministic output.
5. Run the old/new parser against a golden corpus; review every output change with Sinhala-speaking maintainers before switching runtime.
6. Only then remove the inline Java arrays and Python generator mapping tables.

## Release checklist

- [ ] `python3 -m unittest discover -s test/sinhala -p 'test_*.py'`
- [ ] `./gradlew test`
- [ ] `./gradlew connectedDebugAndroidTest`
- [ ] `./gradlew lintDebug`
- [ ] `git diff --exit-code -- srcs/layouts/sinhala_phonetic.xml` after running the generator
- [ ] Confirm private/password editor behavior and suggestion commit on a real device/browser.
- [ ] Inspect the final APK for permissions, unexpected SDKs, and native libraries.
- [ ] Verify release signing inputs are secret-managed and the changelog accurately describes optional dictionary networking.
