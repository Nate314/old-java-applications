package groupcreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A job for each member of a group, by place: the first name of a group is its Leader, the second its Scribe, and so
 * on, so a round's order is its roles and the history needs nothing more. A group of one has no roles, and a place
 * after the fifth has none. {@link #rotate} orders each new group so people get roles they have not had yet.
 */
public final class Roles {
   static final String[] NAMES = {"Leader", "Scribe", "Timer", "Reporter", "Checker"};
   static final int STARTS = 8;

   private Roles() {
   }

   /** The role (an index into {@link #NAMES}) of place {@code k} in a group of {@code size}, or -1 for none. */
   static int of(int k, int size) {
      return size > 1 && k < NAMES.length ? k : -1;
   }

   /** The rounds (from 1) of {@code history} in which {@code name} had {@code role}. */
   static List<Integer> roundsAs(PartnerHistory history, String name, int role) {
      List<Integer> numbers = new ArrayList<>();
      for (int r = 1; r <= history.size(); r++) {
         for (List<String> group : history.round(r)) {
            if (of(role, group.size()) == role && group.size() > role && group.get(role).equals(name)) {
               numbers.add(r);
               break;
            }
         }
      }
      return numbers;
   }

   /**
    * True when {@code name}, in place {@code k} of a group of {@code size}, has had that place's role in
    * {@code history} more often than another role this group has: a repeat the rotation could have avoided. In a pair,
    * a Leader who was once Leader and once Scribe is not one.
    */
   static boolean repeatsEarly(PartnerHistory history, String name, int k, int size) {
      int role = of(k, size);
      if (role < 0) {
         return false;
      }
      int had = roundsAs(history, name, role).size();
      for (int place = 0; place < size; place++) {
         int other = of(place, size);
         if (other >= 0 && other != role && roundsAs(history, name, other).size() < had) {
            return true;
         }
      }
      return false;
   }

   /**
    * The same groups with each one's names reordered so that, as far as possible, nobody gets a role they had before
    * in {@code history}. A role had n times costs n squared, so a twice-held role is avoided first; ties go at random.
    */
   static List<List<String>> rotate(List<List<String>> groups, PartnerHistory history, Random random) {
      List<List<String>> rotated = new ArrayList<>();
      for (List<String> group : groups) {
         rotated.add(order(group, history, random));
      }
      return rotated;
   }

   private static List<String> order(List<String> group, PartnerHistory history, Random random) {
      int n = group.size();
      int[][] cost = new int[n][n];
      for (int m = 0; m < n; m++) {
         for (int place = 0; place < n; place++) {
            int role = of(place, n);
            int had = role < 0 ? 0 : roundsAs(history, group.get(m), role).size();
            cost[m][place] = had * had;
         }
      }
      // placeOf[m] is the place of member m. From a few random starts, swap two members' places while that helps.
      int[] best = null;
      int bestCost = Integer.MAX_VALUE;
      for (int start = 0; start < STARTS && bestCost > 0; start++) {
         List<Integer> places = new ArrayList<>();
         for (int i = 0; i < n; i++) {
            places.add(i);
         }
         Collections.shuffle(places, random);
         int[] placeOf = new int[n];
         for (int m = 0; m < n; m++) {
            placeOf[m] = places.get(m);
         }
         boolean improved = true;
         while (improved) {
            improved = false;
            for (int a = 0; a < n; a++) {
               for (int b = a + 1; b < n; b++) {
                  if (cost[a][placeOf[b]] + cost[b][placeOf[a]] < cost[a][placeOf[a]] + cost[b][placeOf[b]]) {
                     int swap = placeOf[a];
                     placeOf[a] = placeOf[b];
                     placeOf[b] = swap;
                     improved = true;
                  }
               }
            }
         }
         int total = 0;
         for (int m = 0; m < n; m++) {
            total += cost[m][placeOf[m]];
         }
         if (total < bestCost) {
            bestCost = total;
            best = placeOf;
         }
      }
      String[] ordered = new String[n];
      for (int m = 0; m < n; m++) {
         ordered[best[m]] = group.get(m);
      }
      List<String> result = new ArrayList<>();
      Collections.addAll(result, ordered);
      return result;
   }
}
