package web.shim.java.awt.web;

import web.rt.Edt;
import web.rt.Exit;
import web.rt.Js;
import web.rt.Keys;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TKeyboardFocusManager;
import web.shim.java.awt.TWindow;
import web.shim.java.awt.event.TKeyEvent;
import web.shim.java.awt.event.TMouseEvent;
import web.shim.javax.swing.TAbstractButton;
import web.shim.javax.swing.TJInternalFrame;

/**
 * The page "screen": a full-viewport div holding one box per top-level window. Owns the component registry,
 * window z-order, keyboard focus, title-bar dragging and translation of DOM input into AWT events.
 */
public final class TDesktop {
    private static final List<TComponent> registry = new ArrayList<>();
    private static final List<TWindow> windows = new ArrayList<>();
    private static JSObject element;
    private static TWindow active;
    private static int zCounter = 10;

    private static TComponent pressTarget;
    private static TComponent hoverTarget;
    private static TComponent dragWindow;
    private static int dragStartX;
    private static int dragStartY;
    private static double dragMouseX;
    private static double dragMouseY;

    // ---- touch: the one finger (or pen) that drives the mouse, and its long press ----
    private static final int NO_TOUCH = Integer.MIN_VALUE;
    private static final int LONG_PRESS_MS = 500;
    private static final double TOUCH_SLOP = 10;
    private static int touchId = NO_TOUCH;
    private static double touchAt = -1e9;
    private static JSObject touchDown;
    private static int longPressTimer = -1;
    private static JSObject pressRing;
    private static boolean longPressed;
    private static boolean swallowClick;
    private static final int DOUBLE_TAP_MS = 500; // the Windows default double-click time
    private static int tapCount;
    private static double lastTapAt = -1e9;
    private static double lastTapX;
    private static double lastTapY;

