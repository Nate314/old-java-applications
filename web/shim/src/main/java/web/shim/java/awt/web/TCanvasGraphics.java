package web.shim.java.awt.web;

import web.rt.Edt;
import web.rt.Exit;
import web.rt.Js;
import web.rt.Keys;

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import web.shim.java.awt.TBasicStroke;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TGraphics;
import web.shim.java.awt.TGraphics2D;
import web.shim.java.awt.TImage;
import web.shim.java.awt.TRenderingHints;
import web.shim.java.awt.TStroke;
import web.shim.java.awt.image.TImageObserver;

/**
 * Graphics2D over a CanvasRenderingContext2D. The canvas is created lazily on the first drawing call, so
 * components whose paint() draws nothing never get one. Coordinates are CSS pixels; the backing store is
 * scaled by devicePixelRatio.
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
        hints = new HashMap<>(o.hints);
    }

    public static TCanvasGraphics forCanvas(JSObject canvas, double pixelRatio) {
        JSObject context = Js.context2d(canvas);
        return new TCanvasGraphics(() -> context, pixelRatio, null, null);
    }

    private JSObject ctx() {
        if (ctx == null) {
            ctx = surface.context();
        }
        if (lastCtx != ctx || lastOwner != this || appliedVersion != myVersion) {
            lastCtx = ctx;
            lastOwner = this;
            appliedVersion = myVersion;
            double r = pixelRatio;
            setTransform(ctx, r * a, r * b, r * c, r * d, r * e, r * f);
            applyStroke();
        }
        return ctx;
    }

    private void changed() {
        myVersion = ++version;
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
        }
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
        strokeLine(ctx(), color.css(), x1 + o, y1 + o, x2 + o, y2 + o);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            fillRect(ctx(), color.css(), x, y, width, height);
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (width >= 0 && height >= 0) {
            double o = strokeOffset();
            strokeRect(ctx(), color.css(), x + o, y + o, width, height);
        }
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        clear(ctx(), x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        double o = strokeOffset();
        roundRect(ctx(), color.css(), false, x + o, y + o, width, height, arcWidth / 2.0, arcHeight / 2.0);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        roundRect(ctx(), color.css(), true, x, y, width, height, arcWidth / 2.0, arcHeight / 2.0);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (width >= 0 && height >= 0) {
            double o = strokeOffset();
            ellipse(ctx(), color.css(), false, x + o + width / 2.0, y + o + height / 2.0, width / 2.0, height / 2.0);
        }
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            ellipse(ctx(), color.css(), true, x + width / 2.0, y + height / 2.0, width / 2.0, height / 2.0);
        }
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        polygon(ctx(), color.css(), false, xPoints, yPoints, nPoints, strokeOffset());
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        polygon(ctx(), color.css(), true, xPoints, yPoints, nPoints, 0);
    }

    @Override
    public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        if (str != null && !str.isEmpty()) {
            text(ctx(), color.css(), font.css(), str, x, y);
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
    public void dispose() {
    }

    // ---- canvas calls ----

    @JSBody(params = { "g", "a", "b", "c", "d", "e", "f" }, script = "g.setTransform(a, b, c, d, e, f);")
    private static native void setTransform(JSObject g, double a, double b, double c, double d, double e, double f);

    @JSBody(params = { "g", "w", "cap", "join" }, script = "g.lineWidth = w; g.lineCap = cap; g.lineJoin = join;")
    private static native void setLine(JSObject g, double w, String cap, String join);

    @JSBody(params = { "g", "s", "x", "y", "w", "h" }, script = "g.fillStyle = s; g.fillRect(x, y, w, h);")
    private static native void fillRect(JSObject g, String s, double x, double y, double w, double h);

    @JSBody(params = { "g", "s", "x", "y", "w", "h" }, script = "g.strokeStyle = s; g.strokeRect(x, y, w, h);")
    private static native void strokeRect(JSObject g, String s, double x, double y, double w, double h);

    @JSBody(params = { "g", "x", "y", "w", "h" }, script = "g.clearRect(x, y, w, h);")
    private static native void clear(JSObject g, double x, double y, double w, double h);

    @JSBody(params = { "g", "s", "x1", "y1", "x2", "y2" }, script =
            "g.strokeStyle = s; g.beginPath(); g.moveTo(x1, y1); g.lineTo(x2, y2); g.stroke();")
    private static native void strokeLine(JSObject g, String s, double x1, double y1, double x2, double y2);

    @JSBody(params = { "g", "s", "isFill", "cx", "cy", "rx", "ry" }, script =
            "g.beginPath(); g.ellipse(cx, cy, Math.max(rx, 0), Math.max(ry, 0), 0, 0, 2 * Math.PI);"
            + "if (isFill) { g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void ellipse(JSObject g, String s, boolean fill, double cx, double cy, double rx, double ry);

    @JSBody(params = { "g", "s", "isFill", "x", "y", "w", "h", "rx", "ry" }, script =
            "g.beginPath(); g.roundRect(x, y, w, h, [Math.min(rx, ry)]);"
            + "if (isFill) { g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void roundRect(JSObject g, String s, boolean fill, double x, double y, double w, double h, double rx, double ry);

    @JSBody(params = { "g", "s", "isFill", "xs", "ys", "n", "o" }, script =
            "g.beginPath(); for (var _i = 0; _i < n; _i++) { if (_i === 0) g.moveTo(xs[_i] + o, ys[_i] + o); else g.lineTo(xs[_i] + o, ys[_i] + o); }"
            + "g.closePath(); if (isFill) { g.fillStyle = s; g.fill(); } else { g.strokeStyle = s; g.stroke(); }")
    private static native void polygon(JSObject g, String s, boolean fill, int[] xs, int[] ys, int n, double o);

    @JSBody(params = { "g", "s", "cssFont", "str", "x", "y" }, script =
            "g.fillStyle = s; g.font = cssFont; g.textBaseline = 'alphabetic'; g.fillText(str, x, y);")
    private static native void text(JSObject g, String s, String font, String str, double x, double y);

    @JSBody(params = { "g", "img", "x", "y", "w", "h" }, script =
            "g.imageSmoothingQuality = 'high'; g.drawImage(img, x, y, w, h);")
    private static native void image(JSObject g, JSObject img, double x, double y, double w, double h);
}
