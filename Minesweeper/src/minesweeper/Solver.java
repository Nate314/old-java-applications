package minesweeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Picks the computer's next move when it plays (Watch). It sees only what a player sees: each tile is hidden, flagged
 * or a revealed count of the mines around it. Whatever {@link Logic} can work out comes first (taking its own flags as
 * mines), and the move carries the reason. With nothing certain, it guesses the tile least likely to be a mine.
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
      /** Why the move is certain (shown while the computer plays), or null for the first click and guesses. */
      public final Logic.Deduction why;

      Move(int row, int column, boolean flag, boolean sure, Logic.Deduction why) {
         this.row = row;
         this.column = column;
         this.flag = flag;
         this.sure = sure;
         this.why = why;
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
         return new Move(rows / 2, columns / 2, false, true, null);   // the first click is always safe
      }

      // Its own flags are taken as mines (the computer only plants flags it is sure of).
      boolean[][] flags = new boolean[rows][columns];
      int flagCount = 0;
      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            flags[row][column] = view[row][column] == FLAG;
            flagCount += flags[row][column] ? 1 : 0;
         }
      }
      Logic.Deduction step = Logic.find(view, flags, minesLeft + flagCount);
      if (step != null) {
         int cell = step.cells.get(0);
         return new Move(cell / columns, cell % columns, step.mines, true, step);
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
      return new Move(cell / columns, cell % columns, false, false, null);
   }
}
