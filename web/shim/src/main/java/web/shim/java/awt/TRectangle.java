package web.shim.java.awt;

/** java.awt.Rectangle; the tests follow the JDK's, including its rules for empty and negative sizes (RectangleCheck). */
public class TRectangle extends web.shim.java.awt.geom.TRectangle2D {
    public int x;
    public int y;
    public int width;
    public int height;

    public TRectangle() {
    }

    public TRectangle(int width, int height) {
        this(0, 0, width, height);
    }

    public TRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public TRectangle(TRectangle r) {
        this(r.x, r.y, r.width, r.height);
    }

    @Override
    public TRectangle getBounds() {
        return new TRectangle(x, y, width, height);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** Stops at the int range, shrinking the size by what went past, as the JDK does. */
    public void translate(int dx, int dy) {
        int[] h = moved(x, width, dx);
        x = h[0];
        width = h[1];
        int[] v = moved(y, height, dy);
        y = v[0];
        height = v[1];
    }

    private static int[] moved(int at, int size, int d) {
        int to = at + d;
        if (d < 0 && to > at) {
            if (size >= 0) {
                size += to - Integer.MIN_VALUE;
            }
            to = Integer.MIN_VALUE;
        } else if (d >= 0 && to < at) {
            if (size >= 0) {
                size += to - Integer.MAX_VALUE;
                if (size < 0) {
                    size = Integer.MAX_VALUE;
                }
            }
            to = Integer.MAX_VALUE;
        }
        return new int[] { to, size };
    }

    @Override
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    public boolean contains(int px, int py) {
        int w = width;
        int h = height;
        if ((w | h) < 0) {
            return false;
        }
        if (px < x || py < y) {
            return false;
        }
        w += x;
        h += y;
        // the w < x and h < y tests catch an edge that overflowed past Integer.MAX_VALUE, as in the JDK
        return (w < x || w > px) && (h < y || h > py);
    }

    public boolean contains(TPoint p) {
        return contains(p.x, p.y);
    }

    /** The JDK's Rectangle.setRect: the smallest whole-pixel rectangle around the doubles, clamped to int. */
    @Override
    public void setRect(double x, double y, double width, double height) {
        int newx;
        int newy;
        int neww;
        int newh;
        if (x > 2.0 * Integer.MAX_VALUE) {
            newx = Integer.MAX_VALUE;
            neww = -1;
        } else {
            newx = clip(x, false);
            if (width >= 0) {
                width += x - newx;
            }
            neww = clip(width, width >= 0);
        }
        if (y > 2.0 * Integer.MAX_VALUE) {
            newy = Integer.MAX_VALUE;
            newh = -1;
        } else {
            newy = clip(y, false);
            if (height >= 0) {
                height += y - newy;
            }
            newh = clip(height, height >= 0);
        }
        setBounds(newx, newy, neww, newh);
    }

    private static int clip(double v, boolean ceil) {
        if (v <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (v >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) (ceil ? Math.ceil(v) : Math.floor(v));
    }

    public boolean contains(TRectangle r) {
        return contains(r.x, r.y, r.width, r.height);
    }

    public boolean contains(int rx, int ry, int rw, int rh) {
        int w = width;
        int h = height;
        if ((w | h | rw | rh) < 0) {
            return false;
        }
        if (rx < x || ry < y) {
            return false;
        }
        w += x;
        rw += rx;
        if (rw <= rx) {
            if (w >= x || rw > w) {
                return false;
            }
        } else if (w >= x && rw > w) {
            return false;
        }
        h += y;
        rh += ry;
        if (rh <= ry) {
            return h < y && rh <= h;
        }
        return h < y || rh <= h;
    }

    public boolean intersects(TRectangle r) {
        int tw = width;
        int th = height;
        int rw = r.width;
        int rh = r.height;
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int tx = x;
        int ty = y;
        int rx = r.x;
        int ry = r.y;
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        return (rw < rx || rw > tx) && (rh < ry || rh > ty) && (tw < tx || tw > rx) && (th < ty || th > ry);
    }

    /** The overlap; when there is none the width or height is negative (or zero), as in the JDK. */
    public TRectangle intersection(TRectangle r) {
        int tx1 = x;
        int ty1 = y;
        long tx2 = tx1 + (long) width;
        long ty2 = ty1 + (long) height;
        long rx2 = r.x + (long) r.width;
        long ry2 = r.y + (long) r.height;
        tx1 = Math.max(tx1, r.x);
        ty1 = Math.max(ty1, r.y);
        tx2 = Math.min(tx2, rx2) - tx1;
        ty2 = Math.min(ty2, ry2) - ty1;
        return new TRectangle(tx1, ty1, (int) Math.max(tx2, Integer.MIN_VALUE), (int) Math.max(ty2, Integer.MIN_VALUE));
    }

    /** The smallest rectangle covering both; a rectangle with a negative size is left out, as in the JDK. */
    public TRectangle union(TRectangle r) {
        long tx2 = width;
        long ty2 = height;
        if ((tx2 | ty2) < 0) {
            return new TRectangle(r);
        }
        long rx2 = r.width;
        long ry2 = r.height;
        if ((rx2 | ry2) < 0) {
            return new TRectangle(this);
        }
        tx2 += x;
        ty2 += y;
        rx2 += r.x;
        ry2 += r.y;
        int tx1 = Math.min(x, r.x);
        int ty1 = Math.min(y, r.y);
        tx2 = Math.max(tx2, rx2) - tx1;
        ty2 = Math.max(ty2, ry2) - ty1;
        return new TRectangle(tx1, ty1, (int) Math.min(tx2, Integer.MAX_VALUE), (int) Math.min(ty2, Integer.MAX_VALUE));
    }

    /** Grows this rectangle to the smallest one that also covers r. */
    public void add(TRectangle r) {
        int x1 = Math.max(x + width, r.x + r.width);
        int y1 = Math.max(y + height, r.y + r.height);
        x = Math.min(x, r.x);
        y = Math.min(y, r.y);
        width = x1 - x;
        height = y1 - y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TRectangle)) {
            return false;
        }
        TRectangle r = (TRectangle) obj;
        return x == r.x && y == r.y && width == r.width && height == r.height;
    }

    @Override
    public int hashCode() {
        return ((x * 31 + y) * 31 + width) * 31 + height;
    }

    @Override
    public String toString() {
        return "java.awt.Rectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