    static {
        Exit.onExit = TDesktop::disposeAll;
        // Background threads end by throwing ExitError once the program exits; that is not an error.
        Thread.UncaughtExceptionHandler fallback = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (!(e instanceof Exit.ExitError)) {
                if (fallback != null) {
                    fallback.uncaughtException(t, e);
                } else {
                    System.err.println("Exception in thread \"" + t.getName() + "\" " + e);
                    e.printStackTrace();
                }
            }
        });
    }

    private TDesktop() {
    }

    public static int register(TComponent c) {
        registry.add(c);
        return registry.size() - 1;
    }

    private static TComponent lookup(JSObject node) {
        int id = Js.componentIdOf(node);
        return id >= 0 && id < registry.size() ? registry.get(id) : null;
    }

    public static JSObject element() {
        if (element == null) {
            element = Js.desktopElement();
            if (Js.isNull(element)) {
                element = Js.create("div");
                Js.setAttr(element, "id", "desktop");
                Js.append(Js.body(), element);
            }
            installListeners();
        }
        return element;
    }

    public static int screenWidth() {
        return Js.viewportWidth();
    }

    public static int screenHeight() {
        return Js.viewportHeight();
    }

    public static List<TWindow> windows() {
        return new ArrayList<>(windows);
    }

    // ---- windows ----

    public static void show(TWindow w) {
        if (!windows.contains(w)) {
            windows.add(w);
            Js.append(element(), w.element());
        }
        activate(w);
        TWindow shown = w;
        Edt.post(() -> {
            if (shown.isVisible() && shown.focusOwner() == null) {
                shown.setFocusOwner(firstFocusable(shown));
            }
        });
    }

    public static void hide(TWindow w) {
        if (active == w) {
            active = null;
            for (TWindow other : windows) {
                if (other != w && other.isVisible()) {
                    active = other;
                }
            }
        }
    }

    public static void dispose(TWindow w) {
        hide(w);
        windows.remove(w);
        Js.remove(w.element());
    }

    public static void disposeAll() {
        for (TWindow w : new ArrayList<>(windows)) {
            w.dispose();
        }
    }

    public static void activate(TWindow w) {
        active = w;
        Js.style(w.element(), "z-index", String.valueOf(w.isAlwaysOnTop() ? 1000000 + ++zCounter : ++zCounter));
        for (TWindow other : windows) {
            Js.toggleClass(other.element(), "active", other == w);
        }
    }

    public static TWindow activeWindow() {
        return active;
    }

    private static TComponent firstFocusable(TComponent c) {
        if (!c.isVisible() || !c.isEnabled()) {
            return null;
        }
        if (c.acceptsFocus()) {
            return c;
        }
        if (c instanceof TContainer) {
            TContainer container = (TContainer) c;
            for (int i = 0; i < container.getComponentCount(); i++) {
                TComponent found = firstFocusable(container.getComponent(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ---- input ----

    private static void installListeners() {
        JSObject doc = Js.document();
        // The mouse arrives as mouse events. A browser also turns a tap into mouse events after the touch ones, so
        // those are ignored for a moment after a touch: fingers and pens arrive as pointer events (see onTouch*).
        Js.listen(doc, "mousedown", e -> {
            if (!afterTouch()) {
                swallowClick = false;
                onPress(e, Js.target(e));
            }
        });
        Js.listen(doc, "mouseup", e -> {
            if (!afterTouch()) {
                onRelease(e, Js.target(e), true);
            }
        });
        Js.listen(doc, "mousemove", e -> {
            if (!afterTouch()) {
                onMove(e, Js.target(e));
            }
        });
        Js.listen(doc, "mouseleave", e -> setHover(null, e));
        Js.listen(doc, "pointerdown", TDesktop::onTouchDown);
        Js.listen(doc, "pointermove", TDesktop::onTouchMove);
        Js.listen(doc, "pointerup", e -> onTouchUp(e, true));
        Js.listen(doc, "pointercancel", e -> onTouchUp(e, false));
        Js.listenCapture(doc, "click", e -> {
            if (swallowClick) { // the lift that ends a long press is not also a click on the button
                swallowClick = false;
                Js.swallow(e);
            }
        });
        Js.listen(doc, "contextmenu", e -> {
            if (lookup(Js.target(e)) != null) {
                Js.preventDefault(e);
            }
        });
        Js.listen(doc, "keydown", e -> onKey(e, TKeyEvent.KEY_PRESSED));
        Js.listen(doc, "keyup", e -> onKey(e, TKeyEvent.KEY_RELEASED));
    }

    private static void onPress(JSObject e, JSObject node) {
        TComponent c = lookup(node);
        if (c == null || Js.targetIsWindowControl(e)) {
            return;
        }
        TWindow window = c.windowAncestor();
        if (window != null && window.getFocusableWindowState()) {
            activate(window);
        }
        for (TComponent p = c; p != null; p = p.getParent()) {
            if (p instanceof TJInternalFrame) {
                ((TJInternalFrame) p).moveToFront();
                break;
            }
        }
        if (c.isTitleBar(Js.target(e))) {
            TComponent frame = c;
            dragWindow = frame;
            dragStartX = frame instanceof TWindow ? Math.max(frame.getX(), 0) : frame.getX();
            dragStartY = frame instanceof TWindow ? Math.max(frame.getY(), 0) : frame.getY(); // see TWindow.domY
            dragMouseX = Js.clientX(e);
            dragMouseY = Js.clientY(e);
            Js.preventDefault(e);
            return;
        }
        if (window != null && c.acceptsFocus() && c.isEnabled()) {
            window.setFocusOwner(c);
        }
        if (!Js.targetIsTextInput(e) && !c.acceptsFocus()) {
            Js.preventDefault(e); // keep keyboard focus where AWT has it, and avoid text selection
        }
        pressTarget = mouseTarget(c, false);
        post(pressTarget, TMouseEvent.MOUSE_PRESSED, e);
    }

    private static void onRelease(JSObject e, JSObject node, boolean click) {
        if (dragWindow != null) {
            dragWindow = null;
            return;
        }
        TComponent target = pressTarget;
        pressTarget = null;
        if (target == null) {
            return;
        }
        post(target, TMouseEvent.MOUSE_RELEASED, e);
        TComponent under = lookup(node);
        if (click && under != null && mouseTarget(under, false) == target) {
            post(target, TMouseEvent.MOUSE_CLICKED, e);
        }
    }

    private static void onMove(JSObject e, JSObject node) {
        if (dragWindow != null) {
            double scale = Js.desktopScale();
            dragWindow.setLocation(dragStartX + (int) ((Js.clientX(e) - dragMouseX) / scale),
                    dragStartY + (int) ((Js.clientY(e) - dragMouseY) / scale));
            return;
        }
        TComponent c = lookup(node);
        setHover(c == null ? null : mouseTarget(c, false), e);
        if (pressTarget != null && Js.buttons(e) != 0) {
            if (pressTarget.hasMouseMotionListeners()) {
                post(pressTarget, TMouseEvent.MOUSE_DRAGGED, e);
            }
        } else if (c != null) {
            TComponent motion = mouseTarget(c, true);
            if (motion != null) {
                post(motion, TMouseEvent.MOUSE_MOVED, e);
            }
        }
    }

    // ---- touch ----
    // A finger works the apps as the left mouse button: a tap clicks, a drag drags (windows by their title bars,
    // Clock's dial, GroupCreator's names, the Hanoi disks). Holding still on a button that listens to the mouse
    // itself (Minesweeper's tiles) is a right click once LONG_PRESS_MS have passed, as on a phone's desktop.

    private static boolean afterTouch() {
        return Js.now() - touchAt < 1000;
    }

    private static void onTouchDown(JSObject e) {
        if (!Js.isTouch(e)) {
            return;
        }
        touchAt = Js.now();
        swallowClick = false;
        if (touchId != NO_TOUCH || lookup(Js.target(e)) == null) {
            return; // a second finger, or not on an app
        }
        touchId = Js.pointerId(e);
        touchDown = e;
        // A pointer event's detail is 0, so taps are counted here: a quick second tap nearby is a double click
        // (NathaNOS opens its desktop icons on one).
        double x = Js.clientX(e);
        double y = Js.clientY(e);
        boolean again = touchAt - lastTapAt < DOUBLE_TAP_MS && Math.hypot(x - lastTapX, y - lastTapY) < 3 * TOUCH_SLOP;
        tapCount = again ? tapCount + 1 : 1;
        lastTapAt = touchAt;
        lastTapX = x;
        lastTapY = y;
        longPressed = false;
        TComponent c = lookup(Js.target(e));
        setHover(mouseTarget(c, false), e);
        onPress(e, Js.target(e));
        TComponent target = pressTarget;
        if (target instanceof TAbstractButton && target.hasMouseListeners() && target.isEnabled()) {
            pressRing = Js.pressRing(e);
            longPressTimer = Js.setTimeout(TDesktop::longPress, LONG_PRESS_MS);
        }
    }

    private static void onTouchMove(JSObject e) {
        if (Js.pointerId(e) != touchId || !Js.isTouch(e)) {
            return;
        }
        touchAt = Js.now();
        if (Math.hypot(Js.clientX(e) - Js.clientX(touchDown), Js.clientY(e) - Js.clientY(touchDown)) > TOUCH_SLOP) {
            endLongPress();
        }
        if (!longPressed) {
            onMove(e, Js.elementAt(e));
        }
    }

    private static void onTouchUp(JSObject e, boolean lifted) {
        if (Js.pointerId(e) != touchId || !Js.isTouch(e)) {
            return;
        }
        touchAt = Js.now();
        touchId = NO_TOUCH;
        endLongPress();
        if (!longPressed) {
            onRelease(e, Js.elementAt(e), lifted); // a cancelled touch (the browser took it) releases without a click
        }
        setHover(null, e);
    }

    private static void endLongPress() {
        if (longPressTimer != -1) {
            Js.clearTimeout(longPressTimer);
            longPressTimer = -1;
        }
        if (pressRing != null) {
            Js.remove(pressRing);
            pressRing = null;
        }
    }

    /** The finger stayed put: the left press ends without a click and a right click takes its place. */
    private static void longPress() {
        longPressTimer = -1;
        TComponent target = pressTarget;
        if (target == null || touchId == NO_TOUCH) {
            return;
        }
        pressTarget = null;
        longPressed = true;
        swallowClick = true;
        Js.pressRingDone(pressRing);
        pressRing = null;
        post(target, TMouseEvent.MOUSE_RELEASED, touchDown, 1);
        post(target, TMouseEvent.MOUSE_PRESSED, touchDown, 3);
        post(target, TMouseEvent.MOUSE_RELEASED, touchDown, 3);
        post(target, TMouseEvent.MOUSE_CLICKED, touchDown, 3);
    }

    private static void setHover(TComponent target, JSObject e) {
        if (target == hoverTarget) {
            return;
        }
        if (hoverTarget != null) {
            post(hoverTarget, TMouseEvent.MOUSE_EXITED, e);
        }
        hoverTarget = target;
        if (target != null) {
            post(target, TMouseEvent.MOUSE_ENTERED, e);
        }
    }

    /** Swing delivers mouse events to the deepest component that listens for them. */
    private static TComponent mouseTarget(TComponent c, boolean motion) {
        for (TComponent p = c; p != null; p = p.getParent()) {
            if (motion ? p.hasMouseMotionListeners() : (p.hasMouseListeners() || p.acceptsFocus())) {
                return p;
            }
        }
        return motion ? null : c;
    }

    private static void post(TComponent target, int id, JSObject e) {
        post(target, id, e, id == TMouseEvent.MOUSE_MOVED || id == TMouseEvent.MOUSE_ENTERED
                || id == TMouseEvent.MOUSE_EXITED ? 0 : Math.max(Js.button(e), 0) + 1);
    }

    private static void post(TComponent target, int id, JSObject e, int button) {
        if (target == null) {
            return;
        }
        double scale = Js.desktopScale();   // the page desktop may be scaled down to fit the stage
        int x = (int) Math.floor((Js.clientX(e) - Js.clientLeft(target.element())) / scale);
        int y = (int) Math.floor((Js.clientY(e) - Js.clientTop(target.element())) / scale);
        int clicks = id == TMouseEvent.MOUSE_CLICKED || id == TMouseEvent.MOUSE_PRESSED || id == TMouseEvent.MOUSE_RELEASED
                ? (Js.isTouch(e) ? tapCount : Js.detail(e)) : 0;
        TMouseEvent event = new TMouseEvent(target, id, System.currentTimeMillis(), awtModifiers(Js.modifiers(e)),
                x, y, (int) Js.clientX(e), (int) Js.clientY(e), clicks, button == 3, button);
        Edt.post(() -> target.dispatchEvent(event));
    }

    private static int awtModifiers(int dom) {
        int m = 0;
        if ((dom & 1) != 0) {
            m |= 1 | 64;
        }
        if ((dom & 2) != 0) {
            m |= 2 | 128;
        }
        if ((dom & 4) != 0) {
            m |= 4 | 256;
        }
        if ((dom & 8) != 0) {
            m |= 8 | 512;
        }
        return m;
    }

    private static void onKey(JSObject e, int id) {
        TWindow w = active;
        if (w == null || !w.isVisible()) {
            return;
        }
        int vk = Keys.toVk(Js.keyCode(e));
        String key = Js.key(e);
        int dom = Js.modifiers(e);
        boolean inText = Js.targetIsTextInput(e);
        boolean shortcut = (dom & (2 | 4)) != 0 || (vk >= 112 && vk <= 123);
        boolean nativeButtonPress = vk == 32 && Js.targetIsButton(e);
        if (!inText && !shortcut && !nativeButtonPress) {
            Js.preventDefault(e); // arrows, space, slash etc. would otherwise scroll or search the page
        }
        TComponent owner = w.focusOwner();
        TComponent source = owner != null && owner.isShowing() ? owner : w;
        int mods = awtModifiers(dom);
        long when = System.currentTimeMillis();
        TKeyEvent event = new TKeyEvent(source, id, when, mods, vk, Keys.pressedChar(key, vk));
        Edt.post(() -> dispatchKey(event));
        if (id == TKeyEvent.KEY_PRESSED) {
            char typed = Keys.typedChar(key, vk, dom);
            if (typed != 0) {
                TKeyEvent typedEvent = new TKeyEvent(source, TKeyEvent.KEY_TYPED, when, mods, 0, typed);
                Edt.post(() -> dispatchKey(typedEvent));
            }
        }
    }

    /** Sends a key event through the KeyEventDispatchers, then to its source. Also used by java.awt.Robot. */
    public static void dispatchKey(TKeyEvent event) {
        if (TKeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchToDispatchers(event)) {
            return;
        }
        ((TComponent) event.getSource()).dispatchEvent(event);
    }

    /** The component that a synthetic (Robot) key event goes to. */
    public static TComponent keyTarget() {
        TWindow w = active;
        if (w == null) {
            return null;
        }
        TComponent owner = w.focusOwner();
        return owner != null && owner.isShowing() ? owner : w;
    }
}
