package juloo.keyboard2;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

// Keyboard2View initializes framework graphics objects, so these tests require
// Android rather than the throwing android.jar stubs used by local unit tests.
@RunWith(AndroidJUnit4.class)
public class KeyboardAccessibilityTest
{
  @Test
  public void characterKeyUsesVisibleLabel()
  {
    KeyboardData.Key key = new KeyboardData.Key(
        new KeyValue[]{KeyValue.makeCharKey('a'), null, null, null, null, null, null, null, null},
        null, 0, 1f, 0f, null, KeyboardData.Key.Role.Normal);
    assertEquals("a", Keyboard2View.accessibilityLabelForKey(key));
  }

  @Test
  public void editingKeysUseMeaningfulLabels()
  {
    KeyboardData.Key backspace = new KeyboardData.Key(
        new KeyValue[]{KeyValue.getKeyByName("backspace"), null, null, null, null, null, null, null, null},
        null, 0, 1f, 0f, null, KeyboardData.Key.Role.Action);
    assertEquals("Backspace", Keyboard2View.accessibilityLabelForKey(backspace));
  }

  @Test
  public void missingKeyHasNonEmptyFallbackLabel()
  {
    KeyboardData.Key empty = new KeyboardData.Key(
        new KeyValue[9], null, 0, 1f, 0f, null, KeyboardData.Key.Role.Normal);
    assertEquals("Empty key", Keyboard2View.accessibilityLabelForKey(empty));
    assertEquals("Keyboard key", Keyboard2View.accessibilityLabelForKey(null));
  }

  @Test
  public void everyEditingActionHasASpokenLabel()
  {
    String[][] labels = {
      {"backspace", "Backspace"},
      {"forward_delete_word", "Forward delete word"},
      {"delete_word", "Delete word"},
      {"space", "Space"},
      {"copy", "Copy"},
      {"cut", "Cut"},
      {"paste", "Paste"},
      {"pasteAsPlainText", "Paste as plain text"},
      {"selectAll", "Select all"},
      {"undo", "Undo"},
      {"redo", "Redo"},
      {"replaceText", "Replace"},
      {"shareText", "Share"},
      {"textAssist", "Assist"},
      {"autofill", "Autofill"},
      {"selection_cancel", "Selection cancel"}
    };
    assertNamedLabels(labels);
  }

  @Test
  public void modifiersHaveSpokenNamesInsteadOfFontGlyphs()
  {
    assertNamedLabels(new String[][] {
      {"shift", "Shift"},
      {"ctrl", "Control"},
      {"alt", "Alt"},
      {"meta", "Meta"},
      {"fn", "Function"},
      {"accent_dot_above", "Dot above"},
      {"accent_double_grave", "Double grave"},
      {"accent_small_caps", "Small caps"}
    });
  }

  @Test
  public void keyboardEventsHaveSpokenNames()
  {
    assertNamedLabels(new String[][] {
      {"hide_self", "Hide keyboard"},
      {"change_method", "Change keyboard"},
      {"switch_text", "Switch to text layout"},
      {"switch_numeric", "Switch to numeric layout"},
      {"switch_emoji", "Open emoji keyboard"},
      {"switch_clipboard", "Open clipboard history"},
      {"voice_typing", "Voice typing"},
      {"switch_back_clipboard", "Switch back clipboard"},
      {"voice_typing_chooser", "Switch voice typing chooser"},
      {"capslock", "Caps lock"}
    });
  }

  @Test
  public void labelPrefersCenterOverDirectionalValuesAndIndication()
  {
    KeyValue[] values = new KeyValue[9];
    values[0] = KeyValue.makeCharKey('a');
    values[1] = KeyValue.getKeyByName("backspace");
    KeyboardData.Key key = new KeyboardData.Key(values, null, 0, 1f, 0f,
        "Decorative indication", KeyboardData.Key.Role.Normal);

    assertEquals("a", Keyboard2View.accessibilityLabelForKey(key));
  }

  @Test
  public void everyDirectionalSlotCanSupplyTheFallbackLabel()
  {
    for (int index = 1; index < 9; index++)
    {
      KeyValue[] values = new KeyValue[9];
      values[index] = KeyValue.getKeyByName("paste");
      assertEquals("Directional slot " + index, "Paste",
          Keyboard2View.accessibilityLabelForKey(key(values)));
    }
  }

  @Test
  public void fallbackUsesFirstDefinedDirectionalValue()
  {
    KeyValue[] values = new KeyValue[9];
    values[3] = KeyValue.makeCharKey('b');
    values[8] = KeyValue.getKeyByName("backspace");

    assertEquals("b", Keyboard2View.accessibilityLabelForKey(key(values)));
  }

  @Test
  public void unicodeStringsAndCustomVisibleLabelsArePreserved()
  {
    assertLabel(KeyValue.makeStringKey("සිංහල"), "සිංහල");
    assertLabel(KeyValue.makeStringKey("👩‍💻"), "👩‍💻");
    assertLabel(KeyValue.makeCharKey('x', "Custom label", 0), "Custom label");
  }

  @Test
  public void emptySymbolsFallBackToHumanizedKeyKind()
  {
    assertLabel(KeyValue.makeStringKey(""), "String");
    assertLabel(KeyValue.getKeyByName("removed"), "Placeholder");
    assertLabel(KeyValue.makeComposePending("", 0, 0), "Compose pending");
  }

  @Test
  public void humanizedLabelsDoNotDependOnDeviceLocale()
  {
    Locale previous = Locale.getDefault();
    try
    {
      Locale.setDefault(new Locale("tr", "TR"));
      assertNamedLabels(new String[][] {
        {"switch_back_clipboard", "Switch back clipboard"},
        {"accent_circonflexe", "Circonflexe"}
      });
    }
    finally
    {
      Locale.setDefault(previous);
    }
  }

  private static void assertNamedLabels(String[][] labels)
  {
    for (String[] entry : labels)
    {
      assertEquals("Label for " + entry[0], entry[1],
          Keyboard2View.accessibilityLabelForKey(
              key(KeyValue.getKeyByName(entry[0]))));
    }
  }

  private static void assertLabel(KeyValue value, String expected)
  {
    assertEquals(expected, Keyboard2View.accessibilityLabelForKey(key(value)));
  }

  private static KeyboardData.Key key(KeyValue... values)
  {
    KeyValue[] slots = new KeyValue[9];
    System.arraycopy(values, 0, slots, 0, values.length);
    return new KeyboardData.Key(slots, null, 0, 1f, 0f, null,
        KeyboardData.Key.Role.Normal);
  }
}
