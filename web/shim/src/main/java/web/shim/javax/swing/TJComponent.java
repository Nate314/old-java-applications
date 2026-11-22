package web.shim.javax.swing;

import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TComponent;
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
    /** Set by JLabel.setLabelFor. */
    TJLabel labeledBy;

    protected TJComponent() {
    }

    // ---- accessible name from a label ----

    @Override
    protected String labelId() {
        TJLabel label = labeledBy != null ? labeledBy : takesNearbyLabel() ? nearbyLabel() : null;
        return label == null ? null : label.elementId();
    }

    /** A field with no labelFor is named by the nearest JLabel beside it on its left or just above it, as a reader sees it. */
    protected boolean takesNearbyLabel() {
        return false;
    }

    private TJLabel nearbyLabel() {
        TContainer p = getParent();
        TJLabel best = null;
        int bestGap = Integer.MAX_VALUE;
        for (int i = 0; p != null && i < p.getComponentCount(); i++) {
            TComponent c = p.getComponent(i);
            if (!(c instanceof TJLabel) || !c.isVisible() || ((TJLabel) c).getText().isEmpty()) {
                continue;
            }
            int left = getX() - (c.getX() + c.getWidth());
            int above = getY() - (c.getY() + c.getHeight());
            boolean sameRow = c.getY() < getY() + getHeight() && c.getY() + c.getHeight() > getY();
            boolean sameColumn = c.getX() < getX() + getWidth() && c.getX() + c.getWidth() > getX();
            // A label's box may overlap the field's edge by a couple of pixels.
            int gap = sameRow && left >= -2 ? left : sameColumn && above >= -2 ? above : Integer.MAX_VALUE;
            if (gap < bestGap) {
                best = (TJLabel) c;
                bestGap = gap;
            }
        }
        return best;
    }

    @Override
    public void validate() {
        super.validate();
        if (takesNearbyLabel()) {
            syncAccessible(); // laid out: the labels around it have their places now
        }
    }

    /** The component offset places from this one in its container's order, or null. */
    TComponent sibling(int offset) {
        TContainer p = getParent();
        int i = p == null ? -1 : p.getComponentZOrder(this) + offset;
        return p == null || i < 0 || i >= p.getComponentCount() ? null : p.getComponent(i);
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
            syncAccessible();
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
