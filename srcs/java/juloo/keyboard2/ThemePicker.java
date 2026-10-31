package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Categorized Gboard-inspired theme gallery with live keyboard previews, search, favorites, and explicit Apply action. */
final class ThemePicker
{
  public enum ThemeCategory
  {
    ALL,
    FAVORITES,
    SYSTEM,
    DARK,
    LIGHT,
    VIBRANT
  }

  private static final String PREF_FAVORITES = "favorite_themes";

  private ThemePicker() {}

  public static ThemeCategory categoryOf(String key)
  {
    if (key == null) return null;
    switch (key)
    {
      case "system":
      case "monet":
        return ThemeCategory.SYSTEM;
      case "dark":
      case "black":
      case "oled_black":
      case "graphite_pro":
      case "gruvbox_dark":
      case "solarized_dark":
      case "dracula":
      case "nord":
      case "tokyo_night":
      case "catppuccin_mocha":
      case "high_contrast":
        return ThemeCategory.DARK;
      case "light":
      case "white":
      case "gruvbox_light":
      case "catppuccin_latte":
        return ThemeCategory.LIGHT;
      case "sinhala_sand":
      case "lanka_maroon":
        return ThemeCategory.VIBRANT;
      default:
        return null;
    }
  }

  static final class PickerPalette
  {
    final int cardBackground;
    final int selectedCardBackground;
    final int cardBorder;
    final int primaryText;
    final int secondaryText;
    final int inputBackground;
    final int chipBackground;
    final int accent;
    final int accentText;

    PickerPalette(Context context)
    {
      TypedArray attrs = context.obtainStyledAttributes(new int[] {
        android.R.attr.colorBackground,
        android.R.attr.textColorPrimary,
        android.R.attr.textColorSecondary,
        android.R.attr.colorAccent
      });
      int surface = attrs.getColor(0, 0xFFFAFAFA);
      primaryText = attrs.getColor(1, 0xFF202124);
      secondaryText = attrs.getColor(2, 0xFF666666);
      accent = attrs.getColor(3, 0xFF4285F4);
      attrs.recycle();

      cardBackground = surface;
      selectedCardBackground = blend(surface, accent, 0.16f);
      cardBorder = blend(surface, primaryText, 0.18f);
      inputBackground = blend(surface, secondaryText, 0.12f);
      chipBackground = blend(surface, secondaryText, 0.16f);
      accentText = Theme.getContrastColor(accent);
    }
  }

  private static class CardHolder
  {
    final String value;
    final String entryName;
    final ThemeCategory category;
    final LinearLayout cardView;
    final GradientDrawable cardBg;
    final TextView titleView;
    final TextView starView;
    final TextView checkView;
    final List<GradientDrawable> keyBackgrounds = new ArrayList<>();

    CardHolder(String value, String entryName, ThemeCategory category,
        LinearLayout cardView, GradientDrawable cardBg,
        TextView titleView, TextView starView, TextView checkView)
    {
      this.value = value;
      this.entryName = entryName;
      this.category = category;
      this.cardView = cardView;
      this.cardBg = cardBg;
      this.titleView = titleView;
      this.starView = starView;
      this.checkView = checkView;
    }
  }

