package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ThemePickerTest
{
  private Resources resources;
  private AlertDialog.Builder builder;
  private SharedPreferences prefs;
  private SharedPreferences.Editor editor;
  private DialogInterface.OnClickListener select;
  private AlertDialog dialog;

  @Before
  public void setUp()
  {
    resources = mock(Resources.class);
    when(resources.getStringArray(R.array.pref_theme_values))
      .thenReturn(new String[]{ "dark", "graphite_pro", "light" });
    when(resources.getStringArray(R.array.pref_theme_entries))
      .thenReturn(new String[]{ "Dark", "Graphite Pro", "Light" });
    prefs = mock(SharedPreferences.class);
    when(prefs.getString("theme", Config.DEFAULT_THEME)).thenReturn("graphite_pro");
    editor = mock(SharedPreferences.Editor.class, RETURNS_SELF);
    when(prefs.edit()).thenReturn(editor);
    dialog = mock(AlertDialog.class);
    builder = mock(AlertDialog.Builder.class, RETURNS_SELF);
    when(builder.create()).thenReturn(dialog);
  }

  @Test
  public void openingAndDismissingPickerNeverWritesTheTheme()
  {
    openPicker(1);
    dialog.dismiss();
    verify(prefs, never()).edit();
    verifyNoInteractions(editor);
  }

  @Test
  public void selectingThemePersistsOnlyTheChosenValue()
  {
    openPicker(1);
    verify(prefs, never()).edit();
    select.onClick(dialog, 2);
    verify(editor).putString("theme", "light");
    verify(editor).apply();
    verifyNoMoreInteractions(editor);
    verify(dialog).dismiss();
  }

  @Test
  public void selectingCurrentThemeDoesNotRewritePreferences()
  {
    openPicker(1);
    select.onClick(dialog, 1);
    verify(prefs, never()).edit();
    verifyNoInteractions(editor);
    verify(dialog).dismiss();
  }

  @Test
  public void legacyThemeRemainsUnchangedUntilASelectionIsMade()
  {
    when(prefs.getString("theme", Config.DEFAULT_THEME)).thenReturn("forest_mist");
    openPicker(-1);
    verify(prefs, never()).edit();
    select.onClick(dialog, 0);
    verify(editor).putString("theme", "dark");
    verify(editor).apply();
  }

  private void openPicker(int checked)
  {
    assertSame(dialog, ThemePicker.create(builder, resources, prefs));
    ArgumentCaptor<DialogInterface.OnClickListener> listener =
      ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
    ArgumentCaptor<android.widget.ListAdapter> adapter =
      ArgumentCaptor.forClass(android.widget.ListAdapter.class);
    verify(builder).setAdapter(adapter.capture(), listener.capture());
    assertEquals(3, adapter.getValue().getCount());
    assertEquals("dark", adapter.getValue().getItem(0));
    verify(builder).setNegativeButton(android.R.string.cancel, null);
    select = listener.getValue();
  }
}
