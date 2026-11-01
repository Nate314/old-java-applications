package web.rt;

/**
 * App calls to {@code Thread.sleep(long)} are redirected here at compile time (see {@code web.teavm.ShimPlugin}).
 * TeaVM's Thread.sleep accepts negative values, but the JDK throws, and Minesweeper's sleep helper only
 * terminates because of that exception.
 */
public final class Sleep {
    private Sleep() {
    }

    public static void sleep(long millis) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        Exit.check();
        if (millis > 0) {
            Thread.sleep(millis);
        }
        Exit.check();
    }
}
