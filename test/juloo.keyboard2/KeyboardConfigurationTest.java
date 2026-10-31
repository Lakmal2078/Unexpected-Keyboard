package juloo.keyboard2;

import android.content.res.Configuration;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyboardConfigurationTest
{
  @Test
  public void orientationChangeRefreshesKeyboardForExplicitThemes()
  {
    Configuration previous = new Configuration();
    previous.orientation = Configuration.ORIENTATION_PORTRAIT;
    Configuration next = new Configuration(previous);
    next.orientation = Configuration.ORIENTATION_LANDSCAPE;

    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(previous, next, false));
  }

  @Test
  public void windowSizeChangeRefreshesKeyboardForExplicitThemes()
  {
    Configuration previous = new Configuration();
    previous.orientation = Configuration.ORIENTATION_LANDSCAPE;
    previous.screenWidthDp = 900;
    previous.screenHeightDp = 500;
    Configuration next = new Configuration(previous);
    next.screenWidthDp = 620;

    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(previous, next, false));
  }

  @Test
  public void densityAndFontScaleChangesRefreshKeyboardMetrics()
  {
    Configuration previous = new Configuration();
    previous.densityDpi = 320;
    Configuration densityChanged = new Configuration(previous);
    densityChanged.densityDpi = 420;
    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(previous, densityChanged, false));

    Configuration fontChanged = new Configuration(previous);
    fontChanged.fontScale = 1.2f;
    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(previous, fontChanged, false));
  }

  @Test
  public void nightModeChangeRefreshesOnlySystemFollowingThemes()
  {
    Configuration previous = new Configuration();
    previous.uiMode = Configuration.UI_MODE_NIGHT_NO;
    Configuration next = new Configuration(previous);
    next.uiMode = Configuration.UI_MODE_NIGHT_YES;

    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(previous, next, true));
    assertFalse(Keyboard2.shouldRefreshKeyboardConfig(previous, next, false));
  }

  @Test
  public void unchangedConfigurationDoesNotRefreshKeyboard()
  {
    Configuration previous = new Configuration();
    Configuration next = new Configuration(previous);

    assertFalse(Keyboard2.shouldRefreshKeyboardConfig(previous, next, false));
    assertFalse(Keyboard2.shouldRefreshKeyboardConfig(previous, next, true));
  }

  @Test
  public void missingConfigurationRefreshesSafely()
  {
    Configuration current = new Configuration();
    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(null, current, false));
    assertTrue(Keyboard2.shouldRefreshKeyboardConfig(current, null, false));
  }
}
