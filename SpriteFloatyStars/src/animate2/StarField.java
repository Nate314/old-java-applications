package animate2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import javax.swing.Timer;

public final class StarField extends Component {
   /** Movement per star per step was tuned for an uncapped repaint loop; this many steps per second keeps a similar pace. */
   private static final double STEPS_PER_SECOND = 200.0;
   public int starCount;
   private int starSize = 20;
   private int fieldWidth;
   private int fieldHeight;
   private Dimension startSize;
   private double[] x;
   private double[] y;
   private double[] velocityX;
   private double[] velocityY;
   private int[] randomColorValues;
   private int mode = 2;
   private long lastTickNanos = System.nanoTime();

   public StarField(int mode, Dimension size, int starCount, int starSize) {
      this(mode, size, starCount, starSize, true);
   }

   /** With {@code animate} false nothing moves until {@link #advance} is called (Star Catcher draws it as its sky). */
   StarField(int mode, Dimension size, int starCount, int starSize, boolean animate) {
      this.mode = mode;
      this.starCount = starCount;
      this.starSize = starSize;
      this.startSize = size;
      this.fieldWidth = size.width;
      this.fieldHeight = size.height;
      this.x = new double[starCount];
      this.y = new double[starCount];
      this.velocityX = new double[starCount];
      this.velocityY = new double[starCount];
      this.randomColorValues = new int[starCount * 3];

      for (int star = 0; star < this.starCount; star++) {
         this.initStar(star);
      }

      if (animate) {
         new Timer(15, event -> this.tick()).start();
      }
   }

   private void initStar(int star) {
      if (this.mode == 0 || this.mode == 3 || this.mode == 4) {
         this.x[star] = Math.random() * this.startSize.getWidth();
         this.y[star] = Math.random() * this.startSize.getHeight();
         if (this.mode == 3) {
            this.velocityX[star] = 2.0 * (Math.random() - 0.5);
            this.velocityY[star] = 2.0 * (Math.random() - 0.5);
         } else {
            this.velocityX[star] = Math.random();
            this.velocityY[star] = Math.random();
         }
      } else if (this.mode == 1 || this.mode == 2) {
         this.x[star] = this.startSize.getWidth() / 2.0 - (double)(this.starSize / 2);
         this.y[star] = this.startSize.getHeight() / 2.0 - (double)(this.starSize / 2);
         this.velocityX[star] = 2.0 * (Math.random() - 0.5);
         this.velocityY[star] = 2.0 * (Math.random() - 0.5);
      }

      for (int channel = 0; channel < 3; channel++) {
         this.randomColorValues[star * 3 + channel] = (int)(Math.random() * 255.0);
      }
   }

   /** Up arrow: adds a star, growing the arrays when needed. */
   public final void addStar() {
      if (this.starCount == this.x.length) {
         int capacity = Math.max(16, this.x.length * 2);
         this.x = Arrays.copyOf(this.x, capacity);
         this.y = Arrays.copyOf(this.y, capacity);
         this.velocityX = Arrays.copyOf(this.velocityX, capacity);
         this.velocityY = Arrays.copyOf(this.velocityY, capacity);
         this.randomColorValues = Arrays.copyOf(this.randomColorValues, capacity * 3);
      }
      this.initStar(this.starCount);
      this.starCount++;
   }

   /** Down arrow: removes the last star. */
   public final void removeStar() {
      if (this.starCount > 0) {
         this.starCount--;
      }
   }

   private void tick() {
      long now = System.nanoTime();
      // Cap the step so a stall (dragging the window, a GC pause) does not teleport the stars.
      double steps = Math.min(50.0, (now - this.lastTickNanos) / 1.0E9 * STEPS_PER_SECOND);
      this.lastTickNanos = now;
      if (this.getWidth() > 0) {
         this.fieldWidth = this.getWidth();
         this.fieldHeight = this.getHeight();
      }
      this.move(steps);
      this.repaint();
   }

   /** Moves the stars on by {@code seconds} in a field of the given size, without a timer or repaint. */
   final void advance(double seconds, int width, int height) {
      this.fieldWidth = width;
      this.fieldHeight = height;
      this.move(Math.min(50.0, seconds * STEPS_PER_SECOND));
   }

