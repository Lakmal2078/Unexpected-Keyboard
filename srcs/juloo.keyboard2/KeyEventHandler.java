package juloo.keyboard2;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import java.util.Iterator;
import juloo.keyboard2.suggestions.Suggestions;

public final class KeyEventHandler
  implements Config.IKeyEventHandler,
             ClipboardHistoryService.ClipboardPasteCallback,
             CurrentlyTypedWord.Callback
{
  Config _config;
  IReceiver _recv;
  Autocapitalisation _autocap;
  Suggestions _suggestions;
  CurrentlyTypedWord _typedword;
  /** State of the system modifiers. It is updated whether a modifier is down
      or up and a corresponding key event is sent. */
  Pointers.Modifiers _mods;
  /** Consistent with [_mods]. This is a mutable state rather than computed
      from [_mods] to ensure that the meta state is correct while up and down
      events are sent for the modifier keys. */
  int _meta_state = 0;
  /** Whether to force sending arrow keys to move the cursor when
      [setSelection] could be used instead. */
  boolean _move_cursor_force_fallback = false;
  /** Remember the action that was handled. This is used by autocorrect. */
  LastAction _last_action = null;
  LastAction _next_last_action = null;
  StringBuilder _sinhala_buffer = new StringBuilder();

  public KeyEventHandler(Config conf, IReceiver recv, Suggestions sg)
  {
    _config = conf;
    _recv = recv;
    Handler handler = recv.getHandler();
    _autocap = new Autocapitalisation(handler,
        this.new Autocapitalisation_callback());
    _mods = Pointers.Modifiers.EMPTY;
    _suggestions = sg;
    _typedword = new CurrentlyTypedWord(handler, this);
  }

  @Override
  public void finish_composing()
  {
    if (_sinhala_buffer.length() > 0)
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn != null)
      {
        conn.finishComposingText();
      }
      _sinhala_buffer.setLength(0);
      _suggestions.clear();
      _recv.set_suggestions(_suggestions);
    }
  }

  private static boolean is_ascii_letters(String s)
  {
    if (s == null || s.isEmpty())
      return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
        return false;
    }
    return true;
  }

  /** Editing just started. */
  public void started()
  {
    InputConnection ic = _recv.getCurrentInputConnection();
    _autocap.started(_config, ic);
    _typedword.started(_config, ic);
    _suggestions.started();
    _sinhala_buffer.setLength(0);
    _move_cursor_force_fallback =
      _config.editor_config.should_move_cursor_force_fallback;
    _last_action = null;
  }

  /** Selection has been updated. */
  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    if (_sinhala_buffer.length() > 0)
    {
      if (newSelStart != newSelEnd)
      {
        finish_composing();
      }
      else if (candidatesStart != -1 && (newSelStart < candidatesStart || newSelStart > candidatesEnd))
      {
        finish_composing();
      }
    }
    _autocap.selection_updated(oldSelStart, newSelStart);
    _typedword.selection_updated(oldSelStart, newSelStart, newSelEnd);
  }

  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    selection_updated(oldSelStart, newSelStart, newSelEnd, -1, -1);
  }

  /** A key is being pressed. There will not necessarily be a corresponding
      [key_up] event. */
  @Override
  public void key_down(KeyValue key, boolean isSwipe)
  {
    if (key == null)
      return;
    // Stop auto capitalisation when pressing some keys
    switch (key.getKind())
    {
      case Modifier:
        switch (key.getModifier())
        {
          case CTRL:
          case ALT:
          case META:
            _autocap.stop();
            break;
        }
        break;
      case Compose_pending:
        _autocap.stop();
        break;
      case Slider:
        // Don't wait for the next key_up and move the cursor right away. This
        // is called after the trigger distance have been travelled.
        handle_slider(key.getSlider(), key.getSliderRepeat(), true);
        break;
      default: break;
    }
  }

  /** A key has been released. */
  @Override
  public void key_up(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    _next_last_action = LastAction.OTHER;
    Pointers.Modifiers old_mods = _mods;
    update_meta_state(mods);
    switch (key.getKind())
    {
      case Char: send_text(String.valueOf(key.getChar())); break;
      case String: send_text(key.getString()); break;
      case Event: _recv.handle_event_key(key.getEvent()); break;
      case Keyevent: send_key_down_up(key.getKeyevent()); break;
      case Modifier: break;
      case Editing: handle_editing_key(key.getEditing()); break;
      case Compose_pending: _recv.set_compose_pending(true); break;
      case Slider: handle_slider(key.getSlider(), key.getSliderRepeat(), false); break;
      case Macro: evaluate_macro(key.getMacro()); break;
      case Stateful: handle_stateful(key.getStateful()); break;
    }
    update_meta_state(old_mods);
    _last_action = _next_last_action;
  }

  @Override
  public void mods_changed(Pointers.Modifiers mods)
  {
    update_meta_state(mods);
  }

  @Override
  public void suggestion_entered(String text)
  {
    suggestion_entered(text, _config.suggestions_add_space);
  }

  public void suggestion_entered(String text, boolean add_space_after)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (text.startsWith("📋 ") && conn != null)
    {
      String pasteText = (_suggestions.clipboard_full_text != null)
          ? _suggestions.clipboard_full_text
          : text.substring(2).trim();
      conn.commitText(pasteText, 1);
      _suggestions.clear();
      _recv.set_suggestions(_suggestions);
      _next_last_action = LastAction.SUGGESTION_ENTERED;
      return;
    }
    if (text.startsWith("= "))
    {
      text = text.substring(2);
      if (add_space_after)
        text = text + " ";
      String old = _typedword.get();
      int cur_rel = _typedword.cursor_relative();
      replace_surrounding_text(old.length() + cur_rel, -cur_rel, text);
      last_replaced_word = old;
      last_replacement_word_len = text.length();
      _next_last_action = LastAction.SUGGESTION_ENTERED;
      _suggestions.clear();
      _recv.set_suggestions(_suggestions);
      return;
    }
    if (_sinhala_buffer.length() > 0 && conn != null)
    {
      String rawText = text.trim();
      if (add_space_after)
        text = text + " ";
      conn.commitText(text, 1);
      _sinhala_buffer.setLength(0);
      _suggestions.clear();
      java.util.List<String> nextWords = SinhalaTransliterator.getNextWordPredictions(rawText);
      if (!nextWords.isEmpty() && _config.suggestions_enabled)
      {
        _suggestions.set_predictions(nextWords);
      }
      else
      {
        _recv.set_suggestions(_suggestions);
      }
      _typedword.typed(text);
      _next_last_action = LastAction.SUGGESTION_ENTERED;
      return;
    }
    String rawText = text.trim();
    if (add_space_after)
      text = text + " ";
    String old = _typedword.get();
    int cur_rel = _typedword.cursor_relative();
    if (old.length() > 0)
    {
      replace_surrounding_text(old.length() + cur_rel, -cur_rel, text);
      last_replaced_word = old;
      last_replacement_word_len = text.length();
    }
    else if (conn != null)
    {
      conn.commitText(text, 1);
      _typedword.typed(text);
    }
    _suggestions.clear();
    java.util.List<String> nextWords = SinhalaTransliterator.getNextWordPredictions(rawText);
    if (!nextWords.isEmpty() && _config.suggestions_enabled)
    {
      _suggestions.set_predictions(nextWords);
    }
    else
    {
      _recv.set_suggestions(_suggestions);
    }
    _next_last_action = LastAction.SUGGESTION_ENTERED;
  }

  @Override
  public void paste_from_clipboard_pane(String content)
  {
    send_text(content);
  }

  @Override
  public void currently_typed_word(String word)
  {
    if (_config.sinhala_transliteration && _sinhala_buffer.length() > 0)
    {
      return;
    }
    _suggestions.currently_typed_word(word);
  }

  public void dictionary_changed()
  {
    // Refresh the suggestions immediately after dictionary changed.
    _suggestions.currently_typed_word(_typedword.get());
  }

  /** Update [_mods] to be consistent with the [mods], sending key events if
      needed. */
  void update_meta_state(Pointers.Modifiers mods)
  {
    // Released modifiers
    Iterator<KeyValue> it = _mods.diff(mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), false);
    // Activated modifiers
    it = mods.diff(_mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), true);
    _mods = mods;
  }

  void sendMetaKey(int eventCode, int meta_flags, boolean down)
  {
    if (down)
    {
      _meta_state = _meta_state | meta_flags;
      send_keyevent(KeyEvent.ACTION_DOWN, eventCode, _meta_state);
    }
    else
    {
      send_keyevent(KeyEvent.ACTION_UP, eventCode, _meta_state);
      _meta_state = _meta_state & ~meta_flags;
    }
  }

  void sendMetaKeyForModifier(KeyValue kv, boolean down)
  {
    switch (kv.getKind())
    {
      case Modifier:
        switch (kv.getModifier())
        {
          case CTRL:
            sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, down);
            break;
          case ALT:
            sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, down);
            break;
          case SHIFT:
            sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, down);
            break;
          case META:
            sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, down);
            break;
          default:
            break;
        }
        break;
    }
  }

  void send_key_down_up(int keyCode)
  {
    send_key_down_up(keyCode, _meta_state);
  }

  /** Ignores currently pressed system modifiers. */
  void send_key_down_up(int keyCode, int metaState)
  {
    send_keyevent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    send_keyevent(KeyEvent.ACTION_UP, keyCode, metaState);
  }

  void send_keyevent(int eventAction, int eventCode, int metaState)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    if (eventAction == KeyEvent.ACTION_DOWN)
      finish_composing();
    conn.sendKeyEvent(new KeyEvent(1, 1, eventAction, eventCode, 0,
          metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
          KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    if (eventAction == KeyEvent.ACTION_UP)
    {
      _autocap.event_sent(eventCode, metaState);
      _typedword.event_sent(eventCode, metaState);
    }
  }

  void send_text(String text)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;

    boolean is_sinhala = _config.sinhala_transliteration
        && (_meta_state & (KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON)) == 0;

    if (is_sinhala && is_ascii_letters(text))
    {
      _sinhala_buffer.append(text);
      String transliterated = SinhalaTransliterator.transliterateWord(_sinhala_buffer.toString());
      conn.setComposingText(transliterated, 1);
      if (_config.suggestions_enabled)
        _suggestions.currently_typed_word(_sinhala_buffer.toString());
      _next_last_action = LastAction.OTHER;
      return;
    }

    finish_composing();
    _autocap.typed(text);
    _typedword.typed(text);
    conn.commitText(text, 1);
  }

  void replace_surrounding_text(int remove_before, int remove_after,
      String new_text)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.beginBatchEdit();
    conn.deleteSurroundingText(remove_before, remove_after);
    conn.commitText(new_text, 1);
    _typedword.remove_surrounding_text(remove_before, remove_after);
    _typedword.typed(new_text);
    conn.endBatchEdit();
  }

  /** See {!InputConnection.performContextMenuAction}. */
  void send_context_menu_action(int id)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    conn.performContextMenuAction(id);
  }

  @SuppressLint("InlinedApi")
  void handle_editing_key(KeyValue.Editing ev)
  {
    switch (ev)
    {
      case COPY: if(_typedword.is_selection_not_empty()) send_context_menu_action(android.R.id.copy); break;
      case PASTE: send_context_menu_action(android.R.id.paste); break;
      case CUT: if(_typedword.is_selection_not_empty()) send_context_menu_action(android.R.id.cut); break;
      case SELECT_ALL: send_context_menu_action(android.R.id.selectAll); break;
      case SHARE: send_context_menu_action(android.R.id.shareText); break;
      case PASTE_PLAIN: send_context_menu_action(android.R.id.pasteAsPlainText); break;
      case UNDO: send_context_menu_action(android.R.id.undo); break;
      case REDO: send_context_menu_action(android.R.id.redo); break;
      case REPLACE: send_context_menu_action(android.R.id.replaceText); break;
      case ASSIST: send_context_menu_action(android.R.id.textAssist); break;
      case AUTOFILL: send_context_menu_action(android.R.id.autofill); break;
      case DELETE_WORD: handle_delete_word(); break;
      case FORWARD_DELETE_WORD: handle_forward_delete_word(); break;
      case SELECTION_CANCEL: cancel_selection(); break;
      case SPACE_BAR: handle_space_bar(); break;
      case BACKSPACE: handle_backspace(); break;
    }
  }

  static ExtractedTextRequest _move_cursor_req = null;

  /** Query the cursor position. The extracted text is empty. Returns [null] if
      the editor doesn't support this operation. */
  ExtractedText get_cursor_pos(InputConnection conn)
  {
    if (_move_cursor_req == null)
    {
      _move_cursor_req = new ExtractedTextRequest();
      _move_cursor_req.hintMaxChars = 0;
    }
    return conn.getExtractedText(_move_cursor_req, 0);
  }

  /** [r] might be negative, in which case the direction is reversed. */
  void handle_slider(KeyValue.Slider s, int r, boolean key_down)
  {
    finish_composing();
    switch (s)
    {
      case Cursor_left: move_cursor(-r); break;
      case Cursor_right: move_cursor(r); break;
      case Cursor_up: move_cursor_vertical(-r); break;
      case Cursor_down: move_cursor_vertical(r); break;
      case Selection_cursor_left: move_cursor_sel(r, true, key_down); break;
      case Selection_cursor_right: move_cursor_sel(r, false, key_down); break;
    }
  }

  void handle_stateful(KeyValue.Stateful st)
  {
    switch (st)
    {
      case Complete_first:
      case Complete_second:
      case Complete_third:
      case Complete_emoji:
        suggestion_entered(st.toString());
        break;
      case Toggle_language:
        toggle_language();
        break;
    }
  }

  public void toggle_language()
  {
    finish_composing();
    _config.sinhala_transliteration = !_config.sinhala_transliteration;
    Config.globalPrefs().edit().putBoolean("sinhala_transliteration", _config.sinhala_transliteration).apply();
    _suggestions.clear();
    _recv.language_changed();
  }

  /** Move the cursor right or left, if possible without sending key events.
      Unlike arrow keys, the selection is not removed even if shift is not on.
      Falls back to sending arrow keys events if the editor do not support
      moving the cursor or a modifier other than shift is pressed. */
  void move_cursor(int d)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et != null && can_set_selection(conn))
    {
      int sel_start = et.selectionStart;
      int sel_end = et.selectionEnd;
      // Continue expanding the selection even if shift is not pressed
      if (sel_end != sel_start)
      {
        sel_end += d;
        if (sel_end == sel_start) // Avoid making the selection empty
          sel_end += d;
      }
      else
      {
        sel_end += d;
        // Leave 'sel_start' where it is if shift is pressed
        if ((_meta_state & KeyEvent.META_SHIFT_ON) == 0)
          sel_start = sel_end;
      }
      if (conn.setSelection(sel_start, sel_end))
        return; // Fallback to sending key events if [setSelection] failed
    }
    move_cursor_fallback(d);
  }

  /** Move one of the two side of a selection. If [sel_left] is true, the left
      position is moved, otherwise the right position is moved. */
  void move_cursor_sel(int d, boolean sel_left, boolean key_down)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et != null && can_set_selection(conn))
    {
      int sel_start = et.selectionStart;
      int sel_end = et.selectionEnd;
      // Reorder the selection when the slider has just been pressed. The
      // selection might have been reversed if one end crossed the other end
      // with a previous slider.
      if (key_down && sel_start > sel_end)
      {
        sel_start = et.selectionEnd;
        sel_end = et.selectionStart;
      }
      do
      {
        if (sel_left)
          sel_start += d;
        else
          sel_end += d;
        // Move the cursor twice if moving it once would make the selection
        // empty and stop selection mode.
      } while (sel_start == sel_end);
      if (conn.setSelection(sel_start, sel_end))
        return; // Fallback to sending key events if [setSelection] failed
    }
    move_cursor_fallback(d);
  }

  /** Returns whether the selection can be set using [conn.setSelection()].
      This can happen on Termux or when system modifiers are activated for
      example. */
  boolean can_set_selection(InputConnection conn)
  {
    final int system_mods =
      KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON;
    return !_move_cursor_force_fallback && (_meta_state & system_mods) == 0;
  }

  void move_cursor_fallback(int d)
  {
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_LEFT, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_RIGHT, d);
  }

  /** Move the cursor up and down. This sends UP and DOWN key events that might
      make the focus exit the text box. */
  void move_cursor_vertical(int d)
  {
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_UP, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_DOWN, d);
  }

  void evaluate_macro(KeyValue[] keys)
  {
    if (keys.length == 0)
      return;
    // Ignore modifiers that are activated at the time the macro is evaluated
    mods_changed(Pointers.Modifiers.EMPTY);
    evaluate_macro_loop(keys, 0, Pointers.Modifiers.EMPTY, _autocap.pause());
  }

  /** Evaluate the macro asynchronously to make sure event are processed in the
      right order. */
  void evaluate_macro_loop(final KeyValue[] keys, int i, Pointers.Modifiers mods, final boolean autocap_paused)
  {
    boolean should_delay = false;
    KeyValue kv = KeyModifier.modify_no_modmap(keys[i], mods);
    if (kv != null)
    {
      if (kv.hasFlagsAny(KeyValue.FLAG_LATCH))
      {
        // Non-special latchable keys clear latched modifiers
        if (!kv.hasFlagsAny(KeyValue.FLAG_SPECIAL))
          mods = Pointers.Modifiers.EMPTY;
        mods = mods.with_extra_mod(kv);
      }
      else
      {
        key_down(kv, false);
        key_up(kv, mods);
        mods = Pointers.Modifiers.EMPTY;
      }
      should_delay = wait_after_macro_key(kv);
    }
    i++;
    if (i >= keys.length) // Stop looping
    {
      _autocap.unpause(autocap_paused);
    }
    else if (should_delay)
    {
      // Add a delay before sending the next key to avoid race conditions
      // causing keys to be handled in the wrong order. Notably, KeyEvent keys
      // handling is scheduled differently than the other edit functions.
      final int i_ = i;
      final Pointers.Modifiers mods_ = mods;
      _recv.getHandler().postDelayed(new Runnable() {
        public void run()
        {
          evaluate_macro_loop(keys, i_, mods_, autocap_paused);
        }
      }, 1000/30);
    }
    else
      evaluate_macro_loop(keys, i, mods, autocap_paused);
  }

  boolean wait_after_macro_key(KeyValue kv)
  {
    switch (kv.getKind())
    {
      case Keyevent:
      case Editing:
      case Event:
        return true;
      case Slider:
        return _move_cursor_force_fallback;
      default:
        return false;
    }
  }

  /** Repeat calls to [send_key_down_up]. */
  void send_key_down_up_repeat(int event_code, int repeat)
  {
    while (repeat-- > 0)
      send_key_down_up(event_code);
  }

  void cancel_selection()
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    ExtractedText et = get_cursor_pos(conn);
    if (et == null) return;
    final int curs = et.selectionStart;
    // Notify the receiver as Android's [onUpdateSelection] is not triggered.
    if (conn.setSelection(curs, curs))
      _recv.selection_state_changed(false);
  }

  /** The word that was replaced by a suggestion when the last action was to
      enter a suggestion (with the space bar or the candidates view) or [null]
      otherwise. */
  String last_replaced_word = null;
  /** Length of the text before the cursor that should be replaced by
      backspace. */
  int last_replacement_word_len = 0;

  /** Implement autocorrect and Sinhala auto-transliteration when enabled in the settings. */
  void handle_space_bar()
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (_sinhala_buffer.length() > 0 && conn != null)
    {
      String committedSinhala = SinhalaTransliterator.transliterateWord(_sinhala_buffer.toString());
      conn.finishComposingText();
      conn.commitText(" ", 1);
      _sinhala_buffer.setLength(0);
      _suggestions.clear();
      java.util.List<String> nextWords = SinhalaTransliterator.getNextWordPredictions(committedSinhala);
      if (!nextWords.isEmpty() && _config.suggestions_enabled)
      {
        _suggestions.set_predictions(nextWords);
      }
      else
      {
        _recv.set_suggestions(_suggestions);
      }
      _next_last_action = LastAction.OTHER;
      return;
    }
    if (!_typedword.is_selection_not_empty()
        && _typedword.cursor_relative() == 0
        && _last_action == LastAction.OTHER)
    {
      String typed = _typedword.get();
      if (_config.space_bar_auto_complete && _suggestions.count > 0
          && _suggestions.suggestions[0] != null
          && !_suggestions.suggestions[0].startsWith("📋 "))
      {
        // Don't replace the typed word if unecessary to avoid flickering
        String w = _suggestions.suggestions[0];
        if (!w.equals(typed))
        {
          suggestion_entered(w, true);
          return;
        }
      }
    }
    send_text(" ");
  }

  void handle_delete_word()
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn != null)
    {
      CharSequence before = conn.getTextBeforeCursor(50, 0);
      if (before != null && before.length() > 0)
      {
        int len = before.length();
        int i = len;
        // Skip trailing spaces
        while (i > 0 && Character.isWhitespace(before.charAt(i - 1)))
          i--;
        // Skip word characters
        while (i > 0 && !Character.isWhitespace(before.charAt(i - 1)))
          i--;
        int deleteCount = len - i;
        if (deleteCount > 0)
        {
          conn.deleteSurroundingText(deleteCount, 0);
          _typedword.remove_surrounding_text(deleteCount, 0);
          return;
        }
      }
    }
    send_key_down_up(KeyEvent.KEYCODE_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
  }

  void handle_forward_delete_word()
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn != null)
    {
      CharSequence after = conn.getTextAfterCursor(50, 0);
      if (after != null && after.length() > 0)
      {
        int len = after.length();
        int i = 0;
        // Skip leading spaces
        while (i < len && Character.isWhitespace(after.charAt(i)))
          i++;
        // Skip word characters
        while (i < len && !Character.isWhitespace(after.charAt(i)))
          i++;
        if (i > 0)
        {
          conn.deleteSurroundingText(0, i);
          _typedword.remove_surrounding_text(0, i);
          return;
        }
      }
    }
    send_key_down_up(KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
  }

  /** Undo the last autocorrect. */
  void handle_backspace()
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (_sinhala_buffer.length() > 0 && conn != null)
    {
      _sinhala_buffer.deleteCharAt(_sinhala_buffer.length() - 1);
      if (_sinhala_buffer.length() > 0)
      {
        String transliterated = SinhalaTransliterator.transliterateWord(_sinhala_buffer.toString());
        conn.setComposingText(transliterated, 1);
        if (_config.suggestions_enabled)
          _suggestions.currently_typed_word(_sinhala_buffer.toString());
      }
      else
      {
        conn.setComposingText("", 1);
        conn.finishComposingText();
        _suggestions.clear();
        _recv.set_suggestions(_suggestions);
      }
      _next_last_action = LastAction.OTHER;
      return;
    }

    if (_last_action == LastAction.SUGGESTION_ENTERED
        && last_replaced_word != null)
    {
      replace_surrounding_text(last_replacement_word_len, 0, last_replaced_word + " ");
      last_replaced_word = null;
      _next_last_action = LastAction.SUGGESTION_UNDO;
    }
    else
    {
      send_key_down_up(KeyEvent.KEYCODE_DEL);
    }
  }

  public static interface IReceiver extends Suggestions.Callback
  {
    public void handle_event_key(KeyValue.Event ev);
    public void set_shift_state(boolean state, boolean lock);
    public void set_compose_pending(boolean pending);
    public void selection_state_changed(boolean selection_is_ongoing);
    public InputConnection getCurrentInputConnection();
    public Handler getHandler();
    public void language_changed();
  }

  class Autocapitalisation_callback implements Autocapitalisation.Callback
  {
    @Override
    public void update_shift_state(boolean should_enable, boolean should_disable)
    {
      if (should_enable)
        _recv.set_shift_state(true, false);
      else if (should_disable)
        _recv.set_shift_state(false, false);
    }
  }

  public static enum LastAction
  {
    SUGGESTION_ENTERED,
    SUGGESTION_UNDO,
    OTHER
  }
}
