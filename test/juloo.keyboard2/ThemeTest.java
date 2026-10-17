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
      "soft_lavender",
      "high_contrast",
      "high_contrast_yellow"
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
    assertEquals(R.style.Black, Config.getThemeId(null, "oled_black"));
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
}
