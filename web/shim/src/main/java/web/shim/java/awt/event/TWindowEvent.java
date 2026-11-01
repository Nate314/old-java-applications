package web.shim.java.awt.event;

import web.shim.java.awt.TWindow;

/** Only the two window events the apps and NathaNOS use: opened (seen by AWTEventListeners) and closing. */
public class TWindowEvent extends TComponentEvent {
    public static final int WINDOW_OPENED = 200;
    public static final int WINDOW_CLOSING = 201;

    public TWindowEvent(TWindow source, int id) {
        super(source, id);
    }

    public TWindow getWindow() {
        return (TWindow) getSource();
    }
}
