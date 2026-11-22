package test;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Self-check with a key list in place of java.awt.Robot (nothing reaches the real keyboard): the key goes down with
 * the mouse button and up with it, a repeated press or release sends nothing more, Space taps it, the right button
 * does nothing, a slide off the key makes Win+that arrow (and a slide back onto it does not), and the window stays on top without taking the keyboard. Run it headless or not; no window is shown.
 */
public class WindowsKeyCheck {
   public static void main(String[] args) throws Exception {
      List<String> log = new ArrayList<>();
      WindowsKey.Keys keys = new WindowsKey.Keys() {
         @Override
         public void press(int keyCode) {
            log.add("+" + keyCode);
         }

         @Override
         public void release(int keyCode) {
            log.add("-" + keyCode);
         }
      };
      int win = KeyEvent.VK_WINDOWS;
      SwingUtilities.invokeAndWait(() -> {
         WindowsKey key = new WindowsKey(keys);
         key.setSize(100, 106);
         mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
         check(log.equals(List.of("+" + win)) && key.isDown(), "the mouse button down holds Windows: " + log);
         mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
         check(log.size() == 1, "a second press while held sends nothing: " + log);
         paint(key);
         mouse(key, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
         check(log.equals(List.of("+" + win, "-" + win)) && !key.isDown(), "the button up lets go: " + log);
         mouse(key, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
         mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3);
         check(log.size() == 2, "a stray release and the right button send nothing: " + log);
         log.clear();
         // Straight to the listeners: Swing's focus manager would hand the key to the focus owner, and there is none.
         for (java.awt.event.KeyListener l : key.getKeyListeners()) {
            l.keyReleased(new KeyEvent(key, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_SPACE, ' '));
         }
         check(log.equals(List.of("+" + win, "-" + win)), "Space taps it: " + log);
         paint(key);
         log.clear();
         int right = KeyEvent.VK_RIGHT;
         mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
         drag(key, 50 + WindowsKey.SLIDE - 1, 50);
         check(key.slide() == 0, "a short slide picks no arrow");
         drag(key, 50 + WindowsKey.SLIDE + 30, 40);
         check(key.slide() == right, "a slide to the right picks the right arrow");
         paint(key);
         if (System.getProperty("keyshot") != null) {
            // Held and slid right: the four arrows round the logo, the right one lit, "Snap right" under it.
            try {
               BufferedImage held = new BufferedImage(key.getWidth(), key.getHeight(), BufferedImage.TYPE_INT_ARGB);
               key.paint(held.getGraphics());
               javax.imageio.ImageIO.write(held, "png", new File(System.getProperty("keyshot")));
            } catch (java.io.IOException e) {
               throw new RuntimeException(e);
            }
         }
         key.dispatchEvent(new MouseEvent(key, MouseEvent.MOUSE_RELEASED, 0, 0, 50 + WindowsKey.SLIDE + 30, 40, 1, false, MouseEvent.BUTTON1));
         check(log.equals(List.of("+" + win, "+" + right, "-" + right, "-" + win)) && key.slide() == 0,
            "letting go after the slide makes Win+Right: " + log);
         log.clear();
         mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
         drag(key, 45, 50 - WindowsKey.SLIDE - 5);
         check(key.slide() == KeyEvent.VK_UP, "a slide up picks the up arrow");
         drag(key, 52, 48);
         mouse(key, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
         check(log.equals(List.of("+" + win, "-" + win)), "sliding back onto the key before letting go is a plain click: " + log);
         log.clear();
         ShortcutShelf shelf = new ShortcutShelf(keys);
         shelf.setSize(Test.WIDTH, ShortcutShelf.HEIGHT);
         for (int i = 0; i < ShortcutShelf.count(); i++) {
            java.awt.Rectangle r = shelf.rect(i);
            check(r.x >= 0 && r.y >= 0 && r.x + r.width <= Test.WIDTH && r.y + r.height <= ShortcutShelf.HEIGHT
               && shelf.at(r.x + r.width / 2, r.y + r.height / 2) == i, "shortcut " + i + " is inside the shelf and found there");
            log.clear();
            click(shelf, r.x + r.width / 2, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height / 2);
            int code = ShortcutShelf.code(i);
            check(log.equals(List.of("+" + win, "+" + code, "-" + code, "-" + win)), "a click on shortcut " + i + " makes Win+" + code + ": " + log);
         }
         check(ShortcutShelf.code(0) == KeyEvent.VK_E && ShortcutShelf.code(4) == KeyEvent.VK_LEFT, "the shelf starts with E and the arrows follow");
         java.awt.Rectangle leftCap = shelf.rect(4);
         shelf.dispatchEvent(new MouseEvent(shelf, MouseEvent.MOUSE_MOVED, 0, 0, leftCap.x + 3, leftCap.y + 3, 0, false, MouseEvent.NOBUTTON));
         check("Win+←: Snap left".equals(shelf.getToolTipText()), "pointing at the left arrow names it: " + shelf.getToolTipText());
         shelf.dispatchEvent(new MouseEvent(shelf, MouseEvent.MOUSE_EXITED, 0, 0, -1, -1, 0, false, MouseEvent.NOBUTTON));
         check(shelf.getToolTipText().startsWith("Click a shortcut"), "and off the shelf, the shelf's own tip again");
         log.clear();
         java.awt.Rectangle first = shelf.rect(0);
         java.awt.Rectangle second = shelf.rect(1);
         click(shelf, first.x + 5, first.y + 5, second.x + 5, second.y + 5);
         check(log.isEmpty(), "letting go on another shortcut presses none: " + log);
         for (java.awt.event.KeyListener l : shelf.getKeyListeners()) {
            l.keyPressed(new KeyEvent(shelf, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
            l.keyReleased(new KeyEvent(shelf, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_ENTER, (char) 10));
         }
         check(shelf.picked() == 1 && log.equals(List.of("+" + win, "+" + KeyEvent.VK_D, "-" + KeyEvent.VK_D, "-" + win)),
            "Down then Enter presses Win+D: " + log);
         for (java.awt.event.KeyListener l : shelf.getKeyListeners()) {
            l.keyPressed(new KeyEvent(shelf, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
            l.keyPressed(new KeyEvent(shelf, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
         }
         check(shelf.picked() == ShortcutShelf.HOVER_ROW, "Up from the first wraps round to the last, hover to press");
         log.clear();
         java.awt.Rectangle hoverRow = shelf.rect(ShortcutShelf.HOVER_ROW);
         check(hoverRow.y > shelf.rect(ShortcutShelf.count() - 1).y && hoverRow.y + hoverRow.height <= ShortcutShelf.HEIGHT
            && shelf.at(hoverRow.x + 5, hoverRow.y + 5) == ShortcutShelf.HOVER_ROW, "the hover to press row is below the arrows, inside the shelf");
         // Hover to press, timed by hand: a time far ahead, so the real timer never reaches it.
         long t = System.currentTimeMillis() + 1_000_000_000L;
         boolean[] on = {false};
         int[] toggles = {0};
         ShortcutShelf hovering = new ShortcutShelf(keys, () -> on[0], () -> {
            on[0] = !on[0];
            toggles[0]++;
         });
         hovering.setSize(Test.WIDTH, ShortcutShelf.HEIGHT);
         Dwell d = hovering.dwell();
         d.at(0, t);
         d.tick(t + 5000);
         check(log.isEmpty() && d.progress(0, t + 500) == 0, "with hover to press off, resting on a shortcut presses nothing: " + log);
         click(hovering, hoverRow.x + 5, hoverRow.y + 5, hoverRow.x + 5, hoverRow.y + 5);
         check(on[0] && toggles[0] == 1 && log.isEmpty(), "a click on the hover row turns it on and presses no key: " + log);
         check(hovering.getAccessibleContext().getAccessibleName().contains("Win+E"), "the shelf still names its picked shortcut");
         d.at(-1, t);
         d.at(1, t);
         d.tick(t + Dwell.MS - 1);
         check(log.isEmpty() && Math.abs(d.progress(1, t + Dwell.MS / 2) - 0.5) < 1e-9 && d.progress(0, t + 500) == 0,
            "just short of a second, nothing yet, half way at half a second: " + log);
         d.tick(t + Dwell.MS);
         check(log.equals(List.of("+" + win, "+" + KeyEvent.VK_D, "-" + KeyEvent.VK_D, "-" + win)), "a second on Desktop presses Win+D: " + log);
         d.tick(t + 3 * Dwell.MS);
         check(log.size() == 4 && d.progress(1, t + 3 * Dwell.MS) == 0, "and only once while the pointer stays: " + log);
         paint(hovering);
         d.at(-1, t + 3 * Dwell.MS);
         d.at(1, t + 4 * Dwell.MS);
         d.tick(t + 5 * Dwell.MS);
         check(log.size() == 8, "leaving and coming back presses it again: " + log);
         log.clear();
         d.at(2, t);
         d.clicked();
         d.tick(t + 2 * Dwell.MS);
         check(log.isEmpty(), "a click while waiting calls the wait off: " + log);
         d.at(ShortcutShelf.HOVER_ROW, t);
         d.tick(t + Dwell.MS);
         check(!on[0] && toggles[0] == 2 && log.isEmpty(), "resting on the hover row turns it off again, hands free: " + log);
         d.at(-1, t);
         on[0] = true;
         WindowsKey resting = new WindowsKey(keys, () -> on[0]);
         resting.setSize(100, 106);
         resting.dwell().at(0, t);
         resting.dwell().tick(t + Dwell.MS / 3);
         paint(resting);
         resting.dwell().tick(t + Dwell.MS);
         check(log.equals(List.of("+" + win, "-" + win)) && !resting.isDown(), "resting on the key a second taps it, for Start: " + log);
         resting.dwell().at(-1, t);
         log.clear();
         resting.dwell().at(0, t);
         mouse(resting, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
         mouse(resting, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
         resting.dwell().tick(t + 2 * Dwell.MS);
         check(log.equals(List.of("+" + win, "-" + win)), "a click is one press, the hover not a second: " + log);
         resting.dwell().at(-1, t);
         log.clear();
         BufferedImage image = new BufferedImage(Test.WIDTH, ShortcutShelf.HEIGHT, BufferedImage.TYPE_INT_ARGB);
         shelf.paint(image.getGraphics());
         // The large key's shelf: the same shortcuts, twice as big, found where they are drawn.
         ShortcutShelf big = new ShortcutShelf(keys);
         big.setSize(2 * Test.WIDTH, 2 * ShortcutShelf.HEIGHT);
         for (int i = 0; i < ShortcutShelf.count(); i++) {
            java.awt.Rectangle r = big.rect(i);
            log.clear();
            click(big, 2 * r.x + r.width, 2 * r.y + r.height, 2 * r.x + r.width, 2 * r.y + r.height);
            int code = ShortcutShelf.code(i);
            check(log.equals(List.of("+" + win, "+" + code, "-" + code, "-" + win)), "on the large shelf, shortcut " + i + " makes Win+" + code + ": " + log);
         }
         image = new BufferedImage(big.getWidth(), big.getHeight(), BufferedImage.TYPE_INT_ARGB);
         big.paint(image.getGraphics());
         key.setSize(2 * Test.WIDTH, 2 * (Test.HEIGHT - 24));
         paint(key);
      });
      // The shelf and the key as one: a shortcut shows its chord on the key for a moment, and the key held lights the shelf.
      WindowsKey[] paired = new WindowsKey[1];
      SwingUtilities.invokeAndWait(() -> {
         log.clear();
         WindowsKey key = new WindowsKey(keys);
         key.setSize(100, 106);
         ShortcutShelf shelf = new ShortcutShelf(keys, () -> false, () -> { }, key);
         shelf.setSize(Test.WIDTH, ShortcutShelf.HEIGHT);
         check(key.echoed() == null && !shelf.keyHeld(), "nothing echoed or held at first");
         java.awt.Rectangle r = shelf.rect(0);
         click(shelf, r.x + 5, r.y + 5, r.x + 5, r.y + 5);
         check("Win+E".equals(key.echoed()) && !key.isDown() && log.equals(List.of("+" + win, "+" + KeyEvent.VK_E, "-" + KeyEvent.VK_E, "-" + win)),
            "a click on E makes Win+E once and the key shows Win+E without pressing Windows again: " + key.echoed() + " " + log);
         paint(key);
         r = shelf.rect(4);
         click(shelf, r.x + 5, r.y + 5, r.x + 5, r.y + 5);
         check("Win+←".equals(key.echoed()), "an arrow echoes as Win+←: " + key.echoed());
         mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
         check(shelf.keyHeld(), "the key held: the shelf knows, to light its keys");
         shelf.paint(new BufferedImage(Test.WIDTH, ShortcutShelf.HEIGHT, BufferedImage.TYPE_INT_ARGB).getGraphics());
         mouse(key, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
         check(!shelf.keyHeld(), "let go: the shelf's keys go back");
         if (System.getProperty("litshot") != null) {
            // The shelf lit under a held key, above the key, as on the screen.
            mouse(key, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            BufferedImage lit = new BufferedImage(Test.WIDTH, ShortcutShelf.HEIGHT + 106, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics g = lit.getGraphics();
            shelf.paint(g);
            g.translate(8, ShortcutShelf.HEIGHT);
            key.paint(g);
            mouse(key, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
            try {
               javax.imageio.ImageIO.write(lit, "png", new File(System.getProperty("litshot")));
            } catch (java.io.IOException e) {
               throw new RuntimeException(e);
            }
         }
         paired[0] = key;
      });
      Thread.sleep(WindowsKey.ECHO_MS + 400);
      SwingUtilities.invokeAndWait(() -> check(paired[0].echoed() == null, "the echo goes after a moment"));
      log.clear();
      // Where the key was left: kept in a file of its own, damaged or missing lines falling back to the defaults.
      File file = File.createTempFile("windows-key", ".txt");
      file.deleteOnExit();
      Placement fresh = Placement.load(new File(file.getPath() + ".missing"));
      check(fresh.x == -1 && fresh.bottom == -1 && fresh.size == 0 && fresh.shelf, "no file: the defaults, the shelf open for a first look");
      Placement left = new Placement();
      left.x = 300;
      left.bottom = 500;
      left.size = 2;
      left.shelf = true;
      left.hover = true;
      left.save(file);
      Placement back = Placement.load(file);
      check(back.x == 300 && back.bottom == 500 && back.size == 2 && back.shelf && back.hover && back.scale() == 2, "a place comes back as it was saved");
      check(!fresh.hover, "hover to press starts off");
      java.nio.file.Files.write(file.toPath(), "x=12\nbottom=oops\nsize=9\nshelf=on\njunk\n".getBytes("utf-8"));
      back = Placement.load(file);
      check(back.x == -1 && back.bottom == -1 && back.size == 0 && back.shelf, "damaged lines are skipped, half a place is none");
      if (!java.awt.GraphicsEnvironment.isHeadless()) {
         SwingUtilities.invokeAndWait(() -> {
            file.delete();
            JFrame frame = Test.open(keys, file);
            check(frame.isAlwaysOnTop() && !frame.getFocusableWindowState() && frame.isUndecorated(),
               "the window stays on top, borderless, without taking the keyboard");
            check(!frame.isVisible() && frame.getWidth() == Test.WIDTH, "open() builds it without showing it");
            javax.swing.JButton more = find(frame.getContentPane(), Test.HIDE_SHORTCUTS);
            check(more != null && frame.getHeight() == Test.HEIGHT + ShortcutShelf.HEIGHT, "the first time, the shelf is open");
            more.doClick();
            check(Test.SHOW_SHORTCUTS.equals(more.getAccessibleContext().getAccessibleName()) && frame.getHeight() == Test.HEIGHT,
               "and closes");
            int bottom = frame.getY() + frame.getHeight();
            more.doClick();
            check(frame.getHeight() == Test.HEIGHT + ShortcutShelf.HEIGHT && frame.getY() + frame.getHeight() == bottom,
               "the shortcuts open upward, keeping the key where it was");
            // A window that was never shown is not laid out; a peer (still hidden) lets validate() do it.
            frame.addNotify();
            frame.validate();
            ShortcutShelf shelf = (ShortcutShelf) find(frame.getContentPane(), null);
            check(shelf.isVisible() && shelf.getHeight() == ShortcutShelf.HEIGHT, "the shelf gets its height: " + shelf.getHeight());
            java.awt.Rectangle row = shelf.rect(ShortcutShelf.HOVER_ROW);
            click(shelf, row.x + 5, row.y + 5, row.x + 5, row.y + 5);
            check(Placement.load(file).hover, "hover to press is kept once turned on");
            WindowsKey frameKey = (WindowsKey) frame.getContentPane().getComponent(1);
            frameKey.dwell().at(0, System.currentTimeMillis() - Dwell.MS * 2 / 3);
            shelf.dwell().at(1, System.currentTimeMillis() - Dwell.MS / 3);
            if (System.getProperty("shot") != null) {
               shot(frame, System.getProperty("shot"));
            }
            frameKey.dwell().at(-1);
            shelf.dwell().at(-1);
            click(shelf, row.x + 5, row.y + 5, row.x + 5, row.y + 5);
            check(!Placement.load(file).hover, "and off");
            more.doClick();
            check(frame.getHeight() == Test.HEIGHT && frame.getY() + frame.getHeight() == bottom && !shelf.isVisible(), "and close again");
            check(Placement.load(file).x == frame.getX() && Placement.load(file).bottom == bottom, "the place is kept once the user acts");
            javax.swing.JButton size = find(frame.getContentPane(), Test.KEY_SIZE + "Small, click for Medium");
            int right = frame.getX() + frame.getWidth();
            size.doClick();
            check(frame.getWidth() == Math.round(Test.WIDTH * 1.5) && frame.getHeight() == Math.round(Test.HEIGHT * 1.5)
               && frame.getX() + frame.getWidth() == right && frame.getY() + frame.getHeight() == bottom,
               "Medium makes it half again as big, its bottom right corner kept: " + frame.getBounds());
            check(Test.KEY_SIZE.concat("Medium, click for Large").equals(size.getAccessibleContext().getAccessibleName()), "the size button says what it is");
            size.doClick();
            more.doClick();
            check(frame.getWidth() == 2 * Test.WIDTH && frame.getHeight() == 2 * (Test.HEIGHT + ShortcutShelf.HEIGHT), "Large, with the shelf: " + frame.getBounds());
            frame.validate();
            check(shelf.getWidth() == 2 * Test.WIDTH && shelf.getHeight() == 2 * ShortcutShelf.HEIGHT, "the large shelf is twice as big: " + shelf.getSize());
            if (System.getProperty("bigshot") != null) {
               shot(frame, System.getProperty("bigshot"));
            }
            java.awt.Rectangle was = frame.getBounds();
            frame.dispose();
            // Opened again, it is where it was left, as big, with the shelf open.
            JFrame again = Test.open(keys, file);
            check(again.getBounds().equals(was) && find(again.getContentPane(), Test.HIDE_SHORTCUTS) != null,
               "opened again, it comes back as it was left: " + again.getBounds() + " vs " + was);
            again.dispose();
            // Left off the screen (a smaller screen since), it comes back whole on this one.
            Placement off = new Placement();
            off.x = 100000;
            off.bottom = 100000;
            off.save(file);
            JFrame lost = Test.open(keys, file);
            java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            check(lost.getX() + lost.getWidth() == screen.width && lost.getY() + lost.getHeight() == screen.height,
               "a place off the screen is brought back onto it: " + lost.getBounds());
            lost.dispose();
            off.x = 0;
            off.bottom = 10;
            off.save(file);
            lost = Test.open(keys, file);
            check(lost.getX() == 0 && lost.getY() == 0, "and so is one above it: " + lost.getBounds());
            lost.dispose();
         });
      }
      System.out.println("WindowsKeyCheck: all checks passed");
      System.exit(0);
   }

   private static void mouse(WindowsKey key, int id, int button) {
      key.dispatchEvent(new MouseEvent(key, id, 0, 0, 50, 50, 1, false, button));
   }

   private static void click(java.awt.Component c, int x, int y, int upX, int upY) {
      c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
      c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_RELEASED, 0, 0, upX, upY, 1, false, MouseEvent.BUTTON1));
   }

   /** The button with that accessible name in the window (with null, the shortcut shelf). */
   private static javax.swing.JButton find(java.awt.Container in, String name) {
      for (java.awt.Component c : in.getComponents()) {
         if (c instanceof javax.swing.JButton && (name == null ? c instanceof ShortcutShelf
            : name.equals(c.getAccessibleContext().getAccessibleName()))) {
            return (javax.swing.JButton) c;
         }
         if (c instanceof java.awt.Container) {
            javax.swing.JButton b = find((java.awt.Container) c, name);
            if (b != null) {
               return b;
            }
         }
      }
      return null;
   }

   /** The window drawn into a PNG without showing it, to look at the layout. */
   private static void shot(JFrame frame, String path) {
      try {
         BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
         frame.getContentPane().paint(image.getGraphics());
         javax.imageio.ImageIO.write(image, "png", new java.io.File(path));
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static void drag(WindowsKey key, int x, int y) {
      key.dispatchEvent(new MouseEvent(key, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.NOBUTTON));
   }

   /** Draws the key into an image, up and down, so a drawing mistake throws here. */
   private static void paint(javax.swing.JComponent key) {
      BufferedImage image = new BufferedImage(key.getWidth(), key.getHeight(), BufferedImage.TYPE_INT_ARGB);
      key.paint(image.getGraphics());
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         System.out.println("FAILED: " + what);
         System.exit(1);
      }
   }
}
