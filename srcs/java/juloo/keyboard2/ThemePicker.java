package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

/** Theme chooser with a small, accessible keycap preview for every theme. */
final class ThemePicker
{
  private ThemePicker() {}

  static AlertDialog create(AlertDialog.Builder builder, Resources resources, SharedPreferences prefs)
  {
    Context context = builder.getContext();
    String[] values = resources.getStringArray(R.array.pref_theme_values);
    String current = prefs.getString("theme", Config.DEFAULT_THEME);
    ThemeAdapter adapter = new ThemeAdapter(context, resources, values, current);
    return builder.setTitle(R.string.pref_theme)
      .setAdapter(adapter, (dialog, which) -> {
        if (!values[which].equals(current))
          prefs.edit().putString("theme", values[which]).apply();
        dialog.dismiss();
      })
      .setNegativeButton(android.R.string.cancel, null)
      .create();
  }

  private static final class ThemeAdapter extends BaseAdapter
  {
    private final Context context;
    private final String[] values;
    private final String[] entries;
    private final String selected;

    ThemeAdapter(Context context, Resources resources, String[] values, String selected)
    {
      this.context = context;
      this.values = values;
      this.entries = resources.getStringArray(R.array.pref_theme_entries);
      this.selected = selected;
    }

    @Override public int getCount() { return values.length; }
    @Override public Object getItem(int position) { return values[position]; }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent)
    {
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(LinearLayout.VERTICAL);
      row.setPadding(dp(20), dp(8), dp(20), dp(8));
      RadioButton label = new RadioButton(context);
      label.setText(entries[position]);
      label.setTextSize(16);
      label.setChecked(values[position].equals(selected));
      label.setClickable(false);
      label.setFocusable(false);
      row.addView(label, new LinearLayout.LayoutParams(-1, -2));

      ContextThemeWrapper themed = new ContextThemeWrapper(context, Config.getThemeId(context.getResources(), values[position]));
      TypedArray colors = themed.obtainStyledAttributes(R.styleable.keyboard);
      int background = colors.getColor(R.styleable.keyboard_colorKeyboard, 0xFF202020);
      int key = colors.getColor(R.styleable.keyboard_colorKey, 0xFF444444);
      int text = colors.getColor(R.styleable.keyboard_colorLabel, 0xFFFFFFFF);
      int active = colors.getColor(R.styleable.keyboard_colorKeyActivated, key);
      colors.recycle();

      LinearLayout preview = new LinearLayout(context);
      preview.setOrientation(LinearLayout.HORIZONTAL);
      preview.setPadding(dp(8), dp(5), dp(8), dp(5));
      preview.setBackground(round(background, dp(8)));
      int[] swatches = { key, active, key };
      String[] samples = { "ABC", "123", "↵" };
      for (int i = 0; i < swatches.length; i++)
      {
        TextView cap = new TextView(context);
        cap.setText(samples[i]);
        cap.setTextColor(i == 1 ? Theme.getContrastColor(active) : text);
        cap.setTextSize(12);
        cap.setGravity(17);
        cap.setBackground(round(swatches[i], dp(5)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(30), 1);
        lp.setMargins(dp(2), 0, dp(2), 0);
        preview.addView(cap, lp);
      }
      preview.setContentDescription(entries[position] + " sample key colors");
      row.addView(preview, new LinearLayout.LayoutParams(-1, -2));
      row.setContentDescription(entries[position] + ", theme preview");
      return row;
    }

    private int dp(float value) { return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable round(int color, float radius)
    {
      GradientDrawable drawable = new GradientDrawable();
      drawable.setColor(color);
      drawable.setCornerRadius(radius);
      return drawable;
    }
  }
}
