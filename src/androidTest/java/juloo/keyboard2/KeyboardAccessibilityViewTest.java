package juloo.keyboard2;

import android.content.Context;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class KeyboardAccessibilityViewTest
{
  private Config previousConfig;
  private Modmap previousModmap;
  private Config config;
  private RecordingHandler handler;
  private Keyboard2View view;

  @Before
  public void setUp()
  {
    onMain(() -> {
      previousConfig = Config.globalConfig();
      previousModmap = (Modmap)getStaticField(KeyModifier.class, "_modmap");
      config = new Config();
      config.keyboard_rows_height_pixels = 40;
      config.screenHeightPixels = 2000;
      config.horizontal_margin = 7.25f;
      config.key_horizontal_margin = 0.1f;
      config.key_vertical_margin = 0.1f;
      config.longPressTimeout = 10000;
      // Suppress hardware haptics during activation tests.
      config.vibrate_custom = true;
      config.vibrate_duration = 0;
      handler = new RecordingHandler();
      config.handler = handler;
      setStaticField(Config.class, "_globalConfig", config);
      Context context = new ContextThemeWrapper(
          ApplicationProvider.getApplicationContext(), R.style.Dark);
      view = new Keyboard2View(context, null) {
        @Override
        public void refresh_navigation_bar(Context context)
        {
          // The test view has no InputMethodService window to recolor.
        }
      };
    });
  }

  @After
  public void tearDown()
  {
    onMain(() -> {
      try
      {
        if (view != null)
          view.reset();
      }
      finally
      {
        KeyModifier.set_modmap(previousModmap);
        setStaticField(Config.class, "_globalConfig", previousConfig);
      }
    });
  }

  @Test
  public void missingKeyboardExposesNoKeysOrBounds()
  {
    onMain(() -> {
      assertEquals(0, view.accessibilityKeyCount());
      assertNull(view.accessibilityKeyAt(0));
      assertNull(view.accessibilityKeyAt(-1));
      Rect bounds = new Rect(1, 2, 3, 4);
      view.accessibilityBounds(0, bounds);
      assertTrue(bounds.isEmpty());
      assertFalse(view.performAccessibilityClick(0));
    });
  }

  @Test
  public void virtualIdsFollowRowOrderAcrossEmptyRowsAndLayoutChanges()
  {
    onMain(() -> {
      KeyboardData.Key a = key(1f, 0f, KeyValue.makeCharKey('a'));
      KeyboardData.Key b = key(1f, 0f, KeyValue.makeCharKey('b'));
      KeyboardData.Key c = key(1f, 0f, KeyValue.makeCharKey('c'));
      view.setKeyboard(layout(row(1f, 0f, a, b), row(0f, 0f), row(1f, 0f, c)));

      assertEquals(3, view.accessibilityKeyCount());
      assertSame(a, view.accessibilityKeyAt(0));
      assertSame(b, view.accessibilityKeyAt(1));
      assertSame(c, view.accessibilityKeyAt(2));
      assertNull(view.accessibilityKeyAt(-1));
      assertNull(view.accessibilityKeyAt(3));
      assertNull(view.accessibilityKeyAt(Integer.MAX_VALUE));

      view.setKeyboard(layout(row(1f, 0f, c)));
      assertEquals(1, view.accessibilityKeyCount());
      assertSame(c, view.accessibilityKeyAt(0));
      assertNull(view.accessibilityKeyAt(1));

      view.setKeyboard(layout(row(0f, 0f)));
      assertEquals(0, view.accessibilityKeyCount());
      assertNull(view.accessibilityKeyAt(0));
    });
  }

  @Test
  public void unmeasuredOrInvalidKeysClearPreviouslyPopulatedBounds()
  {
    onMain(() -> {
      view.setKeyboard(layout(row(1f, 0f, key(1f, 0f, KeyValue.makeCharKey('a')))));
      Rect bounds = new Rect(1, 2, 3, 4);
      view.accessibilityBounds(0, bounds);
      assertTrue(bounds.isEmpty());
      assertFalse(view.performAccessibilityClick(0));
      assertTrue(handler.downs.isEmpty());

      measure();
      for (int index : new int[]{-1, 1, Integer.MAX_VALUE})
      {
        bounds.set(1, 2, 3, 4);
        view.accessibilityBounds(index, bounds);
        assertTrue("Bounds for invalid key " + index, bounds.isEmpty());
      }
    });
  }

  @Test
  public void boundsIncludeRowAndKeyShiftsAndRoundOutwards()
  {
    onMain(() -> {
      view.setKeyboard(shiftedLayout());
      measure();
      float unit = (view.getMeasuredWidth() - 14.5f) / 4f;

      // Expected coordinates for this fixture: each row is 40 pixels high,
      // vertical padding is 4 pixels, and the initial top inset is 2 pixels.
      assertBounds(0, 7.25f + 0.30f * unit, 12f, 7.25f + 1.45f * unit, 58f);
      assertBounds(1, 7.25f + 2.05f * unit, 12f, 7.25f + 2.70f * unit, 58f);
      assertBounds(2, 7.25f + 0.175f * unit, 102f, 7.25f + 1.575f * unit, 128f);
    });
  }

  @Test
  public void remeasurementUpdatesAccessibilityBounds()
  {
    onMain(() -> {
      view.setKeyboard(shiftedLayout());
      measure();
      Rect before = bounds(2);

      config.keyboard_rows_height_pixels = 60;
      config.horizontal_margin = 17.25f;
      measure();

      Rect after = bounds(2);
      assertNotEquals(before, after);
      assertEquals(153, after.top);
      assertEquals(192, after.bottom);
    });
  }

  @Test
  public void accessibilityClickUsesCenterValueExactlyOnce()
  {
    onMain(() -> {
      KeyValue a = KeyValue.makeCharKey('a');
      view.setKeyboard(layout(row(1f, 0f,
          key(1f, 0f, a, KeyValue.getKeyByName("backspace")))));
      measure();

      assertTrue(view.performAccessibilityClick(0));

      assertEquals(Collections.singletonList(a), handler.downs);
      assertEquals(Collections.singletonList(a), handler.ups);
      assertFalse(handler.lastSwipe);
    });
  }

  @Test
  public void accessibilityShiftClickAppliesToFollowingCharacter()
  {
    onMain(() -> {
      view.setKeyboard(layout(row(1f, 0f,
          key(1f, 0f, KeyValue.SHIFT), key(1f, 0f, KeyValue.makeCharKey('a')))));
      measure();

      assertTrue(view.performAccessibilityClick(0));
      assertTrue(view.performAccessibilityClick(1));

      assertEquals(Arrays.asList(KeyValue.SHIFT, KeyValue.makeCharKey('A')), handler.downs);
      assertEquals(Collections.singletonList(KeyValue.makeCharKey('A')), handler.ups);
      assertTrue(handler.lastUpModifiers.has(KeyValue.Modifier.SHIFT));
    });
  }

  @Test
  public void accessibilityClickDoesNotReleaseAnExistingTouchPointer()
  {
    onMain(() -> {
      KeyValue a = KeyValue.makeCharKey('a');
      KeyValue b = KeyValue.makeCharKey('b');
      view.setKeyboard(layout(row(1f, 0f, key(1f, 0f, a), key(1f, 0f, b))));
      measure();
      Rect first = bounds(0);
      MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN,
          first.exactCenterX(), first.exactCenterY(), 0);
      MotionEvent up = MotionEvent.obtain(0, 1, MotionEvent.ACTION_UP,
          first.exactCenterX(), first.exactCenterY(), 0);
      try
      {
        view.onTouch(view, down);
        assertTrue(view.performAccessibilityClick(1));
        assertEquals(Collections.singletonList(b), handler.ups);
        view.onTouch(view, up);
        assertEquals(Arrays.asList(a, b), handler.downs);
        assertEquals(Arrays.asList(b, a), handler.ups);
      }
      finally
      {
        down.recycle();
        up.recycle();
      }
    });
  }

  @Test
  public void centerKeyWithNoDrawableAreaCannotBeActivated()
  {
    onMain(() -> {
      view.setKeyboard(layout(row(1f, 0f, key(0f, 0f, KeyValue.makeCharKey('a')))));
      measure();

      assertTrue(bounds(0).isEmpty());
      assertFalse(view.performAccessibilityClick(0));
      assertTrue(handler.downs.isEmpty());
      assertTrue(handler.ups.isEmpty());
    });
  }

  @Test
  public void directionalClickUsesFirstDefinedValueAndActiveModifiers()
  {
    onMain(() -> {
      KeyValue[] values = new KeyValue[9];
      values[3] = KeyValue.makeCharKey('b');
      values[8] = KeyValue.makeCharKey('c');
      view.setKeyboard(layout(row(1f, 0f,
          key(1f, 0f, KeyValue.SHIFT), key(1f, 0f, values))));
      view.set_shift_state(true, false);

      assertTrue(view.performAccessibilityClick(1));

      assertEquals(Collections.singletonList(KeyValue.makeCharKey('B')), handler.downs);
      assertEquals(Collections.singletonList(KeyValue.makeCharKey('B')), handler.ups);
      assertTrue(handler.lastUpModifiers.has(KeyValue.Modifier.SHIFT));
      assertEquals("b", Keyboard2View.accessibilityLabelForKey(view.accessibilityKeyAt(1)));
    });
  }

  @Test
  public void lastDirectionalSlotCanBeActivated()
  {
    onMain(() -> {
      KeyValue[] values = new KeyValue[9];
      values[8] = KeyValue.getKeyByName("paste");
      view.setKeyboard(layout(row(1f, 0f, key(1f, 0f, values))));

      assertTrue(view.performAccessibilityClick(0));
      assertEquals(Collections.singletonList(values[8]), handler.downs);
      assertEquals(Collections.singletonList(values[8]), handler.ups);
    });
  }

  @Test
  public void invalidEmptyAndFilteredDirectionalKeysDoNotDispatchInput()
  {
    onMain(() -> {
      view.setKeyboard(layout(row(1f, 0f, key(1f, 0f),
          key(1f, 0f, null, KeyValue.getKeyByName("removed")))));
      measure();

      for (int index : new int[]{-1, 0, 1, 2, Integer.MAX_VALUE})
        assertFalse("Click for non-actionable key " + index, view.performAccessibilityClick(index));
      assertTrue(handler.downs.isEmpty());
      assertTrue(handler.ups.isEmpty());
    });
  }

  @Test
  public void touchHitTestingAgreesWithAccessibleKeyCentersAndHorizontalGaps()
  {
    onMain(() -> {
      view.setKeyboard(shiftedLayout());
      measure();
      for (int index = 0; index < 3; index++)
      {
        Rect bounds = bounds(index);
        tap(bounds.exactCenterX(), bounds.exactCenterY());
      }
      assertEquals(Arrays.asList(KeyValue.makeCharKey('a'), KeyValue.makeCharKey('b'),
          KeyValue.makeCharKey('c')), handler.ups);

      int previousCount = handler.ups.size();
      tap((bounds(0).right + bounds(1).left) / 2f, bounds(0).exactCenterY());
      tap(-1f, bounds(0).exactCenterY());
      tap(view.getMeasuredWidth() + 1f, bounds(0).exactCenterY());
      tap(bounds(0).exactCenterX(), -1f);
      tap(bounds(0).exactCenterX(), view.getMeasuredHeight() + 1f);
      assertEquals(previousCount, handler.ups.size());
    });
  }

  @Test
  public void hostExposesVirtualButtonsThroughAccessibilityProvider()
  {
    onMain(() -> {
      view.setKeyboard(layout(row(1f, 0f,
          key(1f, 0f, KeyValue.makeCharKey('a')), key(1f, 0f))));
      measure();
      assertTrue(view.isFocusable());
      assertEquals(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES,
          view.getImportantForAccessibility());
      AccessibilityNodeProviderCompat provider = ViewCompat.getAccessibilityNodeProvider(view);
      assertNotNull("Install the ExploreByTouchHelper as the view's accessibility delegate", provider);

      AccessibilityNodeInfoCompat host = provider.createAccessibilityNodeInfo(-1);
      assertEquals(2, host.getChildCount());
      AccessibilityNodeInfoCompat active = provider.createAccessibilityNodeInfo(0);
      assertEquals("android.widget.Button", active.getClassName().toString());
      assertEquals("a", active.getText().toString());
      assertEquals("a", active.getContentDescription().toString());
      assertTrue(active.isFocusable());
      assertTrue(active.isEnabled());
      assertTrue(active.isClickable());
      Rect nodeBounds = new Rect();
      active.getBoundsInParent(nodeBounds);
      assertEquals(bounds(0), nodeBounds);
      assertTrue((active.getActions() & AccessibilityNodeInfoCompat.ACTION_CLICK) != 0);

      AccessibilityNodeInfoCompat empty = provider.createAccessibilityNodeInfo(1);
      assertEquals("Empty key", empty.getText().toString());
      assertFalse(empty.isEnabled());
      assertEquals(0, empty.getActions() & AccessibilityNodeInfoCompat.ACTION_CLICK);
      assertFalse(provider.performAction(0, AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, null));
      assertFalse(provider.performAction(1, AccessibilityNodeInfoCompat.ACTION_CLICK, null));
      assertTrue(provider.performAction(0, AccessibilityNodeInfoCompat.ACTION_CLICK, null));
      assertEquals(Collections.singletonList(KeyValue.makeCharKey('a')), handler.ups);
    });
  }

  private void measure()
  {
    view.onMeasure(0, 0);
  }

  private Rect bounds(int index)
  {
    Rect bounds = new Rect();
    view.accessibilityBounds(index, bounds);
    return bounds;
  }

  private void assertBounds(int index, float left, float top, float right, float bottom)
  {
    assertEquals("Bounds for key " + index, new Rect((int)Math.floor(left),
        (int)Math.floor(top), (int)Math.ceil(right), (int)Math.ceil(bottom)), bounds(index));
  }

  private void tap(float x, float y)
  {
    MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0);
    MotionEvent up = MotionEvent.obtain(0, 1, MotionEvent.ACTION_UP, x, y, 0);
    try
    {
      assertTrue(view.onTouch(view, down));
      assertTrue(view.onTouch(view, up));
    }
    finally
    {
      down.recycle();
      up.recycle();
    }
  }

  private static KeyboardData shiftedLayout()
  {
    return layout(row(1.25f, 0.25f,
        key(1.25f, 0.25f, KeyValue.makeCharKey('a')),
        key(0.75f, 0.5f, KeyValue.makeCharKey('b'))),
        row(0f, 0.5f), row(0.75f, 0.5f,
            key(1.5f, 0.125f, KeyValue.makeCharKey('c'))));
  }

  private static KeyboardData layout(KeyboardData.Row... rows)
  {
    return new KeyboardData(Arrays.asList(rows), 4f, null, null, null,
        "Accessibility test", false, false, false);
  }

  private static KeyboardData.Row row(float height, float shift, KeyboardData.Key... keys)
  {
    return new KeyboardData.Row(Arrays.asList(keys), height, shift);
  }

  private static KeyboardData.Key key(float width, float shift, KeyValue... values)
  {
    KeyValue[] slots = new KeyValue[9];
    System.arraycopy(values, 0, slots, 0, values.length);
    return new KeyboardData.Key(slots, null, 0, width, shift, null, KeyboardData.Key.Role.Normal);
  }

  private static void onMain(Runnable action)
  {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
  }

  private static Object getStaticField(Class<?> type, String name)
  {
    try
    {
      Field field = type.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(null);
    }
    catch (ReflectiveOperationException e)
    {
      throw new AssertionError(e);
    }
  }

  private static void setStaticField(Class<?> type, String name, Object value)
  {
    try
    {
      Field field = type.getDeclaredField(name);
      field.setAccessible(true);
      field.set(null, value);
    }
    catch (ReflectiveOperationException e)
    {
      throw new AssertionError(e);
    }
  }

  private static class RecordingHandler implements Config.IKeyEventHandler
  {
    final List<KeyValue> downs = new ArrayList<>();
    final List<KeyValue> ups = new ArrayList<>();
    boolean lastSwipe;
    Pointers.Modifiers lastUpModifiers;

    @Override
    public void key_down(KeyValue value, boolean isSwipe)
    {
      downs.add(value);
      lastSwipe = isSwipe;
    }

    @Override
    public void key_up(KeyValue value, Pointers.Modifiers mods)
    {
      ups.add(value);
      lastUpModifiers = mods;
    }

    @Override
    public void mods_changed(Pointers.Modifiers mods) {}

    @Override
    public void suggestion_entered(String text) {}

    @Override
    public void finish_composing() {}
  }
}
