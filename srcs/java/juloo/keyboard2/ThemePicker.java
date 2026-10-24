package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Categorized Gboard-inspired theme gallery with live keyboard previews and explicit Apply action. */
final class ThemePicker
{
  public enum ThemeCategory
  {
    ALL,
    SYSTEM,
    DARK,
    LIGHT,
    VIBRANT
  }

  private ThemePicker() {}

  public static ThemeCategory categoryOf(String key)
  {
    if (key == null)
      return null;
    switch (key)
    {
      case "system":
      case "monet":
      case "monetlight":
      case "monetdark":
        return ThemeCategory.SYSTEM;

      case "dark":
      case "black":
      case "oled_black":
      case "graphite_pro":
      case "cobalt":
      case "dracula":
      case "nord":
      case "tokyo_night":
      case "catppuccin_mocha":
      case "gruvbox_dark":
      case "solarized_dark":
      case "rosepine":
      case "quiet_teal":
      case "midnight_purple":
      case "midnight_ocean":
      case "matrix_green":
      case "sunset_glow":
      case "unexpected_keyboard":
      case "copper_night":
      case "berry_noir":
      case "aurora_flow":
      case "pixel_retro":
      case "high_contrast":
      case "high_contrast_yellow":
        return ThemeCategory.DARK;

      case "light":
      case "white":
      case "gruvbox_light":
      case "catppuccin_latte":
      case "everforestlight":
      case "sepia_warm":
      case "zen_minimal":
      case "arctic_blue":
      case "soft_lavender":
      case "rose_quartz":
      case "ocean_breeze":
      case "citrus_pop":
      case "mint_matcha":
      case "glass":
      case "soft_3d":
        return ThemeCategory.LIGHT;

      case "lanka_maroon":
      case "lanka_blue":
      case "royal_gold":
      case "sinhala_sand":
      case "emerald_forest":
      case "cyberpunk_neon":
      case "desert":
      case "jungle":
        return ThemeCategory.VIBRANT;

      default:
        return null;
    }
  }

