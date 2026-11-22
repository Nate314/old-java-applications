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
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import web.shim.java.awt.geom.TQuadCurve2D;
import web.shim.java.awt.web.ClipRun;

/**
 * Compares the shim's Rectangle, Polygon, QuadCurve2D bounds, GridLayout arithmetic (GridRun) and clip arithmetic
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
