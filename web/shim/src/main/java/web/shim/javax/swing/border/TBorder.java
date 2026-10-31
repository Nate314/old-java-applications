package web.shim.javax.swing.border;

import web.shim.java.awt.TComponent;
import web.shim.java.awt.TGraphics;
import web.shim.java.awt.TInsets;

public interface TBorder {
    void paintBorder(TComponent c, TGraphics g, int x, int y, int width, int height);

    TInsets getBorderInsets(TComponent c);

    boolean isBorderOpaque();
}
