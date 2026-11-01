package web.shim.javax.swing;

import web.shim.java.awt.TComponent;
import web.shim.java.awt.TGraphics;

public interface TIcon {
    void paintIcon(TComponent c, TGraphics g, int x, int y);

    int getIconWidth();

    int getIconHeight();
}
