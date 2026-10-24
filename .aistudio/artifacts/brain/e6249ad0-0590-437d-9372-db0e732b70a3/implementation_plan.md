# Sinhala Context-Aware Next-Word Predictions & Typing Enhancements

A comprehensive architectural plan to introduce context-aware next-word predictions, expanded colloquial Singlish vocabulary, and a privacy-preserving dynamic on-device bigram learning engine for Sinhala input in Unexpected Keyboard.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed during interactive clarification and guide this implementation:

- **Confirmed Decision 1**: Prioritize **Sinhala typing and word prediction improvements** as the primary development focus.
- **Confirmed Decision 2**: Implement **Context-aware next-word predictions** as the core enhancement.
- **Architectural Decision (Privacy)**: Prediction learning will remain **100% on-device and local**, with zero network transmissions. Learning is strictly suspended in password and sensitive editor fields.
- **Architectural Decision (Performance)**: Pre-compiled static n-gram models with O(1) lookup + a bounded in-memory LRU frequency cache to ensure sub-millisecond keystroke responsiveness without GC churn.

---

## 1. Overview & Core Concept

- **What It Does**: Enhances Unexpected Keyboard's Sinhala transliteration engine to anticipate the user's next words in real time. Upon committing any Sinhala word (by typing space or selecting a candidate), the top suggestion bar immediately presents context-relevant next-word continuations (e.g., typing "සුබ" offers "උදෑසනක්", "දවසක්", "අලුත් අවුරුද්දක්"). In addition, the keyboard learns frequent word pairings typed by the user to adapt predictions over time.
- **Target Audience**: Sri Lankan users and Sinhala speakers communicating via WhatsApp, SMS, email, and social apps using Singlish phonetic typing.
- **Key Value**: Reduces keystrokes by up to 40% for typical Sinhala phrases, minimizes spelling friction, and speeds up daily messaging.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Typing with Real-Time Predictive Continuations**:
   - User types `subha` in Singlish -> candidate bar shows transliteration `සුබ`.
   - User taps space or selects `සුබ` -> word commits.
   - Candidates strip instantly updates with high-probability next words: `[ උදෑසනක් | දවසක් | අලුත් අවුරුද්දක් ]`.
   - Tapping any predicted word commits it immediately and generates the next tier of predictions (e.g. selecting `උදෑසනක්` predicts `[ වේවා | ප්‍රාර්ථනා | වේවා! ]`).
2. **Context-Aware Multi-Word Sentences**:
   - The engine analyzes multi-word window context before the cursor (e.g. Subject-Object-Verb patterns like "මම ගෙදර" -> "යනවා", "ආවා", "ඉන්නේ") rather than only isolated single tokens.
3. **Visual Distinctions on Candidate Bar**:
   - Prediction candidates are cleanly badged with subtle styling differences (e.g. forward arrow indicator `›` or distinct opacity) so the user immediately recognizes predictive suggestions versus active word transliterations.
   - Preserves quick emoji suggestions and quick clipboard paste previews seamlessly.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Static High-Frequency N-Gram Model vs Cloud API**
  - *Chosen Approach*: Curate a comprehensive, pre-indexed Sinhala corpus (~500+ common bigram transitions and trigram collocations) directly in the app.
  - *Why*: True offline capability, zero latency on keystroke, zero API cost, and complete user privacy.
  - *Alternatives Considered*: Firebase / Gemini cloud API (rejected because keyboards require <10ms offline response and must never send private keystrokes across the network).

- **Decision 2: Dynamic On-Device Personal Vocabulary Learning**
  - *Chosen Approach*: A bounded, encrypted/direct-boot-safe local cache of user-typed word pairs. When the user repeatedly types a custom sequence (e.g., "මාලක මල්ලී"), the local learner increases its transition frequency.
  - *Why*: Allows the keyboard to adapt to individual conversational styles and dialect preferences without bloating the base APK.
  - *Safety Boundary*: Suspended automatically when `EditorInfo.inputType` indicates password, numeric, or private browsing modes.

- **Decision 3: Multi-Token Backtrack vs Single-Token Lookup**
  - *Chosen Approach*: Check trigram context first (previous 2 words), fall back to bigram context (previous 1 word), and fall back to general high-frequency conversational openers if beginning a sentence.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      Android Keyboard IME                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ User keystroke / commit
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      KeyEventHandler                        │
│  - Captures committed Sinhala word                          │
│  - Queries InputConnection.getTextBeforeCursor()            │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐ ┌───────────────────────────┐
│  SinhalaPredictionEngine     │ │   DynamicBigramLearner    │
│  - Trigram matching          │ │   - Local LRU store       │
│  - Static Bigram tables      │ │   - Frequency counter     │
│  - Conversational openers    │ │   - Privacy filter        │
└──────────────┬───────────────┘ └────────────┬──────────────┘
               │                              │
               └──────────────┬───────────────┘
                              │ Merged ranked candidates
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Suggestions.java                       │
│  - set_predictions(List<String>)                            │
│  - Formats top 3 candidates + emoji slot                    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      CandidatesView                         │
│  - Renders interactive predictive pills                     │
│  - Handles tap-to-commit & recursive prediction chain       │
└─────────────────────────────────────────────────────────────┘
```

### Component Breakdown & Execution Plan

1. **`SinhalaPredictionEngine.java`**:
   - Extract and expand prediction logic from `SinhalaTransliterator.java` into a dedicated, clean prediction engine.
   - Core tables:
     - Greetings & Courtesies (`ආයුබෝවන්`, `සුබ`, `ස්තූතියි`, `කරුණාකර`, `සමාවෙන්න`).
     - Pronouns & Action Verbs (`මම`, `ඔයා`, `අපි`, `එයා`, `ඔබ`, `ඔයාලා`).
     - Spatial & Temporal Adverbs (`දැන්`, `හෙට`, `ඊයේ`, `අද`, `එහේ`, `මෙහේ`, `ගෙදර`).
     - Questions & Requests (`කොහොමද`, `මොකද`, `කොහෙද`, `කවද්ද`, `පුළුවන්ද`, `ඕනෙද`).
     - Noun-Verb collocations and postpositional markers (`ගැන`, `සඳහා`, `වෙත`, `සමඟ`).
2. **`SinhalaLearner.java` (Dynamic On-Device Learning)**:
   - Records transitions `(word_n-1, word_n)` when user commits text in regular editors.
   - Frequency-weighted scoring to prioritize personal names and everyday phrases.
   - Clear/reset option in Settings for full user control over learned dictionary data.
3. **`KeyEventHandler.java` Integration**:
   - Multi-word context inspection via `InputConnection.getTextBeforeCursor(40, 0)`.
   - Fires prediction update on Space, Enter, or suggestion tap.
4. **`CandidatesView.java` & `res/layout` Polish**:
   - Subtle visual differentiation for predictive continuations so the user clearly distinguishes transliterations from next-word predictions.
5. **Testing & Validation**:
   - Dedicated JUnit tests under `test/sinhala/ContextPredictionTest.java`.
   - Tests for sentence context, single-token fallback, unknown word handling, learning persistence, and private editor filtering.
   - Compilation and regression validation against existing test suites.
