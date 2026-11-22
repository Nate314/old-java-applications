package web.shim.java.awt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import web.shim.java.awt.geom.TEllipse2D;
import web.shim.java.awt.geom.TGeneralPath;
import web.shim.java.awt.geom.TLine2D;
import web.shim.java.awt.geom.TAffineTransform;
import web.shim.java.awt.geom.TPath2D;
import web.shim.java.awt.geom.TPathIterator;
import web.shim.java.awt.geom.TRectangle2D;
import web.shim.java.awt.geom.TRoundRectangle2D;
import web.shim.java.awt.geom.TPoint2D;
import web.shim.java.awt.geom.TQuadCurve2D;
import web.shim.java.awt.web.ClipRun;

/**
 * Compares the shim's Rectangle, Rectangle2D, RoundRectangle2D, every shape's PathIterator and Path2D.append, Point, Polygon, Line2D, Ellipse2D, QuadCurve2D and Path2D geometry, GridLayout arithmetic (GridRun) and clip arithmetic
 * (ClipRun) with the real JDK's on thousands of random cases, including the overflow and negative-size edges.
 * Run headless (see web/README.md, Components).
 */
public final class GeometryCheck {
    private static final Random RND = new Random(312);
    private static int cases;

    public static void main(String[] args) {
        rectangles();
        polygons();
        quadCurves();
        points();
        lines();
        ellipses();
        paths();
        rectangles2D();
        iterators();
        grids();
        clips();
        System.out.println("GeometryCheck: " + cases + " cases match the JDK");
    }

    private static void rectangles() {
        int[] edge = { Integer.MAX_VALUE - 3, Integer.MIN_VALUE + 2, Integer.MAX_VALUE, 0 };
        for (int i = 0; i < 20000; i++) {
            boolean far = i % 10 == 0;
            int[] a = far ? new int[] { pick(edge), pick(edge), pick(edge), pick(edge) } : smallRect();
            int[] b = far && i % 20 == 0 ? new int[] { pick(edge), pick(edge), pick(edge), pick(edge) } : smallRect();
            Rectangle ja = new Rectangle(a[0], a[1], a[2], a[3]);
            Rectangle jb = new Rectangle(b[0], b[1], b[2], b[3]);
            TRectangle sa = new TRectangle(a[0], a[1], a[2], a[3]);
            TRectangle sb = new TRectangle(b[0], b[1], b[2], b[3]);
            String c = ja + " / " + jb;
            same(ja.intersects(jb), sa.intersects(sb), "intersects " + c);
            same(ja.contains(jb), sa.contains(sb), "contains " + c);
            same(ja.contains(jb.x, jb.y), sa.contains(sb.x, sb.y), "contains point " + c);
            same(ja.isEmpty(), sa.isEmpty(), "isEmpty " + c);
            same(ja.intersection(jb).toString(), sa.intersection(sb).toString(), "intersection " + c);
            same(ja.union(jb).toString(), sa.union(sb).toString(), "union " + c);
            ja.translate(3, -4);
            sa.translate(3, -4);
            same(ja.toString(), sa.toString(), "translate " + c);
            same(ja.equals(new Rectangle(ja)), sa.equals(new TRectangle(sa)), "equals " + c);
        }
    }

    private static int[] smallRect() {
        return new int[] { RND.nextInt(41) - 20, RND.nextInt(41) - 20, RND.nextInt(31) - 5, RND.nextInt(31) - 5 };
    }

    private static int pick(int[] from) {
        return RND.nextInt(3) == 0 ? from[RND.nextInt(from.length)] : RND.nextInt(41) - 20;
    }

