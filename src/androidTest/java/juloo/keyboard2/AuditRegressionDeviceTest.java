package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
