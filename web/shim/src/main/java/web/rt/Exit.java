package web.rt;

/** App calls to {@code System.exit(int)} are redirected here at compile time (see {@code web.teavm.ShimPlugin}). */
public final class Exit {
    private static boolean exited;
    /** Closes every window; set by the desktop (this class cannot name shim types, see web/README.md). */
    public static Runnable onExit;

    private Exit() {
    }

    /** Thrown to unwind the calling thread, since System.exit never returns. */
    public static final class ExitError extends Error {
        ExitError() {
            super("System.exit");
        }
    }

    public static void exit(int status) {
        if (!exited) {
            exited = true;
            System.out.println("System.exit(" + status + ")");
            if (onExit != null) {
                onExit.run();
            }
            Js.notifyExited("The program exited (status " + status + ").");
        }
        throw new ExitError();
    }

    public static boolean isExited() {
        return exited;
    }

    /** Called from blocking points so background threads stop after exit instead of spinning forever. */
    public static void check() {
        if (exited) {
            throw new ExitError();
        }
    }
}