    private static void polygons() {
        for (int i = 0; i < 400; i++) {
            int n = RND.nextInt(9);
            int[] xs = new int[n];
            int[] ys = new int[n];
            Polygon jp = new Polygon();
            TPolygon grown = new TPolygon();
            for (int k = 0; k < n; k++) {
                xs[k] = RND.nextInt(31);
                ys[k] = k > 0 && RND.nextInt(5) == 0 ? ys[k - 1] : RND.nextInt(31); // some flat edges
                jp.addPoint(xs[k], ys[k]);
                grown.addPoint(xs[k], ys[k]);
                grown.getBounds(); // bounds kept up to date while points are added
            }
            TPolygon sp = new TPolygon(xs, ys, n);
            String c = "polygon " + i;
            same(jp.getBounds().toString(), sp.getBounds().toString(), "bounds " + c);
            same(jp.getBounds().toString(), grown.getBounds().toString(), "bounds after addPoint " + c);
            for (double y = -2; y <= 32; y += 0.5) {
                for (double x = -2; x <= 32; x += 0.5) {
                    same(jp.contains(x, y), sp.contains(x, y), c + " contains " + x + "," + y);
                    if (x == Math.floor(x) && y == Math.floor(y)) {
                        same(jp.contains((int) x, (int) y), grown.contains((int) x, (int) y), c + " contains int " + x + "," + y);
                    }
                }
            }
            jp.translate(-7, 5);
            sp.translate(-7, 5);
            same(jp.getBounds().toString(), sp.getBounds().toString(), "translate " + c);
        }
    }

