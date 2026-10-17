package juloo.keyboard2.suggestions;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicReference;
import juloo.keyboard2.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class CandidatesViewDeviceTest
{
  @Test
  public void rendersOnlyAvailableWordCandidates()
  {
    Context context = ApplicationProvider.getApplicationContext();
    AtomicReference<TextView[]> itemViews = new AtomicReference<>();
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      CandidatesView candidatesView = new CandidatesView(context, null);
      addItemView(candidatesView, context, R.id.candidates_middle);
      addItemView(candidatesView, context, R.id.candidates_right);
      addItemView(candidatesView, context, R.id.candidates_left);
      addItemView(candidatesView, context, R.id.candidates_emoji);
      candidatesView.onFinishInflate();

      Suggestions suggestions = new Suggestions(null, null);
      suggestions.suggestions[0] = "අම්මා";
      suggestions.suggestions[1] = "සිංහල";
      suggestions.count = 2;
      candidatesView.set_candidates(suggestions);
      itemViews.set(new TextView[] {
        candidatesView.findViewById(R.id.candidates_middle),
        candidatesView.findViewById(R.id.candidates_right),
        candidatesView.findViewById(R.id.candidates_left),
        candidatesView.findViewById(R.id.candidates_emoji)
      });
    });

    TextView[] views = itemViews.get();
    assertEquals("අම්මා", views[0].getText().toString());
    assertEquals(View.VISIBLE, views[0].getVisibility());
    assertEquals("සිංහල", views[1].getText().toString());
    assertEquals(View.VISIBLE, views[1].getVisibility());
    assertEquals(View.GONE, views[2].getVisibility());
    assertEquals(View.GONE, views[3].getVisibility());
  }

  @Test
  public void hidesCandidatesForPasswordEditors()
  {
    int[] passwordTypes = {
      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
      InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
    };

    for (int inputType : passwordTypes)
    {
      EditorInfo editorInfo = new EditorInfo();
      editorInfo.inputType = inputType;
      assertFalse(CandidatesView.should_show(editorInfo));
    }
  }

  private static void addItemView(CandidatesView parent, Context context, int id)
  {
    TextView view = new TextView(context);
    view.setId(id);
    parent.addView(view);
  }
}
