package launcher;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * The arithmetic of a selection move: how far along it is (eased, so it starts fast and settles), and the ring,
 * picture and tone partway between where they were and where they go.
 */
final class Glide {
   static final int RING_MS = 170;   // the selection ring sliding to the new card
   static final int FLY_MS = 320;    // the card's picture flying into the detail pane, its tone washing in

   private Glide() {
   }

   /** 0 at the start, 1 once {@code ms} have passed, eased out (cubic): quick off the mark, gentle landing. */
   static double at(long since, int ms) {
      double t = Math.max(0, Math.min(1, since / (double) ms));
      return 1 - Math.pow(1 - t, 3);
   }

   static Rectangle between(Rectangle a, Rectangle b, double t) {
      return new Rectangle(mid(a.x, b.x, t), mid(a.y, b.y, t), mid(a.width, b.width, t), mid(a.height, b.height, t));
   }

   static Color between(Color a, Color b, double t) {
      return new Color(mid(a.getRed(), b.getRed(), t), mid(a.getGreen(), b.getGreen(), t), mid(a.getBlue(), b.getBlue(), t));
   }

   private static int mid(int a, int b, double t) {
      return (int) Math.round(a + (b - a) * t);
   }
}
