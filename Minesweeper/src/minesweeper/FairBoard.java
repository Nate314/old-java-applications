package minesweeper;

import java.util.Random;

/**
 * No-guess boards: deals random boards until one can be cleared from the first click by {@link Logic} alone, so the
 * player never has to guess.
 */
public final class FairBoard {
   private FairBoard() {
   }

   /**
    * A board around a safe first click at (x, y) that logic clears, or null if none turned up within
    * {@code budgetMillis} (the caller then plays a random board).
    */
   public static String[][] deal(int width, int height, int mines, int x, int y, Random random, long budgetMillis) {
      long deadline = System.currentTimeMillis() + budgetMillis;
      do {
         String[][] grid = new MineFieldGenerator(width, height, mines, x, y, random).getGrid();
         if (solvable(grid, x, y, mines)) {
            return grid;
         }
      } while (System.currentTimeMillis() < deadline);
      return null;
   }

   /** True if, from a first click at (x, y), {@link Logic} works out every tile without a guess. */
   public static boolean solvable(String[][] grid, int x, int y, int mines) {
      int width = grid.length;
      int height = grid[0].length;
      boolean[][] revealed = new boolean[width][height];
      boolean[][] known = new boolean[width][height];
      boolean[][] none = new boolean[width][height];
      MineFieldGenerator.reveal(grid, revealed, none, x, y);
      int[][] view = new int[width][height];
      while (!MineFieldGenerator.isWon(grid, revealed)) {
         for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
               view[i][j] = !revealed[i][j] ? Solver.HIDDEN : " ".equals(grid[i][j]) ? 0 : Integer.parseInt(grid[i][j]);
            }
         }
         Logic.Deduction step = Logic.find(view, known, mines);
         if (step == null) {
            return false;
         }
         for (int cell : step.cells) {
            if (step.mines) {
               known[cell / height][cell % height] = true;
            } else {
               MineFieldGenerator.reveal(grid, revealed, none, cell / height, cell % height);
            }
         }
      }
      return true;
   }
}
