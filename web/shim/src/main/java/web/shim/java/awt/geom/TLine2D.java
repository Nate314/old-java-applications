package web.shim.java.awt.geom;

import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;

/** java.awt.geom.Line2D: Graphics2D.draw strokes it with the current stroke; fill draws nothing, as a line has no inside. */
public abstract class TLine2D implements TShape {
    protected TLine2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract void setLine(double x1, double y1, double x2, double y2);

    public void setLine(TPoint2D p1, TPoint2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setLine(TLine2D l) {
        setLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    public TPoint2D getP1() {
        return new TPoint2D.Double(getX1(), getY1());
    }

    public TPoint2D getP2() {
        return new TPoint2D.Double(getX2(), getY2());
    }

    @Override
    public TRectangle getBounds() {
        double x1 = getX1();
        double y1 = getY1();
        double x2 = getX2();
        double y2 = getY2();
        return TRectangularShape.bounds(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public static class Double extends TLine2D {
        public double x1;
        public double y1;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double x2, double y2) {
            setLine(x1, y1, x2, y2);
        }

        public Double(TPoint2D p1, TPoint2D p2) {
            setLine(p1, p2);
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
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static class Float extends TLine2D {
        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(TPoint2D p1, TPoint2D p2) {
            setLine(p1, p2);
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
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            setLine((float) x1, (float) y1, (float) x2, (float) y2);
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        /** The JDK sums the size in floats here. */
        @Override
        public TRectangle getBounds() {
            return TRectangularShape.bounds(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
        }
    }
}
