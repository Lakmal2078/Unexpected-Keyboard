package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThemeTest
{
  @Test
  public void testCuratedThemesResolveToValidStyles()
  {
    String[] curatedThemes = {
      "light",
      "dark",
      "black",
      "oled_black",
      "white",
      "gruvbox_dark",
      "gruvbox_light",
      "solarized_dark",
      "sepia_warm",
      "dracula",
      "nord",
      "tokyo_night",
      "catppuccin_mocha",
      "catppuccin_latte",
      "rosepine",
      "everforestlight",
      "desert",
      "sinhala_sand",
      "emerald_forest",
      "jungle",
      "cyberpunk_neon",
      "matrix_green",
      "sunset_glow",
      "lanka_maroon",
      "royal_gold",
      "midnight_purple",
      "midnight_ocean",
      "arctic_blue",
      "cobalt",
      "graphite_pro",
      "quiet_teal",
      "soft_lavender",
      "high_contrast",
      "high_contrast_yellow",
      "rose_quartz",
      "ocean_breeze",
      "citrus_pop",
      "copper_night",
      "berry_noir",
      "mint_matcha",
      "glass",
      "aurora_flow",
      "zen_minimal",
      "soft_3d",
      "pixel_retro"
    };

    for (String theme : curatedThemes)
    {
      int themeId = Config.getThemeId(null, theme);
      assertTrue("Theme " + theme + " should resolve to a valid style ID", themeId != 0);
    }
  }

  @Test
  public void testDeprecatedAliasesResolveToFallbacks()
  {
    assertEquals(R.style.Black, Config.getThemeId(null, "altblack"));
    assertEquals(R.style.Black, Config.getThemeId(null, "epaperblack"));
    assertEquals(R.style.OledBlack, Config.getThemeId(null, "oled_black"));
    assertEquals(R.style.White, Config.getThemeId(null, "epaper"));
    assertEquals(R.style.EmeraldForest, Config.getThemeId(null, "pine"));
    assertEquals(R.style.EmeraldForest, Config.getThemeId(null, "forest_mist"));
    assertEquals(R.style.CyberpunkNeon, Config.getThemeId(null, "quantum_neon"));
    assertEquals(R.style.CyberpunkNeon, Config.getThemeId(null, "sinhala_tech"));
    assertEquals(R.style.CyberpunkNeon, Config.getThemeId(null, "amoled_cyan"));
    assertEquals(R.style.MidnightPurple, Config.getThemeId(null, "cyber_purple"));
    assertEquals(R.style.SunsetGlow, Config.getThemeId(null, "neon_rose"));
    assertEquals(R.style.SunsetGlow, Config.getThemeId(null, "gradientpurplepink"));
    assertEquals(R.style.LankaMaroon, Config.getThemeId(null, "sinhala_heritage"));
    assertEquals(R.style.MidnightOcean, Config.getThemeId(null, "aurora"));
    assertEquals(R.style.MidnightOcean, Config.getThemeId(null, "ocean_teal"));
  }

  @Test
  public void testSystemAndUnknownFallbacks()
  {
    assertEquals(R.style.Dark, Config.getThemeId(null, "system"));
    assertEquals(R.style.Dark, Config.getThemeId(null, "unknown_theme_key"));
  }

  @Test
  public void testKeyShapeCornerRadii()
  {
    float keyWidth = 100f;
    float themeRadius = 7f;
    assertEquals(themeRadius,
        Theme.computeKeyBorderRadius("theme", keyWidth, themeRadius, false, 0f), 0f);
    assertEquals(0f,
        Theme.computeKeyBorderRadius("square", keyWidth, themeRadius, false, 0f), 0f);
    assertEquals(16f,
        Theme.computeKeyBorderRadius("rounded", keyWidth, themeRadius, false, 0f), 0f);
    assertEquals(28f,
        Theme.computeKeyBorderRadius("squircle", keyWidth, themeRadius, false, 0f), 0f);
    assertEquals(50f,
        Theme.computeKeyBorderRadius("pill", keyWidth, themeRadius, false, 0f), 0f);
    assertEquals(12f,
        Theme.computeKeyBorderRadius("squircle", keyWidth, themeRadius, true, 0.12f), 0f);
  }

  @Test
  public void testCustomColorContrastAndBlending()
  {
    // High contrast text computation
    assertEquals(0xFFFFFFFF, Theme.getContrastColor(0xFF000000)); // Black -> White text
    assertEquals(0xFFFFFFFF, Theme.getContrastColor(0xFF0D1B2A)); // Navy -> White text
    assertEquals(0xFF121212, Theme.getContrastColor(0xFFFFFFFF)); // White -> Dark text
    assertEquals(0xFF121212, Theme.getContrastColor(0xFFFFD600)); // Yellow -> Dark text

    // Harmonious key color computation
    int darkKey = Theme.computeKeyColor(0xFF000000, 0);
    assertTrue("Key color on black should be lighter than black", (darkKey & 0xFF) > 0);

    int lightKey = Theme.computeKeyColor(0xFFFFFFFF, 0);
    assertTrue("Key color on white should be slightly darker than pure white", (lightKey & 0xFF) < 255);
  }
}
