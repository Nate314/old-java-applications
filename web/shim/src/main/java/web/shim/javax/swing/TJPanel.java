package web.shim.javax.swing;

import web.shim.java.awt.TColor;
import web.shim.java.awt.TFlowLayout;
import web.shim.java.awt.TLayoutManager;

public class TJPanel extends TJComponent {
    public TJPanel() {
        this(new TFlowLayout());
    }

    public TJPanel(TLayoutManager layout) {
        setLayout(layout);
        setOpaque(true);
    }

    public TJPanel(boolean isDoubleBuffered) {
        this();
    }

    @Override
    protected String cssClass() {
        return "panel";
    }

    @Override
    protected TColor defaultBackground() {
        return CONTROL;
    }

    @Override
    protected TColor defaultForeground() {
        return CONTROL_TEXT;
    }
}
