package web.shim.java.awt;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.shim.java.awt.web.TCanvasGraphics;
import web.shim.java.awt.web.TDesktop;
import web.rt.Files;
import web.rt.Js;
import web.shim.java.awt.web.TRepaint;
import web.shim.java.awt.event.TComponentEvent;
import web.shim.java.awt.event.TKeyEvent;
import web.shim.java.awt.event.TKeyListener;
import web.shim.java.awt.event.TMouseEvent;
import web.shim.java.awt.event.TMouseListener;
import web.shim.java.awt.event.TMouseMotionListener;
import web.shim.java.awt.image.TImageObserver;

/**
 * Every component owns one absolutely positioned DOM element. Bounds, visibility, colors and fonts are kept
 * on the Java side (apps compare colors by identity) and mirrored into the element. Custom paint() output goes
 * to a canvas that is created inside the element the first time paint() actually draws.
 */
public class TComponent implements TImageObserver {
    static {
        Files.install(); // every app creates a component before it touches java.io
    }

    TContainer parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;
    boolean enabled = true;
    boolean focusable = true;
    TColor background;
    TColor foreground;
    TFont font;
    TCursor cursor;
    TDimension preferredSize;
    TDimension minimumSize;
    TDimension maximumSize;
    String name;
    boolean valid;
    private boolean dirty = true;
    private final List<TMouseListener> mouseListeners = new ArrayList<>();
    private final List<TMouseMotionListener> mouseMotionListeners = new ArrayList<>();
    private final List<TKeyListener> keyListeners = new ArrayList<>();

    private JSObject element;
    /** Set during paint() when Swing's own background painting ran (JComponent.paintComponent). */
    protected boolean backgroundPainted;
    private JSObject canvas;

    protected TComponent() {
    }

    // ---- DOM ----

    /** The DOM element for this component, created on first use. */
    public final JSObject element() {
        if (element == null) {
            element = createElement();
            Js.setComponentId(element, TDesktop.register(this));
            Js.setBounds(element, domX(x), domY(y), width, height);
            if (!visible) {
                Js.style(element, "display", "none");
            }
            syncAll();
        }
        return element;
    }

    protected final boolean hasElement() {
        return element != null;
    }

    protected JSObject createElement() {
        JSObject el = Js.create("div");
        Js.setClass(el, "c " + cssClass());
        return el;
    }

    /** Skin hook: the CSS class that styles this kind of component. */
    protected String cssClass() {
        return "";
    }

    /** Re-applies every mirrored property to the element. Subclasses add their own. */
    protected void syncAll() {
        syncColors();
        syncFont();
        syncCursor();
        syncEnabled();
    }

    /** Default colors are left to the CSS skin; colors the app sets are applied inline. */
    protected void syncColors() {
        if (element == null) {
            return;
        }
        Js.toggleClass(element, "opaque", isOpaque());
        boolean customBackground = background != null && background != defaultBackground() && isOpaque();
        Js.toggleClass(element, "custom-bg", customBackground);
        if (customBackground) {
            Js.style(element, "background-color", background.css());
        } else {
            Js.unstyle(element, "background-color");
        }
        if (foreground != null && foreground != defaultForeground()) {
            Js.style(element, "color", foreground.css());
        } else {
            Js.unstyle(element, "color");
        }
    }

    protected void syncFont() {
        if (element != null) {
            Js.style(element, "font", getFont().css());
        }
    }

    protected void syncCursor() {
        if (element != null) {
            if (cursor != null) {
                Js.style(element, "cursor", cursor.css());
            } else {
                Js.unstyle(element, "cursor");
            }
        }
    }

    protected void syncEnabled() {
    }

    /** The default background, or null if the component inherits its parent's. */
    protected TColor defaultBackground() {
        return null;
    }

    protected TColor defaultForeground() {
        return null;
    }

    /** Lightweight AWT components and JComponents that are not opaque let the parent show through. */
    public boolean isOpaque() {
        return false;
    }

    // ---- painting ----

    public void paint(TGraphics g) {
    }

    public void update(TGraphics g) {
        paint(g);
    }

    public void paintAll(TGraphics g) {
        paint(g);
    }

