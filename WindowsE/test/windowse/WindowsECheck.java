package windowse;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * Self-check for the presses each shortcut sends, with a key list in place of java.awt.Robot (nothing reaches the
 * real keyboard): Win+E holds Windows once around every E, the Windows key alone is tapped, Stop lets go of Windows
 * early, and onDone runs once.
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
      String winE3 = list("+" + WIN, "+69", "-69", "+69", "-69", "+69", "-69", "-" + WIN);
      check(run(Shortcut.EXPLORER, 3, -1).equals(winE3), "Win+E three times holds Windows once: " + run(Shortcut.EXPLORER, 3, -1));
      check(run(Shortcut.START, 2, -1).equals(list("+" + WIN, "-" + WIN, "+" + WIN, "-" + WIN)), "the Windows key alone, twice");
      check(run(Shortcut.TASKS, 1, -1).equals(list("+" + WIN, "+9", "-9", "-" + WIN)), "Win+Tab");
      check(run(Shortcut.LEFT, 1, -1).equals(list("+" + WIN, "+37", "-37", "-" + WIN)), "Win+Left");
      String stopped = run(Shortcut.EXPLORER, 10, 2);
      check(stopped.equals(list("+" + WIN, "+69", "-69", "+69", "-69", "-" + WIN)), "Stop after two lets go of Windows: " + stopped);
      for (Shortcut s : Shortcut.values()) {
         check(s.times >= 1 && !s.title.isEmpty() && !s.does.isEmpty() && s.combo().startsWith("Win"), s + " is described");
      }
      check(Shortcut.EXPLORER.times == 10, "Win+E still offers 10, as the original did");
      System.out.println("WindowsECheck: all checks passed");
      System.exit(0);
   }

   /** Presses shortcut count times (stopping after stopAfter presses, unless -1) and returns the log. */
   private static String run(Shortcut shortcut, int count, int stopAfter) throws Exception {
      Recorder keys = new Recorder();
      CountDownLatch done = new CountDownLatch(1);
      int[] doneCalls = {0};
      WindowsEPresser[] presser = new WindowsEPresser[1];
      SwingUtilities.invokeAndWait(() -> {
         presser[0] = new WindowsEPresser(keys, shortcut, count, n -> {
            if (n == stopAfter) {
               SwingUtilities.invokeLater(() -> presser[0].stop());
            }
         }, () -> {
            doneCalls[0]++;
            done.countDown();
         });
         presser[0].start();
      });
      check(done.await(5, TimeUnit.SECONDS), shortcut + " finishes");
      Thread.sleep(250);   // a stray timer tick would show up here
      check(doneCalls[0] == 1, shortcut + " calls onDone once");
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
