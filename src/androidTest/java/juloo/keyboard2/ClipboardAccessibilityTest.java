package juloo.keyboard2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ClipboardAccessibilityTest
{
  @Test
  public void historyActionsHaveSpokenDescriptions()
  {
    Context context = ApplicationProvider.getApplicationContext();
    View entry = LayoutInflater.from(context)
        .inflate(R.layout.clipboard_history_entry, null, false);

    assertDescription(context, entry, R.id.clipboard_entry_paste,
        R.string.clipboard_action_paste);
    assertDescription(context, entry, R.id.clipboard_entry_addpin,
        R.string.clipboard_action_pin);
  }

  @Test
  public void pinnedActionsHaveSpokenDescriptions()
  {
    Context context = ApplicationProvider.getApplicationContext();
    View entry = LayoutInflater.from(context)
        .inflate(R.layout.clipboard_pin_entry, null, false);

    assertDescription(context, entry, R.id.clipboard_pin_paste,
        R.string.clipboard_action_paste);
    assertDescription(context, entry, R.id.clipboard_pin_remove,
        R.string.clipboard_action_remove_pin);
  }

  private static void assertDescription(Context context, View parent,
      int viewId, int descriptionResource)
  {
    View action = parent.findViewById(viewId);
    assertNotNull(action);
    assertEquals(context.getString(descriptionResource),
        action.getContentDescription());
  }
}
