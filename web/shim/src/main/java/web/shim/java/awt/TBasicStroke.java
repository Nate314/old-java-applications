package web.shim.java.awt;

public class TBasicStroke implements TStroke {
    public static final int CAP_BUTT = 0;
    public static final int CAP_ROUND = 1;
    public static final int CAP_SQUARE = 2;
    public static final int JOIN_MITER = 0;
    public static final int JOIN_ROUND = 1;
    public static final int JOIN_BEVEL = 2;

    private final float width;
    private final int cap;
    private final int join;

    public TBasicStroke() {
        this(1f);
    }

    public TBasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER);
    }

    public TBasicStroke(float width, int cap, int join) {
        this.width = width;
        this.cap = cap;
        this.join = join;
    }

    public float getLineWidth() {
        return width;
    }

    public int getEndCap() {
        return cap;
    }

    public int getLineJoin() {
        return join;
    }
}
