package web.shim.java.awt;

import java.util.ArrayList;
import java.util.List;
import web.shim.java.awt.web.TDesktop;
import web.shim.java.awt.event.TKeyEvent;

public class TKeyboardFocusManager {
    private static final TKeyboardFocusManager INSTANCE = new TKeyboardFocusManager();
    private final List<TKeyEventDispatcher> dispatchers = new ArrayList<>();

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
}
