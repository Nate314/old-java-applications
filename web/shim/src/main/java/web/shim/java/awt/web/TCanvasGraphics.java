package web.shim.java.awt.web;

import web.rt.Edt;
import web.rt.Exit;
import web.rt.Js;
import web.rt.Keys;

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import web.shim.java.awt.TBasicStroke;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TGraphics;
import web.shim.java.awt.TGradientPaint;
import web.shim.java.awt.TGraphics2D;
import web.shim.java.awt.TImage;
import web.shim.java.awt.TPaint;
import web.shim.java.awt.TPolygon;
import web.shim.java.awt.TRectangle;
import web.shim.java.awt.TRenderingHints;
import web.shim.java.awt.TShape;
import web.shim.java.awt.TStroke;
import web.shim.java.awt.geom.TQuadCurve2D;
import web.shim.java.awt.image.TImageObserver;

/**
 * Graphics2D over a CanvasRenderingContext2D. The canvas is created lazily on the first drawing call, so
 * components whose paint() draws nothing never get one. Coordinates are CSS pixels; the backing store is
 * scaled by Js.pixelRatio() (devicePixelRatio, times the zoom while the page desktop is zoomed up).
 */
public final class TCanvasGraphics extends TGraphics2D {
    /** Supplies the 2D context on first use (creating and sizing the canvas). */
    public interface Surface {
        JSObject context();
    }

    private static final TFont DEFAULT_FONT = new TFont("Dialog", TFont.PLAIN, 12);

    private final Surface surface;
    private final double pixelRatio;
    private JSObject ctx;
    // User-space transform: x' = a*x + c*y + e, y' = b*x + d*y + f
    private double a = 1;
    private double b;
    private double c;
    private double d = 1;
    private double e;
    private double f;
    private TColor color = TColor.black;
    private TFont font = DEFAULT_FONT;
    private TStroke stroke = new TBasicStroke();
    private TGradientPaint gradient; // null while painting with color
    private JSObject gradientStyle;
    private JSObject gradientCtx;
    private double[] clip; // device box (ClipRun), before the pixel ratio; null for none
    private final Map<Object, Object> hints;

    // The context state last written, shared by all graphics objects, to avoid redundant canvas calls.
    private static JSObject lastCtx;
    private static TCanvasGraphics lastOwner;
    private static int version;
    private int appliedVersion = -1;
    private int myVersion;

    public TCanvasGraphics(Surface surface, double pixelRatio, TColor color, TFont font) {
        this.surface = surface;
        this.pixelRatio = pixelRatio;
        if (color != null) {
            this.color = color;
        }
        if (font != null) {
            this.font = font;
        }
        this.hints = new HashMap<>();
    }

    private TCanvasGraphics(TCanvasGraphics o) {
        surface = o.surface;
        pixelRatio = o.pixelRatio;
        ctx = o.ctx;
        a = o.a;
        b = o.b;
        c = o.c;
        d = o.d;
        e = o.e;
        f = o.f;
        color = o.color;
        font = o.font;
        stroke = o.stroke;
        gradient = o.gradient;
        clip = o.clip;
        hints = new HashMap<>(o.hints);
    }

    public static TCanvasGraphics forCanvas(JSObject canvas, double pixelRatio) {
        JSObject context = Js.context2d(canvas);
        return new TCanvasGraphics(() -> context, pixelRatio, null, null);
    }

    private JSObject ctx() {
        Js.painted();
        if (ctx == null) {
            ctx = surface.context();
        }
        if (lastCtx != ctx || lastOwner != this || appliedVersion != myVersion) {
            lastCtx = ctx;
            lastOwner = this;
            appliedVersion = myVersion;
            double r = pixelRatio;
            unclip(ctx); // a canvas clip only comes off by restore(), which also resets the transform and line set below
            if (clip != null) {
                int x0 = ClipRun.pixel(clip[0], r);
                int y0 = ClipRun.pixel(clip[1], r);
                clipTo(ctx, x0, y0, ClipRun.pixel(clip[2], r) - x0, ClipRun.pixel(clip[3], r) - y0);
            }
            setTransform(ctx, r * a, r * b, r * c, r * d, r * e, r * f);
            applyStroke();
        }
        return ctx;
    }

    private void changed() {
        myVersion = ++version;
    }

    /** Takes off a clip some graphics left on this context (TComponent clears canvases through it). */
    public static void release(JSObject context) {
        unclip(context);
        lastCtx = null;
    }

