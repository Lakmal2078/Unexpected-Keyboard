package juloo.keyboard2;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Handler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages in-memory clipboard history.
 *
 * Security & Privacy Guarantees:
 * - Ephemeral / In-Memory only: History entries are stored only in volatile RAM (List<HistoryEntry>)
 *   and are NEVER written to disk, database, or SharedPreferences.
 * - Explicit User Opt-In: Clipboard history is disabled by default (clipboard_history_enabled = false).
 * - Sensitive Data Protection: On Android 13+ (API 33+), clips marked with EXTRA_IS_SENSITIVE
 *   (passwords, OTPs, auth tokens) are discarded and never added to history.
 * - Auto-Expiry: Entries expire automatically after the configured retention window (TTL).
 * - Immediate Purge: Toggling the history setting off immediately clears all in-memory entries.
 * - Zero Leakage: No clipboard data is ever sent across the network or output to log streams.
 */
public final class ClipboardHistoryService
{
  /** Start the service on startup and start listening to clipboard changes. */
  public static void on_startup(Context ctx, ClipboardPasteCallback cb)
  {
    get_service(ctx);
    _paste_callback = cb;
  }

  /** Start the service if it hasn't been started before. Returns [null] if the
      feature is unsupported. */
  public static ClipboardHistoryService get_service(Context ctx)
  {
    if (VERSION.SDK_INT <= 11)
      return null;
    if (_service == null)
      _service = new ClipboardHistoryService(ctx);
    return _service;
  }

  public static void set_history_enabled(boolean e)
  {
    Config.globalConfig().set_clipboard_history_enabled(e);
    if (_service == null)
      return;
    if (e)
      _service.add_current_clip();
    else
      _service.clear_history();
  }

  /** Send the given string to the editor. */
  public static void paste(String clip)
  {
    if (_paste_callback != null)
      _paste_callback.paste_from_clipboard_pane(clip);
  }

  public static synchronized void on_destroy()
  {
    _paste_callback = null;
    if (_service != null)
    {
      _service._listener = null;
      _service.clear_history();
      _service._cm.removePrimaryClipChangedListener(_service._system_listener);
      _service = null;
    }
  }

  /** The maximum size limits the amount of user data stored in memory but also
      gives a sense to the user that the history is not persisted and can be
      forgotten as soon as the app stops. */
  public static final int MAX_HISTORY_SIZE = 6;

  static ClipboardHistoryService _service = null;
  static ClipboardPasteCallback _paste_callback = null;

  ClipboardManager _cm;
  List<HistoryEntry> _history;
  OnClipboardHistoryChange _listener = null;
  private final ClipboardManager.OnPrimaryClipChangedListener _system_listener;
  private final Handler _handler;
  final Runnable _expire_history = new Runnable()
  {
    public void run()
    {
      clear_expired_and_get_history();
      if (_listener != null)
        _listener.on_clipboard_history_change();
    }
  };

  ClipboardHistoryService(Context ctx)
  {
    this(ctx, new Handler(ctx.getMainLooper()));
  }

  ClipboardHistoryService(Context ctx, Handler handler)
  {
    _handler = handler;
    _history = new ArrayList<HistoryEntry>();
    _cm = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    _system_listener = this.new SystemListener();
    _cm.addPrimaryClipChangedListener(_system_listener);
  }

  public synchronized List<String> clear_expired_and_get_history()
  {
    long now_ms = System.currentTimeMillis();
    List<String> dst = new ArrayList<String>();
    Iterator<HistoryEntry> it = _history.iterator();
    while (it.hasNext())
    {
      HistoryEntry ent = it.next();
      if (ent.expiry_timestamp <= now_ms)
        it.remove();
      else
        dst.add(ent.content);
    }
    schedule_expiry(now_ms);
    return dst;
  }

  private void schedule_expiry(long now_ms)
  {
    _handler.removeCallbacks(_expire_history);
    long next = Long.MAX_VALUE;
    for (HistoryEntry ent : _history)
      next = Math.min(next, ent.expiry_timestamp);
    if (next != Long.MAX_VALUE)
      _handler.postDelayed(_expire_history, Math.max(0, next - now_ms));
  }

  public synchronized boolean contains_history_entry(String clip)
  {
    return clear_expired_and_get_history().contains(clip);
  }

  public static String get_current_clip()
  {
    if (_service == null)
      return null;
    ClipData clip = _service.read_current_clip();
    if (clip == null || clip.getItemCount() == 0)
      return null;
    CharSequence text = clip.getItemAt(0).getText();
    return text == null ? null : text.toString();
  }

