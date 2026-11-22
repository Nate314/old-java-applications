package web.shim.java.awt.geom;

import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;

/** java.awt.geom.QuadCurve2D: Graphics2D.draw strokes it and fill fills it up to the chord, as in Java2D. */
public abstract class TQuadCurve2D implements TShape {
    protected TQuadCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getCtrlX();

    public abstract double getCtrlY();

    public abstract double getX2();

    public abstract double getY2();

    public abstract void setCurve(double x1, double y1, double ctrlx, double ctrly, double x2, double y2);

    public void setCurve(TQuadCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(), c.getY2());
    }

    /** The curve's own box (since JDK 19 not the control point's), rounded out to whole pixels as the JDK does. */
    @Override
    public TRectangle getBounds() {
        double[] xs = extent(getX1(), getCtrlX(), getX2());
        double[] ys = extent(getY1(), getCtrlY(), getY2());
        int x = (int) Math.floor(xs[0]);
        int y = (int) Math.floor(ys[0]);
        // min + (max - min), not max: Rectangle2D.getBounds rounds through the width, and so may land one pixel wider
        return new TRectangle(x, y, (int) Math.ceil(xs[0] + (xs[1] - xs[0])) - x, (int) Math.ceil(ys[0] + (ys[1] - ys[0])) - y);
    }

    /**
     * Min and max of one coordinate along the curve: the ends, and the turning point when it falls inside, widened by
     * the rounding error margin, the same sums as the JDK's Curve.accumulateExtremaBoundsForQuad.
     */
    private static double[] extent(double p0, double p1, double p2) {
        double min = Math.min(p0, p2);
        double max = Math.max(p0, p2);
        if (p1 < min || p1 > max) {
            double d10 = p1 - p0;
            double a = (p2 - p1) - d10;
            double b = 2.0 * d10;
            double t = -b / (2.0 * a);
            if (t > 0.0 && t < 1.0) {
                double v = p0 + t * (b + t * a);
                double margin = Math.ulp(Math.abs(p0) + Math.abs(b) + Math.abs(a));
                min = Math.min(min, v - margin);
                max = Math.max(max, v + margin);
            }
        }
        return new double[] { min, max };
    }

    public static class Double extends TQuadCurve2D {
        public double x1;
        public double y1;
        public double ctrlx;
        public double ctrly;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            setCurve(x1, y1, ctrlx, ctrly, x2, y2);
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public double getCtrlX() {
            return ctrlx;
        }

        @Override
        public double getCtrlY() {
            return ctrly;
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public void setCurve(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static class Float extends TQuadCurve2D {
        public float x1;
        public float y1;
        public float ctrlx;
        public float ctrly;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
            setCurve(x1, y1, ctrlx, ctrly, x2, y2);
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public double getCtrlX() {
            return ctrlx;
        }

        @Override
        public double getCtrlY() {
            return ctrly;
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public void setCurve(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            setCurve((float) x1, (float) y1, (float) ctrlx, (float) ctrly, (float) x2, (float) y2);
        }

        public void setCurve(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
