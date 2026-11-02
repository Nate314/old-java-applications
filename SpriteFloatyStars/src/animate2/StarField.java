package animate2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import javax.swing.Timer;

public final class StarField extends Component {
   /** Movement per star per step was tuned for an uncapped repaint loop; this many steps per second keeps a similar pace. */
   private static final double STEPS_PER_SECOND = 200.0;
   /** The five modes, by the names Star Catcher gives the nights flown over them. */
   static final String[] MODE_NAMES = {"Drift", "Burst", "Compass", "Confetti", "Flock"};
   /** Held down, the pointer pulls every star toward it this hard (px per step, per step). */
   private static final double PULL = 0.03;
   /** A kick from the pointer keeps this much of itself each step, so a flung star glides back to its own drift. */
   private static final double KICK_KEPT_PER_STEP = 0.985;
   /** While the pull lasts, kicks die away more slowly, so the stars swing through the pointer and swarm round it. */
   private static final double KICK_KEPT_PULLING = 0.996;
   /** A pointer resting on the field nudges the stars within this many px out of its way. */
   private static final double WAKE_RADIUS = 60.0;
   private static final double WAKE_PUSH = 0.08;
   /** A plain click bursts the stars near it this hard; letting go after holding FLING_FULL_SECONDS, FLING_MAX. */
   static final double BURST = 2.5;
   private static final double FLING_MAX = 6.0;
   private static final double FLING_FULL_SECONDS = 1.5;
   private static final double HINT_SECONDS = 9.0;
   private static final double BADGE_SECONDS = 1.6;
   private static final double RING_SECONDS = 0.5;
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
   /** Each star's push from the pointer, on top of its own velocity, in px per step; it dies away by itself. */
   private double[] kickX;
   private double[] kickY;
   private int mode = 2;
   private long lastTickNanos = System.nanoTime();
   /** Seconds the field has moved for; the pull, the hint, the mode badge and the burst ring are timed by it. */
   private double clock;
   private boolean pulling;
   private double pullStart;
   private boolean pointerOn;
   private double pointerX;
   private double pointerY;
   private boolean hintShown;
   private double hintShownAt;
   private double badgeAt = Double.NEGATIVE_INFINITY;
   private double ringAt = Double.NEGATIVE_INFINITY;
   private double ringX;
   private double ringY;
   private double ringRadius;

   public StarField(int mode, Dimension size, int starCount, int starSize) {
      this(mode, size, starCount, starSize, true);
   }

   /**
    * With {@code animate} false nothing moves until {@link #advance} is called and the pointer is not listened to
    * (Star Catcher draws it as its sky).
    */
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
      this.kickX = new double[starCount];
      this.kickY = new double[starCount];

      for (int star = 0; star < this.starCount; star++) {
         this.initStar(star);
      }

