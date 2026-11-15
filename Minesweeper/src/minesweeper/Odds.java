package minesweeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The chance that each hidden tile is a mine, from the numbers alone (flags are not trusted, as with hints). Every way
 * of placing mines on the tiles next to numbers that fits all the numbers is counted, each weighted by the ways the
 * rest of the mines can lie on the other hidden tiles, so every board consistent with what is seen counts once.
 * Tiles next to numbers are split into groups that share no number, which keeps the counting small, and
 * placements are counted together whenever they agree on the numbers still being filled (see {@link Group#count}).
 */
public final class Odds {
   /**
    * Work allowed per call (states times mine counts); past that the odds are not worked out. Expert boards with many
    * scattered openings take up to about 1.6 million, so real boards stay far below it.
    */
   private static final int STEP_BUDGET = 20_000_000;

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

      void add(int[] around, int need) {
         this.numbers.add(around);
         this.needs.add(need);
      }

      /**
       * Counts the ways tile by tile along the walk, keeping only how many mines each number cut by the walk has so far,
       * so placements that agree on those are counted together; a sweep from each end, joined at every tile, gives how
       * many ways put a mine on it. False if the budget ran out.
       */
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
         int cap = Math.min(mines, n);
         int[][] at = new int[this.numbers.size()][];
         for (int j = 0; j < at.length; j++) {
            at[j] = new int[this.numbers.get(j).length];
            for (int t = 0; t < at[j].length; t++) {
               at[j][t] = this.cells.indexOf(this.numbers.get(j)[t]);
            }
            Arrays.sort(at[j]);
         }
         List<Map<String, double[]>> forward = new ArrayList<>();
         List<Map<String, double[]>> withMine = new ArrayList<>();
         List<Map<String, double[]>> backward = new ArrayList<>();
         if (!this.sweep(at, cap, false, forward, withMine, budget) || !this.sweep(at, cap, true, backward, new ArrayList<>(), budget)) {
            return false;
         }
         this.ways = new double[n + 1];
         double[] all = forward.get(n).get("");
         if (all != null) {
            System.arraycopy(all, 0, this.ways, 0, cap + 1);
         }
         this.mineWays = new double[n + 1][n];
         for (int i = 0; i < n; i++) {
            // Ways with a mine on tile i: the walk up to it (ending in that mine) joined with the walk from the far end.
            Map<String, double[]> after = backward.get(n - 1 - i);
            for (Map.Entry<String, double[]> left : withMine.get(i + 1).entrySet()) {
               double[] right = after.get(left.getKey());
               if (right == null) {
                  continue;
               }
               budget[0] -= cap * cap;
               for (int a = 0; a <= cap; a++) {
                  for (int b = 0; a + b <= cap; b++) {
                     this.mineWays[a + b][i] += left.getValue()[a] * right[b];
                  }
               }
            }
         }
         return budget[0] >= 0;
      }

      /**
       * Walks the tiles (from the last one back if {@code fromEnd}), filling {@code steps} with, before each tile, the
       * ways to hold 0 to {@code cap} mines per state: the mines so far of each number with tiles on both sides, as
       * characters in number order. From the end a number's state is the mines it still needs, so states from both ends
       * that meet at a tile match exactly. {@code withMine} keeps the same for the ways whose last tile is a mine.
       */
      private boolean sweep(int[][] at, int cap, boolean fromEnd, List<Map<String, double[]>> steps,
            List<Map<String, double[]>> withMine, int[] budget) {
         int n = this.cells.size();
         int numbers = at.length;
         // Per number: the step it starts and ends at in this direction, and how many of its tiles come after each step.
         int[] first = new int[numbers];
         int[] last = new int[numbers];
         List<List<Integer>> touching = new ArrayList<>();
         for (int s = 0; s < n; s++) {
            touching.add(new ArrayList<>());
         }
         for (int j = 0; j < numbers; j++) {
            int lo = at[j][0];
            int hi = at[j][at[j].length - 1];
            first[j] = fromEnd ? n - 1 - hi : lo;
            last[j] = fromEnd ? n - 1 - lo : hi;
            for (int i : at[j]) {
               touching.get(fromEnd ? n - 1 - i : i).add(j);
            }
         }
         Map<String, double[]> start = new HashMap<>();
         double[] one = new double[cap + 1];
         one[0] = 1;
         start.put("", one);
         steps.add(start);
         withMine.add(new HashMap<>());
         int[] placed = new int[numbers];
         int[] left = new int[numbers];
         for (int s = 0; s < n; s++) {
            Map<String, double[]> next = new HashMap<>();
            Map<String, double[]> mine = new HashMap<>();
            // The numbers cut before and after this tile.
            List<Integer> before = new ArrayList<>();
            List<Integer> after = new ArrayList<>();
            for (int j = 0; j < numbers; j++) {
               if (first[j] < s && last[j] >= s) {
                  before.add(j);
               }
               if (first[j] <= s && last[j] > s) {
                  after.add(j);
               }
               left[j] = 0;
               for (int i : at[j]) {
                  left[j] += (fromEnd ? n - 1 - i : i) > s ? 1 : 0;
               }
            }
            for (Map.Entry<String, double[]> state : steps.get(s).entrySet()) {
               budget[0] -= cap;
               if (budget[0] < 0) {
                  return false;
               }
               for (int b = 0; b < before.size(); b++) {
                  int j = before.get(b);
                  int value = state.getKey().charAt(b);
                  placed[j] = fromEnd ? this.needs.get(j) - value : value;
               }
               for (int choice = 0; choice < 2; choice++) {
                  boolean fits = true;
                  for (int j : touching.get(s)) {
                     int sofar = (first[j] == s ? 0 : placed[j]) + choice;
                     int need = this.needs.get(j);
                     fits &= sofar <= need && sofar + left[j] >= need;
                  }
                  if (!fits) {
                     continue;
                  }
                  StringBuilder key = new StringBuilder();
                  for (int j : after) {
                     int sofar = (first[j] == s ? 0 : placed[j]) + (touching.get(s).contains(j) ? choice : 0);
                     key.append((char) (fromEnd ? this.needs.get(j) - sofar : sofar));
                  }
                  double[] from = state.getValue();
                  double[] to = next.computeIfAbsent(key.toString(), k -> new double[cap + 1]);
                  double[] toMine = choice == 1 ? mine.computeIfAbsent(key.toString(), k -> new double[cap + 1]) : null;
                  for (int k = 0; k + choice <= cap; k++) {
                     to[k + choice] += from[k];
                     if (toMine != null) {
                        toMine[k + 1] += from[k];
                     }
                  }
               }
            }
            steps.add(next);
            withMine.add(mine);
         }
         return true;
      }
   }
}
