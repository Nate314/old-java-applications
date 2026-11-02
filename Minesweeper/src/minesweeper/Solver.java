package minesweeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Picks the computer's next move when it plays (Watch). It sees only what a player sees: each tile is hidden, flagged
 * or a revealed count of the mines around it. A count whose mines are all flagged opens its other neighbors, a count
 * with as many hidden neighbors as mines left flags them, and two overlapping counts can settle the tiles only one
 * of them touches. With nothing certain, it guesses the tile least likely to be a mine.
 */
public final class Solver {
   public static final int HIDDEN = -1;
   public static final int FLAG = -2;

   /** A move: reveal or flag the tile at (row, column). */
   public static final class Move {
      public final int row;
      public final int column;
      public final boolean flag;
      /** False when nothing was certain and this is a guess. */
      public final boolean sure;

      Move(int row, int column, boolean flag, boolean sure) {
         this.row = row;
         this.column = column;
         this.flag = flag;
         this.sure = sure;
      }
   }

   /** A revealed count: its hidden neighbors (as row * columns + column) hold exactly {@code mines} mines. */
   private static final class Count {
      final List<Integer> hidden = new ArrayList<>();
      int mines;
   }

   private Solver() {
   }

   /**
    * The next move on a board seen as {@code view[row][column]}: {@link #HIDDEN}, {@link #FLAG} or a revealed count
    * (0 to 8), with {@code minesLeft} mines not yet flagged. Null when no hidden tile is left.
    */
   public static Move next(int[][] view, int minesLeft, Random random) {
      int rows = view.length;
      int columns = view[0].length;
      List<Count> counts = new ArrayList<>();
      List<Integer> hidden = new ArrayList<>();
      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            if (view[row][column] == HIDDEN) {
               hidden.add(row * columns + column);
            } else if (view[row][column] > 0) {
               Count count = new Count();
               count.mines = view[row][column];
               for (int[] n : MineFieldGenerator.neighbors(rows, columns, row, column)) {
                  int seen = view[n[0]][n[1]];
                  if (seen == FLAG) {
                     count.mines--;
                  } else if (seen == HIDDEN) {
                     count.hidden.add(n[0] * columns + n[1]);
                  }
               }
               if (!count.hidden.isEmpty()) {
                  counts.add(count);
               }
            }
         }
      }
      if (hidden.isEmpty()) {
         return null;
      }
      if (hidden.size() == rows * columns) {
         return new Move(rows / 2, columns / 2, false, true);   // the first click is always safe
      }

      for (Count count : counts) {
         if (count.mines <= 0) {
            return move(count.hidden.get(0), columns, false);
         }
         if (count.mines == count.hidden.size()) {
            return move(count.hidden.get(0), columns, true);
         }
      }
      // Two counts, one's tiles all among the other's: the other's extra tiles hold the difference in mines.
      for (Count small : counts) {
         for (Count big : counts) {
            if (small == big || small.hidden.size() >= big.hidden.size() || !big.hidden.containsAll(small.hidden)) {
               continue;
            }
            List<Integer> extra = new ArrayList<>(big.hidden);
            extra.removeAll(small.hidden);
            int mines = big.mines - small.mines;
            if (mines == 0) {
               return move(extra.get(0), columns, false);
            }
            if (mines == extra.size()) {
               return move(extra.get(0), columns, true);
            }
         }
      }

      // A guess: each tile's risk is the worst of its counts' odds, or the mines left spread over the hidden tiles.
      double spread = (double) minesLeft / hidden.size();
      double best = Double.MAX_VALUE;
      List<Integer> safest = new ArrayList<>();
      for (int cell : hidden) {
         double risk = -1;
         for (Count count : counts) {
            if (count.hidden.contains(cell)) {
               risk = Math.max(risk, (double) count.mines / count.hidden.size());
            }
         }
         if (risk < 0) {
            risk = spread;
         }
         if (risk < best - 1e-9) {
            best = risk;
            safest.clear();
         }
         if (risk < best + 1e-9) {
            safest.add(cell);
         }
      }
      int cell = safest.get(random.nextInt(safest.size()));
      return new Move(cell / columns, cell % columns, false, false);
   }

   private static Move move(int cell, int columns, boolean flag) {
      return new Move(cell / columns, cell % columns, flag, true);
   }
}
