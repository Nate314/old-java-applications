package web.shim.java.awt.geom;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import web.shim.java.awt.TShape;

/**
 * Compares the shim's AffineTransform (every operation, chained at random), Arc2D, CubicCurve2D, Path2D's float
 * overloads, transform and createTransformedShape with the real JDK's on random cases. Run headless (see web/README.md).
 */
public final class TransformCheck {
    private static final Random RND = new Random(431);
    private static int cases;

    public static void main(String[] args) throws Exception {
        transforms();
        arcs();
        cubics();
        paths();
        System.out.println("TransformCheck: " + cases + " cases match the JDK");
    }

    private static double coord() {
        switch (RND.nextInt(6)) {
            case 0:
                return RND.nextInt(41) - 20;
            case 1:
                return 0;
            default:
                return (RND.nextDouble() - 0.5) * 400;
        }
    }

    /** Mostly random, sometimes whole quarter turns (the JDK's exact paths) and their float-rounded neighbours. */
    private static double angle() {
        switch (RND.nextInt(4)) {
            case 0:
                return Math.PI / 2 * (RND.nextInt(9) - 4);
            case 1:
                return Math.toRadians(RND.nextInt(721) - 360);
            default:
                return (RND.nextDouble() - 0.5) * 20;
        }
    }

    private static void transforms() throws Exception {
        for (int i = 0; i < 20000; i++) {
            AffineTransform j = new AffineTransform();
            TAffineTransform s = new TAffineTransform();
            StringBuilder c = new StringBuilder();
            int n = 1 + RND.nextInt(6);
            for (int k = 0; k < n; k++) {
                int op = RND.nextInt(16);
                double p = coord();
                double q = coord();
                double a = angle();
                c.append(' ').append(op).append('(').append(p).append(',').append(q).append(',').append(a).append(')');
                switch (op) {
                    case 0:
                        j.translate(p, q);
                        s.translate(p, q);
                        break;
                    case 1:
                        j.rotate(a);
                        s.rotate(a);
                        break;
                    case 2:
                        j.rotate(a, p, q);
                        s.rotate(a, p, q);
                        break;
                    case 3:
                        j.scale(p / 7, q / 7);
                        s.scale(p / 7, q / 7);
                        break;
                    case 4:
                        j.shear(p / 50, q / 50);
                        s.shear(p / 50, q / 50);
                        break;
                    case 5:
                        j.setToRotation(a);
                        s.setToRotation(a);
                        break;
                    case 6:
                        j.setToRotation(a, p, q);
                        s.setToRotation(a, p, q);
                        break;
                    case 7:
                        j.concatenate(AffineTransform.getRotateInstance(a, p, q));
                        s.concatenate(TAffineTransform.getRotateInstance(a, p, q));
                        break;
                    case 8:
                        j.preConcatenate(AffineTransform.getScaleInstance(p, q));
                        s.preConcatenate(TAffineTransform.getScaleInstance(p, q));
                        break;
                    case 9:
                        j.concatenate(AffineTransform.getShearInstance(p / 30, q / 30));
                        s.concatenate(TAffineTransform.getShearInstance(p / 30, q / 30));
                        break;
                    case 10:
                        j.preConcatenate(AffineTransform.getTranslateInstance(p, q));
                        s.preConcatenate(TAffineTransform.getTranslateInstance(p, q));
                        break;
                    case 11:
                        j.setToScale(p, q);
                        s.setToScale(p, q);
                        break;
                    case 12: {
                        double[] m = { p, q, coord(), coord(), coord(), coord() };
                        c.append(Arrays.toString(m));
                        j.concatenate(new AffineTransform(m));
                        s.concatenate(new TAffineTransform(m));
                        break;
                    }
                    case 13:
                        j.preConcatenate(AffineTransform.getRotateInstance(a));
                        s.preConcatenate(TAffineTransform.getRotateInstance(a));
                        break;
                    case 14:
                        j.setToShear(p / 30, q / 30);
                        s.setToShear(p / 30, q / 30);
                        break;
                    default:
                        j.setTransform(new AffineTransform((float) p, (float) q, (float) a, (float) (p / 3), 1.5f, -2.25f));
                        s.setTransform(new TAffineTransform((float) p, (float) q, (float) a, (float) (p / 3), 1.5f, -2.25f));
                        break;
                }
            }
            String what = "transform" + c;
            sameMatrix(j, s, what);
            same(j.getDeterminant() + 0.0, s.getDeterminant() + 0.0, "determinant " + what);
            same(j.isIdentity(), s.isIdentity(), "identity " + what);
            same(j.equals(new AffineTransform(j)), s.equals(s.clone()), "equals " + what);
            Point2D.Double jp = new Point2D.Double(coord(), coord());
            TPoint2D.Double sp = new TPoint2D.Double(jp.x, jp.y);
            same(pt(j.transform(jp, null)), pt(s.transform(sp, null)), "point " + jp + " " + what);
            same(pt(j.deltaTransform(jp, null)), pt(s.deltaTransform(sp, null)), "delta " + jp + " " + what);
            float[] jf = { (float) jp.x, (float) jp.y, 3.5f, -1.25f };
            float[] sf = jf.clone();
            j.transform(jf, 0, jf, 0, 2);
            s.transform(sf, 0, sf, 0, 2);
            for (int k = 0; k < 4; k++) {
                same(jf[k] + 0.0f, sf[k] + 0.0f, "floats " + k + " " + what);
            }
            // the JDK inverts with sums picked by the matrix's kind; the shim's one formula agrees to rounding
            boolean jInv = true;
            boolean sInv = true;
            AffineTransform ji = null;
            TAffineTransform si = null;
            try {
                ji = j.createInverse();
            } catch (NoninvertibleTransformException e) {
                jInv = false;
            }
            try {
                si = s.createInverse();
            } catch (TNoninvertibleTransformException e) {
                sInv = false;
            }
            same(jInv, sInv, "invertible " + what);
            if (jInv) {
                double[] jm = new double[6];
                double[] sm = new double[6];
                ji.getMatrix(jm);
                si.getMatrix(sm);
                for (int k = 0; k < 6; k++) {
                    close(jm[k], sm[k], "inverse " + k + " " + what);
                }
            }
        }
    }

