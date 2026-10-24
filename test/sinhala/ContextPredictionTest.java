package juloo.keyboard2;

import android.os.Handler;
import android.view.inputmethod.InputConnection;
import java.util.List;
import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ContextPredictionTest
{
  private SinhalaLearner learner;
  private Config config;
  private InputConnection connection;
  private KeyEventHandler handler;
  private Suggestions suggestions;

  @Before
  public void setUp()
  {
    learner = new SinhalaLearner((android.content.Context) null);
    config = new Config();
    config.editor_config = new EditorConfig();
    config.editor_config.initial_text_before_cursor = "";
    config.editor_config.should_show_candidates_view = true;
    config.suggestions_enabled = true;
    config.sinhala_transliteration = true;

    connection = mock(InputConnection.class);
    KeyEventHandler.IReceiver receiver = mock(KeyEventHandler.IReceiver.class);
    when(receiver.getCurrentInputConnection()).thenReturn(connection);
    when(receiver.getHandler()).thenReturn(mock(Handler.class));

    suggestions = new Suggestions(receiver, config);
    handler = new KeyEventHandler(config, receiver, suggestions);
    handler.setSinhalaLearner(learner);
    handler.started();
  }

  @Test
  public void staticBigramsProvideAccuratePredictionsForGreetingsAndPronouns()
  {
    List<String> subhaPreds = SinhalaPredictionEngine.getNextWordPredictions("සුබ");
    assertNotNull(subhaPreds);
    assertTrue("Should contain 'උදෑසනක්'", subhaPreds.contains("උදෑසනක්"));
    assertTrue("Should contain 'දවසක්'", subhaPreds.contains("දවසක්"));

    List<String> mamaPreds = SinhalaPredictionEngine.getNextWordPredictions("මම");
    assertNotNull(mamaPreds);
    assertTrue("Should contain 'යනවා'", mamaPreds.contains("යනවා"));
  }

  @Test
  public void trigramContextResolvesMultiWordPhrases()
  {
    List<String> trigram1 = SinhalaPredictionEngine.getPredictions("මම ගෙදර", null);
    assertNotNull(trigram1);
    assertTrue("Predictions for 'මම ගෙදර' should include 'යනවා'", trigram1.contains("යනවා"));

    List<String> trigram2 = SinhalaPredictionEngine.getPredictions("සුබ අලුත්", null);
    assertNotNull(trigram2);
    assertTrue("Predictions for 'සුබ අලුත්' should include 'අවුරුද්දක් වේවා'", trigram2.contains("අවුරුද්දක් වේවා"));
  }

  @Test
  public void tokenExtractionHandlesPunctuationAndWhitespace()
  {
    String[] tokens = SinhalaPredictionEngine.extractRecentTokens("අපි අද හවස, මොකද?");
    assertEquals(2, tokens.length);
    assertEquals("හවස", tokens[0]);
    assertEquals("මොකද", tokens[1]);
  }

  @Test
  public void dynamicLearnerRecordsAndPrioritizesUserTransitions()
  {
    learner.recordTransition("මම", "ක්‍රිකට්");
    learner.recordTransition("මම", "ක්‍රිකට්");
    learner.recordTransition("මම", "ක්‍රිකට්");

    List<String> learned = learner.getLearnedPredictions("මම");
    assertFalse("Learned list should not be empty", learned.isEmpty());
    assertEquals("ක්‍රිකට්", learned.get(0));

    // Dynamic prediction engine integration
    List<String> preds = SinhalaPredictionEngine.getPredictions("මම", learner);
    assertTrue("Prediction engine should prioritize user-learned bigram", preds.contains("ක්‍රිකට්"));
    assertEquals("ක්‍රිකට්", preds.get(0));
  }

  @Test
  public void learnerFiltersOutNonSinhalaNumbersAndUrls()
  {
    learner.recordTransition("මම", "12345");
    learner.recordTransition("මම", "https://google.com");
    learner.recordTransition("123", "ගෙදර");

    List<String> learned = learner.getLearnedPredictions("මම");
    assertFalse("Should reject numeric next word", learned.contains("12345"));
    assertFalse("Should reject URL next word", learned.contains("https://google.com"));
  }

  @Test
  public void clearingLearnerResetsLearnedTransitions()
  {
    learner.recordTransition("අපි", "පාසලට");
    assertFalse(learner.getLearnedPredictions("අපි").isEmpty());

    learner.clearLearnedData();
    assertTrue(learner.getLearnedPredictions("අපි").isEmpty());
  }

  @Test
  public void committingSinhalaWordPresentsPredictionsInSuggestions()
  {
    when(connection.getTextBeforeCursor(anyInt(), anyInt())).thenReturn("සුබ ");

    handler.suggestion_entered("සුබ");

    assertTrue("Suggestions should mark is_prediction true", suggestions.is_prediction);
    assertTrue("Suggestions count should be > 0", suggestions.count > 0);
    assertNotNull("First suggestion should be present", suggestions.suggestions[0]);
  }

  @Test
  public void sensitiveEditorSuspendsLearning()
  {
    config.editor_config.is_sensitive_input = true;
    handler.started();

    handler.suggestion_entered("රහස්");
    handler.suggestion_entered("වචනය");

    List<String> learned = learner.getLearnedPredictions("රහස්");
    assertTrue("Learning must be suspended in sensitive input fields", learned.isEmpty());
  }
}
