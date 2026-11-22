package web.shim.java.awt;

import java.util.ArrayList;
import java.util.List;
import web.shim.java.awt.web.TDesktop;
import web.shim.java.awt.event.TFocusEvent;
import web.shim.java.awt.event.TKeyEvent;

public class TKeyboardFocusManager {
    private static final TKeyboardFocusManager INSTANCE = new TKeyboardFocusManager();
    private final List<TKeyEventDispatcher> dispatchers = new ArrayList<>();
    /** The component the focus listeners last heard has the focus. */
    private TComponent focused;

    public static TKeyboardFocusManager getCurrentKeyboardFocusManager() {
        return INSTANCE;
    }

    public void addKeyEventDispatcher(TKeyEventDispatcher d) {
        if (d != null) {
            dispatchers.add(d);
        }
    }

    public void removeKeyEventDispatcher(TKeyEventDispatcher d) {
        dispatchers.remove(d);
    }

    public TComponent getFocusOwner() {
        return TDesktop.keyTarget();
    }

    public TWindow getActiveWindow() {
        return TDesktop.activeWindow();
    }

    /** Runs the registered dispatchers; true if one of them consumed the event. */
    public boolean dispatchToDispatchers(TKeyEvent e) {
        for (TKeyEventDispatcher d : new ArrayList<>(dispatchers)) {
            if (d.dispatchKeyEvent(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells the focus listeners the focus is now on {@code now} (null: on no component), as Swing's FOCUS_LOST then
     * FOCUS_GAINED: nothing when it did not move. A loss to another window (or to none) is temporary, as when Swing's
     * window is deactivated, unless the window that had it is gone.
     */
    public void focusMovedTo(TComponent now) {
        TComponent was = focused;
        if (was == now) {
            return;
        }
        focused = now;
        if (was != null) {
            TWindow from = was.windowAncestor();
            boolean temporary = from != null && from.isVisible() && (now == null || now.windowAncestor() != from);
            was.dispatchEvent(new TFocusEvent(was, TFocusEvent.FOCUS_LOST, temporary, now));
        }
        if (now != null) {
            now.dispatchEvent(new TFocusEvent(now, TFocusEvent.FOCUS_GAINED, false, was));
        }
    }
}
