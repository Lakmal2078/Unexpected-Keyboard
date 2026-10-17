package juloo.keyboard2;

import android.text.InputType;
import juloo.keyboard2.suggestions.SuggestionPolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuggestionPolicyTest
{
  @Test
  public void filtersEveryPasswordInputVariation()
  {
    assertSensitive(InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    assertSensitive(InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    assertSensitive(InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
    assertSensitive(InputType.TYPE_CLASS_NUMBER
        | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
  }

  @Test
  public void allowsOrdinaryTextAndNumberInputs()
  {
    assertFalse(SuggestionPolicy.is_sensitive_input(InputType.TYPE_CLASS_TEXT));
    assertFalse(SuggestionPolicy.is_sensitive_input(InputType.TYPE_CLASS_NUMBER));
    assertFalse(SuggestionPolicy.is_sensitive_input(InputType.TYPE_CLASS_PHONE));
  }

  private static void assertSensitive(int inputType)
  {
    assertTrue(SuggestionPolicy.is_sensitive_input(inputType));
  }
}
