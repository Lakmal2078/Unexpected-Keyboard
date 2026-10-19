package juloo.keyboard2;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Animatable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.dict.DictionariesActivity;
import juloo.keyboard2.R;

public class LauncherActivity extends Activity implements Handler.Callback
{
  /** Text is replaced when receiving key events. */
  TextView _tryhere_text;
  EditText _tryhere_area;
  /** Periodically restart the animations. */
  List<Animatable> _animations;
  Handler _handler;
  TextView _enabled_status;
  TextView _selected_status;
  TextView _typing_status;
  private boolean _restoring_text = false;
  private boolean _typing_completed = false;
  private static final String STATE_TYPING_COMPLETED = "typing_completed";

  private static final class RetainedState
  {
    String text;
    boolean completed;
    RetainedState(String text, boolean completed) { this.text = text; this.completed = completed; }
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.launcher_activity);
    _tryhere_text = (TextView)findViewById(R.id.launcher_tryhere_text);
    _tryhere_area = (EditText)findViewById(R.id.launcher_tryhere_area);
    _enabled_status = (TextView)findViewById(R.id.launcher_enabled_status);
    _selected_status = (TextView)findViewById(R.id.launcher_selected_status);
    _typing_status = (TextView)findViewById(R.id.launcher_typing_status);
    RetainedState retained = (RetainedState)getLastNonConfigurationInstance();
    if (retained != null)
    {
      _restoring_text = true;
      _tryhere_area.setText(retained.text);
      _typing_completed = retained.completed;
      _restoring_text = false;
    }
    if (_tryhere_area instanceof TypingTestEditText)
      ((TypingTestEditText)_tryhere_area).setOnImeInputListener(text -> {
        if (!_restoring_text && text != null && text.length() > 0 && isOwnKeyboardSelected())
        {
          _typing_completed = true;
          refreshSetupStatus();
        }
      });
    _tryhere_area.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    refreshSetupStatus();
    if (VERSION.SDK_INT >= 28)
      _tryhere_area.addOnUnhandledKeyEventListener(
          this.new Tryhere_OnUnhandledKeyEventListener());
    _handler = new Handler(getMainLooper(), this);
  }

  @Override
  public Object onRetainNonConfigurationInstance()
  {
    // Keep test content in memory across configuration changes only; never place it in a Bundle or preferences.
    return new RetainedState(_tryhere_area == null ? "" : _tryhere_area.getText().toString(), _typing_completed);
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    refreshSetupStatus();
  }

  private ComponentName ownImeComponent()
  {
    return new ComponentName(this, Keyboard2.class);
  }

  private boolean isOwnKeyboardEnabled()
  {
    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    if (imm == null) return false;
    ComponentName own = ownImeComponent();
    for (InputMethodInfo info : imm.getEnabledInputMethodList())
    {
      if (own.equals(new ComponentName(info.getPackageName(), info.getServiceName()))) return true;
    }
    return false;
  }

  private boolean isOwnKeyboardSelected()
  {
    String selected = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
    if (selected == null) return false;
    ComponentName parsed = ComponentName.unflattenFromString(selected);
    return ownImeComponent().equals(parsed);
  }

  private void refreshSetupStatus()
  {
    if (_enabled_status == null || _selected_status == null || _typing_status == null) return;
    boolean enabled = isOwnKeyboardEnabled();
    boolean selected = isOwnKeyboardSelected();
    _enabled_status.setText(getString(R.string.launcher_status_enabled, getString(enabled ? R.string.launcher_status_done : R.string.launcher_status_needed)));
    _selected_status.setText(getString(R.string.launcher_status_selected, getString(selected ? R.string.launcher_status_done : R.string.launcher_status_needed)));
    _typing_status.setText(getString(R.string.launcher_status_typing, getString(_typing_completed ? R.string.launcher_status_done : R.string.launcher_status_needed)));
    findViewById(R.id.launcher_button_imepicker).setEnabled(enabled);
  }

  public void clear_typing_test(View v)
  {
    _restoring_text = true;
    _tryhere_area.setText("");
    _restoring_text = false;
    _typing_completed = false;
    refreshSetupStatus();
    _tryhere_area.requestFocus();
  }

  @Override
  public void onStart()
  {
    super.onStart();
    _animations = new ArrayList<Animatable>();
    _animations.add(find_anim(R.id.launcher_anim_swipe));
    _animations.add(find_anim(R.id.launcher_anim_round_trip));
    _animations.add(find_anim(R.id.launcher_anim_circle));
    _handler.removeMessages(0);
    _handler.sendEmptyMessageDelayed(0, 500);
  }

  @Override
  public void onStop()
  {
    _handler.removeMessages(0);
    for (Animatable anim : _animations)
      anim.stop();
    _animations.clear();
    super.onStop();
  }

  @Override
  public boolean handleMessage(Message _msg)
  {
    for (Animatable anim : _animations)
      anim.start();
    _handler.sendEmptyMessageDelayed(0, 3000);
    return true;
  }

  @Override
  public final boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.launcher_menu, menu);
    return true;
  }

  @Override
  public final boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == R.id.btnLaunchSettingsActivity)
    {
      Intent intent = new Intent(LauncherActivity.this, SettingsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }
    return super.onOptionsItemSelected(item);
  }

  public void launch_imesettings(View _btn)
  {
    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
  }

  public void launch_imepicker(View v)
  {
    InputMethodManager imm =
      (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    imm.showInputMethodPicker();
  }

  public void launch_dictionaries_activity(View v)
  {
    startActivity(new Intent(this, DictionariesActivity.class));
  }

  Animatable find_anim(int id)
  {
    ImageView img = (ImageView)findViewById(id);
    return (Animatable)img.getDrawable();
  }

  @androidx.annotation.RequiresApi(28)
  final class Tryhere_OnUnhandledKeyEventListener implements View.OnUnhandledKeyEventListener
  {
    public boolean onUnhandledKeyEvent(View v, KeyEvent ev)
    {
      // Don't handle the back key
      if (ev.getKeyCode() == KeyEvent.KEYCODE_BACK)
        return false;
      // Key release of modifiers would erase interesting data
      if (KeyEvent.isModifierKey(ev.getKeyCode()))
        return false;
      StringBuilder s = new StringBuilder();
      if (ev.isAltPressed()) s.append("Alt+");
      if (ev.isShiftPressed()) s.append("Shift+");
      if (ev.isCtrlPressed()) s.append("Ctrl+");
      if (ev.isMetaPressed()) s.append("Meta+");
      // s.append(ev.getDisplayLabel());
      String kc = KeyEvent.keyCodeToString(ev.getKeyCode());
      s.append(kc.replaceFirst("^KEYCODE_", ""));
      _tryhere_text.setText(s.toString());
      return false;
    }
  }
}
