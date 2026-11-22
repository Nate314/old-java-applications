package web.shim.java.awt;

/** java.awt.Shape: Rectangle, Polygon, Line2D, Ellipse2D, QuadCurve2D and Path2D here; Graphics2D draws others as their bounds. */
public interface TShape {
    TRectangle getBounds();
}
