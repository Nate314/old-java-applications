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
      typingChecks();
      screenChecks();
      boardChecks();
      sendChecks();
      pillChecks();
      System.out.println("WindowsECheck: all checks passed");
      System.exit(0);
   }

   /** Both keyboards place every shortcut's key once, inside the board and clear of the others; the pocket pad fits a phone. */
   static void boardChecks() {
      for (boolean pocket : new boolean[] {false, true}) {
         javax.swing.JTextField field = new javax.swing.JTextField();
         ShortcutBoard board = new ShortcutBoard(s -> { }, s -> { }, i -> { }, pocket, field);
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

   /** The real keyboard: E, D and R pick their key, other keys none; the count keeps digits; E then 5 is Win+E x5. */
   static void typingChecks() throws Exception {
      check(Shortcut.forLetter(KeyEvent.VK_E) == Shortcut.EXPLORER && Shortcut.forLetter(KeyEvent.VK_D) == Shortcut.DESKTOP
         && Shortcut.forLetter(KeyEvent.VK_R) == Shortcut.RUN, "E, D and R pick their keys");
      for (int code : new int[] {KeyEvent.VK_Q, KeyEvent.VK_TAB, KeyEvent.VK_LEFT, KeyEvent.VK_UP, 0}) {
         check(Shortcut.forLetter(code) == null, "key " + code + " picks nothing");
      }
      check(WindowsEWindow.countAfter("10e", -1).equals("10") && WindowsEWindow.countAfter("105", 5).equals("5")
         && WindowsEWindow.countAfter("1x2", -1).equals("12"), "the count keeps its digits, or the digit replacing it");
      if (java.awt.GraphicsEnvironment.isHeadless()) {
         return;
      }
      Recorder keys = new Recorder();
      String[] got = new String[3];
      SwingUtilities.invokeAndWait(() -> {
         WindowsEWindow w = new WindowsEWindow(keys, t -> { }, null, null, false);
         javax.swing.JTextField field = field(w);
         type(field, KeyEvent.VK_D, field.getText() + "d");   // as the field would have it after the letter
         got[0] = field.getText();
         type(field, KeyEvent.VK_5, field.getText() + "5");
         type(field, KeyEvent.VK_2, field.getText() + "2");
         got[1] = field.getText();
         type(field, KeyEvent.VK_E, field.getText() + "e");
         type(field, KeyEvent.VK_BACK_SPACE, "1");
         type(field, KeyEvent.VK_3, "13");
         got[2] = field.getText();
         w.dispose();
      });
      check("1".equals(got[0]), "D picks Win+D and its count, the letter left out: " + got[0]);
      check("52".equals(got[1]), "5 then 2 replace the offered count: " + got[1]);
      check("13".equals(got[2]), "after Backspace, digits go on the count: " + got[2]);
      check(keys.log.isEmpty(), "typing presses nothing");
   }

   /** A key let go in the field, once the field's text is text. */
   private static void type(javax.swing.JTextField field, int code, String text) {
      field.setText(text);
      KeyEvent e = new KeyEvent(field, KeyEvent.KEY_RELEASED, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
      for (java.awt.event.KeyListener l : field.getKeyListeners()) {   // straight to the listeners: the window is not shown
         l.keyReleased(e);
      }
   }

   private static javax.swing.JTextField field(java.awt.Container in) {
      for (java.awt.Component c : in.getComponents()) {
         javax.swing.JTextField f = c instanceof javax.swing.JTextField ? (javax.swing.JTextField) c
            : c instanceof java.awt.Container ? field((java.awt.Container) c) : null;
         if (f != null) {
            return f;
         }
      }
      return null;
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

   /** Each step is a pill: a click on its x (or Delete) takes that step off, a click elsewhere on it only shows it. */
   static void pillChecks() throws Exception {
      if (java.awt.GraphicsEnvironment.isHeadless()) {
         System.out.println("WindowsECheck: headless, the step pill checks skipped");
         return;
      }
      for (boolean pocket : new boolean[] {false, true}) {
         Recorder keys = new Recorder();
         List<String> copied = new ArrayList<>();
         String[] got = new String[4];
         SwingUtilities.invokeAndWait(() -> {
            WindowsEWindow w = new WindowsEWindow(keys, copied::add, null, Step.parse("Ex3-Left-E-Right"), pocket);
            List<StepPill> pills = pills(w);
            got[0] = String.valueOf(pills.size());
            click(pills.get(0), 4);   // on its label: shown, not taken off
            button(w, "Send").doClick();
            StepPill second = pills.get(1);
            click(second, second.getWidth() - 6);   // on its x
            button(w, "Send").doClick();
            StepPill last = pills(w).get(2);
            for (java.awt.event.KeyListener l : last.getKeyListeners()) {
               l.keyPressed(new KeyEvent(last, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED));
            }
            button(w, "Send").doClick();
            got[1] = String.valueOf(pills(w).size());
            w.dispose();
         });
         String where = " (pocket " + pocket + ")";
         check("4".equals(got[0]), "four steps, four pills" + where + ": " + got[0]);
         check(copied.equals(Arrays.asList("Ex3-Left-E-Right", "Ex3-E-Right", "Ex3-E")), "a click on a label keeps the step, its x and Delete take it off" + where + ": " + copied);
         check("2".equals(got[1]), "two pills left" + where + ": " + got[1]);
         check(keys.log.isEmpty(), "editing the list presses nothing" + where);
      }
   }

   private static List<StepPill> pills(java.awt.Container in) {
      List<StepPill> found = new ArrayList<>();
      for (java.awt.Component c : in.getComponents()) {
         if (c instanceof StepPill) {
            found.add((StepPill) c);
         } else if (c instanceof java.awt.Container) {
            found.addAll(pills((java.awt.Container) c));
         }
      }
      return found;
   }

   /** A click at x on the pill, as the mouse makes it: pressed there, then the button's action. */
   private static void click(StepPill pill, int x) {
      java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(pill, java.awt.event.MouseEvent.MOUSE_PRESSED, 0, 0, x, 8, 1, false);
      for (java.awt.event.MouseListener l : pill.getMouseListeners()) {
         l.mousePressed(e);
      }
      pill.doClick();
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
