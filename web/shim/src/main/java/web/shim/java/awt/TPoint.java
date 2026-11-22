package web.shim.java.awt;

public class TPoint extends web.shim.java.awt.geom.TPoint2D {
    public int x;
    public int y;

    public TPoint() {
    }

    public TPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Rounds to the nearest whole point, halves up, as the JDK does. */
    @Override
    public void setLocation(double x, double y) {
        this.x = (int) Math.floor(x + 0.5);
        this.y = (int) Math.floor(y + 0.5);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TPoint && ((TPoint) obj).x == x && ((TPoint) obj).y == y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    @Override
    public String toString() {
        return "java.awt.Point[x=" + x + ",y=" + y + "]";
    }
}
