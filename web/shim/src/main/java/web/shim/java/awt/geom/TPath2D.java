package web.shim.java.awt.geom;

import java.util.Arrays;
import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;

/**
 * java.awt.geom.Path2D (and GeneralPath): lines, quadratic and cubic curves and closes, which Graphics2D.draw strokes
 * and fill fills with the path's winding rule, as in Java2D.
 */
public abstract class TPath2D implements TShape {
    public static final int WIND_EVEN_ODD = 0;
    public static final int WIND_NON_ZERO = 1;
    // PathIterator's SEG_ numbers, which TCanvasGraphics reads back
    private static final int MOVE = 0;
    private static final int LINE = 1;
    private static final int QUAD = 2;
    private static final int CUBIC = 3;
    private static final int CLOSE = 4;

    private int rule;
    private int[] types = new int[20];
    private int numTypes;
    private double[] coords = new double[40];
    private int numCoords;

    TPath2D(int rule) {
        setWindingRule(rule);
    }

    /** How a coordinate is kept: Path2D.Float rounds it to a float. */
    abstract double keep(double v);

    public final void moveTo(double x, double y) {
        if (numTypes > 0 && types[numTypes - 1] == MOVE) { // a second moveTo in a row replaces the first, as in the JDK
            coords[numCoords - 2] = keep(x);
            coords[numCoords - 1] = keep(y);
        } else {
            add(MOVE, x, y);
        }
    }

    public final void lineTo(double x, double y) {
        add(LINE, x, y);
    }

    public final void quadTo(double x1, double y1, double x2, double y2) {
        add(QUAD, x1, y1, x2, y2);
    }

