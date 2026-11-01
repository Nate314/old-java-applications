package web.shim.javax.swing;

import org.teavm.jso.JSObject;
import web.rt.Js;
import web.shim.java.awt.web.TTitleBar;
import web.shim.java.awt.TBorderLayout;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TImage;
import web.shim.java.awt.TInsets;
import web.shim.java.awt.TLayoutManager;
import web.shim.java.beans.TPropertyVetoException;
import web.shim.javax.swing.border.TBorder;
import web.shim.javax.swing.plaf.TInternalFrameUI;
import web.shim.javax.swing.plaf.basic.TBasicInternalFrameUI;

/** A window box inside a JDesktopPane, with the same title bar as a JFrame. Dragged by its title bar. */
public class TJInternalFrame extends TJComponent implements TWindowConstants {
    private final TContainer contentPane;
    private final boolean rootPaneChecking;
    private final TBasicInternalFrameUI ui = new TBasicInternalFrameUI(this);
    private String title;
    private boolean resizable;
    private boolean closable;
    private boolean maximizable;
    private boolean iconifiable;
    private boolean selected;
    private boolean hasBorder = true;
    private boolean northPane = true;
    private int closeOperation = DISPOSE_ON_CLOSE;
    private TIcon frameIcon;
    private TTitleBar titleBar;

    public TJInternalFrame() {
        this("", false, false, false, false);
    }

    public TJInternalFrame(String title) {
        this(title, false, false, false, false);
    }

    public TJInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable) {
        this(title, resizable, closable, maximizable, false);
    }

    public TJInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
        this.title = title;
        this.resizable = resizable;
        this.closable = closable;
        this.maximizable = maximizable;
        this.iconifiable = iconifiable;
        visible = false;
        TJPanel pane = new TJPanel(new TBorderLayout());
        contentPane = pane;
        super.addImpl(pane, null, -1);
        rootPaneChecking = true;
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("div");
        Js.setClass(el, "c window internal");
        titleBar = new TTitleBar(el, this::closeRequested);
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        syncDecorations();
    }

    private void syncDecorations() {
        if (hasElement()) {
            Js.toggleClass(element(), "decorated", hasBorder);
            Js.toggleClass(element(), "selected", selected);
            titleBar.update(northPane, title, closable);
            titleBar.setIcon(frameIcon instanceof TImageIcon ? ((TImageIcon) frameIcon).getImage() : null);
        }
    }

    @Override
    public TInsets getInsets() {
        int b = hasBorder ? TTitleBar.BORDER : 0;
        return new TInsets(b + (northPane ? TTitleBar.HEIGHT : 0), b, b, b);
    }

    @Override
    public boolean isTitleBar(JSObject node) {
        return northPane && titleBar != null && titleBar.contains(node);
    }

    @Override
    public void doLayout() {
        TInsets i = getInsets();
        contentPane.setBounds(i.left, i.top, width - i.left - i.right, height - i.top - i.bottom);
    }

    // ---- content pane ----

    public TContainer getContentPane() {
        return contentPane;
    }

    @Override
    protected void addImpl(TComponent comp, Object constraints, int index) {
        if (rootPaneChecking) {
            contentPane.add(comp, constraints, index);
        } else {
            super.addImpl(comp, constraints, index);
        }
    }

    @Override
    public void setLayout(TLayoutManager mgr) {
        if (rootPaneChecking) {
            contentPane.setLayout(mgr);
        } else {
            super.setLayout(mgr);
        }
    }

    // ---- properties ----

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        syncDecorations();
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean b) {
        resizable = b;
    }

    public boolean isClosable() {
        return closable;
    }

    public void setClosable(boolean b) {
        closable = b;
        syncDecorations();
    }

    public boolean isMaximizable() {
        return maximizable;
    }

    public void setMaximizable(boolean b) {
        maximizable = b;
    }

    public boolean isIconifiable() {
        return iconifiable;
    }

    public void setIconifiable(boolean b) {
        iconifiable = b;
    }

    public TIcon getFrameIcon() {
        return frameIcon;
    }

    public void setFrameIcon(TIcon icon) {
        frameIcon = icon;
        syncDecorations();
    }

    public void setDefaultCloseOperation(int operation) {
        closeOperation = operation;
    }

    public int getDefaultCloseOperation() {
        return closeOperation;
    }

    @Override
    public void setBorder(TBorder b) {
        super.setBorder(b);
        hasBorder = b != null;
        syncDecorations();
        invalidate();
    }

    public TInternalFrameUI getUI() {
        return ui;
    }

    /** Called by BasicInternalFrameUI.setNorthPane: null removes the title bar. */
    public void setNorthPaneVisible(boolean b) {
        northPane = b;
        syncDecorations();
        invalidate();
    }

    // ---- selection and z-order ----

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean b) throws TPropertyVetoException {
        selected = b;
        if (b) {
            moveToFront();
        }
        syncDecorations();
    }

    public void moveToFront() {
        TContainer p = getParent();
        if (p != null && p.getComponentZOrder(this) != 0) {
            p.setComponentZOrder(this, 0);
        }
    }

    public void toFront() {
        moveToFront();
    }

    private void closeRequested() {
        if (closeOperation == DISPOSE_ON_CLOSE) {
            dispose();
        } else if (closeOperation == HIDE_ON_CLOSE) {
            setVisible(false);
        }
    }

    public void dispose() {
        setVisible(false);
        TContainer p = getParent();
        if (p != null) {
            p.remove(this);
        }
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    protected web.shim.java.awt.TColor defaultBackground() {
        return CONTROL;
    }

}
