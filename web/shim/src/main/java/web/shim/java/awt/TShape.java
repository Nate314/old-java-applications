package web.shim.java.awt;

/** java.awt.Shape: Rectangle, Polygon and QuadCurve2D here; Graphics2D draws others as their bounds. */
public interface TShape {
    TRectangle getBounds();
}
