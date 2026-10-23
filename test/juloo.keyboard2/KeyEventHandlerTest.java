package juloo.keyboard2;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KeyEventHandlerTest
{
  private Config config;
  private InputConnection connection;
  private KeyEventHandler handler;
  private Suggestions suggestions;
  private StringBuilder document;

  @Before
  public void setUp()
  {
    config = new Config();
    config.editor_config = new EditorConfig();
    config.editor_config.initial_text_before_cursor = "";
    config.editor_config.should_show_candidates_view = true;
    config.suggestions_enabled = true;
    config.quick_calculator_enabled = true;
    config.smart_snippets_enabled = true;
    config.suggestions_add_space = true;
    connection = mock(InputConnection.class);
    document = new StringBuilder();
    when(connection.commitText(any(CharSequence.class), anyInt())).thenAnswer(call -> {
      document.append((CharSequence)call.getArgument(0));
      return true;
    });
    when(connection.deleteSurroundingText(anyInt(), anyInt())).thenAnswer(call -> {
      int count = call.getArgument(0);
      document.delete(Math.max(0, document.length() - count), document.length());
      return true;
    });
    KeyEventHandler.IReceiver receiver = mock(KeyEventHandler.IReceiver.class);
    when(receiver.getCurrentInputConnection()).thenReturn(connection);
    when(receiver.getHandler()).thenReturn(mock(Handler.class));
    suggestions = new Suggestions(receiver, config);
    handler = new KeyEventHandler(config, receiver, suggestions);
    handler.started();
  }

  private void type(String text)
  {
    for (int i = 0; i < text.length(); i++)
      handler.key_up(KeyValue.makeCharKey(text.charAt(i)), Pointers.Modifiers.EMPTY);
  }

  @Test
  public void candidateTapArmsUndoForThatReplacement()
  {
    type("teh");
    handler.suggestion_entered("the");
    assertEquals("the ", document.toString());
    handler.handle_backspace();
    assertEquals("teh ", document.toString());
  }

  @Test
  public void nextWordTapCannotReusePreviousReplacement()
  {
    type("teh");
    handler.suggestion_entered("the");
    handler.suggestion_entered("world");
    handler.handle_backspace();
    assertEquals("the world ", document.toString());
    verify(connection, never()).deleteSurroundingText(6, 0);
    verify(connection, atLeastOnce()).sendKeyEvent(any(KeyEvent.class));
    assertNull(handler.last_replaced_word);
  }

  @Test
  public void editingRestartDropsReplacementMetadata()
  {
    type("teh");
    handler.suggestion_entered("the");
    handler.started();
    assertNull(handler.last_replaced_word);
    assertNull(handler._last_action);
  }

  @Test
  public void clipboardPaneCommitsLiteralAsciiInSinhalaMode()
  {
    config.sinhala_transliteration = true;
    handler.paste_from_clipboard_pane("amma");
    assertEquals("amma", document.toString());
    assertEquals("amma", handler._typedword.get());
    assertEquals(0, handler._sinhala_buffer.length());
    verify(connection, never()).setComposingText(any(), anyInt());
  }

  @Test
  public void clipboardCandidateUsesFullLiteralText()
  {
    config.sinhala_transliteration = true;
    suggestions.clipboard_full_text = "amma\nhello";
    handler.suggestion_entered("📋 amma hello");
    assertEquals("amma\nhello", document.toString());
    assertNull(handler.last_replaced_word);
  }

  @Test
  public void calculatorReceivesAndReplacesTheWholeExpression()
  {
    type("(2+3)*4");
    assertEquals("= 20", suggestions.suggestions[0]);
    handler.suggestion_entered(suggestions.suggestions[0]);
    assertEquals("20 ", document.toString());
  }

  @Test
  public void spaceAutocompletePreservesNumberedListsAndCitations()
  {
    config.space_bar_auto_complete = true;
    for (String literal : new String[] { "(1)", "(2023)", "(.5)" })
    {
      type(literal);
      handler.key_up(KeyValue.getKeyByName("space"), Pointers.Modifiers.EMPTY);
    }
    assertEquals("(1) (2023) (.5) ", document.toString());
  }

  @Test
  public void functionAndCommaExpressionsReachCalculator()
  {
    type("sqrt(144)");
    assertEquals("= 12", suggestions.suggestions[0]);
    handler.suggestion_entered(suggestions.suggestions[0]);
    type("1,000*2");
    assertEquals("= 2000", suggestions.suggestions[0]);
    handler.suggestion_entered(suggestions.suggestions[0]);
    assertEquals("12 2000 ", document.toString());
  }

  @Test
  public void functionNameIsRestoredToLiteralAtBracketInSinhalaMode()
  {
    config.sinhala_transliteration = true;
    type("sqrt(144)");
    assertEquals("= 12", suggestions.suggestions[0]);
    handler.suggestion_entered(suggestions.suggestions[0]);
    assertEquals("12 ", document.toString());
  }

  @Test
  public void snippetAliasesWorkWhenTypedInSinhalaMode()
  {
    config.sinhala_transliteration = true;
    type("//lk");
    assertEquals("ශ්‍රී ලංකා", suggestions.suggestions[0]);
    handler.suggestion_entered(suggestions.suggestions[0]);
    type("..sinhala");
    assertEquals("සිංහල", suggestions.suggestions[0]);
    handler.suggestion_entered(suggestions.suggestions[0]);
    assertEquals("ශ්‍රී ලංකා සිංහල ", document.toString());
    verify(connection, never()).setComposingText(any(), anyInt());
  }

  @Test
  public void passwordEditorDoesNotTrackOrTransliterateTypedText()
  {
    type("old");
    handler._typedword.delayed_refresh();
    config.editor_config.is_sensitive_input = true;
    config.editor_config.is_password_input = true;
    config.sinhala_transliteration = true;
    handler.started();
    clearInvocations(connection);

    type("private");
    handler._typedword.delayed_refresh_run.run();
    handler.selection_updated(0, 7, 7, -1, -1);
    handler.selection_updated(7, 0, 7, -1, -1);

    assertEquals("oldprivate", document.toString());
    assertEquals("", handler._typedword.get());
    assertEquals(0, suggestions.count);
    assertEquals(0, handler._sinhala_buffer.length());
    assertTrue(handler._typedword.is_selection_not_empty());
    verify(connection, never()).setComposingText(any(), anyInt());
    verify(connection, never()).getTextBeforeCursor(anyInt(), anyInt());
    verify(connection, never()).getSurroundingText(anyInt(), anyInt(), anyInt());
  }

  @Test
  public void incognitoEditorAllowsSinhalaTransliterationWithoutTracking()
  {
    type("old");
    handler._typedword.delayed_refresh();
    config.editor_config.is_sensitive_input = true;
    config.editor_config.is_password_input = false;
    config.sinhala_transliteration = true;
    handler.started();
    clearInvocations(connection);

    type("sinhala");
    handler.handle_space_bar();

    assertEquals("oldසිංහල ", document.toString());
    assertEquals("", handler._typedword.get());
    assertEquals(0, suggestions.count);
    assertEquals(0, handler._sinhala_buffer.length());
  }
}
