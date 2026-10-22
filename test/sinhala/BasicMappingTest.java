package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BasicMappingTest
{
  @Test
  public void mapsRequestedBasicSyllables()
  {
    assertEquals("ක", SinhalaTransliterator.transliterate("ka"));
    assertEquals("කා", SinhalaTransliterator.transliterate("kaa"));
    assertEquals("කි", SinhalaTransliterator.transliterate("ki"));
    assertEquals("කු", SinhalaTransliterator.transliterate("ku"));
    assertEquals("කෙ", SinhalaTransliterator.transliterate("ke"));
    assertEquals("කො", SinhalaTransliterator.transliterate("ko"));
  }
}
