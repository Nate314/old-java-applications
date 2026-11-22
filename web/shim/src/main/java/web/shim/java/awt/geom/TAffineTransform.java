package web.shim.java.awt.geom;

import web.shim.java.awt.TShape;

/**
 * java.awt.geom.AffineTransform: a 2x3 matrix that translates, rotates, scales and shears with the JDK's sums (rotations
 * by quarter turns are exact, as in the JDK). Graphics2D keeps its transform in one, and path iterators map points through it.
 */
public class TAffineTransform implements Cloneable {
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

    public TAffineTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        setTransform(m00, m10, m01, m11, m02, m12);
    }

    public TAffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        setTransform(m00, m10, m01, m11, m02, m12);
    }

    /** Four values (the scale and shear) or six (with the translation), as the JDK's double[] constructor. */
    public TAffineTransform(double[] m) {
        setTransform(m[0], m[1], m[2], m[3], m.length > 5 ? m[4] : 0, m.length > 5 ? m[5] : 0);
    }

    public static TAffineTransform getTranslateInstance(double tx, double ty) {
        TAffineTransform t = new TAffineTransform();
        t.setToTranslation(tx, ty);
        return t;
    }

    public static TAffineTransform getRotateInstance(double theta) {
        TAffineTransform t = new TAffineTransform();
        t.setToRotation(theta);
        return t;
    }

    public static TAffineTransform getRotateInstance(double theta, double anchorx, double anchory) {
        TAffineTransform t = new TAffineTransform();
        t.setToRotation(theta, anchorx, anchory);
        return t;
    }

    public static TAffineTransform getScaleInstance(double sx, double sy) {
        TAffineTransform t = new TAffineTransform();
        t.setToScale(sx, sy);
        return t;
    }

    public static TAffineTransform getShearInstance(double shx, double shy) {
        TAffineTransform t = new TAffineTransform();
        t.setToShear(shx, shy);
        return t;
    }

    public void setTransform(TAffineTransform t) {
        setTransform(t.m00, t.m10, t.m01, t.m11, t.m02, t.m12);
    }

    public void setTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public void setToIdentity() {
        setTransform(1, 0, 0, 1, 0, 0);
    }

    public void setToTranslation(double tx, double ty) {
        setTransform(1, 0, 0, 1, tx, ty);
    }

    public void setToScale(double sx, double sy) {
        setTransform(sx, 0, 0, sy, 0, 0);
    }

    public void setToShear(double shx, double shy) {
        setTransform(1, shy, shx, 1, 0, 0);
    }

    /** The JDK snaps the sine or cosine to 0 when the other comes out exactly 1 or -1. */
    public void setToRotation(double theta) {
        double sin = Math.sin(theta);
        double cos = Math.cos(theta);
        if (sin == 1.0 || sin == -1.0) {
            cos = 0.0;
        } else if (cos == 1.0 || cos == -1.0) {
            sin = 0.0;
        }
        setTransform(cos, sin, -sin, cos, 0, 0);
    }

    public void setToRotation(double theta, double anchorx, double anchory) {
        setToRotation(theta);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
    }

    public void translate(double tx, double ty) {
        m02 = tx * m00 + ty * m01 + m02;
        m12 = tx * m10 + ty * m11 + m12;
    }

    /** The JDK's rotate: quarter and half turns swap or negate the columns exactly, other angles multiply through. */
    public void rotate(double theta) {
        double sin = Math.sin(theta);
        if (sin == 1.0) {
            quarter(1);
        } else if (sin == -1.0) {
            quarter(-1);
        } else {
            double cos = Math.cos(theta);
            if (cos == -1.0) {
                m00 = -m00;
                m11 = -m11;
                if (m01 != 0.0 || m10 != 0.0) { // the JDK leaves zero shear unnegated (no -0.0)
                    m01 = -m01;
                    m10 = -m10;
                }
            } else if (cos != 1.0) {
                double a = m00;
                double b = m01;
                m00 = cos * a + sin * b;
                m01 = -sin * a + cos * b;
                a = m10;
                b = m11;
                m10 = cos * a + sin * b;
                m11 = -sin * a + cos * b;
            }
        }
    }

    /** rotate90 (sign 1) or rotate270 (sign -1). */
    private void quarter(int sign) {
        double a = m00;
        m00 = sign * m01;
        m01 = -sign * a;
        a = m10;
        m10 = sign * m11;
        m11 = -sign * a;
    }

    public void rotate(double theta, double anchorx, double anchory) {
        translate(anchorx, anchory);
        rotate(theta);
        translate(-anchorx, -anchory);
    }

    public void scale(double sx, double sy) {
        m00 *= sx;
        m10 *= sx;
        m01 *= sy;
        m11 *= sy;
    }

    public void shear(double shx, double shy) {
        double a = m00;
        double b = m01;
        m00 = a + b * shy;
        m01 = a * shx + b;
        a = m10;
        b = m11;
        m10 = a + b * shy;
        m11 = a * shx + b;
    }

    /** this = this x t: t applies first, as Graphics2D.transform does. */
    public void concatenate(TAffineTransform t) {
        double a = m00;
        double b = m01;
        m00 = t.m00 * a + t.m10 * b;
        m01 = t.m01 * a + t.m11 * b;
        m02 += t.m02 * a + t.m12 * b;
        a = m10;
        b = m11;
        m10 = t.m00 * a + t.m10 * b;
        m11 = t.m01 * a + t.m11 * b;
        m12 += t.m02 * a + t.m12 * b;
    }

    /** this = t x this: t applies after. */
    public void preConcatenate(TAffineTransform t) {
        double a = m02;
        double b = m12;
        m02 = a * t.m00 + b * t.m01 + t.m02;
        m12 = a * t.m10 + b * t.m11 + t.m12;
        a = m00;
        b = m10;
        m00 = a * t.m00 + b * t.m01;
        m10 = a * t.m10 + b * t.m11;
        a = m01;
        b = m11;
        m01 = a * t.m00 + b * t.m01;
        m11 = a * t.m10 + b * t.m11;
    }

    public double getDeterminant() {
        return m00 * m11 - m01 * m10;
    }

    public TAffineTransform createInverse() throws TNoninvertibleTransformException {
        double det = getDeterminant();
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) {
            throw new TNoninvertibleTransformException("Determinant is " + det);
        }
        return new TAffineTransform(m11 / det, -m10 / det, -m01 / det, m00 / det,
                (m01 * m12 - m11 * m02) / det, (m10 * m02 - m00 * m12) / det);
    }

    public void invert() throws TNoninvertibleTransformException {
        setTransform(createInverse());
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

    public double getShearX() {
        return m01;
    }

    public double getShearY() {
        return m10;
    }

    /** m00 m10 m01 m11, then m02 m12 when the array holds six. */
    public void getMatrix(double[] m) {
        m[0] = m00;
        m[1] = m10;
        m[2] = m01;
        m[3] = m11;
        if (m.length > 5) {
            m[4] = m02;
            m[5] = m12;
        }
    }

    /** Maps numPts x,y pairs; src and dst may be the same array at the same offset. */
    public void transform(double[] src, int srcOff, double[] dst, int dstOff, int numPts) {
        for (int i = 0; i < numPts * 2; i += 2) {
            double x = src[srcOff + i];
            double y = src[srcOff + i + 1];
            dst[dstOff + i] = m00 * x + m01 * y + m02;
            dst[dstOff + i + 1] = m10 * x + m11 * y + m12;
        }
    }

    public void transform(float[] src, int srcOff, float[] dst, int dstOff, int numPts) {
        for (int i = 0; i < numPts * 2; i += 2) {
            double x = src[srcOff + i];
            double y = src[srcOff + i + 1];
            dst[dstOff + i] = (float) (m00 * x + m01 * y + m02);
            dst[dstOff + i + 1] = (float) (m10 * x + m11 * y + m12);
        }
    }

    /** Maps a point into dst, or a new point of src's kind when dst is null, as the JDK. */
    public TPoint2D transform(TPoint2D src, TPoint2D dst) {
        double x = src.getX();
        double y = src.getY();
        return into(src, dst, m00 * x + m01 * y + m02, m10 * x + m11 * y + m12);
    }

    public TPoint2D deltaTransform(TPoint2D src, TPoint2D dst) {
        double x = src.getX();
        double y = src.getY();
        return into(src, dst, m00 * x + m01 * y, m10 * x + m11 * y);
    }

    public TPoint2D inverseTransform(TPoint2D src, TPoint2D dst) throws TNoninvertibleTransformException {
        return createInverse().transform(src, dst);
    }

    private static TPoint2D into(TPoint2D src, TPoint2D dst, double x, double y) {
        if (dst == null) {
            dst = src instanceof TPoint2D.Double ? new TPoint2D.Double() : new TPoint2D.Float();
        }
        dst.setLocation(x, y);
        return dst;
    }

    /** The shape's outline mapped through this transform, as a Path2D.Double (null stays null). */
    public TShape createTransformedShape(TShape s) {
        return s == null ? null : new TPath2D.Double(s, this);
    }

    public boolean isIdentity() {
        return m00 == 1 && m10 == 0 && m01 == 0 && m11 == 1 && m02 == 0 && m12 == 0;
    }

    @Override
    public Object clone() {
        return new TAffineTransform(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TAffineTransform)) {
            return false;
        }
        TAffineTransform t = (TAffineTransform) o;
        return m00 == t.m00 && m01 == t.m01 && m02 == t.m02 && m10 == t.m10 && m11 == t.m11 && m12 == t.m12;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(new double[] { m00, m01, m02, m10, m11, m12 });
    }
}
