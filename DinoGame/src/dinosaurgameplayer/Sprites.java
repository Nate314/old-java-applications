package dinosaurgameplayer;

import java.awt.Graphics2D;

/** Pixel drawings for the game, one character per cell: X is drawn in the current color, anything else is not. */
final class Sprites {
   private Sprites() {
   }

   /** The dinosaur down to its hips (20 x 16 cells); one of the LEGS rows goes below. */
   static final String[] DINO = {
      "...........XXXXXXXX.",
      "..........XX.XXXXXXX",
      "..........XXXXXXXXXX",
      "..........XXXXXXXXXX",
      "..........XXXXX.....",
      "..........XXXXXXXX..",
      "X........XXXXX......",
      "X.......XXXXXX......",
      "XX.....XXXXXXXXX....",
      "XXX...XXXXXXXX.X....",
      "XXXXXXXXXXXXXX......",
      "XXXXXXXXXXXXXX......",
      ".XXXXXXXXXXXXX......",
      "..XXXXXXXXXXX.......",
      "...XXXXXXXXXX.......",
      "....XXXXXXXX........",
   };
   static final String[] LEGS_STAND = {
      ".....XXX.XXX........",
      ".....XX...XX........",
      ".....X.....X........",
      ".....XX....XX.......",
   };
   static final String[] LEGS_A = {
      ".....XXX.XXX........",
      ".....XX....XX.......",
      ".....X..............",
      ".....XX.............",
   };
   static final String[] LEGS_B = {
      ".....XXX.XXX........",
      ".....XXX...X........",
      "...........X........",
      "...........XX.......",
   };
   /** Ducking (26 x 11 cells), two strides. */
   static final String[] DUCK_A = {
      "X................XXXXXXXX.",
      "XX..XXXXXXXXXX..XX.XXXXXXX",
      "XXXXXXXXXXXXXXXXXXXXXXXXXX",
      ".XXXXXXXXXXXXXXXXXXXXXXXXX",
      "..XXXXXXXXXXXXXXXXXXXXX...",
      "...XXXXXXXXXXXXXXXXXXXXX..",
      "....XXXXXXXXXXXXX.........",
      ".....XXXXXXXXXX.XX........",
      ".....XXX...XX.............",
      ".....XX.....X.............",
      ".....X......XX............",
   };
   static final String[] DUCK_B = {
      DUCK_A[0], DUCK_A[1], DUCK_A[2], DUCK_A[3], DUCK_A[4], DUCK_A[5], DUCK_A[6], DUCK_A[7],
      ".....XXX...XX.............",
      "......XX...X..............",
      "......XX..................",
   };
   /** A bird flying left (21 x 14 cells), wings up and down. */
   static final String[] BIRD_UP = {
      ".......X.............",
      ".......XX............",
      "....X..XXX...........",
      "...XX..XXXX..........",
      "..XXX..XXXXX.........",
      ".XXXXXXXXXXXXX.......",
      "XXXXXXXXXXXXXXXXXXX..",
      "........XXXXXXXXXXXXX",
      ".........XXXXXXXXX...",
      ".........XXXXXXXX....",
   };
   static final String[] BIRD_DOWN = {
      ".....................",
      ".....................",
      "....X................",
      "...XX................",
      "..XXX................",
      ".XXXXXXXXXXXXX.......",
      "XXXXXXXXXXXXXXXXXXX..",
      "........XXXXXXXXXXXXX",
      ".........XXXXXXXXX...",
      "........XXXXXXXX.....",
      "........XXXX.........",
      "........XXX..........",
      "........XX...........",
      "........X............",
   };
   static final String[] CLOUD = {
      "........XXXXX.........",
      "......XX.....XX.......",
      ".....X.........XXX....",
      "...XX.............X...",
      ".XX................XX.",
      "XXXXXXXXXXXXXXXXXXXXXX",
   };

   /** Draws a sprite with its top left at (x, y), each cell a cell x cell square, joining runs into one rectangle. */
   static void draw(Graphics2D g, String[] rows, int x, int y, int cell) {
      for (int r = 0; r < rows.length; r++) {
         String row = rows[r];
         int c = 0;
         while (c < row.length()) {
            if (row.charAt(c) != 'X') {
               c++;
               continue;
            }
            int start = c;
            while (c < row.length() && row.charAt(c) == 'X') {
               c++;
            }
            g.fillRect(x + start * cell, y + r * cell, (c - start) * cell, cell);
         }
      }
   }

   /** The two cacti as rectangles {x, y, w, h} from their top left. Each has one row solid from arm to arm at the
    *  original bot's height (22 above the ground), so a pixel that checks once a step cannot slip between the arms
    *  and the trunk. */
   private static final int[][] SMALL_CACTUS = { { 7, 0, 4, 1 }, { 6, 1, 6, 34 }, { 1, 6, 3, 10 }, { 13, 3, 3, 13 }, { 1, 12, 15, 4 } };
   private static final int[][] LARGE_CACTUS = { { 10, 0, 6, 2 }, { 9, 2, 8, 48 }, { 1, 12, 4, 18 }, { 20, 8, 4, 22 }, { 1, 26, 23, 4 } };

   /** One cactus, 17 x 35 or (large) 25 x 50, with its top left at (x, y), in the current color. */
   static void cactus(Graphics2D g, int x, int y, boolean large) {
      for (int[] r : large ? LARGE_CACTUS : SMALL_CACTUS) {
         g.fillRect(x + r[0], y + r[1], r[2], r[3]);
      }
   }

   /** Whether the cactus drawn at (x, y) covers pixel (px, py). */
   static boolean cactusCovers(int x, int y, boolean large, int px, int py) {
      for (int[] r : large ? LARGE_CACTUS : SMALL_CACTUS) {
         if (px >= x + r[0] && px < x + r[0] + r[2] && py >= y + r[1] && py < y + r[1] + r[3]) {
            return true;
         }
      }
      return false;
   }

   /** Whether the sprite drawn by draw(g, rows, x, y, cell) covers pixel (px, py). */
   static boolean covers(String[] rows, int x, int y, int cell, int px, int py) {
      int r = Math.floorDiv(py - y, cell);
      int c = Math.floorDiv(px - x, cell);
      return r >= 0 && r < rows.length && c >= 0 && c < rows[r].length() && rows[r].charAt(c) == 'X';
   }
}
