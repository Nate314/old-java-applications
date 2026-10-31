package web.shim.java.awt;

public class TCursor {
    public static final int DEFAULT_CURSOR = 0;
    public static final int CROSSHAIR_CURSOR = 1;
    public static final int TEXT_CURSOR = 2;
    public static final int WAIT_CURSOR = 3;
    public static final int HAND_CURSOR = 12;
    public static final int MOVE_CURSOR = 13;

    private static final String[] CSS = { "default", "crosshair", "text", "wait", "sw-resize", "se-resize", "nw-resize",
            "ne-resize", "n-resize", "s-resize", "w-resize", "e-resize", "pointer", "move" };
    private static final TCursor DEFAULT = new TCursor(DEFAULT_CURSOR);

    private final int type;
    private final String css;

    public TCursor(int type) {
        this.type = type;
        this.css = type >= 0 && type < CSS.length ? CSS[type] : "default";
    }

    TCursor(String css) {
        this.type = -1;
        this.css = css;
    }

    public static TCursor getPredefinedCursor(int type) {
        return new TCursor(type);
    }

    public static TCursor getDefaultCursor() {
        return DEFAULT;
    }

    public int getType() {
        return type;
    }

    public String css() {
        return css;
    }
}
