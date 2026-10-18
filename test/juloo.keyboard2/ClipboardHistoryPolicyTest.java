package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClipboardHistoryPolicyTest
{
  @Test
  public void sensitiveClipIsRejectedOnAndroid13AndNewer()
  {
    assertTrue(ClipboardHistoryService.shouldRejectSensitiveClip(33, true));
    assertTrue(ClipboardHistoryService.shouldRejectSensitiveClip(35, true));
  }

  @Test
  public void ordinaryClipIsNotRejected()
  {
    assertFalse(ClipboardHistoryService.shouldRejectSensitiveClip(33, false));
  }

  @Test
  public void olderAndroidDoesNotAssumeSensitiveMarkerIsAvailable()
  {
    assertFalse(ClipboardHistoryService.shouldRejectSensitiveClip(32, true));
  }

  @Test
  public void rejectionChangesOnlyAtAndroid13Boundary()
  {
    int[] supportedSdkLevels = {21, 28, 31, 32, 33, 34, 35, 36};
    for (int sdkInt : supportedSdkLevels)
    {
      assertEquals("Sensitive clip policy at API " + sdkInt,
          sdkInt >= 33,
          ClipboardHistoryService.shouldRejectSensitiveClip(sdkInt, true));
      assertFalse("Ordinary clips must remain eligible at API " + sdkInt,
          ClipboardHistoryService.shouldRejectSensitiveClip(sdkInt, false));
    }
  }
}
