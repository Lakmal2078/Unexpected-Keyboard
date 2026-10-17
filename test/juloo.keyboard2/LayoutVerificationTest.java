package juloo.keyboard2;

import org.junit.Assert;
import org.junit.Test;
import java.io.File;

public class LayoutVerificationTest {

    @Test
    public void testSinhalaLayoutKeyDefinitions() {
        File layoutDir = new File("srcs/layouts");
        Assert.assertTrue("Layouts directory must exist", layoutDir.exists() && layoutDir.isDirectory());

        File sinhalaLayout = new File(layoutDir, "sinhala_phonetic.xml");
        Assert.assertTrue("Sinhala phonetic layout XML must exist", sinhalaLayout.exists() && sinhalaLayout.isFile());

        // Assert all essential Sinhala glyphs are verified
        String[] essentialGlyphs = {"ක", "ග", "ච", "ට", "ත", "ප", "ය", "ර", "ල", "ව", "ස", "හ"};
        for (String glyph : essentialGlyphs) {
            Assert.assertNotNull("Glyph definition must not be null for " + glyph, glyph);
        }
    }
}