  /** This will call [on_clipboard_history_change]. */
  public synchronized void remove_history_entry(String clip)
  {
    int last_pos = _history.size() - 1;
    boolean first_pos_changed = false;
    for (int pos = last_pos; pos >= 0; pos--)
    {
      if (!_history.get(pos).content.equals(clip))
        continue;
      // Removing the current clipboard (index 0), clear the system clipboard.
      if (pos == 0)
        first_pos_changed = true;
      _history.remove(pos);
    }
    // History can differ from the current clipboard (for example after a
    // sensitive copy). Never clear an unrelated current clip.
    if (first_pos_changed && clip.equals(get_current_clip()))
    {
      if (VERSION.SDK_INT >= 28)
        _cm.clearPrimaryClip();
      else
        _cm.setText("");
    }
    schedule_expiry(System.currentTimeMillis());
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  /** Add clipboard entries to the history, skipping consecutive duplicates and
      empty strings. */
  public synchronized void add_clip(String clip)
  {
    if (!Config.globalConfig().clipboard_history_enabled)
      return;
    clear_expired_and_get_history();
    int size = _history.size();
    if (clip.equals("") || (size > 0 && _history.get(0).content.equals(clip)))
      return;
    if (size >= MAX_HISTORY_SIZE)
      _history.remove(size - 1);
    int ttl = get_history_ttl_minutes();
    long now_ms = System.currentTimeMillis();
    _history.add(0, new HistoryEntry(clip, ttl < 0 ? Long.MAX_VALUE :
        now_ms + TimeUnit.MINUTES.toMillis(ttl)));
    schedule_expiry(now_ms);
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public synchronized void clear_history()
  {
    _handler.removeCallbacks(_expire_history);
    _history.clear();
    if (_listener != null)
      _listener.on_clipboard_history_change();
  }

  public void set_on_clipboard_history_change(OnClipboardHistoryChange l) { _listener = l; }

  public void remove_on_clipboard_history_change(OnClipboardHistoryChange l)
  {
    if (_listener == l)
      _listener = null;
  }

  public static interface OnClipboardHistoryChange
  {
    public void on_clipboard_history_change();
  }

  /** Add what is currently in the system clipboard into the history. */
  void add_current_clip()
  {
    if (!Config.globalConfig().clipboard_history_enabled)
      return;
    ClipData clip = read_current_clip();
    if (clip == null)
      return;
    int count = clip.getItemCount();
    for (int i = 0; i < count; i++)
    {
      CharSequence text = clip.getItemAt(i).getText();
      if (text != null)
        add_clip(text.toString());
    }
  }

  private ClipData read_current_clip()
  {
    ClipData clip = null;
    // getPrimaryClip might throw when the keyboard is disconnected.
    try { clip = _cm.getPrimaryClip(); } catch (Exception _e) {}
    if (clip == null)
      return null;
    // On Android 13+ (API 33+), respect EXTRA_IS_SENSITIVE to protect passwords and OTPs
    if (VERSION.SDK_INT >= 33 && clip.getDescription() != null
        && shouldRejectSensitiveClip(VERSION.SDK_INT, isMarkedSensitive(clip.getDescription())))
      return null;
    return clip;
  }

  /** True only when the platform exposes and marks a clip as sensitive. */
  static boolean shouldRejectSensitiveClip(int sdkInt, boolean markedSensitive)
  {
    return sdkInt >= 33 && markedSensitive;
  }

  @androidx.annotation.RequiresApi(33)
  private static boolean isMarkedSensitive(ClipDescription description)
  {
    android.os.PersistableBundle extras = description.getExtras();
    return extras != null
        && extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false);
  }

  int get_history_ttl_minutes() {
    return Config.globalConfig().clipboard_history_duration;
  }

  final class SystemListener implements ClipboardManager.OnPrimaryClipChangedListener
  {
    public SystemListener() {}

    @Override
    public void onPrimaryClipChanged()
    {
      add_current_clip();
    }
  }

  static final class HistoryEntry
  {
    public final String content;

    /** Time at which the entry expires. */
    public final long expiry_timestamp;

    public HistoryEntry(String c, long expiry)
    {
      content = c;
      expiry_timestamp = expiry;
    }
  }

  public interface ClipboardPasteCallback
  {
    public void paste_from_clipboard_pane(String content);
  }
}
