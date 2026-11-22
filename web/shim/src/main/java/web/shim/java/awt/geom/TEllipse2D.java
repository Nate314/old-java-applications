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
