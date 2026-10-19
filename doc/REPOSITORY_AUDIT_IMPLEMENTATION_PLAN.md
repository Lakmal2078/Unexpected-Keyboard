# Repository Audit — Implementation Plan

## Scope and status

This plan is based on a source-level review of the repository. It does not claim that Android builds, tests, or scanners have been run unless their results are linked from GitHub Actions.

## Phase 0 — Establish a baseline

Run from a configured Android development environment with the recursive submodules initialized:

```sh
git submodule update --init --recursive
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
python3 check_layout.py
```

Record existing failures before changing behavior. The emulator test requires a configured Android SDK/emulator; the app build may also require the documented NDK and font tooling.

## Phase 1 — Security automation

- Add CodeQL analysis for Java/Kotlin on pull requests, pushes to the primary branches, and a weekly schedule.
- Add Dependabot update schedules for Gradle and GitHub Actions.
- Review dependency changes in pull requests and fail the review when newly introduced vulnerabilities are high severity or above.
- Confirm results in GitHub Actions and the repository Security tab. A workflow file alone is not evidence that a scan completed successfully.

## Phase 2 — Transliterator data and lookup

- Preserve the existing transliteration tests as the behavior baseline.
- Move common-word and next-word prediction data to a validated resource format only after designing a loader that works with the current static API and Android resource lifecycle.
- Reject malformed data and detect duplicate keys.
- Make suggestion ordering explicit and deterministic.
- Benchmark the existing prefix scan against a trie or sorted-prefix index before selecting a replacement.
- Acceptance: legacy outputs and candidate order remain stable unless an intentional change is documented; tests cover empty input, exact matches, prefixes, duplicate candidates, punctuation, and Unicode.

## Phase 3 — Sinhala experience

- Complete user-facing Sinhala string resources while preserving Android format placeholders.
- Verify the source, license, format, and offline behavior of any Sinhala dictionary.
- Add a separate Wijesekara layout without replacing the existing phonetic layout.
- Expand phonetic/vowel coverage using table-driven tests and native-language review.
- Acceptance: resource compilation, layout validation, transliteration regression tests, and manual locale/device checks pass.

## Phase 4 — Hardening and optional features

- Verify downloaded dictionary data against a trusted SHA-256 checksum.
- Refactor large Java classes in small behavior-preserving changes.
- Pilot a compatible static analyzer and define a warning baseline.
- Expand candidate-view UI/device tests.
- Implement local user lexicon only as opt-in, disabled by default, and local-only.
- Add input scheme selection, Sinhala digit/punctuation long-press behavior, and Sinhala theme preview samples only with UI/accessibility tests.
- Verify whether Netlify deployment is actually configured before changing build/deployment settings.

## Privacy constraints

- No keystroke logging or transmission.
- Do not learn from typed text by default.
- Keep any user lexicon opt-in and on-device.
- Respect password/private-editor flags and existing suggestion suppression rules.

## Acceptance gate

Do not merge a phase until the relevant CI checks pass or any pre-existing failure is explicitly documented. Review workflow runs and security findings after opening the pull request.
