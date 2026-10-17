package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class SinhalaTransliteratorTest
{
  @Test
  public void testRequestedWords()
  {
    // The specific words requested by the user
    assertEquals("අම්මා", SinhalaTransliterator.transliterate("amma"));
    assertEquals("සිංහල", SinhalaTransliterator.transliterate("sinhala"));
    assertEquals("කොහොමද", SinhalaTransliterator.transliterate("kohomada"));
    assertEquals("ලක්මාල්", SinhalaTransliterator.transliterate("lakmal"));
  }

  @Test
  public void testMultipleSuggestions()
  {
    java.util.List<String> lakmalSug = SinhalaTransliterator.getSuggestions("lakmal");
    assertEquals("ලක්මාල්", lakmalSug.get(0)); // Primary
    assertTrue(lakmalSug.contains("ලක්මල්"));  // Secondary phonetic alternative
    assertTrue(lakmalSug.contains("lakmal"));  // English raw word

    java.util.List<String> ammaSug = SinhalaTransliterator.getSuggestions("amma");
    assertEquals("අම්මා", ammaSug.get(0));
    assertTrue(ammaSug.contains("අම්ම"));
  }

  @Test
  public void testCommonConversationalWords()
  {
    assertEquals("තාත්තා", SinhalaTransliterator.transliterate("thaththa"));
    assertEquals("අක්කා", SinhalaTransliterator.transliterate("akka"));
    assertEquals("අයියා", SinhalaTransliterator.transliterate("aiyya"));
    assertEquals("මල්ලි", SinhalaTransliterator.transliterate("malli"));
    assertEquals("නංගි", SinhalaTransliterator.transliterate("nangi"));
    assertEquals("මොකද", SinhalaTransliterator.transliterate("mokada"));
    assertEquals("ලංකා", SinhalaTransliterator.transliterate("lanka"));
    assertEquals("ආයුබෝවන්", SinhalaTransliterator.transliterate("ayubowan"));
    assertEquals("ස්තූතියි", SinhalaTransliterator.transliterate("sthuthi"));
  }

  @Test
  public void testPhoneticRules()
  {
    // Systematic phonetic rules
    // Vowels
    assertEquals("අ", SinhalaTransliterator.transliterate("a"));
    assertEquals("ආ", SinhalaTransliterator.transliterate("aa"));
    assertEquals("ඇ", SinhalaTransliterator.transliterate("ae"));
    assertEquals("ඉ", SinhalaTransliterator.transliterate("i"));
    assertEquals("උ", SinhalaTransliterator.transliterate("u"));
    assertEquals("එ", SinhalaTransliterator.transliterate("e"));
    assertEquals("ඔ", SinhalaTransliterator.transliterate("o"));

    // Consonant + Vowels
    assertEquals("ක", SinhalaTransliterator.transliterate("ka"));
    assertEquals("කා", SinhalaTransliterator.transliterate("kaa"));
    assertEquals("කැ", SinhalaTransliterator.transliterate("kae"));
    assertEquals("කි", SinhalaTransliterator.transliterate("ki"));
    assertEquals("කී", SinhalaTransliterator.transliterate("kii"));
    assertEquals("කු", SinhalaTransliterator.transliterate("ku"));
    assertEquals("කූ", SinhalaTransliterator.transliterate("kuu"));
    assertEquals("කෙ", SinhalaTransliterator.transliterate("ke"));
    assertEquals("කේ", SinhalaTransliterator.transliterate("kee"));
    assertEquals("කො", SinhalaTransliterator.transliterate("ko"));
    assertEquals("කෝ", SinhalaTransliterator.transliterate("koo"));
    assertEquals("ක්", SinhalaTransliterator.transliterate("k"));

    // Conjuncts: Rakaranshaya and Yanshaya
    assertEquals("ක්‍රමය", SinhalaTransliterator.transliterate("kramaya"));
    assertEquals("ව්‍යංජන", SinhalaTransliterator.transliterate("vyanjana"));
  }

  @Test
  public void testSentencesWithPunctuation()
  {
    assertEquals("අම්මා, කොහොමද?", SinhalaTransliterator.transliterate("amma, kohomada?"));
    assertEquals("සුබ උදෑසනක්!", SinhalaTransliterator.transliterate("suba udaesanak!"));
    assertEquals("සිංහල භාෂාව", SinhalaTransliterator.transliterate("sinhala bhashawa"));
  }

  @Test
  public void testUserReportedWordsAndPhrases()
  {
    assertEquals("ගමගේ", SinhalaTransliterator.transliterate("gamage"));
    assertEquals("ගමගේ", SinhalaTransliterator.transliterate("gamagee"));
    assertEquals("නැතුව", SinhalaTransliterator.transliterate("nethuwa"));
    assertEquals("විදියට", SinhalaTransliterator.transliterate("widiyata"));
    assertEquals("හරියට", SinhalaTransliterator.transliterate("hariyata"));
    assertEquals("හදන්න", SinhalaTransliterator.transliterate("hadanna"));
    assertEquals("පුළුවන්ද", SinhalaTransliterator.transliterate("puluwanda"));
    assertEquals("වැඩ", SinhalaTransliterator.transliterate("wada"));
    assertEquals("කරනවා", SinhalaTransliterator.transliterate("karanawa"));
    assertEquals("යනවා", SinhalaTransliterator.transliterate("yanawa"));

    // Verify diacritic safety in suggestions
    java.util.List<String> gamageeSug = SinhalaTransliterator.getSuggestions("gamagee");
    assertFalse(gamageeSug.contains("ගමගේා")); // Invalid diacritic combination must NOT be generated
  }

  @Test
  public void testLeadingAnusvaraPrevention()
  {
    String res = SinhalaTransliterator.parsePhonetic("nsb");
    assertFalse(res.startsWith("\u0D82")); // Anusvara (ං) must never be the first character
  }

  @Test
  public void testNextWordPredictions()
  {
    java.util.List<String> subaPredictions = SinhalaTransliterator.getNextWordPredictions("සුබ");
    assertTrue(subaPredictions.contains("උදෑසනක්"));
    assertTrue(subaPredictions.contains("දවසක්"));

    java.util.List<String> sthuthiPredictions = SinhalaTransliterator.getNextWordPredictions("sthuthi");
    assertTrue(sthuthiPredictions.contains("බොහොම"));

    java.util.List<String> jayaPredictions = SinhalaTransliterator.getNextWordPredictions("ජය");
    assertTrue(jayaPredictions.contains("වේවා"));
  }

  @Test
  public void testExpandedVocabulary()
  {
    assertEquals("කවදාද", SinhalaTransliterator.transliterate("kawadada"));
    assertEquals("කීයද", SinhalaTransliterator.transliterate("kiyada"));
    assertEquals("කරමු", SinhalaTransliterator.transliterate("karamu"));
    assertEquals("දෙන්න", SinhalaTransliterator.transliterate("denna"));
    assertEquals("ගන්න", SinhalaTransliterator.transliterate("ganna"));
    assertEquals("බැරිද", SinhalaTransliterator.transliterate("baerida"));
    assertEquals("අලුත්", SinhalaTransliterator.transliterate("aluth"));
    assertEquals("අවුරුද්දක්", SinhalaTransliterator.transliterate("awuruddak"));
    assertEquals("මිතුරා", SinhalaTransliterator.transliterate("mithura"));
    assertEquals("ෆෝන්", SinhalaTransliterator.transliterate("phone"));
    assertEquals("ඇප්", SinhalaTransliterator.transliterate("app"));
    assertEquals("කීබෝඩ්", SinhalaTransliterator.transliterate("keyboard"));
  }

  public static void main(String[] args)
  {
    SinhalaTransliteratorTest test = new SinhalaTransliteratorTest();
    int passed = 0;
    int failed = 0;

    java.lang.reflect.Method[] methods = SinhalaTransliteratorTest.class.getDeclaredMethods();
    for (java.lang.reflect.Method m : methods)
    {
      if (m.isAnnotationPresent(Test.class))
      {
        try
        {
          m.invoke(test);
          System.out.println("PASS: " + m.getName());
          passed++;
        }
        catch (Throwable t)
        {
          System.err.println("FAIL: " + m.getName() + " -> " + t.getCause());
          t.printStackTrace();
          failed++;
        }
      }
    }

    System.out.println("\nSummary: " + passed + " passed, " + failed + " failed.");
    if (failed > 0)
    {
      System.exit(1);
    }
  }
}