    private static void quadCurves() {
        for (int i = 0; i < 5000; i++) {
            double[] p = new double[6];
            for (int k = 0; k < 6; k++) {
                p[k] = i % 3 == 0 ? RND.nextInt(61) - 30 : RND.nextDouble() * 60 - 30;
            }
            Rectangle j = new QuadCurve2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]).getBounds();
            TRectangle s = new TQuadCurve2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]).getBounds();
            same(j.toString(), s.toString(), "quad bounds " + java.util.Arrays.toString(p));
            Rectangle jf = new QuadCurve2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5]).getBounds();
            TRectangle sf = new TQuadCurve2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5]).getBounds();
            same(jf.toString(), sf.toString(), "quad float bounds " + java.util.Arrays.toString(p));
        }
    }

    private static double coord(int i) {
        return i % 3 == 0 ? RND.nextInt(61) - 30 : RND.nextDouble() * 60 - 30;
    }

    private static void points() {
        for (int i = 0; i < 5000; i++) {
            double x = coord(i) * 1.7;
            double y = coord(i) * 1.3;
            Point jp = new Point();
            TPoint sp = new TPoint();
            jp.setLocation(x, y);
            sp.setLocation(x, y);
            same(jp.toString(), sp.toString(), "Point.setLocation " + x + "," + y);
            Point2D.Float jf = new Point2D.Float((float) x, (float) y);
            TPoint2D.Float sf = new TPoint2D.Float((float) x, (float) y);
            same(jf.toString(), sf.toString(), "Point2D.Float " + x + "," + y);
            same(jf.distance(3.25, -7), sf.distance(3.25, -7), "distance " + x + "," + y);
            same(jf.distanceSq(new Point2D.Double(y, x)), sf.distanceSq(new TPoint2D.Double(y, x)), "distanceSq " + x + "," + y);
            same(new Point2D.Double(x, y).toString(), new TPoint2D.Double(x, y).toString(), "Point2D.Double " + x + "," + y);
        }
    }

    private static void lines() {
        for (int i = 0; i < 5000; i++) {
            double[] p = { coord(i), coord(i), coord(i), coord(i) };
            String c = java.util.Arrays.toString(p);
            same(new Line2D.Double(p[0], p[1], p[2], p[3]).getBounds().toString(),
                    new TLine2D.Double(p[0], p[1], p[2], p[3]).getBounds().toString(), "line bounds " + c);
            same(new Line2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]).getBounds().toString(),
                    new TLine2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]).getBounds().toString(), "line float bounds " + c);
            same(new Line2D.Double(p[0], p[1], p[2], p[3]).getP2().toString(),
                    new TLine2D.Double(new TPoint2D.Double(p[0], p[1]), new TPoint2D.Double(p[2], p[3])).getP2().toString(), "line P2 " + c);
        }
    }

    private static void ellipses() {
        for (int i = 0; i < 2000; i++) {
            double[] p = { coord(i), coord(i), coord(i) + 10, coord(i) + 10 }; // some negative sizes
            String c = java.util.Arrays.toString(p);
            Ellipse2D je = new Ellipse2D.Double(p[0], p[1], p[2], p[3]);
            TEllipse2D se = new TEllipse2D.Double(p[0], p[1], p[2], p[3]);
            Ellipse2D jf = new Ellipse2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
            TEllipse2D sf = new TEllipse2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
            same(je.getBounds().toString(), se.getBounds().toString(), "ellipse bounds " + c);
            same(jf.getBounds().toString(), sf.getBounds().toString(), "ellipse float bounds " + c);
            same(je.getCenterX() + "," + je.getCenterY() + "," + je.getMaxX() + "," + je.getMaxY(),
                    se.getCenterX() + "," + se.getCenterY() + "," + se.getMaxX() + "," + se.getMaxY(), "ellipse center " + c);
            for (int k = 0; k < 40; k++) {
                double x = coord(k) * 1.5;
                double y = coord(k) * 1.5;
                same(je.contains(x, y), se.contains(x, y), "ellipse contains " + x + "," + y + " " + c);
                same(jf.contains(x, y), sf.contains(x, y), "ellipse float contains " + x + "," + y + " " + c);
            }
            int[] r = smallRect();
            Rectangle jr = new Rectangle(r[0], r[1], r[2], r[3]);
            TRectangle sr = new TRectangle(r[0], r[1], r[2], r[3]);
            same(jr.getCenterX() + "," + jr.getCenterY() + "," + jr.getMinX() + "," + jr.getMaxY(),
                    sr.getCenterX() + "," + sr.getCenterY() + "," + sr.getMinX() + "," + sr.getMaxY(), "Rectangle center " + jr);
        }
    }

    /** Random paths of moves, lines, quads, cubics and closes, built the same way on both sides. */
    private static void paths() {
        for (int i = 0; i < 5000; i++) {
            int kind = i % 3;
            int rule = RND.nextInt(2);
            Path2D jp = kind == 0 ? new Path2D.Double(rule) : kind == 1 ? new Path2D.Float(rule) : new GeneralPath(rule);
            TPath2D sp = kind == 0 ? new TPath2D.Double(rule) : kind == 1 ? new TPath2D.Float(rule) : new TGeneralPath(rule);
            StringBuilder c = new StringBuilder(jp.getClass().getSimpleName());
            int n = RND.nextInt(7);
            for (int k = 0; k < n; k++) {
                int op = k == 0 ? 0 : RND.nextInt(5);
                double[] p = { coord(i), coord(i), coord(i), coord(i), coord(i), coord(i) };
                c.append(' ').append(op).append(java.util.Arrays.toString(p));
                if (op == 0) {
                    jp.moveTo(p[0], p[1]);
                    sp.moveTo(p[0], p[1]);
                } else if (op == 1) {
                    jp.lineTo(p[0], p[1]);
                    sp.lineTo(p[0], p[1]);
                } else if (op == 2) {
                    jp.quadTo(p[0], p[1], p[2], p[3]);
                    sp.quadTo(p[0], p[1], p[2], p[3]);
                } else if (op == 3) {
                    jp.curveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
                    sp.curveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
                } else {
                    jp.closePath();
                    sp.closePath();
                }
            }
            same(jp.getBounds().toString(), sp.getBounds().toString(), "path bounds " + c);
            same(jp.getWindingRule(), sp.getWindingRule(), "winding rule " + c);
            int segs = 0;
            for (java.awt.geom.PathIterator it = jp.getPathIterator(null); !it.isDone(); it.next()) {
                double[] jc = new double[6];
                same(it.currentSegment(jc), sp.types()[segs], "segment " + segs + " kind " + c);
                segs++;
            }
            same(segs, sp.segments(), "segments " + c);
            jp.reset();
            sp.reset();
            same(jp.getBounds().toString(), sp.getBounds().toString(), "reset path bounds " + c);
        }
    }

    /** Rectangle2D and RoundRectangle2D inside and overlap tests, Rectangle.setRect rounding and Rectangle.contains(Point). */
    private static void rectangles2D() {
        double[] edge = { 2.0 * Integer.MAX_VALUE + 5, Integer.MIN_VALUE - 7.5, Integer.MAX_VALUE - 0.5, -0.0 };
        for (int i = 0; i < 20000; i++) {
            double[] p = { coord(i), coord(i), coord(i) + 10, coord(i) + 10, coord(i) * 2, coord(i) * 2 };
            boolean far = i % 10 == 0;
            if (far) {
                p[RND.nextInt(4)] = edge[RND.nextInt(edge.length)];
            }
            String c = java.util.Arrays.toString(p);
            Rectangle2D jd = new Rectangle2D.Double(p[0], p[1], p[2], p[3]);
            Rectangle2D jf = new Rectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
            TRectangle2D sd = new TRectangle2D.Double(p[0], p[1], p[2], p[3]);
            TRectangle2D sf = new TRectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
            RoundRectangle2D jr = new RoundRectangle2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]);
            TRoundRectangle2D sr = new TRoundRectangle2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]);
            RoundRectangle2D jrf = new RoundRectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5]);
            TRoundRectangle2D srf = new TRoundRectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5]);
            same(jd.isEmpty(), sd.isEmpty(), "rect2d isEmpty " + c);
            same(jf.isEmpty(), sf.isEmpty(), "rect2d float isEmpty " + c);
            if (!far) { // getBounds past the int range is the JDK's own overflow
                same(jd.getBounds().toString(), sd.getBounds().toString(), "rect2d bounds " + c);
                same(jr.getBounds().toString(), sr.getBounds().toString(), "roundrect bounds " + c);
            }
            Rectangle jint = new Rectangle();
            TRectangle sint = new TRectangle();
            jint.setRect(p[0], p[1], p[2], p[3]);
            sint.setRect(p[0], p[1], p[2], p[3]);
            same(jint.toString(), sint.toString(), "Rectangle.setRect " + c);
            for (int k = 0; k < 8; k++) {
                double x = p[0] + RND.nextDouble() * (Math.abs(p[2]) + 4) - 2;
                double y = p[1] + RND.nextDouble() * (Math.abs(p[3]) + 4) - 2;
                if (k == 0) {
                    x = p[0];
                    y = p[1] + p[3];
                }
                double w = RND.nextDouble() * 20 - 2;
                double h = RND.nextDouble() * 20 - 2;
                String at = " at " + x + "," + y + " " + w + "x" + h;
                same(jd.contains(x, y), sd.contains(x, y), "rect2d contains " + c + at);
                same(jf.contains(x, y), sf.contains(x, y), "rect2d float contains " + c + at);
                same(jd.contains(new Point2D.Double(x, y)), sd.contains(new TPoint2D.Double(x, y)), "rect2d contains point " + c + at);
                same(jd.contains(x, y, w, h), sd.contains(x, y, w, h), "rect2d contains rect " + c + at);
                same(jd.intersects(x, y, w, h), sd.intersects(x, y, w, h), "rect2d intersects " + c + at);
                same(jr.contains(x, y), sr.contains(x, y), "roundrect contains " + c + at);
                same(jrf.contains(x, y), srf.contains(x, y), "roundrect float contains " + c + at);
                int ix = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.floor(x)));
                int iy = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.floor(y)));
                same(jint.contains(new Point(ix, iy)), sint.contains(new TPoint(ix, iy)), "Rectangle contains Point " + jint + at);
            }
            same(jd.intersects(jf), sd.intersects(sf), "rect2d intersects rect2d " + c);
        }
    }

    /** Every shape's path iterator (double and float coords, with and without a transform), new Path2D(shape) and append. */
    private static void iterators() {
        AffineTransform jt = new AffineTransform();
        jt.translate(3.25, -7.5);
        TAffineTransform st = new TAffineTransform();
        st.translate(3.25, -7.5);
        for (int i = 0; i < 20000; i++) {
            double[] p = { coord(i), coord(i), coord(i), coord(i), coord(i), coord(i) };
            if (i % 7 != 0) { // mostly a positive size; else a negative one, which draws nothing
                p[2] = Math.abs(p[2]);
                p[3] = Math.abs(p[3]);
            }
            String c = (i % 9) + " " + java.util.Arrays.toString(p);
            java.awt.Shape j;
            TShape s;
            switch (i % 9) {
                case 0:
                    j = new Rectangle2D.Double(p[0], p[1], p[2], p[3]);
                    s = new TRectangle2D.Double(p[0], p[1], p[2], p[3]);
                    break;
                case 1:
                    j = new Rectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
                    s = new TRectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
                    break;
                case 2:
                    j = new RoundRectangle2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]);
                    s = new TRoundRectangle2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]);
                    break;
                case 3:
                    j = new RoundRectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5]);
                    s = new TRoundRectangle2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3], (float) p[4], (float) p[5]);
                    break;
                case 4:
                    j = new Ellipse2D.Double(p[0], p[1], p[2], p[3]);
                    s = new TEllipse2D.Double(p[0], p[1], p[2], p[3]);
                    break;
                case 5:
                    j = new Line2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
                    s = new TLine2D.Float((float) p[0], (float) p[1], (float) p[2], (float) p[3]);
                    break;
                case 6:
                    j = new QuadCurve2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]);
                    s = new TQuadCurve2D.Double(p[0], p[1], p[2], p[3], p[4], p[5]);
                    break;
                case 7: {
                    int n = RND.nextInt(5);
                    int[] xs = new int[n];
                    int[] ys = new int[n];
                    for (int k = 0; k < n; k++) {
                        xs[k] = RND.nextInt(61) - 30;
                        ys[k] = RND.nextInt(61) - 30;
                    }
                    j = new Polygon(xs, ys, n);
                    s = new TPolygon(xs, ys, n);
                    c += " polygon " + java.util.Arrays.toString(xs) + java.util.Arrays.toString(ys);
                    break;
                }
                default:
                    j = new Rectangle((int) p[0], (int) p[1], (int) p[2], (int) p[3]);
                    s = new TRectangle((int) p[0], (int) p[1], (int) p[2], (int) p[3]);
                    break;
            }
            same(walk(j.getPathIterator(null), false), walk(s.getPathIterator(null), false), "path " + c);
            same(walk(j.getPathIterator(null), true), walk(s.getPathIterator(null), true), "float path " + c);
            same(walk(j.getPathIterator(jt), false), walk(s.getPathIterator(st), false), "moved path " + c);
            same(walk(new Path2D.Double(j).getPathIterator(null), false), walk(new TPath2D.Double(s).getPathIterator(null), false), "Path2D of " + c);
            same(walk(new GeneralPath(j).getPathIterator(null), false), walk(new TGeneralPath(s).getPathIterator(null), false), "GeneralPath of " + c);
            // append onto a path that ends at the shape's first point, or somewhere else, or is closed
            Path2D ja = new Path2D.Float();
            TPath2D sa = new TPath2D.Float();
            int how = RND.nextInt(3);
            double[] first = new double[6];
            boolean any = !j.getPathIterator(null).isDone();
            if (any) {
                j.getPathIterator(null).currentSegment(first);
            }
            ja.moveTo(1, 2);
            sa.moveTo(1, 2);
            ja.lineTo(how == 0 && any ? first[0] : 5, how == 0 && any ? first[1] : 6);
            sa.lineTo(how == 0 && any ? first[0] : 5, how == 0 && any ? first[1] : 6);
            if (how == 2) {
                ja.closePath();
                sa.closePath();
            }
            boolean connect = RND.nextBoolean();
            ja.append(j, connect);
            sa.append(s, connect);
            same(walk(ja.getPathIterator(null), false), walk(sa.getPathIterator(null), false), "append " + how + " " + connect + " " + c);
        }
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
            b.append(' ').append(asFloat ? f[k] : d[k]);
        }
    }

    private static void grids() {
        for (int i = 0; i < 3000; i++) {
            int rows = RND.nextInt(5);
            int cols = rows == 0 ? 1 + RND.nextInt(5) : RND.nextInt(5);
            int hgap = RND.nextInt(8);
            int vgap = RND.nextInt(8);
            int n = RND.nextInt(13);
            Insets in = new Insets(RND.nextInt(6), RND.nextInt(6), RND.nextInt(6), RND.nextInt(6));
            Container parent = new Container() {
                @Override
                public Insets getInsets() {
                    return in;
                }
            };
            GridLayout grid = new GridLayout(rows, cols, hgap, vgap);
            parent.setLayout(grid);
            int maxW = 0;
            int maxH = 0;
            for (int k = 0; k < n; k++) {
                Component comp = new Component() {
                };
                Dimension d = new Dimension(RND.nextInt(80), RND.nextInt(40));
                comp.setPreferredSize(d);
                comp.setVisible(RND.nextInt(6) != 0); // GridLayout counts hidden ones too
                maxW = Math.max(maxW, d.width);
                maxH = Math.max(maxH, d.height);
                parent.add(comp);
            }
            String c = "grid " + rows + "x" + cols + " gaps " + hgap + "," + vgap + " n=" + n + " " + in;
            Dimension jd = grid.preferredLayoutSize(parent);
            int[] sd = GridRun.size(n, rows, cols, hgap, vgap, maxW, maxH);
            same(jd.width + "x" + jd.height, (sd[0] + in.left + in.right) + "x" + (sd[1] + in.top + in.bottom), "size " + c);
            int w = RND.nextInt(400);
            int h = RND.nextInt(300);
            parent.setSize(w, h);
            grid.layoutContainer(parent);
            int[] cells = GridRun.cells(n, rows, cols, hgap, vgap, w - in.left - in.right, h - in.top - in.bottom);
            for (int k = 0; k < n; k++) {
                Rectangle b = parent.getComponent(k).getBounds();
                same(b.toString(), new Rectangle(in.left + cells[k * 4], in.top + cells[k * 4 + 1], cells[k * 4 + 2], cells[k * 4 + 3]).toString(),
                        "cell " + k + " of " + c + " in " + w + "x" + h);
            }
        }
    }

    /** getClipBounds after translate/scale/setClip/clipRect steps, and which pixels a fill inside the clip reaches. */
    private static void clips() {
        for (int i = 0; i < 3000; i++) {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            double[] t = { 1, 0, 0, 1, 0, 0 };
            double[] clip = null;
            StringBuilder steps = new StringBuilder();
            int n = 1 + RND.nextInt(5);
            for (int k = 0; k < n; k++) {
                int op = RND.nextInt(4);
                if (op == 0) {
                    int dx = RND.nextInt(21) - 10;
                    int dy = RND.nextInt(21) - 10;
                    g.translate(dx, dy);
                    t[4] += t[0] * dx + t[2] * dy;
                    t[5] += t[1] * dx + t[3] * dy;
                    steps.append(" translate ").append(dx).append(',').append(dy);
                } else if (op == 1 && i % 2 == 0) {
                    double s = RND.nextInt(2) == 0 ? 2 : 0.5;
                    g.scale(s, s);
                    t[0] *= s;
                    t[1] *= s;
                    t[2] *= s;
                    t[3] *= s;
                    steps.append(" scale ").append(s);
                } else {
                    int[] r = { RND.nextInt(61) - 10, RND.nextInt(61) - 10, RND.nextInt(50), RND.nextInt(50) };
                    double[] box = ClipRun.toDevice(t[0], t[1], t[2], t[3], t[4], t[5], r[0], r[1], r[2], r[3]);
                    if (op == 3 && clip != null) {
                        g.clipRect(r[0], r[1], r[2], r[3]);
                        clip = ClipRun.intersect(clip, box);
                        steps.append(" clipRect ");
                    } else {
                        g.setClip(r[0], r[1], r[2], r[3]);
                        clip = box;
                        steps.append(" setClip ");
                    }
                    steps.append(java.util.Arrays.toString(r));
                }
            }
            Rectangle j = g.getClipBounds();
            int[] s = clip == null ? null : ClipRun.userBounds(clip, t[0], t[1], t[2], t[3], t[4], t[5]);
            String c = "clip" + steps;
            if (j == null || s == null) {
                same(j == null, s == null, "no clip " + c);
            } else if (clip[2] <= clip[0] || clip[3] <= clip[1]) {
                same(true, s[2] == 0 && s[3] == 0, "empty clip bounds " + c); // the JDK's box here varies; the pixels are checked below
            } else {
                same(j.toString(), new Rectangle(s[0], s[1], s[2], s[3]).toString(), "getClipBounds " + c);
            }
            // fill everything in device space; the painted pixels must be the clip's pixels
            g.setTransform(new java.awt.geom.AffineTransform());
            g.setColor(Color.RED);
            g.fillRect(-5, -5, 80, 80);
            g.dispose();
            int[] want = clip == null ? new int[] { 0, 0, 64, 64 }
                    : new int[] { clamp(ClipRun.pixel(clip[0], 1)), clamp(ClipRun.pixel(clip[1], 1)), clamp(ClipRun.pixel(clip[2], 1)), clamp(ClipRun.pixel(clip[3], 1)) };
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    boolean in = x >= want[0] && x < want[2] && y >= want[1] && y < want[3];
                    same(in, img.getRGB(x, y) != 0, "pixel " + x + "," + y + " " + c);
                }
            }
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(64, v));
    }

    private static void same(Object want, Object got, String what) {
        cases++;
        if (!want.equals(got)) {
            throw new AssertionError(what + ": JDK " + want + ", shim " + got);
        }
    }
}
