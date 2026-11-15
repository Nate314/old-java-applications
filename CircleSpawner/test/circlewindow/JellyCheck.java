package circlewindow;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

/** Self-check for carrying and throwing a shape (Jelly). Run with java -ea (headless is fine). */
public class JellyCheck {
   public static void main(String[] args) {
      // Two windows: the pointer is on the first; the second is 300 px off.
      List<Rectangle> shape = Arrays.asList(new Rectangle(100, 100, 20, 20), new Rectangle(400, 100, 20, 20));
      Jelly jelly = new Jelly(shape, 110, 110, 1000, 800, false);
      jelly.hold(50, 30);
      jelly.step();
      check(jelly.x[0] == 150 && jelly.y[0] == 130, "the window under the pointer follows it exactly: " + jelly.x[0] + "," + jelly.y[0]);
      check(jelly.x[1] > 400 && jelly.x[1] < 450, "a window farther off lags behind: " + jelly.x[1]);

      // Held still, then let go: it comes to rest with both windows exactly in place, 50,30 along.
      for (int i = 0; i < 20; i++) {
         jelly.step();
      }
      jelly.letGo();
      check(settle(jelly), "a dropped shape comes to rest");
      check(jelly.x[1] == 450 && jelly.y[1] == 130 && jelly.movedX() == 50 && jelly.movedY() == 30,
         "at rest every window is in its place: " + jelly.x[1] + "," + jelly.y[1]);

      // Thrown right: it glides on past where it was let go, bounces off the right edge and stays on the screen.
      jelly = new Jelly(shape, 110, 110, 1000, 800, false);
      for (int i = 1; i <= 5; i++) {
         jelly.hold(40 * i, 0);
         jelly.step();
      }
      jelly.letGo();
      check(settle(jelly), "a thrown shape comes to rest");
      check(jelly.movedX() > 200, "a throw carries it on: " + jelly.movedX());
      check(jelly.x[1] + 20 <= 1000 && jelly.x[0] >= 0, "it stays on the screen: " + jelly.x[0] + " to " + (jelly.x[1] + 20));

      // Let go past the bottom: it comes back up onto the screen.
      jelly = new Jelly(shape, 110, 110, 1000, 800, false);
      jelly.hold(0, 900);
      jelly.step();
      jelly.letGo();
      check(settle(jelly) && jelly.y[0] + 20 <= 800, "dropped off the bottom, it comes back: " + jelly.y[0]);

      // Reduced motion: the whole shape moves as one piece and a throw does not carry it on.
      jelly = new Jelly(shape, 110, 110, 1000, 800, true);
      for (int i = 1; i <= 5; i++) {
         jelly.hold(40 * i, 0);
         jelly.step();
         check(jelly.x[1] - jelly.x[0] == 300, "still: the windows keep their distance");
      }
      jelly.letGo();
      check(jelly.step() && jelly.movedX() == 200, "still: it rests where it was let go: " + jelly.movedX());
      System.out.println("JellyCheck passed");
   }

   private static boolean settle(Jelly jelly) {
      for (int i = 0; i < 2000; i++) {
         if (jelly.step()) {
            return true;
         }
      }
      return false;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
