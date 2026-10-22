package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CompoundInputTest
{
  @Test
  public void mapsYanshayaAndRakaranshaya()
  {
    assertEquals("ක්‍රමය", SinhalaTransliterator.transliterate("kramaya"));
    assertEquals("ව්‍යංජන", SinhalaTransliterator.transliterate("vyanjana"));
    assertEquals("ප්‍රකාශන", SinhalaTransliterator.transliterate("prakaashana"));
    assertEquals("විද්‍යාව", SinhalaTransliterator.transliterate("vidhyaava"));
  }

  @Test
  public void keepsPunctuationAndSpacesAroundTranslatedWords()
  {
    assertEquals("අම්මා, කොහොමද?", SinhalaTransliterator.transliterate("amma, kohomada?"));
    assertEquals("සුබ උදෑසනක්!", SinhalaTransliterator.transliterate("suba udaesanak!"));
  }
}
