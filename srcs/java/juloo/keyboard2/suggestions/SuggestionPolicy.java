package juloo.keyboard2.suggestions;

import android.annotation.SuppressLint;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public final class SuggestionPolicy
{
  private SuggestionPolicy() {}

  // This inlined integer mask is safe on older Android versions too.
  // This inlined integer mask is safe on older Android versions too.
  @SuppressLint("InlinedApi")
  public static boolean is_sensitive_input(int inputType, int imeOptions)
  {
    // Previously, IME_FLAG_NO_PERSONALIZED_LEARNING was treated as sensitive.
    // We now ignore this flag to allow suggestions in incognito/private mode.
    return is_sensitive_input(inputType);
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
}
