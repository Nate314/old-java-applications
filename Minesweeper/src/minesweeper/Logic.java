package minesweeper;

import java.util.ArrayList;
import java.util.List;

/**
 * What can be worked out for certain from what a player sees, one step at a time, with the reason in words. Three
 * rules, tried in this order:
 * <ol>
 * <li>One number: its mines are all found, so its other hidden tiles are safe; or it has just as many hidden tiles
 * left as mines still missing, so they are all mines.</li>
 * <li>Two numbers that touch some of the same tiles: if one of them must put at least as many mines in the shared
 * tiles as the other needs in all, the other's own tiles are safe; if the other can take only some of the first's
 * mines, the rest go in the first's own tiles.</li>
 * <li>The mine count: every mine found means the other tiles are safe; as many tiles left as mines means all mines.</li>
 * </ol>
 * The board is {@code view[x][y]}, as {@link GameFrame} keeps it: {@link Solver#HIDDEN}, {@link Solver#FLAG} or a
 * revealed count. Flags are not trusted (a player's may be wrong); the tiles known to be mines are passed separately.
 */
public final class Logic {
   public enum Rule { ONE_NUMBER, TWO_NUMBERS, MINE_COUNT }

   /** One step: these tiles are all safe or all mines, because of these numbers. */
   public static final class Deduction {
      public final Rule rule;
      /** True: the tiles are mines; false: they are safe. */
      public final boolean mines;
      /** The tiles it settles, as x * height + y. */
      public final List<Integer> cells;
      /** The numbers it reads (none for the mine count); with two, the first is the one that decides. */
      public final List<Integer> numbers;
      /** The reason, short enough for one line under the board. */
      public final String reason;

      Deduction(Rule rule, boolean mines, List<Integer> cells, List<Integer> numbers, String reason) {
         this.rule = rule;
         this.mines = mines;
         this.cells = cells;
         this.numbers = numbers;
         this.reason = reason;
      }

      /** The first line of a hint: what to do. */
      public String action() {
         return this.mines ? (this.cells.size() == 1 ? "Mine: flag the red tile." : "Mines: flag the red tiles.")
            : this.cells.size() == 1 ? "Safe: open the glowing tile." : "Safe: open the glowing tiles.";
      }
   }

   /** A revealed number: its hidden tiles that are not known mines, and how many mines they still hold. */
   private static final class Count {
      final int x;
      final int y;
      final int value;
      final List<Integer> hidden = new ArrayList<>();
      int mines;

      Count(int x, int y, int value) {
         this.x = x;
         this.y = y;
         this.value = value;
         this.mines = value;
      }
   }

   private Logic() {
   }

