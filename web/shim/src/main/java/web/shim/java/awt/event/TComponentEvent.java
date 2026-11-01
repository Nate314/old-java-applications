package web.shim.java.awt.event;

import web.shim.java.awt.TAWTEvent;
import web.shim.java.awt.TComponent;

public class TComponentEvent extends TAWTEvent {
    public static final int COMPONENT_MOVED = 100;
    public static final int COMPONENT_RESIZED = 101;
    public static final int COMPONENT_SHOWN = 102;
    public static final int COMPONENT_HIDDEN = 103;

    public TComponentEvent(TComponent source, int id) {
        super(source, id);
    }

    public TComponent getComponent() {
        return (TComponent) getSource();
    }
}