    public void repaint() {
        TRepaint.repaint(this);
    }

    public void repaint(long tm) {
        repaint();
    }

    public void repaint(int x, int y, int width, int height) {
        repaint();
    }

    public void repaint(long tm, int x, int y, int width, int height) {
        repaint();
    }

    public void markDirty() {
        dirty = true;
        TRepaint.requestFrame();
    }

    void markTreeDirty() {
        markDirty();
    }

    public boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    /** Runs paint() with a Graphics on this component's (lazily created) canvas. */
    public void paintNow() {
        backgroundPainted = false;
        if (width <= 0 || height <= 0) {
            return;
        }
        if (canvas != null) {
            Js.sizeCanvas(canvas, canvasWidth(), canvasHeight());
        }
        TCanvasGraphics g = new TCanvasGraphics(this::canvasContext, Js.pixelRatio(), getForeground(), getFont());
        g.translate(-canvasX(), -canvasY());
        paint(g);
    }

    /** Clears earlier paint output, if this component has painted before (the CSS background shows through). */
    protected void clearCanvas() {
        if (canvas != null) {
            Js.clearCanvas(canvas);
        }
    }

    private JSObject canvasContext() {
        if (canvas == null) {
            canvas = Js.create("canvas");
            Js.setClass(canvas, canvasOnTop() ? "paint top" : "paint");
            Js.style(canvas, "left", canvasX() + "px");
            Js.style(canvas, "top", canvasY() + "px");
            Js.sizeCanvas(canvas, canvasWidth(), canvasHeight());
            Js.append(canvasParent(), canvas);
            if (isOpaque() && !canvasOnTop() && !backgroundPainted) {
                // Swing paints opaque components into a back buffer that starts out black.
                Js.fillCanvas(canvas, "#000");
            }
        }
        return Js.context2d(canvas);
    }

    /** Where the paint canvas sits inside this component. Windows only expose their client area. */
    protected int canvasX() {
        return 0;
    }

    protected int canvasY() {
        return 0;
    }

    protected int canvasWidth() {
        return width;
    }

    protected int canvasHeight() {
        return height;
    }

    /** Top-level windows draw over their children (JFrame.paint overrides replace the whole window). */
    protected boolean canvasOnTop() {
        return false;
    }

    protected JSObject canvasParent() {
        return element();
    }

    // ---- geometry ----

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TDimension getSize() {
        return new TDimension(width, height);
    }

    public TPoint getLocation() {
        return new TPoint(x, y);
    }

    public TRectangle getBounds() {
        return new TRectangle(x, y, width, height);
    }

    public void setSize(int width, int height) {
        setBounds(x, y, width, height);
    }

    public void setSize(TDimension d) {
        setSize(d.width, d.height);
    }

    public void setLocation(int x, int y) {
        setBounds(x, y, width, height);
    }

    public void setLocation(TPoint p) {
        setLocation(p.x, p.y);
    }

