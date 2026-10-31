package web.rt;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** Thin JavaScript interop used by the shim. Kept as raw JS snippets so the shim does not depend on JSO wrapper APIs. */
public final class Js {
    private Js() {
    }

    @JSFunctor
    public interface Callback extends JSObject {
        void call();
    }

    @JSFunctor
    public interface EventCallback extends JSObject {
        void handle(JSObject event);
    }

    // ---- DOM ----

    @JSBody(params = "tag", script = "return document.createElement(tag);")
    public static native JSObject create(String tag);

    @JSBody(params = { "el", "cls" }, script = "el.className = cls;")
    public static native void setClass(JSObject el, String cls);

    @JSBody(params = { "el", "cls", "on" }, script = "el.classList.toggle(cls, on);")
    public static native void toggleClass(JSObject el, String cls, boolean on);

    @JSBody(params = { "parent", "child" }, script = "parent.appendChild(child);")
    public static native void append(JSObject parent, JSObject child);


    @JSBody(params = "el", script = "if (el.parentNode) el.parentNode.removeChild(el);")
    public static native void remove(JSObject el);

    @JSBody(params = { "el", "x", "y", "w", "h" }, script =
            "var _s = el.style; _s.left = x + 'px'; _s.top = y + 'px'; _s.width = w + 'px'; _s.height = h + 'px';")
    public static native void setBounds(JSObject el, int x, int y, int w, int h);

    @JSBody(params = { "el", "name", "value" }, script = "el.style.setProperty(name, value);")
    public static native void style(JSObject el, String name, String value);

    @JSBody(params = { "el", "name" }, script = "el.style.removeProperty(name);")
    public static native void unstyle(JSObject el, String name);

    @JSBody(params = { "el", "text" }, script = "el.textContent = text;")
    public static native void setText(JSObject el, String text);

    @JSBody(params = { "el", "name", "value" }, script = "el.setAttribute(name, value);")
    public static native void setAttr(JSObject el, String name, String value);

    @JSBody(params = { "el", "name" }, script = "el.removeAttribute(name);")
    public static native void removeAttr(JSObject el, String name);

    @JSBody(params = { "el", "on" }, script = "el.disabled = on;")
    public static native void setDisabled(JSObject el, boolean on);

    @JSBody(params = "el", script = "return el.value;")
    public static native String getValue(JSObject el);

    @JSBody(params = { "el", "v" }, script = "el.value = v;")
    public static native void setValue(JSObject el, String v);

    @JSBody(params = "el", script = "return el.selectedIndex;")
    public static native int getSelectedIndex(JSObject el);

    @JSBody(params = { "el", "i" }, script = "el.selectedIndex = i;")
    public static native void setSelectedIndex(JSObject el, int i);

    @JSBody(params = { "el", "text" }, script =
            "var _o = document.createElement('option'); _o.textContent = text; el.appendChild(_o);")
    public static native void addOption(JSObject el, String text);

    @JSBody(params = "el", script = "el.focus({preventScroll: true});")
    public static native void focus(JSObject el);

    @JSBody(params = { "el", "id" }, script = "el.__cid = id;")
    public static native void setComponentId(JSObject el, int id);

    /** Walks up from a DOM node to the nearest element that belongs to a Java component. */
    @JSBody(params = "node", script =
            "for (var _e = node; _e; _e = _e.parentNode) { if (_e.__cid !== undefined) return _e.__cid; } return -1;")
    public static native int componentIdOf(JSObject node);

    @JSBody(params = { "el", "type", "cb" }, script = "el.addEventListener(type, cb);")
    public static native void listen(JSObject el, String type, EventCallback cb);

    @JSBody(script = "return document.getElementById('desktop');")
    public static native JSObject desktopElement();

    @JSBody(script = "return document.body;")
    public static native JSObject body();

    @JSBody(script = "return document;")
    public static native JSObject document();


    @JSBody(script = "return window.innerWidth;")
    public static native int viewportWidth();

    @JSBody(script = "return window.innerHeight;")
    public static native int viewportHeight();

    @JSBody(script = "return window.devicePixelRatio || 1;")
    public static native double pixelRatio();

    @JSBody(params = "el", script = "var _r = el.getBoundingClientRect(); return _r.left;")
    public static native double clientLeft(JSObject el);

