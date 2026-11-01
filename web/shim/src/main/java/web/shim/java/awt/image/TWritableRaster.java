package web.shim.java.awt.image;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** Pixel access to a TBufferedImage. Writes go to an ImageData that is copied to the canvas before it is drawn. */
public class TWritableRaster {
    private final JSObject canvas;
    private final int width;
    private final int height;
    private JSObject imageData;
    private boolean dirty;

    TWritableRaster(JSObject canvas, int width, int height) {
        this.canvas = canvas;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPixel(int x, int y, int[] rgb) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        if (imageData == null) {
            imageData = readImageData(canvas, width, height);
        }
        writePixel(imageData, (y * width + x) * 4, rgb[0], rgb[1], rgb[2], rgb.length > 3 ? rgb[3] : 255);
        dirty = true;
    }

    void flush() {
        if (dirty) {
            putImageData(canvas, imageData);
            dirty = false;
        }
    }

    @JSBody(params = { "c", "w", "h" }, script = "return c.getContext('2d').getImageData(0, 0, w, h);")
    private static native JSObject readImageData(JSObject c, int w, int h);

    @JSBody(params = { "d", "i", "r", "g", "b", "a" }, script = "var _p = d.data; _p[i] = r; _p[i + 1] = g; _p[i + 2] = b; _p[i + 3] = a;")
    private static native void writePixel(JSObject d, int i, int r, int g, int b, int a);

    @JSBody(params = { "c", "d" }, script = "c.getContext('2d').putImageData(d, 0, 0);")
    private static native void putImageData(JSObject c, JSObject d);
}
