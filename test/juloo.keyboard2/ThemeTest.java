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
    assertEquals(R.style.Light, Config.getThemeId(null, "system"));
    assertEquals(R.style.Light, Config.getThemeId(null, "unknown_theme_key"));
  }

  @Test
  public void testDayNightThemePairing()
  {
    android.content.SharedPreferences prefs = org.mockito.Mockito.mock(android.content.SharedPreferences.class);
    org.mockito.Mockito.when(prefs.getString("day_theme", "light")).thenReturn("sepia_warm");
    org.mockito.Mockito.when(prefs.getString("night_theme", "dark")).thenReturn("oled_black");

    // With null res (uiMode 0 -> light mode), system theme resolves to day_theme ("sepia_warm")
    assertEquals(R.style.SepiaWarm, Config.getThemeId(null, "system", prefs));

    // With explicit night configuration
    android.content.res.Resources nightRes = org.mockito.Mockito.mock(android.content.res.Resources.class);
    android.content.res.Configuration nightConfig = new android.content.res.Configuration();
    nightConfig.uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES;
    org.mockito.Mockito.when(nightRes.getConfiguration()).thenReturn(nightConfig);

    assertEquals(R.style.OledBlack, Config.getThemeId(nightRes, "system", prefs));

    // Circular prevention: if pairTheme is "system", fallback gracefully to Dark/Light
    org.mockito.Mockito.when(prefs.getString("night_theme", "dark")).thenReturn("system");
    assertEquals(R.style.Dark, Config.getThemeId(nightRes, "system", prefs));
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

  @Test
  public void testAll51ThemesMapToExactlyOneCategory()
  {
    String[] all51Themes = {
      "system", "dark", "light", "black", "oled_black", "white",
      "monet", "monetlight", "monetdark", "gruvbox_dark", "gruvbox_light",
      "solarized_dark", "dracula", "nord", "tokyo_night", "catppuccin_mocha",
      "catppuccin_latte", "rosepine", "everforestlight", "sepia_warm",
      "desert", "sinhala_sand", "emerald_forest", "jungle", "cyberpunk_neon",
      "matrix_green", "sunset_glow", "lanka_maroon", "royal_gold",
      "midnight_purple", "midnight_ocean", "arctic_blue", "cobalt",
      "graphite_pro", "quiet_teal", "soft_lavender", "high_contrast",
      "high_contrast_yellow", "lanka_blue", "unexpected_keyboard",
      "rose_quartz", "ocean_breeze", "citrus_pop", "copper_night",
      "berry_noir", "mint_matcha", "glass", "aurora_flow", "zen_minimal",
      "soft_3d", "pixel_retro"
    };

    assertEquals(51, all51Themes.length);

    java.util.Map<ThemePicker.ThemeCategory, Integer> categoryCounts = new java.util.HashMap<>();
    for (ThemePicker.ThemeCategory cat : ThemePicker.ThemeCategory.values())
    {
      categoryCounts.put(cat, 0);
    }

    for (String themeKey : all51Themes)
    {
      ThemePicker.ThemeCategory category = ThemePicker.categoryOf(themeKey);
      assertNotNull("Theme key '" + themeKey + "' must map to a category", category);
      assertNotEquals("Theme key '" + themeKey + "' should not map to ALL category",
          ThemePicker.ThemeCategory.ALL, category);
      assertNotEquals("Theme key '" + themeKey + "' should not map to FAVORITES category",
          ThemePicker.ThemeCategory.FAVORITES, category);
      categoryCounts.put(category, categoryCounts.get(category) + 1);
    }

    assertEquals(4, (int) categoryCounts.get(ThemePicker.ThemeCategory.SYSTEM));
    assertEquals(24, (int) categoryCounts.get(ThemePicker.ThemeCategory.DARK));
    assertEquals(15, (int) categoryCounts.get(ThemePicker.ThemeCategory.LIGHT));
    assertEquals(8, (int) categoryCounts.get(ThemePicker.ThemeCategory.VIBRANT));
    assertEquals(0, (int) categoryCounts.get(ThemePicker.ThemeCategory.FAVORITES));

    assertNull(ThemePicker.categoryOf(null));
    assertNull(ThemePicker.categoryOf("unknown_custom_theme"));
  }

  @Test
  public void testNightModeResolution()
  {
    assertTrue(Config.isNightMode(android.content.res.Configuration.UI_MODE_NIGHT_YES));
    assertFalse(Config.isNightMode(android.content.res.Configuration.UI_MODE_NIGHT_NO));
    assertFalse(Config.isNightMode(android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED));
    assertFalse(Config.isNightMode(0));
  }
}
