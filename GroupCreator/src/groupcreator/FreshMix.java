package groupcreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Makes the next round of groups so people meet new partners. The group sizes come from
 * {@link GroupMaker#makeGroups}, as in a plain shuffle; then, from several random starts, names are swapped between
 * groups while a swap lowers the cost, and the cheapest result wins. A pair costs the square of the rounds it has
 * already shared, so a pair that has met twice is avoided before two pairs that met once.
 */
public final class FreshMix {
   static final int STARTS = 24;

   private FreshMix() {
   }

   /** Throws IllegalArgumentException, as makeGroups does, when the names cannot be split between min and max. */
   public static List<List<String>> mix(List<String> names, int min, int max, PartnerHistory history, Random random) {
      List<List<String>> shuffled = GroupMaker.makeGroups(names, min, max, random);
      if (history.isEmpty()) {
         return shuffled;
      }
      int n = names.size();
      int[][] weight = new int[n][n];
      for (int i = 0; i < n; i++) {
         for (int j = 0; j < n; j++) {
            int met = history.together(names.get(i), names.get(j));
            weight[i][j] = met * met;
         }
      }
      int[] sizes = new int[shuffled.size()];
      for (int g = 0; g < sizes.length; g++) {
         sizes[g] = shuffled.get(g).size();
      }

      int[] best = null;
      int bestCost = Integer.MAX_VALUE;
      for (int start = 0; start < STARTS && bestCost > 0; start++) {
         int[] groupOf = deal(sizes, n, random);
         improve(groupOf, weight, random);
         int cost = cost(groupOf, weight);
         if (cost < bestCost) {
            bestCost = cost;
            best = groupOf;
         }
      }

      List<List<String>> groups = new ArrayList<>();
      for (int g = 0; g < sizes.length; g++) {
         groups.add(new ArrayList<>());
      }
      List<Integer> order = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         order.add(i);
      }
      // Names go in their group in a random order, so the first name of a group says nothing.
      Collections.shuffle(order, random);
      for (int i : order) {
         groups.get(best[i]).add(names.get(i));
      }
      return groups;
   }

   /** A random group for each name, with the given group sizes. */
   private static int[] deal(int[] sizes, int n, Random random) {
      List<Integer> slots = new ArrayList<>();
      for (int g = 0; g < sizes.length; g++) {
         for (int k = 0; k < sizes[g]; k++) {
            slots.add(g);
         }
      }
      Collections.shuffle(slots, random);
      int[] groupOf = new int[n];
      for (int i = 0; i < n; i++) {
         groupOf[i] = slots.get(i);
      }
      return groupOf;
   }

   /** Swaps two names of different groups whenever that lowers the cost, until no swap does. */
   private static void improve(int[] groupOf, int[][] weight, Random random) {
      int n = groupOf.length;
      List<Integer> order = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         order.add(i);
      }
      boolean improved = true;
      for (int pass = 0; improved && pass < 50; pass++) {
         improved = false;
         Collections.shuffle(order, random);
         for (int a : order) {
            for (int b : order) {
               if (groupOf[a] == groupOf[b]) {
                  continue;
               }
               // The cost of a and b where they are, minus the cost of each in the other's group.
               int delta = 0;
               for (int x = 0; x < n; x++) {
                  if (x == a || x == b) {
                     continue;
                  }
                  if (groupOf[x] == groupOf[b]) {
                     delta += weight[a][x] - weight[b][x];
                  } else if (groupOf[x] == groupOf[a]) {
                     delta += weight[b][x] - weight[a][x];
                  }
               }
               if (delta < 0) {
                  int swap = groupOf[a];
                  groupOf[a] = groupOf[b];
                  groupOf[b] = swap;
                  improved = true;
               }
            }
         }
      }
   }

   static int cost(int[] groupOf, int[][] weight) {
      int cost = 0;
      for (int i = 0; i < groupOf.length; i++) {
         for (int j = i + 1; j < groupOf.length; j++) {
            if (groupOf[i] == groupOf[j]) {
               cost += weight[i][j];
            }
         }
      }
      return cost;
   }
}
