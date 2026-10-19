package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import java.util.Arrays;

/** The toolbar opens a chooser; only an explicit selection changes the theme. */
final class ThemePicker
{
  private ThemePicker() {}

  static AlertDialog create(AlertDialog.Builder builder, Resources resources, SharedPreferences prefs)
  {
    String[] values = resources.getStringArray(R.array.pref_theme_values);
    String current = prefs.getString("theme", Config.DEFAULT_THEME);
    int checked = Arrays.asList(values).indexOf(current);
    return builder
      .setTitle(R.string.pref_theme)
      .setSingleChoiceItems(R.array.pref_theme_entries, checked, (dialog, which) -> {
        if (!values[which].equals(current))
          prefs.edit().putString("theme", values[which]).apply();
        dialog.dismiss();
      })
      .setNegativeButton(android.R.string.cancel, null)
      .create();
  }
}