    public final void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        add(CUBIC, x1, y1, x2, y2, x3, y3);
    }

    public final void closePath() {
        if (numTypes == 0 || types[numTypes - 1] != CLOSE) {
            add(CLOSE);
        }
    }

    public final void reset() {
        numTypes = 0;
        numCoords = 0;
    }

    public final int getWindingRule() {
        return rule;
    }

    public final void setWindingRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new IllegalArgumentException("winding rule must be WIND_EVEN_ODD or WIND_NON_ZERO");
        }
        this.rule = rule;
    }

    private void add(int type, double... c) {
        if (type != MOVE && type != CLOSE && numTypes == 0) {
            // shortcut: the JDK throws IllegalPathStateException, add that class if an app catches it
            throw new IllegalStateException("missing initial moveto in path definition");
        }
        if (numTypes == types.length) {
            types = Arrays.copyOf(types, numTypes * 2);
        }
        if (numCoords + c.length > coords.length) {
            coords = Arrays.copyOf(coords, (numCoords + c.length) * 2);
        }
        types[numTypes++] = type;
        for (double v : c) {
            coords[numCoords++] = keep(v);
        }
    }

    /** The segment kinds (PathIterator's SEG_ numbers) for TCanvasGraphics; only the first {@link #segments()} count. */
    public final int[] types() {
        return types;
    }

    /** The segments' points, two numbers each, for TCanvasGraphics. */
    public final double[] coords() {
        return coords;
    }

    public final int segments() {
        return numTypes;
    }

    /** The JDK's Path2D.getBounds2D (ends, then curve turning points with their rounding margin), rounded out to pixels. */
    @Override
    public final TRectangle getBounds() {
        double[] b = null; // left, right, top, bottom
        double lastX = 0;
        double lastY = 0;
        double startX = 0;
        double startY = 0;
        for (int i = 0, k = 0; i < numTypes; i++) {
            int type = types[i];
            int n = type == MOVE || type == LINE ? 2 : type == QUAD ? 4 : type == CUBIC ? 6 : 0;
            double endX = n == 0 ? startX : coords[k + n - 2];
            double endY = n == 0 ? startY : coords[k + n - 1];
            if (type == MOVE) {
                if (b == null) {
                    b = new double[] { endX, endX, endY, endY };
                }
                startX = endX;
                startY = endY;
            }
            if (b != null) { // a path can only start with closePath before it has a point (the JDK throws there)
                b[0] = Math.min(b[0], endX);
                b[1] = Math.max(b[1], endX);
                b[2] = Math.min(b[2], endY);
                b[3] = Math.max(b[3], endY);
                if (type == QUAD) {
                    quadExtrema(b, 0, lastX, coords[k], coords[k + 2]);
                    quadExtrema(b, 2, lastY, coords[k + 1], coords[k + 3]);
                } else if (type == CUBIC) {
                    cubicExtrema(b, 0, lastX, coords[k], coords[k + 2], coords[k + 4]);
                    cubicExtrema(b, 2, lastY, coords[k + 1], coords[k + 3], coords[k + 5]);
                }
            }
            lastX = endX;
            lastY = endY;
            k += n;
        }
        return b == null ? new TRectangle() : TRectangularShape.bounds(b[0], b[2], b[1] - b[0], b[3] - b[2]);
    }

    /** sun.awt.geom.Curve.accumulateExtremaBoundsForQuad. */
    private static void quadExtrema(double[] b, int o, double p0, double ctrl, double p2) {
        if (ctrl < b[o] || ctrl > b[o + 1]) {
            double d10 = ctrl - p0;
            double a = (p2 - ctrl) - d10;
            double bb = 2.0 * d10;
            double t = -bb / (2.0 * a);
            if (t > 0.0 && t < 1.0) {
                widen(b, o, p0 + t * (bb + t * a), Math.ulp(Math.abs(p0) + Math.abs(bb) + Math.abs(a)));
            }
        }
    }

    /** sun.awt.geom.Curve.accumulateExtremaBoundsForCubic, solving for the turning points as QuadCurve2D.solveQuadratic does. */
    private static void cubicExtrema(double[] b, int o, double p0, double c1, double c2, double p3) {
        if (c1 < b[o] || c1 > b[o + 1] || c2 < b[o] || c2 > b[o + 1]) {
            double dx32 = 3.0 * (c2 - c1);
            double dx21 = 3.0 * (c1 - p0);
            double k3 = (p3 - p0) - dx32;
            double k2 = dx32 - dx21;
            double k1 = dx21;
            double margin = Math.ulp(Math.abs(p0) + Math.abs(k1) + Math.abs(k2) + Math.abs(k3));
            // roots of k1 + 2 k2 t + 3 k3 t^2
            double qa = 3.0 * k3;
            double qb = 2.0 * k2;
            double[] ts;
            if (qa == 0.0) {
                ts = qb == 0.0 ? new double[0] : new double[] { -k1 / qb };
            } else {
                double d = qb * qb - 4.0 * qa * k1;
                if (d < 0.0) {
                    return;
                }
                d = Math.sqrt(d);
                if (qb < 0.0) {
                    d = -d;
                }
                double q = (qb + d) / -2.0;
                ts = q != 0.0 ? new double[] { q / qa, k1 / q } : new double[] { q / qa };
            }
            for (double t : ts) {
                if (t > 0.0 && t < 1.0) {
                    widen(b, o, p0 + t * (k1 + t * (k2 + t * k3)), margin);
                }
            }
        }
    }

    private static void widen(double[] b, int o, double v, double margin) {
        if (v - margin < b[o]) {
            b[o] = v - margin;
        }
        if (v + margin > b[o + 1]) {
            b[o + 1] = v + margin;
        }
    }

    public static class Float extends TPath2D {
        public Float() {
            this(WIND_NON_ZERO);
        }

        public Float(int rule) {
            super(rule);
        }

        @Override
        double keep(double v) {
            return (float) v;
        }
    }

    public static class Double extends TPath2D {
        public Double() {
            this(WIND_NON_ZERO);
        }

        public Double(int rule) {
            super(rule);
        }

        @Override
        double keep(double v) {
            return v;
        }
    }
}
