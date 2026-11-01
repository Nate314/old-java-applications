package web.shim.java.awt.image;

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
