package test;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
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
      });
      if (!java.awt.GraphicsEnvironment.isHeadless()) {
         SwingUtilities.invokeAndWait(() -> {
            JFrame frame = Test.open(keys);
            check(frame.isAlwaysOnTop() && !frame.getFocusableWindowState() && frame.isUndecorated(),
               "the window stays on top, borderless, without taking the keyboard");
            check(!frame.isVisible() && frame.getWidth() == Test.WIDTH, "open() builds it without showing it");
            frame.dispose();
         });
      }
      System.out.println("WindowsKeyCheck: all checks passed");
      System.exit(0);
   }

   private static void mouse(WindowsKey key, int id, int button) {
      key.dispatchEvent(new MouseEvent(key, id, 0, 0, 50, 50, 1, false, button));
   }

   private static void drag(WindowsKey key, int x, int y) {
      key.dispatchEvent(new MouseEvent(key, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.NOBUTTON));
   }

   /** Draws the key into an image, up and down, so a drawing mistake throws here. */
   private static void paint(WindowsKey key) {
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