   /**
    * The first step that can be worked out, or null when nothing is certain. {@code known[x][y]} marks tiles already
    * known to be mines; they are left out of the settled tiles. {@code totalMines} is the board's mine count.
    */
   public static Deduction find(int[][] view, boolean[][] known, int totalMines) {
      int width = view.length;
      int height = view[0].length;
      List<Count> counts = new ArrayList<>();
      Count[][] at = new Count[width][height];
      List<Integer> unknown = new ArrayList<>();
      int knownCount = 0;
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            if (view[x][y] < 0) {
               if (known[x][y]) {
                  knownCount++;
               } else {
                  unknown.add(x * height + y);
               }
            } else if (view[x][y] > 0) {
               Count count = new Count(x, y, view[x][y]);
               for (int[] n : MineFieldGenerator.neighbors(width, height, x, y)) {
                  if (view[n[0]][n[1]] < 0) {
                     if (known[n[0]][n[1]]) {
                        count.mines--;
                     } else {
                        count.hidden.add(n[0] * height + n[1]);
                     }
                  }
               }
               if (!count.hidden.isEmpty()) {
                  counts.add(count);
                  at[x][y] = count;
               }
            }
         }
      }

      for (Count count : counts) {
         if (count.mines <= 0) {
            return new Deduction(Rule.ONE_NUMBER, false, count.hidden, cells(count, height),
               count.value == 1 ? "This 1's mine is already found." : "This " + count.value + "'s mines are all found.");
         }
         if (count.mines == count.hidden.size()) {
            String tiles = count.mines == 1 ? "1 hidden tile" : count.mines + " hidden tiles";
            String more = count.mines == count.value ? " " : " more ";
            return new Deduction(Rule.ONE_NUMBER, true, count.hidden, cells(count, height),
               "This " + count.value + " needs " + count.mines + more + (count.mines == 1 ? "mine" : "mines") + " and has " + tiles + " left.");
         }
      }

      // Two numbers that share tiles are at most two steps apart.
      for (Count a : counts) {
         for (int x = Math.max(0, a.x - 2); x <= Math.min(width - 1, a.x + 2); x++) {
            for (int y = Math.max(0, a.y - 2); y <= Math.min(height - 1, a.y + 2); y++) {
               Count b = at[x][y];
               if (b == null || b == a) {
                  continue;
               }
               Deduction pair = pair(a, b, height);
               if (pair != null) {
                  return pair;
               }
            }
         }
      }

      int left = totalMines - knownCount;
      if (!unknown.isEmpty() && left == 0) {
         return new Deduction(Rule.MINE_COUNT, false, unknown, new ArrayList<>(), "All " + plural(totalMines, "mine") + " are found.");
      }
      if (!unknown.isEmpty() && left == unknown.size()) {
         return new Deduction(Rule.MINE_COUNT, true, unknown, new ArrayList<>(),
            "The last " + plural(left, "hidden tile") + " hold the last " + plural(left, "mine") + ".");
      }
      return null;
   }

   /**
    * A hint for a player: the first step that changes something, worked out from the numbers alone, so a wrong flag
    * cannot lead it astray. Mines it finds that the player has already flagged are taken as known and it looks
    * further; with {@code safeOnly}, so are all the mines it finds, and the step is always one that finds safe tiles
    * (after a loss, the tiles that could have been opened instead). Null when nothing is certain.
    */
   public static Deduction hint(int[][] view, boolean[][] flagged, int totalMines, boolean safeOnly) {
      boolean[][] known = new boolean[view.length][view[0].length];
      int height = view[0].length;
      while (true) {
         Deduction step = find(view, known, totalMines);
         if (step == null || !step.mines) {
            return step;
         }
         boolean allFlagged = true;
         for (int cell : step.cells) {
            allFlagged &= flagged[cell / height][cell % height];
            known[cell / height][cell % height] = true;
         }
         if (!allFlagged && !safeOnly) {
            return step;
         }
      }
   }

   /**
    * Two numbers A and B touching some of the same tiles. A puts at least (A's mines - A's own tiles) mines in the
    * shared tiles; when that is all B needs, B's own tiles are safe, and A's own tiles hold the rest of A's mines.
    */
   private static Deduction pair(Count a, Count b, int height) {
      List<Integer> shared = new ArrayList<>(a.hidden);
      shared.retainAll(b.hidden);
      if (shared.isEmpty()) {
         return null;
      }
      List<Integer> onlyA = new ArrayList<>(a.hidden);
      onlyA.removeAll(shared);
      List<Integer> onlyB = new ArrayList<>(b.hidden);
      onlyB.removeAll(shared);
      if (a.mines - onlyA.size() < b.mines) {
         return null;
      }
      List<Integer> numbers = new ArrayList<>(cells(a, height));
      numbers.addAll(cells(b, height));
      String nameA = name(a, b);
      String nameB = name(b, a);
      if (!onlyB.isEmpty()) {
         return new Deduction(Rule.TWO_NUMBERS, false, onlyB, numbers,
            "The " + nameA + " puts " + plural(b.mines, "mine") + " by the " + nameB + ": all it needs.");
      }
      if (!onlyA.isEmpty() && a.mines - b.mines == onlyA.size()) {
         return new Deduction(Rule.TWO_NUMBERS, true, onlyA, numbers,
            "Only " + b.mines + " of the " + nameA + "'s " + plural(a.mines, "mine") + " fit by the " + nameB + ".");
      }
      return null;
   }

   /** A number named by where it sits next to the other one: "left 1", "top 2". */
   private static String name(Count count, Count other) {
      int dx = count.x - other.x;
      int dy = count.y - other.y;
      String side = Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? "left" : "right") : (dy < 0 ? "top" : "bottom");
      return side + " " + count.value;
   }

   private static List<Integer> cells(Count count, int height) {
      List<Integer> list = new ArrayList<>();
      list.add(count.x * height + count.y);
      return list;
   }

   private static String plural(int n, String word) {
      return n + " " + word + (n == 1 ? "" : "s");
   }
}
