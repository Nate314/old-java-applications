package dinosaurgameplayer;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The land behind a run: flat-topped mesas far off and low dunes nearer, each sliding past slower than the ground
 * (the mesas at an eighth of its speed, the dunes at a third), so the run has depth. Worked out from the distance
 * alone, so a replay or a ghost's course shows the same land. Pale grays only: never blue 83, so the bot never sees it.
 */
final class Land {

   static final Color FAR = new Color(0xE9E9E9);
   static final Color SHADE = new Color(0xE0E0E0);
   static final Color NEAR = new Color(0xDDDDDD);
   private static final double FAR_PACE = 0.125;
   private static final double NEAR_PACE = 0.33;
   private static final int MESA_EVERY = 170;

   private Land() {
   }

   static void paint(Graphics2D g, double distance, double dark) {
      paintMesas(g, distance * FAR_PACE, dark);
      paintDunes(g, distance * NEAR_PACE, dark);
   }

   /** One mesa in most stretches of MESA_EVERY: a flat top, a sunlit left side and a shaded right one. */
   private static void paintMesas(Graphics2D g, double x0, double dark) {
      Color far = Sky.tint(FAR, dark);
      Color shade = Sky.tint(SHADE, dark);
      int base = Runner.GROUND;
      // Start a stretch early: a wide mesa can reach in from the left.
      for (long n = (long) Math.floor(x0 / MESA_EVERY) - 1; n * MESA_EVERY - x0 < Runner.WIDTH; n++) {
         long h = (n * 2654435761L) >>> 9;
         if (h % 5 == 0) {
            continue;
         }
         int width = 80 + (int) (h % 70);
         int height = 20 + (int) (h / 7 % 26);
         int slope = 10 + (int) (h / 13 % 14);
         int left = (int) Math.round(n * MESA_EVERY - x0) + (int) (h / 17 % 40);
         int top = base - height;
         g.setColor(far);
         g.fillPolygon(new int[] { left, left + slope, left + width - slope, left + width },
            new int[] { base, top, top, base }, 4);
         g.setColor(shade);
         g.fillPolygon(new int[] { left + width - slope, left + width, left + width - slope - 6 },
            new int[] { top, base, base }, 3);
         if (h % 3 == 0) {
            // A butte: a narrower step on top.
            int w = width / 3;
            int bx = left + slope + (int) (h / 23 % Math.max(1, width - 2 * slope - w));
            g.setColor(far);
            g.fillPolygon(new int[] { bx, bx + 5, bx + w - 5, bx + w }, new int[] { top, top - 12, top - 12, top }, 4);
            g.setColor(shade);
            g.fillPolygon(new int[] { bx + w - 5, bx + w, bx + w - 8 }, new int[] { top - 12, top, top }, 3);
         }
      }
   }

   /** Low rolling dunes along the ground line, from three waves added together. */
   private static void paintDunes(Graphics2D g, double x0, double dark) {
      int step = 6;
      int n = Runner.WIDTH / step + 1;
      int[] px = new int[n + 3];
      int[] py = new int[n + 3];
      for (int i = 0; i <= n; i++) {
         double u = i * step + x0;
         double rise = 11 + 6 * Math.sin(u / 61) + 4 * Math.sin(u / 27 + 1.3) + 2 * Math.sin(u / 113 + 0.4);
         px[i] = i * step;
         py[i] = Runner.GROUND - (int) Math.round(Math.max(1, rise));
      }
      // Closed along the ground line.
      px[n + 1] = Runner.WIDTH;
      py[n + 1] = Runner.GROUND;
      px[n + 2] = 0;
      py[n + 2] = Runner.GROUND;
      g.setColor(Sky.tint(NEAR, dark));
      g.fillPolygon(px, py, n + 3);
   }
}
