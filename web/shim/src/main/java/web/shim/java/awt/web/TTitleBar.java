package web.shim.java.awt.web;

import web.rt.Edt;
import web.rt.Exit;
import web.rt.Js;
import web.rt.Keys;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import web.shim.java.awt.TImage;

/** The title bar (icon, title, close button) shared by JFrame and JInternalFrame boxes. */
public final class TTitleBar {
    /** Classic Swing-like decoration sizes: 24 px title bar plus a 4 px frame border. */
    public static final int HEIGHT = 24;
    public static final int BORDER = 4;

    private final JSObject bar;
    private final JSObject icon;
    private final JSObject title;

    public TTitleBar(JSObject parent, Runnable onClose) {
        bar = Js.create("div");
        Js.setClass(bar, "titlebar");
        icon = Js.create("span");
        Js.setClass(icon, "icon");
        Js.append(bar, icon);
        title = Js.create("span");
        Js.setClass(title, "title");
        Js.append(bar, title);
        JSObject close = Js.create("button");
        Js.setClass(close, "close");
        Js.setAttr(close, "aria-label", "Close");
        Js.setAttr(close, "title", "Close");
        Js.listen(close, "click", e -> Edt.post(onClose));
        Js.append(bar, close);
        Js.append(parent, bar);
    }

    public void update(boolean shown, String text, boolean closable) {
        Js.style(bar, "display", shown ? "flex" : "none");
        Js.setText(title, text == null ? "" : text);
        Js.toggleClass(bar, "no-close", !closable);
    }

    public void setIcon(TImage image) {
        clear(icon);
        if (image != null && image.getWidth(null) > 0) {
            TImage scaled = image.getWidth(null) == 16 && image.getHeight(null) == 16 ? image
                    : image.getScaledInstance(16, 16, TImage.SCALE_SMOOTH);
            Js.append(icon, scaled.toCanvas());
        }
    }

    public boolean contains(JSObject node) {
        return Js.contains(bar, node);
    }

    @JSBody(params = "el", script = "while (el.firstChild) el.removeChild(el.firstChild);")
    private static native void clear(JSObject el);
}
