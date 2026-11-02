package web.shim.java.awt.image;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import web.shim.java.awt.web.TCanvasGraphics;
import web.rt.Js;
import web.shim.java.awt.TGraphics;
import web.shim.java.awt.TGraphics2D;
import web.shim.java.awt.TImage;

/** An offscreen image on a canvas element (1:1 pixels). */
public class TBufferedImage extends TImage {
    public static final int TYPE_INT_RGB = 1;
    public static final int TYPE_INT_ARGB = 2;
    public static final int TYPE_INT_ARGB_PRE = 3;

    private final int type;
    private TWritableRaster raster;

    public TBufferedImage(int width, int height, int type) {
        this.width = width;
        this.height = height;
        this.type = type;
        this.source = Js.create("canvas");
        Js.sizeCanvasExact(source, width, height);
        if (type == TYPE_INT_RGB) {
            TGraphics2D g = createGraphics(); // a new RGB image is opaque black
            g.setColor(web.shim.java.awt.TColor.black);
            g.fillRect(0, 0, width, height);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getType() {
        return type;
    }

    public TWritableRaster getRaster() {
        if (raster == null) {
            raster = new TWritableRaster(source, width, height);
        }
        return raster;
    }

    /** The pixel at (x, y) as ARGB, read back from the canvas. */
    public int getRGB(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
        return readPixel(drawSource(), x, y);
    }

    @JSBody(params = { "c", "x", "y" }, script =
            "var _d = c.getContext('2d').getImageData(x, y, 1, 1).data; return (_d[3] << 24) | (_d[0] << 16) | (_d[1] << 8) | _d[2];")
    private static native int readPixel(JSObject c, int x, int y);

    @Override
    public JSObject drawSource() {
        if (raster != null) {
            raster.flush();
        }
        return source;
    }

    @Override
    public TGraphics getGraphics() {
        return createGraphics();
    }

    public TGraphics2D createGraphics() {
        drawSource();
        return TCanvasGraphics.forCanvas(source, 1.0);
    }
}
