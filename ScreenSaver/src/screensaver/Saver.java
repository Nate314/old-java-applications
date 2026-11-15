package screensaver;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Random;

/** One screen saver: reset to a screen size, advanced by elapsed time, and painted over a black screen. */
interface Saver {
   String name();

   /** Starts over on a screen of this size. */
   void reset(int width, int height, Random random);

   /** Advances by this many milliseconds, already multiplied by the speed the arrow keys set. */
   void tick(double millis);

   /** Draws the current picture; the screen has just been filled with black. */
   void paint(Graphics2D g);

   /**
    * Play (T): the pointer is at this screen point, pressed or not, or has gone (null). Savers that do nothing with
    * it keep this default; each one forgets the pointer on reset.
    */
   default void touch(Point at, boolean down) {
   }

   /**
    * How far a thing at (x, y) is pushed away from the pointer: up to most pixels when on it, fading to nothing at
    * radius. Returns {dx, dy}, both 0 with no pointer.
    */
   static double[] away(double x, double y, Point from, double radius, double most) {
      if (from == null) {
         return new double[] {0, 0};
      }
      double dx = x - from.x;
      double dy = y - from.y;
      double d = Math.sqrt(dx * dx + dy * dy);
      if (d >= radius) {
         return new double[] {0, 0};
      }
      double f = 1 - d / radius;
      double push = most * f * f;
      // Right on the pointer there is no direction; push straight up.
      return d < 0.5 ? new double[] {0, -push} : new double[] {dx / d * push, dy / d * push};
   }
}
