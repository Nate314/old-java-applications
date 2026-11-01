package web.shim.java.awt;

import web.rt.Edt;

public class TEventQueue {
    public void postEvent(TAWTEvent event) {
        Object source = event.getSource();
        if (source instanceof TComponent) {
            Edt.post(() -> ((TComponent) source).dispatchEvent(event));
        }
    }

    public static void invokeLater(Runnable runnable) {
        Edt.post(runnable);
    }

    public static boolean isDispatchThread() {
        return Edt.isEdt();
    }
}
