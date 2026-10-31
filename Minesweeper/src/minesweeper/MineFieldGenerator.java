package minesweeper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Builds a board of "b" (mine), " " (blank) or a neighbor count, and computes flood reveals. */
public final class MineFieldGenerator {
   private final int mineCount;
   private final String[][] grid;

   /** Places mines at random, keeping the safe cell and its neighbors mine-free so the first click opens an area. */
   public MineFieldGenerator(int rows, int columns, int mineCount, int safeRow, int safeColumn, Random random) {
      this.grid = new String[rows][columns];
      List<Integer> candidates = new ArrayList<>();
      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            this.grid[row][column] = " ";
            if (Math.abs(row - safeRow) > 1 || Math.abs(column - safeColumn) > 1) {
               candidates.add(row * columns + column);
            }
         }
      }

      this.mineCount = Math.min(mineCount, candidates.size());
      for (int mineIndex = 0; mineIndex < this.mineCount; mineIndex++) {
         int cell = candidates.remove(random.nextInt(candidates.size()));
         this.grid[cell / columns][cell % columns] = "b";
      }

      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            if (!isMine(this.grid, row, column)) {
               int nearby = 0;
               for (int[] neighbor : neighbors(rows, columns, row, column)) {
                  if (isMine(this.grid, neighbor[0], neighbor[1])) {
                     nearby++;
                  }
               }
               this.grid[row][column] = nearby == 0 ? " " : String.valueOf(nearby);
            }
         }
      }
   }

   public final String[][] getGrid() {
      return this.grid;
   }

   public final int getMineCount() {
      return this.mineCount;
   }

   public static boolean isMine(String[][] grid, int row, int column) {
      return "b".equals(grid[row][column]);
   }

   /** The up to 8 in-bounds cells around (row, column), not including the cell itself. */
   public static List<int[]> neighbors(int rows, int columns, int row, int column) {
      List<int[]> result = new ArrayList<>();
      for (int dr = -1; dr <= 1; dr++) {
         for (int dc = -1; dc <= 1; dc++) {
            int r = row + dr;
            int c = column + dc;
            if ((dr != 0 || dc != 0) && r >= 0 && r < rows && c >= 0 && c < columns) {
               result.add(new int[]{r, c});
            }
         }
      }
      return result;
   }

   /**
    * Marks (row, column) revealed and, if it is blank, every connected blank cell and the numbered cells bordering them.
    * Cells in {@code blocked} (flagged tiles) are skipped. Returns the newly revealed cells in reveal order.
    */
   public static List<int[]> reveal(String[][] grid, boolean[][] revealed, boolean[][] blocked, int row, int column) {
      List<int[]> newlyRevealed = new ArrayList<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      if (!revealed[row][column]) {
         revealed[row][column] = true;
         queue.add(new int[]{row, column});
      }
      while (!queue.isEmpty()) {
         int[] cell = queue.poll();
         newlyRevealed.add(cell);
         if (" ".equals(grid[cell[0]][cell[1]])) {
            for (int[] neighbor : neighbors(grid.length, grid[0].length, cell[0], cell[1])) {
               if (!revealed[neighbor[0]][neighbor[1]] && !blocked[neighbor[0]][neighbor[1]]) {
                  revealed[neighbor[0]][neighbor[1]] = true;
                  queue.add(neighbor);
               }
            }
         }
      }
      return newlyRevealed;
   }

   /** True when every non-mine cell is revealed. */
   public static boolean isWon(String[][] grid, boolean[][] revealed) {
      for (int row = 0; row < grid.length; row++) {
         for (int column = 0; column < grid[row].length; column++) {
            if (!isMine(grid, row, column) && !revealed[row][column]) {
               return false;
            }
         }
      }
      return true;
   }
}
