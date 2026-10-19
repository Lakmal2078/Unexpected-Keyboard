package juloo.keyboard2;

import android.os.Parcelable;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherDashboardTest
{
  @Test
  public void typingTextIsExcludedFromSavedViewState()
  {
    try (ActivityScenario<LauncherActivity> scenario = ActivityScenario.launch(LauncherActivity.class))
    {
      scenario.onActivity(activity -> {
        EditText editor = activity.findViewById(R.id.launcher_tryhere_area);
        editor.setText("synthetic private typing test");
        editor.setSelection(editor.length());
        SparseArray<Parcelable> state = new SparseArray<>();
        activity.findViewById(android.R.id.content).saveHierarchyState(state);
        assertNull("Test text must not enter saved view state", state.get(editor.getId()));
      });
    }
  }

  @Test
  public void rotationRetainsTextInMemoryAndClearRemovesIt()
  {
    try (ActivityScenario<LauncherActivity> scenario = ActivityScenario.launch(LauncherActivity.class))
    {
      scenario.onActivity(activity -> {
        EditText editor = activity.findViewById(R.id.launcher_tryhere_area);
        editor.setText("synthetic rotation test");
      });
      scenario.recreate();
      scenario.onActivity(activity -> {
        EditText editor = activity.findViewById(R.id.launcher_tryhere_area);
        assertEquals("synthetic rotation test", editor.getText().toString());
        activity.clear_typing_test(editor);
      });
      scenario.recreate();
      scenario.onActivity(activity -> {
        EditText editor = activity.findViewById(R.id.launcher_tryhere_area);
        assertEquals("", editor.getText().toString());
      });
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 28)
  public void unhandledHardwareKeyUpdatesDiagnosticView()
  {
    try (ActivityScenario<LauncherActivity> scenario = ActivityScenario.launch(LauncherActivity.class))
    {
      scenario.onActivity(activity -> {
        View editor = activity.findViewById(R.id.launcher_tryhere_area);
        activity.new Tryhere_OnUnhandledKeyEventListener().onUnhandledKeyEvent(
            editor, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F1));
        TextView diagnostic = activity.findViewById(R.id.launcher_tryhere_text);
        assertEquals("F1", diagnostic.getText().toString());
      });
    }
  }
}
