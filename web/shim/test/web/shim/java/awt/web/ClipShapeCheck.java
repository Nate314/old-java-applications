package web.shim.java.awt.web;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Random;
import web.shim.java.awt.TPolygon;
import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;
import web.shim.java.awt.geom.TEllipse2D;
import web.shim.java.awt.geom.TPath2D;
import web.shim.java.awt.geom.TRectangle2D;
import web.shim.java.awt.geom.TRoundRectangle2D;

/**
 * Runs the shim's Graphics2D clip calls (TCanvasGraphics: setClip, clip, clipRect, getClip handed back later, under
 * random translates, turns, scales and slants) next to the JDK's on a BufferedImage, then compares which pixels a fill
 * reaches: the JDK's painted pixels against the shim's clip (pixel centers inside its box and every outline). The canvas
 * smooths clip edges where Java2D does not, and Java2D flattens curves, so a pixel within 1 px of an outline or next to
 * the shim's own edge may differ (at most 1 in 1000); one away from every edge may not.
 */
public final class ClipShapeCheck {
    private static final Random RND = new Random(438);
    private static final int SIZE = 48;
    private static int cases;
    private static int edges;

    public static void main(String[] args) throws Exception {
        Field clipField = TCanvasGraphics.class.getDeclaredField("clip");
        clipField.setAccessible(true);
        int paths = 0;
        for (int i = 0; i < 4000; i++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D j = img.createGraphics();
            j.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE); // the JDK's exact outlines, not nudged to pixels
            TCanvasGraphics s = new TCanvasGraphics(() -> null, 1, null, null);
            Shape jSaved = null;
            TShape sSaved = null;
            StringBuilder steps = new StringBuilder();
            int n = 1 + RND.nextInt(6);
            for (int k = 0; k < n; k++) {
                int op = RND.nextInt(10);
                if (op == 0) {
                    double dx = RND.nextInt(31) - 15 + (RND.nextBoolean() ? 0 : RND.nextDouble());
                    double dy = RND.nextInt(31) - 15 + (RND.nextBoolean() ? 0 : RND.nextDouble());
                    j.translate(dx, dy);
                    s.translate(dx, dy);
                    steps.append(" translate ").append(dx).append(',').append(dy);
                } else if (op == 1) {
                    double a = RND.nextInt(3) == 0 ? Math.PI / 2 * RND.nextInt(4) : (RND.nextDouble() - 0.5) * 4;
                    double x = RND.nextInt(SIZE);
                    double y = RND.nextInt(SIZE);
                    j.rotate(a, x, y);
                    s.rotate(a, x, y);
                    steps.append(" rotate ").append(a).append(" at ").append(x).append(',').append(y);
                } else if (op == 2) {
                    double sx = 0.5 + RND.nextDouble() * 1.5;
                    double sy = RND.nextBoolean() ? sx : 0.5 + RND.nextDouble() * 1.5;
                    j.scale(sx, sy);
                    s.scale(sx, sy);
                    steps.append(" scale ").append(sx).append(',').append(sy);
                } else if (op == 3) {
                    double h = (RND.nextDouble() - 0.5);
                    j.shear(h, 0);
                    s.shear(h, 0);
                    steps.append(" shear ").append(h);
                } else if (op == 4 || op == 5) {
                    Object[] pair = shape();
                    if (op == 4) {
                        j.setClip((Shape) pair[0]);
                        s.setClip((TShape) pair[1]);
                    } else {
                        j.clip((Shape) pair[0]);
                        s.clip((TShape) pair[1]);
                    }
                    steps.append(op == 4 ? " setClip " : " clip ").append(pair[2]);
                } else if (op == 6) {
                    int[] r = { RND.nextInt(50) - 5, RND.nextInt(50) - 5, RND.nextInt(40), RND.nextInt(40) };
                    j.clipRect(r[0], r[1], r[2], r[3]);
                    s.clipRect(r[0], r[1], r[2], r[3]);
                    steps.append(" clipRect ").append(java.util.Arrays.toString(r));
                } else if (op == 7) {
                    jSaved = j.getClip();
                    sSaved = s.getClip();
                    steps.append(" getClip");
                } else if (op == 8 && jSaved != null) {
                    j.setClip(jSaved);
                    s.setClip(sSaved);
                    steps.append(" setClip(saved)");
                } else if (op == 9 && RND.nextInt(4) == 0) {
                    j.setClip(null);
                    s.setClip(null);
                    steps.append(" setClip(null)");
                }
            }
            Rectangle jb = j.getClipBounds();
            Rectangle sb = rect(s.getClipBounds());
            j.setTransform(new AffineTransform());
            j.setColor(Color.RED);
            j.fillRect(-5, -5, SIZE + 10, SIZE + 10);
            j.dispose();
            if (jb == null || sb == null) {
                same(jb == null, sb == null, "no clip bounds" + steps);
            } else if (painted(img) && ((TClipShape) clipField.get(s)).isBox()) {
                // a box clip's bounds are the JDK's; with outlines the JDK's own box is loose (and varies for an empty clip), so only pixels count
                // within a pixel: both round a box mapped back through a turned or uneven scale, and the sums' last bits differ
                same(true, Math.abs(jb.x - sb.x) <= 1 && Math.abs(jb.y - sb.y) <= 1 && Math.abs(jb.width - sb.width) <= 1 && Math.abs(jb.height - sb.height) <= 1,
                        "getClipBounds JDK " + jb + " shim " + sb + steps);
            }
            TClipShape clip = (TClipShape) clipField.get(s);
            if (clip != null && clip.paths != null) {
                paths++;
            }
            boolean[][] want = covered(clip);
            Shape[] near = near(clip);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    boolean got = img.getRGB(x, y) != 0;
                    if (got != want[y][x]) {
                        same(true, edge(want, near, x, y), "pixel " + x + "," + y + " JDK " + got + " away from any edge," + steps);
                        edges++;
                    } else {
                        cases++;
                    }
                }
            }
        }
        same(true, paths > 1000, "cases with outlines: " + paths);
        same(true, edges * 1000L < cases, "edge pixels that differ: " + edges + " of " + cases);
        System.out.println("ClipShapeCheck: " + cases + " cases match the JDK (" + paths + " clips with outlines, " + edges + " edge pixels differ)");
    }

    private static boolean painted(BufferedImage img) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (img.getRGB(x, y) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Rectangle rect(TRectangle r) {
        return r == null ? null : new Rectangle(r.x, r.y, r.width, r.height);
    }

    /** Which device pixels the shim's clip keeps: the center inside the box and inside every outline by its rule. */
    private static boolean[][] covered(TClipShape clip) {
        boolean[][] in = new boolean[SIZE][SIZE];
        Path2D[] outlines = new Path2D[clip == null || clip.paths == null ? 0 : clip.paths.length];
        for (int i = 0; i < outlines.length; i++) {
            outlines[i] = jdk(clip.paths[i]);
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean on = clip == null || x >= ClipRun.pixel(clip.box[0], 1) && x < ClipRun.pixel(clip.box[2], 1)
                        && y >= ClipRun.pixel(clip.box[1], 1) && y < ClipRun.pixel(clip.box[3], 1);
                for (Path2D p : outlines) {
                    on &= p.contains(x + 0.5, y + 0.5);
                }
                in[y][x] = on;
            }
        }
        return in;
    }

    /** Each outline widened to a band 1 px each side: where the JDK's flattened curves and a canvas's smoothing may differ. */
    private static Shape[] near(TClipShape clip) {
        Shape[] out = new Shape[clip == null || clip.paths == null ? 0 : clip.paths.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = new BasicStroke(2f).createStrokedShape(jdk(clip.paths[i]));
        }
        return out;
    }

    private static boolean edge(boolean[][] in, Shape[] near, int x, int y) {
        for (Shape band : near) {
            if (band.contains(x + 0.5, y + 0.5)) {
                return true;
            }
        }
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && in[ny][nx] != in[y][x]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Path2D jdk(TPath2D p) {
        Path2D.Double out = new Path2D.Double(p.getWindingRule());
        double[] c = p.coords();
        for (int i = 0, k = 0; i < p.segments(); i++) {
            switch (p.types()[i]) {
                case 0: out.moveTo(c[k], c[k + 1]); k += 2; break;
                case 1: out.lineTo(c[k], c[k + 1]); k += 2; break;
                case 2: out.quadTo(c[k], c[k + 1], c[k + 2], c[k + 3]); k += 4; break;
                case 3: out.curveTo(c[k], c[k + 1], c[k + 2], c[k + 3], c[k + 4], c[k + 5]); k += 6; break;
                default: out.closePath();
            }
        }
        return out;
    }

    private static double d() {
        return RND.nextInt(3) == 0 ? RND.nextInt(50) - 5 : RND.nextDouble() * 54 - 5;
    }

    /** The same shape for both: { JDK shape, shim shape, description }. */
    private static Object[] shape() {
        double x = d();
        double y = d();
        double w = RND.nextDouble() * 40;
        double h = RND.nextDouble() * 40;
        switch (RND.nextInt(6)) {
            case 0: {
                int ix = (int) x, iy = (int) y, iw = (int) w, ih = (int) h;
                return new Object[] { new Rectangle(ix, iy, iw, ih), new TRectangle(ix, iy, iw, ih), "Rectangle " + ix + "," + iy + " " + iw + "x" + ih };
            }
            case 1:
                return new Object[] { new Rectangle2D.Double(x, y, w, h), new TRectangle2D.Double(x, y, w, h), "Rectangle2D " + x + "," + y + " " + w + "x" + h };
            case 2:
                return new Object[] { new Ellipse2D.Double(x, y, w, h), new TEllipse2D.Double(x, y, w, h), "Ellipse2D " + x + "," + y + " " + w + "x" + h };
            case 3: {
                double a = RND.nextDouble() * 20;
                double b = RND.nextDouble() * 20;
                return new Object[] { new RoundRectangle2D.Double(x, y, w, h, a, b), new TRoundRectangle2D.Double(x, y, w, h, a, b), "RoundRectangle2D " + x + "," + y + " " + w + "x" + h + " " + a + "," + b };
            }
            case 4: {
                int m = 3 + RND.nextInt(5);
                int[] xs = new int[m];
                int[] ys = new int[m];
                for (int i = 0; i < m; i++) {
                    xs[i] = RND.nextInt(60) - 6;
                    ys[i] = RND.nextInt(60) - 6;
                }
                return new Object[] { new Polygon(xs, ys, m), new TPolygon(xs, ys, m), "Polygon " + java.util.Arrays.toString(xs) + java.util.Arrays.toString(ys) };
            }
            default: {
                int rule = RND.nextInt(2);
                Path2D.Double jp = new Path2D.Double(rule);
                TPath2D.Double sp = new TPath2D.Double(rule);
                StringBuilder c = new StringBuilder("Path2D rule " + rule);
                jp.moveTo(x, y);
                sp.moveTo(x, y);
                int m = 2 + RND.nextInt(5);
                for (int i = 0; i < m; i++) {
                    double ax = d(), ay = d(), bx = d(), by = d();
                    if (RND.nextBoolean()) {
                        jp.lineTo(ax, ay);
                        sp.lineTo(ax, ay);
                        c.append(" L").append(ax).append(',').append(ay);
                    } else {
                        jp.quadTo(ax, ay, bx, by);
                        sp.quadTo(ax, ay, bx, by);
                        c.append(" Q").append(ax).append(',').append(ay).append(' ').append(bx).append(',').append(by);
                    }
                }
                jp.closePath();
                sp.closePath();
                return new Object[] { jp, sp, c.toString() };
            }
        }
    }

    private static void same(Object want, Object got, String what) {
        if (!want.equals(got)) {
            throw new AssertionError(what + ": want " + want + ", got " + got);
        }
    }
}
