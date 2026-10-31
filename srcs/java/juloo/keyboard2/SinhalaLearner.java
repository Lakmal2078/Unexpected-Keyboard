package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Privacy-preserving, on-device dynamic bigram learner for Sinhala text.
 * Learns frequent word transitions typed by the user to personalize predictions.
 * All data stays strictly on-device, bounded in size (max 250 previous words,
 * max 8 candidates per word), and is never logged or transmitted.
 * Entries evicted from memory are simultaneously purged from disk.
 * Users can wipe learned data at any time via clearLearnedData().
 */
public final class SinhalaLearner
{
  private static final String PREF_NAME = "sinhala_learned_bigrams";
  public static final int MAX_PREV_WORDS = 250;
  public static final int MAX_CANDIDATES_PER_WORD = 8;
  public static final int MAX_WORD_LENGTH = 30;

  public static final int MAX_CORRECTIONS = 200;

  // Keep weak references to live IME learner instances so a Settings action
  // can clear both persisted data and in-memory predictions without leaking them.
  private static final Map<SinhalaLearner, Boolean> ACTIVE_INSTANCES = new WeakHashMap<>();

  private final SharedPreferences _prefs;
  private final Map<String, String> _correctionsCache =
      new LinkedHashMap<String, String>(16, 0.75f, true)
      {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
        {
          boolean remove = size() > MAX_CORRECTIONS;
          if (remove && _prefs != null && eldest != null)
          {
            _prefs.edit().remove("corr|" + eldest.getKey()).apply();
          }
          return remove;
        }
      };
  private final Map<String, Map<String, Integer>> _memoryCache =
      new LinkedHashMap<String, Map<String, Integer>>(16, 0.75f, true)
      {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Integer>> eldest)
        {
          boolean remove = size() > MAX_PREV_WORDS;
          if (remove && _prefs != null && eldest != null)
          {
            String prevWord = eldest.getKey();
            Map<String, Integer> candidates = eldest.getValue();
            if (candidates != null && !candidates.isEmpty())
            {
              SharedPreferences.Editor editor = _prefs.edit();
              for (String nextWord : candidates.keySet())
              {
                editor.remove(prevWord + "|" + nextWord);
              }
              editor.apply();
            }
          }
          return remove;
        }
      };

  public SinhalaLearner(Context context)
  {
    _prefs = context != null
        ? DirectBootAwarePreferences.get_protected_prefs(context, PREF_NAME)
        : null;
    loadFromPreferences();
    registerInstance();
  }

  /**
   * For testing or pure in-memory execution.
   */
  public SinhalaLearner(SharedPreferences prefs)
  {
    _prefs = prefs;
    loadFromPreferences();
    registerInstance();
  }

  private void registerInstance()
  {
    synchronized (ACTIVE_INSTANCES)
    {
      ACTIVE_INSTANCES.put(this, Boolean.TRUE);
    }
  }

  private synchronized void loadFromPreferences()
  {
    if (_prefs == null)
      return;
    Map<String, ?> all = _prefs.getAll();
    if (all == null || all.isEmpty())
      return;

    for (Map.Entry<String, ?> entry : all.entrySet())
    {
      String key = entry.getKey();
      if (key.startsWith("corr|"))
      {
        String raw = key.substring(5);
        Object val = entry.getValue();
        if (val instanceof String && isValidSinhalaWord((String) val) && _correctionsCache.size() < MAX_CORRECTIONS)
        {
          _correctionsCache.put(raw, (String) val);
        }
        continue;
      }

      int sep = key.indexOf('|');
      if (sep <= 0 || sep >= key.length() - 1)
        continue;
      String prev = key.substring(0, sep);
      String next = key.substring(sep + 1);
      if (!isValidSinhalaWord(prev) || !isValidSinhalaWord(next))
        continue;
      Object val = entry.getValue();
      int count = (val instanceof Integer) ? (Integer) val : 1;

      Map<String, Integer> nexts = getOrCreateNexts(prev);
      if (nexts.size() < MAX_CANDIDATES_PER_WORD)
      {
        nexts.put(next, count);
      }
    }
  }

  private Map<String, Integer> getOrCreateNexts(String key)
  {
    Map<String, Integer> nexts = _memoryCache.get(key);
    if (nexts == null)
    {
      nexts = new HashMap<>();
      _memoryCache.put(key, nexts);
    }
    return nexts;
  }

  /**
   * Records a transition between two consecutive Sinhala words.
   * Suspends silently on non-Sinhala tokens, numbers, or malformed text.
   */
  public synchronized void recordTransition(String prevWord, String nextWord)
  {
    if (!isValidSinhalaWord(prevWord) || !isValidSinhalaWord(nextWord))
      return;
    if (prevWord.equals(nextWord))
      return;

    Map<String, Integer> nexts = getOrCreateNexts(prevWord);
    Integer countVal = nexts.get(nextWord);
    int current = (countVal != null) ? countVal : 0;
    int updated = current + 1;
    nexts.put(nextWord, updated);

    SharedPreferences.Editor editor = (_prefs != null) ? _prefs.edit() : null;

    // Prune excess next-words for this previous word to MAX_CANDIDATES_PER_WORD
    if (nexts.size() > MAX_CANDIDATES_PER_WORD)
    {
      String leastKey = null;
      int minCount = Integer.MAX_VALUE;
      for (Map.Entry<String, Integer> e : nexts.entrySet())
      {
        int count = e.getValue();
        if (count < minCount || (count == minCount && leastKey != null && e.getKey().compareTo(leastKey) < 0))
        {
          minCount = count;
          leastKey = e.getKey();
        }
      }
      if (leastKey != null)
      {
        nexts.remove(leastKey);
        if (editor != null)
        {
          editor.remove(prevWord + "|" + leastKey);
        }
      }
    }

    if (editor != null)
    {
      if (nexts.containsKey(nextWord))
      {
        editor.putInt(prevWord + "|" + nextWord, updated);
      }
      editor.apply();
    }
  }

  /**
   * Returns top dynamic predictions for the given previous word, ordered by frequency.
   */
  public synchronized List<String> getLearnedPredictions(String prevWord)
  {
    List<String> results = new ArrayList<>();
    if (prevWord == null || prevWord.isEmpty())
      return results;

    String clean = prevWord.trim();
    Map<String, Integer> nexts = _memoryCache.get(clean);
    if (nexts == null || nexts.isEmpty())
      return results;

    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(nexts.entrySet());
    Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>()
    {
      @Override
      public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b)
      {
        return b.getValue().compareTo(a.getValue());
      }
    });

    for (Map.Entry<String, Integer> entry : sorted)
    {
      results.add(entry.getKey());
    }
    return results;
  }

  public synchronized void recordCorrection(String rawSinglish, String chosenSinhala)
  {
    if (rawSinglish == null || chosenSinhala == null)
      return;
    String raw = rawSinglish.trim().toLowerCase(java.util.Locale.ROOT);
    if (raw.length() < 2 || raw.length() > MAX_WORD_LENGTH || !isValidSinhalaWord(chosenSinhala))
      return;
    _correctionsCache.put(raw, chosenSinhala);
    if (_prefs != null)
    {
      _prefs.edit().putString("corr|" + raw, chosenSinhala).apply();
    }
  }

  public synchronized String getLearnedCorrection(String rawSinglish)
  {
    if (rawSinglish == null)
      return null;
    return _correctionsCache.get(rawSinglish.trim().toLowerCase(java.util.Locale.ROOT));
  }

  public synchronized void clearLearnedData()
  {
    _memoryCache.clear();
    _correctionsCache.clear();
    if (_prefs != null)
      _prefs.edit().clear().apply();
  }

  /** Clear persisted learning and every live learner cache in this app process. */
  public static void clearAllLearnedData(Context context)
  {
    if (context == null)
      return;
    SharedPreferences prefs =
        DirectBootAwarePreferences.get_protected_prefs(context, PREF_NAME);
    clearAllLearnedData(prefs);
  }

  static void clearAllLearnedData(SharedPreferences prefs)
  {
    clearActiveCaches();
    if (prefs != null)
      prefs.edit().clear().apply();
  }

  private static void clearActiveCaches()
  {
    List<SinhalaLearner> learners;
    synchronized (ACTIVE_INSTANCES)
    {
      learners = new ArrayList<>(ACTIVE_INSTANCES.keySet());
    }
    for (SinhalaLearner learner : learners)
    {
      if (learner != null)
      {
        synchronized (learner)
        {
          learner._memoryCache.clear();
          learner._correctionsCache.clear();
        }
      }
    }
  }

  /**
   * Validates that token contains Sinhala characters and no digits/punctuation.
   */
  public static boolean isValidSinhalaWord(String word)
  {
    if (word == null)
      return false;
    String trimmed = word.trim();
    int len = trimmed.length();
    if (len < 2 || len > MAX_WORD_LENGTH)
      return false;

    boolean hasSinhala = false;
    for (int i = 0; i < len; i++)
    {
      char c = trimmed.charAt(i);
      // Sinhala Unicode block: 0x0D80 to 0x0DFF, plus ZWJ (0x200D) / ZWNJ (0x200C)
      if ((c >= '\u0D80' && c <= '\u0DFF') || c == '\u200D' || c == '\u200C')
      {
        hasSinhala = true;
      }
      else
      {
        // Contains ASCII digit, symbol, punctuation, or whitespace
        return false;
      }
    }
    return hasSinhala;
  }
}
