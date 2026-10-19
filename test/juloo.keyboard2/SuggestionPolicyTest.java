package juloo.keyboard2;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
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

  @Test
  public void privateModeAppliesToEveryInputClassAndAction()
  {
    for (int inputType : new int[] { InputType.TYPE_CLASS_TEXT,
        InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_NULL })
    {
      assertTrue(SuggestionPolicy.is_sensitive_input(inputType,
          EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING | EditorInfo.IME_ACTION_SEARCH));
      assertFalse(SuggestionPolicy.is_sensitive_input(inputType, EditorInfo.IME_ACTION_SEARCH));
    }
  }

  private static void assertSensitive(int inputType)
  {
    assertTrue(SuggestionPolicy.is_sensitive_input(inputType));
  }
}
