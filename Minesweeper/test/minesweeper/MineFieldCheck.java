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

      checkChord();

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
      checkSolver();
      checkLogic();
      checkFairBoards();
      System.out.println("MineFieldCheck passed");
   }

   /**
    * Hints: on a small board each rule says the right thing, and over many random games (with wrong flags planted on
    * purpose) a hint never calls a mine safe or a safe tile a mine.
    */
   private static void checkLogic() {
      // A 1-1 on a board 3 across (x) and 2 down (y), one mine at (0,1), the 1s at (0,0) and (1,0) revealed. The left
      // 1's mine is in (0,1) or (1,1), which the right 1 touches too, so the right 1's other tiles (2,0), (2,1) are safe.
      int[][] view = {{1, Solver.HIDDEN}, {1, Solver.HIDDEN}, {Solver.HIDDEN, Solver.HIDDEN}};
      Logic.Deduction step = Logic.find(view, new boolean[3][2], 1);
      check(step != null && !step.mines && step.rule == Logic.Rule.TWO_NUMBERS, "a 1-1 frees the far tiles");
      check(step.cells.size() == 2 && step.cells.contains(2 * 2) && step.cells.contains(2 * 2 + 1), "the far tiles are (2,0) and (2,1)");
      check(step.reason.equals("The left 1 puts 1 mine by the right 1: all it needs."), "reason: " + step.reason);
      check(step.action().equals("Safe: open the glowing tiles."), "action: " + step.action());

      // One number: a 2 in a corner with two hidden tiles left has both as mines; once flagged, the rest is safe.
      int[][] corner = {{2, Solver.HIDDEN}, {Solver.HIDDEN, 2}};
      step = Logic.find(new int[][]{{2, Solver.HIDDEN}, {Solver.HIDDEN, Solver.HIDDEN}}, new boolean[2][2], 2);
      check(step == null, "a 2 with three hidden tiles settles nothing");
      step = Logic.find(corner, new boolean[2][2], 2);
      check(step != null && step.mines && step.cells.size() == 2, "a 2 with two hidden tiles: both mines");
      check(step.reason.equals("This 2 needs 2 mines and has 2 hidden tiles left."), "reason: " + step.reason);

      // The mine count: every mine known leaves the rest safe.
      boolean[][] known = {{false, true}, {false, false}};
      step = Logic.find(new int[][]{{Solver.HIDDEN, Solver.HIDDEN}, {Solver.HIDDEN, Solver.HIDDEN}}, known, 1);
      check(step != null && !step.mines && step.rule == Logic.Rule.MINE_COUNT && step.cells.size() == 3, "all mines found");

      // Random games: reveal what the hint says is safe, flag what it says are mines, and plant wrong flags.
      for (int seed = 0; seed < 300; seed++) {
         Random random = new Random(seed);
         Level level = Level.values()[seed % 3];
         int size = level.size;
         int x0 = random.nextInt(size);
         int y0 = random.nextInt(size);
         String[][] grid = new MineFieldGenerator(size, size, level.mines, x0, y0, random).getGrid();
         boolean[][] revealed = new boolean[size][size];
         boolean[][] flagged = new boolean[size][size];
         MineFieldGenerator.reveal(grid, revealed, flagged, x0, y0);
         for (int move = 0; move < size * size && !MineFieldGenerator.isWon(grid, revealed); move++) {
            if (random.nextInt(8) == 0) {   // a careless flag, right or wrong
               int x = random.nextInt(size);
               int y = random.nextInt(size);
               flagged[x][y] = !revealed[x][y];
            }
            int[][] seen = new int[size][size];
            for (int x = 0; x < size; x++) {
               for (int y = 0; y < size; y++) {
                  seen[x][y] = revealed[x][y] ? (grid[x][y].trim().isEmpty() ? 0 : Integer.parseInt(grid[x][y]))
                     : flagged[x][y] ? Solver.FLAG : Solver.HIDDEN;
               }
            }
            Logic.Deduction hint = Logic.hint(seen, flagged, level.mines, move % 5 == 4);
            if (hint == null) {
               break;
            }
            check(!hint.cells.isEmpty(), "a hint with no tiles");
            for (int cell : hint.cells) {
               int x = cell / size;
               int y = cell % size;
               check(!revealed[x][y], "a hint about a revealed tile");
               check(MineFieldGenerator.isMine(grid, x, y) == hint.mines, "a wrong hint: " + hint.reason);
            }
            for (int cell : hint.cells) {
               int x = cell / size;
               int y = cell % size;
               if (hint.mines) {
                  flagged[x][y] = true;
               } else {
                  flagged[x][y] = false;
                  MineFieldGenerator.reveal(grid, revealed, flagged, x, y);
               }
            }
         }
      }
   }

   /** No-guess boards: the right mines, a safe start, and logic alone clears each one from the first click. */
   private static void checkFairBoards() {
      for (Level level : Level.values()) {
         long start = System.currentTimeMillis();
         for (int seed = 0; seed < 50; seed++) {
            Random random = new Random(seed);
            int x = random.nextInt(level.size);
            int y = random.nextInt(level.size);
            String[][] grid = FairBoard.deal(level.size, level.size, level.mines, x, y, random, 2000);
            check(grid != null, level + ": no fair board in time");
            int mines = 0;
            for (String[] line : grid) {
               for (String value : line) {
                  mines += "b".equals(value) ? 1 : 0;
               }
            }
            check(mines == level.mines, level + " fair board has " + mines + " mines");
            check(" ".equals(grid[x][y]), "a fair board's first click is not an opening");
            check(FairBoard.solvable(grid, x, y, level.mines), "a fair board logic cannot clear");
         }
         System.out.println("Fair " + level + ": 50 boards in " + (System.currentTimeMillis() - start) + " ms");
      }
   }

   /**
    * Opening around a number: nothing until its flags add up, then exactly its hidden unflagged neighbors (a mine too,
    * when a flag is wrong); never for a hidden cell or a blank.
    */
   private static void checkChord() {
      String[][] grid = {
         {"b", "2", "1"},
         {"2", "b", "1"},
         {"1", "1", "1"}};
      boolean[][] revealed = new boolean[3][3];
      boolean[][] flagged = new boolean[3][3];
      check(MineFieldGenerator.chordTargets(grid, revealed, flagged, 2, 2).isEmpty(), "hidden cell opened around");
      revealed[2][2] = true;
      check(MineFieldGenerator.chordTargets(grid, revealed, flagged, 2, 2).isEmpty(), "opened with no flag");
      check(MineFieldGenerator.hiddenNeighbors(revealed, flagged, 2, 2).size() == 3, "hidden neighbors of a corner");
      flagged[1][1] = true;
      List<int[]> targets = MineFieldGenerator.chordTargets(grid, revealed, flagged, 2, 2);
      check(targets.size() == 2, "a 1 with its flag opens its 2 other neighbors, got " + targets.size());
      for (int[] t : targets) {
         check(!MineFieldGenerator.isMine(grid, t[0], t[1]) && !flagged[t[0]][t[1]], "opened a mine or a flag");
      }
      // A wrong flag: the 2 at (0, 1) counts two flags, but one is on a safe cell, so the mine under (0, 0) is opened.
      revealed[0][1] = true;
      flagged[0][2] = true; // wrong: (0, 2) is a 1, and the mine at (1, 1) is flagged already
      targets = MineFieldGenerator.chordTargets(grid, revealed, flagged, 0, 1);
      boolean opensMine = false;
      for (int[] t : targets) {
         opensMine |= MineFieldGenerator.isMine(grid, t[0], t[1]);
      }
      check(opensMine, "a wrong flag should let the number open a mine");
      check(MineFieldGenerator.chordTargets(new String[][]{{" "}}, new boolean[][]{{true}}, new boolean[1][1], 0, 0).isEmpty(),
         "a blank opened around");
   }

   /** Best times: only faster wins count, and the times and last level survive a reload of the file. */
   /**
    * The computer (Watch) plays whole games: a move it is sure of is never a mine (or a wrong flag), it never touches
    * a revealed tile, and it wins most Beginner games (only guesses can lose).
    */
   private static void checkSolver() {
      for (Level level : Level.values()) {
         int wins = 0;
         int games = 200;
         for (int seed = 0; seed < games; seed++) {
            Random random = new Random(seed);
            int size = level.size;
            String[][] grid = null;
            boolean[][] revealed = new boolean[size][size];
            boolean[][] flagged = new boolean[size][size];
            int flags = 0;
            for (int step = 0; step < size * size * 2; step++) {
               int[][] view = new int[size][size];
               for (int r = 0; r < size; r++) {
                  for (int c = 0; c < size; c++) {
                     view[r][c] = revealed[r][c] ? (grid[r][c].trim().isEmpty() ? 0 : Integer.parseInt(grid[r][c]))
                        : flagged[r][c] ? Solver.FLAG : Solver.HIDDEN;
                  }
               }
               Solver.Move move = Solver.next(view, level.mines - flags, random);
               check(move != null, "no move on an unfinished board");
               check(!revealed[move.row][move.column] && !flagged[move.row][move.column], "moved on a revealed or flagged tile");
               if (grid == null) {
                  grid = new MineFieldGenerator(size, size, level.mines, move.row, move.column, random).getGrid();
               }
               boolean mine = MineFieldGenerator.isMine(grid, move.row, move.column);
               if (move.flag) {
                  check(mine, "flagged a safe tile");
                  flagged[move.row][move.column] = true;
                  flags++;
                  continue;
               }
               check(!move.sure || !mine, "a sure move hit a mine");
               if (mine) {
                  break;
               }
               MineFieldGenerator.reveal(grid, revealed, flagged, move.row, move.column);
               if (MineFieldGenerator.isWon(grid, revealed)) {
                  wins++;
                  break;
               }
            }
         }
         System.out.println("Solver " + level + ": " + wins + " of " + games + " games won");
         check(level != Level.BEGINNER || wins > games * 0.7, "the computer won only " + wins + " Beginner games");
      }
   }

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
         check(reloaded.noGuess(), "no guess is on by default");
         reloaded.setNoGuess(false);
         check(!BestTimes.load(file).noGuess() && BestTimes.load(file).best(Level.EXPERT) == 61234, "no guess off reloaded");
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