    /** The current paint as a canvas fillStyle/strokeStyle: the color's CSS string, or the gradient. */
    private JSObject style() {
        if (gradient == null) {
            return JSString.valueOf(color.css());
        }
        if (gradientCtx != ctx) {
            gradientCtx = ctx;
            // Java2D works the color out at each pixel's corner, the canvas at its center: move the line half a pixel to match
            gradientStyle = linearGradient(ctx, gradient.x1() + 0.5, gradient.y1() + 0.5, gradient.x2() + 0.5, gradient.y2() + 0.5,
                    gradient.getColor1().css(), gradient.getColor2().css(), gradient.isCyclic());
        }
        return gradientStyle;
    }

    private void applyStroke() {
        if (stroke instanceof TBasicStroke) {
            TBasicStroke s = (TBasicStroke) stroke;
            setLine(ctx, Math.max(s.getLineWidth(), 1f / (float) pixelRatio),
                    s.getEndCap() == TBasicStroke.CAP_BUTT ? "butt" : s.getEndCap() == TBasicStroke.CAP_ROUND ? "round" : "square",
                    s.getLineJoin() == TBasicStroke.JOIN_ROUND ? "round" : s.getLineJoin() == TBasicStroke.JOIN_BEVEL ? "bevel" : "miter");
        }
    }

    /** Java strokes 1px-wide lines through pixel centers; canvas needs a half-pixel shift for odd widths. */
    private double strokeOffset() {
        float w = stroke instanceof TBasicStroke ? ((TBasicStroke) stroke).getLineWidth() : 1f;
        return Math.round(Math.max(w, 1f)) % 2 == 1 ? 0.5 : 0;
    }

    @Override
    public TGraphics create() {
        return new TCanvasGraphics(this);
    }

    @Override
    public void translate(int x, int y) {
        translate((double) x, (double) y);
    }

    @Override
    public void translate(double tx, double ty) {
        e += a * tx + c * ty;
        f += b * tx + d * ty;
        changed();
    }