    private static void arcs() {
        for (int i = 0; i < 20000; i++) {
            double[] p = { coord(), coord(), Math.abs(coord()), Math.abs(coord()) };
            if (i % 11 == 0) {
                p[2] = -p[2];
            }
            double start = RND.nextInt(3) == 0 ? 90 * (RND.nextInt(9) - 4) : (RND.nextDouble() - 0.5) * 1000;
            double extent = RND.nextInt(3) == 0 ? 45 * (RND.nextInt(19) - 9) : (RND.nextDouble() - 0.5) * 800;
            int type = RND.nextInt(3);
            boolean asFloat = i % 2 == 1;
            Arc2D j = asFloat ? new Arc2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) start, (float) extent, type)
                    : new Arc2D.Double(p[0], p[1], p[2], p[3], start, extent, type);
            TArc2D s = asFloat ? new TArc2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) start, (float) extent, type)
                    : new TArc2D.Double(p[0], p[1], p[2], p[3], start, extent, type);
            String c = "arc " + asFloat + " " + Arrays.toString(p) + " " + start + " " + extent + " " + type;
            shapes(j, s, c);
            same(j.getBounds().toString(), s.getBounds().toString(), "bounds " + c);
            same(j.isEmpty(), s.isEmpty(), "empty " + c);
            same(pt(j.getStartPoint()), pt(s.getStartPoint()), "start point " + c);
            same(pt(j.getEndPoint()), pt(s.getEndPoint()), "end point " + c);
            for (int k = 0; k < 8; k++) {
                double x = p[0] + RND.nextDouble() * p[2];
                double y = p[1] + RND.nextDouble() * p[3];
                same(j.contains(x, y), s.contains(x, y), "contains " + x + "," + y + " " + c);
                double a = (RND.nextDouble() - 0.5) * 1500;
                same(j.containsAngle(a), s.containsAngle(a), "contains angle " + a + " " + c);
            }
            double[] q = { coord(), coord(), coord(), coord() };
            j.setAngles(q[0], q[1], q[2], q[3]);
            s.setAngles(q[0], q[1], q[2], q[3]);
            same(j.getAngleStart() + " " + j.getAngleExtent(), s.getAngleStart() + " " + s.getAngleExtent(), "set angles " + Arrays.toString(q) + " " + c);
            j.setArcByCenter(q[0], q[1], Math.abs(q[2]), start, extent, (type + 1) % 3);
            s.setArcByCenter(q[0], q[1], Math.abs(q[2]), start, extent, (type + 1) % 3);
            shapes(j, s, "by center " + Arrays.toString(q) + " " + c);
        }
    }

    private static void cubics() {
        for (int i = 0; i < 20000; i++) {
            double[] p = new double[8];
            for (int k = 0; k < 8; k++) {
                p[k] = coord();
            }
            boolean asFloat = i % 2 == 1;
            CubicCurve2D j = asFloat ? new CubicCurve2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5], (float) p[6], (float) p[7])
                    : new CubicCurve2D.Double(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);
            TCubicCurve2D s = asFloat ? new TCubicCurve2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5], (float) p[6], (float) p[7])
                    : new TCubicCurve2D.Double(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7]);
            String c = "cubic " + asFloat + " " + Arrays.toString(p);
            shapes(j, s, c);
            same(j.getBounds().toString(), s.getBounds().toString(), "bounds " + c);
            same(pt(j.getCtrlP2()), pt(s.getCtrlP2()), "ctrl 2 " + c);
        }
    }

    /** Path2D.Float built through its float overloads, then transform and createTransformedShape, against the JDK. */
    private static void paths() {
        for (int i = 0; i < 10000; i++) {
            Path2D.Float j = new Path2D.Float(RND.nextInt(2), 4);
            TPath2D.Float s = new TPath2D.Float(j.getWindingRule(), 4);
            float[] f = new float[6];
            for (int k = 0; k < 6; k++) {
                f[k] = (float) coord();
            }
            j.moveTo(f[0], f[1]);
            s.moveTo(f[0], f[1]);
            j.lineTo(f[2], f[3]);
            s.lineTo(f[2], f[3]);
            j.quadTo(f[4], f[5], f[0], f[3]);
            s.quadTo(f[4], f[5], f[0], f[3]);
            j.curveTo(f[1], f[2], f[3], f[4], f[5], f[0]);
            s.curveTo(f[1], f[2], f[3], f[4], f[5], f[0]);
            j.closePath();
            s.closePath();
            String c = "float path " + Arrays.toString(f);
            same(walk(j.getPathIterator(null), true), walk(s.getPathIterator(null), true), c);
            AffineTransform jt = AffineTransform.getRotateInstance(angle(), coord(), coord());
            jt.scale(1.5, 0.75);
            TAffineTransform st = new TAffineTransform(jt.getScaleX(), jt.getShearY(), jt.getShearX(), jt.getScaleY(), jt.getTranslateX(), jt.getTranslateY());
            c += " " + jt;
            same(walk(j.createTransformedShape(jt).getPathIterator(null), false), walk(((TShape) s.createTransformedShape(st)).getPathIterator(null), false), "transformed " + c);
            j.transform(jt);
            s.transform(st);
            same(walk(j.getPathIterator(null), false), walk(s.getPathIterator(null), false), "transform " + c);
            same(j.getBounds().toString(), s.getBounds().toString(), "transformed bounds " + c);
            Ellipse2D je = new Ellipse2D.Double(f[0], f[1], Math.abs(f[2]), Math.abs(f[3]));
            TEllipse2D se = new TEllipse2D.Double(f[0], f[1], Math.abs(f[2]), Math.abs(f[3]));
            same(walk(jt.createTransformedShape(je).getPathIterator(null), false), walk(st.createTransformedShape(se).getPathIterator(null), false), "ellipse " + c);
            same(walk(new Path2D.Double(je, jt).getPathIterator(null), false), walk(new TPath2D.Double(se, st).getPathIterator(null), false), "Path2D.Double(ellipse, at) " + c);
        }
    }

    /** Path walks: double and float coordinates, and mapped through a transform. */
    private static void shapes(Shape j, TShape s, String c) {
        same(walk(j.getPathIterator(null), false), walk(s.getPathIterator(null), false), "path " + c);
        same(walk(j.getPathIterator(null), true), walk(s.getPathIterator(null), true), "float path " + c);
        AffineTransform jt = AffineTransform.getRotateInstance(0.3, 5, -2);
        TAffineTransform st = TAffineTransform.getRotateInstance(0.3, 5, -2);
        same(walk(j.getPathIterator(jt), false), walk(s.getPathIterator(st), false), "turned path " + c);
    }

    private static String walk(PathIterator it, boolean asFloat) {
        StringBuilder b = new StringBuilder("rule " + it.getWindingRule());
        for (; !it.isDone(); it.next()) {
            double[] d = new double[6];
            float[] f = new float[6];
            segment(b, asFloat ? it.currentSegment(f) : it.currentSegment(d), d, f, asFloat);
        }
        return b.toString();
    }

    private static String walk(TPathIterator it, boolean asFloat) {
        StringBuilder b = new StringBuilder("rule " + it.getWindingRule());
        for (; !it.isDone(); it.next()) {
            double[] d = new double[6];
            float[] f = new float[6];
            segment(b, asFloat ? it.currentSegment(f) : it.currentSegment(d), d, f, asFloat);
        }
        return b.toString();
    }

    private static void segment(StringBuilder b, int type, double[] d, float[] f, boolean asFloat) {
        b.append(" | ").append(type);
        for (int k = 0; k < (type == 4 ? 0 : type == 3 ? 6 : type == 2 ? 4 : 2); k++) {
            b.append(' ').append(asFloat ? f[k] + 0.0f : d[k] + 0.0); // + 0.0: as in pt
        }
    }

    // + 0.0: the JDK skips zero terms for some matrix kinds and so may give -0.0 where the shim gives 0.0; both draw the same
    private static String pt(Point2D p) {
        return p.getClass().getSimpleName() + " " + (p.getX() + 0.0) + " " + (p.getY() + 0.0);
    }

    private static String pt(TPoint2D p) {
        return p.getClass().getSimpleName() + " " + (p.getX() + 0.0) + " " + (p.getY() + 0.0);
    }

    private static void sameMatrix(AffineTransform j, TAffineTransform s, String what) {
        double[] jm = new double[6];
        double[] sm = new double[6];
        j.getMatrix(jm);
        s.getMatrix(sm);
        for (int k = 0; k < 6; k++) {
            cases++;
            if (!(jm[k] == sm[k] || (jm[k] != jm[k] && sm[k] != sm[k]))) {
                throw new AssertionError(what + ": JDK " + Arrays.toString(jm) + ", shim " + Arrays.toString(sm));
            }
        }
    }

    private static void close(double want, double got, String what) {
        cases++;
        if (!(Math.abs(want - got) <= 1e-9 * Math.max(1, Math.max(Math.abs(want), Math.abs(got))))) {
            throw new AssertionError(what + ": JDK " + want + ", shim " + got);
        }
    }

    private static void same(Object want, Object got, String what) {
        cases++;
        if (!want.equals(got)) {
            throw new AssertionError(what + ": JDK " + want + ", shim " + got);
        }
    }
}
