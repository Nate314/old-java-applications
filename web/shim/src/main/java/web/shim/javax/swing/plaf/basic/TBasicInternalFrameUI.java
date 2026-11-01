package web.shim.javax.swing.plaf.basic;

import web.shim.javax.swing.TJComponent;
import web.shim.javax.swing.TJInternalFrame;
import web.shim.javax.swing.plaf.TInternalFrameUI;

/** Only the title bar ("north pane") switch is modelled; NathaNOS removes it from its taskbar. */
public class TBasicInternalFrameUI extends TInternalFrameUI {
    private final TJInternalFrame frame;
    private TJComponent northPane;

    public TBasicInternalFrameUI(TJInternalFrame frame) {
        this.frame = frame;
    }

    public TJComponent getNorthPane() {
        return northPane;
    }

    public void setNorthPane(TJComponent c) {
        northPane = c;
        frame.setNorthPaneVisible(c != null);
    }
}
