package web.shim.java.awt;

import web.shim.java.awt.geom.TPoint2D;

/**
 * java.awt.GradientPaint: color1 at point 1 blending to color2 at point 2 along the line between them, in user space.
 * Past the ends it keeps the end colors, or with cyclic it goes back and forth. The canvas draws it (TCanvasGraphics).
 */
public class TGradientPaint implements TPaint {
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final TColor color1;
    private final TColor color2;
    private final boolean cyclic;

    public TGradientPaint(float x1, float y1, TColor color1, float x2, float y2, TColor color2) {
        this(x1, y1, color1, x2, y2, color2, false);
    }

    public TGradientPaint(float x1, float y1, TColor color1, float x2, float y2, TColor color2, boolean cyclic) {
        if (color1 == null || color2 == null) {
            throw new NullPointerException("Colors cannot be null");
        }
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    public TGradientPaint(TPoint2D p1, TColor color1, TPoint2D p2, TColor color2) {
        this((float) p1.getX(), (float) p1.getY(), color1, (float) p2.getX(), (float) p2.getY(), color2, false);
    }

    public TGradientPaint(TPoint2D p1, TColor color1, TPoint2D p2, TColor color2, boolean cyclic) {
        this((float) p1.getX(), (float) p1.getY(), color1, (float) p2.getX(), (float) p2.getY(), color2, cyclic);
    }

    public float x1() {
        return x1;
    }

    public float y1() {
        return y1;
    }

    public float x2() {
        return x2;
    }

    public float y2() {
        return y2;
    }

    public TColor getColor1() {
        return color1;
    }

    public TColor getColor2() {
        return color2;
    }

    public boolean isCyclic() {
        return cyclic;
    }
}
