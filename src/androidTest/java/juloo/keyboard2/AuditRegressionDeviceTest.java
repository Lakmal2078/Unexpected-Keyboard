package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Build;
import android.view.ContextThemeWrapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AuditRegressionDeviceTest
{
  @Test
  public void settingsAndKeyboardShareTheLatestPreferences()
  {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences keyboardPrefs = DirectBootAwarePreferences.get_shared_preferences(context);
    String key = "sinhala_transliteration";
    boolean hadValue = keyboardPrefs.contains(key);
    boolean original = keyboardPrefs.getBoolean(key, true);
    try
    {
      keyboardPrefs.edit().putBoolean(key, false).commit();
      try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class))
      {
        scenario.onActivity(activity -> {
          SharedPreferences settingsPrefs = activity.getPreferenceManager().getSharedPreferences();
          assertFalse(settingsPrefs.getBoolean(key, true));
          settingsPrefs.edit().putBoolean(key, true).commit();
        });
      }
      assertTrue(keyboardPrefs.getBoolean(key, false));
      if (Build.VERSION.SDK_INT >= 24)
        assertSame(keyboardPrefs, DirectBootAwarePreferences.get_protected_prefs(context));
    }
    finally
    {
      SharedPreferences.Editor editor = keyboardPrefs.edit();
      if (hadValue) editor.putBoolean(key, original); else editor.remove(key);
      editor.commit();
    }
  }

  @Test
  public void resetRestoresKeyboardDefaultsAndPreservesLearnedSinhalaData()
  {
    Context context = ApplicationProvider.getApplicationContext();
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(context);
    SharedPreferences learned = DirectBootAwarePreferences.get_protected_prefs(
        context, "sinhala_learned_bigrams");
    Map<String, ?> previousPrefs = new HashMap<>(prefs.getAll());
    String marker = "reset_test_preservation_marker";
    boolean hadMarker = learned.contains(marker);
    String previousMarker = learned.getString(marker, null);
    try
    {
      assertTrue(prefs.edit()
          .putBoolean("sinhala_transliteration", false)
          .putString("theme", "dracula")
          .putString("key_shape", "pill")
          .putBoolean("need_migration", true)
          .commit());
      assertTrue(learned.edit().putString(marker, "keep").commit());

      assertTrue(SettingsActivity.resetKeyboardSettings(prefs, Build.VERSION.SDK_INT >= 24));

      assertTrue(prefs.getBoolean("sinhala_transliteration", false));
      assertEquals(Config.DEFAULT_THEME, prefs.getString("theme", Config.DEFAULT_THEME));
      assertEquals("theme", prefs.getString("key_shape", "theme"));
      assertEquals("no_number_row", prefs.getString("number_row", "no_number_row"));
      assertEquals(65, prefs.getInt("longpress_interval", 65));
      assertEquals(Config.CONFIG_VERSION, prefs.getInt("version", 0));
      if (Build.VERSION.SDK_INT >= 24)
        assertFalse(prefs.getBoolean("need_migration", true));
      assertEquals("keep", learned.getString(marker, null));
    }
    finally
    {
      restorePreferences(prefs, previousPrefs);
      SharedPreferences.Editor editor = learned.edit();
      if (hadMarker) editor.putString(marker, previousMarker);
      else editor.remove(marker);
      editor.commit();
    }
  }

  @Test
  public void themePickerPaletteTracksPlatformLightAndDarkThemes()
  {
    Context context = ApplicationProvider.getApplicationContext();
    ContextThemeWrapper lightContext =
        new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light);
    ContextThemeWrapper darkContext =
        new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault);

    ThemePicker.PickerPalette light = new ThemePicker.PickerPalette(lightContext);
    ThemePicker.PickerPalette dark = new ThemePicker.PickerPalette(darkContext);

    assertNotEquals("Card surfaces must follow the active platform theme",
        light.cardBackground, dark.cardBackground);
    assertNotEquals("Primary text must follow the active platform theme",
        light.primaryText, dark.primaryText);
    assertEquals(Theme.getContrastColor(light.accent), light.accentText);
    assertEquals(Theme.getContrastColor(dark.accent), dark.accentText);
  }

  @Test
  public void themePickerScreenshotsRenderInLightAndDarkModes() throws Exception
  {
    Context context = ApplicationProvider.getApplicationContext();
    File externalPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    assertNotNull("External app picture directory must be available", externalPictures);
    File screenshotDirectory = new File(externalPictures, "unexpected-keyboard-uiux");
    assertTrue("Screenshot directory must be available",
        screenshotDirectory.isDirectory() || screenshotDirectory.mkdirs());

    try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class))
    {
      captureThemePickerScreenshot(scenario, android.R.style.Theme_DeviceDefault_Light,
          new File(screenshotDirectory, "theme-picker-light.png"));
      captureThemePickerScreenshot(scenario, android.R.style.Theme_DeviceDefault,
          new File(screenshotDirectory, "theme-picker-dark.png"));
    }
  }

  private static void captureThemePickerScreenshot(ActivityScenario<SettingsActivity> scenario,
      int themeResource, File output) throws IOException
  {
    final AlertDialog[] dialog = new AlertDialog[1];
    scenario.onActivity(activity -> {
      ContextThemeWrapper themedContext = new ContextThemeWrapper(activity, themeResource);
      SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(activity);
      dialog[0] = ThemePicker.create(
          new AlertDialog.Builder(themedContext), themedContext.getResources(), prefs);
      dialog[0].show();
    });

    android.app.Instrumentation instrumentation =
        InstrumentationRegistry.getInstrumentation();
    instrumentation.waitForIdleSync();
    Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
    assertNotNull("Theme picker screenshot must be captured", screenshot);
    try (FileOutputStream stream = new FileOutputStream(output))
    {
      assertTrue("Theme picker screenshot must be written",
          screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream));
    }
    finally
    {
      screenshot.recycle();
      scenario.onActivity(activity -> dialog[0].dismiss());
    }
  }

  @Test
  public void SinhalaLearnerPersistsAndClearActionRemovesDiskAndMemoryData() throws Exception
  {
    Context context = ApplicationProvider.getApplicationContext();
    Context storageContext = Build.VERSION.SDK_INT >= 24
        ? context.createDeviceProtectedStorageContext() : context;
    SharedPreferences prefs =
        DirectBootAwarePreferences.get_protected_prefs(context, "sinhala_learned_bigrams");
    File preferencesFile = new File(storageContext.getDataDir(),
        "shared_prefs/sinhala_learned_bigrams.xml");
    String transitionKey = "පරීක්ෂණය|සාර්ථකයි";
    Map<String, ?> previousValues = new HashMap<>(prefs.getAll());
    try
    {
      assertTrue("Learning store must be cleared before this test", prefs.edit().clear().commit());

      SinhalaLearner learner = new SinhalaLearner(context);
      learner.recordTransition("පරීක්ෂණය", "සාර්ථකයි");

      // commit() waits for the earlier apply() write to finish before reading the file.
      assertTrue("Learning preferences must flush successfully", prefs.edit().commit());
      assertTrue("The transition must be present in the on-disk preferences XML",
          preferencesFile.isFile() && readFile(preferencesFile).contains(transitionKey));

      SinhalaLearner afterServiceRestart = new SinhalaLearner(context);
      assertTrue("A new learner must reload persisted context predictions",
          afterServiceRestart.getLearnedPredictions("පරීක්ෂණය").contains("සාර්ථකයි"));

      assertTrue("Clear must report a successful durable write",
          SinhalaLearner.clearAllLearnedData(context));
      assertTrue("All live learner caches must be cleared",
          afterServiceRestart.getLearnedPredictions("පරීක්ෂණය").isEmpty());
      assertTrue("The cleared preference file must no longer contain the transition",
          !preferencesFile.exists() || !readFile(preferencesFile).contains(transitionKey));
    }
    finally
    {
      restorePreferences(prefs, previousValues);
    }
  }

  private static String readFile(File file) throws IOException
  {
    try (FileInputStream input = new FileInputStream(file);
         ByteArrayOutputStream output = new ByteArrayOutputStream())
    {
      byte[] buffer = new byte[1024];
      int count;
      while ((count = input.read(buffer)) != -1)
        output.write(buffer, 0, count);
      return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  @SuppressWarnings("unchecked")
  private static void restorePreferences(SharedPreferences prefs, Map<String, ?> values)
  {
    SharedPreferences.Editor editor = prefs.edit().clear();
    for (Map.Entry<String, ?> entry : values.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
      else if (value instanceof Float) editor.putFloat(key, (Float)value);
      else if (value instanceof Integer) editor.putInt(key, (Integer)value);
      else if (value instanceof Long) editor.putLong(key, (Long)value);
      else if (value instanceof String) editor.putString(key, (String)value);
      else if (value instanceof Set) editor.putStringSet(key, (Set<String>)value);
    }
    editor.commit();
  }

  @Test
  public void cachedClipboardPaneRegistersAfterEveryAttach()
  {
    Context context = ApplicationProvider.getApplicationContext();
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      ClipboardHistoryService previous = ClipboardHistoryService._service;
      ClipboardHistoryService service = new ClipboardHistoryService(context);
      ClipboardHistoryService._service = service;
      try
      {
        ClipboardHistoryView view = new ClipboardHistoryView(context, null);
        view.onAttachedToWindow();
        assertSame(view, service._listener);
        view.onDetachedFromWindow();
        assertNull(service._listener);
        view.onAttachedToWindow();
        assertSame(view, service._listener);
        service._history.add(new ClipboardHistoryService.HistoryEntry("expired synthetic entry", 0));
        service._expire_history.run();
        assertEquals(0, view._adapter.getCount());
        view.onDetachedFromWindow();
      }
      finally
      {
        ClipboardHistoryService.on_destroy();
        ClipboardHistoryService._service = previous;
      }
    });
  }

  @Test
  public void bundledEnglishAndSinhalaDictionariesPassParserValidation()
  {
    Context context = ApplicationProvider.getApplicationContext();
    Config config = new Config();
    Dictionaries dictionaries = Dictionaries.instance(context);

    dictionaries.set_current_dictionary(config, "en_US");
    assertNotNull("Bundled English dictionary must pass Cdict validation",
        config.current_dictionary);

    dictionaries.set_current_dictionary(config, "si");
    assertNotNull("Bundled Sinhala dictionary must pass Cdict validation",
        config.current_dictionary);
  }

  @Test
  public void calculatorPunctuationCannotBeRemovedByDictionarySuggestions()
  {
    Context context = ApplicationProvider.getApplicationContext();
    Config config = new Config();
    config.editor_config = new EditorConfig();
    config.editor_config.should_show_candidates_view = true;
    config.suggestions_enabled = true;
    config.sinhala_transliteration = false;
    config.quick_calculator_enabled = true;
    Dictionaries.instance(context).set_current_dictionary(config, "en_US");
    assertNotNull(config.current_dictionary);
    Suggestions suggestions = new Suggestions(ignored -> {}, config);
    suggestions.started();
    suggestions.currently_typed_word("hello");
    assertTrue(suggestions.count > 0);
    for (String prose : new String[] { "word,", "(hello", "hello)" })
    {
      suggestions.currently_typed_word(prose);
      assertEquals(prose, 0, suggestions.count);
    }
    suggestions.currently_typed_word("(2+3)*4");
    assertEquals("= 20", suggestions.suggestions[0]);
  }

  @Test
  public void stoppedLauncherCancelsAnimationLoop()
  {
    try (ActivityScenario<LauncherActivity> scenario = ActivityScenario.launch(LauncherActivity.class))
    {
      scenario.moveToState(Lifecycle.State.CREATED);
      scenario.onActivity(activity -> {
        assertFalse(activity._handler.hasMessages(0));
        assertTrue(activity._animations.isEmpty());
      });
      scenario.moveToState(Lifecycle.State.STARTED);
      scenario.onActivity(activity -> assertTrue(activity._handler.hasMessages(0)));
    }
  }
}
