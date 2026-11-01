package web.shim.java.awt;

import java.util.Map;

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

    public abstract void drawString(String str, float x, float y);

    // shortcut: only Rectangle is a TShape, add paths when an app draws another shape
    public void draw(TShape s) {
        TRectangle r = s.getBounds();
        drawRect(r.x, r.y, r.width, r.height);
    }

    public void fill(TShape s) {
        TRectangle r = s.getBounds();
        fillRect(r.x, r.y, r.width, r.height);
    }
}
