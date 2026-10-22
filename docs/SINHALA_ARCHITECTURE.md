# Sinhala Transliteration Architecture

## Scope and current components

Sinhala input has two separate user-facing modes that should not be conflated:

1. **Singlish transliteration** converts Latin sequences typed into the keyboard (for example `ka`, `kaa`, `sinhala`) into Sinhala Unicode text.
2. **Physical Sinhala layout** maps physical key positions and modifier layers to Sinhala letters, vowel signs, symbols and joiners.

The mapping rules are different because one parses Latin phonetic sequences and the other maps key positions.

## Runtime transliteration flow

```text
Key event
  -> KeyEventHandler.send_text()
  -> append ASCII letters to the active Sinhala composing buffer
  -> SinhalaTransliterator.transliterateWord(buffer)
  -> InputConnection.setComposingText(...)
  -> separator / space / candidate selection commits the selected text
```

Sensitive editors disable Sinhala transliteration and word tracking. Do not call editor-text APIs in password/private fields.

The transliterator's runtime tables include:
- `COMMON_WORDS`: high-frequency spelling aliases and proper names.
- `NEXT_WORD_PREDICTIONS`: context-based next-word suggestions.
- `VOWELS_INDEPENDENT` and `VOWELS_DEPENDENT`: independent vowels and consonant-attached vowel signs.
- `CONSONANTS`: Latin consonant spellings, aspirated forms, special nasal/sangyaka forms, and conjunct support.

Prefix autocomplete keeps a deterministic order: shorter matching spelling first, then lexical order. That order is now computed once at class initialization rather than on every query.

## Physical layout generation flow

```text
gen_sinhala_phonetic_layout.py
  -> parse srcs/layouts/latn_qwerty_us.xml
  -> apply transitions and extra-character placements
  -> apply Sinhala key-level mapping and modifier maps
  -> escape combining Unicode signs as numeric XML entities
  -> write srcs/layouts/sinhala_phonetic.xml as UTF-8
  -> CI checks generated output has no diff
```

The generator requires Python 3.11 or newer (`StrEnum`). It raises a `LayoutGenError` for transition conflicts and now also raises `TypeError` for malformed escape-list entries.

## Compatibility and baseline tests

Run all Sinhala-focused Python checks:

```sh
python3 -m unittest discover -s test/sinhala -p 'test_*.py'
python3 gen_sinhala_phonetic_layout.py
git diff --exit-code -- srcs/layouts/sinhala_phonetic.xml
```

Run Android JVM tests and device tests:

```sh
./gradlew test
./gradlew connectedDebugAndroidTest
```

Current documented baselines include:

| Latin input | Expected Unicode |
|---|---|
| `ka` | ක |
| `kaa` | කා |
| `ki` | කි |
| `ku` | කු |
| `ke` | කෙ |
| `ko` | කො |
| `sinhala` | සිංහල |
| `kramaya` | ක්‍රමය |
| `vyanjana` | ව්‍යංජන |

The current repository contains pre-existing transliteration and dictionary tests. New tests under `test/sinhala/` keep basic mappings, consonants, vowels, compounds, edge cases, performance smoke checks, regressions, and keyboard integration easier to locate. Spelling aliases are not considered duplicate defects just because different Latin strings produce the same Unicode word.

## Adding a mapping safely

1. Add the smallest rule that matches the intended phonetic behavior; do not insert a common-word override if the rule should work systematically for every consonant.
2. For runtime sequences, keep multi-character forms before their shorter prefixes (for example `aae` before `ae`, and `chh` before `ch`).
3. Add an exact output assertion in the relevant `test/sinhala/*Test.java` file. Include uppercase/autocapitalization, punctuation, and adjacent-vowel cases when relevant.
4. For physical key placement, update the corresponding generator table and run the generator; review the XML diff on a device/layout viewer.
5. Run all test and generation commands above and have a Sinhala-speaking reviewer validate colloquial output before merging.

## Mapping-source migration (planned, not yet implemented)

The current physical layout definitions live in `gen_sinhala_phonetic_layout.py`; the runtime transliteration and vocabulary tables live in `SinhalaTransliterator.java`. They are not identical/duplicated mappings, so treating them as a single untyped dictionary would introduce ambiguity.

A future `srcs/sinhala/sinhala_phonetic.json` should have explicit schema sections for `transliteration`, `commonWords`, `predictions`, and `physicalLayout`. A generator can then emit both Java runtime data and physical XML. This migration must first establish a golden corpus of current inputs and outputs, detect duplicate keys within each section, and verify deterministic generation. It was intentionally not included in this remediation because mechanically migrating colloquial mapping rules without Sinhala-reviewed parity tests risks changing user-visible output.

## Release checklist

- [ ] Confirm all required vowel/consonant examples pass.
- [ ] Review conjunct, ZWJ, combining-mark, punctuation, whitespace and case regressions.
- [ ] Run the generator and ensure checked-in XML stays unchanged unless intentionally reviewed.
- [ ] Run JVM, instrumentation and lint tasks.
- [ ] Test composition, space commit and candidate replacement in ordinary, password and private editor fields.
- [ ] Inspect APK permissions, dependencies, network behavior and native files before release.
- [ ] Update changelog and ask a Sinhala-speaking reviewer to validate any mapping changes.
