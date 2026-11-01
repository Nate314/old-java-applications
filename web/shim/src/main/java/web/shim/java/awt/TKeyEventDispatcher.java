package web.shim.java.awt;

import web.shim.java.awt.event.TKeyEvent;

public interface TKeyEventDispatcher {
    boolean dispatchKeyEvent(TKeyEvent e);
}
