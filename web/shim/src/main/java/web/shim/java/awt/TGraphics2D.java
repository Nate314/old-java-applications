package web.shim.java.awt;

import java.util.Map;
import web.shim.java.awt.geom.TAffineTransform;

public abstract class TGraphics2D extends TGraphics {
    protected TGraphics2D() {
    }

    public abstract void setStroke(TStroke s);

    public abstract TStroke getStroke();

    public abstract void setRenderingHint(TRenderingHints.Key key, Object value);

    public abstract Object getRenderingHint(TRenderingHints.Key key);

    public abstract void setRenderingHints(Map<?, ?> hints);

    public abstract void addRenderingHints(Map<?, ?> hints);

    public abstract void translate(double tx, double ty);

    public abstract void rotate(double theta);

    public abstract void scale(double sx, double sy);

    public void rotate(double theta, double x, double y) {
        translate(x, y);
        rotate(theta);
        translate(-x, -y);
    }

    public abstract void shear(double shx, double shy);

    /** A copy of the user-space transform (the page's pixel ratio stays outside it). */
    public abstract TAffineTransform getTransform();

    public abstract void setTransform(TAffineTransform t);

    /** Applies t before the current transform, as Java2D's Graphics2D.transform. */
    public abstract void transform(TAffineTransform t);

    public abstract void drawString(String str, float x, float y);

    public abstract void setPaint(TPaint paint);

    public abstract TPaint getPaint();

    /** Narrows the clip to the shape (null drops it). */
    public abstract void clip(TShape s);

    public abstract void draw(TShape s);

    public abstract void fill(TShape s);
}