  static AlertDialog create(AlertDialog.Builder builder, Resources resources, SharedPreferences prefs)
  {
    final Context context = builder.getContext();
    final PickerPalette palette = new PickerPalette(context);
    final String[] values = (resources != null)
        ? resources.getStringArray(R.array.pref_theme_values) : new String[0];
    final String[] entries = (resources != null)
        ? resources.getStringArray(R.array.pref_theme_entries) : values;
    final String[] selected = { (prefs != null) ? prefs.getString("theme", Config.DEFAULT_THEME) : Config.DEFAULT_THEME };
    final boolean[] borders = { (prefs != null) && prefs.getBoolean("border_config", false) };
    final ThemeCategory[] activeCategory = { ThemeCategory.ALL };
    final String[] currentQuery = { "" };
    final Set<String> favorites = new HashSet<>();
    if (prefs != null)
    {
      Set<String> savedFavs = prefs.getStringSet(PREF_FAVORITES, null);
      if (savedFavs != null)
      {
        favorites.addAll(savedFavs);
      }
    }

    final Map<String, int[]> colorCache = new HashMap<>();

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));

    TextView hint = new TextView(context);
    hint.setText(getStringSafe(context, R.string.pref_theme_choose, "Choose a keyboard theme"));
    hint.setTextSize(14);
    hint.setTextColor(palette.secondaryText);
    root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

    // Search bar
    final EditText searchInput = new EditText(context);
    searchInput.setHint(getStringSafe(context, R.string.pref_theme_search_hint, "Search themes…"));
    searchInput.setSingleLine(true);
    searchInput.setTextSize(14);
    searchInput.setBackground(rounded(palette.inputBackground, dp(context, 8)));
    searchInput.setTextColor(palette.primaryText);
    searchInput.setHintTextColor(palette.secondaryText);
    searchInput.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
    LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, -2);
    searchLp.topMargin = dp(context, 6);
    searchLp.bottomMargin = dp(context, 6);
    root.addView(searchInput, searchLp);

    // Category filter chips
    HorizontalScrollView chipScroll = new HorizontalScrollView(context);
    chipScroll.setHorizontalScrollBarEnabled(false);
    LinearLayout chipRow = new LinearLayout(context);
    chipRow.setOrientation(LinearLayout.HORIZONTAL);
    chipRow.setPadding(0, dp(context, 2), 0, dp(context, 6));

    final ThemeCategory[] categories = {
      ThemeCategory.ALL,
      ThemeCategory.FAVORITES,
      ThemeCategory.SYSTEM,
      ThemeCategory.DARK,
      ThemeCategory.LIGHT,
      ThemeCategory.VIBRANT
    };

    final List<TextView> chipViews = new ArrayList<>();
    final List<CardHolder> cardHolders = new ArrayList<>();

    ScrollView scroll = new ScrollView(context);
    LinearLayout gallery = new LinearLayout(context);
    gallery.setOrientation(LinearLayout.VERTICAL);
    gallery.setPadding(0, dp(context, 4), 0, dp(context, 4));

    final TextView noResultsView = new TextView(context);
    noResultsView.setText(getStringSafe(context, R.string.pref_theme_no_results, "No matching themes found"));
    noResultsView.setTextSize(14);
    noResultsView.setGravity(Gravity.CENTER);
    noResultsView.setPadding(0, dp(context, 32), 0, dp(context, 32));
    noResultsView.setTextColor(palette.secondaryText);
    noResultsView.setVisibility(View.GONE);
    gallery.addView(noResultsView, new LinearLayout.LayoutParams(-1, -2));

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
        updateChipStyles(context, chipViews, categories, activeCategory[0], palette);
        filterCards(cardHolders, noResultsView, currentQuery[0], activeCategory[0], favorites);
      });
      chipViews.add(chip);
      chipRow.addView(chip);
    }
    updateChipStyles(context, chipViews, categories, activeCategory[0], palette);
    chipScroll.addView(chipRow);
    root.addView(chipScroll, new LinearLayout.LayoutParams(-1, -2));

    // Build theme cards once
    buildCards(context, gallery, values, entries, selected, borders[0], cardHolders,
        colorCache, favorites, palette, () -> {
          if (activeCategory[0] == ThemeCategory.FAVORITES)
          {
            filterCards(cardHolders, noResultsView, currentQuery[0], activeCategory[0], favorites);
          }
        });

    scroll.addView(gallery, new ScrollView.LayoutParams(-1, -2));
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

    // Search filter listener
    searchInput.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        currentQuery[0] = (s != null) ? s.toString().trim() : "";
        filterCards(cardHolders, noResultsView, currentQuery[0], activeCategory[0], favorites);
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    Switch borderSwitch = new Switch(context);
    borderSwitch.setText(getStringSafe(context, R.string.pref_theme_key_borders, "Key borders"));
    borderSwitch.setTextSize(15);
    borderSwitch.setChecked(borders[0]);
    borderSwitch.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 8));
    borderSwitch.setOnCheckedChangeListener((button, checked) -> {
      borders[0] = checked;
      int borderStroke = dp(context, 1);
      int borderColor = checked ? 0x66808080 : 0x00000000;
      for (CardHolder holder : cardHolders)
      {
        for (GradientDrawable keyBg : holder.keyBackgrounds)
        {
          keyBg.setStroke(borderStroke, borderColor);
        }
      }
    });
    root.addView(borderSwitch, new LinearLayout.LayoutParams(-1, -2));

    String applyLabel = getStringSafe(context, R.string.pref_theme_apply, "Apply");
    return builder.setTitle(R.string.pref_theme)
      .setView(root)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(applyLabel, (dialog, which) -> {
        if (prefs != null)
        {
          prefs.edit()
            .putString("theme", selected[0])
            .putBoolean("border_config", borders[0])
            .putStringSet(PREF_FAVORITES, new HashSet<>(favorites))
            .apply();
        }
      })
      .create();
  }

  private static void buildCards(Context context, LinearLayout gallery, String[] values,
      String[] entries, String[] selected, boolean borders, List<CardHolder> cardHolders,
      Map<String, int[]> colorCache, Set<String> favorites, PickerPalette palette,
      Runnable onFavoriteChanged)
  {
    if (values == null) return;
    for (int i = 0; i < values.length; i++)
    {
      final String value = values[i];
      final ThemeCategory cat = categoryOf(value);
      boolean chosen = value.equals(selected[0]);
      int[] colors = colorCache.get(value);
      if (colors == null)
      {
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
      GradientDrawable cardBg = rounded(chosen ? palette.selectedCardBackground : palette.cardBackground, dp(context, 12));
      cardBg.setStroke(dp(context, chosen ? 2 : 1), chosen ? palette.accent : palette.cardBorder);
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
      title.setTextColor(palette.primaryText);
      title.setTypeface(null, chosen ? Typeface.BOLD : Typeface.NORMAL);
      titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

      // Favorite toggle star
      final TextView star = new TextView(context);
      boolean isFav = favorites.contains(value);
      star.setText(isFav ? "★" : "☆");
      star.setTextSize(20);
      star.setTextColor(isFav ? 0xFFF9A825 : palette.secondaryText);
      star.setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2));
      star.setContentDescription(isFav
          ? getStringSafe(context, R.string.pref_theme_favorite_remove, "Remove from favorites")
          : getStringSafe(context, R.string.pref_theme_favorite_add, "Add to favorites"));
      star.setOnClickListener(v -> {
        if (favorites.contains(value))
        {
          favorites.remove(value);
          star.setText("☆");
          star.setTextColor(palette.secondaryText);
          star.setContentDescription(getStringSafe(context, R.string.pref_theme_favorite_add, "Add to favorites"));
        }
        else
        {
          favorites.add(value);
          star.setText("★");
          star.setTextColor(0xFFF9A825);
          star.setContentDescription(getStringSafe(context, R.string.pref_theme_favorite_remove, "Remove from favorites"));
        }
        if (onFavoriteChanged != null)
        {
          onFavoriteChanged.run();
        }
      });
      titleRow.addView(star, new LinearLayout.LayoutParams(-2, -2));

      TextView check = new TextView(context);
      check.setText(chosen ? "✓" : "");
      check.setTextSize(22);
      check.setTextColor(palette.accent);
      check.setGravity(Gravity.CENTER);
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

      CardHolder holder = new CardHolder(value, entryName, cat, card, cardBg, title, star, check);

      for (int ri = 0; ri < rows.length; ri++)
      {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = dp(context, 3);
        for (int ki = 0; ki < rows[ri].length; ki++)
        {
          boolean special = (ri == 2 && (ki == 0 || ki == rows[ri].length - 1)) ||
              (ri == 3 && (ki == 0 || ki == rows[ri].length - 1));
          TextView cap = new TextView(context);
          cap.setText(rows[ri][ki]);
          cap.setTextSize(ri == 3 && ki == 3 ? 9 : 11);
          cap.setSingleLine(true);
          cap.setGravity(Gravity.CENTER);
          cap.setTextColor(special ? Theme.getContrastColor(activeColor) : textColor);
          GradientDrawable keyBg = rounded(special ? activeColor : keyColor, dp(context, 4));
          if (borders)
          {
            keyBg.setStroke(dp(context, 1), 0x66808080);
          }
          cap.setBackground(keyBg);
          holder.keyBackgrounds.add(keyBg);
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
        String prev = selected[0];
        selected[0] = value;
        // In-place selection update: zero view allocations or recreation
        for (CardHolder h : cardHolders)
        {
          if (h.value.equals(prev))
          {
            h.cardBg.setColor(palette.cardBackground);
            h.cardBg.setStroke(dp(context, 1), palette.cardBorder);
            h.checkView.setText("");
            h.titleView.setTypeface(null, Typeface.NORMAL);
            h.cardView.setContentDescription(h.entryName);
          }
          else if (h.value.equals(value))
          {
            h.cardBg.setColor(palette.selectedCardBackground);
            h.cardBg.setStroke(dp(context, 2), palette.accent);
            h.checkView.setText("✓");
            h.titleView.setTypeface(null, Typeface.BOLD);
            h.cardView.setContentDescription(h.entryName + ", selected");
          }
        }
      });

      cardHolders.add(holder);
    }
  }

  private static void filterCards(List<CardHolder> cardHolders, TextView noResultsView,
      String query, ThemeCategory category, Set<String> favorites)
  {
    String q = (query != null) ? query.toLowerCase(Locale.ROOT) : "";
    int visibleCount = 0;
    for (CardHolder holder : cardHolders)
    {
      boolean categoryMatch = true;
      if (category == ThemeCategory.FAVORITES)
      {
        categoryMatch = favorites.contains(holder.value);
      }
      else if (category != ThemeCategory.ALL)
      {
        categoryMatch = (holder.category == category);
      }

      boolean queryMatch = true;
      if (!q.isEmpty())
      {
        queryMatch = holder.entryName.toLowerCase(Locale.ROOT).contains(q)
            || holder.value.toLowerCase(Locale.ROOT).contains(q);
      }

      boolean show = categoryMatch && queryMatch;
      holder.cardView.setVisibility(show ? View.VISIBLE : View.GONE);
      if (show)
      {
        visibleCount++;
      }
    }
    noResultsView.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
  }

  private static void updateChipStyles(Context context, List<TextView> chips,
      ThemeCategory[] categories, ThemeCategory active, PickerPalette palette)
  {
    for (int i = 0; i < chips.size() && i < categories.length; i++)
    {
      TextView chip = chips.get(i);
      boolean isSelected = (categories[i] == active);
      chip.setBackground(rounded(isSelected ? palette.accent : palette.chipBackground, dp(context, 16)));
      chip.setTextColor(isSelected ? palette.accentText : palette.primaryText);
      chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
    }
  }

  private static String getCategoryLabel(Context context, ThemeCategory cat)
  {
    if (context != null)
    {
      try
      {
        switch (cat)
        {
          case ALL:
            return getStringSafe(context, R.string.pref_theme_category_all, "All");
          case FAVORITES:
            return getStringSafe(context, R.string.pref_theme_category_favorites, "Favorites");
          case SYSTEM:
            return getStringSafe(context, R.string.pref_theme_category_system, "System");
          case DARK:
            return getStringSafe(context, R.string.pref_theme_category_dark, "Dark");
          case LIGHT:
            return getStringSafe(context, R.string.pref_theme_category_light, "Light");
          case VIBRANT:
            return getStringSafe(context, R.string.pref_theme_category_vibrant, "Vibrant");
        }
      }
      catch (Exception ignored) {}
    }
    switch (cat)
    {
      case ALL: return "All";
      case FAVORITES: return "Favorites";
      case SYSTEM: return "System";
      case DARK: return "Dark";
      case LIGHT: return "Light";
      case VIBRANT: return "Vibrant";
      default: return "";
    }
  }

  private static String getStringSafe(Context context, int resId, String fallback)
  {
    if (context != null)
    {
      try
      {
        String s = context.getString(resId);
        if (s != null) return s;
      }
      catch (Exception ignored) {}
    }
    return fallback;
  }

  private static int[] resolveThemeColors(Context context, String value)
  {
    Resources.Theme theme = null;
    if (context != null && context.getResources() != null)
    {
      theme = context.getResources().newTheme();
      if (theme != null)
      {
        theme.applyStyle(Config.getThemeId(context.getResources(), value), true);
      }
    }
    if (theme == null && context != null)
    {
      theme = context.getTheme();
    }
    TypedArray colors = theme != null ? theme.obtainStyledAttributes(R.styleable.keyboard) : null;
    int keyboardColor = 0xFF202020;
    int keyColor = 0xFF444444;
    int textColor = Color.WHITE;
    int activeColor = keyColor;
    if (colors != null)
    {
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
        || context.getResources().getDisplayMetrics() == null)
    {
      return (int)(value + 0.5f);
    }
    return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
  }

  private static int blend(int base, int overlay, float fraction)
  {
    float t = Math.max(0f, Math.min(1f, fraction));
    int red = Math.round(Color.red(base) * (1f - t) + Color.red(overlay) * t);
    int green = Math.round(Color.green(base) * (1f - t) + Color.green(overlay) * t);
    int blue = Math.round(Color.blue(base) * (1f - t) + Color.blue(overlay) * t);
    return Color.rgb(red, green, blue);
  }

  private static GradientDrawable rounded(int color, float radius)
  {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radius);
    return drawable;
  }
}