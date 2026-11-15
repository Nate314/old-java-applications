package windowse;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * Self-check for the presses each shortcut sends, with a key list in place of java.awt.Robot (nothing reaches the
 * real keyboard): every press holds Windows only around its own key (so keys typed between presses are not Win
 * shortcuts), the Windows key alone is tapped, a list of steps is pressed in order, Stop ends early, and onDone runs
 * once. Also what the little screen says each list does.
 */
public class WindowsECheck {
   /** Writes each press as "+code" and each release as "-code". */
   static final class Recorder implements Keys {
      final List<String> log = new ArrayList<>();

      @Override
      public void press(int keyCode) {
         this.log.add("+" + keyCode);
      }

      @Override
      public void release(int keyCode) {
         this.log.add("-" + keyCode);
      }
   }

   private static final int WIN = KeyEvent.VK_WINDOWS;

   public static void main(String[] args) throws Exception {
      String winE = list("+" + WIN, "+69", "-69", "-" + WIN);
      String winE3 = list(winE, winE, winE);
      check(run(Shortcut.EXPLORER, 3, -1).equals(winE3), "Win+E three times lets go of Windows after each: " + run(Shortcut.EXPLORER, 3, -1));
      check(run(Shortcut.START, 2, -1).equals(list("+" + WIN, "-" + WIN, "+" + WIN, "-" + WIN)), "the Windows key alone, twice");
      check(run(Shortcut.TASKS, 1, -1).equals(list("+" + WIN, "+9", "-9", "-" + WIN)), "Win+Tab");
      check(run(Shortcut.LEFT, 1, -1).equals(list("+" + WIN, "+37", "-37", "-" + WIN)), "Win+Left");
      String stopped = run(Shortcut.EXPLORER, 10, 2);
      check(stopped.equals(list(winE, winE)), "Stop after two: " + stopped);
      String sides = run(WindowsEWindow.SIDE_BY_SIDE, -1);
      check(sides.equals(list(winE, "+" + WIN, "+37", "-37", "-" + WIN, winE, "+" + WIN, "+39", "-39", "-" + WIN)),
         "Side by side presses Win+E, Win+Left, Win+E, Win+Right in order: " + sides);
      List<Step> mixed = Arrays.asList(new Step(Shortcut.START, 1), new Step(Shortcut.EXPLORER, 2));
      check(run(mixed, -1).equals(list("+" + WIN, "-" + WIN, winE, winE)), "a list of steps, each its times");
      check(WindowsEWindow.labels(mixed).equals("Win, Win+E x2"), "the list is written out: " + WindowsEWindow.labels(mixed));
      for (Shortcut s : Shortcut.values()) {
         check(s.times >= 1 && !s.title.isEmpty() && !s.does.isEmpty() && s.combo().startsWith("Win"), s + " is described");
      }
      check(Shortcut.EXPLORER.times == 10, "Win+E still offers 10, as the original did");
      screenChecks();
      System.out.println("WindowsECheck: all checks passed");
      System.exit(0);
   }

   /** The little screen: what each shortcut does to it, and the words for it. */
   private static void screenChecks() {
      Screen sides = Screen.after(WindowsEWindow.SIDE_BY_SIDE);
      check(sides.summary().equals("2 File Explorer windows: 1 on the left half, 1 on the right half."), "Side by side: " + sides.summary());
      check(says(steps(Shortcut.EXPLORER, 10), "10 File Explorer windows."), "Win+E x10");
      check(says(steps(Shortcut.EXPLORER, 1000), "40 File Explorer windows."), "Win+E stops at 40 windows, as the pretend Windows does");
      check(says(steps(Shortcut.EXPLORER, 2, Shortcut.DESKTOP, 1), "2 File Explorer windows: 2 hidden by Win+D."), "Win+D hides");
      check(says(steps(Shortcut.EXPLORER, 2, Shortcut.DESKTOP, 2), "2 File Explorer windows."), "Win+D again brings them back");
      check(says(steps(Shortcut.EXPLORER, 1, Shortcut.LEFT, 1, Shortcut.RIGHT, 1), "1 File Explorer window."), "Right from the left half puts it back");
      check(says(steps(Shortcut.EXPLORER, 1, Shortcut.UP, 1, Shortcut.DOWN, 2), "1 File Explorer window: 1 minimized."), "Down restores, then minimizes");
      check(says(steps(Shortcut.EXPLORER, 2, Shortcut.DOWN, 1, Shortcut.LEFT, 1), "2 File Explorer windows: 1 on the left half, 1 minimized."),
         "the arrows move the window in front, the next one once the first is minimized");
      check(says(steps(Shortcut.LEFT, 1), "Nothing open. The arrows found no File Explorer window to move."), "an arrow with no window");
      check(says(steps(Shortcut.START, 1, Shortcut.RUN, 1), "The Start menu open; the Run box open."), "Start and Run");
      check(says(steps(Shortcut.START, 2), "Nothing open."), "the Windows key twice closes the Start menu");
      check(says(steps(Shortcut.START, 1, Shortcut.DESKTOP, 1), "Nothing open."), "Win+D closes the Start menu");
      check(says(steps(Shortcut.TASKS, 1000001), "Task view open."), "a huge count keeps a toggle's parity");
      check(WindowsEWindow.parseCount(" 7 ") == 7 && WindowsEWindow.parseCount("-3") == 0 && WindowsEWindow.parseCount("x") == -1, "counts");
   }

   /** Steps from pairs: shortcut, times, shortcut, times... */
   private static List<Step> steps(Object... pairs) {
      List<Step> list = new ArrayList<>();
      for (int i = 0; i < pairs.length; i += 2) {
         list.add(new Step((Shortcut) pairs[i], (Integer) pairs[i + 1]));
      }
      return list;
   }

   private static boolean says(List<Step> steps, String summary) {
      String got = Screen.after(steps).summary();
      if (!got.equals(summary)) {
         System.out.println(WindowsEWindow.labels(steps) + " says: " + got);
      }
      return got.equals(summary);
   }

   /** Presses shortcut count times (stopping after stopAfter presses, unless -1) and returns the log. */
   private static String run(Shortcut shortcut, int count, int stopAfter) throws Exception {
      return run(Collections.singletonList(new Step(shortcut, count)), stopAfter);
   }

   /** Presses the steps (stopping after stopAfter presses, unless -1) and returns the log. */
   private static String run(List<Step> steps, int stopAfter) throws Exception {
      String what = WindowsEWindow.labels(steps);
      Recorder keys = new Recorder();
      CountDownLatch done = new CountDownLatch(1);
      int[] doneCalls = {0};
      WindowsEPresser[] presser = new WindowsEPresser[1];
      SwingUtilities.invokeAndWait(() -> {
         presser[0] = new WindowsEPresser(keys, steps, n -> {
            if (n == stopAfter) {
               SwingUtilities.invokeLater(() -> presser[0].stop());
            }
         }, () -> {
            doneCalls[0]++;
            done.countDown();
         });
         presser[0].start();
      });
      check(done.await(5, TimeUnit.SECONDS), what + " finishes");
      Thread.sleep(250);   // a stray timer tick would show up here
      check(doneCalls[0] == 1, what + " calls onDone once");
      String[] log = new String[1];
      SwingUtilities.invokeAndWait(() -> log[0] = String.join(" ", keys.log));
      return log[0];
   }

   private static String list(String... keys) {
      return String.join(" ", keys);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError("failed: " + what);
      }
   }
}
