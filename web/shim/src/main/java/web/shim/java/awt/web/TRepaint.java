package web.shim.java.awt.web;

import web.rt.Edt;
import web.rt.Exit;
import web.rt.Js;
import web.rt.Keys;

import java.util.ArrayList;
import java.util.List;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TWindow;

/**
 * Coalesces repaint requests into one paint pass per animation frame, run on the EDT.
 * A thread other than the EDT that calls repaint() waits for that pass, so render loops such as
 * {@code while (true) panel.repaint();} run once per frame instead of freezing the tab.
 */
public final class TRepaint {
    private static boolean frameRequested;
    private static boolean passQueued;
    private static List<AsyncCallback<Void>> waiters = new ArrayList<>();

    private TRepaint() {
    }

    /** Called by Component.repaint(). */
    public static void repaint(TComponent c) {
        c.markDirty();
        Exit.check();
        if (!Edt.isEdt()) {
            waitForPaint();
        }
    }

    public static void requestFrame() {
        if (!frameRequested) {
            frameRequested = true;
            Js.requestAnimationFrame(TRepaint::onFrame);
            // Browsers pause animation frames for hidden or occluded windows; keep render threads moving anyway.
            Js.setTimeout(TRepaint::onFrame, 50);
        }
    }

    private static void onFrame() {
        if (!frameRequested) {
            return; // the other of the two triggers already ran
        }
        frameRequested = false;
        if (!passQueued) {
            passQueued = true;
            Edt.post(TRepaint::paintPass);
        }
    }

    private static void paintPass() {
        passQueued = false;
        List<AsyncCallback<Void>> done = waiters;
        waiters = new ArrayList<>();
        try {
            for (TWindow w : TDesktop.windows()) {
                if (w.isVisible()) {
                    if (!w.isValid()) {
                        w.validate();
                    }
                    paintTree(w);
                }
            }
        } finally {
            for (AsyncCallback<Void> cb : done) {
                Edt.resumeLater(cb);
            }
        }
    }

    private static void paintTree(TComponent c) {
        if (!c.isVisible()) {
            return;
        }
        if (c.takeDirty()) {
            try {
                c.paintNow();
            } catch (Exit.ExitError e) {
                throw e;
            } catch (Throwable e) {
                System.err.println("Exception while painting " + c.getClass().getName() + ": " + e);
                e.printStackTrace();
            }
        }
        if (c instanceof TContainer) {
            TContainer container = (TContainer) c;
            for (int i = 0; i < container.getComponentCount(); i++) {
                paintTree(container.getComponent(i));
            }
        }
    }

    @Async
    private static native Void waitForPaint();

    private static void waitForPaint(AsyncCallback<Void> callback) {
        waiters.add(callback);
        requestFrame();
    }
}
