package juloo.keyboard2;

import android.app.AlertDialog;
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
                SinhalaLearner.clearAllLearnedData(this);
                Toast.makeText(this, R.string.pref_clear_sinhala_learned_data_done,
                    Toast.LENGTH_SHORT).show();
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

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

}
