package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/** Flying through space: stars come from the middle of the screen and streak past the edges. */
class Starfield implements Saver {
   private static final int STARS = 360;
   /** Depth travelled per millisecond at normal speed: a star that starts far away takes about 3 s to pass. */
   private static final double DEPTH_PER_MILLI = 0.00033;
   private static final double NEAREST = 0.04;
   /** A star is drawn as a streak from this much farther away, so streaks do not depend on the frame rate. */
   private static final double STREAK = 0.03;

   private final double[] x = new double[STARS];
   private final double[] y = new double[STARS];
   private final double[] z = new double[STARS];
   private int width;
   private int height;
   private Random random;

   @Override
   public String name() {
      return "Starfield";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.width = width;
      this.height = height;
      this.random = random;
      for (int i = 0; i < STARS; i++) {
         this.place(i, NEAREST + random.nextDouble() * (1 - NEAREST));
      }
   }

   private void place(int i, double depth) {
      this.x[i] = this.random.nextDouble() * 2 - 1;
      this.y[i] = this.random.nextDouble() * 2 - 1;
      this.z[i] = depth;
   }

   @Override
   public void tick(double millis) {
      for (int i = 0; i < STARS; i++) {
         this.z[i] -= millis * DEPTH_PER_MILLI;
         if (this.z[i] < NEAREST || !this.onScreen(this.screenX(i, this.z[i]), this.screenY(i, this.z[i]))) {
            this.place(i, 1.0);
         }
      }
   }

   private double scale() {
      return Math.max(this.width, this.height) * 0.5;
   }

   private double screenX(int i, double depth) {
      return this.width / 2.0 + this.x[i] / depth * this.scale() * 0.5;
   }

   private double screenY(int i, double depth) {
      return this.height / 2.0 + this.y[i] / depth * this.scale() * 0.5;
   }

   private boolean onScreen(double sx, double sy) {
      return sx >= -20 && sy >= -20 && sx <= this.width + 20 && sy <= this.height + 20;
   }

   @Override
   public void paint(Graphics2D g) {
      for (int i = 0; i < STARS; i++) {
         double nearness = 1 - this.z[i];
         int shade = (int)Math.max(40, Math.min(255, 40 + nearness * 230));
         // Far stars are a little blue, near ones white.
         g.setColor(new Color(shade * 3 / 4 + 10, shade * 4 / 5 + 10, Math.min(255, shade + 30)));
         float thickness = (float)Math.max(1, nearness * nearness * 4);
         g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         int x1 = (int)this.screenX(i, Math.min(1, this.z[i] + STREAK));
         int y1 = (int)this.screenY(i, Math.min(1, this.z[i] + STREAK));
         int x2 = (int)this.screenX(i, this.z[i]);
         int y2 = (int)this.screenY(i, this.z[i]);
         if (x1 == x2 && y1 == y2) {
            g.fillRect(x2, y2, 1, 1);
         } else {
            g.drawLine(x1, y1, x2, y2);
         }
      }
      g.setStroke(new BasicStroke(1));
   }
}
