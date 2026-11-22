package web.shim.java.awt.geom;

import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;

/** java.awt.geom.CubicCurve2D: Graphics2D.draw strokes it and fill fills it up to the chord, through its path, as in Java2D. */
public abstract class TCubicCurve2D implements TShape {
    protected TCubicCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getCtrlX1();

    public abstract double getCtrlY1();

    public abstract double getCtrlX2();

    public abstract double getCtrlY2();

    public abstract double getX2();

    public abstract double getY2();

    public abstract void setCurve(double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2, double ctrly2, double x2, double y2);

    public void setCurve(TCubicCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(), c.getCtrlX2(), c.getCtrlY2(), c.getX2(), c.getY2());
    }

    public TPoint2D getP1() {
        return point(getX1(), getY1());
    }

    public TPoint2D getCtrlP1() {
        return point(getCtrlX1(), getCtrlY1());
    }

    public TPoint2D getCtrlP2() {
        return point(getCtrlX2(), getCtrlY2());
    }

    public TPoint2D getP2() {
        return point(getX2(), getY2());
    }

    /** A point of the curve's own precision, as the JDK's getP1 and the like return. */
    TPoint2D point(double x, double y) {
        return new TPoint2D.Double(x, y);
    }

    private TPath2D path() {
        TPath2D p = new TPath2D.Double();
        p.moveTo(getX1(), getY1());
        p.curveTo(getCtrlX1(), getCtrlY1(), getCtrlX2(), getCtrlY2(), getX2(), getY2());
        return p;
    }

    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        return path().getPathIterator(at);
    }

    /** The curve's own box with the JDK's turning-point sums (Path2D's), rounded out to whole pixels. */
    @Override
    public TRectangle getBounds() {
        return path().getBounds();
    }

    public static class Double extends TCubicCurve2D {
        public double x1;
        public double y1;
        public double ctrlx1;
        public double ctrly1;
        public double ctrlx2;
        public double ctrly2;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2, double ctrly2, double x2, double y2) {
            setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
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
        public double getCtrlX1() {
            return ctrlx1;
        }

        @Override
        public double getCtrlY1() {
            return ctrly1;
        }

        @Override
        public double getCtrlX2() {
            return ctrlx2;
        }

        @Override
        public double getCtrlY2() {
            return ctrly2;
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
        public void setCurve(double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2, double ctrly2, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static class Float extends TCubicCurve2D {
        public float x1;
        public float y1;
        public float ctrlx1;
        public float ctrly1;
        public float ctrlx2;
        public float ctrly2;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float ctrlx1, float ctrly1, float ctrlx2, float ctrly2, float x2, float y2) {
            setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
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
        public double getCtrlX1() {
            return ctrlx1;
        }

        @Override
        public double getCtrlY1() {
            return ctrly1;
        }

        @Override
        public double getCtrlX2() {
            return ctrlx2;
        }

        @Override
        public double getCtrlY2() {
            return ctrly2;
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
        public void setCurve(double x1, double y1, double ctrlx1, double ctrly1, double ctrlx2, double ctrly2, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.ctrlx1 = (float) ctrlx1;
            this.ctrly1 = (float) ctrly1;
            this.ctrlx2 = (float) ctrlx2;
            this.ctrly2 = (float) ctrly2;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        @Override
        TPoint2D point(double x, double y) {
            return new TPoint2D.Float((float) x, (float) y);
        }

        public void setCurve(float x1, float y1, float ctrlx1, float ctrly1, float ctrlx2, float ctrly2, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
