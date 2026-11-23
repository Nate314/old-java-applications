package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The original screen saver: tiles appear one at a time at random grid positions until the grid is full, then
 * leave in reverse order, and it starts over in a new order. Green circles, green squares or bright circles.
 */
class Tiles implements Saver {
   static final int GREEN_CIRCLES = 0;
   static final int GREEN_SQUARES = 1;
   static final int BRIGHT_CIRCLES = 2;
   static final int MIN_SIZE = 10;
   /** One tile per step at normal speed, as the original's 250 ms timer. */
   static final double STEP_MILLIS = 250.0;
   /** A tile springs in over this long (saver time, so a fast speed makes it quicker), overshooting a little. */
   static final double IN_MILLIS = 420.0;
   /** A leaving tile shrinks away over this long instead of blinking out. */
   static final double OUT_MILLIS = 300.0;
   /** As a tile lands a faint ring spreads from it and fades over this long. */
   static final double RING_MILLIS = 700.0;

   private final int style;
   private double size;
   private int width;
   private int height;
   private Random random;
   private final List<Point> positions = new ArrayList<>();
   private final List<Color> colors = new ArrayList<>();
   /** Tile indexes in the random order they appear; the first revealedCount are on screen. */
   private final List<Integer> order = new ArrayList<>();
   private int revealedCount;
   private boolean adding;
   private double waited;
   /** Play (T): the pointer, which pushes the tiles near it aside (harder while pressed), or null. */
   private Point pointer;
   private boolean pressed;
   /** Saver time, and when each order slot last came or went on it, for the springing in and shrinking away. */
   private double clock;
   private double[] changed = new double[0];

   Tiles(int style) {
      this.style = style;
   }

   @Override
   public String name() {
      return this.style == GREEN_CIRCLES ? "Green circles" : this.style == GREEN_SQUARES ? "Green squares" : "Bright circles";
   }

   static int gcf(int a, int b) {
      return b == 0 ? a : gcf(b, a % b);
   }

   /**
    * The original used the greatest common factor of the screen's sides (120 px on 1920x1080), which is a pixel or
    * two on many other screens. That is kept when it gives between 4 and 12 rows; otherwise about 9 rows.
    */
   static double autoSize(int width, int height) {
      int gcf = gcf(width, height);
      if (gcf >= height / 12.0 && gcf <= height / 4.0) {
         return gcf;
      }
      return Math.max(MIN_SIZE, Math.round(height / 9.0));
   }

   double size() {
      return this.size;
   }

   /** Sets the tile size (the Up and Down keys, or the saved size) and, once on a screen, starts a new cycle. */
   void setSize(double size) {
      this.size = Math.max(MIN_SIZE, size);
      if (this.random != null) {
         this.size = Math.max(MIN_SIZE, Math.min(Math.max(this.width, this.height), this.size));
         this.buildGrid();
      }
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.width = width;
      this.height = height;
      this.random = random;
      this.pointer = null;
      if (this.size <= 0) {
         this.size = autoSize(width, height);
      }
      this.buildGrid();
   }

   /** Lays out tiles of the current size to cover the screen, centered, and starts a new reveal cycle. */
   private void buildGrid() {
      int s = (int)this.size;
      int columns = (this.width + s - 1) / s;
      int rows = (this.height + s - 1) / s;
      int left = (this.width - columns * s) / 2;
      int top = (this.height - rows * s) / 2;
      this.positions.clear();
      this.colors.clear();
      this.order.clear();
      for (int c = 0; c < columns; c++) {
         for (int r = 0; r < rows; r++) {
            this.order.add(this.positions.size());
            this.positions.add(new Point(left + c * s, top + r * s));
            if (this.style == BRIGHT_CIRCLES) {
               this.colors.add(Color.getHSBColor(this.random.nextFloat(), 0.8F, 1.0F));
            } else {
               int shade = this.random.nextInt(255);
               this.colors.add(new Color(shade, 255, shade));
            }
         }
      }
      Collections.shuffle(this.order, this.random);
      this.changed = new double[this.order.size()];
      Arrays.fill(this.changed, Double.NEGATIVE_INFINITY);
      this.revealedCount = 0;
      this.adding = true;
      this.waited = 0;
   }

   int tileCount() {
      return this.positions.size();
   }

   int revealedCount() {
      return this.revealedCount;
   }

   @Override
   public void tick(double millis) {
      this.clock += millis;
      this.waited += millis;
      // At high speed several tiles go in one frame; the cap keeps a long pause from replaying a whole cycle.
      int steps = 0;
      while (this.waited >= STEP_MILLIS && steps < 2000) {
         this.waited -= STEP_MILLIS;
         this.step(this.clock - this.waited);
         steps++;
      }
      if (steps == 2000) {
         this.waited = 0;
      }
   }

