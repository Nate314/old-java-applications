package minesweeper;

import java.util.List;
import java.util.Random;

/** Self-check for board generation and flood reveal. Run with java -ea. */
public class MineFieldCheck {
   public static void main(String[] args) {
      for (int seed = 0; seed < 500; seed++) {
         Random random = new Random(seed);
         int safeRow = random.nextInt(15);
         int safeColumn = random.nextInt(15);
         MineFieldGenerator generator = new MineFieldGenerator(15, 15, 18, safeRow, safeColumn, random);
         String[][] grid = generator.getGrid();

         int mines = 0;
         for (int row = 0; row < 15; row++) {
            for (int column = 0; column < 15; column++) {
               String value = grid[row][column];
               check(value != null, "cell " + row + "," + column + " is null");
               if (MineFieldGenerator.isMine(grid, row, column)) {
                  mines++;
                  check(Math.abs(row - safeRow) > 1 || Math.abs(column - safeColumn) > 1, "mine next to first click");
               } else {
                  int expected = 0;
                  for (int[] n : MineFieldGenerator.neighbors(15, 15, row, column)) {
                     expected += MineFieldGenerator.isMine(grid, n[0], n[1]) ? 1 : 0;
                  }
                  check(value.equals(expected == 0 ? " " : String.valueOf(expected)), "wrong count at " + row + "," + column);
               }
            }
         }
         check(mines == 18, "expected 18 mines, got " + mines);

         // The first click is blank, so it must open the whole connected region and its border.
         boolean[][] revealed = new boolean[15][15];
         boolean[][] flagged = new boolean[15][15];
         List<int[]> opened = MineFieldGenerator.reveal(grid, revealed, flagged, safeRow, safeColumn);
         check(opened.size() >= 4, "first click opened only " + opened.size());
         for (int row = 0; row < 15; row++) {
            for (int column = 0; column < 15; column++) {
               if (revealed[row][column]) {
                  check(!MineFieldGenerator.isMine(grid, row, column), "flood fill revealed a mine");
                  if (" ".equals(grid[row][column])) {
                     for (int[] n : MineFieldGenerator.neighbors(15, 15, row, column)) {
                        check(revealed[n[0]][n[1]], "blank cell neighbor not revealed");
                     }
                  }
               }
            }
         }
      }

      // Neighbor lists: corners have 3, edges 5, inside 8, and never the cell itself.
      check(MineFieldGenerator.neighbors(15, 15, 0, 0).size() == 3, "corner neighbors");
      check(MineFieldGenerator.neighbors(15, 15, 14, 14).size() == 3, "bottom-right corner neighbors");
      check(MineFieldGenerator.neighbors(15, 15, 0, 7).size() == 5, "edge neighbors");
      check(MineFieldGenerator.neighbors(15, 15, 7, 7).size() == 8, "inner neighbors");

      // Flags do not win the game: only revealing every safe cell does.
      String[][] tiny = {{"b", "1"}, {"1", "1"}};
      boolean[][] revealed = new boolean[2][2];
      check(!MineFieldGenerator.isWon(tiny, revealed), "won with nothing revealed");
      revealed[0][1] = revealed[1][0] = revealed[1][1] = true;
      check(MineFieldGenerator.isWon(tiny, revealed), "not won with all safe cells revealed");

      System.out.println("MineFieldCheck passed");
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
