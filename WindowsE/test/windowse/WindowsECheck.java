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
      boardChecks();
      sendChecks();
      System.out.println("WindowsECheck: all checks passed");
      System.exit(0);
   }

   /** Both keyboards place every shortcut's key once, inside the board and clear of the others; the pocket pad fits a phone. */
   static void boardChecks() {
      for (boolean pocket : new boolean[] {false, true}) {
         javax.swing.JTextField field = new javax.swing.JTextField();
         ShortcutBoard board = new ShortcutBoard(s -> { }, s -> { }, pocket, field);
         java.awt.Dimension size = board.getPreferredSize();
         java.util.Set<Shortcut> seen = java.util.EnumSet.noneOf(Shortcut.class);
         List<java.awt.Rectangle> placed = new ArrayList<>();
         for (java.awt.Component c : board.getComponents()) {
            if (c == field) {
               continue;
            }
            KeyCap cap = (KeyCap) c;
            java.awt.Rectangle r = cap.getBounds();
            check(seen.add(cap.shortcut), cap.shortcut + " has one key (pocket " + pocket + ")");
            check(r.x >= 0 && r.y >= 0 && r.x + r.width <= size.width && r.y + r.height <= size.height, cap.shortcut + " is on the board");
            for (java.awt.Rectangle o : placed) {
               check(!r.intersects(o), cap.shortcut + " clears the other keys");
            }
            placed.add(r);
         }
         check(seen.size() == Shortcut.values().length, "every shortcut has a key (pocket " + pocket + ")");
         for (Shortcut s : Shortcut.values()) {   // the count field sits after the picked key's chips, above the keyboard
            board.pick(s);
            java.awt.Rectangle r = field.getBounds();
            check(r.x >= 60 && r.y >= 0 && r.x + r.width <= size.width - 150 && placed.stream().noneMatch(r::intersects),
               "the count field is in the head for " + s + " (pocket " + pocket + "): " + r);
         }
         check(pocket ? size.width <= 400 : size.width > 800, "board width " + size.width + " (pocket " + pocket + ")");
      }
   }

   /** Send's codes read back as the same list, a bad code is no list, and a friend's list is loaded without a press. */
   static void sendChecks() throws Exception {
      String sides = Step.code(WindowsEWindow.SIDE_BY_SIDE);
      check(sides.equals("E-Left-E-Right"), "Side by side's code: " + sides);
      List<Step> mixed = steps(Shortcut.START, 1, Shortcut.EXPLORER, 40, Shortcut.TASKS, 1, Shortcut.DESKTOP, 2, Shortcut.RUN, 1,
         Shortcut.UP, 1, Shortcut.DOWN, 3);
      String code = Step.code(mixed);
      check(code.equals("Win-Ex40-Tab-Dx2-R-Up-Downx3"), "a mixed list's code: " + code);
      check(WindowsEWindow.labels(Step.parse(code)).equals(WindowsEWindow.labels(mixed)), "the code reads back as the list");
      check(WindowsEWindow.labels(Step.parse("ex10-LEFT-right")).equals("Win+E x10, Win+Left, Win+Right"), "codes in any case");
      for (String bad : new String[] {"", "Q", "E-", "Ex0", "Ex1000", "Exy", "E-E-E-E-E-E-E-E-E", "Win+E"}) {
         check(Step.parse(bad) == null, "'" + bad + "' is no list");
      }
      check(Step.parse("Ex999") != null && Step.parse("E-E-E-E-E-E-E-E") != null, "999 times and 8 steps are allowed");
      if (java.awt.GraphicsEnvironment.isHeadless()) {
         System.out.println("WindowsECheck: headless, the window's Send checks skipped");
         return;
      }
      for (boolean pocket : new boolean[] {false, true}) {
         for (String page : new String[] {null, "https://example.test/WindowsE/"}) {
            Recorder keys = new Recorder();
            List<String> copied = new ArrayList<>();
            String[] got = new String[3];
            SwingUtilities.invokeAndWait(() -> {
               WindowsEWindow w = new WindowsEWindow(keys, copied::add, page, Step.parse("Ex3-Left"), pocket);
               got[0] = w.getTitle();
               button(w, "Send").doClick();
               got[1] = w.getTitle();
               got[2] = String.valueOf(w.getWidth());
               w.dispose();
            });
            String where = " (pocket " + pocket + ", page " + page + ")";
            check(got[0].contains("a friend sent this list"), "a friend's list is announced" + where + ": " + got[0]);
            check(keys.log.isEmpty(), "a friend's list presses nothing" + where);
            check(copied.equals(Collections.singletonList(page == null ? "Ex3-Left" : page + "?keys=Ex3-Left")), "Send copies " + copied + where);
            check(got[1].equals(page == null ? "Windows+E: code Ex3-Left copied" : "Windows+E: link copied, send it to a friend"), "Send says so: " + got[1]);
            check(pocket ? Integer.parseInt(got[2]) <= 400 : true, "the window is " + got[2] + " wide" + where);
            System.out.println("WindowsECheck: window " + got[2] + " px wide" + where);
         }
      }
   }

   private static javax.swing.JButton button(java.awt.Container in, String text) {
      for (java.awt.Component c : in.getComponents()) {
         if (c instanceof javax.swing.JButton && text.equals(((javax.swing.JButton) c).getText())) {
            return (javax.swing.JButton) c;
         }
         javax.swing.JButton b = c instanceof java.awt.Container ? button((java.awt.Container) c, text) : null;
         if (b != null) {
            return b;
         }
      }
      return null;
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
