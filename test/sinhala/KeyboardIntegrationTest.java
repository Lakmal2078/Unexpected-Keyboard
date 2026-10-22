package juloo.keyboard2;

import android.os.Handler;
import android.view.inputmethod.InputConnection;
import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KeyboardIntegrationTest
{
  private Config config;
  private InputConnection connection;
  private KeyEventHandler handler;

  @Before
  public void setUp()
  {
    config = new Config();
    config.editor_config = new EditorConfig();
    config.editor_config.initial_text_before_cursor = "";
    config.suggestions_enabled = false;
    config.sinhala_transliteration = true;

    connection = mock(InputConnection.class);
    KeyEventHandler.IReceiver receiver = mock(KeyEventHandler.IReceiver.class);
    when(receiver.getCurrentInputConnection()).thenReturn(connection);
    when(receiver.getHandler()).thenReturn(mock(Handler.class));

    handler = new KeyEventHandler(config, receiver, new Suggestions(receiver, config));
    handler.started();
  }

  @Test
  public void keyEventsComposeAndCommitSinhalaThroughInputConnection()
  {
    handler.key_up(KeyValue.makeCharKey('k'), Pointers.Modifiers.EMPTY);
    verify(connection).setComposingText("ක්", 1);

    handler.key_up(KeyValue.makeCharKey('a'), Pointers.Modifiers.EMPTY);
    verify(connection).setComposingText("ක", 1);

    handler.key_up(KeyValue.getKeyByName("space"), Pointers.Modifiers.EMPTY);
    verify(connection).finishComposingText();
    verify(connection).commitText(" ", 1);
  }

  @Test
  public void sensitiveEditorCommitsLiteralInputWithoutComposing()
  {
    config.editor_config.is_sensitive_input = true;
    handler.started();
    handler.key_up(KeyValue.makeCharKey('k'), Pointers.Modifiers.EMPTY);
    handler.key_up(KeyValue.makeCharKey('a'), Pointers.Modifiers.EMPTY);

    verify(connection, never()).setComposingText(any(CharSequence.class), anyInt());
    verify(connection).commitText("k", 1);
    verify(connection).commitText("a", 1);
    assertEquals(0, handler._sinhala_buffer.length());
  }
}
