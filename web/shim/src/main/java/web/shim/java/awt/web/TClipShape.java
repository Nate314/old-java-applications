package web.shim.java.awt.web;

import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TShape;
import web.shim.java.awt.geom.TAffineTransform;
import web.shim.java.awt.geom.TNoninvertibleTransformException;
import web.shim.java.awt.geom.TPath2D;
import web.shim.java.awt.geom.TPathIterator;
import web.shim.java.awt.geom.TRectangle2D;

/**
 * A Graphics clip in device space (CSS pixels, before the pixel ratio): a box (ClipRun) and, for clips that are not
 * upright rectangles, the shapes' outlines it is also cut to. A pixel is inside when its center is inside the box and
 * every outline. Free of canvas calls, so ClipShapeCheck can compare it with the JDK's Graphics2D on the desktop. Named T like
 * the AWT stand-ins, so TeaVM's shim substitution finds its methods (under a plain name, app builds failed with "method not found").
 * getClip hands one out seen from the graphics' transform at that moment, so setClip gives the same pixels back later.
 */
public final class TClipShape implements TShape {
    final double[] box; // x0, y0, x1, y1
    final TPath2D[] paths; // null for a box alone
    private final TAffineTransform at; // user space -> device space, for getClip's user-space view

    private TClipShape(double[] box, TPath2D[] paths, TAffineTransform at) {
        this.box = box;
        this.paths = paths;
        this.at = at;
    }

    /** Graphics.setClip(shape) under t (null drops the clip). */
    public static TClipShape set(TShape s, TAffineTransform t) {
        if (s == null) {
            return null;
        }
        if (s instanceof TClipShape) {
            return ((TClipShape) s).under(t);
        }
        if (s instanceof TRectangle2D && upright(t)) {
            TRectangle2D r = (TRectangle2D) s;
            return new TClipShape(device(t, r.getX(), r.getY(), r.getWidth(), r.getHeight()), null, null);
        }
        TPath2D p = new TPath2D.Double(s, t);
        TRectangle b = p.getBounds();
        return new TClipShape(new double[] { b.x, b.y, b.x + b.width, b.y + b.height }, new TPath2D[] { p }, null);
    }

    /** Graphics2D.clip(shape): the overlap of the clip and the shape under t (null drops the clip, as in the JDK). */
    public static TClipShape narrow(TClipShape clip, TShape s, TAffineTransform t) {
        TClipShape add = set(s, t);
        if (clip == null || add == null) {
            return add;
        }
        TPath2D[] paths = clip.paths;
        if (add.paths != null) {
            int n = paths == null ? 0 : paths.length;
            TPath2D[] both = new TPath2D[n + add.paths.length];
            if (n > 0) {
                System.arraycopy(paths, 0, both, 0, n);
            }
            System.arraycopy(add.paths, 0, both, n, add.paths.length);
            paths = both;
        }
        return new TClipShape(ClipRun.intersect(clip.box, add.box), paths, null);
    }

    /** The same pixels, as getClip returns them to a graphics with transform t. */
    public TClipShape seenFrom(TAffineTransform t) {
        return new TClipShape(box, paths, new TAffineTransform(t));
    }

    /** The clip's box in user space under t, rounded out (getClipBounds), or null for a flat transform. */
    public int[] userBounds(TAffineTransform t) {
        return ClipRun.userBounds(box, t.getScaleX(), t.getShearY(), t.getShearX(), t.getScaleY(), t.getTranslateX(), t.getTranslateY());
    }

    /** This clip handed back to a graphics whose transform is now t: its user-space region, mapped through t. */
    private TClipShape under(TAffineTransform t) {
        TAffineTransform m = new TAffineTransform(t);
        if (at != null) {
            try {
                m.concatenate(at.createInverse());
            } catch (TNoninvertibleTransformException e) {
                return this; // shortcut: a clip taken under a flat transform comes back as it was in device space, as no app does this yet
            }
        }
        if (m.isIdentity()) {
            return new TClipShape(box, paths, null);
        }
        boolean upright = upright(m);
        int n = paths == null ? 0 : paths.length;
        TPath2D[] moved = new TPath2D[n + (upright ? 0 : 1)];
        for (int i = 0; i < n; i++) {
            moved[i] = new TPath2D.Double(paths[i], m);
        }
        if (!upright) { // the box turns into a slanted outline of its own
            moved[n] = new TPath2D.Double(new TRectangle2D.Double(box[0], box[1], box[2] - box[0], box[3] - box[1]), m);
        }
        return new TClipShape(device(m, box[0], box[1], box[2] - box[0], box[3] - box[1]), moved.length == 0 ? null : moved, null);
    }

    /** Whether the clip is a box alone (no outlines). */
    public boolean isBox() {
        return paths == null;
    }

    /** A transform that keeps rectangles upright (no turn but quarter turns, no slant), so a rectangle clip stays a box. */
    public static boolean upright(TAffineTransform t) {
        return t.getShearX() == 0 && t.getShearY() == 0 || t.getScaleX() == 0 && t.getScaleY() == 0;
    }

    private static double[] device(TAffineTransform t, double x, double y, double w, double h) {
        return ClipRun.toDevice(t.getScaleX(), t.getShearY(), t.getShearX(), t.getScaleY(), t.getTranslateX(), t.getTranslateY(), x, y, w, h);
    }

    @Override
    public TRectangle getBounds() {
        int[] r = userBounds(at == null ? new TAffineTransform() : at);
        return r == null ? new TRectangle() : new TRectangle(r[0], r[1], r[2], r[3]);
    }

    // shortcut: walks the last shape clipped to (or the box), the exact outline when one shape made the clip; an outline of several overlapping shapes needs Area
    @Override
    public TPathIterator getPathIterator(TAffineTransform extra) {
        TShape s = paths == null ? new TRectangle2D.Double(box[0], box[1], box[2] - box[0], box[3] - box[1]) : paths[paths.length - 1];
        TAffineTransform m = new TAffineTransform();
        if (extra != null) {
            m.setTransform(extra);
        }
        if (at != null) {
            try {
                m.concatenate(at.createInverse());
            } catch (TNoninvertibleTransformException e) {
                // a flat transform has no user space to show it in; walk it in device space
            }
        }
        return new TPath2D.Double(s, m).getPathIterator(null);
    }
}
