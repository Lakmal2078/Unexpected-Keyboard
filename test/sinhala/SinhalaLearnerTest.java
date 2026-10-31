package juloo.keyboard2;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SinhalaLearnerTest
{
  private FakeSharedPreferences fakePrefs;
  private SinhalaLearner learner;

  @Before
  public void setUp()
  {
    fakePrefs = new FakeSharedPreferences();
    learner = new SinhalaLearner(fakePrefs);
  }

  @Test
  public void testRecordTransitionAndFrequencyOrdering()
  {
    learner.recordTransition("මම", "යනවා");
    learner.recordTransition("මම", "යනවා");
    learner.recordTransition("මම", "එනවා");

    List<String> preds = learner.getLearnedPredictions("මම");
    assertNotNull(preds);
    assertEquals(2, preds.size());
    assertEquals("යනවා", preds.get(0)); // count 2
    assertEquals("එනවා", preds.get(1)); // count 1

    // Verify persisted in SharedPreferences
    assertEquals(2, fakePrefs.getInt("මම|යනවා", 0));
    assertEquals(1, fakePrefs.getInt("මම|එනවා", 0));
  }

  @Test
  public void testCandidatePruning_neverExceedsMaxCandidates()
  {
    String prev = "සුබ";
    String[] nextWords = new String[] {
      "පැතුම්", "උදෑසනක්", "දවසක්", "සන්ධ්‍යාවක්",
      "රාත්‍රියක්", "අලුත්", "ගමනක්", "වැඩක්",
      "ආරම්භයක්", "ජයග්‍රහණයක්" // 10 candidates
    };

    for (String next : nextWords)
    {
      learner.recordTransition(prev, next);
    }

    List<String> preds = learner.getLearnedPredictions(prev);
    assertTrue("Candidates count must not exceed MAX_CANDIDATES_PER_WORD",
        preds.size() <= SinhalaLearner.MAX_CANDIDATES_PER_WORD);
    assertEquals(SinhalaLearner.MAX_CANDIDATES_PER_WORD, preds.size());

    // Verify SharedPreferences also pruned excess keys
    int diskKeysForPrev = 0;
    for (String key : fakePrefs.getAll().keySet())
    {
      if (key.startsWith(prev + "|"))
      {
        diskKeysForPrev++;
      }
    }
    assertEquals(SinhalaLearner.MAX_CANDIDATES_PER_WORD, diskKeysForPrev);
  }

  @Test
  public void testMemoryEvictionPurgesFromDisk()
  {
    // Insert more than MAX_PREV_WORDS to trigger LRU eviction
    int totalWords = SinhalaLearner.MAX_PREV_WORDS + 10;
    List<String> prevWords = new ArrayList<>();

    // Generate strictly unique Sinhala words: 17 * 16 = 272 possible unique 2-letter words
    for (int i = 0; i < totalWords; i++)
    {
      char c1 = (char) (0x0D9A + (i / 16)); // Sinhala consonants
      char c2 = (char) (0x0DAB + (i % 16)); // Sinhala consonants
      String word = "" + c1 + c2;
      prevWords.add(word);
      learner.recordTransition(word, "යනවා");
    }

    // Eldest words should have been evicted from disk
    String eldestWord = prevWords.get(0);
    assertFalse("Eldest word transition should be purged from disk on eviction",
        fakePrefs.contains(eldestWord + "|යනවා"));

    // Most recent word should still be present
    String newestWord = prevWords.get(totalWords - 1);
    assertTrue("Newest word transition should exist on disk",
        fakePrefs.contains(newestWord + "|යනවා"));
  }

  @Test
  public void testClearLearnedData()
  {
    SinhalaLearner secondActiveLearner = new SinhalaLearner(fakePrefs);
    learner.recordTransition("මම", "යනවා");
    learner.recordTransition("ඔබ", "එනවා");
    secondActiveLearner.recordCorrection("amma", "අම්මා");
    assertFalse(fakePrefs.getAll().isEmpty());

    learner.clearLearnedData();

    assertTrue("First active learner cache must be empty",
        learner.getLearnedPredictions("මම").isEmpty());
    assertTrue("Other active learner cache must be empty",
        secondActiveLearner.getLearnedPredictions("මම").isEmpty());
    assertNull("Learned corrections must be cleared",
        secondActiveLearner.getLearnedCorrection("amma"));
    assertTrue("SharedPreferences must be empty", fakePrefs.getAll().isEmpty());
  }

  @Test
  public void testInputValidation_rejectsInvalidTokens()
  {
    // English words
    assertFalse(SinhalaLearner.isValidSinhalaWord("hello"));
    assertFalse(SinhalaLearner.isValidSinhalaWord("Sinhala"));

    // Digits and mixed numbers
    assertFalse(SinhalaLearner.isValidSinhalaWord("123"));
    assertFalse(SinhalaLearner.isValidSinhalaWord("මම1"));

    // Punctuation and spaces
    assertFalse(SinhalaLearner.isValidSinhalaWord("මම."));
    assertFalse(SinhalaLearner.isValidSinhalaWord("මම යනවා"));
    assertFalse(SinhalaLearner.isValidSinhalaWord(null));
    assertFalse(SinhalaLearner.isValidSinhalaWord(""));
    assertFalse(SinhalaLearner.isValidSinhalaWord("ක")); // 1 character is < 2

    // Valid Sinhala words with modifiers and ZWJ
    assertTrue(SinhalaLearner.isValidSinhalaWord("මම"));
    assertTrue(SinhalaLearner.isValidSinhalaWord("ශ්‍රී")); // contains ZWJ 0x200D
    assertTrue(SinhalaLearner.isValidSinhalaWord("ලංකා"));
    assertTrue(SinhalaLearner.isValidSinhalaWord("ආයුබෝවන්"));
  }

  @Test
  public void testIdenticalWordTransitionRejected()
  {
    learner.recordTransition("මම", "මම");
    assertTrue("Identical word repetition must not be learned as a bigram",
        learner.getLearnedPredictions("මම").isEmpty());
  }

  @Test
  public void testRecordCorrectionAndRetrieval()
  {
    assertNull(learner.getLearnedCorrection("amma"));
    learner.recordCorrection("amma", "අම්මා");
    assertEquals("අම්මා", learner.getLearnedCorrection("amma"));
    assertEquals("අම්මා", learner.getLearnedCorrection("AMMA")); // case-insensitive

    // Persisted in SharedPreferences
    assertEquals("අම්මා", fakePrefs.getString("corr|amma", null));

    // Clear data clears corrections too
    learner.clearLearnedData();
    assertNull(learner.getLearnedCorrection("amma"));
    assertNull(fakePrefs.getString("corr|amma", null));
  }

  /**
   * Lightweight in-memory fake SharedPreferences for testing.
   */
  private static class FakeSharedPreferences implements SharedPreferences
  {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public Map<String, ?> getAll()
    {
      return new HashMap<>(map);
    }

    @Override
    public String getString(String key, String defValue)
    {
      Object val = map.get(key);
      return val instanceof String ? (String) val : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues)
    {
      Object val = map.get(key);
      return val instanceof Set ? new HashSet<>((Set<String>) val) : defValues;
    }

    @Override
    public int getInt(String key, int defValue)
    {
      Object val = map.get(key);
      return val instanceof Integer ? (Integer) val : defValue;
    }

    @Override
    public long getLong(String key, long defValue)
    {
      Object val = map.get(key);
      return val instanceof Long ? (Long) val : defValue;
    }

    @Override
    public float getFloat(String key, float defValue)
    {
      Object val = map.get(key);
      return val instanceof Float ? (Float) val : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue)
    {
      Object val = map.get(key);
      return val instanceof Boolean ? (Boolean) val : defValue;
    }

    @Override
    public boolean contains(String key)
    {
      return map.containsKey(key);
    }

    @Override
    public Editor edit()
    {
      return new FakeEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    private class FakeEditor implements Editor
    {
      private final Map<String, Object> pending = new HashMap<>();
      private final Set<String> removed = new HashSet<>();
      private boolean clearAll = false;

      @Override
      public Editor putString(String key, String value)
      {
        pending.put(key, value);
        removed.remove(key);
        return this;
      }

      @Override
      public Editor putStringSet(String key, Set<String> values)
      {
        pending.put(key, values);
        removed.remove(key);
        return this;
      }

      @Override
      public Editor putInt(String key, int value)
      {
        pending.put(key, value);
        removed.remove(key);
        return this;
      }

      @Override
      public Editor putLong(String key, long value)
      {
        pending.put(key, value);
        removed.remove(key);
        return this;
      }

      @Override
      public Editor putFloat(String key, float value)
      {
        pending.put(key, value);
        removed.remove(key);
        return this;
      }

      @Override
      public Editor putBoolean(String key, boolean value)
      {
        pending.put(key, value);
        removed.remove(key);
        return this;
      }

      @Override
      public Editor remove(String key)
      {
        removed.add(key);
        pending.remove(key);
        return this;
      }

      @Override
      public Editor clear()
      {
        clearAll = true;
        pending.clear();
        removed.clear();
        return this;
      }

      @Override
      public boolean commit()
      {
        apply();
        return true;
      }

      @Override
      public void apply()
      {
        if (clearAll)
        {
          map.clear();
        }
        for (String r : removed)
        {
          map.remove(r);
        }
        map.putAll(pending);
      }
    }
  }
}
