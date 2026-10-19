package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

/** Test editor that observes text sent through the active IME InputConnection. */
public class TypingTestEditText extends EditText
{
  public interface OnImeInputListener { void onImeInput(CharSequence text); }
  private OnImeInputListener listener;

  public TypingTestEditText(Context context) { super(context); }
  public TypingTestEditText(Context context, AttributeSet attrs) { super(context, attrs); }
  public TypingTestEditText(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

  public void setOnImeInputListener(OnImeInputListener listener) { this.listener = listener; }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs)
  {
    InputConnection target = super.onCreateInputConnection(outAttrs);
    if (target == null) return null;
    return new InputConnectionWrapper(target, false)
    {
      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        boolean accepted = super.commitText(text, newCursorPosition);
        if (accepted && listener != null && text != null && text.length() > 0) listener.onImeInput(text);
        return accepted;
      }

      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition)
      {
        boolean accepted = super.setComposingText(text, newCursorPosition);
        if (accepted && listener != null && text != null && text.length() > 0) listener.onImeInput(text);
        return accepted;
      }
    };
  }
}
