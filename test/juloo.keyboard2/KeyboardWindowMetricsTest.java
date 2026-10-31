package juloo.keyboard2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeyboardWindowMetricsTest
{
  @Test
  public void usesConstrainedWindowWidthWhenAvailable()
  {
    assertEquals(720, Keyboard2View.resolveAvailableWidth(720, 1440));
  }

  @Test
  public void fallsBackToDisplayWidthForUnspecifiedMeasureSpec()
  {
    assertEquals(1440, Keyboard2View.resolveAvailableWidth(0, 1440));
  }

  @Test
  public void preservesFullWidthWhenWindowMatchesDisplay()
  {
    assertEquals(1440, Keyboard2View.resolveAvailableWidth(1440, 1440));
  }

  @Test
  public void limitsRowsToConstrainedWindowHeight()
  {
    assertEquals(70f, Keyboard2View.resolveMaxRowHeight(300, 20f, 4f), 0.001f);
  }

  @Test
  public void leavesConfiguredHeightUnchangedWhenWindowHeightIsUnspecified()
  {
    assertEquals(Float.POSITIVE_INFINITY,
        Keyboard2View.resolveMaxRowHeight(0, 20f, 4f), 0f);
  }

  @Test
  public void avoidsZeroHeightRowsInVerySmallWindows()
  {
    assertEquals(1f, Keyboard2View.resolveMaxRowHeight(10, 20f, 4f), 0.001f);
  }
}