   private void move(double steps) {
      if (this.mode == 4) {
         this.flock();
      }

      for (int star = 0; star < this.starCount; star++) {
         if (this.mode == 0 || this.mode == 4) {
            this.x[star] = this.x[star] - this.velocityX[star] * steps;
            this.y[star] = this.y[star] + this.velocityY[star] * steps;
            if (this.x[star] < (double)(0 - this.starSize)) {
               this.x[star] = this.x[star] + (double)(this.fieldWidth + 2 * this.starSize);
            } else if (this.y[star] > (double)(this.fieldHeight + this.starSize)) {
               this.y[star] = this.y[star] - (double)(this.fieldHeight + 2 * this.starSize);
            }
         } else {
            this.x[star] = this.x[star] - this.velocityX[star] * steps;
            this.y[star] = this.y[star] - this.velocityY[star] * steps;
            // Bounce by heading back inside, so a star never sticks to an edge by flipping every frame.
            if (this.x[star] < 0) {
               this.velocityX[star] = Math.abs(this.velocityX[star]) * -1.0;
            } else if (this.x[star] > this.fieldWidth - this.starSize) {
               this.velocityX[star] = Math.abs(this.velocityX[star]);
            }
            if (this.y[star] > this.fieldHeight - this.starSize) {
               this.velocityY[star] = Math.abs(this.velocityY[star]);
            } else if (this.y[star] < 0) {
               this.velocityY[star] = Math.abs(this.velocityY[star]) * -1.0;
            }
         }
      }
   }

   /** Mode 5: overlapping stars average their velocities so they drift together. */
   private void flock() {
      for (int star = 0; star < this.starCount; star++) {
         for (int other = 0; other < this.starCount; other++) {
            if (this.x[star] < this.x[other]
               && this.x[star] + (double)this.starSize > this.x[other]
               && this.y[star] < this.y[other]
               && this.y[star] + (double)this.starSize > this.y[other]) {
               double averageVelocityX = (this.velocityX[star] + this.velocityX[other]) / 2.0;
               double averageVelocityY = (this.velocityY[star] + this.velocityY[other]) / 2.0;
               this.velocityX[star] = averageVelocityX;
               this.velocityX[other] = averageVelocityX;
               this.velocityY[star] = averageVelocityY;
               this.velocityY[other] = averageVelocityY;
            }
         }
      }
   }

   @Override
   public final void paint(Graphics g) {
      super.paint(g);
      Graphics2D g2 = (Graphics2D)g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(Color.blue);
      g2.fillRect(0, 0, this.getWidth(), this.getHeight());
      this.drawStars(g2);
   }

   /** Draws the stars alone, in the current mode's colors. */
   final void drawStars(Graphics2D g2) {
      for (int star = 0; star < this.starCount; star++) {
         this.setStarColor(g2, star);
         g2.fillOval((int)this.x[star], (int)this.y[star], this.starSize, this.starSize);
      }
   }

   private void setStarColor(Graphics g, int star) {
      if (this.mode == 0 || this.mode == 4) {
         g.setColor(Color.white);
      } else if (this.mode == 1) {
         int red = clampColor(this.x[star] / (double)this.fieldWidth * 250.0);
         int greenBlue = clampColor(this.y[star] / (double)this.fieldHeight * 250.0);
         g.setColor(new Color(red, greenBlue, greenBlue));
      } else if (this.mode == 2) {
         boolean right = this.velocityX[star] > 0;
         boolean up = this.velocityY[star] > 0;
         g.setColor(right && up ? new Color(255, 0, 0) : right ? new Color(0, 255, 0) : up ? new Color(0, 255, 255) : new Color(255, 0, 255));
      } else if (this.mode == 3) {
         g.setColor(new Color(this.randomColorValues[star * 3], this.randomColorValues[star * 3 + 1], this.randomColorValues[star * 3 + 2]));
      }
   }

   private static int clampColor(double value) {
      return Math.max(0, Math.min(255, (int)value));
   }
}
