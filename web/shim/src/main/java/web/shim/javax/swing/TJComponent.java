package web.shim.javax.swing;

import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TGraphics;
import web.shim.javax.swing.border.TBorder;

public abstract class TJComponent extends TContainer {
    /** Metal (Ocean) defaults. Identity matters: a component showing one of these leaves the color to the CSS skin. */
    static final TColor CONTROL = new TColor(238, 238, 238);
    static final TColor CONTROL_TEXT = new TColor(51, 51, 51);
    static final TColor TEXT_BACKGROUND = new TColor(255, 255, 255);
    static final TColor TEXT_FOREGROUND = new TColor(0, 0, 0);
    static final TFont CONTROL_FONT = new TFont("Dialog", TFont.BOLD, 12);
    static final TFont TEXT_FONT = new TFont("Dialog", TFont.PLAIN, 12);

    private boolean opaque;
    private TBorder border;
    private boolean borderSet;
    private String toolTip;

    protected TJComponent() {
    }

    @Override
    public void paint(TGraphics g) {
        paintComponent(g);
        paintBorder(g);
        paintChildren(g);
    }

    /** The widget itself is DOM, so the default only clears earlier custom drawing. */
    protected void paintComponent(TGraphics g) {
        backgroundPainted = true;
        clearCanvas();
    }

    protected void paintBorder(TGraphics g) {
    }

    protected void paintChildren(TGraphics g) {
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    public void setOpaque(boolean b) {
        opaque = b;
        syncColors();
    }

    public TBorder getBorder() {
        return border;
    }

    /** Only removing the border (setBorder(null)) is supported; it hides the widget's skin border. */
    public void setBorder(TBorder b) {
        border = b;
        borderSet = true;
        if (hasElement()) {
            Js.toggleClass(element(), "no-border", b == null);
        }
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        if (borderSet) {
            Js.toggleClass(element(), "no-border", border == null);
        }
        if (toolTip != null) {
            Js.setAttr(element(), "title", toolTip);
        }
    }

    public void setToolTipText(String text) {
        toolTip = text;
        if (hasElement()) {
            if (text == null) {
                Js.removeAttr(element(), "title");
            } else {
                Js.setAttr(element(), "title", text);
            }
        }
    }

    public String getToolTipText() {
        return toolTip;
    }

    public void setDoubleBuffered(boolean b) {
    }

    public void setRequestFocusEnabled(boolean b) {
        setFocusable(b);
    }

    @Override
    public void revalidate() {
        invalidate();
    }
}
