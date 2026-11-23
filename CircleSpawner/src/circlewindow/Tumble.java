package circlewindow;

import java.awt.Rectangle;
import java.util.List;
import java.util.Random;

/**
 * A clicked shape coming apart and back together: every window flies out from the middle, falls, bounces off the
 * floor and sides of the screen and lands in a heap on the floor; after a short rest each one leaps back to its
 * place in the shape, one after another, the shape rebuilt exactly as it was. Positions are top-left corners.
 */
final class Tumble {
   private static final double GRAVITY = 0.9;
   /** How much of its speed a window keeps off the floor and the sides, and sliding along the floor. */
   private static final double BOUNCE = 0.4;
   private static final double SLIDE = 0.85;
   /** Frames the heap lies still before it gathers, and the longest the fall may take before it gathers anyway. */
   private static final int REST_FRAMES = 30;
   private static final int MAX_FALL_FRAMES = 150;
   /** Frames for one window to fly home, and over how many frames the windows set off, one after another. */
   private static final int GATHER_FRAMES = 30;
   private static final int STAGGER_FRAMES = 25;

   final double[] x;
   final double[] y;
   private final double[] vx;
   private final double[] vy;
   private final int[] w;
   private final int[] h;
   private final int[] homeX;
   private final int[] homeY;
   private final double[] fromX;
   private final double[] fromY;
   private final int screenWidth;
   private final int screenHeight;
   private int frame;
   private int still;
   /** Frames since the heap began to gather, or -1 while it is still falling. */
   private int gather = -1;

   Tumble(List<Rectangle> home, int screenWidth, int screenHeight, Random random) {
      int n = home.size();
      this.x = new double[n];
      this.y = new double[n];
      this.vx = new double[n];
      this.vy = new double[n];
      this.w = new int[n];
      this.h = new int[n];
      this.homeX = new int[n];
      this.homeY = new int[n];
      this.fromX = new double[n];
      this.fromY = new double[n];
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      double centerX = 0;
      double centerY = 0;
      for (Rectangle r : home) {
         centerX += (r.x + r.width / 2.0) / n;
         centerY += (r.y + r.height / 2.0) / n;
      }
      for (int i = 0; i < n; i++) {
         Rectangle r = home.get(i);
         this.x[i] = this.homeX[i] = r.x;
         this.y[i] = this.homeY[i] = r.y;
         this.w[i] = r.width;
         this.h[i] = r.height;
         this.vx[i] = ((r.x + r.width / 2.0) - centerX) * 0.08 + (random.nextDouble() - 0.5) * 8.0;
         this.vy[i] = ((r.y + r.height / 2.0) - centerY) * 0.08 - 6.0 - random.nextDouble() * 6.0;
      }
   }

   /** True once the windows have left the heap and are on their way back. */
   boolean gathering() {
      return this.gather >= 0;
   }

   /** One frame; true when every window is back exactly in its place. */
   boolean step() {
      if (this.gather < 0) {
         this.fall();
         return false;
      }
      this.gather++;
      int n = this.x.length;
      for (int i = 0; i < n; i++) {
         double t = Math.max(0, Math.min(1, (this.gather - STAGGER_FRAMES * i / (double)Math.max(1, n)) / GATHER_FRAMES));
         t = t * t * (3.0 - 2.0 * t);
         this.x[i] = this.fromX[i] + (this.homeX[i] - this.fromX[i]) * t;
         // A hop: up over the heap on the way, as if each window jumped back into place.
         this.y[i] = this.fromY[i] + (this.homeY[i] - this.fromY[i]) * t - 120 * Math.sin(Math.PI * t);
      }
      return this.gather >= STAGGER_FRAMES + GATHER_FRAMES;
   }

   private void fall() {
      this.frame++;
      boolean resting = true;
      for (int i = 0; i < this.x.length; i++) {
         this.vy[i] += GRAVITY;
         this.x[i] += this.vx[i];
         this.y[i] += this.vy[i];
         double floor = this.screenHeight - this.h[i];
         if (this.y[i] >= floor) {
            this.y[i] = floor;
            this.vy[i] = Math.abs(this.vy[i]) < 3 ? 0 : -Math.abs(this.vy[i]) * BOUNCE;
            this.vx[i] *= SLIDE;
         }
         double right = Math.max(0, this.screenWidth - this.w[i]);
         if (this.x[i] < 0 || this.x[i] > right) {
            this.x[i] = Math.max(0, Math.min(right, this.x[i]));
            this.vx[i] = -this.vx[i] * BOUNCE;
         }
         resting &= this.y[i] >= floor && this.vy[i] == 0 && Math.abs(this.vx[i]) < 0.3;
      }
      this.still = resting ? this.still + 1 : 0;
      if (this.still >= REST_FRAMES || this.frame >= MAX_FALL_FRAMES) {
         this.gather = 0;
         System.arraycopy(this.x, 0, this.fromX, 0, this.x.length);
         System.arraycopy(this.y, 0, this.fromY, 0, this.y.length);
      }
   }
}
