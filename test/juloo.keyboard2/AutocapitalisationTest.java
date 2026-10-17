package juloo.keyboard2;

import android.os.Handler;
import android.view.inputmethod.BaseInputConnection;
import android.view.KeyEvent;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

public class AutocapitalisationTest {

    private boolean shiftEnabled;
    private boolean shiftDisabled;

    private Autocapitalisation autocap;
    private MockInputConnection ic;

    class MockInputConnection extends BaseInputConnection {
        public MockInputConnection() { super(null, true); }
        public int cursorCapsMode = 0;
        public CharSequence textAfterCursor = "";

        @Override
        public int getCursorCapsMode(int reqModes) {
            return cursorCapsMode;
        }

        @Override
        public CharSequence getTextAfterCursor(int length, int flags) {
            return textAfterCursor;
        }
    }

    private Config createMockConfig() throws Exception {
        Config config = Mockito.mock(Config.class);
        config.editor_config = new EditorConfig();
        return config;
    }

    @Before
    public void setup() {
        shiftEnabled = false;
        shiftDisabled = false;
        ic = new MockInputConnection();
        Handler handler = Mockito.mock(Handler.class);

        Mockito.when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = invocation.getArgument(0);
                r.run(); // execute directly
                return true;
            }
        });

        autocap = new Autocapitalisation(handler, new Autocapitalisation.Callback() {
            @Override
            public void update_shift_state(boolean should_enable, boolean should_disable) {
                shiftEnabled = should_enable;
                shiftDisabled = should_disable;
            }
        });
    }

    @Test
    public void testTypingAndCursor() throws Exception {
        Config config = createMockConfig();
        config.editor_config.caps_mode = 1;
        config.autocapitalisation = true;

        autocap.started(config, ic);

        assertFalse(shiftEnabled);
        assertTrue(shiftDisabled);

        autocap.typed("H");
        assertFalse(shiftEnabled);
        assertFalse(shiftDisabled);

        autocap.typed(" ");
        assertFalse(shiftEnabled);

        ic.cursorCapsMode = 1;
        autocap.typed(" ");
        assertTrue(shiftEnabled);
    }

    @Test
    public void testSelectionUpdated() throws Exception {
        Config config = createMockConfig();
        config.editor_config.caps_mode = 1;
        config.autocapitalisation = true;

        autocap.started(config, ic);
        ic.cursorCapsMode = 1;

        // make sure _cursor is not 0
        autocap.typed("a");

        ic.textAfterCursor = "";
        autocap.selection_updated(1, 0);
        assertTrue(shiftEnabled);

        // make sure _cursor is not 0 again
        autocap.typed("a");

        ic.textAfterCursor = "foo";
        autocap.selection_updated(1, 0);
        assertFalse(shiftEnabled);
    }

    @Test
    public void testEventSent() throws Exception {
        Config config = createMockConfig();
        config.editor_config.caps_mode = 1;
        config.autocapitalisation = true;

        autocap.started(config, ic);
        ic.cursorCapsMode = 1;

        autocap.event_sent(KeyEvent.KEYCODE_DEL, 0);
        assertTrue(shiftEnabled);

        autocap.event_sent(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON);
        assertTrue(shiftEnabled); // because method returns before changing state
    }

    @Test
    public void testPauseUnpause() throws Exception {
        Config config = createMockConfig();
        config.editor_config.caps_mode = 1;
        config.autocapitalisation = true;

        autocap.started(config, ic);
        ic.cursorCapsMode = 1;

        boolean paused = autocap.pause();
        assertTrue(paused);
        assertFalse(autocap._enabled);
        assertFalse(shiftEnabled);

        autocap.unpause(paused);
        assertTrue(autocap._enabled);
        assertTrue(shiftEnabled);
    }
}