   /** Adds one tile until the grid is full, then removes them in reverse order, then starts over in a new order. */
   private void step(double at) {
      if (this.adding) {
         this.changed[this.revealedCount] = at;
         this.revealedCount++;
         this.chime(this.revealedCount - 1, 0.55);
         if (this.revealedCount >= this.order.size()) {
            this.adding = false;
         }
      } else {
         this.revealedCount--;
         this.changed[this.revealedCount] = at;
         this.chime(this.revealedCount, 0.3);
         if (this.revealedCount <= 0) {
            this.adding = true;
            Collections.shuffle(this.order, this.random);
            // The slots now sit elsewhere: the last one goes at once rather than shrinking in a new place.
            Arrays.fill(this.changed, Double.NEGATIVE_INFINITY);
         }
      }
   }

   /** Music (M): a tile coming or going rings a note, higher the nearer the top of the screen it sits. */
   private void chime(int index, double loudness) {
      Point position = this.positions.get(this.order.get(index));
      Music.ring(Music.BEADS, 1 - (position.y + this.size / 2) / this.height, loudness);
   }

   @Override
   public void touch(Point at, boolean down) {
      this.pointer = at;
      this.pressed = down;
   }

   /** How big tile slot i is drawn, 0 to about 1.1: springing in, full, shrinking away, or gone (0). */
   double scale(int i) {
      double age = this.clock - this.changed[i];
      if (i >= this.revealedCount) {
         double u = Math.min(1, age / OUT_MILLIS);
         return 1 - u * u;
      }
      double u = Math.min(1, age / IN_MILLIS);
      if (Main.calm()) {
         return u;
      }
      // Ease out with a little overshoot, so each tile lands with a bounce.
      double v = u - 1;
      return 1 + 2.70158 * v * v * v + 1.70158 * v * v;
   }

   @Override
   public void paint(Graphics2D g) {
      int s = (int)this.size;
      double radius = s * (this.pressed ? 4.5 : 3);
      double most = s * (this.pressed ? 1.6 : 0.9);
      boolean calm = Main.calm();
      for (int i = 0; i < this.order.size(); i++) {
         double grown = this.scale(i);
         if (grown <= 0) {
            continue;
         }
         Point position = this.positions.get(this.order.get(i));
         Color color = this.colors.get(i);
         double[] push = Saver.away(position.x + s / 2.0, position.y + s / 2.0, this.pointer, radius, most);
         // A pushed tile also shrinks a little, so neighbors pushed together do not pile up.
         int t = (int)Math.round(s * grown * (1 - 0.35 * Math.min(1, Math.hypot(push[0], push[1]) / s)));
         int x = (int)Math.round(position.x + push[0]) + (s - t) / 2;
         int y = (int)Math.round(position.y + push[1]) + (s - t) / 2;
         // The ring goes out as the tile lands, halfway through springing in.
         double ringAge = this.clock - this.changed[i] - IN_MILLIS / 2;
         if (!calm && i < this.revealedCount && ringAge >= 0 && ringAge < RING_MILLIS) {
            this.ring(g, color, position.x + s / 2.0 + push[0], position.y + s / 2.0 + push[1], s, ringAge / RING_MILLIS);
         }
         this.bead(g, color, x, y, t);
      }
   }

   /** One tile as a lit bead (or a bevelled square): its color, a darker rim toward the bottom right and a soft shine. */
   private void bead(Graphics2D g, Color color, int x, int y, int t) {
      boolean square = this.style == GREEN_SQUARES;
      if (t < 8) {
         g.setColor(color);
         if (square) {
            g.fillRect(x, y, t, t);
         } else {
            g.fillOval(x, y, t, t);
         }
         return;
      }
      int lit = (int)Math.round(t * 0.88);
      int in = Math.max(1, t / 30);
      g.setColor(color.darker());
      if (square) {
         g.fillRect(x, y, t, t);
         g.setColor(color);
         g.fillRect(x + in, y + in, lit, lit);
         g.setColor(new Color(255, 255, 255, 80));
         g.fillRect(x + t / 8, y + t / 10, t / 2, Math.max(2, t / 10));
      } else {
         g.fillOval(x, y, t, t);
         g.setColor(color);
         g.fillOval(x + in, y + in, lit, lit);
         g.setColor(new Color(255, 255, 255, 100));
         g.fillOval(x + t / 5, y + t / 7, t * 3 / 10, t / 5);
      }
   }

   /** The ring a landing tile sends out, u (0 to 1) of the way through: spreading to twice its size and fading. */
   private void ring(Graphics2D g, Color color, double cx, double cy, int s, double u) {
      int r = (int)Math.round(s * (0.45 + 0.55 * u));
      g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(90 * (1 - u) * (1 - u))));
      g.setStroke(new BasicStroke(Math.max(1, s / 30f)));
      int x = (int)Math.round(cx) - r;
      int y = (int)Math.round(cy) - r;
      if (this.style == GREEN_SQUARES) {
         g.drawRect(x, y, 2 * r, 2 * r);
      } else {
         g.drawOval(x, y, 2 * r, 2 * r);
      }
      g.setStroke(new BasicStroke(1));
   }
}
