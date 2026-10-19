package juloo.keyboard2;

import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SuggestionsTest
{
  private Config config;
  private Suggestions suggestions;
  private Suggestions.Callback callback;

  @Before
  public void setUp()
  {
    config = new Config();
    config.editor_config = new EditorConfig();
    config.editor_config.should_show_candidates_view = true;
    config.suggestions_enabled = true;
    config.quick_calculator_enabled = true;
    callback = mock(Suggestions.Callback.class);
    suggestions = new Suggestions(callback, config);
  }

  @Test
  public void emptyWordPublishesClearedCandidatesWithoutClipboardPreview()
  {
    suggestions.started();
    suggestions.currently_typed_word("2+2");
    assertEquals("= 4", suggestions.suggestions[0]);
    clearInvocations(callback);
    suggestions.currently_typed_word("");
    assertEquals(0, suggestions.count);
    assertNull(suggestions.suggestions[0]);
    verify(callback).set_suggestions(suggestions);
  }

  @Test
  public void startingWithoutClipboardStillPublishesEmptyState()
  {
    config.clipboard_suggestion_enabled = false;
    suggestions.started();
    verify(callback).set_suggestions(suggestions);
  }

  @Test
  public void ambiguousSinhalaPrefixKeepsLiteralSpellingVisible()
  {
    config.sinhala_transliteration = true;
    suggestions.started();
    suggestions.currently_typed_word("la");
    assertTrue(Arrays.asList(suggestions.suggestions).contains("la"));
    assertNotEquals("la", suggestions.suggestions[0]);
    assertEquals(3, suggestions.count);
  }

  @Test
  public void sensitiveEditorDoesNotGenerateProductivityCandidates()
  {
    config.editor_config.is_sensitive_input = true;
    suggestions.started();
    suggestions.currently_typed_word("2+2");
    assertEquals(0, suggestions.count);
  }
}
