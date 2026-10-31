package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // Migrate once, then let Settings and the keyboard share the same store.
    try
    {
      DirectBootAwarePreferences.get_shared_preferences(this);
      if (Build.VERSION.SDK_INT >= 24)
        getPreferenceManager().setStorageDeviceProtected();
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);

    Preference resetSettings = findPreference("reset_keyboard_settings");
    if (resetSettings != null)
    {
      resetSettings.setOnPreferenceClickListener(preference -> {
        new AlertDialog.Builder(this)
            .setTitle(R.string.pref_reset_keyboard_settings_confirm_title)
            .setMessage(R.string.pref_reset_keyboard_settings_confirm_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.pref_reset_keyboard_settings_action, (dialog, which) -> {
              SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
              boolean reset = resetKeyboardSettings(prefs, Build.VERSION.SDK_INT >= 24);
              Toast.makeText(this, reset
                      ? R.string.pref_reset_keyboard_settings_done
                      : R.string.pref_reset_keyboard_settings_failed,
                  reset ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
              if (reset)
                recreate();
            })
            .show();
        return true;
      });
    }

    Preference clearLearnedData = findPreference("clear_sinhala_learned_data");
    if (clearLearnedData != null)
    {
      clearLearnedData.setOnPreferenceClickListener(preference -> {
        new AlertDialog.Builder(this)
            .setTitle(R.string.pref_clear_sinhala_learned_data_confirm_title)
            .setMessage(R.string.pref_clear_sinhala_learned_data_confirm_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.pref_clear_sinhala_learned_data_action, (dialog, which) -> {
              try
              {
                int message = SinhalaLearner.clearAllLearnedData(this)
                    ? R.string.pref_clear_sinhala_learned_data_done
                    : R.string.pref_clear_sinhala_learned_data_failed;
                Toast.makeText(this, message,
                    message == R.string.pref_clear_sinhala_learned_data_done
                        ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
              }
              catch (Exception e)
              {
                Toast.makeText(this, R.string.pref_clear_sinhala_learned_data_failed,
                    Toast.LENGTH_LONG).show();
              }
            })
            .show();
        return true;
      });
    }
  }

  static boolean resetKeyboardSettings(SharedPreferences prefs, boolean preserveMigrationMarker)
  {
    if (prefs == null)
      return false;
    SharedPreferences.Editor editor = prefs.edit().clear();
    // Prevent legacy credential-protected preferences from being copied back after reset.
    if (preserveMigrationMarker)
      editor.putBoolean("need_migration", false);
    if (!editor.commit())
      return false;
    Config.migrate(prefs);
    return true;
  }

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

}
