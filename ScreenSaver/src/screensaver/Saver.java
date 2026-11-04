package screensaver;

import java.awt.Graphics2D;
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
}
