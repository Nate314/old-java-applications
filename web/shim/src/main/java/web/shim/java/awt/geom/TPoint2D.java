package web.shim.java.awt.geom;

public abstract class TPoint2D {
    protected TPoint2D() {
    }

    public abstract double getX();

    public abstract double getY();

    public double distance(double px, double py) {
        return Math.hypot(px - getX(), py - getY());
    }

    public double distance(TPoint2D p) {
        return distance(p.getX(), p.getY());
    }
}
