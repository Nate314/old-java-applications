package web.shim.java.awt.geom;

/**
 * java.awt.geom.Arc2D: an open, chord or pie slice of the ellipse in its frame, angles in degrees counterclockwise from
 * 3 o'clock. Its outline is the JDK's ArcIterator (a cubic per quarter turn or less), so Graphics2D draws and fills it
 * through the path, and contains is the JDK's test.
 */
public abstract class TArc2D extends TRectangularShape {
    public static final int OPEN = 0;
    public static final int CHORD = 1;
    public static final int PIE = 2;

    private int type;

    protected TArc2D(int type) {
        setArcType(type);
    }

    public abstract double getAngleStart();

    public abstract double getAngleExtent();

    public abstract void setArc(double x, double y, double w, double h, double angSt, double angExt, int closure);

    public abstract void setAngleStart(double angSt);

    public abstract void setAngleExtent(double angExt);

    public int getArcType() {
        return type;
    }

    public void setArcType(int type) {
        if (type < OPEN || type > PIE) {
            throw new IllegalArgumentException("invalid type for Arc: " + type);
        }
        this.type = type;
    }

    public void setArc(TArc2D a) {
        setArc(a.getX(), a.getY(), a.getWidth(), a.getHeight(), a.getAngleStart(), a.getAngleExtent(), a.type);
    }

    public void setArcByCenter(double x, double y, double radius, double angSt, double angExt, int closure) {
        setArc(x - radius, y - radius, radius * 2.0, radius * 2.0, angSt, angExt, closure);
    }

    public void setFrame(double x, double y, double w, double h) {
        setArc(x, y, w, h, getAngleStart(), getAngleExtent(), type);
    }

    public void setAngles(double x1, double y1, double x2, double y2) {
        double x = getCenterX();
        double y = getCenterY();
        double w = getWidth();
        double h = getHeight();
        // the y is negated because the angles run counterclockwise on a y-down screen
        double ang1 = Math.atan2(w * (y - y1), h * (x1 - x));
        double ang2 = Math.atan2(w * (y - y2), h * (x2 - x));
        ang2 -= ang1;
        if (ang2 <= 0.0) {
            ang2 += Math.PI * 2.0;
        }
        setAngleStart(Math.toDegrees(ang1));
        setAngleExtent(Math.toDegrees(ang2));
    }

    public boolean isEmpty() {
        return getWidth() <= 0.0 || getHeight() <= 0.0;
    }

    public TPoint2D getStartPoint() {
        return point(-getAngleStart());
    }

    public TPoint2D getEndPoint() {
        return point(-(getAngleStart() + getAngleExtent()));
    }

    private TPoint2D point(double degrees) {
        double angle = Math.toRadians(degrees);
        return new TPoint2D.Double(getX() + (Math.cos(angle) * 0.5 + 0.5) * getWidth(), getY() + (Math.sin(angle) * 0.5 + 0.5) * getHeight());
    }

    /** The JDK's Arc2D.normalizeDegrees: into (-180, 180]. */
    static double normalizeDegrees(double angle) {
        if (angle > 180.0) {
            if (angle <= 180.0 + 360.0) {
                angle = angle - 360.0;
            } else {
                angle = Math.IEEEremainder(angle, 360.0);
                if (angle == -180.0) {
                    angle = 180.0;
                }
            }
        } else if (angle <= -180.0) {
            if (angle > -180.0 - 360.0) {
                angle = angle + 360.0;
            } else {
                angle = Math.IEEEremainder(angle, 360.0);
                if (angle == -180.0) {
                    angle = 180.0;
                }
            }
        }
        return angle;
    }

    public boolean containsAngle(double angle) {
        double angExt = getAngleExtent();
        boolean backwards = angExt < 0.0;
        if (backwards) {
            angExt = -angExt;
        }
        if (angExt >= 360.0) {
            return true;
        }
        angle = normalizeDegrees(angle) - normalizeDegrees(getAngleStart());
        if (backwards) {
            angle = -angle;
        }
        if (angle < 0.0) {
            angle += 360.0;
        }
        return angle >= 0.0 && angle < angExt;
    }

    /** The JDK's Arc2D.contains: inside the ellipse, then inside the slice (pie) or on the arc's side of the chord. */
    public boolean contains(double x, double y) {
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx = (x - getX()) / ellw - 0.5;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy = (y - getY()) / ellh - 0.5;
        if (normx * normx + normy * normy >= 0.25) {
            return false;
        }
        double angExt = Math.abs(getAngleExtent());
        if (angExt >= 360.0) {
            return true;
        }
        boolean inarc = containsAngle(-Math.toDegrees(Math.atan2(normy, normx)));
        if (type == PIE) {
            return inarc;
        }
        if (inarc) {
            if (angExt >= 180.0) {
                return true;
            }
        } else if (angExt <= 180.0) {
            return false;
        }
        double angle = Math.toRadians(-getAngleStart());
        double x1 = Math.cos(angle);
        double y1 = Math.sin(angle);
        angle += Math.toRadians(-getAngleExtent());
        double x2 = Math.cos(angle);
        double y2 = Math.sin(angle);
        boolean inside = relativeCCW(x1, y1, x2, y2, 2 * normx, 2 * normy) * relativeCCW(x1, y1, x2, y2, 0, 0) >= 0;
        return inarc ? !inside : inside;
    }

