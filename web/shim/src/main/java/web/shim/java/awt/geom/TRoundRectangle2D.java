package web.shim.java.awt.geom;

/**
 * java.awt.geom.RoundRectangle2D: corners of arc width x arc height (each at most the side), the JDK's inside test, and
 * the JDK's path (lines and one cubic per corner), which Graphics2D draws and fills.
 */
public abstract class TRoundRectangle2D extends TRectangularShape {
    // RoundRectIterator's corner control value
    private static final double CV;
    static {
        double angle = Math.PI / 4.0;
        double a = 1.0 - Math.cos(angle);
        double b = Math.tan(angle);
        double c = Math.sqrt(1.0 + b * b) - 1 + a;
        CV = 4.0 / 3.0 * a * b / c;
    }
    private static final double ACV = (1.0 - CV) / 2.0;
    // per segment, points of {x share of width, x share of arc width, y share of height, y share of arc height}
    private static final double[][] CTRL = {
        { 0.0, 0.0, 0.0, 0.5 },
        { 0.0, 0.0, 1.0, -0.5 },
        { 0.0, 0.0, 1.0, -ACV, 0.0, ACV, 1.0, 0.0, 0.0, 0.5, 1.0, 0.0 },
        { 1.0, -0.5, 1.0, 0.0 },
        { 1.0, -ACV, 1.0, 0.0, 1.0, 0.0, 1.0, -ACV, 1.0, 0.0, 1.0, -0.5 },
        { 1.0, 0.0, 0.0, 0.5 },
        { 1.0, 0.0, 0.0, ACV, 1.0, -ACV, 0.0, 0.0, 1.0, -0.5, 0.0, 0.0 },
        { 0.0, 0.5, 0.0, 0.0 },
        { 0.0, ACV, 0.0, 0.0, 0.0, 0.0, 0.0, ACV, 0.0, 0.0, 0.0, 0.5 },
    };

    protected TRoundRectangle2D() {
    }

    public abstract double getArcWidth();

    public abstract double getArcHeight();

    public abstract boolean isEmpty();

    public abstract void setRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight);

    public void setRoundRect(TRoundRectangle2D rr) {
        setRoundRect(rr.getX(), rr.getY(), rr.getWidth(), rr.getHeight(), rr.getArcWidth(), rr.getArcHeight());
    }

    public boolean contains(double x, double y) {
        if (isEmpty()) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        double x1 = x0 + getWidth();
        double y1 = y0 + getHeight();
        if (x < x0 || y < y0 || x >= x1 || y >= y1) {
            return false;
        }
        double aw = Math.min(getWidth(), Math.abs(getArcWidth())) / 2.0;
        double ah = Math.min(getHeight(), Math.abs(getArcHeight())) / 2.0;
        // inside the cross between the corners, else inside the nearest corner's ellipse
        if (x >= (x0 += aw) && x < (x0 = x1 - aw)) {
            return true;
        }
        if (y >= (y0 += ah) && y < (y0 = y1 - ah)) {
            return true;
        }
        x = (x - x0) / aw;
        y = (y - y0) / ah;
        return x * x + y * y <= 1.0;
    }

    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        TPath2D p = new TPath2D.Double();
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        double aw = Math.min(w, Math.abs(getArcWidth()));
        double ah = Math.min(h, Math.abs(getArcHeight()));
        if (aw >= 0 && ah >= 0) {
            double[] c = new double[6];
            for (int i = 0; i < CTRL.length; i++) {
                double[] k = CTRL[i];
                for (int j = 0, n = 0; j < k.length; j += 4) {
                    c[n++] = x + k[j] * w + k[j + 1] * aw;
                    c[n++] = y + k[j + 2] * h + k[j + 3] * ah;
                }
                if (i == 0) {
                    p.moveTo(c[0], c[1]);
                } else if (k.length == 4) {
                    p.lineTo(c[0], c[1]);
                } else {
                    p.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]);
                }
            }
            p.closePath();
        }
        return p.getPathIterator(at);
    }

    public static class Double extends TRoundRectangle2D {
        public double x;
        public double y;
        public double width;
        public double height;
        public double arcwidth;
        public double archeight;

        public Double() {
        }

        public Double(double x, double y, double w, double h, double arcw, double arch) {
            setRoundRect(x, y, w, h, arcw, arch);
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
        public double getArcWidth() {
            return arcwidth;
        }

        @Override
        public double getArcHeight() {
            return archeight;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setRoundRect(double x, double y, double w, double h, double arcw, double arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }
    }

    public static class Float extends TRoundRectangle2D {
        public float x;
        public float y;
        public float width;
        public float height;
        public float arcwidth;
        public float archeight;

        public Float() {
        }

        public Float(float x, float y, float w, float h, float arcw, float arch) {
            setRoundRect(x, y, w, h, arcw, arch);
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
        public double getArcWidth() {
            return arcwidth;
        }

        @Override
        public double getArcHeight() {
            return archeight;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        public void setRoundRect(float x, float y, float w, float h, float arcw, float arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }

        @Override
        public void setRoundRect(double x, double y, double w, double h, double arcw, double arch) {
            setRoundRect((float) x, (float) y, (float) w, (float) h, (float) arcw, (float) arch);
        }
    }
}
