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

   /** One cactus, 17 x 35 or (large) 25 x 50, with its top left at (x, y). */
   static void cactus(Graphics2D g, int x, int y, boolean large) {
      g.setColor(RunnerPanel.INK);
      // Each has one row solid from arm to arm, at the bot's height (22 above the ground), so a pixel that
      // checks once a step cannot slip between the arms and the trunk.
      if (large) {
         g.fillRect(x + 10, y, 6, 2);
         g.fillRect(x + 9, y + 2, 8, 48);
         g.fillRect(x + 1, y + 12, 4, 18);
         g.fillRect(x + 20, y + 8, 4, 22);
         g.fillRect(x + 1, y + 26, 23, 4);
      } else {
         g.fillRect(x + 7, y, 4, 1);
         g.fillRect(x + 6, y + 1, 6, 34);
         g.fillRect(x + 1, y + 6, 3, 10);
         g.fillRect(x + 13, y + 3, 3, 13);
         g.fillRect(x + 1, y + 12, 15, 4);
      }
   }
}
