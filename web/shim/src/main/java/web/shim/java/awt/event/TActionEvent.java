package web.shim.java.awt.event;

import web.shim.java.awt.TAWTEvent;

public class TActionEvent extends TAWTEvent {
    public static final int ACTION_PERFORMED = 1001;

    private final String command;
    private final long when;
    private final int modifiers;

    public TActionEvent(Object source, int id, String command) {
        this(source, id, command, System.currentTimeMillis(), 0);
    }

    public TActionEvent(Object source, int id, String command, int modifiers) {
        this(source, id, command, System.currentTimeMillis(), modifiers);
    }

    public TActionEvent(Object source, int id, String command, long when, int modifiers) {
        super(source, id);
        this.command = command;
        this.when = when;
        this.modifiers = modifiers;
    }

    public String getActionCommand() {
        return command;
    }

    public long getWhen() {
        return when;
    }

    public int getModifiers() {
        return modifiers;
    }
}