  static AlertDialog create(AlertDialog.Builder builder, Resources resources, SharedPreferences prefs)
  {
    final Context context = builder.getContext();
    final String[] values = resources.getStringArray(R.array.pref_theme_values);
    final String[] entries = resources.getStringArray(R.array.pref_theme_entries);
    final String[] selected = { prefs.getString("theme", Config.DEFAULT_THEME) };
    final boolean[] borders = { prefs.getBoolean("border_config", false) };
    final ThemeCategory[] activeCategory = { ThemeCategory.ALL };
    final Map<String, int[]> colorCache = new HashMap<>();

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));

    TextView hint = new TextView(context);
    hint.setText("Choose a keyboard theme");
    hint.setTextSize(14);
    hint.setTextColor(0xFF777777);
    root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

    // Category filter chips
    HorizontalScrollView chipScroll = new HorizontalScrollView(context);
    chipScroll.setHorizontalScrollBarEnabled(false);
    LinearLayout chipRow = new LinearLayout(context);
    chipRow.setOrientation(LinearLayout.HORIZONTAL);
    chipRow.setPadding(0, dp(context, 6), 0, dp(context, 6));

    final ThemeCategory[] categories = {
      ThemeCategory.ALL,
      ThemeCategory.SYSTEM,
      ThemeCategory.DARK,
      ThemeCategory.LIGHT,
      ThemeCategory.VIBRANT
    };

    final List<TextView> chipViews = new ArrayList<>();
    ScrollView scroll = new ScrollView(context);
    LinearLayout gallery = new LinearLayout(context);
    gallery.setOrientation(LinearLayout.VERTICAL);
    gallery.setPadding(0, dp(context, 8), 0, dp(context, 8));

    for (final ThemeCategory cat : categories)
    {
      final TextView chip = new TextView(context);
      chip.setText(getCategoryLabel(context, cat));
      chip.setTextSize(13);
      chip.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
      LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-2, -2);
      chipLp.rightMargin = dp(context, 6);
      chip.setLayoutParams(chipLp);
      chip.setContentDescription(getCategoryLabel(context, cat));
      chip.setOnClickListener(v -> {
        activeCategory[0] = cat;
        updateChipStyles(context, chipViews, categories, activeCategory[0]);
        renderGallery(context, gallery, values, entries, selected, borders[0], activeCategory[0], colorCache);
      });
      chipViews.add(chip);
      chipRow.addView(chip);
    }
    updateChipStyles(context, chipViews, categories, activeCategory[0]);
    chipScroll.addView(chipRow);
    root.addView(chipScroll, new LinearLayout.LayoutParams(-1, -2));

    scroll.addView(gallery, new ScrollView.LayoutParams(-1, -2));
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    Switch borderSwitch = new Switch(context);
    borderSwitch.setText("Key borders");
    borderSwitch.setTextSize(15);
    borderSwitch.setChecked(borders[0]);
    borderSwitch.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 8));
    borderSwitch.setOnCheckedChangeListener((button, checked) -> {
      borders[0] = checked;
      renderGallery(context, gallery, values, entries, selected, borders[0], activeCategory[0], colorCache);
    });
    root.addView(borderSwitch, new LinearLayout.LayoutParams(-1, -2));

    renderGallery(context, gallery, values, entries, selected, borders[0], activeCategory[0], colorCache);

    return builder.setTitle(R.string.pref_theme)
      .setView(root)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton("Apply", (dialog, which) -> prefs.edit()
        .putString("theme", selected[0])
        .putBoolean("border_config", borders[0])
        .apply())
      .create();
  }

  private static void updateChipStyles(Context context, List<TextView> chips,
      ThemeCategory[] categories, ThemeCategory active)
  {
    for (int i = 0; i < chips.size() && i < categories.length; i++)
    {
      TextView chip = chips.get(i);
      boolean isSelected = (categories[i] == active);
      chip.setBackground(rounded(isSelected ? 0xFF1A73E8 : 0xFFF1F3F4, dp(context, 16)));
      chip.setTextColor(isSelected ? Color.WHITE : 0xFF3C4043);
      chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
    }
  }

  private static String getCategoryLabel(Context context, ThemeCategory cat)
  {
    if (context != null && context.getResources() != null)
    {
      try
      {
        switch (cat)
        {
          case ALL:
            String allStr = context.getString(R.string.pref_theme_category_all);
            if (allStr != null) return allStr;
            break;
          case SYSTEM:
            String sysStr = context.getString(R.string.pref_theme_category_system);
            if (sysStr != null) return sysStr;
            break;
          case DARK:
            String darkStr = context.getString(R.string.pref_theme_category_dark);
            if (darkStr != null) return darkStr;
            break;
          case LIGHT:
            String lightStr = context.getString(R.string.pref_theme_category_light);
            if (lightStr != null) return lightStr;
            break;
          case VIBRANT:
            String vibStr = context.getString(R.string.pref_theme_category_vibrant);
            if (vibStr != null) return vibStr;
            break;
        }
      }
      catch (Exception ignored) {}
    }
    switch (cat)
    {
      case ALL: return "All";
      case SYSTEM: return "System";
      case DARK: return "Dark";
      case LIGHT: return "Light";
      case VIBRANT: return "Vibrant";
      default: return "";
    }
  }

  private static void renderGallery(Context context, LinearLayout gallery, String[] values,
      String[] entries, String[] selected, boolean borders, ThemeCategory filter,
      Map<String, int[]> colorCache)
  {
    gallery.removeAllViews();
    for (int i = 0; i < values.length; i++) {
      final String value = values[i];
      final ThemeCategory cat = categoryOf(value);
      if (filter != ThemeCategory.ALL && cat != filter) {
        continue;
      }

      boolean chosen = value.equals(selected[0]);
      int[] colors = colorCache.get(value);
      if (colors == null) {
        colors = resolveThemeColors(context, value);
        colorCache.put(value, colors);
      }
      int keyboardColor = colors[0];
      int keyColor = colors[1];
      int textColor = colors[2];
      int activeColor = colors[3];

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
      String entryName = (entries != null && i < entries.length && entries[i] != null) ? entries[i] : value;
      title.setText(entryName);
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
      card.setContentDescription(entryName + (chosen ? ", selected" : ""));
      card.setOnClickListener(v -> {
        selected[0] = value;
        renderGallery(context, gallery, values, entries, selected, borders, filter, colorCache);
      });
    }
  }

  private static int[] resolveThemeColors(Context context, String value)
  {
    Resources.Theme theme = null;
    if (context.getResources() != null) {
      theme = context.getResources().newTheme();
      if (theme != null) {
        theme.applyStyle(Config.getThemeId(context.getResources(), value), true);
      }
    }
    if (theme == null) {
      theme = context.getTheme();
    }
    TypedArray colors = theme != null ? theme.obtainStyledAttributes(R.styleable.keyboard) : null;
    int keyboardColor = 0xFF202020;
    int keyColor = 0xFF444444;
    int textColor = Color.WHITE;
    int activeColor = keyColor;
    if (colors != null) {
      keyboardColor = colors.getColor(R.styleable.keyboard_colorKeyboard, 0xFF202020);
      keyColor = colors.getColor(R.styleable.keyboard_colorKey, 0xFF444444);
      textColor = colors.getColor(R.styleable.keyboard_colorLabel, Color.WHITE);
      activeColor = colors.getColor(R.styleable.keyboard_colorKeyActivated, keyColor);
      colors.recycle();
    }
    return new int[] { keyboardColor, keyColor, textColor, activeColor };
  }

  private static int dp(Context context, float value)
  {
    if (context == null || context.getResources() == null
        || context.getResources().getDisplayMetrics() == null) {
      return (int)(value + 0.5f);
    }
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