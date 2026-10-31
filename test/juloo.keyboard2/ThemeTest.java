package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThemeTest
{
  @Test
  public void testCuratedThemesResolveToValidStyles()
  {
    String[] curatedThemes = {
      "system", "dark", "light", "black", "oled_black", "white", "monet",
      "graphite_pro", "gruvbox_dark", "solarized_dark", "dracula", "nord",
      "tokyo_night", "catppuccin_mocha", "catppuccin_latte", "sinhala_sand",
      "lanka_maroon", "high_contrast"
    };
    assertEquals(18, curatedThemes.length);
    for (String theme : curatedThemes)
    {
      int themeId = Config.getThemeId(null, theme);
      assertTrue("Theme " + theme + " should resolve to a valid style ID", themeId != 0);
      assertNotNull("Theme " + theme + " should have a gallery category",
          ThemePicker.categoryOf(theme));
    }
  }

  @Test
  public void testLegacyThemePreferencesMigrateToCuratedCatalog()
  {
    String[] legacyThemes = {
      "monetlight", "monetdark", "gruvbox_light", "rosepine", "everforestlight",
      "sepia_warm", "desert", "emerald_forest", "jungle", "cyberpunk_neon",
      "matrix_green", "sunset_glow", "lanka_blue", "royal_gold", "midnight_purple",
      "midnight_ocean", "arctic_blue", "cobalt", "quiet_teal", "soft_lavender",
      "high_contrast_yellow", "unexpected_keyboard", "rose_quartz", "ocean_breeze",
      "citrus_pop", "copper_night", "berry_noir", "mint_matcha", "glass",
      "aurora_flow", "zen_minimal", "soft_3d", "pixel_retro", "epaper",
      "epaperblack", "altblack", "pine", "forest_mist", "quantum_neon",
      "sinhala_tech", "amoled_cyan", "cyber_purple", "neon_rose",
      "gradientpurplepink", "sinhala_heritage", "aurora", "ocean_teal"
    };
    assertEquals("light", Config.migrateThemePreference("soft_lavender"));
    for (String theme : legacyThemes)
    {
      String migrated = Config.migrateThemePreference(theme);
      assertTrue("Legacy theme " + theme + " migrated to unsupported key " + migrated,
          java.util.Arrays.asList("system", "dark", "light", "black", "oled_black",
              "white", "monet", "graphite_pro", "gruvbox_dark", "solarized_dark",
              "dracula", "nord", "tokyo_night", "catppuccin_mocha",
              "catppuccin_latte", "sinhala_sand", "lanka_maroon", "high_contrast")
              .contains(migrated));
    }
  }

  @Test
  public void testVersionFourPreferencesAreMigrated()
  {
    android.content.SharedPreferences prefs =
        org.mockito.Mockito.mock(android.content.SharedPreferences.class);
    android.content.SharedPreferences.Editor editor =
        org.mockito.Mockito.mock(android.content.SharedPreferences.Editor.class);
    org.mockito.Mockito.when(prefs.getInt("version", 0)).thenReturn(4);
    org.mockito.Mockito.when(prefs.edit()).thenReturn(editor);
    org.mockito.Mockito.when(editor.putInt("version", 6)).thenReturn(editor);
    org.mockito.Mockito.when(prefs.contains("theme")).thenReturn(true);
    org.mockito.Mockito.when(prefs.contains("day_theme")).thenReturn(true);
    org.mockito.Mockito.when(prefs.contains("night_theme")).thenReturn(true);
    org.mockito.Mockito.when(prefs.getString("theme", Config.DEFAULT_THEME))
        .thenReturn("soft_lavender");
    org.mockito.Mockito.when(prefs.getString("day_theme", "light")).thenReturn("sepia_warm");
    org.mockito.Mockito.when(prefs.getString("night_theme", "dark")).thenReturn("rosepine");
    java.util.Set<String> favorites = new java.util.HashSet<>(
        java.util.Arrays.asList("monetlight", "sinhala_heritage"));
    org.mockito.Mockito.when(prefs.getStringSet("favorite_themes", null)).thenReturn(favorites);
    org.mockito.Mockito.when(editor.putString(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString())).thenReturn(editor);
    org.mockito.Mockito.when(editor.putStringSet(org.mockito.ArgumentMatchers.eq("favorite_themes"),
        org.mockito.ArgumentMatchers.anySet())).thenReturn(editor);

    Config.migrate(prefs);

    org.mockito.Mockito.verify(editor).putString("theme", "light");
    org.mockito.Mockito.verify(editor).putString("day_theme", "light");
    org.mockito.Mockito.verify(editor).putString("night_theme", "graphite_pro");
    org.mockito.Mockito.verify(editor).putStringSet(org.mockito.ArgumentMatchers.eq("favorite_themes"),
        org.mockito.ArgumentMatchers.eq(new java.util.HashSet<>(
            java.util.Arrays.asList("monet", "lanka_maroon"))));
    org.mockito.Mockito.verify(editor).apply();
  }

  @Test
  public void testShowNumpadPreferenceNormalization()
  {
    assertEquals("never", Config.migrateShowNumpadPreference("1"));
    assertEquals("never", Config.migrateShowNumpadPreference(null));
    assertEquals("never", Config.migrateShowNumpadPreference("invalid"));
    assertEquals("never", Config.migrateShowNumpadPreference("never"));
    assertEquals("landscape", Config.migrateShowNumpadPreference("landscape"));
    assertEquals("always", Config.migrateShowNumpadPreference("always"));
  }

  @Test
  public void testVersionFiveInvalidNumpadDefaultIsMigrated()
  {
    android.content.SharedPreferences prefs =
        org.mockito.Mockito.mock(android.content.SharedPreferences.class);
    android.content.SharedPreferences.Editor editor =
        org.mockito.Mockito.mock(android.content.SharedPreferences.Editor.class);
    org.mockito.Mockito.when(prefs.getInt("version", 0)).thenReturn(5);
    org.mockito.Mockito.when(prefs.edit()).thenReturn(editor);
    org.mockito.Mockito.when(editor.putInt("version", 6)).thenReturn(editor);
    org.mockito.Mockito.when(prefs.contains("show_numpad")).thenReturn(true);
    org.mockito.Mockito.when(prefs.getString("show_numpad", "never")).thenReturn("1");
    org.mockito.Mockito.when(editor.putString(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString())).thenReturn(editor);

    Config.migrate(prefs);

    org.mockito.Mockito.verify(editor).putString("show_numpad", "never");
    org.mockito.Mockito.verify(editor).putInt("version", 6);
    org.mockito.Mockito.verify(editor).apply();
  }

  @Test
  public void testUnknownLegacyThemeUsesDefault()
  {
    assertEquals(Config.DEFAULT_THEME, Config.migrateThemePreference("unknown_theme"));
    assertEquals(Config.DEFAULT_THEME, Config.migrateThemePreference(null));
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
  public void testCuratedThemesMapToExactlyOneCategory()
  {
    String[] themes = {
      "system", "dark", "light", "black", "oled_black", "white", "monet",
      "graphite_pro", "gruvbox_dark", "solarized_dark", "dracula", "nord",
      "tokyo_night", "catppuccin_mocha", "catppuccin_latte", "sinhala_sand",
      "lanka_maroon", "high_contrast"
    };
    assertEquals(18, themes.length);
    java.util.Map<ThemePicker.ThemeCategory, Integer> counts = new java.util.HashMap<>();
    for (ThemePicker.ThemeCategory category : ThemePicker.ThemeCategory.values())
      counts.put(category, 0);
    for (String theme : themes)
    {
      ThemePicker.ThemeCategory category = ThemePicker.categoryOf(theme);
      assertNotNull("Theme " + theme + " must map to a gallery category", category);
      assertNotEquals(ThemePicker.ThemeCategory.ALL, category);
      assertNotEquals(ThemePicker.ThemeCategory.FAVORITES, category);
      counts.put(category, counts.get(category) + 1);
    }
    assertEquals(2, (int) counts.get(ThemePicker.ThemeCategory.SYSTEM));
    assertEquals(11, (int) counts.get(ThemePicker.ThemeCategory.DARK));
    assertEquals(3, (int) counts.get(ThemePicker.ThemeCategory.LIGHT));
    assertEquals(2, (int) counts.get(ThemePicker.ThemeCategory.VIBRANT));
    assertNull(ThemePicker.categoryOf(null));
    assertNull(ThemePicker.categoryOf("removed_theme"));
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
