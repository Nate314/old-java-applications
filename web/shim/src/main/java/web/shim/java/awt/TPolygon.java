package web.shim.java.awt;

import java.util.Arrays;
import web.shim.java.awt.geom.TAffineTransform;
import web.shim.java.awt.geom.TPath2D;
import web.shim.java.awt.geom.TPathIterator;

/** java.awt.Polygon: the point lists, bounds and the JDK's even-odd inside test (PolygonCheck compares them). */
public class TPolygon implements TShape {
    public int npoints;
    public int[] xpoints;
    public int[] ypoints;
    protected TRectangle bounds;

    public TPolygon() {
        xpoints = new int[4];
        ypoints = new int[4];
    }

    public TPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
        }
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        this.npoints = npoints;
        this.xpoints = Arrays.copyOf(xpoints, npoints);
        this.ypoints = Arrays.copyOf(ypoints, npoints);
    }

    public void reset() {
        npoints = 0;
        bounds = null;
    }

    public void invalidate() {
        bounds = null;
    }

    public void translate(int dx, int dy) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += dx;
            ypoints[i] += dy;
        }
        if (bounds != null) {
            bounds.translate(dx, dy);
        }
    }

    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length || npoints >= ypoints.length) {
            int n = Math.max(npoints * 2, 4);
            xpoints = Arrays.copyOf(xpoints, n);
            ypoints = Arrays.copyOf(ypoints, n);
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
        if (bounds != null) {
            int x1 = Math.min(bounds.x, x);
            int y1 = Math.min(bounds.y, y);
            int x2 = Math.max(bounds.x + bounds.width, x);
            int y2 = Math.max(bounds.y + bounds.height, y);
            bounds.setBounds(x1, y1, x2 - x1, y2 - y1);
        }
    }

    @Override
    public TRectangle getBounds() {
        if (npoints == 0) {
            return new TRectangle();
        }
        if (bounds == null) {
            int x1 = Integer.MAX_VALUE;
            int y1 = Integer.MAX_VALUE;
            int x2 = Integer.MIN_VALUE;
            int y2 = Integer.MIN_VALUE;
            for (int i = 0; i < npoints; i++) {
                x1 = Math.min(x1, xpoints[i]);
                x2 = Math.max(x2, xpoints[i]);
                y1 = Math.min(y1, ypoints[i]);
                y2 = Math.max(y2, ypoints[i]);
            }
            bounds = new TRectangle(x1, y1, x2 - x1, y2 - y1);
        }
        return new TRectangle(bounds);
    }

    /** Move, lines, close with the even-odd rule, as the JDK's; nothing for no points. */
    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        TPath2D p = new TPath2D.Double(TPath2D.WIND_EVEN_ODD);
        for (int i = 0; i < npoints; i++) {
            if (i == 0) {
                p.moveTo(xpoints[i], ypoints[i]);
            } else {
                p.lineTo(xpoints[i], ypoints[i]);
            }
        }
        if (npoints > 0) {
            p.closePath();
        }
        return p.getPathIterator(at);
    }

    public boolean contains(TPoint p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    public boolean contains(double x, double y) {
        if (npoints <= 2) {
            return false;
        }
        TRectangle b = getBounds();
        if (x < b.x || y < b.y || x >= b.x + b.width || y >= b.y + b.height) {
            return false;
        }
        int hits = 0;
        int lastx = xpoints[npoints - 1];
        int lasty = ypoints[npoints - 1];
        for (int i = 0; i < npoints; i++) {
            int curx = xpoints[i];
            int cury = ypoints[i];
            int px = lastx;
            int py = lasty;
            lastx = curx;
            lasty = cury;
            if (cury == py) {
                continue;
            }
            int leftx;
            if (curx < px) {
                if (x >= px) {
                    continue;
                }
                leftx = curx;
            } else {
                if (x >= curx) {
                    continue;
                }
                leftx = px;
            }
            double test1;
            double test2;
            if (cury < py) {
                if (y < cury || y >= py) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if (y < py || y >= cury) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - px;
                test2 = y - py;
            }
            if (test1 < (test2 / (py - cury) * (px - curx))) {
                hits++;
            }
        }
        return (hits & 1) != 0;
    }
}
