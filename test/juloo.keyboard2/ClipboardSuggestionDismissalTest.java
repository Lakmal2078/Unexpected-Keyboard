package juloo.keyboard2.suggestions;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import juloo.keyboard2.Config;
import juloo.keyboard2.EditorConfig;

public class ClipboardSuggestionDismissalTest
{
  @Test
  public void dismissedClipboardStaysHiddenUntilClipboardContentChanges()
  {
    Config config = new Config();
    config.editor_config = new EditorConfig();
    Suggestions suggestions = new Suggestions(mock(Suggestions.Callback.class), config);
    suggestions.clipboard_full_text = "copied text";

    suggestions.dismiss_clipboard_suggestion();

    assertFalse(suggestions.should_show_clipboard_suggestion("copied text"));
    assertTrue(suggestions.should_show_clipboard_suggestion("new copied text"));
    assertTrue(suggestions.should_show_clipboard_suggestion("copied text"));
  }

  @Test
  public void emptyClipboardDoesNotShowPreview()
  {
    Config config = new Config();
    config.editor_config = new EditorConfig();
    Suggestions suggestions = new Suggestions(mock(Suggestions.Callback.class), config);

    assertFalse(suggestions.should_show_clipboard_suggestion(null));
    assertFalse(suggestions.should_show_clipboard_suggestion(""));
    assertFalse(suggestions.should_show_clipboard_suggestion("  "));
  }
}
