package web.shim.javax.swing;

import org.teavm.jso.JSObject;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TFontMetrics;

public class TJLabel extends TJComponent implements TSwingConstants {
    private String text;
    private int horizontalAlignment;

    public TJLabel() {
        this("", LEADING);
    }

    public TJLabel(String text) {
        this(text, LEADING);
    }

    public TJLabel(String text, int horizontalAlignment) {
        this.text = text == null ? "" : text;
        this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("div");
        Js.setClass(el, "c label");
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        syncText();
        Js.style(element(), "justify-content", horizontalAlignment == CENTER ? "center"
                : horizontalAlignment == RIGHT || horizontalAlignment == TRAILING ? "flex-end" : "flex-start");
    }

    private boolean isHtml() {
        return text.regionMatches(true, 0, "<html>", 0, 6);
    }

    private void syncText() {
        if (isHtml()) {
            Js.setHtml(element(), text);
        } else {
            Js.setText(element(), text);
        }
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
    protected TDimension computePreferredSize() {
        if (isHtml()) {
            String css = getFont().css();
            return new TDimension((int) Math.ceil(Js.measureHtml(css, text, false)),
                    (int) Math.ceil(Js.measureHtml(css, text, true)));
        }
        TFontMetrics fm = getFontMetrics(getFont());
        return new TDimension(fm.stringWidth(text), text.isEmpty() ? 0 : fm.getHeight());
    }

    @Override
    public boolean acceptsFocus() {
        return false;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        if (hasElement()) {
            syncText();
        }
        invalidate();
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        horizontalAlignment = alignment;
        if (hasElement()) {
            syncAll();
        }
    }
}
