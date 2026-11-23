package web.shim.java.awt.geom;

/** java.awt.geom.Ellipse2D: Graphics2D.draw and fill draw the ellipse inside its frame, as fillOval does. */
public abstract class TEllipse2D extends TRectangularShape {
    protected TEllipse2D() {
    }

    public abstract void setFrame(double x, double y, double w, double h);

    /** Strictly inside, the JDK's sums. */
    public boolean contains(double x, double y) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        double nx = (x - getX()) / w - 0.5;
        double ny = (y - getY()) / h - 0.5;
        return nx * nx + ny * ny < 0.25;
    }

    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    // EllipseIterator's control values: per quarter, three x,y points as shares of the frame
    private static final double PCV = 0.5 + 0.5522847498307933 * 0.5;
    private static final double NCV = 0.5 - 0.5522847498307933 * 0.5;
    private static final double[][] CTRL = {
        { 1.0, PCV, PCV, 1.0, 0.5, 1.0 },
        { NCV, 1.0, 0.0, PCV, 0.0, 0.5 },
        { 0.0, NCV, NCV, 0.0, 0.5, 0.0 },
        { PCV, 0.0, 1.0, NCV, 1.0, 0.5 },
    };

    /** Four cubics from the right middle, as the JDK's EllipseIterator; nothing for a negative size. */
    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        TPath2D p = new TPath2D.Double();
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        if (w >= 0 && h >= 0) {
            p.moveTo(x + w, y + h * 0.5);
            for (double[] k : CTRL) {
                p.curveTo(x + k[0] * w, y + k[1] * h, x + k[2] * w, y + k[3] * h, x + k[4] * w, y + k[5] * h);
            }
            p.closePath();
        }
        return p.getPathIterator(at);
    }

    public static class Double extends TEllipse2D {
        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setFrame(x, y, w, h);
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
        public void setFrame(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }

    public static class Float extends TEllipse2D {
        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            setFrame(x, y, w, h);
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
        public void setFrame(double x, double y, double w, double h) {
            setFrame((float) x, (float) y, (float) w, (float) h);
        }

        public void setFrame(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }
}
