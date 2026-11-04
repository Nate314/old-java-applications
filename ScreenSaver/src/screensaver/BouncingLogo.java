package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * A logo drifts diagonally and bounces off the edges, changing color on every bounce. When it bounces off two edges
 * at once it has hit a corner exactly, and the corner bursts into sparks.
 */
class BouncingLogo implements Saver {
   static final String TEXT = "OLD JAVA";
   /** Two bounces this close together, in milliseconds at normal speed, count as one corner hit. */
   static final double CORNER_MILLIS = 120;
   private static final int SPARKS = 48;
   private static final double SPARK_MILLIS = 1600;

   private int width;
   private int height;
   private Random random;
   private Font font;
   /** The logo's size, known once it has been measured on the first paint. */
   private int boxWidth;
   private int boxHeight;
   private double x;
   private double y;
   private double dx;
   private double dy;
   private float hue;
   private double sinceBounceX = Double.MAX_VALUE;
   private double sinceBounceY = Double.MAX_VALUE;
   private int corners;
   private final double[] sparkX = new double[SPARKS];
   private final double[] sparkY = new double[SPARKS];
   private final double[] sparkDx = new double[SPARKS];
   private final double[] sparkDy = new double[SPARKS];
   private final Color[] sparkColor = new Color[SPARKS];
   private double sparkAge = SPARK_MILLIS;

   @Override
   public String name() {
      return "Bouncing logo";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.width = width;
      this.height = height;
      this.random = random;
      this.font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(16, Math.min(width, height) / 11));
      this.boxWidth = 0;
      this.boxHeight = 0;
      double speed = Math.max(width, height) / 9000.0;
      this.dx = speed * (random.nextBoolean() ? 1 : -1);
      this.dy = speed * 0.8 * (random.nextBoolean() ? 1 : -1);
      this.hue = random.nextFloat();
      this.sinceBounceX = Double.MAX_VALUE;
      this.sinceBounceY = Double.MAX_VALUE;
      this.sparkAge = SPARK_MILLIS;
   }

   /** Sets the logo's size and position directly (the self-check aims it at a corner). */
   void place(int boxWidth, int boxHeight, double x, double y, double dx, double dy) {
      this.boxWidth = boxWidth;
      this.boxHeight = boxHeight;
      this.x = x;
      this.y = y;
      this.dx = dx;
      this.dy = dy;
   }

   int corners() {
      return this.corners;
   }

   @Override
   public void tick(double millis) {
      this.sparkAge += millis;
      for (int i = 0; i < SPARKS; i++) {
         this.sparkX[i] += this.sparkDx[i] * millis;
         this.sparkY[i] += this.sparkDy[i] * millis;
         this.sparkDy[i] += 0.0004 * millis;
      }
      if (this.boxWidth == 0) {
         return;
      }
      this.sinceBounceX += millis;
      this.sinceBounceY += millis;
      this.x += this.dx * millis;
      this.y += this.dy * millis;
      boolean bounced = false;
      if (this.x < 0 || this.x + this.boxWidth > this.width) {
         this.dx = this.x < 0 ? Math.abs(this.dx) : -Math.abs(this.dx);
         this.x = Math.max(0, Math.min(this.width - this.boxWidth, this.x));
         this.sinceBounceX = 0;
         bounced = true;
      }
      if (this.y < 0 || this.y + this.boxHeight > this.height) {
         this.dy = this.y < 0 ? Math.abs(this.dy) : -Math.abs(this.dy);
         this.y = Math.max(0, Math.min(this.height - this.boxHeight, this.y));
         this.sinceBounceY = 0;
         bounced = true;
      }
      if (bounced) {
         this.hue = (this.hue + 0.17F + this.random.nextFloat() * 0.5F) % 1F;
         if (this.sinceBounceX < CORNER_MILLIS && this.sinceBounceY < CORNER_MILLIS) {
            this.corners++;
            this.burst();
            // One corner hit, not two: forget both bounces.
            this.sinceBounceX = Double.MAX_VALUE;
            this.sinceBounceY = Double.MAX_VALUE;
         }
      }
   }

   /** Sparks fly out of the corner the logo just hit. */
   private void burst() {
      double cornerX = this.dx > 0 ? 0 : this.width;
      double cornerY = this.dy > 0 ? 0 : this.height;
      double speed = Math.max(this.width, this.height) / 2500.0;
      for (int i = 0; i < SPARKS; i++) {
         double angle = Math.atan2(this.dy, this.dx) + (this.random.nextDouble() - 0.5) * 1.6;
         double v = speed * (0.3 + this.random.nextDouble());
         this.sparkX[i] = cornerX;
         this.sparkY[i] = cornerY;
         this.sparkDx[i] = Math.cos(angle) * v;
         this.sparkDy[i] = Math.sin(angle) * v;
         this.sparkColor[i] = Color.getHSBColor(this.random.nextFloat(), 0.7F, 1F);
      }
      this.sparkAge = 0;
   }

   @Override
   public void paint(Graphics2D g) {
      g.setFont(this.font);
      FontMetrics metrics = g.getFontMetrics();
      if (this.boxWidth == 0) {
         int pad = this.font.getSize() / 2;
         this.boxWidth = metrics.stringWidth(TEXT) + pad * 2;
         this.boxHeight = metrics.getAscent() + metrics.getDescent() + pad;
         this.x = this.random.nextDouble() * Math.max(1, this.width - this.boxWidth);
         this.y = this.random.nextDouble() * Math.max(1, this.height - this.boxHeight);
      }
      int bx = (int)this.x;
      int by = (int)this.y;
      int arc = this.boxHeight / 2;
      Color color = Color.getHSBColor(this.hue, 0.8F, 1F);
      g.setColor(color.darker().darker());
      g.fillRoundRect(bx, by, this.boxWidth, this.boxHeight, arc, arc);
      g.setStroke(new BasicStroke(Math.max(2F, this.boxHeight / 16F)));
      g.setColor(color);
      g.drawRoundRect(bx, by, this.boxWidth, this.boxHeight, arc, arc);
      int textX = bx + (this.boxWidth - metrics.stringWidth(TEXT)) / 2;
      int textY = by + (this.boxHeight + metrics.getAscent() - metrics.getDescent()) / 2;
      g.drawString(TEXT, textX, textY);
      g.setStroke(new BasicStroke(1));

      if (this.sparkAge < SPARK_MILLIS) {
         double life = 1 - this.sparkAge / SPARK_MILLIS;
         int size = Math.max(3, (int)(this.font.getSize() / 5 * life) + 2);
         for (int i = 0; i < SPARKS; i++) {
            Color c = this.sparkColor[i];
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(255 * life)));
            g.fillOval((int)this.sparkX[i] - size / 2, (int)this.sparkY[i] - size / 2, size, size);
         }
         String message = this.corners == 1 ? "Corner!" : "Corner! That makes " + this.corners;
         g.setColor(new Color(255, 255, 255, (int)(255 * life)));
         g.drawString(message, (this.width - metrics.stringWidth(message)) / 2, this.height / 2);
      }
   }
}
