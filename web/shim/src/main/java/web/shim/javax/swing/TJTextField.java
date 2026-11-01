package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TFontMetrics;
import web.shim.java.awt.event.TActionEvent;
import web.shim.java.awt.event.TActionListener;
import web.shim.java.awt.event.TKeyEvent;

/** A DOM text input. The DOM holds the text once the element exists; editing is native. */
public class TJTextField extends TJComponent {
    /** Metal text field border insets (2 px) plus margin. */
    private static final int PAD = 3;

    private final List<TActionListener> actionListeners = new ArrayList<>();
    private String text;
    private int columns;
    private boolean editable = true;

    public TJTextField() {
        this("", 0);
    }

    public TJTextField(String text) {
        this(text, 0);
    }

    public TJTextField(int columns) {
        this("", columns);
    }

    public TJTextField(String text, int columns) {
        this.text = text == null ? "" : text;
        this.columns = columns;
        setOpaque(true);
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("input");
        Js.setClass(el, "c textfield");
        Js.setAttr(el, "type", "text");
        Js.setAttr(el, "spellcheck", "false");
        Js.setAttr(el, "autocomplete", "off");
        Js.setValue(el, text);
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        Js.setDisabled(element(), !isEnabled());
        if (editable) {
            Js.removeAttr(element(), "readonly");
        } else {
            Js.setAttr(element(), "readonly", "");
        }
    }

    @Override
    protected void syncEnabled() {
        if (hasElement()) {
            Js.setDisabled(element(), !isEnabled());
        }
    }

    @Override
    protected TColor defaultBackground() {
        return TEXT_BACKGROUND;
    }

    @Override
    protected TColor defaultForeground() {
        return TEXT_FOREGROUND;
    }

    @Override
    protected TFont defaultFont() {
        return TEXT_FONT;
    }

    @Override
    public boolean acceptsFocus() {
        return isFocusable();
    }

    @Override
    protected void focusGained() {
        Js.focus(element());
    }

    @Override
    protected TDimension computePreferredSize() {
        TFontMetrics fm = getFontMetrics(getFont());
        int w = columns > 0 ? columns * fm.charWidth('m') : fm.stringWidth(getText()) + 1;
        return new TDimension(w + 2 * PAD, fm.getHeight() + 2 * PAD);
    }

    @Override
    protected void processKeyEvent(TKeyEvent e) {
        super.processKeyEvent(e);
        if (e.getID() == TKeyEvent.KEY_PRESSED && e.getKeyCode() == 10) {
            TActionEvent action = new TActionEvent(this, TActionEvent.ACTION_PERFORMED, getText());
            for (TActionListener l : new ArrayList<>(actionListeners)) {
                l.actionPerformed(action);
            }
        }
    }

    public String getText() {
        return hasElement() ? Js.getValue(element()) : text;
    }

    public void setText(String t) {
        text = t == null ? "" : t;
        if (hasElement()) {
            Js.setValue(element(), text);
        }
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
        invalidate();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        editable = b;
        if (hasElement()) {
            syncAll();
        }
    }

    public void addActionListener(TActionListener l) {
        if (l != null) {
            actionListeners.add(l);
        }
    }

    public void removeActionListener(TActionListener l) {
        actionListeners.remove(l);
    }

}
