package web.shim.javax.swing;

import web.rt.Edt;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TWindow;

public class TSwingUtilities {
    private TSwingUtilities() {
    }

    public static void invokeLater(Runnable doRun) {
        Edt.post(doRun);
    }

    public static boolean isEventDispatchThread() {
        return Edt.isEdt();
    }

    public static TWindow getWindowAncestor(TComponent c) {
        return c == null ? null : c.windowAncestor();
    }
}
