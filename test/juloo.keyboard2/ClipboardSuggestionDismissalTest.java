package juloo.keyboard2.suggestions;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;


public class ClipboardSuggestionDismissalTest
{
  @Test
  public void dismissedClipboardStaysHiddenUntilClipboardContentChanges()
  {
    Suggestions suggestions = new Suggestions(mock(Suggestions.Callback.class), null);
    suggestions.clipboard_full_text = "copied text";

    suggestions.dismiss_clipboard_suggestion();

    assertFalse(suggestions.should_show_clipboard_suggestion("copied text"));
    assertTrue(suggestions.should_show_clipboard_suggestion("new copied text"));
    assertTrue(suggestions.should_show_clipboard_suggestion("copied text"));
  }

  @Test
  public void emptyClipboardDoesNotShowPreview()
  {
    Suggestions suggestions = new Suggestions(mock(Suggestions.Callback.class), null);

    assertFalse(suggestions.should_show_clipboard_suggestion(null));
    assertFalse(suggestions.should_show_clipboard_suggestion(""));
    assertFalse(suggestions.should_show_clipboard_suggestion("  "));
  }
}