    public void setBounds(TRectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    /** The primitive for every geometry change (AWT's reshape), so overriding setSize cannot recurse. */
    public void setBounds(int x, int y, int width, int height) {
        width = Math.max(width, 0);
        height = Math.max(height, 0);
        boolean resized = width != this.width || height != this.height;
        if (!resized && x == this.x && y == this.y) {
            return;
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (element != null) {
            Js.setBounds(element, domX(x), domY(y), width, height);
        }
        if (resized) {
            if (canvas != null) {
                Js.style(canvas, "left", canvasX() + "px");
                Js.style(canvas, "top", canvasY() + "px");
                Js.sizeCanvas(canvas, canvasWidth(), canvasHeight());
            }
            boundsResized();
            invalidate();
            markDirty();
        }
    }

    /** Where the element is drawn; windows keep their title bar on the page. */
    protected int domX(int x) {
        return x;
    }

    protected int domY(int y) {
        return y;
    }

    protected void boundsResized() {
    }

    public TDimension getPreferredSize() {
        return preferredSize != null ? new TDimension(preferredSize) : computePreferredSize();
    }

    /** Natural size when the app did not set one. */
    protected TDimension computePreferredSize() {
        return new TDimension(Math.max(width, 0), Math.max(height, 0));
    }

    public void setPreferredSize(TDimension d) {
        preferredSize = d;
        invalidate();
    }

    public boolean isPreferredSizeSet() {
        return preferredSize != null;
    }

    public TDimension getMinimumSize() {
        return minimumSize != null ? new TDimension(minimumSize) : getPreferredSize();
    }

    public void setMinimumSize(TDimension d) {
        minimumSize = d;
    }

    public TDimension getMaximumSize() {
        return maximumSize != null ? new TDimension(maximumSize) : new TDimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    public void setMaximumSize(TDimension d) {
        maximumSize = d;
    }

    public void invalidate() {
        valid = false;
        if (parent != null && parent.valid) {
            parent.invalidate();
        }
        TRepaint.requestFrame();
    }

    public void validate() {
        valid = true;
    }

    public void revalidate() {
        invalidate();
    }

    public boolean isValid() {
        return valid;
    }

    public void doLayout() {
    }

    // ---- state ----

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean b) {
        if (visible == b) {
            return;
        }
        visible = b;
        if (element != null) {
            if (b) {
                Js.unstyle(element, "display");
            } else {
                Js.style(element, "display", "none");
            }
        }
        if (b) {
            markTreeDirty();
        }
        if (parent != null) {
            parent.invalidate();
        }
    }

    @Deprecated
    public void show() {
        setVisible(true);
    }

    @Deprecated
    public void hide() {
        setVisible(false);
    }

    public boolean isShowing() {
        if (!visible) {
            return false;
        }
        return parent != null ? parent.isShowing() : this instanceof TWindow && ((TWindow) this).isDisplayed();
    }

    public boolean isDisplayable() {
        return parent != null ? parent.isDisplayable() : this instanceof TWindow && ((TWindow) this).isDisplayed();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b) {
        enabled = b;
        syncEnabled();
    }

    @Deprecated
    public void enable() {
        setEnabled(true);
    }

    @Deprecated
    public void disable() {
        setEnabled(false);
    }

    public TColor getBackground() {
        if (background != null) {
            return background;
        }
        TColor d = defaultBackground();
        if (d != null) {
            return d;
        }
        return parent != null ? parent.getBackground() : null;
    }

    public void setBackground(TColor c) {
        background = c;
        syncColors();
        markDirty();
    }

    public boolean isBackgroundSet() {
        return background != null;
    }

    public TColor getForeground() {
        if (foreground != null) {
            return foreground;
        }
        TColor d = defaultForeground();
        if (d != null) {
            return d;
        }
        return parent != null ? parent.getForeground() : TColor.black;
    }

    public void setForeground(TColor c) {
        foreground = c;
        syncColors();
        markDirty();
    }

    public TFont getFont() {
        if (font != null) {
            return font;
        }
        TFont d = defaultFont();
        if (d != null) {
            return d;
        }
        return parent != null ? parent.getFont() : DIALOG_12;
    }

    static final TFont DIALOG_12 = new TFont("Dialog", TFont.PLAIN, 12);

    protected TFont defaultFont() {
        return null;
    }

    public void setFont(TFont f) {
        font = f;
        syncFont();
        invalidate();
        markDirty();
    }

    public TFontMetrics getFontMetrics(TFont f) {
        return new TFontMetrics(f);
    }

    public TCursor getCursor() {
        if (cursor != null) {
            return cursor;
        }
        return parent != null ? parent.getCursor() : TCursor.getDefaultCursor();
    }

    public void setCursor(TCursor c) {
        cursor = c;
        syncCursor();
    }

    public TToolkit getToolkit() {
        return TToolkit.getDefaultToolkit();
    }

    public TContainer getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TGraphics getGraphics() {
        return new TCanvasGraphics(this::canvasContext, Js.pixelRatio(), getForeground(), getFont());
    }

    @Override
    public boolean imageUpdate(TImage img, int infoflags, int x, int y, int width, int height) {
        return false;
    }

    /** The top-level window this component is in, or null. */
    public TWindow windowAncestor() {
        TComponent c = this;
        while (c != null && !(c instanceof TWindow)) {
            c = c.parent;
        }
        return (TWindow) c;
    }

    /** True if the DOM node is this component's own title bar (windows and internal frames). */
    public boolean isTitleBar(JSObject node) {
        return false;
    }

    // ---- focus ----

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocusable(boolean b) {
        focusable = b;
    }

    /** Whether Swing's focus traversal policy would give this component the keyboard focus. */
    public boolean acceptsFocus() {
        return focusable && !(this instanceof TContainer);
    }

    public void requestFocus() {
        TWindow w = windowAncestor();
        if (w != null) {
            w.setFocusOwner(this);
        }
    }

    public boolean requestFocusInWindow() {
        requestFocus();
        return true;
    }

    public boolean hasFocus() {
        TWindow w = windowAncestor();
        return w != null && w.focusOwner() == this;
    }

    public boolean isFocusOwner() {
        return hasFocus();
    }

    /** Called when this component becomes the focus owner, to move DOM focus if it has an input element. */
    protected void focusGained() {
    }

    // ---- events ----

    public void addMouseListener(TMouseListener l) {
        if (l != null) {
            mouseListeners.add(l);
        }
    }

    public void removeMouseListener(TMouseListener l) {
        mouseListeners.remove(l);
    }

    public void addMouseMotionListener(TMouseMotionListener l) {
        if (l != null) {
            mouseMotionListeners.add(l);
        }
    }

    public void removeMouseMotionListener(TMouseMotionListener l) {
        mouseMotionListeners.remove(l);
    }

    public void addKeyListener(TKeyListener l) {
        if (l != null) {
            keyListeners.add(l);
        }
    }

    public void removeKeyListener(TKeyListener l) {
        keyListeners.remove(l);
    }

    public boolean hasMouseListeners() {
        return !mouseListeners.isEmpty();
    }

    public boolean hasMouseMotionListeners() {
        return !mouseMotionListeners.isEmpty();
    }

    public final void dispatchEvent(TAWTEvent e) {
        processEvent(e);
    }

    protected void processEvent(TAWTEvent e) {
        if (e instanceof TKeyEvent) {
            processKeyEvent((TKeyEvent) e);
        } else if (e instanceof TMouseEvent) {
            int id = e.getID();
            if (id == TMouseEvent.MOUSE_MOVED || id == TMouseEvent.MOUSE_DRAGGED) {
                processMouseMotionEvent((TMouseEvent) e);
            } else {
                processMouseEvent((TMouseEvent) e);
            }
        } else if (e instanceof TComponentEvent) {
            processComponentEvent((TComponentEvent) e);
        }
    }

    protected void processKeyEvent(TKeyEvent e) {
        for (TKeyListener l : new ArrayList<>(keyListeners)) {
            switch (e.getID()) {
                case TKeyEvent.KEY_PRESSED:
                    l.keyPressed(e);
                    break;
                case TKeyEvent.KEY_RELEASED:
                    l.keyReleased(e);
                    break;
                case TKeyEvent.KEY_TYPED:
                    l.keyTyped(e);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processMouseEvent(TMouseEvent e) {
        for (TMouseListener l : new ArrayList<>(mouseListeners)) {
            switch (e.getID()) {
                case TMouseEvent.MOUSE_PRESSED:
                    l.mousePressed(e);
                    break;
                case TMouseEvent.MOUSE_RELEASED:
                    l.mouseReleased(e);
                    break;
                case TMouseEvent.MOUSE_CLICKED:
                    l.mouseClicked(e);
                    break;
                case TMouseEvent.MOUSE_ENTERED:
                    l.mouseEntered(e);
                    break;
                case TMouseEvent.MOUSE_EXITED:
                    l.mouseExited(e);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processMouseMotionEvent(TMouseEvent e) {
        for (TMouseMotionListener l : new ArrayList<>(mouseMotionListeners)) {
            if (e.getID() == TMouseEvent.MOUSE_DRAGGED) {
                l.mouseDragged(e);
            } else {
                l.mouseMoved(e);
            }
        }
    }

    protected void processComponentEvent(TComponentEvent e) {
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + (name != null ? name : "") + "," + x + "," + y + "," + width + "x" + height + "]";
    }
}
