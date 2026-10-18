package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

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
}
