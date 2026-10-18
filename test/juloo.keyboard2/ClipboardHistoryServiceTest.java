package juloo.keyboard2;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ClipboardHistoryServiceTest
{
  private ClipboardManager clipboard;
  private ClipboardHistoryService service;
  private ClipboardManager.OnPrimaryClipChangedListener systemListener;
  private ClipboardHistoryService.OnClipboardHistoryChange historyListener;
  private ClipboardHistoryService.ClipboardPasteCallback pasteCallback;
  private Config previousConfig;
  private ClipboardHistoryService previousService;
  private ClipboardHistoryService.ClipboardPasteCallback previousPasteCallback;

  @Before
  public void setUp() throws Exception
  {
    previousConfig = Config.globalConfig();
    previousService = ClipboardHistoryService._service;
    previousPasteCallback = ClipboardHistoryService._paste_callback;
    ClipboardHistoryService._service = null;
    ClipboardHistoryService._paste_callback = null;
    Config config = new Config();
    config.clipboard_history_enabled = true;
    config.clipboard_history_duration = -1;
    setGlobalConfig(config);

    clipboard = mock(ClipboardManager.class);
    Context context = mock(Context.class);
    when(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboard);
    service = new ClipboardHistoryService(context);
    ClipboardHistoryService._service = service;
    pasteCallback = mock(ClipboardHistoryService.ClipboardPasteCallback.class);
    ClipboardHistoryService._paste_callback = pasteCallback;
    historyListener = mock(ClipboardHistoryService.OnClipboardHistoryChange.class);
    service.set_on_clipboard_history_change(historyListener);

    ArgumentCaptor<ClipboardManager.OnPrimaryClipChangedListener> listener =
        ArgumentCaptor.forClass(ClipboardManager.OnPrimaryClipChangedListener.class);
    verify(clipboard).addPrimaryClipChangedListener(listener.capture());
    systemListener = listener.getValue();
  }

  @After
  public void tearDown() throws Exception
  {
    try
    {
      ClipboardHistoryService.on_destroy();
    }
    finally
    {
      ClipboardHistoryService._service = previousService;
      ClipboardHistoryService._paste_callback = previousPasteCallback;
      setGlobalConfig(previousConfig);
    }
  }

  @Test
  public void destroyPurgesHistoryEvenForExistingServiceReferences()
  {
    service.add_clip("synthetic first entry");
    service.add_clip("synthetic second entry");
    List<ClipboardHistoryService.HistoryEntry> retainedHistory = service._history;
    assertEquals(2, retainedHistory.size());

    ClipboardHistoryService.on_destroy();

    assertTrue(retainedHistory.isEmpty());
    assertTrue(service.clear_expired_and_get_history().isEmpty());
    assertNull(ClipboardHistoryService._service);
    assertNull(ClipboardHistoryService.get_current_clip());
  }

  @Test
  public void destroyDetachesHistoryListenerBeforePurging()
  {
    service.add_clip("synthetic entry");
    clearInvocations(historyListener);

    ClipboardHistoryService.on_destroy();

    assertNull(service._listener);
    verifyNoInteractions(historyListener);
  }

  @Test
  public void destroyUnregistersTheExactSystemListenerWithoutChangingClipboard()
  {
    assertNotNull(systemListener);

    ClipboardHistoryService.on_destroy();

    verify(clipboard).removePrimaryClipChangedListener(same(systemListener));
    // Registration was already verified in setUp. Teardown must not erase the
    // user's system clipboard or read it back into the history.
    verifyNoMoreInteractions(clipboard);
  }

  @Test
  public void destroyDisconnectsPasteCallback()
  {
    ClipboardHistoryService.paste("synthetic before teardown");
    verify(pasteCallback).paste_from_clipboard_pane("synthetic before teardown");
    clearInvocations(pasteCallback);

    ClipboardHistoryService.on_destroy();
    ClipboardHistoryService.paste("synthetic after teardown");

    assertNull(ClipboardHistoryService._paste_callback);
    verifyNoInteractions(pasteCallback);
  }

  @Test
  public void repeatedDestroyUnregistersOnlyOnce()
  {
    ClipboardHistoryService.on_destroy();
    ClipboardHistoryService.on_destroy();

    verify(clipboard, times(1)).removePrimaryClipChangedListener(same(systemListener));
    assertNull(ClipboardHistoryService._service);
    assertNull(ClipboardHistoryService._paste_callback);
  }

  @Test
  public void destroyWithoutServiceStillClearsPasteCallback()
  {
    ClipboardHistoryService.on_destroy();
    clearInvocations(clipboard);
    ClipboardHistoryService._paste_callback = pasteCallback;

    ClipboardHistoryService.on_destroy();
    ClipboardHistoryService.paste("synthetic entry");

    assertNull(ClipboardHistoryService._paste_callback);
    verifyNoInteractions(clipboard, pasteCallback);
  }

  @Test
  public void registeredSystemListenerImportsClipWithoutDescription()
  {
    ClipData clip = textClip();
    when(clipboard.getPrimaryClip()).thenReturn(clip);

    systemListener.onPrimaryClipChanged();

    assertEquals(1, service.clear_expired_and_get_history().size());
    verify(historyListener).on_clipboard_history_change();
  }

  @Test
  public void clipDescriptionWithoutExtrasIsAccepted()
  {
    ClipData clip = textClip();
    ClipDescription description = mock(ClipDescription.class);
    when(clip.getDescription()).thenReturn(description);
    when(clipboard.getPrimaryClip()).thenReturn(clip);

    service.add_current_clip();

    assertEquals(1, service.clear_expired_and_get_history().size());
    verify(description).getExtras();
  }

  @Test
  public void absentClipboardDoesNotNotifyHistoryListener()
  {
    service.add_current_clip();

    assertTrue(service.clear_expired_and_get_history().isEmpty());
    verifyNoInteractions(historyListener);
  }

  @Test
  public void disconnectedClipboardDoesNotDiscardExistingHistory()
  {
    service.add_clip("synthetic entry");
    clearInvocations(historyListener);
    when(clipboard.getPrimaryClip()).thenThrow(new SecurityException("Disconnected"));

    service.add_current_clip();

    assertEquals(1, service.clear_expired_and_get_history().size());
    verifyNoInteractions(historyListener);
  }

  private static ClipData textClip()
  {
    ClipData clip = mock(ClipData.class);
    ClipData.Item item = mock(ClipData.Item.class);
    when(item.getText()).thenReturn("synthetic clipboard entry");
    when(clip.getItemCount()).thenReturn(1);
    when(clip.getItemAt(0)).thenReturn(item);
    return clip;
  }

  private static void setGlobalConfig(Config config) throws Exception
  {
    Field field = Config.class.getDeclaredField("_globalConfig");
    field.setAccessible(true);
    field.set(null, config);
  }
}
