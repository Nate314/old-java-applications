package web.shim.javax.swing;

import web.rt.Exit;
import web.shim.java.awt.TBorderLayout;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TFrame;
import web.shim.java.awt.TGraphics;
import web.shim.java.awt.TLayoutManager;

/** A Frame whose add/remove/setLayout go to the content pane, as in Swing. */
public class TJFrame extends TFrame implements TWindowConstants {
    private TContainer contentPane;
    private int closeOperation = HIDE_ON_CLOSE;
    private final boolean rootPaneChecking;

    public TJFrame() {
        TJPanel pane = new TJPanel(new TBorderLayout());
        contentPane = pane;
        super.addImpl(pane, null, -1);
        rootPaneChecking = true;
    }

    public TJFrame(String title) {
        this();
        setTitle(title);
    }

    public TContainer getContentPane() {
        return contentPane;
    }

    public void setContentPane(TContainer pane) {
        super.remove(contentPane);
        contentPane = pane;
        super.addImpl(pane, null, -1);
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
    public void remove(TComponent comp) {
        if (comp == contentPane || !rootPaneChecking) {
            super.remove(comp);
        } else {
            contentPane.remove(comp);
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

    public void setDefaultCloseOperation(int operation) {
        closeOperation = operation;
    }

    public int getDefaultCloseOperation() {
        return closeOperation;
    }

    @Override
    protected void closeRequested() {
        switch (closeOperation) {
            case EXIT_ON_CLOSE:
                Exit.exit(0);
                break;
            case DISPOSE_ON_CLOSE:
                dispose();
                break;
            case HIDE_ON_CLOSE:
                setVisible(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void update(TGraphics g) {
        paint(g);
    }
}
