package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TFrame;
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

    /**
     * A field with no labelFor is named by the nearest JLabel beside it on its left or just above it, anywhere in its
     * window (NearLabel), as a reader sees it; with none, by its window's title (fallbackName).
     */
    protected boolean takesNearbyLabel() {
        return false;
    }

    private TJLabel nearbyLabel() {
        TContainer root = getParent();
        while (root != null && root.getParent() != null && !(root instanceof TJInternalFrame)) {
            root = root.getParent();
        }
        List<TJLabel> found = new ArrayList<>();
        labelsIn(root, found);
        if (getToolTipText() != null) {
            found.removeIf(l -> l.getParent() != getParent()); // its tooltip names it before a label from another panel
        }
        int[][] boxes = new int[found.size()][];
        boolean[] sibling = new boolean[found.size()];
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = box(found.get(i), root);
            sibling[i] = found.get(i).getParent() == getParent();
        }
        int i = root == null ? -1 : NearLabel.pick(box(this, root), boxes, sibling);
        return i < 0 ? null : found.get(i);
    }

    private static void labelsIn(TContainer p, List<TJLabel> found) {
        for (int i = 0; p != null && i < p.getComponentCount(); i++) {
            TComponent c = p.getComponent(i);
            if (!c.isVisible()) {
                continue;
            }
            if (c instanceof TJLabel && !((TJLabel) c).getText().trim().isEmpty()) {
                found.add((TJLabel) c);
            } else if (c instanceof TContainer && !(c instanceof TJInternalFrame)) {
                labelsIn((TContainer) c, found);
            }
        }
    }

    /** c's box in root's coordinates. */
    private static int[] box(TComponent c, TContainer root) {
        int x = 0;
        int y = 0;
        for (TComponent at = c; at != null && at != root; at = at.getParent()) {
            x += at.getX();
            y += at.getY();
        }
        return new int[] { x, y, c.getWidth(), c.getHeight() };
    }

    /**
     * A field no label names takes its window's title (Then pages' one-field windows: Circle Creator, Windows+E).
     * shortcut: read at layout, so a later setTitle (an error shown in the title) leaves the old name; fine as a name.
     */
    @Override
    protected String fallbackName() {
        if (!takesNearbyLabel() || getToolTipText() != null) { // the tooltip, its title, names it
            return null;
        }
        for (TComponent c = getParent(); c != null; c = c.getParent()) {
            if (c instanceof TJInternalFrame) {
                return ((TJInternalFrame) c).getTitle();
            }
            if (c instanceof TFrame) {
                return ((TFrame) c).getTitle();
            }
        }
        return null;
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
