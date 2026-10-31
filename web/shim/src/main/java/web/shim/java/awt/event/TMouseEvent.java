package web.shim.java.awt.event;

import web.shim.java.awt.TComponent;
import web.shim.java.awt.TPoint;

public class TMouseEvent extends TInputEvent {
    public static final int MOUSE_CLICKED = 500;
    public static final int MOUSE_PRESSED = 501;
    public static final int MOUSE_RELEASED = 502;
    public static final int MOUSE_MOVED = 503;
    public static final int MOUSE_ENTERED = 504;
    public static final int MOUSE_EXITED = 505;
    public static final int MOUSE_DRAGGED = 506;
    public static final int NOBUTTON = 0;
    public static final int BUTTON1 = 1;
    public static final int BUTTON2 = 2;
    public static final int BUTTON3 = 3;

    private final int x;
    private final int y;
    private final int clickCount;
    private final boolean popupTrigger;
    private final int button;
    private int xOnScreen;
    private int yOnScreen;

    public TMouseEvent(TComponent source, int id, long when, int modifiers, int x, int y, int clickCount,
            boolean popupTrigger, int button) {
        this(source, id, when, modifiers, x, y, x, y, clickCount, popupTrigger, button);
    }

    public TMouseEvent(TComponent source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs,
            int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers);
        this.xOnScreen = xAbs;
        this.yOnScreen = yAbs;
        this.x = x;
        this.y = y;
        this.clickCount = clickCount;
        this.popupTrigger = popupTrigger;
        this.button = button;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TPoint getPoint() {
        return new TPoint(x, y);
    }

    /** Screen coordinates are page-viewport coordinates. */
    public TPoint getLocationOnScreen() {
        return new TPoint(xOnScreen, yOnScreen);
    }

    public int getXOnScreen() {
        return xOnScreen;
    }

    public int getYOnScreen() {
        return yOnScreen;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getButton() {
        return button;
    }

    public boolean isPopupTrigger() {
        return popupTrigger;
    }
}
