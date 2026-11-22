package web.shim.java.awt.event;

import web.shim.java.awt.TComponent;

/** FOCUS_GAINED and FOCUS_LOST, sent as the keyboard focus moves between an app's components (see TKeyboardFocusManager). */
public class TFocusEvent extends TComponentEvent {
    public static final int FOCUS_FIRST = 1004;
    public static final int FOCUS_LAST = 1005;
    public static final int FOCUS_GAINED = FOCUS_FIRST;
    public static final int FOCUS_LOST = FOCUS_LAST;

    private final boolean temporary;
    private final TComponent opposite;

    public TFocusEvent(TComponent source, int id) {
        this(source, id, false, null);
    }

    public TFocusEvent(TComponent source, int id, boolean temporary) {
        this(source, id, temporary, null);
    }

    public TFocusEvent(TComponent source, int id, boolean temporary, TComponent opposite) {
        super(source, id);
        this.temporary = temporary;
        this.opposite = opposite;
    }

    public boolean isTemporary() {
        return temporary;
    }

    /** The component that lost the focus to this one, or gains it from this one; null when there is none. */
    public TComponent getOppositeComponent() {
        return opposite;
    }
}
