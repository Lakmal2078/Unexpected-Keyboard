package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Gboard-inspired theme gallery with keyboard previews and an explicit Apply action. */
final class ThemePicker
{
  private ThemePicker() {}

  static AlertDialog create(AlertDialog.Builder builder, Resources resources, SharedPreferences prefs)
  {
    final Context context = builder.getContext();
    final String[] values = resources.getStringArray(R.array.pref_theme_values);
    final String[] entries = resources.getStringArray(R.array.pref_theme_entries);
    final String[] selected = { prefs.getString("theme", Config.DEFAULT_THEME) };
    final boolean[] borders = { prefs.getBoolean("border_config", false) };

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));
    TextView hint = new TextView(context);
    hint.setText("Choose a keyboard theme");
    hint.setTextSize(14);
    hint.setTextColor(0xFF777777);
    root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

    ScrollView scroll = new ScrollView(context);
    LinearLayout gallery = new LinearLayout(context);
    gallery.setOrientation(LinearLayout.VERTICAL);
    gallery.setPadding(0, dp(context, 8), 0, dp(context, 8));
    scroll.addView(gallery, new ScrollView.LayoutParams(-1, -2));
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    Switch borderSwitch = new Switch(context);
    borderSwitch.setText("Key borders");
    borderSwitch.setTextSize(15);
    borderSwitch.setChecked(borders[0]);
    borderSwitch.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 8));
    borderSwitch.setOnCheckedChangeListener((button, checked) -> {
      borders[0] = checked;
      renderGallery(context, gallery, values, entries, selected, borders[0]);
    });
    root.addView(borderSwitch, new LinearLayout.LayoutParams(-1, -2));

    renderGallery(context, gallery, values, entries, selected, borders[0]);
    return builder.setTitle(R.string.pref_theme)
      .setView(root)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton("Apply", (dialog, which) -> prefs.edit()
        .putString("theme", selected[0])
        .putBoolean("border_config", borders[0])
        .apply())
      .create();
  }

  private static void renderGallery(Context context, LinearLayout gallery, String[] values,
      String[] entries, String[] selected, boolean borders)
  {
    gallery.removeAllViews();
    for (int i = 0; i < values.length; i++) {
      final String value = values[i];
      boolean chosen = value.equals(selected[0]);
      ContextThemeWrapper themed = new ContextThemeWrapper(context,
          Config.getThemeId(context.getResources(), value));
      TypedArray colors = themed.obtainStyledAttributes(R.styleable.keyboard);
      int keyboardColor = colors.getColor(R.styleable.keyboard_colorKeyboard, 0xFF202020);
      int keyColor = colors.getColor(R.styleable.keyboard_colorKey, 0xFF444444);
      int textColor = colors.getColor(R.styleable.keyboard_colorLabel, Color.WHITE);
      int activeColor = colors.getColor(R.styleable.keyboard_colorKeyActivated, keyColor);
      colors.recycle();

      LinearLayout card = new LinearLayout(context);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(dp(context, 10), dp(context, 9), dp(context, 10), dp(context, 9));
      GradientDrawable cardBg = rounded(chosen ? 0xFFE8F0FE : 0xFFF7F7F7, dp(context, 12));
      cardBg.setStroke(dp(context, chosen ? 2 : 1), chosen ? 0xFF4285F4 : 0xFFE0E0E0);
      card.setBackground(cardBg);
      LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
      cardLp.bottomMargin = dp(context, 8);
      gallery.addView(card, cardLp);

      LinearLayout titleRow = new LinearLayout(context);
      titleRow.setGravity(Gravity.CENTER_VERTICAL);
      TextView title = new TextView(context);
      title.setText(entries[i]);
      title.setTextSize(15);
      title.setTextColor(0xFF202124);
      title.setTypeface(null, chosen ? Typeface.BOLD : Typeface.NORMAL);
      titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
      TextView check = new TextView(context);
      check.setText(chosen ? "✓" : "");
      check.setTextSize(22);
      check.setTextColor(0xFF1A73E8);
      titleRow.addView(check, new LinearLayout.LayoutParams(dp(context, 30), -2));
      card.addView(titleRow);

      LinearLayout keyboard = new LinearLayout(context);
      keyboard.setOrientation(LinearLayout.VERTICAL);
      keyboard.setPadding(dp(context, 5), dp(context, 6), dp(context, 5), dp(context, 6));
      keyboard.setBackground(rounded(keyboardColor, dp(context, 8)));
      String[][] rows = {
        { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" },
        { "a", "s", "d", "f", "g", "h", "j", "k", "l" },
        { "⇧", "z", "x", "c", "v", "b", "n", "m", "⌫" },
        { "?123", ",", "☻", "English", ".", "↵" }
      };
      for (int ri = 0; ri < rows.length; ri++) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = dp(context, 3);
        for (int ki = 0; ki < rows[ri].length; ki++) {
          boolean special = (ri == 2 && (ki == 0 || ki == rows[ri].length - 1)) ||
              (ri == 3 && (ki == 0 || ki == rows[ri].length - 1));
          TextView cap = new TextView(context);
          cap.setText(rows[ri][ki]);
          cap.setTextSize(ri == 3 && ki == 3 ? 9 : 11);
          cap.setSingleLine(true);
          cap.setGravity(Gravity.CENTER);
          cap.setTextColor(special ? Theme.getContrastColor(activeColor) : textColor);
          GradientDrawable keyBg = rounded(special ? activeColor : keyColor, dp(context, 4));
          if (borders) keyBg.setStroke(dp(context, 1), 0x66808080);
          cap.setBackground(keyBg);
          cap.setPadding(dp(context, 1), 0, dp(context, 1), 0);
          LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(0, dp(context, ri == 3 ? 25 : 27), 1);
          keyLp.setMargins(dp(context, 1), 0, dp(context, 1), 0);
          row.addView(cap, keyLp);
        }
        keyboard.addView(row, rowLp);
      }
      card.addView(keyboard);
      card.setContentDescription(entries[i] + (chosen ? ", selected" : ""));
      card.setOnClickListener(v -> {
        selected[0] = value;
        renderGallery(context, gallery, values, entries, selected, borders);
      });
    }
  }

  private static int dp(Context context, float value)
  {
    return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
  }

  private static GradientDrawable rounded(int color, float radius)
  {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    return drawable;
  }
}