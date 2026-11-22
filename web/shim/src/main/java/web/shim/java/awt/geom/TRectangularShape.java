package web.shim.java.awt.geom;

import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;

/** java.awt.geom.RectangularShape: the frame's edges and center, and its bounds rounded out to whole pixels as the JDK does. */
public abstract class TRectangularShape implements TShape {
    protected TRectangularShape() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public double getMinX() {
        return getX();
    }

    public double getMinY() {
        return getY();
    }

    public double getMaxX() {
        return getX() + getWidth();
    }

    public double getMaxY() {
        return getY() + getHeight();
    }

    public double getCenterX() {
        return getX() + getWidth() / 2.0;
    }

    public double getCenterY() {
        return getY() + getHeight() / 2.0;
    }

    @Override
    public TRectangle getBounds() {
        return bounds(getX(), getY(), getWidth(), getHeight());
    }

    /** Rectangle2D.getBounds: the smallest whole-pixel box around a box given in doubles (empty for a negative size). */
    public static TRectangle bounds(double x, double y, double w, double h) {
        if (w < 0 || h < 0) {
            return new TRectangle();
        }
        double x1 = Math.floor(x);
        double y1 = Math.floor(y);
        return new TRectangle((int) x1, (int) y1, (int) (Math.ceil(x + w) - x1), (int) (Math.ceil(y + h) - y1));
    }
}
