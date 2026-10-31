package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.rt.Edt;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TFontMetrics;
import web.shim.java.awt.event.TActionEvent;
import web.shim.java.awt.event.TActionListener;

/** A DOM select. */
public class TJComboBox<E> extends TJComponent {
    private final List<E> items = new ArrayList<>();
    private final List<TActionListener> actionListeners = new ArrayList<>();
    private int selectedIndex = -1;

    public TJComboBox() {
        setOpaque(true);
    }

    public TJComboBox(E[] items) {
        this();
        for (E item : items) {
            addItem(item);
        }
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("select");
        Js.setClass(el, "c combo");
        for (E item : items) {
            Js.addOption(el, String.valueOf(item));
        }
        Js.setSelectedIndex(el, selectedIndex);
        Js.listen(el, "change", e -> {
            selectedIndex = Js.getSelectedIndex(el);
            Edt.post(this::fireActionPerformed);
        });
        return el;
    }

    @Override
    protected void syncEnabled() {
        if (hasElement()) {
            Js.setDisabled(element(), !isEnabled());
        }
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
        return CONTROL_FONT;
    }

    @Override
    public boolean acceptsFocus() {
        return isFocusable();
    }

    @Override
    protected TDimension computePreferredSize() {
        TFontMetrics fm = getFontMetrics(getFont());
        int w = 0;
        for (E item : items) {
            w = Math.max(w, fm.stringWidth(String.valueOf(item)));
        }
        // text inset + arrow button, as in Metal's combo box
        return new TDimension(w + 8 + 22, fm.getHeight() + 8);
    }

    public void addItem(E item) {
        items.add(item);
        if (hasElement()) {
            Js.addOption(element(), String.valueOf(item));
        }
        if (selectedIndex < 0) {
            setSelectedIndex(0);
        }
        invalidate();
    }

    public int getItemCount() {
        return items.size();
    }

    public E getItemAt(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        if (hasElement()) {
            Js.setSelectedIndex(element(), index);
        }
    }

    public Object getSelectedItem() {
        return getItemAt(selectedIndex);
    }

    public void setSelectedItem(Object item) {
        setSelectedIndex(items.indexOf(item));
    }

    public void addActionListener(TActionListener l) {
        if (l != null) {
            actionListeners.add(l);
        }
    }

    public void removeActionListener(TActionListener l) {
        actionListeners.remove(l);
    }

    private void fireActionPerformed() {
        TActionEvent e = new TActionEvent(this, TActionEvent.ACTION_PERFORMED, "comboBoxChanged");
        for (TActionListener l : new ArrayList<>(actionListeners)) {
            l.actionPerformed(e);
        }
    }
}
