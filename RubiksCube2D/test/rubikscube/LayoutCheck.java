package rubikscube;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Self-check for the phone layout: on screens narrower than 600 px the window takes the screen's width, fits its
 * height, and in both views every control lies inside the window, clear of the others and of the cube; the desktop
 * window is unchanged. Needs a display (it makes the window without showing it). Run with java -ea.
 */
public class LayoutCheck {
   public static void main(String[] args) throws Exception {
      CubeFrame desk = new CubeFrame(new Dimension(1920, 1080));
      check(desk.getWidth() == 550 && desk.getHeight() == 700, "desktop window is 550x700");
      check(findButton(desk, "Teach me").getBounds().equals(new Rectangle(12, 10, 116, 28)), "desktop Teach me where it was");
      desk.dispose();
      for (int[] screen : new int[][]{{360, 640}, {390, 700}, {412, 800}, {599, 900}}) {
         CubeFrame frame = new CubeFrame(new Dimension(screen[0], screen[1]));
         String name = screen[0] + "x" + screen[1];
         check(frame.getWidth() == screen[0], name + ": window as wide as the screen, not " + frame.getWidth());
         check(frame.getHeight() <= Math.max(676, screen[1]), name + ": window fits the screen's height, not " + frame.getHeight());
         for (int pass = 0; pass < 2; pass++) {
            checkLayout(frame, name + (pass == 0 ? ", 3D" : ", net"));
            frame.toggleView();
         }
         frame.dispose();
      }
      System.out.println("LayoutCheck passed.");
      System.exit(0);
   }

   private static void checkLayout(CubeFrame frame, String what) {
      JPanel panel = (JPanel) frame.getContentPane().getComponent(0);
      // The window's content: its size less a title bar's worth (about 40 px tall and 16 px wide on the desktop).
      Rectangle window = new Rectangle(0, 0, frame.getWidth() - 16, frame.getHeight() - 40);
      Rectangle cube = null;
      List<Component> shown = new ArrayList<>();
      for (Component c : panel.getComponents()) {
         if (!c.isVisible() || c.getWidth() == 0) {
            continue;
         }
         check(window.contains(c.getBounds()), what + ": " + c + " inside the window");
         if (c instanceof CubeView3D) {
            cube = c.getBounds();
         } else {
            shown.add(c);
         }
      }
      check(shown.size() > 20, what + ": controls show");
      int bottom = 0;
      for (int i = 0; i < shown.size(); i++) {
         Rectangle a = shown.get(i).getBounds();
         for (int j = i + 1; j < shown.size(); j++) {
            check(!a.intersects(shown.get(j).getBounds()), what + ": " + shown.get(i) + " overlaps " + shown.get(j));
         }
         bottom = Math.max(bottom, a.y + a.height);
         // Over the cube only the row of buttons at the top.
         if (cube != null && a.y > 40) {
            check(a.y >= cube.y + cube.height, what + ": " + shown.get(i) + " clear of the cube");
         }
      }
      check(window.height - bottom < 40, what + ": no empty strip at the bottom (" + (window.height - bottom) + " px)");
   }

   private static javax.swing.JButton findButton(CubeFrame frame, String text) {
      for (Component c : ((JPanel) frame.getContentPane().getComponent(0)).getComponents()) {
         if (c instanceof javax.swing.JButton && text.equals(((javax.swing.JButton) c).getText())) {
            return (javax.swing.JButton) c;
         }
      }
      throw new AssertionError("no " + text + " button");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
