package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import web.shim.java.awt.TColor;

public class TJDesktopPane extends TJLayeredPane {
    private static final TColor DESKTOP = new TColor(184, 207, 229);

    public TJDesktopPane() {
        setOpaque(true);
    }

    @Override
    protected String cssClass() {
        return "desktoppane";
    }

    @Override
    protected TColor defaultBackground() {
        return DESKTOP;
    }

    /** Internal frames, topmost first. */
    public TJInternalFrame[] getAllFrames() {
        List<TJInternalFrame> frames = new ArrayList<>();
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) instanceof TJInternalFrame) {
                frames.add((TJInternalFrame) getComponent(i));
            }
        }
        return frames.toArray(new TJInternalFrame[0]);
    }
}