    public boolean contains(TPoint2D p) {
        return contains(p.getX(), p.getY());
    }

    /** Line2D.relativeCCW. */
    private static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double ccw = px * y2 - py * x2;
        if (ccw == 0.0) {
            ccw = px * x2 + py * y2;
            if (ccw > 0.0) {
                px -= x2;
                py -= y2;
                ccw = px * x2 + py * y2;
                if (ccw < 0.0) {
                    ccw = 0.0;
                }
            }
        }
        return ccw < 0.0 ? -1 : ccw > 0.0 ? 1 : 0;
    }

    /** The JDK's ArcIterator: a move to the start, cubics of at most a quarter turn, then a line to the center (pie) and a close. */
    @Override
    public TPathIterator getPathIterator(TAffineTransform at) {
        TPath2D p = new TPath2D.Double();
        double w = getWidth() / 2;
        double h = getHeight() / 2;
        if (w >= 0 && h >= 0) {
            double x = getX() + w;
            double y = getY() + h;
            double angle = -Math.toRadians(getAngleStart());
            double ext = -getAngleExtent();
            int arcSegs;
            double increment;
            double cv;
            if (ext >= 360.0 || ext <= -360) {
                arcSegs = 4;
                increment = Math.PI / 2;
                cv = 0.5522847498307933;
                if (ext < 0) {
                    increment = -increment;
                    cv = -cv;
                }
            } else {
                arcSegs = (int) Math.ceil(Math.abs(ext) / 90.0);
                increment = Math.toRadians(ext / arcSegs);
                double half = increment / 2.0;
                cv = 4.0 / 3.0 * Math.sin(half) / (1.0 + Math.cos(half));
                if (cv == 0) {
                    arcSegs = 0;
                }
            }
            p.moveTo(x + Math.cos(angle) * w, y + Math.sin(angle) * h);
            for (int i = 0; i < arcSegs; i++) {
                double a = angle + increment * i;
                double relx = Math.cos(a);
                double rely = Math.sin(a);
                double c1x = x + (relx - cv * rely) * w;
                double c1y = y + (rely + cv * relx) * h;
                a += increment;
                relx = Math.cos(a);
                rely = Math.sin(a);
                p.curveTo(c1x, c1y, x + (relx + cv * rely) * w, y + (rely - cv * relx) * h, x + relx * w, y + rely * h);
            }
            if (type == PIE) {
                p.lineTo(x, y);
            }
            if (type != OPEN) {
                p.closePath();
            }
        }
        return p.getPathIterator(at);
    }

    public static class Double extends TArc2D {
        public double x;
        public double y;
        public double width;
        public double height;
        public double start;
        public double extent;

        public Double() {
            this(OPEN);
        }

        public Double(int type) {
            super(type);
        }

        public Double(double x, double y, double w, double h, double start, double extent, int type) {
            super(type);
            setArc(x, y, w, h, start, extent, type);
        }

        public Double(TRectangle2D frame, double start, double extent, int type) {
            this(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight(), start, extent, type);
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
        public double getAngleStart() {
            return start;
        }

        @Override
        public double getAngleExtent() {
            return extent;
        }

        @Override
        public void setArc(double x, double y, double w, double h, double angSt, double angExt, int closure) {
            setArcType(closure);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = angSt;
            this.extent = angExt;
        }

        @Override
        public void setAngleStart(double angSt) {
            start = angSt;
        }

        @Override
        public void setAngleExtent(double angExt) {
            extent = angExt;
        }
    }

    public static class Float extends TArc2D {
        public float x;
        public float y;
        public float width;
        public float height;
        public float start;
        public float extent;

        public Float() {
            this(OPEN);
        }

        public Float(int type) {
            super(type);
        }

        public Float(float x, float y, float w, float h, float start, float extent, int type) {
            super(type);
            setArc(x, y, w, h, start, extent, type);
        }

        public Float(TRectangle2D frame, float start, float extent, int type) {
            this((float) frame.getX(), (float) frame.getY(), (float) frame.getWidth(), (float) frame.getHeight(), start, extent, type);
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
        public double getAngleStart() {
            return start;
        }

        @Override
        public double getAngleExtent() {
            return extent;
        }

        @Override
        public void setArc(double x, double y, double w, double h, double angSt, double angExt, int closure) {
            setArcType(closure);
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
            this.start = (float) angSt;
            this.extent = (float) angExt;
        }

        @Override
        public void setAngleStart(double angSt) {
            start = (float) angSt;
        }

        @Override
        public void setAngleExtent(double angExt) {
            extent = (float) angExt;
        }
    }
}
