package animate2;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Mode 6, Orbits: up to four suns the visitor places, and the stars falling round them. Each star starts on a
 * circle round a sun, at the speed that keeps it there, and from then on only gravity (and the pointer) moves it,
 * so a second sun, or a sun dragged across the field, bends every path. A star that falls into a sun, or flies far
 * away, starts again on a circle round one of them.
 */
final class Orbits {
   static final int MAX_SUNS = 4;
   /** The night behind the suns (the other modes keep the original blue). */
   static final Color SKY = new Color(4, 6, 28);
   /** A press let go within this long, and this near where it began, is a click: it places a sun. */
   static final double CLICK_SECONDS = 0.3;
   static final double CLICK_SLOP = 8.0;
   /** In space a push lasts, so bursts and flings here are this share of their strength in the other modes. */
   static final double BURST_SHARE = 0.35;
   /** A sun's pull: GM / r^2. GM grows with the cube of the field's size, so an orbit takes as long in any window. */
   private static final double GM_PER_400 = 125.0;
   /** Fastest a star may go, in px per step, so a close pass does not throw it across the field in one step. */
   private static final double MAX_SPEED = 8.0;
   private static final double GROW_SECONDS = 0.35;
   /** Star colors by distance from the nearest sun, as a share of the field's size: white hot to violet. */
   private static final Color[] HEAT = heat(24);

   private final StarField field;
   /** Suns oldest first, so a fifth takes the place of the oldest. */
   private final double[] sunX = new double[MAX_SUNS];
   private final double[] sunY = new double[MAX_SUNS];
   private final double[] bornAt = new double[MAX_SUNS];
   private int suns;
   private int grabbed = -1;
   private double grabDX;
   private double grabDY;
   private int swallowed;

   Orbits(StarField field) {
      this.field = field;
   }

   /** One sun in the middle of the field. */
   void reset() {
      this.suns = 0;
      this.grabbed = -1;
      this.swallowed = 0;
      this.addSun(this.field.fieldWidth() / 2.0, this.field.fieldHeight() / 2.0);
      this.bornAt[0] = Double.NEGATIVE_INFINITY;
   }

   int suns() {
      return this.suns;
   }

   double sunX(int sun) {
      return this.sunX[sun];
   }

   double sunY(int sun) {
      return this.sunY[sun];
   }

   /** Stars that have fallen into a sun since the mode started. */
   int swallowed() {
      return this.swallowed;
   }

   private double size() {
      return Math.max(100.0, Math.min(this.field.fieldWidth(), this.field.fieldHeight()));
   }

   private double gm() {
      double scale = this.size() / 400.0;
      return GM_PER_400 * scale * scale * scale;
   }

   /** Gravity is softened inside this distance, so a star passing through a sun's middle is not flung to infinity. */
   private double softening() {
      return this.size() / 50.0;
   }

   double sunRadius() {
      return Math.max(8.0, this.size() / 50.0);
   }

   /**
    * Puts a star on a circle round a sun: a fresh field spreads them from near the sun out to most of the way to the
    * edge, a star starting again goes to the outer part. With no suns it just drifts somewhere.
    */
   void place(int star, boolean fresh) {
      double size = this.size();
      if (this.suns == 0) {
         this.field.setStar(star, Math.random() * this.field.fieldWidth(), Math.random() * this.field.fieldHeight(),
            Math.random() - 0.5, Math.random() - 0.5);
         return;
      }
      int sun = (int)(Math.random() * this.suns);
      double r = size * (fresh ? 0.07 + 0.38 * Math.sqrt(Math.random()) : 0.25 + 0.2 * Math.random());
      double angle = Math.random() * 2.0 * Math.PI;
      double speed = this.circularSpeed(r) * (1.0 + 0.06 * (Math.random() - 0.5));
      this.field.setStar(star, this.sunX[sun] + r * Math.cos(angle), this.sunY[sun] + r * Math.sin(angle),
         speed * Math.sin(angle), -speed * Math.cos(angle));
   }

   /** The speed that keeps a star on a circle of radius r round a lone sun. */
   double circularSpeed(double r) {
      double soft2 = r * r + this.softening() * this.softening();
      return Math.sqrt(this.gm() * r * r / (soft2 * Math.sqrt(soft2)));
   }

