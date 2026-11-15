package minesweeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The chance that each hidden tile is a mine, from the numbers alone (flags are not trusted, as with hints). Every way
 * of placing mines on the tiles next to numbers that fits all the numbers is counted, each weighted by the ways the
 * rest of the mines can lie on the other hidden tiles, so every board consistent with what is seen counts once.
 * Tiles next to numbers are split into groups that share no number, which keeps the counting small.
 */
public final class Odds {
   /**
    * Search steps allowed per call; past that the odds are not worked out.
    * shortcut: counts every placement, so a huge loose frontier (many scattered openings) runs out; in the self-check's
    * Expert games it never does. Upgrade to counting with memo over the frontier if players hit it.
    */
   private static final int STEP_BUDGET = 1_000_000;

   private Odds() {
   }

   /**
    * Odds for {@code view[x][y]} ({@link Solver#HIDDEN}, {@link Solver#FLAG} or a revealed count) with {@code mines}
    * mines on the board: for each tile, 0 to 1 if hidden or flagged, -1 if revealed. Null when no placement fits
    * (cannot happen on a real board) or the count ran past its budget.
    */
   public static double[][] of(int[][] view, int mines) {
      int width = view.length;
      int height = view[0].length;
      int cells = width * height;
      // Each number's hidden neighbors and how many mines they hold.
      List<int[]> numberCells = new ArrayList<>();
      List<Integer> needs = new ArrayList<>();
      boolean[] hidden = new boolean[cells];
      boolean[] frontier = new boolean[cells];
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            hidden[x * height + y] = view[x][y] < 0;
         }
      }
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            if (view[x][y] > 0) {
               List<Integer> around = new ArrayList<>();
               for (int[] n : MineFieldGenerator.neighbors(width, height, x, y)) {
                  if (hidden[n[0] * height + n[1]]) {
                     around.add(n[0] * height + n[1]);
                     frontier[n[0] * height + n[1]] = true;
                  }
               }
               if (around.isEmpty()) {
                  continue;
               }
               numberCells.add(ints(around));
               needs.add(view[x][y]);
            }
         }
      }
      int others = 0;
      for (int c = 0; c < cells; c++) {
         others += hidden[c] && !frontier[c] ? 1 : 0;
      }

      // Group the frontier: tiles sharing a number are in one group (union-find over the numbers' tiles).
      int[] parent = new int[cells];
      for (int c = 0; c < cells; c++) {
         parent[c] = c;
      }
      for (int[] around : numberCells) {
         for (int c : around) {
            parent[find(parent, c)] = find(parent, around[0]);
         }
      }
      List<Group> groups = new ArrayList<>();
      int[] groupOf = new int[cells];
      Arrays.fill(groupOf, -1);
      for (int c = 0; c < cells; c++) {
         if (frontier[c]) {
            int root = find(parent, c);
            if (groupOf[root] < 0) {
               groupOf[root] = groups.size();
               groups.add(new Group());
            }
            groups.get(groupOf[root]).cells.add(c);
         }
      }
      for (int i = 0; i < numberCells.size(); i++) {
         groups.get(groupOf[find(parent, numberCells.get(i)[0])]).add(numberCells.get(i), needs.get(i));
      }
      int[] budget = {STEP_BUDGET};
      for (Group group : groups) {
         if (!group.count(mines, budget)) {
            return null;
         }
      }

      // Ways to place the other mines among the tiles touching no number.
      double[] rest = new double[mines + 1];
      for (int k = 0; k <= mines; k++) {
         rest[k] = choose(others, mines - k);
      }
      double[] all = withoutGroup(groups, -1, mines);
      double total = 0;
      double otherMines = 0;
      for (int k = 0; k <= mines; k++) {
         total += all[k] * rest[k];
         otherMines += all[k] * rest[k] * (mines - k);
      }
      if (total <= 0) {
         return null;
      }
      double[][] odds = new double[width][height];
      for (int c = 0; c < cells; c++) {
         odds[c / height][c % height] = !hidden[c] ? -1 : others == 0 ? 0 : otherMines / others / total;
      }
      for (int g = 0; g < groups.size(); g++) {
         Group group = groups.get(g);
         double[] outside = withoutGroup(groups, g, mines);
         for (int i = 0; i < group.cells.size(); i++) {
            double sum = 0;
            for (int k = 0; k < group.ways.length; k++) {
               for (int j = 0; k + j <= mines; j++) {
                  sum += group.mineWays[k][i] * outside[j] * rest[k + j];
               }
            }
            int c = group.cells.get(i);
            odds[c / height][c % height] = sum / total;
         }
      }
      return odds;
   }

   /** A chance as a whole percent for the board, never 0% or 100% unless it is certain. */
   public static String percent(double odds) {
      long percent = Math.round(odds * 100);
      return (odds > 1e-9 && percent == 0 ? 1 : odds < 1 - 1e-9 && percent == 100 ? 99 : percent) + "%";
   }

   /** Ways the groups other than {@code skip} can hold 0 to {@code mines} mines between them. */
   private static double[] withoutGroup(List<Group> groups, int skip, int mines) {
      double[] ways = new double[mines + 1];
      ways[0] = 1;
      for (int g = 0; g < groups.size(); g++) {
         if (g == skip) {
            continue;
         }
         double[] next = new double[mines + 1];
         double[] own = groups.get(g).ways;
         for (int a = 0; a <= mines; a++) {
            for (int b = 0; b < own.length && a + b <= mines; b++) {
               next[a + b] += ways[a] * own[b];
            }
         }
         ways = next;
      }
      return ways;
   }

   private static double choose(int n, int k) {
      if (k < 0 || k > n) {
         return 0;
      }
      double result = 1;
      for (int i = 1; i <= k; i++) {
         result = result * (n - k + i) / i;
      }
      return result;
   }

   private static int[] ints(List<Integer> list) {
      int[] array = new int[list.size()];
      for (int i = 0; i < array.length; i++) {
         array[i] = list.get(i);
      }
      return array;
   }

   private static boolean contains(int[] array, int value) {
      for (int item : array) {
         if (item == value) {
            return true;
         }
      }
      return false;
   }

   private static int find(int[] parent, int c) {
      while (parent[c] != c) {
         c = parent[c] = parent[parent[c]];
      }
      return c;
   }

   /** Tiles that share numbers, and how many ways they can hold k mines (with how many of those put one on each tile). */
   private static final class Group {
      final List<Integer> cells = new ArrayList<>();
      final List<int[]> numbers = new ArrayList<>();
      final List<Integer> needs = new ArrayList<>();
      double[] ways;
      double[][] mineWays;
      // The search: each number's mines so far and tiles still open, per tile its numbers (as indexes).
      private int[] placed;
      private int[] open;
      private int[][] touching;
      private boolean[] mine;

      void add(int[] around, int need) {
         this.numbers.add(around);
         this.needs.add(need);
      }

      boolean count(int mines, int[] budget) {
         int n = this.cells.size();
         // Tiles in the order a walk along the numbers meets them, so each number is settled soon after it starts.
         List<Integer> order = new ArrayList<>();
         order.add(this.cells.get(0));
         for (int q = 0; q < order.size(); q++) {
            for (int[] around : this.numbers) {
               if (contains(around, order.get(q))) {
                  for (int c : around) {
                     if (!order.contains(c)) {
                        order.add(c);
                     }
                  }
               }
            }
         }
         this.cells.clear();
         this.cells.addAll(order);
         this.ways = new double[n + 1];
         this.mineWays = new double[n + 1][n];
         this.placed = new int[this.numbers.size()];
         this.open = new int[this.numbers.size()];
         this.mine = new boolean[n];
         List<List<Integer>> touch = new ArrayList<>();
         for (int i = 0; i < n; i++) {
            touch.add(new ArrayList<>());
         }
         for (int j = 0; j < this.numbers.size(); j++) {
            for (int c : this.numbers.get(j)) {
               touch.get(this.cells.indexOf(c)).add(j);
               this.open[j]++;
            }
         }
         this.touching = new int[n][];
         for (int i = 0; i < n; i++) {
            this.touching[i] = ints(touch.get(i));
         }
         return this.search(0, 0, Math.min(mines, n), budget);
      }

      /** Tries tile i as safe, then as a mine, keeping every number reachable; false if the budget ran out. */
      private boolean search(int i, int k, int mines, int[] budget) {
         if (--budget[0] < 0) {
            return false;
         }
         if (i == this.cells.size()) {
            this.ways[k]++;
            for (int t = 0; t < i; t++) {
               if (this.mine[t]) {
                  this.mineWays[k][t]++;
               }
            }
            return true;
         }
         for (int choice = 0; choice < 2; choice++) {
            if (choice == 1 && k == mines) {
               break;
            }
            boolean fits = true;
            for (int j : this.touching[i]) {
               this.open[j]--;
               this.placed[j] += choice;
               int need = this.needs.get(j);
               fits &= this.placed[j] <= need && this.placed[j] + this.open[j] >= need;
            }
            this.mine[i] = choice == 1;
            boolean ok = !fits || this.search(i + 1, k + choice, mines, budget);
            for (int j : this.touching[i]) {
               this.open[j]++;
               this.placed[j] -= choice;
            }
            this.mine[i] = false;
            if (!ok) {
               return false;
            }
         }
         return true;
      }
   }
}
