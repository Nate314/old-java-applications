package minesweeper;

import java.io.File;
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

      // Every level fits its mines around a safe first click.
      for (Level level : Level.values()) {
         String[][] board = new MineFieldGenerator(level.size, level.size, level.mines, 0, 0, new Random(1)).getGrid();
         int count = 0;
         for (String[] line : board) {
            for (String value : line) {
               count += "b".equals(value) ? 1 : 0;
            }
         }
         check(count == level.mines, level + " has " + count + " mines");
      }

      checkBestTimes();
      System.out.println("MineFieldCheck passed");
   }

   /** Best times: only faster wins count, and the times and last level survive a reload of the file. */
   private static void checkBestTimes() {
      try {
         File file = File.createTempFile("minesweeper-best", ".txt");
         file.delete();
         BestTimes times = BestTimes.load(file);
         check(times.lastLevel() == Level.CLASSIC && times.best(Level.EXPERT) == 0, "fresh file");
         check(times.record(Level.EXPERT, 90000), "first win is a record");
         check(!times.record(Level.EXPERT, 95000), "slower win is not a record");
         check(times.record(Level.EXPERT, 61234), "faster win is a record");
         times.setLastLevel(Level.BEGINNER);
         BestTimes reloaded = BestTimes.load(file);
         check(reloaded.best(Level.EXPERT) == 61234 && reloaded.best(Level.CLASSIC) == 0, "times reloaded");
         check(reloaded.lastLevel() == Level.BEGINNER, "level reloaded");
         check(BestTimes.format(61234).equals("1:01.2") && BestTimes.format(9900).equals("0:09.9"), "format");
         file.delete();
      } catch (java.io.IOException e) {
         throw new AssertionError(e);
      }
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