   /** Moves every star on by {@code steps}, in whole steps or less, under the pull of every sun. */
   void move(double steps) {
      int count = Math.max(1, (int)Math.ceil(steps));
      double dt = steps / count;
      double gm = this.gm();
      double soft2 = this.softening() * this.softening();
      double swallow = this.sunRadius() * 0.7;
      double width = this.field.fieldWidth();
      double height = this.field.fieldHeight();
      double far = 1.5 * Math.max(width, height);
      for (int step = 0; step < count; step++) {
         for (int star = 0; star < this.field.starCount; star++) {
            double x = this.field.centerX(star);
            double y = this.field.centerY(star);
            double vx = this.field.velocityX(star);
            double vy = this.field.velocityY(star);
            boolean fell = false;
            for (int sun = 0; sun < this.suns; sun++) {
               double dx = this.sunX[sun] - x;
               double dy = this.sunY[sun] - y;
               double d2 = dx * dx + dy * dy;
               if (d2 < swallow * swallow) {
                  fell = true;
                  break;
               }
               double s2 = d2 + soft2;
               double pull = gm / (s2 * Math.sqrt(s2)) * dt;
               vx += dx * pull;
               vy += dy * pull;
            }
            if (fell) {
               this.swallowed++;
               this.place(star, false);
               continue;
            }
            double speed = Math.hypot(vx, vy);
            if (speed > MAX_SPEED) {
               vx *= MAX_SPEED / speed;
               vy *= MAX_SPEED / speed;
            }
            x += vx * dt;
            y += vy * dt;
            if (this.suns == 0) { // nothing to hold them: they drift and wrap round, as in Drift
               x = x < 0 ? x + width : x >= width ? x - width : x;
               y = y < 0 ? y + height : y >= height ? y - height : y;
            } else if (Math.hypot(x - width / 2.0, y - height / 2.0) > far) {
               this.place(star, false);
               continue;
            }
            this.field.setStar(star, x, y, vx, vy);
         }
      }
   }

   /** A click: a new sun there, growing in. With four already, the oldest goes. */
   void addSun(double x, double y) {
      if (this.suns == MAX_SUNS) {
         this.removeSun(0);
      }
      this.sunX[this.suns] = x;
      this.sunY[this.suns] = y;
      this.bornAt[this.suns] = this.field.clock();
      this.suns++;
   }

   private void removeSun(int sun) {
      for (int i = sun; i < this.suns - 1; i++) {
         this.sunX[i] = this.sunX[i + 1];
         this.sunY[i] = this.sunY[i + 1];
         this.bornAt[i] = this.bornAt[i + 1];
      }
      this.suns--;
      if (this.grabbed == sun) {
         this.grabbed = -1;
      } else if (this.grabbed > sun) {
         this.grabbed--;
      }
   }

   /** The sun under (x, y), with a little room round it for a finger, or -1. */
   int sunAt(double x, double y) {
      int best = -1;
      double bestDistance = this.sunRadius() + 12.0;
      for (int sun = 0; sun < this.suns; sun++) {
         double distance = Math.hypot(x - this.sunX[sun], y - this.sunY[sun]);
         if (distance < bestDistance) {
            best = sun;
            bestDistance = distance;
         }
      }
      return best;
   }

   /** Pressed on a sun: it follows the pointer until let go. False when there is no sun there. */
   boolean grab(double x, double y) {
      int sun = this.sunAt(x, y);
      if (sun < 0) {
         return false;
      }
      this.grabbed = sun;
      this.grabDX = x - this.sunX[sun];
      this.grabDY = y - this.sunY[sun];
      return true;
   }

   boolean holding() {
      return this.grabbed >= 0;
   }

   void dragTo(double x, double y) {
      if (this.grabbed >= 0) {
         this.sunX[this.grabbed] = x - this.grabDX;
         this.sunY[this.grabbed] = y - this.grabDY;
      }
   }

