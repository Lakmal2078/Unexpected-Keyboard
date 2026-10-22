package juloo.keyboard2;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RegressionTest
{
  @Test
  public void protectsExistingWordAndSuggestionBehavior()
  {
    assertEquals("සිංහල", SinhalaTransliterator.transliterate("sinhala"));
    assertEquals("ලංකා", SinhalaTransliterator.transliterate("lanka"));
    assertEquals("ගමගේ", SinhalaTransliterator.transliterate("gamage"));
    assertEquals("නැතුව", SinhalaTransliterator.transliterate("nethuwa"));
    assertEquals("බැරි", SinhalaTransliterator.transliterate("baeri"));

    List<String> first = SinhalaTransliterator.getSuggestions("ka");
    for (int i = 0; i < 50; i++)
      assertEquals(first, SinhalaTransliterator.getSuggestions("ka"));
  }
}