    @Override
    public void rotate(double theta) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double na = a * cos + c * sin;
        double nb = b * cos + d * sin;
        double nc = -a * sin + c * cos;
        double nd = -b * sin + d * cos;
        a = na;
        b = nb;
        c = nc;
        d = nd;
        changed();
    }

    @Override
    public void scale(double sx, double sy) {
        a *= sx;
        b *= sx;
        c *= sy;
        d *= sy;
        changed();
    }

    @Override
    public TColor getColor() {
        return color;
    }

    @Override
    public void setColor(TColor c) {
        if (c != null) {
            color = c;
            gradient = null;
        }
    }

    @Override
    public void setPaint(TPaint paint) {
        if (paint instanceof TColor) {
            setColor((TColor) paint);
        } else if (paint instanceof TGradientPaint) {
            gradient = (TGradientPaint) paint;
            gradientCtx = null;
        }
    }

    @Override
    public TPaint getPaint() {
        return gradient != null ? gradient : color;
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        clip = ClipRun.toDevice(a, b, c, d, e, f, x, y, width, height);
        changed();
    }

    // shortcut: a non-rectangle clip clips to its bounds, add clip paths when an app clips to another shape
    @Override
    public void setClip(TShape s) {
        if (s == null) {
            clip = null;
            changed();
        } else {
            TRectangle r = s.getBounds();
            setClip(r.x, r.y, r.width, r.height);
        }
    }

    @Override
    public TShape getClip() {
        return getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        double[] box = ClipRun.toDevice(a, b, c, d, e, f, x, y, width, height);
        clip = clip == null ? box : ClipRun.intersect(clip, box);
        changed();
    }

    @Override
    public void clip(TShape s) {
        if (s == null) {
            setClip(null);
        } else {
            TRectangle r = s.getBounds();
            clipRect(r.x, r.y, r.width, r.height);
        }
    }

    @Override
    public TRectangle getClipBounds() {
        int[] r = clip == null ? null : ClipRun.userBounds(clip, a, b, c, d, e, f);
        return r == null ? null : new TRectangle(r[0], r[1], r[2], r[3]);
    }

    @Override
    public TFont getFont() {
        return font;
    }

    @Override
    public void setFont(TFont font) {
        if (font != null) {
            this.font = font;
        }
    }

    @Override
    public void setStroke(TStroke s) {
        stroke = s;
        changed();
    }

    @Override
    public TStroke getStroke() {
        return stroke;
    }

    @Override
    public void setRenderingHint(TRenderingHints.Key key, Object value) {
        hints.put(key, value);
    }

    @Override
    public Object getRenderingHint(TRenderingHints.Key key) {
        return hints.get(key);
    }

    @Override
    public void setRenderingHints(Map<?, ?> newHints) {
        hints.clear();
        hints.putAll(newHints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> newHints) {
        hints.putAll(newHints);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        double o = strokeOffset();
        strokeLine(ctx(), style(), x1 + o, y1 + o, x2 + o, y2 + o);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            fillRect(ctx(), style(), x, y, width, height);
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (width >= 0 && height >= 0) {
            double o = strokeOffset();
            strokeRect(ctx(), style(), x + o, y + o, width, height);
        }
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        clear(ctx(), x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        double o = strokeOffset();
        roundRect(ctx(), style(), false, x + o, y + o, width, height, arcWidth / 2.0, arcHeight / 2.0);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        roundRect(ctx(), style(), true, x, y, width, height, arcWidth / 2.0, arcHeight / 2.0);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (width >= 0 && height >= 0) {
            double o = strokeOffset();
            ellipse(ctx(), style(), false, x + o + width / 2.0, y + o + height / 2.0, width / 2.0, height / 2.0);
        }
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width >= 0 && height >= 0) {
            double o = strokeOffset();
            arc(ctx(), style(), x + o + width / 2.0, y + o + height / 2.0, width / 2.0, height / 2.0,
                    -Math.toRadians(startAngle), -Math.toRadians(startAngle + arcAngle), arcAngle > 0);
        }
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            ellipse(ctx(), style(), true, x + width / 2.0, y + height / 2.0, width / 2.0, height / 2.0);
        }
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        polygon(ctx(), style(), false, xPoints, yPoints, nPoints, strokeOffset());
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        polygon(ctx(), style(), true, xPoints, yPoints, nPoints, 0);
    }

    @Override
    public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        if (str != null && !str.isEmpty()) {
            text(ctx(), style(), font.css(), str, x, y);
        }
    }

    @Override
    public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {
        return img == null || drawImage(img, x, y, img.getWidth(null), img.getHeight(null), observer);
    }

    @Override
    public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
        if (img == null) {
            return true;
        }
        int pending = img.productionRemaining();
        if (pending > 0) {
            if (observer instanceof TComponent) {
                TComponent c = (TComponent) observer; // AWT repaints the observer once the image is ready
                Js.setTimeout(c::markDirty, pending);
            }
            return false;
        }
        JSObject source = img.drawSource();
        if (source != null && width > 0 && height > 0) {
            image(ctx(), source, x, y, width, height);
        }
        return true;
    }

    @Override
    public void draw(TShape s) {
        if (s instanceof TPolygon) {
            drawPolygon((TPolygon) s);
        } else if (s instanceof TQuadCurve2D) {
            quad((TQuadCurve2D) s, false, strokeOffset());
        } else {
            super.draw(s);
        }
    }

    @Override
    public void fill(TShape s) {
        if (s instanceof TPolygon) {
            fillPolygon((TPolygon) s);
        } else if (s instanceof TQuadCurve2D) {
            quad((TQuadCurve2D) s, true, 0);
        } else {
            super.fill(s);
        }
    }

    private void quad(TQuadCurve2D q, boolean fill, double o) {
        quad(ctx(), style(), fill, q.getX1() + o, q.getY1() + o, q.getCtrlX() + o, q.getCtrlY() + o, q.getX2() + o, q.getY2() + o);
    }

    @Override
    public void dispose() {
        if (ctx != null && lastCtx == ctx && lastOwner == this && clip != null) {
            release(ctx);
        }
    }

    // ---- canvas calls ----

    @JSBody(params = { "g", "x", "y", "w", "h" }, script =
            "g.save(); g.__shimClip = true; g.setTransform(1, 0, 0, 1, 0, 0); g.beginPath(); g.rect(x, y, w, h); g.clip();")
    private static native void clipTo(JSObject g, int x, int y, int w, int h);

    @JSBody(params = "g", script = "if (g.__shimClip) { g.__shimClip = false; g.restore(); }")
    private static native void unclip(JSObject g);

    // shortcut: a cyclic gradient repeats for about 8192 px (at most 512 cycles) each way, since a canvas gradient only pads; widen it if an app paints past that
    @JSBody(params = { "g", "x1", "y1", "x2", "y2", "c1", "c2", "cyclic" }, script =
            "var dx = x2 - x1, dy = y2 - y1;"
            + "if (dx === 0 && dy === 0) return c2;"
            + "var n = cyclic ? Math.min(Math.ceil(8192 / Math.sqrt(dx * dx + dy * dy)), 512) : 0;"
            + "var gr = g.createLinearGradient(x1 - n * dx, y1 - n * dy, x2 + n * dx, y2 + n * dy);"
            + "for (var k = 0; k <= 2 * n + 1; k++) gr.addColorStop(k / (2 * n + 1), (k - n) % 2 === 0 ? c1 : c2);"
            + "return gr;")
    private static native JSObject linearGradient(JSObject g, double x1, double y1, double x2, double y2, String c1, String c2, boolean cyclic);

    @JSBody(params = { "g", "s", "isFill", "x1", "y1", "cx", "cy", "x2", "y2" }, script =
            "g.beginPath(); g.moveTo(x1, y1); g.quadraticCurveTo(cx, cy, x2, y2);"
            + "if (isFill) { g.closePath(); g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void quad(JSObject g, JSObject s, boolean fill, double x1, double y1, double cx, double cy, double x2, double y2);

    @JSBody(params = { "g", "a", "b", "c", "d", "e", "f" }, script = "g.setTransform(a, b, c, d, e, f);")
    private static native void setTransform(JSObject g, double a, double b, double c, double d, double e, double f);

    @JSBody(params = { "g", "w", "cap", "join" }, script = "g.lineWidth = w; g.lineCap = cap; g.lineJoin = join;")
    private static native void setLine(JSObject g, double w, String cap, String join);

    @JSBody(params = { "g", "s", "x", "y", "w", "h" }, script = "g.fillStyle = s; g.fillRect(x, y, w, h);")
    private static native void fillRect(JSObject g, JSObject s, double x, double y, double w, double h);

    @JSBody(params = { "g", "s", "x", "y", "w", "h" }, script = "g.strokeStyle = s; g.strokeRect(x, y, w, h);")
    private static native void strokeRect(JSObject g, JSObject s, double x, double y, double w, double h);

    @JSBody(params = { "g", "x", "y", "w", "h" }, script = "g.clearRect(x, y, w, h);")
    private static native void clear(JSObject g, double x, double y, double w, double h);

    @JSBody(params = { "g", "s", "x1", "y1", "x2", "y2" }, script =
            "g.strokeStyle = s; g.beginPath(); g.moveTo(x1, y1); g.lineTo(x2, y2); g.stroke();")
    private static native void strokeLine(JSObject g, JSObject s, double x1, double y1, double x2, double y2);

    @JSBody(params = { "g", "s", "isFill", "cx", "cy", "rx", "ry" }, script =
            "g.beginPath(); g.ellipse(cx, cy, Math.max(rx, 0), Math.max(ry, 0), 0, 0, 2 * Math.PI);"
            + "if (isFill) { g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void ellipse(JSObject g, JSObject s, boolean fill, double cx, double cy, double rx, double ry);

    /** Java angles turn counterclockwise with y up; the canvas's turn clockwise with y down, hence the negated angles. */
    @JSBody(params = { "g", "s", "cx", "cy", "rx", "ry", "from", "to", "ccw" }, script =
            "g.beginPath(); g.ellipse(cx, cy, Math.max(rx, 0), Math.max(ry, 0), 0, from, to, ccw); g.strokeStyle = s; g.stroke();")
    private static native void arc(JSObject g, JSObject s, double cx, double cy, double rx, double ry, double from, double to, boolean ccw);

    @JSBody(params = { "g", "s", "isFill", "x", "y", "w", "h", "rx", "ry" }, script =
            "g.beginPath(); g.roundRect(x, y, w, h, [Math.min(rx, ry)]);"
            + "if (isFill) { g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void roundRect(JSObject g, JSObject s, boolean fill, double x, double y, double w, double h, double rx, double ry);

    @JSBody(params = { "g", "s", "isFill", "xs", "ys", "n", "o" }, script =
            "g.beginPath(); for (var _i = 0; _i < n; _i++) { if (_i === 0) g.moveTo(xs[_i] + o, ys[_i] + o); else g.lineTo(xs[_i] + o, ys[_i] + o); }"
            + "g.closePath(); if (isFill) { g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void polygon(JSObject g, JSObject s, boolean fill, int[] xs, int[] ys, int n, double o);

    @JSBody(params = { "g", "s", "cssFont", "str", "x", "y" }, script =
            "g.fillStyle = s; g.font = cssFont; g.textBaseline = 'alphabetic'; g.fillText(str, x, y);")
    private static native void text(JSObject g, JSObject s, String font, String str, double x, double y);

    @JSBody(params = { "g", "img", "x", "y", "w", "h" }, script =
            "g.imageSmoothingQuality = 'high'; g.drawImage(img, x, y, w, h);")
    private static native void image(JSObject g, JSObject img, double x, double y, double w, double h);
}
