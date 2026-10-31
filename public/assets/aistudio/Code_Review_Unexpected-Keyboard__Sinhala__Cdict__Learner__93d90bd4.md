# Code Review: Unexpected-Keyboard (Sinhala පරිවර්තන, ශබ්දකෝෂ සහ learner කොටස්)

Source: Unexpected-Keyboard-master (uploaded ZIP) · Review date: 2026-10-31

## සාරාංශය

repository එක (\~41,000 පේළි) extract කර ඇත. සම්පූර්ණ codebase එක මම කියවා නැත. Sinhala input, binary dictionary parser (`Cdict`), dictionary download සහ on-device learner (`SinhalaLearner`) යන ප්‍රධාන risk ඇති කොටස්වල විස්තරාත්මක static review එකක් කර ඇත. Build හෝ test run කර නැත. මූලික තත්ත්වය හොඳයි: Cdict parser එක offsets සහ cycles validate කරයි, download එකට size cap එකක් ඇත, sensitive input fields වලදී learning නවතී. එහෙත් privacy claim එක සහ disk growth ගැන ප්‍රධාන ගැටළු දෙකක් ඇත.

## විවේචනාත්මක ගැටළු (Critical / High)

| # | ගොනුව | පේළි | ගැටළුව | බරපතලකම |
| --- | --- | --- | --- | --- |
| 1 | `SinhalaLearner.java`, `DirectBootAwarePreferences.java` | `SinhalaLearner` 39; `DirectBootAwarePreferences` 12–16 | `SinhalaLearner` ටයිප් කළ වචන යුගල (\`prev | next` ) plaintext SharedPreferences වලට ලියයි. මේවා  `get\_protected\_prefs\` හරහා device-protected storage එකේ තබයි. ඒ ගොනුවේ comment එක කියන්නේ "no personal or sensitive information is stored there". එහෙත් එය පුද්ගලික typing history වේ. Class doc එක "never logged or transmitted" ද කියයි. Privacy documentation සහ implementation අතර එකඟතාවක් නැත. |
| 2 | `SinhalaLearner.java` | 30–33 (`removeEldestEntry`), 106–126, 130 | Memory cache එක 250 prev-words දක්වා සීමා කරයි. එහෙත් eviction වලදී SharedPreferences එකෙන් කිසිවක් මකන්නේ නැත. Class doc එක "bounded in size" ලෙස සඳහන් කරයි. Disk ගොනුව අසීමිතව වැඩි වේ. Restart වන විට `getAll()` මගින් සියලු keys ආපසු load වේ. | 🟠 High |

### ගැටළු 1 සඳහා නිරාකරණය

- Learner එකට ප්‍රමාණය සීමා කළ, පැහැදිලිව in-memory-only storage එකක් ලබා දෙන්න. නැත්නම් persistence opt-in කරන්න.
- `DirectBootAwarePreferences` comment එක නිවැරදි කරන්න. `Settings > Clear learned data` ලෙස විකල්පයක් (`clearLearnedData()` දැනටමත් ඇත) UI එකේ පෙන්වන්න.

### ගැටළු 2 සඳහා නිරාකරණය

- Eviction වලදී `removeEldestEntry` තුළ `prefs.edit().remove(...)` කරන්න. Startup වලදී ද එකම සීමාව enforce කරන්න.

## මධ්‍යම ගැටළු (Medium)

| # | ගොනුව | පේළි | ගැටළුව | යෝජනාව |
| --- | --- | --- | --- | --- |
| 3 | `Cdict.java` | 111, 131–133 | `validateTrie` එක node offset එකෙහි `HashMap<Integer,Integer>` භාවිතා කරයි. දෙවන dictionary load එකේදී ඇති දත්ත ප්‍රමාණය අනුව මෙය MB ගණනකට memory පරිභෝජනය කළ හැක. Dictionary load වන්නේ UI thread එකේද යන්න තහවුරු කර නැත. | `byte[]` / `BitSet` (`data.length` size) එකක් භාවිතා කරන්න. Visiting/Done states 2-bit වලින් පමණක් ගබඩා කරන්න. |
| 4 | `DictionaryListView.java` | 194–204 | Downloaded `.dict` ගොනුවට checksum හෝ signature පරීක්ෂාවක් නැත. Structural validation (`Cdict.of_bytes`) පමණක් ඇත. Repo එක compromise වුවහොත් වැරදි suggestions ලැබිය හැක. Code execution risk නැත, මන්ද ගොනුව data වේ. | Release එකේ SHA-256 manifest එකක් embed කර download එකේ verify කරන්න. |

## කුඩා ගැටළු (Low / Suggestions)

| # | ගොනුව | පේළි | යෝජනාව |
| --- | --- | --- | --- |
| 5 | `Cdict.java` | 318, 521–557 | `word()` 128-byte buffer එකක් භාවිතා කරයි. ඊට වැඩි වචනයක් නිහඬව කපනු ලැබේ. UTF-8 multi-byte අක්ෂරයක මැද කපුණොත් broken UTF-8 ලැබේ. Buffer එක ප්‍රමාණවත් ලෙස විශාල කරන්න, හෝ truncation නම් ඒ බව දන්වන්න. |
| 6 | `Cdict.java` | 373–382, 458–475 | `add_resolved_word` එක linear dedupe කරයි (O(n²) fuzzy results වලදී). `levenshtein` හැම call එකකම arrays allocate කරයි. Hot path එකේ `int[]` reuse කරන්න, dedupe එක `HashSet` ඔස්සේ කරන්න. |
| 7 | `SinhalaLearner.java` | 118–126 | Pruning එකේ off-by-one: නව වචනය minimum count එක ඇති පළමු entry එක වුවහොත් evict නොවී size එක 9 දක්වා වැඩිවිය හැක. Cap එක 8 නම්, අවම වශයෙන් tie-breaking එක deterministic කරන්න. |
| 8 | `SinhalaLearner.java` | 130 | හැම committed word එකකටම `prefs.edit().putInt(...).apply()` කරයි. SharedPreferences `apply()` මුළු XML ගොනුවම නැවත ලියයි. Entries වැඩිවන විට I/O වැඩි වේ. Batch කරන්න, හෝ SQLite වලට මාරු වන්න. |
| 9 | `Dictionaries.java` | `load_uncached` | `catch (Throwable t) { }` හිස් ගැනීමෙන් OOM වැනි දේවල් නිහඬ වේ. අවම වශයෙන් `Logs.exn` කරන්න. |
| 10 | `build.gradle.kts` | 85–86, 96–97 | Release build එකේ `isMinifyEnabled=false` සහ `isShrinkResources=false` ඇත. Security audit එකේ ද සඳහන් වේ. Release hardening සඳහා R8 නැවත enable කරන්න. |
| 11 | `test/sinhala/` | — | `SinhalaLearner` සඳහා eviction, pruning, persistence, sensitive-field skip ආවරණය කරන unit tests නැත. මේවා එකතු කරන්න. |

## හොඳ දේවල්

- **`Cdict` malformed-input වලට එරෙහි validation:** offsets, pointer bounds, cycle detection (iterative DFS, back-edge check), alias bounds සහ name termination හොඳින් පරීක්ෂා කරයි. Recursion depth attacker-controlled නොවේ.
- **Download guards:** `MAX_DICTIONARY_BYTES` (64 MB) සහ `read_bounded`, connect/read timeouts, `Accept-Encoding: identity` සහ load-check පෙර install කිරීම.
- **Sensitive inputs:** `KeyEventHandler` learner එකට record කිරීමට පෙර `is_sensitive_input` පරීක්ෂා කරයි. Password/number fields සඳහා `SuggestionPolicy` පැහැදිලිය.
- **Learner bounded caches:** `MAX_CANDIDATES_PER_WORD`, `MAX_WORD_LENGTH`, `isValidSinhalaWord` (ASCII digits/punctuation reject) හොඳ input sanitization වේ.
- **Documentation:** `docs/SINHALA_ARCHITECTURE.md` හොඳ ව්‍යුහයක් ඇති, තාක්ෂණික ලේඛනයකි.

