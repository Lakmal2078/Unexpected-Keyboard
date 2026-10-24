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

/**
 * Privacy-preserving, on-device dynamic bigram learner for Sinhala text.
 * Learns frequent word transitions typed by the user to personalize predictions.
 * All data stays strictly on-device, bounded in size, and is never logged or transmitted.
 */
public final class SinhalaLearner
{
  private static final String PREF_NAME = "sinhala_learned_bigrams";
  private static final int MAX_PREV_WORDS = 250;
  private static final int MAX_CANDIDATES_PER_WORD = 8;
  private static final int MAX_WORD_LENGTH = 30;

  private final SharedPreferences _prefs;
  private final Map<String, Map<String, Integer>> _memoryCache =
      new LinkedHashMap<String, Map<String, Integer>>(16, 0.75f, true)
      {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Integer>> eldest)
        {
          return size() > MAX_PREV_WORDS;
        }
      };

  public SinhalaLearner(Context context)
  {
    _prefs = context != null
        ? DirectBootAwarePreferences.get_protected_prefs(context, PREF_NAME)
        : null;
    loadFromPreferences();
  }

  /**
   * For testing or pure in-memory execution.
   */
  public SinhalaLearner(SharedPreferences prefs)
  {
    _prefs = prefs;
    loadFromPreferences();
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
      String key = entry.getKey(); // formatted as "prevWord|nextWord"
      int sep = key.indexOf('|');
      if (sep <= 0 || sep >= key.length() - 1)
        continue;
      String prev = key.substring(0, sep);
      String next = key.substring(sep + 1);
      Object val = entry.getValue();
      int count = (val instanceof Integer) ? (Integer) val : 1;

      Map<String, Integer> nexts = _memoryCache.computeIfAbsent(prev, k -> new HashMap<>());
      nexts.put(next, count);
    }
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

    Map<String, Integer> nexts = _memoryCache.computeIfAbsent(prevWord, k -> new HashMap<>());
    int current = nexts.getOrDefault(nextWord, 0);
    int updated = current + 1;
    nexts.put(nextWord, updated);

    // Prune excess next-words for this previous word
    if (nexts.size() > MAX_CANDIDATES_PER_WORD)
    {
      String leastKey = null;
      int minCount = Integer.MAX_VALUE;
      for (Map.Entry<String, Integer> e : nexts.entrySet())
      {
        if (e.getValue() < minCount)
        {
          minCount = e.getValue();
          leastKey = e.getKey();
        }
      }
      if (leastKey != null && !leastKey.equals(nextWord))
      {
        nexts.remove(leastKey);
        if (_prefs != null)
        {
          _prefs.edit().remove(prevWord + "|" + leastKey).apply();
        }
      }
    }

    if (_prefs != null)
    {
      _prefs.edit().putInt(prevWord + "|" + nextWord, updated).apply();
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

  public synchronized void clearLearnedData()
  {
    _memoryCache.clear();
    if (_prefs != null)
    {
      _prefs.edit().clear().apply();
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
