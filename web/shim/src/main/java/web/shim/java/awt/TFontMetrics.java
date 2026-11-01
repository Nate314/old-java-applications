package web.shim.java.awt;

import web.rt.Js;

/** Measures with a canvas, using the same CSS font the DOM widgets use, so preferred sizes match what is drawn. */
public class TFontMetrics {
    protected TFont font;

    public TFontMetrics(TFont font) {
        this.font = font;
    }

    public TFont getFont() {
        return font;
    }

    public int stringWidth(String s) {
        return s == null || s.isEmpty() ? 0 : (int) Math.ceil(Js.measure(font.css(), s));
    }

    public int charWidth(char c) {
        return stringWidth(String.valueOf(c));
    }

    public int getAscent() {
        return (int) Math.ceil(font.getSize() * 0.91);
    }

    public int getDescent() {
        return (int) Math.ceil(font.getSize() * 0.22);
    }

    public int getLeading() {
        return 0;
    }

    public int getHeight() {
        return getAscent() + getDescent() + getLeading();
    }

    public int getMaxAscent() {
        return getAscent();
    }

    public int getMaxDescent() {
        return getDescent();
    }
}
