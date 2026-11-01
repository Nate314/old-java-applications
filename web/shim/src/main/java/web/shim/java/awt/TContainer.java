package web.shim.java.awt;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.rt.Js;

public class TContainer extends TComponent {
    final List<TComponent> children = new ArrayList<>();
    TLayoutManager layout;

    public TContainer() {
    }

    // ---- children ----

    public TComponent add(TComponent comp) {
        addImpl(comp, null, -1);
        return comp;
    }

    public TComponent add(TComponent comp, int index) {
        addImpl(comp, null, index);
        return comp;
    }

    public void add(TComponent comp, Object constraints) {
        addImpl(comp, constraints, -1);
    }

    public void add(TComponent comp, Object constraints, int index) {
        addImpl(comp, constraints, index);
    }

    public TComponent add(String name, TComponent comp) {
        addImpl(comp, name, -1);
        return comp;
    }

    protected void addImpl(TComponent comp, Object constraints, int index) {
        if (comp.parent != null) {
            comp.parent.remove(comp);
        }
        if (index < 0 || index > children.size()) {
            index = children.size();
        }
        children.add(index, comp);
        comp.parent = this;
        if (layout instanceof TLayoutManager2) {
            ((TLayoutManager2) layout).addLayoutComponent(comp, constraints);
        } else if (layout != null) {
            layout.addLayoutComponent(constraints instanceof String ? (String) constraints : null, comp);
        }
        Js.append(childHost(), comp.element());
        restack();
        comp.syncAll(); // inherited colors and fonts may have changed
        comp.markTreeDirty();
        invalidate();
    }

    /**
     * AWT paints index 0 on top. Stacking is done with z-index rather than DOM order: moving a node while the
     * mouse is pressed on it would make the browser drop the click.
     */
    void restack() {
        int n = children.size();
        for (int i = 0; i < n; i++) {
            Js.style(children.get(i).element(), "z-index", String.valueOf(n - i));
        }
    }

    /** The DOM element children are placed in. */
    protected JSObject childHost() {
        return element();
    }

    public void remove(TComponent comp) {
        int i = children.indexOf(comp);
        if (i >= 0) {
            remove(i);
        }
    }

    public void remove(int index) {
        TComponent comp = children.remove(index);
        if (layout != null) {
            layout.removeLayoutComponent(comp);
        }
        comp.parent = null;
        if (comp.hasElement()) {
            Js.remove(comp.element());
        }
        invalidate();
        markDirty();
    }

    public void removeAll() {
        while (!children.isEmpty()) {
            remove(children.size() - 1);
        }
    }

    public int getComponentCount() {
        return children.size();
    }

    public TComponent getComponent(int n) {
        return children.get(n);
    }

    public TComponent[] getComponents() {
        return children.toArray(new TComponent[0]);
    }

    public int getComponentZOrder(TComponent comp) {
        return children.indexOf(comp);
    }

    public void setComponentZOrder(TComponent comp, int index) {
        if (children.remove(comp)) {
            children.add(Math.min(index, children.size()), comp);
            restack();
        }
    }

    public boolean isAncestorOf(TComponent c) {
        for (TContainer p = c.parent; p != null; p = p.parent) {
            if (p == this) {
                return true;
            }
        }
        return false;
    }

    @Override
    void markTreeDirty() {
        super.markTreeDirty();
        for (TComponent c : children) {
            c.markTreeDirty();
        }
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        if (hasElement()) {
            for (TComponent c : children) {
                if (c.hasElement()) {
                    c.syncAll();
                }
            }
        }
    }

    // ---- layout ----

    public TLayoutManager getLayout() {
        return layout;
    }

    public void setLayout(TLayoutManager mgr) {
        layout = mgr;
        invalidate();
    }

    public TInsets getInsets() {
        return new TInsets(0, 0, 0, 0);
    }

    @Override
    public void doLayout() {
        if (layout != null) {
            layout.layoutContainer(this);
        }
    }

    @Override
    public void validate() {
        doLayout();
        for (TComponent c : children) {
            c.validate();
        }
        valid = true;
    }

    @Override
    protected TDimension computePreferredSize() {
        return layout != null ? layout.preferredLayoutSize(this) : super.computePreferredSize();
    }

    @Override
    public TDimension getMinimumSize() {
        if (minimumSize == null && layout != null) {
            return layout.minimumLayoutSize(this);
        }
        return super.getMinimumSize();
    }

    @Override
    public boolean acceptsFocus() {
        return false;
    }
}
