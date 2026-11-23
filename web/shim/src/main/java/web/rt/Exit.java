package web.rt;

/** App calls to {@code System.exit(int)} are redirected here at compile time (see {@code web.teavm.ShimPlugin}). */
public final class Exit {
    private static boolean exited;
    private static boolean closing;
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
        if (!closing) {
            closing = true;
            System.out.println("System.exit(" + status + ")");
            // Windows close before exited is set: a window's dispose may still repaint (SynthesizerKeyboard's does),
            // and the page hears of the exit even if one throws.
            try {
                if (onExit != null) {
                    onExit.run();
                }
            } finally {
                exited = true;
                Js.notifyExited("The program exited (status " + status + ").");
            }
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
