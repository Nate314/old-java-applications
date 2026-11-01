package web.shim.java.awt;

import java.util.EventObject;

public abstract class TAWTEvent extends EventObject {
    public static final long WINDOW_EVENT_MASK = 0x40;

    protected int id;
    protected boolean consumed;

    public TAWTEvent(Object source, int id) {
        super(source);
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public void setSource(Object newSource) {
        source = newSource;
    }

    protected void consume() {
        consumed = true;
    }

    protected boolean isConsumed() {
        return consumed;
    }
}
