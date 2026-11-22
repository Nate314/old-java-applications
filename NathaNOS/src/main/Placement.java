package main;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Where a new window opens, as a window manager decides: on the part of the desktop that hides the least of the
 * windows already open, then of the desktop icons, and among equally clear spots the one nearest the middle. The
 * first window still opens in the middle; the next ones open beside it rather than on top of it.
 */
final class Placement {
   /** Spots are tried this far apart, plus the middle and the far edge. */
   private static final int STEP = 8;

   private Placement() {
   }

   /** The top-left corner for a w x h window in room, given the open windows and the icons' area (null for none). */
   static Point spot(int w, int h, Rectangle room, List<Rectangle> windows, Rectangle icons) {
      int midX = room.x + Math.max(0, (room.width - w) / 2);
      int midY = room.y + Math.max(0, (room.height - h) / 2);
      Point best = null;
      long bestScore = 0;
      long bestDistance = 0;
      for (int x : tries(room.x, room.x + Math.max(0, room.width - w), midX)) {
         for (int y : tries(room.y, room.y + Math.max(0, room.height - h), midY)) {
            // Hiding a window costs twice what hiding icons does: an app in use matters more than a shortcut.
            long score = icons == null ? 0 : overlap(x, y, w, h, icons);
            for (Rectangle window : windows) {
               score += 2 * overlap(x, y, w, h, window);
            }
            long distance = (long)(x - midX) * (x - midX) + (long)(y - midY) * (y - midY);
            if (best == null || score < bestScore || score == bestScore && distance < bestDistance) {
               best = new Point(x, y);
               bestScore = score;
               bestDistance = distance;
            }
         }
      }
      return best;
   }

   /** from, from + STEP, ... up to last, then last and mid themselves. */
   private static int[] tries(int from, int last, int mid) {
      int steps = (last - from) / STEP + 1;
      int[] tries = new int[steps + 2];
      for (int i = 0; i < steps; i++) {
         tries[i] = from + i * STEP;
      }
      tries[steps] = last;
      tries[steps + 1] = mid;
      return tries;
   }

   private static long overlap(int x, int y, int w, int h, Rectangle r) {
      long across = Math.min(x + w, r.x + r.width) - Math.max(x, r.x);
      long down = Math.min(y + h, r.y + r.height) - Math.max(y, r.y);
      return across > 0 && down > 0 ? across * down : 0;
   }
}
