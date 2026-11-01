package web.rt;

import java.util.ArrayDeque;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;

/**
 * The event dispatch thread. DOM callbacks run outside any TeaVM thread and cannot block, so they only queue work
 * here; the EDT (a TeaVM green thread) runs listeners and paint passes one at a time, like Swing's EDT, so app code
 * there may call Thread.sleep.
 */
public final class Edt {
    private static final ArrayDeque<Runnable> queue = new ArrayDeque<>();
    private static AsyncCallback<Void> idle;
    private static Thread thread;

    private Edt() {
    }

    public static void post(Runnable task) {
        start();
        queue.add(task);
        if (idle != null) {
            AsyncCallback<Void> wake = idle;
            idle = null;
            resumeLater(wake);
        }
    }

    /**
     * Resumes a suspended thread from a fresh JS task. Completing the callback directly from inside another
     * running TeaVM thread fails with "Another thread is running".
     */
    public static void resumeLater(AsyncCallback<Void> callback) {
        Js.setTimeout(() -> callback.complete(null), 0);
    }

    public static boolean isEdt() {
        return thread != null && Thread.currentThread() == thread;
    }

    private static void start() {
        if (thread == null) {
            thread = new Thread(Edt::loop, "AWT-EventQueue-0");
            thread.start();
        }
    }

    private static void loop() {
        while (true) {
            Runnable task = queue.poll();
            if (task == null) {
                waitForWork();
                continue;
            }
            try {
                task.run();
            } catch (Exit.ExitError e) {
                // System.exit unwinds the current listener; the app's windows are already gone.
            } catch (Throwable e) {
                // Swing prints uncaught listener exceptions and keeps dispatching.
                System.err.println("Exception in thread \"AWT-EventQueue-0\" " + e);
                e.printStackTrace();
            }
        }
    }

    @Async
    private static native Void waitForWork();

    private static void waitForWork(AsyncCallback<Void> callback) {
        idle = callback;
    }
}
