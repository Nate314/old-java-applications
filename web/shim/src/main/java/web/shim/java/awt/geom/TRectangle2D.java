package web.shim.java.awt.geom;

/** java.awt.geom.Rectangle2D (and so Rectangle): the JDK's inside and overlap sums on doubles, and its path. */
public abstract class TRectangle2D extends TRectangularShape {
    protected TRectangle2D() {
    }

    public abstract void setRect(double x, double y, double w, double h);

    public abstract boolean isEmpty();

    public void setRect(TRectangle2D r) {
        setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(double x, double y) {
        double x0 = getX();
        double y0 = getY();
        return x >= x0 && y >= y0 && x < x0 + getWidth() && y < y0 + getHeight();
    }

    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return x >= x0 && y >= y0 && x + w <= x0 + getWidth() && y + h <= y0 + getHeight();
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight();
    }

    public boolean intersects(TRectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /** Move, four lines back to the start, close; nothing for a negative size (the JDK's RectIterator). */
    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        TPath2D p = new TPath2D.Double();
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        if (w >= 0 && h >= 0) {
            p.moveTo(x, y);
            p.lineTo(x + w, y);
            p.lineTo(x + w, y + h);
            p.lineTo(x, y + h);
            p.lineTo(x, y);
            p.closePath();
        }
        return p.getPathIterator(at);
    }

    public static class Double extends TRectangle2D {
        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setRect(x, y, w, h);
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

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }

    public static class Float extends TRectangle2D {
        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            setRect(x, y, w, h);
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

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        public void setRect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public void setRect(double x, double y, double w, double h) {
            setRect((float) x, (float) y, (float) w, (float) h);
        }
    }
}