    @JSBody(params = "el", script = "var _r = el.getBoundingClientRect(); return _r.top;")
    public static native double clientTop(JSObject el);

    // ---- events ----

    @JSBody(params = "e", script = "return e.target;")
    public static native JSObject target(JSObject e);

    @JSBody(params = "e", script = "return e.clientX;")
    public static native double clientX(JSObject e);

    @JSBody(params = "e", script = "return e.clientY;")
    public static native double clientY(JSObject e);

    @JSBody(params = "e", script = "return e.button;")
    public static native int button(JSObject e);

    @JSBody(params = "e", script = "return e.buttons;")
    public static native int buttons(JSObject e);

    @JSBody(params = "e", script = "return e.detail || 1;")
    public static native int detail(JSObject e);

    @JSBody(params = "e", script = "return e.keyCode;")
    public static native int keyCode(JSObject e);

    @JSBody(params = "e", script = "return e.key || '';")
    public static native String key(JSObject e);


    @JSBody(params = "e", script = "return (e.shiftKey ? 1 : 0) | (e.ctrlKey ? 2 : 0) | (e.metaKey ? 4 : 0) | (e.altKey ? 8 : 0);")
    public static native int modifiers(JSObject e);

    @JSBody(params = "e", script = "e.preventDefault();")
    public static native void preventDefault(JSObject e);


    @JSBody(params = "e", script = "var _t = e.target; return !!(_t && _t.closest && _t.closest('input, select, textarea'));")
    public static native boolean targetIsTextInput(JSObject e);

    @JSBody(params = "e", script = "var _t = e.target; return !!(_t && _t.closest && _t.closest('button'));")
    public static native boolean targetIsButton(JSObject e);

    @JSBody(params = { "titlebar", "node" }, script = "return !!titlebar && titlebar.contains(node);")
    public static native boolean contains(JSObject titlebar, JSObject node);

    @JSBody(params = "e", script = "var _t = e.target; return !!(_t && _t.closest && _t.closest('.titlebar button'));")
    public static native boolean targetIsWindowControl(JSObject e);

    // ---- timers ----

    @JSBody(params = { "cb", "ms" }, script = "return setTimeout(cb, ms);")
    public static native int setTimeout(Callback cb, int ms);

    @JSBody(params = "id", script = "clearTimeout(id);")
    public static native void clearTimeout(int id);

    @JSBody(params = "cb", script = "requestAnimationFrame(cb);")
    public static native void requestAnimationFrame(Callback cb);

    // ---- canvas ----

    @JSBody(params = "canvas", script = "return canvas.getContext('2d');")
    public static native JSObject context2d(JSObject canvas);

    /** Sets the canvas backing store to CSS size times devicePixelRatio. Returns true if it changed (which clears it). */
    @JSBody(params = { "canvas", "w", "h" }, script =
            "var _r = window.devicePixelRatio || 1; var _bw = Math.max(1, Math.round(w * _r)), _bh = Math.max(1, Math.round(h * _r));"
            + "canvas.style.width = w + 'px'; canvas.style.height = h + 'px';"
            + "if (canvas.width === _bw && canvas.height === _bh) return false;"
            + "canvas.width = _bw; canvas.height = _bh; return true;")
    public static native boolean sizeCanvas(JSObject canvas, int w, int h);

    @JSBody(params = "canvas", script =
            "var _g = canvas.getContext('2d'); _g.save(); _g.setTransform(1, 0, 0, 1, 0, 0); _g.clearRect(0, 0, canvas.width, canvas.height); _g.restore();")
    public static native void clearCanvas(JSObject canvas);

    @JSBody(params = { "canvas", "color" }, script =
            "var _g = canvas.getContext('2d'); _g.save(); _g.setTransform(1, 0, 0, 1, 0, 0); _g.fillStyle = color;"
            + " _g.fillRect(0, 0, canvas.width, canvas.height); _g.restore();")
    public static native void fillCanvas(JSObject canvas, String color);

    @JSBody(params = { "canvas", "w", "h" }, script = "canvas.width = w; canvas.height = h;")
    public static native void sizeCanvasExact(JSObject canvas, int w, int h);

