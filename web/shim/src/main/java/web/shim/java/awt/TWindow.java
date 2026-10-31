package web.shim.java.awt;

import org.teavm.jso.JSObject;
import web.shim.java.awt.web.TDesktop;
import web.rt.Js;
import web.shim.java.awt.web.TTitleBar;

/**
 * A top-level window: a box on the page desktop. Decorated subclasses get a title bar with a close button;
 * insets match that chrome so app layout math (which uses getWidth() of the window) works as on the desktop.
 */
public class TWindow extends TContainer {
    private boolean displayed;
    private boolean alwaysOnTop;
    private TComponent focusOwner;
    private TTitleBar titleBar;

    public TWindow() {
        visible = false;
    }

    // ---- decorations ----

    protected boolean isDecoratedWindow() {
        return false;
    }

    @Override
    public TInsets getInsets() {
        int b = TTitleBar.BORDER;
        return isDecoratedWindow() ? new TInsets(TTitleBar.HEIGHT + b, b, b, b) : new TInsets(0, 0, 0, 0);
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("div");
        Js.setClass(el, "c window");
        titleBar = new TTitleBar(el, this::closeRequested);
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        syncDecorations();
    }

    protected void syncDecorations() {
        if (hasElement()) {
            Js.toggleClass(element(), "decorated", isDecoratedWindow());
            titleBar.update(isDecoratedWindow(), windowTitle(), true);
        }
    }

    protected String windowTitle() {
        return "";
    }

    protected void setTitleIcon(TImage image) {
        element();
        titleBar.setIcon(image);
    }

    @Override
    public boolean isTitleBar(JSObject node) {
        return isDecoratedWindow() && titleBar != null && titleBar.contains(node);
    }

    /** The title bar's close button was clicked (runs on the EDT). */
    protected void closeRequested() {
    }

    /**
     * The page is often smaller than the screens these apps were written for, so a window placed above or left of
     * the page edge (e.g. centered while 1000 px tall) is drawn at the edge instead, keeping its title bar reachable.
     */
    @Override
    protected int domX(int x) {
        return isDecoratedWindow() ? Math.max(x, 0) : x;
    }

    @Override
    protected int domY(int y) {
        return isDecoratedWindow() ? Math.max(y, 0) : y;
    }

    // ---- painting: paint() draws over the client area, on top of the children ----

    @Override
    protected int canvasX() {
        return getInsets().left;
    }

    @Override
    protected int canvasY() {
        return getInsets().top;
    }

    @Override
    protected int canvasWidth() {
        TInsets i = getInsets();
        return Math.max(width - i.left - i.right, 0);
    }

    @Override
    protected int canvasHeight() {
        TInsets i = getInsets();
        return Math.max(height - i.top - i.bottom, 0);
    }

    @Override
    protected boolean canvasOnTop() {
        return true;
    }

    // ---- layout: like a Frame's BorderLayout with one centered child ----

    @Override
    public void doLayout() {
        if (layout != null) {
            super.doLayout();
            return;
        }
        TInsets i = getInsets();
        for (TComponent c : children) {
            c.setBounds(i.left, i.top, width - i.left - i.right, height - i.top - i.bottom);
        }
    }

    @Override
    protected TDimension computePreferredSize() {
        if (layout != null) {
            return layout.preferredLayoutSize(this);
        }
        TInsets i = getInsets();
        TDimension d = new TDimension(i.left + i.right, i.top + i.bottom);
        for (TComponent c : children) {
            TDimension p = c.getPreferredSize();
            d.width = Math.max(d.width, p.width + i.left + i.right);
            d.height = Math.max(d.height, p.height + i.top + i.bottom);
        }
        return d;
    }

    public void pack() {
        TDimension d = getPreferredSize();
        setSize(d.width, d.height);
        validate();
    }

    // ---- showing ----

    boolean isDisplayed() {
        return displayed;
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            boolean wasVisible = visible;
            super.setVisible(true);
            displayed = true;
            TDesktop.show(this);
            if (!wasVisible) {
                validate();
                markTreeDirty();
            }
        } else {
            super.setVisible(false);
            TDesktop.hide(this);
        }
    }

    public void dispose() {
        visible = false;
        if (displayed) {
            displayed = false;
            TDesktop.dispose(this);
        }
    }

    public void toFront() {
        if (displayed) {
            TDesktop.activate(this);
        }
    }

    public void toBack() {
    }

    public boolean isActive() {
        return TDesktop.activeWindow() == this;
    }

    public boolean isFocused() {
        return isActive();
    }

    public void setAlwaysOnTop(boolean b) {
        alwaysOnTop = b;
        if (displayed && b) {
            TDesktop.activate(this);
        }
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setLocationRelativeTo(TComponent c) {
        if (c == null || !c.isShowing()) {
            setLocation((TDesktop.screenWidth() - width) / 2, (TDesktop.screenHeight() - height) / 2);
        } else {
            TWindow w = c.windowAncestor();
            int cx = (w != null ? w.x : 0) + c.width / 2;
            int cy = (w != null ? w.y : 0) + c.height / 2;
            setLocation(cx - width / 2, cy - height / 2);
        }
    }

    // ---- focus ----

    public TComponent focusOwner() {
        return focusOwner;
    }

    public TComponent getFocusOwner() {
        return isActive() ? focusOwner : null;
    }

    public void setFocusOwner(TComponent c) {
        focusOwner = c;
        if (c != null) {
            c.focusGained();
        }
    }
}
