package web.shim.java.awt.web;

/**
 * Clip arithmetic for TCanvasGraphics, free of shim types so ClipCheck can run it on the desktop. A clip is kept as
 * a device box {x0, y0, x1, y1} under the transform x' = a*x + c*y + e, y' = b*x + d*y + f.
 */
public final class ClipRun {
    private ClipRun() {
    }

    // a box keeps its corners only: TClipShape adds the slanted outline when the transform turns or slants it
    public static double[] toDevice(double a, double b, double c, double d, double e, double f, double x, double y, double w, double h) {
        double[] out = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (int i = 0; i < 4; i++) {
            double px = (i & 1) == 0 ? x : x + w;
            double py = (i & 2) == 0 ? y : y + h;
            grow(out, a * px + c * py + e, b * px + d * py + f);
        }
        return out;
    }

    /** The overlap of two boxes; no overlap is an empty box at the far edge, as AWT's Rectangle2D.intersect gives. */
    public static double[] intersect(double[] p, double[] q) {
        double x0 = Math.max(p[0], q[0]);
        double y0 = Math.max(p[1], q[1]);
        return new double[] { x0, y0, Math.max(x0, Math.min(p[2], q[2])), Math.max(y0, Math.min(p[3], q[3])) };
    }

    /** The device box back in user space, rounded out to whole pixels (Graphics.getClipBounds), or null for a flat transform. */
    public static int[] userBounds(double[] box, double a, double b, double c, double d, double e, double f) {
        double det = a * d - b * c;
        if (det == 0) {
            return null;
        }
        double[] out = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        for (int i = 0; i < 4; i++) {
            double px = ((i & 1) == 0 ? box[0] : box[2]) - e;
            double py = ((i & 2) == 0 ? box[1] : box[3]) - f;
            grow(out, (d * px - c * py) / det, (a * py - b * px) / det);
        }
        int x = (int) Math.floor(out[0]);
        int y = (int) Math.floor(out[1]);
        if (box[2] <= box[0] || box[3] <= box[1]) {
            return new int[] { x, y, 0, 0 }; // nothing left to draw in; the JDK's box for this varies, but is never usable
        }
        return new int[] { x, y, (int) Math.ceil(out[2]) - x, (int) Math.ceil(out[3]) - y };
    }

    /** First and last+1 device pixel inside a box edge pair: a pixel is in when its center is, as in Java2D. */
    public static int pixel(double edge, double pixelRatio) {
        return (int) Math.ceil(edge * pixelRatio - 0.5);
    }

    private static void grow(double[] box, double x, double y) {
        box[0] = Math.min(box[0], x);
        box[1] = Math.min(box[1], y);
        box[2] = Math.max(box[2], x);
        box[3] = Math.max(box[3], y);
    }
}
