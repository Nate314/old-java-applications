package minesweeper;

import java.util.ArrayList;
import java.util.List;

/**
 * A first game's two lessons, shown on the board itself: first a number whose hidden tiles are all its mines (flag
 * them), then a number whose mines are all flagged (click it to open the rest around it). Each finds the example on
 * the board as it stands; with none there, the lines say the rule without one.
 */
final class Coach {
   /** The lesson to flag a mine, then the one to open around a number, then none. */
   static final int FLAG = 0;
   static final int CHORD = 1;
   static final int DONE = 2;

   /** The number the lesson reads (ringed in gold), or -1; cells as row * columns + column. */
   final int number;
   /** The tiles it points at: mines to flag, or the safe tiles clicking the number opens. */
   final List<Integer> cells;
   final boolean mines;
   final String action;
   final String reason;

   private Coach(int number, List<Integer> cells, boolean mines, String action, String reason) {
      this.number = number;
      this.cells = cells;
      this.mines = mines;
      this.action = action;
      this.reason = reason;
   }

   /**
    * Lesson one, on the board as a player sees it ({@code view} as {@link GameFrame} keeps it): the smallest number
    * with as many hidden tiles around it as its count, so they are all mines, and some of them not flagged yet.
    */
   static Coach flag(int[][] view) {
      int columns = view[0].length;
      Coach best = null;
      int bestValue = 9;
      for (int r = 0; r < view.length; r++) {
         for (int c = 0; c < columns; c++) {
            int value = view[r][c];
            if (value <= 0 || value >= bestValue) {
               continue;
            }
            int around = 0;
            List<Integer> hidden = new ArrayList<>();
            for (int[] n : MineFieldGenerator.neighbors(view.length, columns, r, c)) {
               around += view[n[0]][n[1]] < 0 ? 1 : 0;
               if (view[n[0]][n[1]] == Solver.HIDDEN) {
                  hidden.add(n[0] * columns + n[1]);
               }
            }
            if (around == value && !hidden.isEmpty()) {
               bestValue = value;
               String plural = value == 1 ? "1 mine and 1 hidden tile." : value + " mines and " + value + " hidden tiles.";
               best = new Coach(r * columns + c, hidden, true, "The gold " + value + " touches " + plural,
                  value == 1 ? "So flag the red tile: right click or long press." : "So flag the red tiles: right click or long press.");
            }
         }
      }
      return best != null ? best : new Coach(-1, new ArrayList<>(), true, "Each number counts the mines touching it.",
         "Flag a sure mine: right click or long press.");
   }

   /**
    * Lesson two: a number whose flags add up and are all right, so clicking it opens safe tiles; with none yet, the
    * next mine to flag on the way there.
    */
   static Coach chord(String[][] grid, boolean[][] revealed, boolean[][] flagged, int[][] view) {
      int columns = grid[0].length;
      for (int r = 0; r < grid.length; r++) {
         for (int c = 0; c < columns; c++) {
            List<int[]> targets = MineFieldGenerator.chordTargets(grid, revealed, flagged, r, c);
            List<Integer> cells = new ArrayList<>();
            for (int[] t : targets) {
               if (MineFieldGenerator.isMine(grid, t[0], t[1])) { // a wrong flag: clicking it would lose
                  cells.clear();
                  break;
               }
               cells.add(t[0] * columns + t[1]);
            }
            if (!cells.isEmpty()) {
               String value = grid[r][c].trim();
               return new Coach(r * columns + c, cells, false,
                  "The gold " + value + " has " + (value.equals("1") ? "its mine" : "all " + value + " mines") + " flagged.",
                  "Click or tap it to open the glowing tiles at once.");
            }
         }
      }
      Coach next = flag(view);
      if (next.number >= 0) {
         return new Coach(next.number, next.cells, true, "Flag the red " + (next.cells.size() == 1 ? "tile" : "tiles") + " too, then click a number",
            "whose mines are all flagged to open the rest.");
      }
      return new Coach(-1, new ArrayList<>(), false, "Flag all the mines around a number,",
         "then click it to open its other tiles at once.");
   }
}
