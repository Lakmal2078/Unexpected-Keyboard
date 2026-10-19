package juloo.keyboard2;

import android.content.Intent;
import android.content.res.Resources;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import juloo.keyboard2.suggestions.CandidatesView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockMakers;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InputPrivacyTest
{
  private Config config;
  private Config previousConfig;
  private Keyboard2 keyboard;

  @Before
  public void setUp() throws Exception
  {
    previousConfig = Config.globalConfig();
    config = new Config();
    config.editor_config = new EditorConfig();
    config.suggestions_enabled = true;
    config.sinhala_transliteration = true;
    config.quick_calculator_enabled = true;
    config.smart_snippets_enabled = true;
    config.clipboard_suggestion_enabled = true;
    setField(Config.class, null, "_globalConfig", config);
    keyboard = mock(Keyboard2.class, CALLS_REAL_METHODS);
    setField(Keyboard2.class, keyboard, "_config", config);
  }

  @After
  public void tearDown() throws Exception
  {
    setField(Config.class, null, "_globalConfig", previousConfig);
  }

  @Test
  public void hostOptOutHidesCandidatesWithEveryFeatureEnabled() throws Exception
  {
    for (int flags : new int[] { InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT })
    {
      refreshEditor(InputType.TYPE_CLASS_TEXT | flags, 0);
      assertFalse(config.editor_config.should_show_candidates_view);
      assertFalse((Boolean)invoke("should_show_candidates"));
    }
    refreshEditor(InputType.TYPE_CLASS_TEXT, 0);
    assertTrue(config.editor_config.should_show_candidates_view);
    assertTrue((Boolean)invoke("should_show_candidates"));
  }

  @Test
  public void translateNeverReadsOrExportsPasswordSelections() throws Exception
  {
    for (int inputType : new int[] {
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD })
    {
      refreshEditor(inputType, 0);
      assertTrue(config.editor_config.is_sensitive_input);
      invoke("launch_translate");
    }
    verify(keyboard, never()).getCurrentInputConnection();
    verify(keyboard, never()).getPackageManager();
    verify(keyboard, never()).startActivity(any(Intent.class));
  }

  @Test
  public void privateEditorBlocksTranslateAndRestoresToolbarOnOrdinaryInput() throws Exception
  {
    ViewGroup toolbar = mock(ViewGroup.class);
    View translate = mock(View.class);
    when(toolbar.findViewById(R.id.toolbar_btn_translate)).thenReturn(translate);
    setField(Keyboard2.class, keyboard, "_quick_toolbar", toolbar);
    setField(Keyboard2.class, keyboard, "_candidates_view", mock(CandidatesView.class));

    refreshEditor(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
    assertTrue(config.editor_config.is_sensitive_input);
    assertFalse(config.editor_config.should_show_candidates_view);
    invoke("launch_translate");
    invoke("update_top_bar_visibility");
    verify(translate).setEnabled(false);
    verify(keyboard, never()).getCurrentInputConnection();
    verify(keyboard, never()).startActivity(any(Intent.class));

    refreshEditor(InputType.TYPE_CLASS_TEXT, 0);
    assertFalse(config.editor_config.is_sensitive_input);
    invoke("update_top_bar_visibility");
    verify(translate).setEnabled(true);
  }

  @Test
  public void translationFailureLogsNoSelectionOrExceptionMessage() throws Exception
  {
    doThrow(new SecurityException("synthetic private selection"))
      .when(keyboard).getCurrentInputConnection();
    try (MockedStatic<Log> logs = mockStatic(Log.class, withSettings().mockMaker(MockMakers.INLINE)))
    {
      invoke("launch_translate");
      logs.verify(() -> Log.w(Logs.TAG, "Unable to launch translation: SecurityException"));
      logs.verifyNoMoreInteractions();
    }
  }

  private void refreshEditor(int inputType, int options)
  {
    EditorInfo info = mock(EditorInfo.class);
    info.inputType = inputType;
    info.imeOptions = options;
    info.packageName = "synthetic.editor";
    config.editor_config.refresh(info, mock(Resources.class));
  }

  private Object invoke(String name) throws Exception
  {
    Method method = Keyboard2.class.getDeclaredMethod(name);
    method.setAccessible(true);
    return method.invoke(keyboard);
  }

  private static void setField(Class<?> type, Object target, String name, Object value)
      throws Exception
  {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