   /** Let go of the sun held: it stays where it is, unless it was dragged to (or past) the edge, which takes it away. */
   void drop() {
      if (this.grabbed >= 0) {
         int sun = this.grabbed;
         this.grabbed = -1;
         if (this.atEdge(sun)) {
            this.removeSun(sun);
         }
      }
   }

   /** Right click on a sun (on the desktop): takes it away. */
   boolean removeAt(double x, double y) {
      int sun = this.sunAt(x, y);
      if (sun >= 0) {
         this.removeSun(sun);
      }
      return sun >= 0;
   }

   private boolean atEdge(int sun) {
      double r = this.sunRadius();
      return this.sunX[sun] < r || this.sunY[sun] < r
         || this.sunX[sun] > this.field.fieldWidth() - r || this.sunY[sun] > this.field.fieldHeight() - r;
   }

   void drawStars(Graphics2D g2) {
      int drawn = Math.max(2, this.field.starSize() / 3);
      double size = this.size();
      for (int star = 0; star < this.field.starCount; star++) {
         double x = this.field.centerX(star);
         double y = this.field.centerY(star);
         double nearest = Double.MAX_VALUE;
         for (int sun = 0; sun < this.suns; sun++) {
            nearest = Math.min(nearest, Math.hypot(x - this.sunX[sun], y - this.sunY[sun]));
         }
         double share = this.suns == 0 ? 0.3 : nearest / size / 0.5;
         g2.setColor(HEAT[(int)Math.max(0, Math.min(HEAT.length - 1, share * (HEAT.length - 1)))]);
         g2.fillOval((int)(x - drawn / 2.0), (int)(y - drawn / 2.0), drawn, drawn);
      }
   }

   /** Each sun as a glowing disk; the one held is ringed, and turns red where letting go would take it away. */
   void drawSuns(Graphics2D g2) {
      double clock = this.field.clock();
      for (int sun = 0; sun < this.suns; sun++) {
         double grown = Math.min(1.0, (clock - this.bornAt[sun]) / GROW_SECONDS);
         double pulse = 1.0 + 0.06 * Math.sin(clock * 3.0 + sun * 1.7);
         double r = this.sunRadius() * grown * pulse;
         int x = (int)this.sunX[sun];
         int y = (int)this.sunY[sun];
         boolean leaving = sun == this.grabbed && this.atEdge(sun);
         Color glow = leaving ? new Color(255, 80, 60, 26) : new Color(255, 190, 80, 26);
         g2.setColor(glow);
         for (int ring = 4; ring >= 1; ring--) {
            int rr = (int)(r * (1.0 + 0.8 * ring));
            g2.fillOval(x - rr, y - rr, 2 * rr, 2 * rr);
         }
         int core = (int)r;
         g2.setColor(leaving ? new Color(255, 110, 90) : new Color(255, 214, 120));
         g2.fillOval(x - core, y - core, 2 * core, 2 * core);
         int hot = (int)(r * 0.6);
         g2.setColor(leaving ? new Color(255, 200, 190) : new Color(255, 250, 225));
         g2.fillOval(x - hot, y - hot, 2 * hot, 2 * hot);
         if (sun == this.grabbed) {
            int ring = (int)(r + 7);
            g2.setColor(leaving ? new Color(255, 120, 100, 230) : new Color(255, 255, 255, 200));
            g2.drawOval(x - ring, y - ring, 2 * ring, 2 * ring);
         }
      }
   }

   /** White hot near a sun, through blue-white, gold and orange, to rose and violet far out. */
   private static Color[] heat(int count) {
      int[][] stops = {{255, 255, 255}, {170, 215, 255}, {255, 232, 150}, {255, 150, 60}, {235, 80, 110}, {150, 70, 210}};
      Color[] colors = new Color[count];
      for (int i = 0; i < count; i++) {
         double at = (double)i / (count - 1) * (stops.length - 1);
         int low = Math.min(stops.length - 2, (int)at);
         double t = at - low;
         colors[i] = new Color(
            (int)(stops[low][0] + (stops[low + 1][0] - stops[low][0]) * t),
            (int)(stops[low][1] + (stops[low + 1][1] - stops[low][1]) * t),
            (int)(stops[low][2] + (stops[low + 1][2] - stops[low][2]) * t));
      }
      return colors;
   }
}
