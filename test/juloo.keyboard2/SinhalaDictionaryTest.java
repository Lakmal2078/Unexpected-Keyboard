package juloo.keyboard2;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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
      "ශ්‍රී", "ලංකා", "ස්තූතියි", "ආයුබෝවන්", "කරුණාකර", "සිංහල", "ප්‍රශ්නය", "ක්‍රිකට්"
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
