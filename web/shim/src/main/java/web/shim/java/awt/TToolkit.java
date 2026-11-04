package web.shim.java.awt;

import java.util.ArrayList;
import java.util.List;
import web.shim.java.awt.datatransfer.TClipboard;
import web.shim.java.awt.event.TAWTEventListener;
import web.shim.java.awt.web.TDesktop;

public class TToolkit {
    private static final TToolkit INSTANCE = new TToolkit();
    private final TEventQueue eventQueue = new TEventQueue();
    private final TClipboard clipboard = new TClipboard("System");
    private final List<TAWTEventListener> awtListeners = new ArrayList<>();
    private final List<Long> awtListenerMasks = new ArrayList<>();

    protected TToolkit() {
    }

    public static TToolkit getDefaultToolkit() {
        return INSTANCE;
    }

    /** The "screen" is the browser viewport that holds the page desktop. */
    public TDimension getScreenSize() {
        return new TDimension(TDesktop.screenWidth(), TDesktop.screenHeight());
    }

    public int getScreenResolution() {
        return 96;
    }

    public TImage getImage(String filename) {
        return TImage.load(filename);
    }

    public TImage getImage(java.net.URL url) {
        return TImage.load(url.getPath());
    }

    public TImage createImage(String filename) {
        return TImage.load(filename);
    }

    public TCursor createCustomCursor(TImage cursor, TPoint hotSpot, String name) {
        String url = cursor == null ? null : cursor.url();
        if (url == null || cursor.getWidth(null) <= 0) {
            return TCursor.getDefaultCursor();
        }
        return new TCursor("url(\"" + url + "\") " + hotSpot.x + " " + hotSpot.y + ", auto");
    }

    public TEventQueue getSystemEventQueue() {
        return eventQueue;
    }

    public void addAWTEventListener(TAWTEventListener listener, long eventMask) {
        awtListeners.add(listener);
        awtListenerMasks.add(eventMask);
    }

    /** Hands an event to the AWTEventListeners registered for its mask, as AWT does before dispatching it. */
    public void notifyAWTEventListeners(TAWTEvent event, long eventMask) {
        for (int i = 0; i < awtListeners.size(); i++) {
            if ((awtListenerMasks.get(i) & eventMask) != 0) {
                awtListeners.get(i).eventDispatched(event);
            }
        }
    }

    public void beep() {
    }

    /** The browser's clipboard (text only, write only). */
    public TClipboard getSystemClipboard() {
        return this.clipboard;
    }

    public void sync() {
    }
}
