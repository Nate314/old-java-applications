package circlewindow;

import java.awt.Rectangle;
import java.util.List;

/**
 * The shape held by the pointer, as a jelly: the windows under the pointer go where it goes, and every other window
 * springs after them, looser the farther it is, so the shape stretches as it is dragged and wobbles when it stops.
 * Let go while moving and the shape is thrown: it glides on, slowing, bounces off the edges of the screen and comes
 * to rest with every window back in its exact place in the shape. Positions are the windows' top-left corners.
 */
final class Jelly {
   /** Spring pull per frame of the nearest loose window, falling off with distance from the pointer. */
   private static final double PULL = 0.4;
   private static final double FALLOFF = 100;
   private static final double DAMPING = 0.8;
   /** How much of its speed a thrown shape keeps each frame, and off an edge of the screen. */
   private static final double GLIDE = 0.95;
   private static final double BOUNCE = 0.6;
   private static final double MAX_THROW = 60;
   private static final double REST = 0.3;

   final double[] x;
   final double[] y;
   private final double[] vx;
   private final double[] vy;
   private final int[] homeX;
   private final int[] homeY;
   /** 1 for the windows under the pointer (they follow it exactly), less for the others. */
   private final double[] pull;
   private final Rectangle area;
   private final int screenWidth;
   private final int screenHeight;
   private final boolean still;
   /** How far the shape has been moved from where it was picked up, and how fast it is going. */
   private double moveX;
   private double moveY;
   private double speedX;
   private double speedY;
   private double lastX;
   private double lastY;
   private boolean held = true;

   /**
    * Picks up the shape whose windows have the given bounds at the screen point (grabX, grabY), on a screen of the
    * given size. With still (reduced motion) the shape moves as one piece and is never thrown.
    */
   Jelly(List<Rectangle> bounds, int grabX, int grabY, int screenWidth, int screenHeight, boolean still) {
      int n = bounds.size();
      this.x = new double[n];
      this.y = new double[n];
      this.vx = new double[n];
      this.vy = new double[n];
      this.homeX = new int[n];
      this.homeY = new int[n];
      this.pull = new double[n];
      this.area = n == 0 ? new Rectangle() : new Rectangle(bounds.get(0));
      for (int i = 0; i < n; i++) {
         Rectangle r = bounds.get(i);
         this.area.add(r);
         this.x[i] = this.homeX[i] = r.x;
         this.y[i] = this.homeY[i] = r.y;
         double dx = Math.max(0, Math.max(r.x - grabX, grabX - (r.x + r.width)));
         double dy = Math.max(0, Math.max(r.y - grabY, grabY - (r.y + r.height)));
         double distance = Math.hypot(dx, dy);
         this.pull[i] = still || distance == 0 ? 1 : PULL / (1 + distance / FALLOFF);
      }
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.still = still;
   }

   /** The pointer has moved (dx, dy) from where it picked the shape up. */
   void hold(int dx, int dy) {
      this.moveX = dx;
      this.moveY = dy;
   }

   /** Lets go: the shape keeps the speed the pointer had. */
   void letGo() {
      this.held = false;
      if (this.still) {
         this.speedX = 0;
         this.speedY = 0;
      }
      double speed = Math.hypot(this.speedX, this.speedY);
      if (speed > MAX_THROW) {
         this.speedX *= MAX_THROW / speed;
         this.speedY *= MAX_THROW / speed;
      }
      this.keepOnScreen();
   }

   /** How far the shape has moved, in whole pixels, once it is at rest. */
   int movedX() {
      return (int)Math.round(this.moveX);
   }

   int movedY() {
      return (int)Math.round(this.moveY);
   }

   /** One frame; true when the shape has been let go and come to rest, with every window exactly in place. */
   boolean step() {
      if (this.held) {
         // The pointer's speed, smoothed over a few frames, is what a throw starts with.
         this.speedX = (this.speedX + this.moveX - this.lastX) / 2;
         this.speedY = (this.speedY + this.moveY - this.lastY) / 2;
         this.lastX = this.moveX;
         this.lastY = this.moveY;
      } else {
         this.moveX += this.speedX;
         this.moveY += this.speedY;
         this.speedX *= GLIDE;
         this.speedY *= GLIDE;
         this.keepOnScreen();
      }
      boolean resting = !this.held && Math.hypot(this.speedX, this.speedY) < REST;
      for (int i = 0; i < this.x.length; i++) {
         double toX = this.homeX[i] + this.moveX;
         double toY = this.homeY[i] + this.moveY;
         if (this.pull[i] >= 1) {
            this.x[i] = toX;
            this.y[i] = toY;
            this.vx[i] = 0;
            this.vy[i] = 0;
            continue;
         }
         this.vx[i] = (this.vx[i] + (toX - this.x[i]) * this.pull[i]) * DAMPING;
         this.vy[i] = (this.vy[i] + (toY - this.y[i]) * this.pull[i]) * DAMPING;
         this.x[i] += this.vx[i];
         this.y[i] += this.vy[i];
         resting &= Math.abs(toX - this.x[i]) < 0.5 && Math.abs(toY - this.y[i]) < 0.5
            && Math.hypot(this.vx[i], this.vy[i]) < REST;
      }
      if (resting) {
         this.settle();
      }
      return resting;
   }

   /** Puts every window exactly in its place in the shape, where the shape has got to (in whole pixels). */
   void settle() {
      for (int i = 0; i < this.x.length; i++) {
         this.x[i] = this.homeX[i] + this.movedX();
         this.y[i] = this.homeY[i] + this.movedY();
      }
   }

   /** Bounces the shape back off any edge of the screen it has gone past (unless it is wider or taller than it). */
   private void keepOnScreen() {
      double left = this.area.x + this.moveX;
      double top = this.area.y + this.moveY;
      if (this.area.width <= this.screenWidth) {
         if (left < 0) {
            this.moveX -= left;
            this.speedX = Math.abs(this.speedX) * BOUNCE;
         } else if (left + this.area.width > this.screenWidth) {
            this.moveX -= left + this.area.width - this.screenWidth;
            this.speedX = -Math.abs(this.speedX) * BOUNCE;
         }
      }
      if (this.area.height <= this.screenHeight) {
         if (top < 0) {
            this.moveY -= top;
            this.speedY = Math.abs(this.speedY) * BOUNCE;
         } else if (top + this.area.height > this.screenHeight) {
            this.moveY -= top + this.area.height - this.screenHeight;
            this.speedY = -Math.abs(this.speedY) * BOUNCE;
         }
      }
   }
}
