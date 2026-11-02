package screensaver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
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
      this.waited += millis;
      // At high speed several tiles go in one frame; the cap keeps a long pause from replaying a whole cycle.
      int steps = 0;
      while (this.waited >= STEP_MILLIS && steps < 2000) {
         this.waited -= STEP_MILLIS;
         this.step();
         steps++;
      }
      if (steps == 2000) {
         this.waited = 0;
      }
   }

   /** Adds one tile until the grid is full, then removes them in reverse order, then starts over in a new order. */
   private void step() {
      if (this.adding) {
         this.revealedCount++;
         if (this.revealedCount >= this.order.size()) {
            this.adding = false;
         }
      } else {
         this.revealedCount--;
         if (this.revealedCount <= 0) {
            this.adding = true;
            Collections.shuffle(this.order, this.random);
         }
      }
   }

   @Override
   public void paint(Graphics2D g) {
      int s = (int)this.size;
      for (int i = 0; i < this.revealedCount; i++) {
         Point position = this.positions.get(this.order.get(i));
         g.setColor(this.colors.get(i));
         if (this.style == GREEN_SQUARES) {
            g.fillRect(position.x, position.y, s, s);
         } else {
            g.fillOval(position.x, position.y, s, s);
         }
      }
   }
}
