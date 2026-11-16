package web.shim.javax.swing;

import org.teavm.jso.JSObject;
import web.rt.Js;
import web.shim.java.awt.TDimension;

/**
 * A Swing check box: a button drawn flat with a box before its text (shim.css, .c.check), ticked while selected.
 * A click flips the selection before the action listeners hear of it, as in Swing.
 */
public class TJCheckBox extends TAbstractButton {
    /** The box and the gap after it (shim.css draws them). */
    private static final int BOX = 19;

    public TJCheckBox() {
        this("", false);
    }

    public TJCheckBox(String text) {
        this(text, false);
    }

    public TJCheckBox(String text, boolean selected) {
        setText(text);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setSelected(selected);
    }

    @Override
    protected JSObject createElement() {
        JSObject el = super.createElement();
        Js.setClass(el, "c button check no-border");
        Js.setAttr(el, "role", "checkbox");
        // Registered after the button's own listener, which only posts the action: the box flips first.
        Js.listen(el, "click", e -> {
            if (isEnabled()) {
                setSelected(!isSelected());
            }
        });
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        showSelected();
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        showSelected();
    }

    @Override
    public void doClick() {
        setSelected(!isSelected());
        super.doClick();
    }

    private void showSelected() {
        if (hasElement()) {
            Js.toggleClass(element(), "on", isSelected());
            Js.setAttr(element(), "aria-checked", String.valueOf(isSelected()));
        }
    }

    @Override
    protected TDimension computePreferredSize() {
        TDimension size = super.computePreferredSize();
        return new TDimension(size.width + BOX, size.height);
    }
}
