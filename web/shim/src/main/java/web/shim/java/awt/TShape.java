package web.shim.java.awt;

import web.shim.java.awt.geom.TAffineTransform;
import web.shim.java.awt.geom.TPathIterator;

/** java.awt.Shape: Graphics2D draws and fills any shape, an app's own included, by its path iterator. */
public interface TShape {
    TRectangle getBounds();

    TPathIterator getPathIterator(TAffineTransform at);
}
