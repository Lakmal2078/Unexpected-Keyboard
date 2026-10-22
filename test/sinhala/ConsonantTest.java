package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ConsonantTest
{
  @Test
  public void mapsCoreAndAspiratedConsonants()
  {
    assertEquals("ක", SinhalaTransliterator.parsePhonetic("ka"));
    assertEquals("ඛ", SinhalaTransliterator.parsePhonetic("kha"));
    assertEquals("ග", SinhalaTransliterator.parsePhonetic("ga"));
    assertEquals("ඝ", SinhalaTransliterator.parsePhonetic("gha"));
    assertEquals("ච", SinhalaTransliterator.parsePhonetic("cha"));
    assertEquals("ඡ", SinhalaTransliterator.parsePhonetic("chha"));
    assertEquals("ජ", SinhalaTransliterator.parsePhonetic("ja"));
    assertEquals("ඣ", SinhalaTransliterator.parsePhonetic("jha"));
    assertEquals("ත", SinhalaTransliterator.parsePhonetic("tha"));
    assertEquals("ද", SinhalaTransliterator.parsePhonetic("da"));
    assertEquals("ධ", SinhalaTransliterator.parsePhonetic("dha"));
    assertEquals("ප", SinhalaTransliterator.parsePhonetic("pa"));
    assertEquals("භ", SinhalaTransliterator.parsePhonetic("bha"));
    assertEquals("න", SinhalaTransliterator.parsePhonetic("na"));
    assertEquals("ව", SinhalaTransliterator.parsePhonetic("wa"));
    assertEquals("ෆ", SinhalaTransliterator.parsePhonetic("fa"));
  }

  @Test
  public void supportsSangyakaConsonants()
  {
    assertEquals("ඟ", SinhalaTransliterator.parsePhonetic("nnga"));
    assertEquals("ඬ", SinhalaTransliterator.parsePhonetic("nnda"));
    assertEquals("ඳ", SinhalaTransliterator.parsePhonetic("nndha"));
    assertEquals("ඹ", SinhalaTransliterator.parsePhonetic("mmba"));
  }
}