    @JSBody(params = { "canvas", "img", "w", "h" }, script =
            "var _g = canvas.getContext('2d'); var _r = window.devicePixelRatio || 1; _g.setTransform(_r, 0, 0, _r, 0, 0);"
            + "_g.imageSmoothingQuality = 'high'; _g.drawImage(img, 0, 0, w, h);")
    public static native void drawImageScaled(JSObject canvas, JSObject img, int w, int h);

    @JSBody(params = "canvas", script = "return canvas.toDataURL();")
    public static native String toDataUrl(JSObject canvas);

    /** Returns the pixel at viewport point (x, y) as 0xRRGGBB from the paint canvases and widget backgrounds there, or -1. */
    @JSBody(params = { "x", "y" }, script =
            "var _px = function (_c, _px0, _py0) { var _r = _c.getBoundingClientRect(); if (_px0 < _r.left || _py0 < _r.top || _px0 >= _r.right || _py0 >= _r.bottom || !_c.width) return -1;"
            + "  var _d = _c.getContext('2d').getImageData(Math.floor((_px0 - _r.left) * _c.width / _r.width), Math.floor((_py0 - _r.top) * _c.height / _r.height), 1, 1).data;"
            + "  return _d[3] === 0 ? -1 : (_d[0] << 16) | (_d[1] << 8) | _d[2]; };"
            + "var _els = document.elementsFromPoint(x, y);"
            + "for (var _i = 0; _i < _els.length; _i++) { var _e = _els[_i], _v;"
            + "  var _top = _e.querySelector(':scope > canvas.paint.top'); if (_top && (_v = _px(_top, x, y)) >= 0) return _v; }"
            + "for (var _i = 0; _i < _els.length; _i++) { var _e = _els[_i], _v;"
            + "  var _own = _e.querySelector(':scope > canvas.paint:not(.top)'); if (_own && (_v = _px(_own, x, y)) >= 0) return _v;"
            + "  var _bg = getComputedStyle(_e).backgroundColor; var _m = /rgba?\\((\\d+), (\\d+), (\\d+)(, ([\\d.]+))?\\)/.exec(_bg);"
            + "  if (_m && (_m[5] === undefined || +_m[5] > 0)) return (+_m[1] << 16) | (+_m[2] << 8) | +_m[3]; }"
            + "return -1;")
    public static native int pixelAt(int x, int y);

    // ---- text measuring ----

    @JSBody(params = { "cssFont", "str" }, script =
            "var _c = window.__measureCtx || (window.__measureCtx = document.createElement('canvas').getContext('2d'));"
            + "_c.font = cssFont; return _c.measureText(str).width;")
    public static native double measure(String font, String text);

    // ---- preloaded files (filled by boot.js before main runs) ----

    @JSBody(script = "return Object.keys(window.__shimFiles || {});")
    public static native String[] preloadedNames();

    @JSBody(params = "name", script = "return window.__shimFiles[name];")
    public static native JSObject preloadedBytes(String name);

    @JSBody(params = "bytes", script = "return bytes.length;")
    public static native int length(JSObject bytes);

    @JSBody(params = { "bytes", "i" }, script = "return bytes[i];")
    public static native int byteAt(JSObject bytes, int i);

    /** Finds a preloaded image by path. Falls back to a case-insensitive match, like the Windows file system these apps were written on. */
    @JSBody(params = "name", script =
            "var _m = window.__shimImages || {}; if (_m[name]) return _m[name];"
            + "var _l = name.toLowerCase(); for (var _k in _m) { if (_k.toLowerCase() === _l) return _m[_k]; } return null;")
    public static native JSObject preloadedImage(String name);

    @JSBody(params = "img", script = "return img.naturalWidth;")
    public static native int naturalWidth(JSObject img);

    @JSBody(params = "img", script = "return img.naturalHeight;")
    public static native int naturalHeight(JSObject img);

    @JSBody(params = "img", script = "return img.src;")
    public static native String src(JSObject img);


    @JSBody(params = "o", script = "return o == null;")
    public static native boolean isNull(JSObject o);

    @JSBody(params = "msg", script = "console.error(msg);")
    public static native void consoleError(String msg);

    @JSBody(params = "msg", script = "if (window.__shimExited) window.__shimExited(msg);")
    public static native void notifyExited(String msg);
}
