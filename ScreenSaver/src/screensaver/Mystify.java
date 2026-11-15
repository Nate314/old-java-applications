package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Random;

/** Two four-cornered outlines whose corners bounce off the edges, each leaving a fading trail, in slowly changing colors. */
class Mystify implements Saver {
   private static final int SHAPES = 2;
   private static final int CORNERS = 4;
   /** Outlines kept per shape, the newest first. */
   private static final int TRAIL = 9;
   /** A new outline joins the trail this often, in milliseconds at normal speed. */
   private static final double TRAIL_MILLIS = 45;

   /** [shape][corner] position and velocity (pixels per millisecond). */
   private final double[][] x = new double[SHAPES][CORNERS];
   private final double[][] y = new double[SHAPES][CORNERS];
   private final double[][] dx = new double[SHAPES][CORNERS];
   private final double[][] dy = new double[SHAPES][CORNERS];
   /** [shape][trail index][corner] */
   private final int[][][] trailX = new int[SHAPES][TRAIL][CORNERS];
   private final int[][][] trailY = new int[SHAPES][TRAIL][CORNERS];
   private final float[] hue = new float[SHAPES];
   private int width;
   private int height;
   private double sinceTrail;
   /** Each corner's own speed, which it drifts back to after the pointer let it go. */
   private final double[][] cruise = new double[SHAPES][CORNERS];
   /** Play (T): while the pointer is held down, every corner is drawn toward it, or null. */
   private Point pull;

   @Override
   public String name() {
      return "Mystify";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.width = width;
      this.height = height;
      double speed = Math.max(width, height) / 6000.0;
      for (int s = 0; s < SHAPES; s++) {
         this.hue[s] = random.nextFloat();
         for (int c = 0; c < CORNERS; c++) {
            this.x[s][c] = random.nextDouble() * width;
            this.y[s][c] = random.nextDouble() * height;
            this.dx[s][c] = (0.4 + random.nextDouble()) * speed * (random.nextBoolean() ? 1 : -1);
            this.dy[s][c] = (0.4 + random.nextDouble()) * speed * (random.nextBoolean() ? 1 : -1);
            this.cruise[s][c] = Math.hypot(this.dx[s][c], this.dy[s][c]);
         }
         for (int t = 0; t < TRAIL; t++) {
            this.record(s, t);
         }
      }
      this.sinceTrail = 0;
      this.pull = null;
   }

   @Override
   public void touch(Point at, boolean down) {
      this.pull = down ? at : null;
   }

   /** How fast a corner moves now, in pixels per millisecond. */
   double speed(int shape, int corner) {
      return Math.hypot(this.dx[shape][corner], this.dy[shape][corner]);
   }

   double cruise(int shape, int corner) {
      return this.cruise[shape][corner];
   }

   /** Pulls a corner toward the pointer (up to three times its own speed), or eases it back to its own speed. */
   private void steer(int s, int c, double millis) {
      double v = this.speed(s, c);
      double want = this.cruise[s][c];
      if (this.pull != null) {
         double tx = this.pull.x - this.x[s][c];
         double ty = this.pull.y - this.y[s][c];
         double d = Math.max(1, Math.hypot(tx, ty));
         double a = want / 250 * millis;
         this.dx[s][c] += tx / d * a;
         this.dy[s][c] += ty / d * a;
         v = this.speed(s, c);
         want = Math.min(v, want * 3);
      } else {
         want = v + (want - v) * Math.min(1, millis / 1500);
      }
      if (v > 0) {
         this.dx[s][c] *= want / v;
         this.dy[s][c] *= want / v;
      }
   }

   private void record(int shape, int index) {
      for (int c = 0; c < CORNERS; c++) {
         this.trailX[shape][index][c] = (int)this.x[shape][c];
         this.trailY[shape][index][c] = (int)this.y[shape][c];
      }
   }

   @Override
   public void tick(double millis) {
      this.sinceTrail += millis;
      // Moves in steps of one trail interval at most, so a fast speed still leaves an even trail.
      while (this.sinceTrail >= TRAIL_MILLIS) {
         this.sinceTrail -= TRAIL_MILLIS;
         this.move(TRAIL_MILLIS);
         for (int s = 0; s < SHAPES; s++) {
            int[][] oldX = this.trailX[s];
            int[][] oldY = this.trailY[s];
            System.arraycopy(oldX, 0, oldX, 1, TRAIL - 1);
            System.arraycopy(oldY, 0, oldY, 1, TRAIL - 1);
            oldX[0] = new int[CORNERS];
            oldY[0] = new int[CORNERS];
            this.record(s, 0);
            this.hue[s] = (this.hue[s] + 0.0015F) % 1F;
         }
      }
   }

   private void move(double millis) {
      for (int s = 0; s < SHAPES; s++) {
         for (int c = 0; c < CORNERS; c++) {
            this.steer(s, c, millis);
            this.x[s][c] += this.dx[s][c] * millis;
            this.y[s][c] += this.dy[s][c] * millis;
            if (this.x[s][c] < 0 || this.x[s][c] > this.width) {
               this.dx[s][c] = -this.dx[s][c];
               this.x[s][c] = Math.max(0, Math.min(this.width, this.x[s][c]));
            }
            if (this.y[s][c] < 0 || this.y[s][c] > this.height) {
               this.dy[s][c] = -this.dy[s][c];
               this.y[s][c] = Math.max(0, Math.min(this.height, this.y[s][c]));
            }
         }
      }
   }

   @Override
   public void paint(Graphics2D g) {
      g.setStroke(new BasicStroke(1.6F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      for (int s = 0; s < SHAPES; s++) {
         for (int t = TRAIL - 1; t >= 0; t--) {
            float brightness = 1F - t / (float)TRAIL * 0.85F;
            g.setColor(Color.getHSBColor(this.hue[s], 0.85F, brightness));
            g.drawPolygon(this.trailX[s][t], this.trailY[s][t], CORNERS);
         }
      }
      g.setStroke(new BasicStroke(1));
   }
}
