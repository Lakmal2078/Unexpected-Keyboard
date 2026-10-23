package juloo.keyboard2.suggestions;

import android.annotation.SuppressLint;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public final class SuggestionPolicy
{
  private SuggestionPolicy() {}

  // This inlined integer mask is safe on older Android versions too.
  @SuppressLint("InlinedApi")
  public static boolean is_sensitive_input(int inputType, int imeOptions)
  {
    return (imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        || is_sensitive_input(inputType);
  }

  public static boolean is_sensitive_input(int inputType)
  {
    int inputClass = inputType & InputType.TYPE_MASK_CLASS;
    int variation = inputType & InputType.TYPE_MASK_VARIATION;
    int flags = inputType & InputType.TYPE_MASK_FLAGS;
    // Treat inputs that explicitly disable suggestions as sensitive.
    if ((flags & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0)
      return true;
    if (inputClass == InputType.TYPE_CLASS_TEXT)
    {
      switch (variation)
      {
        case InputType.TYPE_TEXT_VARIATION_PASSWORD:
        case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
        case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
          return true;
        default:
          return false;
      }
    }
    return inputClass == InputType.TYPE_CLASS_NUMBER
      && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
  }

  /** Whether the input is a password field where transliteration should be
      disabled. Unlike [is_sensitive_input], this does NOT include incognito
      mode (IME_FLAG_NO_PERSONALIZED_LEARNING) or no-suggestions flags, which
      should only suppress suggestions/learning but not block transliteration. */
  public static boolean is_password_input(int inputType)
  {
    int inputClass = inputType & InputType.TYPE_MASK_CLASS;
    int variation = inputType & InputType.TYPE_MASK_VARIATION;
    if (inputClass == InputType.TYPE_CLASS_TEXT)
    {
      switch (variation)
      {
        case InputType.TYPE_TEXT_VARIATION_PASSWORD:
        case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
        case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
          return true;
        default:
          return false;
      }
    }
    return inputClass == InputType.TYPE_CLASS_NUMBER
      && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
  }
}
