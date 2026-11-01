package web.shim.java.awt.event;

import web.shim.java.awt.TComponent;

public abstract class TInputEvent extends TComponentEvent {
    public static final int SHIFT_MASK = 1;
    public static final int CTRL_MASK = 2;
    public static final int META_MASK = 4;
    public static final int ALT_MASK = 8;
    public static final int BUTTON1_MASK = 16;
    public static final int SHIFT_DOWN_MASK = 64;
    public static final int CTRL_DOWN_MASK = 128;
    public static final int META_DOWN_MASK = 256;
    public static final int ALT_DOWN_MASK = 512;

    private final long when;
    private final int modifiers;

    TInputEvent(TComponent source, int id, long when, int modifiers) {
        super(source, id);
        this.when = when;
        this.modifiers = modifiers;
    }

    public long getWhen() {
        return when;
    }

    public int getModifiers() {
        return modifiers & 15;
    }

    public int getModifiersEx() {
        return modifiers & ~15;
    }

    public boolean isShiftDown() {
        return (modifiers & SHIFT_MASK) != 0;
    }

    public boolean isControlDown() {
        return (modifiers & CTRL_MASK) != 0;
    }

    public boolean isMetaDown() {
        return (modifiers & META_MASK) != 0;
    }

    public boolean isAltDown() {
        return (modifiers & ALT_MASK) != 0;
    }

    @Override
    public void consume() {
        super.consume();
    }

    @Override
    public boolean isConsumed() {
        return super.isConsumed();
    }
}
