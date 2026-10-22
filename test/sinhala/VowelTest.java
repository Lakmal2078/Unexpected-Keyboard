package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VowelTest
{
  @Test
  public void mapsIndependentVowelsAndLongForms()
  {
    assertEquals("අ", SinhalaTransliterator.parsePhonetic("a"));
    assertEquals("ආ", SinhalaTransliterator.parsePhonetic("aa"));
    assertEquals("ඇ", SinhalaTransliterator.parsePhonetic("ae"));
    assertEquals("ඈ", SinhalaTransliterator.parsePhonetic("aae"));
    assertEquals("ඉ", SinhalaTransliterator.parsePhonetic("i"));
    assertEquals("ඊ", SinhalaTransliterator.parsePhonetic("ii"));
    assertEquals("උ", SinhalaTransliterator.parsePhonetic("u"));
    assertEquals("ඌ", SinhalaTransliterator.parsePhonetic("uu"));
    assertEquals("එ", SinhalaTransliterator.parsePhonetic("e"));
    assertEquals("ඒ", SinhalaTransliterator.parsePhonetic("ee"));
    assertEquals("ඔ", SinhalaTransliterator.parsePhonetic("o"));
    assertEquals("ඕ", SinhalaTransliterator.parsePhonetic("oo"));
  }

  @Test
  public void mapsDependentVowelSigns()
  {
    assertEquals("කා", SinhalaTransliterator.parsePhonetic("kaa"));
    assertEquals("කැ", SinhalaTransliterator.parsePhonetic("kae"));
    assertEquals("කෑ", SinhalaTransliterator.parsePhonetic("kaae"));
    assertEquals("කි", SinhalaTransliterator.parsePhonetic("ki"));
    assertEquals("කී", SinhalaTransliterator.parsePhonetic("kii"));
    assertEquals("කු", SinhalaTransliterator.parsePhonetic("ku"));
    assertEquals("කූ", SinhalaTransliterator.parsePhonetic("kuu"));
    assertEquals("කෙ", SinhalaTransliterator.parsePhonetic("ke"));
    assertEquals("කේ", SinhalaTransliterator.parsePhonetic("kee"));
    assertEquals("කො", SinhalaTransliterator.parsePhonetic("ko"));
    assertEquals("කෝ", SinhalaTransliterator.parsePhonetic("koo"));
  }
}
