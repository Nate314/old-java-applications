package web.shim.java.awt.geom;

/** Holds a 2x3 matrix. FlappyBird only creates and copies these; Graphics2D does not consume them. */
public class TAffineTransform {
    private double m00 = 1;
    private double m10;
    private double m01;
    private double m11 = 1;
    private double m02;
    private double m12;

    public TAffineTransform() {
    }

    public TAffineTransform(TAffineTransform t) {
        setTransform(t);
    }

    public void setTransform(TAffineTransform t) {
        m00 = t.m00;
        m10 = t.m10;
        m01 = t.m01;
        m11 = t.m11;
        m02 = t.m02;
        m12 = t.m12;
    }

    public void setToIdentity() {
        m00 = 1;
        m10 = 0;
        m01 = 0;
        m11 = 1;
        m02 = 0;
        m12 = 0;
    }

    public void translate(double tx, double ty) {
        m02 += tx * m00 + ty * m01;
        m12 += tx * m10 + ty * m11;
    }

    public double getTranslateX() {
        return m02;
    }

    public double getTranslateY() {
        return m12;
    }

    public double getScaleX() {
        return m00;
    }

    public double getScaleY() {
        return m11;
    }

    public boolean isIdentity() {
        return m00 == 1 && m10 == 0 && m01 == 0 && m11 == 1 && m02 == 0 && m12 == 0;
    }
}
