package circlewindow;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Self-check for a clicked shape tumbling to the floor and gathering back (Tumble). Run with java -ea. */
public final class TumbleCheck {
   public static void main(String[] args) {
      // A tall window, a wide one and a small one in the middle of a 1000 x 800 screen.
      List<Rectangle> home = Arrays.asList(new Rectangle(400, 200, 30, 300), new Rectangle(450, 300, 200, 40),
         new Rectangle(500, 400, 10, 10));
      Tumble tumble = new Tumble(home, 1000, 800, new Random(7));
      boolean landed = false;
      boolean moved = false;
      int frames = 0;
      boolean done = false;
      while (!done && frames < 1000) {
         boolean wasFalling = !tumble.gathering();
         done = tumble.step();
         frames++;
         for (int i = 0; i < home.size(); i++) {
            Rectangle r = home.get(i);
            moved |= tumble.x[i] != r.x || tumble.y[i] != r.y;
            if (wasFalling) {
               check(tumble.y[i] + r.height <= 800 && tumble.x[i] >= 0 && tumble.x[i] + r.width <= 1000,
                  "while falling window " + i + " stays above the floor and between the sides: " + tumble.x[i] + "," + tumble.y[i]);
            }
         }
         if (wasFalling && tumble.gathering()) {
            // The heap: every window lies on the floor when it starts to gather.
            landed = true;
            for (int i = 0; i < home.size(); i++) {
               check(tumble.y[i] + home.get(i).height == 800, "window " + i + " rests on the floor: " + tumble.y[i]);
            }
         }
      }
      check(moved, "the windows leave their places");
      check(landed, "the shape lands in a heap before it gathers");
      check(done && frames < 300, "it is back together within 5 s at 60 frames a second: " + frames + " frames");
      for (int i = 0; i < home.size(); i++) {
         check(Math.round(tumble.x[i]) == home.get(i).x && Math.round(tumble.y[i]) == home.get(i).y,
            "window " + i + " is back exactly in its place: " + tumble.x[i] + "," + tumble.y[i]);
      }
      System.out.println("TumbleCheck passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
