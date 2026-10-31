package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.rt.Edt;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TInsets;

/** Tabs along the top as DOM buttons; the selected tab's component fills the rest, the others are hidden as in Swing. */
public class TJTabbedPane extends TJComponent {
    private static final int GAP = 2;

    private final List<String> titles = new ArrayList<>();
    private final List<TComponent> tabs = new ArrayList<>();
    private int selectedIndex = -1;
    private JSObject strip;

    public TJTabbedPane() {
        setOpaque(true);
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("div");
        Js.setClass(el, "c tabbedpane");
        strip = Js.create("div");
        Js.setClass(strip, "tabs");
        Js.append(el, strip);
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        syncTabs();
    }

    private void syncTabs() {
        if (strip == null) {
            return;
        }
        Js.style(strip, "height", tabHeight() + "px");
        Js.setText(strip, "");
        for (int i = 0; i < titles.size(); i++) {
            int index = i;
            JSObject tab = Js.create("button");
            Js.setAttr(tab, "type", "button");
            Js.setClass(tab, i == selectedIndex ? "tab selected" : "tab");
            Js.setText(tab, titles.get(i));
            Js.listen(tab, "click", e -> Edt.post(() -> setSelectedIndex(index)));
            Js.append(strip, tab);
        }
    }

    private int tabHeight() {
        return getFontMetrics(getFont()).getHeight() + 6;
    }

    @Override
    protected TColor defaultBackground() {
        return CONTROL;
    }

    @Override
    protected TColor defaultForeground() {
        return CONTROL_TEXT;
    }

    @Override
    protected TFont defaultFont() {
        return TEXT_FONT;
    }

    @Override
    public TInsets getInsets() {
        return new TInsets(GAP + tabHeight() + GAP, GAP, GAP, GAP);
    }

    @Override
    public void doLayout() {
        TInsets in = getInsets();
        for (TComponent c : tabs) {
            c.setBounds(in.left, in.top, getWidth() - in.left - in.right, getHeight() - in.top - in.bottom);
        }
    }

    @Override
    protected TDimension computePreferredSize() {
        TInsets in = getInsets();
        int w = 0;
        int h = 0;
        for (TComponent c : tabs) {
            TDimension d = c.getPreferredSize();
            w = Math.max(w, d.width);
            h = Math.max(h, d.height);
        }
        return new TDimension(w + in.left + in.right, h + in.top + in.bottom);
    }

    public void addTab(String title, TComponent component) {
        titles.add(title == null ? "" : title);
        tabs.add(component);
        add(component);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        } else {
            component.setVisible(false);
        }
        syncTabs();
    }

    public int getTabCount() {
        return tabs.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= tabs.size() || index == selectedIndex) {
            return;
        }
        selectedIndex = index;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setVisible(i == index);
        }
        syncTabs();
        invalidate();
    }

    public TComponent getSelectedComponent() {
        return selectedIndex < 0 ? null : tabs.get(selectedIndex);
    }

    public String getTitleAt(int index) {
        return titles.get(index);
    }
}
