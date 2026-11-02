package circlewindow;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Turns a shape into the rectangles of the windows that draw it, in the order they should appear. */
final class Shapes {
   static final String CIRCLE = "Circle";
   static final String RING = "Ring";
   static final String HEART = "Heart";
   static final String STAR = "Star";
   static final String WORD = "Word";
   static final String[] ALL = {CIRCLE, RING, HEART, STAR, WORD};
   /** Grid cells across a ring, heart or star: fine enough to look smooth, coarse enough to stay a few dozen windows. */
   private static final int CELLS = 28;
   static final int MAX_WORD_LENGTH = 12;

   private Shapes() {
   }

   /**
    * The windows for a shape about size pixels across, centered on (centerX, centerY). A word is size pixels
    * tall, or less if that would make it wider than maxWidth.
    */
   static List<Rectangle> build(String shape, int size, String word, int centerX, int centerY, int maxWidth) {
      if (CIRCLE.equals(shape)) {
         return circle(size, centerX, centerY);
      }
      if (WORD.equals(shape)) {
         return word(cleanWord(word), size, centerX, centerY, maxWidth);
      }
      int cell = Math.max(2, size / CELLS);
      int cells = Math.max(1, size / cell);
      boolean[][] grid = new boolean[cells][cells];
      for (int row = 0; row < cells; row++) {
         for (int column = 0; column < cells; column++) {
            double u = (column + 0.5) / cells * 2.0 - 1.0;
            double v = (row + 0.5) / cells * 2.0 - 1.0;
            grid[row][column] = inside(shape, u, v);
         }
      }
      int left = centerX - cells * cell / 2;
      int top = centerY - cells * cell / 2;
      List<Rectangle> pieces = merge(grid, cell, left, top);
      // Grow from the middle outwards.
      pieces.sort(Comparator.comparingDouble(r -> Math.hypot(r.x + r.width / 2.0 - centerX, r.y + r.height / 2.0 - centerY)));
      return pieces;
   }

   /** The word in capitals with the characters the font cannot draw left out, at most MAX_WORD_LENGTH long. */
   static String cleanWord(String word) {
      StringBuilder clean = new StringBuilder();
      for (char c : word.trim().toUpperCase().toCharArray()) {
         if (PixelFont.has(c) && clean.length() < MAX_WORD_LENGTH) {
            clean.append(c);
         }
      }
      return clean.toString();
   }

   /**
    * The original Circle Spawner: slice i is a rectangle whose corners sit on the circle, half-width i and
    * half-height sqrt(r^2 - i^2). The overlapping slices together fill the disk.
    */
   private static List<Rectangle> circle(int diameter, int centerX, int centerY) {
      List<Rectangle> slices = new ArrayList<>();
      double radius = diameter / 2.0;
      for (int sliceIndex = 1; sliceIndex <= (int)radius; sliceIndex++) {
         double sliceWidth = 2.0 * sliceIndex;
         double sliceHeight = 2.0 * Math.sqrt(radius * radius - (double)sliceIndex * sliceIndex);
         if (sliceHeight < 1.0) {
            continue;
         }
         slices.add(new Rectangle((int)(centerX - sliceWidth / 2.0), (int)(centerY - sliceHeight / 2.0), (int)sliceWidth, (int)sliceHeight));
      }
      return slices;
   }

   /** Whether the point (u, v), each from -1 to 1 with v pointing down, is inside the shape. */
   private static boolean inside(String shape, double u, double v) {
      if (RING.equals(shape)) {
         double r = Math.hypot(u, v);
         return r <= 1.0 && r >= 0.55;
      }
      if (HEART.equals(shape)) {
         // (x^2 + y^2 - 1)^3 - x^2 y^3 <= 0, which spans about x -1.14..1.14 and y -1..1.25.
         double x = u * 1.2;
         double y = 1.3 - (v + 1.0) * 1.15;
         double a = x * x + y * y - 1.0;
         return a * a * a - x * x * y * y * y <= 0.0;
      }
      if (STAR.equals(shape)) {
         return inStar(u, v + 0.08);
      }
      return false;
   }

   /** A five-pointed star with its top point up, outer radius 1 and inner radius 0.42. */
   private static boolean inStar(double u, double v) {
      double[] xs = new double[10];
      double[] ys = new double[10];
      for (int i = 0; i < 10; i++) {
         double r = i % 2 == 0 ? 1.0 : 0.42;
         double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
         xs[i] = r * Math.cos(angle);
         ys[i] = r * Math.sin(angle);
      }
      boolean in = false;
      for (int i = 0, j = 9; i < 10; j = i++) {
         if ((ys[i] > v) != (ys[j] > v) && u < (xs[j] - xs[i]) * (v - ys[i]) / (ys[j] - ys[i]) + xs[i]) {
            in = !in;
         }
      }
      return in;
   }

   /** The word in the pixel font, one letter after another from left to right. */
   private static List<Rectangle> word(String word, int size, int centerX, int centerY, int maxWidth) {
      int columns = Math.max(1, word.length() * (PixelFont.WIDTH + 1) - 1);
      int cell = Math.max(2, Math.min(size / PixelFont.HEIGHT, maxWidth / columns));
      boolean[][] grid = new boolean[PixelFont.HEIGHT][columns];
      for (int i = 0; i < word.length(); i++) {
         for (int row = 0; row < PixelFont.HEIGHT; row++) {
            for (int column = 0; column < PixelFont.WIDTH; column++) {
               grid[row][i * (PixelFont.WIDTH + 1) + column] = PixelFont.lit(word.charAt(i), column, row);
            }
         }
      }
      List<Rectangle> pieces = merge(grid, cell, centerX - columns * cell / 2, centerY - PixelFont.HEIGHT * cell / 2);
      pieces.sort(Comparator.comparingInt((Rectangle r) -> r.x).thenComparingInt(r -> r.y));
      return pieces;
   }

   /**
    * Covers the lit cells with upright strips, as few as a simple sweep finds: each column's runs of lit
    * cells, with a run that repeats the one in the column before it joined to it. Strips side by side let
    * the colors change from left to right.
    */
   private static List<Rectangle> merge(boolean[][] grid, int cell, int left, int top) {
      List<Rectangle> done = new ArrayList<>();
      List<Rectangle> open = new ArrayList<>();
      int rows = grid.length;
      int columns = rows == 0 ? 0 : grid[0].length;
      for (int column = 0; column < columns; column++) {
         List<Rectangle> next = new ArrayList<>();
         int row = 0;
         while (row < rows) {
            if (!grid[row][column]) {
               row++;
               continue;
            }
            int start = row;
            while (row < rows && grid[row][column]) {
               row++;
            }
            Rectangle before = null;
            for (Rectangle r : open) {
               if (r.y == start && r.height == row - start) {
                  before = r;
                  break;
               }
            }
            if (before != null) {
               open.remove(before);
               before.width++;
               next.add(before);
            } else {
               next.add(new Rectangle(column, start, 1, row - start));
            }
         }
         done.addAll(open);
         open = next;
      }
      done.addAll(open);
      List<Rectangle> pieces = new ArrayList<>();
      for (Rectangle r : done) {
         pieces.add(new Rectangle(left + r.x * cell, top + r.y * cell, r.width * cell, r.height * cell));
      }
      return pieces;
   }
}