      if (animate) {
         this.hintShown = true;
         this.badgeAt = 0.0;
         new StarHand(this);
         new Timer(15, event -> this.tick()).start();
      }
   }

   private void initStar(int star) {
      this.kickX[star] = 0.0;
      this.kickY[star] = 0.0;
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
         this.kickX = Arrays.copyOf(this.kickX, capacity);
         this.kickY = Arrays.copyOf(this.kickY, capacity);
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

   /** Keys 1 to 5 and N: the stars start over in another mode (0 to 4), sized to the window as it is now. */
   final void setMode(int mode) {
      this.mode = Math.floorMod(mode, MODE_NAMES.length);
      this.startSize = new Dimension(this.fieldWidth, this.fieldHeight);
      for (int star = 0; star < this.starCount; star++) {
         this.initStar(star);
      }
      this.badgeAt = this.clock;
   }

   final int mode() {
      return this.mode;
   }

   /** H: shows or hides the two lines of controls along the bottom (they also fade by themselves). */
   final void toggleHint() {
      this.hintShown = !this.hintVisible();
      this.hintShownAt = this.clock;
   }

   final boolean hintVisible() {
      return this.hintShown && this.clock - this.hintShownAt < HINT_SECONDS;
   }

   /** The pointer is over the field at (x, y), or has left it. */
   final void pointer(double x, double y, boolean on) {
      this.pointerX = x;
      this.pointerY = y;
      this.pointerOn = on;
   }

   /** Pressed: from now on every star is drawn toward the pointer. */
   final void startPull(double x, double y) {
      this.pointer(x, y, true);
      this.pulling = true;
      this.pullStart = this.clock;
   }

   /** Let go: the stars near the pointer burst away, harder the longer they were gathered. */
   final void release(double x, double y) {
      if (!this.pulling) {
         return;
      }
      this.pulling = false;
      this.pointer(x, y, this.pointerOn);
      this.burst(x, y, this.flingStrength());
   }

   /** How hard letting go now would burst the stars: from a click's BURST up to FLING_MAX after a long hold. */
   final double flingStrength() {
      return BURST + (FLING_MAX - BURST) * this.held();
   }

   final boolean pulling() {
      return this.pulling;
   }

   /** How far the pull has charged the fling, 0 to 1. */
   private double held() {
      return Math.min(1.0, (this.clock - this.pullStart) / FLING_FULL_SECONDS);
   }

   /** Pushes the stars within reach of (x, y) straight away from it, hardest at the middle. */
   final void burst(double x, double y, double strength) {
      double reach = Math.max(200.0, 0.4 * Math.min(this.fieldWidth, this.fieldHeight));
      for (int star = 0; star < this.starCount; star++) {
         double dx = this.centerX(star) - x;
         double dy = this.centerY(star) - y;
         double distance = Math.hypot(dx, dy);
         if (distance >= reach) {
            continue;
         }
         if (distance < 0.5) { // right under the pointer: any way out
            double angle = Math.random() * 2.0 * Math.PI;
            dx = Math.cos(angle);
            dy = Math.sin(angle);
            distance = 1.0;
         }
         double push = strength * (1.0 - distance / reach);
         this.kickX[star] += dx / distance * push;
         this.kickY[star] += dy / distance * push;
      }
      this.ringAt = this.clock;
      this.ringX = x;
      this.ringY = y;
      this.ringRadius = reach;
   }

   /** Space: a burst from the middle of the field. */
   final void burstCenter() {
      this.burst(this.fieldWidth / 2.0, this.fieldHeight / 2.0, BURST + 1.5);
   }

   final double centerX(int star) {
      return this.x[star] + this.starSize / 2.0;
   }

   final double centerY(int star) {
      return this.y[star] + this.starSize / 2.0;
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
      this.clock += steps / STEPS_PER_SECOND;
      if (this.mode == 4) {
         this.flock();
      }

      this.pushByPointer(steps);
      for (int star = 0; star < this.starCount; star++) {
         if (this.mode == 0 || this.mode == 4) {
            this.x[star] = this.x[star] - this.velocityX[star] * steps;
            this.y[star] = this.y[star] + this.velocityY[star] * steps;
            // The drift only goes left and down; a kick can carry a star off any edge, and it wraps there too.
            if (this.x[star] < (double)(0 - this.starSize)) {
               this.x[star] = this.x[star] + (double)(this.fieldWidth + 2 * this.starSize);
            } else if (this.x[star] > (double)(this.fieldWidth + this.starSize)) {
               this.x[star] = this.x[star] - (double)(this.fieldWidth + 2 * this.starSize);
            }
            if (this.y[star] > (double)(this.fieldHeight + this.starSize)) {
               this.y[star] = this.y[star] - (double)(this.fieldHeight + 2 * this.starSize);
            } else if (this.y[star] < (double)(0 - this.starSize)) {
               this.y[star] = this.y[star] + (double)(this.fieldHeight + 2 * this.starSize);
            }
         } else {
            this.x[star] = this.x[star] - this.velocityX[star] * steps;
            this.y[star] = this.y[star] - this.velocityY[star] * steps;
            // Bounce by heading back inside, so a star never sticks to an edge by flipping every frame.
            // A kick bounces off the edges too, losing some of itself.
            if (this.x[star] < 0) {
               this.velocityX[star] = Math.abs(this.velocityX[star]) * -1.0;
               this.kickX[star] = Math.abs(this.kickX[star]) * 0.7;
            } else if (this.x[star] > this.fieldWidth - this.starSize) {
               this.velocityX[star] = Math.abs(this.velocityX[star]);
               this.kickX[star] = Math.abs(this.kickX[star]) * -0.7;
            }
            if (this.y[star] > this.fieldHeight - this.starSize) {
               this.velocityY[star] = Math.abs(this.velocityY[star]);
               this.kickY[star] = Math.abs(this.kickY[star]) * -0.7;
            } else if (this.y[star] < 0) {
               this.velocityY[star] = Math.abs(this.velocityY[star]) * -1.0;
               this.kickY[star] = Math.abs(this.kickY[star]) * 0.7;
            }
         }
      }
   }

   /**
    * Held down, the pointer pulls every star toward it, and they swarm round it still jiggling with their own drift;
    * resting on the field, it nudges the nearest ones aside. Then each kick moves its star and dies away a little.
    */
   private void pushByPointer(double steps) {
      double kept = Math.pow(this.pulling ? KICK_KEPT_PULLING : KICK_KEPT_PER_STEP, steps);
      for (int star = 0; star < this.starCount; star++) {
         if (this.pulling || this.pointerOn) {
            double dx = this.pointerX - this.centerX(star);
            double dy = this.pointerY - this.centerY(star);
            double distance = Math.hypot(dx, dy);
            if (this.pulling) {
               if (distance > 1.0) {
                  this.kickX[star] += dx / distance * PULL * steps;
                  this.kickY[star] += dy / distance * PULL * steps;
               }
            } else if (distance < WAKE_RADIUS && distance > 0.5) {
               double push = WAKE_PUSH * (1.0 - distance / WAKE_RADIUS) * steps;
               this.kickX[star] -= dx / distance * push;
               this.kickY[star] -= dy / distance * push;
            }
         }
         this.kickX[star] *= kept;
         this.kickY[star] *= kept;
         this.x[star] += this.kickX[star] * steps;
         this.y[star] += this.kickY[star] * steps;
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
      this.drawHand(g2);
   }

   /** Draws the stars alone, in the current mode's colors. */
   final void drawStars(Graphics2D g2) {
      for (int star = 0; star < this.starCount; star++) {
         this.setStarColor(g2, star);
         g2.fillOval((int)this.x[star], (int)this.y[star], this.starSize, this.starSize);
      }
   }

   /** The pull's glow round the pointer, the ring a burst sends out, the mode's name after a change, and the hint. */
   private void drawHand(Graphics2D g2) {
      if (this.pulling) {
         double pulse = 0.5 + 0.5 * Math.sin(this.clock * 9.0);
         int r = (int)(10 + 16 * this.held() + 4 * pulse);
         int px = (int)this.pointerX;
         int py = (int)this.pointerY;
         g2.setColor(new Color(255, 255, 255, 30 + (int)(40 * this.held())));
         g2.fillOval(px - r, py - r, 2 * r, 2 * r);
         g2.setColor(new Color(255, 230, 140, 220));
         g2.drawOval(px - r, py - r, 2 * r, 2 * r);
      }
      double ringAge = (this.clock - this.ringAt) / RING_SECONDS;
      if (ringAge >= 0.0 && ringAge < 1.0) {
         int r = (int)(this.ringRadius * Math.sqrt(ringAge));
         g2.setColor(new Color(255, 255, 255, (int)(180 * (1.0 - ringAge))));
         g2.drawOval((int)this.ringX - r, (int)this.ringY - r, 2 * r, 2 * r);
      }
      double badgeAge = this.clock - this.badgeAt;
      if (badgeAge >= 0.0 && badgeAge < BADGE_SECONDS) {
         g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, this.textSize() * 3 / 2));
         this.tag(g2, "Mode " + (this.mode + 1) + ": " + MODE_NAMES[this.mode], 14, 14, fade(BADGE_SECONDS - badgeAge));
      }
      if (this.hintVisible()) {
         int alpha = fade(HINT_SECONDS - (this.clock - this.hintShownAt));
         g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, this.textSize()));
         int lineHeight = g2.getFontMetrics().getHeight() + 10;
         String[] lines = {"Hold to pull the stars in, let go to fling them. Click to burst.",
                           "1-5 or N: mode    Space: burst    Up/Down: stars    H: help"};
         int top = this.getHeight() - 8 - lines.length * lineHeight;
         for (String line : lines) {
            this.tag(g2, line, 14, top, alpha);
            top += lineHeight;
         }
      }
   }

   /** Text grows with the window, so it stays readable where the browser shows a big window scaled down. */
   private int textSize() {
      return Math.max(14, this.getWidth() / 38);
   }

   /** Full strength until the last 0.4 s of {@code secondsLeft}, then fading out. */
   private static int fade(double secondsLeft) {
      return (int)(255 * Math.max(0.0, Math.min(1.0, secondsLeft / 0.4)));
   }

   /** Text on a dark rounded tag whose top left corner is (x, y). */
   private void tag(Graphics2D g2, String text, int x, int y, int alpha) {
      FontMetrics metrics = g2.getFontMetrics();
      g2.setColor(new Color(0, 0, 40, alpha * 3 / 4));
      g2.fillRoundRect(x, y, metrics.stringWidth(text) + 16, metrics.getHeight() + 6, 10, 10);
      g2.setColor(new Color(255, 255, 255, alpha));
      g2.drawString(text, x + 8, y + 3 + metrics.getAscent());
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
