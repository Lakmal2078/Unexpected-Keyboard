package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.view.View;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ThemePickerTest
{
  private Resources resources;
  private Context context;
  private AlertDialog.Builder builder;
  private SharedPreferences prefs;
  private SharedPreferences.Editor editor;
  private DialogInterface.OnClickListener apply;
  private AlertDialog dialog;

  @Before
  public void setUp()
  {
    resources = mock(Resources.class);
    context = mock(Context.class);
    when(resources.getStringArray(R.array.pref_theme_values))
      .thenReturn(new String[]{ "dark", "graphite_pro", "light" });
    when(resources.getStringArray(R.array.pref_theme_entries))
      .thenReturn(new String[]{ "Dark", "Graphite Pro", "Light" });
    prefs = mock(SharedPreferences.class);
    when(prefs.getString("theme", Config.DEFAULT_THEME)).thenReturn("graphite_pro");
    when(prefs.getBoolean("border_config", false)).thenReturn(false);
    editor = mock(SharedPreferences.Editor.class, RETURNS_SELF);
    when(prefs.edit()).thenReturn(editor);
    dialog = mock(AlertDialog.class);
    builder = mock(AlertDialog.Builder.class, RETURNS_SELF);
    when(builder.getContext()).thenReturn(context);
    when(context.getResources()).thenReturn(resources);
    when(resources.getDisplayMetrics()).thenReturn(new DisplayMetrics());
    Resources.Theme theme = mock(Resources.Theme.class);
    TypedArray colors = mock(TypedArray.class);
    when(resources.newTheme()).thenReturn(theme);
    when(context.getTheme()).thenReturn(theme);
    when(theme.obtainStyledAttributes(any(int[].class))).thenReturn(colors);
    when(builder.create()).thenReturn(dialog);
    when(context.getString(R.string.pref_theme_apply)).thenReturn("Apply");
    when(context.getString(R.string.pref_theme_choose)).thenReturn("Choose a keyboard theme");
    when(context.getString(R.string.pref_theme_key_borders)).thenReturn("Key borders");
    when(context.getString(R.string.pref_theme_search_hint)).thenReturn("Search themes…");
    when(context.getString(R.string.pref_theme_no_results)).thenReturn("No matching themes found");
    when(context.getString(R.string.pref_theme_category_all)).thenReturn("All");
    when(context.getString(R.string.pref_theme_category_favorites)).thenReturn("Favorites");
    when(context.getString(R.string.pref_theme_category_system)).thenReturn("System");
    when(context.getString(R.string.pref_theme_category_dark)).thenReturn("Dark");
    when(context.getString(R.string.pref_theme_category_light)).thenReturn("Light");
    when(context.getString(R.string.pref_theme_category_vibrant)).thenReturn("Vibrant");
    when(prefs.getStringSet("favorite_themes", null)).thenReturn(null);
    when(editor.putStringSet(eq("favorite_themes"), any())).thenReturn(editor);
  }

  @Test
  public void openingAndDismissingPickerNeverWritesPreferences()
  {
    openPicker();
    dialog.dismiss();
    verify(prefs, never()).edit();
    verifyNoInteractions(editor);
  }

  @Test
  public void applyingPickerPersistsCurrentThemeAndBorderSetting()
  {
    openPicker();
    verify(prefs, never()).edit();

    apply.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

    verify(editor).putString("theme", "graphite_pro");
    verify(editor).putBoolean("border_config", false);
    verify(editor).putStringSet(eq("favorite_themes"), any());
    verify(editor).apply();
  }

  private void openPicker()
  {
    assertSame(dialog, ThemePicker.create(builder, resources, prefs));
    ArgumentCaptor<View> view = ArgumentCaptor.forClass(View.class);
    verify(builder).setView(view.capture());
    assertNotNull(view.getValue());

    ArgumentCaptor<DialogInterface.OnClickListener> listener =
      ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
    verify(builder).setPositiveButton(eq("Apply"), listener.capture());
    apply = listener.getValue();
    verify(builder).setNegativeButton(android.R.string.cancel, null);
  }
}
