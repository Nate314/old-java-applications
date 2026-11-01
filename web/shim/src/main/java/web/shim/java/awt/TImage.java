package web.shim.java.awt;

import org.teavm.jso.JSObject;
import web.rt.Js;
import web.shim.java.awt.image.TImageObserver;

/**
 * An image backed by a preloaded HTMLImageElement or a canvas. Scaling is lazy: a scaled instance shares the
 * source and only records the size to draw at. A missing file gives width and height -1, as on the desktop.
 */
public class TImage {
    public static final int SCALE_DEFAULT = 1;
    public static final int SCALE_FAST = 2;
    public static final int SCALE_SMOOTH = 4;
    public static final int SCALE_REPLICATE = 8;
    public static final int SCALE_AREA_AVERAGING = 16;

    protected JSObject source;
    protected int width;
    protected int height;
    /** The image this one is a scaled instance of (its pixels may still change, e.g. a BufferedImage). */
    private TImage original;
    /** 0 = ready; otherwise when an asynchronous scale finishes (-1 = not started). */
    private double readyAt;

    protected TImage() {
    }

    TImage(JSObject source, int width, int height) {
        this.source = source;
        this.width = width;
        this.height = height;
    }

    /** Loads an image that boot.js preloaded for this page, or a broken (-1 x -1) image if it was not found. */
    public static TImage load(String path) {
        String key = path.replace('\\', '/');
        while (key.startsWith("./")) {
            key = key.substring(2);
        }
        JSObject img = Js.preloadedImage(key);
        if (img == null || Js.isNull(img)) {
            return new TImage(null, -1, -1);
        }
        return new TImage(img, Js.naturalWidth(img), Js.naturalHeight(img));
    }

    public int getWidth(TImageObserver observer) {
        return width;
    }

    public int getHeight(TImageObserver observer) {
        return height;
    }

    public TImage getScaledInstance(int w, int h, int hints) {
        if (width <= 0 || height <= 0) {
            return new TImage(source, -1, -1);
        }
        if (w < 0 && h < 0) {
            w = width;
            h = height;
        } else if (w < 0) {
            w = (int) Math.round((double) width * h / height);
        } else if (h < 0) {
            h = (int) Math.round((double) height * w / width);
        }
        TImage scaled = new TImage(source, w, h);
        scaled.original = this;
        scaled.readyAt = -1;
        return scaled;
    }

    /**
     * AWT scales images asynchronously: drawing a scaled instance starts production, draws nothing and returns
     * false until it is done. FlappyBird relies on that (its start check looks for the still-unpainted black
     * frame). Returns the milliseconds left, or 0 if the image can be drawn now.
     */
    public int productionRemaining() {
        if (readyAt == 0) {
            return 0;
        }
        double now = System.currentTimeMillis();
        if (readyAt < 0) {
            readyAt = now + SCALE_MILLIS;
        }
        if (now >= readyAt) {
            readyAt = 0;
            return 0;
        }
        return (int) (readyAt - now) + 1;
    }

    private static final int SCALE_MILLIS = 100;

    public TGraphics getGraphics() {
        throw new UnsupportedOperationException("getGraphics() not supported for non-offscreen images");
    }

    public void flush() {
    }

    /** The drawable JS object (image or canvas), or null if this image is broken. */
    public JSObject drawSource() {
        return original != null ? original.drawSource() : source;
    }

    /** A URL for this image's pixels, for CSS cursors and DOM icons. */
    public String url() {
        JSObject s = drawSource();
        if (s == null) {
            return null;
        }
        return this instanceof web.shim.java.awt.image.TBufferedImage ? Js.toDataUrl(s) : Js.src(s);
    }

    /** Draws this image into a fresh canvas element of its own size (used for icons). */
    public JSObject toCanvas() {
        JSObject canvas = Js.create("canvas");
        if (width > 0 && height > 0) {
            Js.sizeCanvas(canvas, width, height);
            Js.drawImageScaled(canvas, drawSource(), width, height); // icons are shown at once, without the async scaling delay
        }
        return canvas;
    }
}
