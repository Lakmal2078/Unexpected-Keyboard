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
}
