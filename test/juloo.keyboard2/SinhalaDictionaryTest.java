package juloo.keyboard2;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import juloo.cdict.Cdict;
import org.junit.Test;
import static org.junit.Assert.*;

public class SinhalaDictionaryTest
{
  @Test
  public void sinhalaDictionaryAssetExistsAndLoads() throws Exception
  {
    File dictFile = new File("assets/dict/si.dict");
    assertTrue("assets/dict/si.dict should exist", dictFile.exists());
    assertTrue("si.dict file size should be > 100KB", dictFile.length() > 100_000);

    byte[] data = Files.readAllBytes(dictFile.toPath());
    Cdict[] dicts = Cdict.of_bytes(data);
    assertNotNull("Cdict array should not be null", dicts);
    assertEquals("Should have exactly 1 main dictionary", 1, dicts.length);
    assertEquals("Dictionary name should be 'main'", "main", dicts[0].name);

    Cdict d = dicts[0];

    // Core words
    String[] coreWords = {
      "මම", "ඔයා", "අපි", "එයා", "ගෙදර", "වැඩ", "විශ්වවිද්‍යාලය",
      "ශ්‍රී", "ලංකා", "ස්තූතියි", "ආයුබෝවන්", "කරුණාකර", "සිංහල", "ප්‍රශ්නය", "ක්‍රිකට්",
      "සවුදි", "අරාබිය", "පන්සල්"
    };

    for (String w : coreWords)
    {
      Cdict.Result r = d.find(w);
      assertTrue("Dictionary should contain word: " + w, r.found);
      assertTrue("Word index should be >= 0", r.index >= 0);
      assertEquals("Decoded word at index should match original", w, d.word(r.index));
      assertTrue("Frequency should be valid (1-15)", d.freq(r.index) >= 1 && d.freq(r.index) <= 15);
    }
  }

  @Test
  public void sinhalaDictionaryPrefixAndSuffixSearch() throws Exception
  {
    byte[] data = Files.readAllBytes(Paths.get("assets/dict/si.dict"));
    Cdict d = Cdict.of_bytes(data)[0];

    // Prefix search for "සිං" (should find words like සිංහල)
    Cdict.Result r = d.find("සිං");
    int[] suffixes = d.suffixes(r, 10);
    assertTrue("Should find multiple suffix matches for prefix 'සිං'", suffixes.length > 0);

    boolean foundSinhala = false;
    for (int idx : suffixes)
    {
      String w = d.word(idx);
      if ("සිංහල".equals(w))
      {
        foundSinhala = true;
        break;
      }
    }
    assertTrue("Suffix search for 'සිං' should contain 'සිංහල'", foundSinhala);
  }

  @Test
  public void sinhalaDictionaryDistanceSearchForTypos() throws Exception
  {
    byte[] data = Files.readAllBytes(Paths.get("assets/dict/si.dict"));
    Cdict d = Cdict.of_bytes(data)[0];

    // "ආයුබෝවන" is distance 1 from "ආයුබෝවන්"
    int[] dists = d.distance("ආයුබෝවන", 1, 5);
    assertTrue("Distance search should find candidates", dists.length > 0);

    boolean foundAyubowan = false;
    for (int idx : dists)
    {
      if ("ආයුබෝවන්".equals(d.word(idx)))
      {
        foundAyubowan = true;
        break;
      }
    }
    assertTrue("Distance search for 'ආයුබෝවන' should find 'ආයුබෝවන්'", foundAyubowan);
  }

  @Test
  public void everyDictionaryWordHasAStableCdictLookupIndexAndValidUnicode() throws Exception
  {
    byte[] data = Files.readAllBytes(Paths.get("assets/dict/si.dict"));
    Cdict d = Cdict.of_bytes(data)[0];
    Set<String> seen = new HashSet<>();
    int index;
    for (index = 0; index < 100_000; index++)
    {
      String word = d.word(index);
      assertNotNull("word() must return a value at index " + index, word);
      if (!seen.add(word))
      {
        // Cdict returns the last word for an out-of-range index. Reaching it
        // immediately after all unique words proves the valid index range.
        assertEquals("Unexpected duplicate before the dictionary boundary", seen.size(), index);
        break;
      }
      for (int offset = 0; offset < word.length(); )
      {
        int cp = word.codePointAt(offset);
        assertTrue("Non-Sinhala code point in dictionary word: " + word,
            (cp >= 0x0D80 && cp <= 0x0DFF) || cp == 0x200D);
        offset += Character.charCount(cp);
      }
      Cdict.Result result = d.find(word);
      assertTrue("Every indexed word must be findable: " + word, result.found);
      assertEquals("Lookup index must round-trip for " + word, index, result.index);
      assertEquals("Word lookup must round-trip for " + word, word, d.word(result.index));
      assertTrue("Frequency must be within Cdict range for " + word,
          d.freq(result.index) >= 0 && d.freq(result.index) <= 15);
    }
    assertTrue("Dictionary index scan must find a boundary", index < 100_000);
    assertTrue("Dictionary should contain a substantial Sinhala vocabulary", seen.size() > 1_000);
    assertFalse(seen.contains("අවස්්ථාවක"));
    assertFalse(seen.contains("ආර්්ථික"));
    assertFalse(seen.contains("කිිසිසේත්"));
    assertFalse(seen.contains("කෑෑමට"));
  }

  @Test
  public void methodXmlDeclaresSinhalaDictionary() throws Exception
  {
    String content = new String(Files.readAllBytes(Paths.get("res/xml/method.xml")));
    assertTrue("method.xml should associate dictionary=si with si_LK",
        content.contains("imeSubtypeLocale=\"si_LK\"") &&
        content.contains("dictionary=si"));
  }

  @Test
  public void dictionariesXmlLocalesAreSorted() throws Exception
  {
    String xml = new String(Files.readAllBytes(Paths.get("res/values/dictionaries.xml")));
    assertTrue("dictionaries.xml must contain dict_name_si", xml.contains("dict_name_si"));
    assertTrue("dictionaries.xml must contain සිංහල", xml.contains("සිංහල"));
    assertTrue("dictionaries.xml must contain si locale item", xml.contains("<item>si</item>"));
  }
}
