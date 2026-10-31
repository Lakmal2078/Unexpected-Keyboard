package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.util.LogPrinter;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.dict.DictionariesActivity;
import juloo.keyboard2.dict.DictionarySwitcher;
import juloo.keyboard2.prefs.LayoutsPreference;
import juloo.keyboard2.suggestions.CandidatesView;
import juloo.keyboard2.suggestions.Suggestions;

public class Keyboard2 extends InputMethodService
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  /** The view containing the keyboard and candidates view. */
  private ViewGroup _keyboard_container_view;
  private Keyboard2View _keyboard_layout_view;
  private CandidatesView _candidates_view;
  private ViewGroup _quick_toolbar;
  private boolean _has_active_suggestions = false;
  private Suggestions _suggestions;
  private KeyEventHandler _keyeventhandler;
  /** If not 'null', the layout to use instead of [_config.current_layout]. */
  private KeyboardData _currentSpecialLayout;
  /** Layout associated with the currently selected locale. Not 'null'. */
  private KeyboardData _localeTextLayout;
  /** Installed and current locales. */
  private Dictionaries _dictionaries;
  private ViewGroup _emojiPane = null;
  private ViewGroup _clipboard_pane = null;
  private Handler _handler;

  private Config _config;
  /** Last configuration used to derive orientation- and window-dependent keyboard metrics. */
  private Configuration _last_configuration;

  private FoldStateTracker _foldStateTracker;

  /** Layout currently visible before it has been modified. */
  KeyboardData current_layout_unmodified()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    KeyboardData layout = null;
    int layout_i = _config.get_current_layout();
    if (layout_i >= _config.layouts.size())
      layout_i = 0;
    if (layout_i < _config.layouts.size())
      layout = _config.layouts.get(layout_i);
    if (layout == null)
      layout = _localeTextLayout;
    return layout;
  }

  /** Layout currently visible. */
  KeyboardData current_layout()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    return LayoutModifier.modify_layout(current_layout_unmodified());
  }

  void setTextLayout(int l)
  {
    _config.set_current_layout(l);
    _currentSpecialLayout = null;
    // The active dictionary depends on the current layout.
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  void incrTextLayout(int delta)
  {
    int s = _config.layouts.size();
    setTextLayout((_config.get_current_layout() + delta + s) % s);
  }

  void setSpecialLayout(KeyboardData l)
  {
    _currentSpecialLayout = l;
    _keyboard_layout_view.setKeyboard(l);
  }

  KeyboardData loadLayout(int layout_id)
  {
    return KeyboardData.load(getResources(), layout_id);
  }

  /** Load a layout that contains a numpad. */
  KeyboardData loadNumpad(int layout_id)
  {
    return LayoutModifier.modify_numpad(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  KeyboardData loadNumericLayout()
  {
    return loadNumpad(_config.orientation_landscape ?
        R.xml.numeric_landscape : R.xml.numeric);
  }

  KeyboardData loadPinentry(int layout_id)
  {
    return LayoutModifier.modify_pinentry(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    _handler = new Handler(getMainLooper());
    _foldStateTracker = new FoldStateTracker(this);
    _dictionaries = Dictionaries.instance(this);
    Config.initGlobalConfig(prefs, getResources(),
        _foldStateTracker.isUnfolded(), _dictionaries);
    _config = Config.globalConfig();
    _last_configuration = new Configuration(getResources().getConfiguration());
    Receiver recvr = this.new Receiver();
    _suggestions = new Suggestions(recvr, _config);
    _keyeventhandler = new KeyEventHandler(_config, recvr, _suggestions, this);
    KeyValue.Stateful._handler = recvr;
    _config.handler = _keyeventhandler;
    prefs.registerOnSharedPreferenceChangeListener(this);
    Logs.set_debug_logs(getResources().getBoolean(R.bool.debug_logs));
    refreshSubtypeImm();
    create_keyboard_view();
    ClipboardHistoryService.on_startup(this, _keyeventhandler);
    _foldStateTracker.setChangedCallback(() -> { refresh_config_and_layout(); });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    _foldStateTracker.close();
    try
    {
      SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
      prefs.unregisterOnSharedPreferenceChangeListener(this);
    }
    catch (Exception e) {}
    KeyValue.Stateful._handler = null;
    ClipboardHistoryService.on_destroy();
  }

  private void create_keyboard_view()
  {
    _keyboard_container_view = (ViewGroup)inflate_view(R.layout.keyboard);
    _keyboard_layout_view = (Keyboard2View)_keyboard_container_view.findViewById(R.id.keyboard_view);
    _candidates_view = (CandidatesView)_keyboard_container_view.findViewById(R.id.candidates_view);
    _quick_toolbar = (ViewGroup)_keyboard_container_view.findViewById(R.id.quick_toolbar);
    setup_quick_toolbar();
  }

  private void setup_quick_toolbar()
  {
    if (_quick_toolbar == null)
      return;
    View btnApps = _quick_toolbar.findViewById(R.id.toolbar_btn_apps);
    if (btnApps != null)
    {
      btnApps.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_emojiPane == null)
            _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
          // Keep the emoji pane aligned with the configured keyboard size.
          View emojiGrid = _emojiPane.findViewById(R.id.emoji_grid);
          if (emojiGrid != null)
          {
            ViewGroup.LayoutParams lp = emojiGrid.getLayoutParams();
            if (lp != null)
            {
              lp.height = _config.keyboard_rows_height_pixels * 3;
              emojiGrid.setLayoutParams(lp);
            }
          }
          setInputView(_emojiPane);
        }
      });
      btnApps.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          incrTextLayout(1);
          return true;
        }
      });
    }

    View btnKeyboard = _quick_toolbar.findViewById(R.id.toolbar_btn_keyboard);
    if (btnKeyboard != null)
    {
      btnKeyboard.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          get_imm().showInputMethodPicker();
        }
      });
      btnKeyboard.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          incrTextLayout(1);
          return true;
        }
      });
    }

    View btnTranslate = _quick_toolbar.findViewById(R.id.toolbar_btn_translate);
    if (btnTranslate != null)
    {
      btnTranslate.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          launch_translate();
        }
      });
      btnTranslate.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          try
          {
            SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(Keyboard2.this);
            boolean nextState = !_config.sinhala_transliteration;
            prefs.edit().putBoolean("sinhala_transliteration", nextState).apply();
          }
          catch (Exception e) {}
          return true;
        }
      });
    }

    View btnSettings = _quick_toolbar.findViewById(R.id.toolbar_btn_settings);
    if (btnSettings != null)
    {
      btnSettings.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          Intent intent = new Intent(Keyboard2.this, SettingsActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
        }
      });
    }

    View btnClipboard = _quick_toolbar.findViewById(R.id.toolbar_btn_clipboard);
    if (btnClipboard != null)
    {
      btnClipboard.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_clipboard_pane == null)
            _clipboard_pane = (ViewGroup)inflate_view(R.layout.clipboard_pane);
          // Clipboard history scrolls within the same height budget as the keyboard.
          View clipboardScroll = _clipboard_pane.findViewById(R.id.clipboard_scroll_view);
          if (clipboardScroll != null)
          {
            ViewGroup.LayoutParams lp = clipboardScroll.getLayoutParams();
            if (lp != null)
            {
              lp.height = _config.keyboard_rows_height_pixels * 3;
              clipboardScroll.setLayoutParams(lp);
            }
          }
          setInputView(_clipboard_pane);
        }
      });
    }

    View btnTheme = _quick_toolbar.findViewById(R.id.toolbar_btn_theme);
    if (btnTheme != null)
    {
      btnTheme.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          show_theme_picker();
        }
      });
      btnTheme.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          Intent intent = new Intent(Keyboard2.this, SettingsActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
          return true;
        }
      });
    }

    if (_quick_toolbar != null)
    {
      int[] btnIds = new int[] {
        R.id.toolbar_btn_apps, R.id.toolbar_btn_keyboard, R.id.toolbar_btn_translate,
        R.id.toolbar_btn_settings, R.id.toolbar_btn_clipboard, R.id.toolbar_btn_theme
      };
      if (_config.custom_colors_enabled)
      {
        int iconTint = Theme.getContrastColor(_config.custom_bg_color);
        for (int id : btnIds)
        {
          android.widget.ImageButton btn = _quick_toolbar.findViewById(id);
          if (btn != null)
            btn.setColorFilter(iconTint);
        }
      }
      else
      {
        for (int id : btnIds)
        {
          android.widget.ImageButton btn = _quick_toolbar.findViewById(id);
          if (btn != null)
            btn.clearColorFilter();
        }
      }
    }
  }

  private void launch_translate()
  {
    // Check before reading the selection or constructing an external intent.
    if (_config == null || _config.editor_config.is_sensitive_input)
      return;
    try
    {
      CharSequence selected = null;
      InputConnection ic = getCurrentInputConnection();
      if (ic != null)
        selected = ic.getSelectedText(0);
      Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.translate");
      if (intent != null && (selected == null || selected.length() == 0))
      {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        return;
      }
      String query = (selected != null && selected.length() > 0)
          ? android.net.Uri.encode(selected.toString()) : "";
      String url = query.isEmpty()
          ? "https://translate.google.com/"
          : "https://translate.google.com/?sl=auto&tl=si&text=" + query + "&op=translate";
      Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
      webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(webIntent);
    }
    catch (ActivityNotFoundException | SecurityException | IllegalArgumentException e)
    {
      // Exception messages can include the URL and selected text.
      Log.w(Logs.TAG, "Unable to launch translation: " + e.getClass().getSimpleName());
    }
  }

  private void show_theme_picker()
  {
    try
    {
      SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
      AlertDialog dialog = ThemePicker.create(new AlertDialog.Builder(this), getResources(), prefs);
      Utils.show_dialog_on_ime(dialog, this);
    }
    catch (android.content.res.Resources.NotFoundException | ClassCastException
        | IllegalStateException | SecurityException | WindowManager.BadTokenException e)
    {
      Log.w(Logs.TAG, "Unable to open theme picker: " + e.getClass().getSimpleName());
    }
  }

  InputMethodManager get_imm()
  {
    return (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  }

  private void refreshSubtypeImm()
  {
    _config.shouldOfferVoiceTyping = true;
    KeyboardData default_layout = null;
    _config.device_locales = DeviceLocales.load(this);
    if (_config.device_locales.default_ != null)
    {
      String layout_name = _config.device_locales.default_.default_layout;
      if (layout_name != null)
        default_layout = LayoutsPreference.layout_of_string(getResources(), layout_name);
    }
    _config.extra_keys_subtype = _config.device_locales.extra_keys();
    if (default_layout == null)
      default_layout = loadLayout(R.xml.latn_qwerty_us);
    _localeTextLayout = default_layout;
  }

  private void refresh_current_dictionary()
  {
    _config.should_show_dictionary_switch =
      (_config.device_locales.installed.size() > 0);
    String dict_name = _dictionaries.get_selected(_config);
    if (dict_name == null)
      dict_name = (_config.device_locales.default_ != null) ?
        _config.device_locales.default_.dictionary : null;
    if (dict_name == null || !_dictionaries.get_installed().contains(dict_name))
      dict_name = "en_US";
    _dictionaries.set_current_dictionary(_config, dict_name);
  }

  /** Remember and apply the dictionary chosen by the user for the current
      context. */
  private void select_dictionary(String dict_name)
  {
    _dictionaries.set_selected(_config, dict_name);
    refresh_current_dictionary();
    refresh_candidates_view();
  }

  private boolean should_show_candidates()
  {
    boolean has_dict = _config.current_dictionary != null;
    boolean has_sinhala = _config.sinhala_transliteration;
    boolean has_productivity = _config.quick_calculator_enabled
        || _config.smart_snippets_enabled
        || _config.clipboard_suggestion_enabled;
    return _config.suggestions_enabled
      && !_config.editor_config.is_sensitive_input
      && (has_dict || has_sinhala || has_productivity)
      && _config.editor_config.should_show_candidates_view
      && !_config.split_layout;
  }

  private void update_top_bar_visibility()
  {
    if (_quick_toolbar != null)
    {
      View translate = _quick_toolbar.findViewById(R.id.toolbar_btn_translate);
      if (translate != null)
        translate.setEnabled(!_config.editor_config.is_sensitive_input);
    }
    boolean can_show_candidates = should_show_candidates();
    boolean show_toolbar = _config.show_quick_toolbar && !_config.split_layout;
    if (show_toolbar)
    {
      if (can_show_candidates && _has_active_suggestions)
      {
        _candidates_view.setVisibility(View.VISIBLE);
        if (_quick_toolbar != null)
          _quick_toolbar.setVisibility(View.GONE);
      }
      else
      {
        if (_quick_toolbar != null)
          _quick_toolbar.setVisibility(View.VISIBLE);
        _candidates_view.setVisibility(View.GONE);
      }
    }
    else
    {
      if (_quick_toolbar != null)
        _quick_toolbar.setVisibility(View.GONE);
      _candidates_view.setVisibility(can_show_candidates ? View.VISIBLE : View.GONE);
    }
  }

  private void refresh_candidates_view()
  {
    _has_active_suggestions = false;
    float row_height = _config.keyboard_rows_height_pixels * (1 - _config.key_vertical_margin);
    if (_quick_toolbar != null)
    {
      ViewGroup.LayoutParams lp = _quick_toolbar.getLayoutParams();
      if (lp != null)
      {
        lp.height = (int)row_height;
        _quick_toolbar.setLayoutParams(lp);
      }
    }
    if (should_show_candidates())
    {
      _candidates_view.refresh_config(_config);
      _keyeventhandler.dictionary_changed();
    }
    update_top_bar_visibility();
  }

  /** Whether a configuration change affects keyboard geometry or its system theme. */
  static boolean shouldRefreshKeyboardConfig(Configuration previous, Configuration next,
      boolean themeFollowsSystem)
  {
    if (previous == null || next == null)
      return true;

    boolean geometryChanged =
        previous.orientation != next.orientation
        || previous.screenWidthDp != next.screenWidthDp
        || previous.screenHeightDp != next.screenHeightDp
        || previous.smallestScreenWidthDp != next.smallestScreenWidthDp
        || previous.densityDpi != next.densityDpi
        || previous.screenLayout != next.screenLayout
        || Float.compare(previous.fontScale, next.fontScale) != 0;

    boolean nightModeChanged = (previous.uiMode & Configuration.UI_MODE_NIGHT_MASK)
        != (next.uiMode & Configuration.UI_MODE_NIGHT_MASK);
    return geometryChanged || (themeFollowsSystem && nightModeChanged);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    if (_config == null || newConfig == null) return;

    Configuration previous = _last_configuration;
    _last_configuration = new Configuration(newConfig);
    if (shouldRefreshKeyboardConfig(previous, newConfig, _config.theme_follows_system))
      refresh_config_and_layout();
  }

  /** Might re-create the keyboard view. [_keyboard_layout_view.setKeyboard()] and
      [setInputView()] must be called soon after. */
  private void refresh_config()
  {
    int prev_theme = _config.theme;
    boolean prev_custom_colors = _config.custom_colors_enabled;
    int prev_custom_bg = _config.custom_bg_color;
    int prev_custom_accent = _config.custom_accent_color;

    _config.refresh(getResources(), _foldStateTracker.isUnfolded(), _dictionaries);
    refresh_current_dictionary();
    // Refreshing the theme config or custom colors requires re-creating the views
    if (prev_theme != _config.theme
        || prev_custom_colors != _config.custom_colors_enabled
        || prev_custom_bg != _config.custom_bg_color
        || prev_custom_accent != _config.custom_accent_color)
    {
      create_keyboard_view();
      _emojiPane = null;
      _clipboard_pane = null;
      setInputView(_keyboard_container_view);
    }
    // Set keyboard background
    if (_config.custom_colors_enabled)
    {
      _keyboard_container_view.setBackgroundColor(_config.custom_bg_color);
      if (_keyboard_container_view.getBackground() != null)
      {
        Drawable bg = _keyboard_container_view.getBackground().mutate();
        bg.setAlpha(_config.keyboardOpacity);
        _keyboard_container_view.setBackground(bg);
      }
    }
    else if (_keyboard_container_view.getBackground() != null)
    {
      Drawable bg = _keyboard_container_view.getBackground().mutate();
      bg.setAlpha(_config.keyboardOpacity);
      _keyboard_container_view.setBackground(bg);
    }
    _keyboard_layout_view.reset();
    refresh_candidates_view();
  }

  /** Recompute config, select the layout for the current window/editor, and refresh key handling. */
  private void refresh_config_and_layout()
  {
    refresh_config();
    _currentSpecialLayout = refresh_special_layout();
    if (_keyboard_layout_view != null)
      _keyboard_layout_view.setKeyboard(current_layout());
    if (_keyeventhandler != null)
      _keyeventhandler.started();
  }

  private KeyboardData refresh_special_layout()
  {
    if (_config.editor_config.numeric_layout)
    {
      switch (_config.selected_number_layout)
      {
        case PIN:
          return loadPinentry(_config.orientation_landscape ?
              R.xml.pin_landscape : R.xml.pin);
        case NUMBER:
          return loadNumericLayout();
      }
    }
    return null;
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    _config.editor_config.refresh(info, getResources());
    refresh_config_and_layout();
    setInputView(_keyboard_container_view);
    Logs.debug_startup_input_view(info, _config);
  }

  @Override
  public void setExtractViewShown(boolean shown){
      super.setExtractViewShown(false);
  }

  @Override
  public void setInputView(View v)
  {
    ViewParent parent = v.getParent();
    if (parent != null && parent instanceof ViewGroup)
      ((ViewGroup)parent).removeView(v);
    super.setInputView(v);
    updateSoftInputWindowLayoutParams();
    v.requestApplyInsets();
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    updateSoftInputWindowLayoutParams();
  }

  private void updateSoftInputWindowLayoutParams() {
    final Window window = getWindow().getWindow();
    // On API >= 35, Keyboard2View behaves as edge-to-edge
    // APIs 30 to 34 have visual artifact when edge-to-edge is enabled
    if (VERSION.SDK_INT >= 35)
    {
      WindowManager.LayoutParams wattrs = window.getAttributes();
      wattrs.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
      // Allow to draw behind system bars
      wattrs.setFitInsetsTypes(0);
      window.setDecorFitsSystemWindows(false);
    }
    updateLayoutHeightOf(window, ViewGroup.LayoutParams.MATCH_PARENT);
    final View inputArea = window.findViewById(android.R.id.inputArea);

    updateLayoutHeightOf(
            (View) inputArea.getParent(),
            isFullscreenMode()
                    ? ViewGroup.LayoutParams.MATCH_PARENT
                    : ViewGroup.LayoutParams.WRAP_CONTENT);
    updateLayoutGravityOf((View) inputArea.getParent(), Gravity.BOTTOM);

  }

  private static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutHeightOf(final View view, final int layoutHeight) {
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private static void updateLayoutGravityOf(final View view, final int layoutGravity) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LinearLayout.LayoutParams) {
      final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else if (lp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    }
  }

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    refreshSubtypeImm();
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    _keyeventhandler.selection_updated(oldSelStart, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    if ((oldSelStart == oldSelEnd) != (newSelStart == newSelEnd))
      _keyboard_layout_view.set_selection_state(newSelStart != newSelEnd);
  }

  @Override
  public void onFinishInputView(boolean finishingInput)
  {
    super.onFinishInputView(finishingInput);
    if (_keyeventhandler != null)
      _keyeventhandler.finish_composing();
    _keyboard_layout_view.reset();
  }

  @Override
  public void onFinishInput()
  {
    super.onFinishInput();
    if (_keyeventhandler != null)
      _keyeventhandler.finish_composing();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences _prefs, String _key)
  {
    refresh_config_and_layout();
  }

  @Override
  public boolean onEvaluateFullscreenMode()
  {
    /* Entirely disable fullscreen mode. */
    return false;
  }

  @Override
  public boolean onEvaluateInputViewShown()
  {
    // Since Android 16, this method returns [false] for unknown reasons.
    if (super.onEvaluateInputViewShown())
      return true;
    if (getResources().getConfiguration().hardKeyboardHidden
        == Configuration.HARDKEYBOARDHIDDEN_NO
        && _config.physical_keyboard_hide)
    {
      Logs.debug("Physical keyboard is present");
      return false;
    }
    return true;
  }

  public void launch_dictionaries_activity()
  {
    start_activity(DictionariesActivity.class);
  }

  /** Called from [onClick] attributes. */
  public void launch_dictionaries_activity(View v)
  {
    launch_dictionaries_activity();
  }

  void start_activity(Class cls)
  {
    Intent intent = new Intent(this, cls);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  /** Not static */
  public class Receiver implements KeyEventHandler.IReceiver,
         KeyValue.Stateful.Symbol_provider, DictionarySwitcher.Callback
  {
    public void handle_event_key(KeyValue.Event ev)
    {
      switch (ev)
      {
        case CONFIG:
          start_activity(SettingsActivity.class);
          break;

        case SWITCH_TEXT:
          _currentSpecialLayout = null;
          _keyboard_layout_view.setKeyboard(current_layout());
          break;

        case SWITCH_NUMERIC:
          setSpecialLayout(loadNumericLayout());
          break;

        case SWITCH_EMOJI:
          if (_emojiPane == null)
            _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
          setInputView(_emojiPane);
          break;

        case SWITCH_CLIPBOARD:
          if (_clipboard_pane == null)
            _clipboard_pane = (ViewGroup)inflate_view(R.layout.clipboard_pane);
          setInputView(_clipboard_pane);
          break;

        case SWITCH_BACK_EMOJI:
        case SWITCH_BACK_CLIPBOARD:
          setInputView(_keyboard_container_view);
          break;

        case CHANGE_METHOD_PICKER:
          get_imm().showInputMethodPicker();
          break;

        case CHANGE_METHOD_PREV:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToLastInputMethod(getConnectionToken());
          else
            switchToPreviousInputMethod();
          break;

        case CHANGE_METHOD_NEXT:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToNextInputMethod(getConnectionToken(), false);
          else
            switchToNextInputMethod(false);
          break;

        case ACTION:
          InputConnection conn = getCurrentInputConnection();
          if (conn != null)
            conn.performEditorAction(_config.editor_config.actionId);
          break;

        case SWITCH_FORWARD:
          incrTextLayout(1);
          break;

        case SWITCH_BACKWARD:
          incrTextLayout(-1);
          break;

        case SWITCH_GREEKMATH:
          setSpecialLayout(loadNumpad(R.xml.greekmath));
          break;

        case CAPS_LOCK:
          set_shift_state(true, true);
          break;

        case SWITCH_VOICE_TYPING:
          if (!VoiceImeSwitcher.switch_to_voice_ime(Keyboard2.this, get_imm(),
                Config.globalPrefs()))
            _config.shouldOfferVoiceTyping = false;
          break;

        case SWITCH_VOICE_TYPING_CHOOSER:
          VoiceImeSwitcher.choose_voice_ime(Keyboard2.this, get_imm(),
              Config.globalPrefs());
          break;

        case HIDE_SELF:
          Keyboard2.this.requestHideSelf(0);
          break;

        case CHANGE_DICTIONARY:
          new DictionarySwitcher(Keyboard2.this, _dictionaries, this).choose();
          break;
      }
    }

    public void set_shift_state(boolean state, boolean lock)
    {
      _keyboard_layout_view.set_shift_state(state, lock);
    }

    public void set_compose_pending(boolean pending)
    {
      _keyboard_layout_view.set_compose_pending(pending);
    }

    public void selection_state_changed(boolean selection_is_ongoing)
    {
      _keyboard_layout_view.set_selection_state(selection_is_ongoing);
    }

    public InputConnection getCurrentInputConnection()
    {
      return Keyboard2.this.getCurrentInputConnection();
    }

    public Handler getHandler()
    {
      return _handler;
    }

    public void set_suggestions(Suggestions suggestions)
    {
      _has_active_suggestions = (suggestions != null)
          && (suggestions.count > 0 || suggestions.emoji_suggestion != null);
      _candidates_view.set_candidates(suggestions);
      update_top_bar_visibility();
    }

    public String provide_stateful_key_symbol(KeyValue.Stateful q)
    {
      switch (q)
      {
        case Complete_first: return _suggestions.suggestions[0];
        case Complete_second: return _suggestions.suggestions[1];
        case Complete_third: return _suggestions.suggestions[2];
        case Complete_emoji: return _suggestions.emoji_suggestion;
        case Toggle_language:
          return _config.sinhala_transliteration ? "සි" : "EN";
      }
      return "";
    }

    public void language_changed()
    {
      refresh_current_dictionary();
      refresh_candidates_view();
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.invalidate();
    }

    public void on_change_dictionary(String dict_name)
    {
      select_dictionary(dict_name);
    }

    public void launch_dictionaries_activity()
    {
      Keyboard2.this.launch_dictionaries_activity();
    }
  }

  private IBinder getConnectionToken()
  {
    return getWindow().getWindow().getAttributes().token;
  }

  private View inflate_view(int layout)
  {
    return View.inflate(new ContextThemeWrapper(this, _config.theme), layout, null);
  }
}
