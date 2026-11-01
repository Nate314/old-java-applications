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
