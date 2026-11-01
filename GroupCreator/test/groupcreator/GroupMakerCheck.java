package groupcreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Self-check for the grouping. Run with java -ea. */
public class GroupMakerCheck {
   public static void main(String[] args) {
      Random random = new Random(1);
      boolean sawMax = false;
      for (int count = 1; count <= 20; count++) {
         for (int min = 1; min <= 5; min++) {
            for (int max = min; max <= 6; max++) {
               List<String> names = new ArrayList<>();
               for (int i = 0; i < count; i++) {
                  names.add("n" + i);
               }
               String list = String.join(" , ", names) + ",";
               List<List<String>> groups;
               try {
                  groups = GroupMaker.makeGroups(list, min, max, random);
               } catch (IllegalArgumentException e) {
                  check(!GroupMaker.canSplit(count, min, max), "refused a splittable case " + count + " " + min + "-" + max);
                  continue;
               }
               List<String> seen = new ArrayList<>();
               for (List<String> group : groups) {
                  check(group.size() >= min && group.size() <= max, "group size " + group.size() + " outside " + min + "-" + max);
                  sawMax |= group.size() == max && min < max;
                  seen.addAll(group);
               }
               seen.sort(null);
               names.sort(null);
               check(seen.equals(names), "names lost, duplicated or untrimmed: " + seen);
            }
         }
      }
      check(sawMax, "a group never reached max");

      check(!GroupMaker.canSplit(5, 3, 4), "5 names cannot make groups of 3 or 4");
      check(GroupMaker.canSplit(7, 3, 4), "7 = 3 + 4");
      check(!GroupMaker.canSplit(2, 3, 3), "too few names");
      expectRefused(" , ,", 1, 3);
      expectRefused("a,b", 0, 3);
      expectRefused("a,b", 3, 2);

      // Every order of three names should come up about equally often.
      Map<String, Integer> orders = new HashMap<>();
      for (int i = 0; i < 60000; i++) {
         orders.merge(GroupMaker.makeGroups("a,b,c", 3, 3, random).get(0).toString(), 1, Integer::sum);
      }
      check(orders.size() == 6, "expected 6 orders, got " + orders.keySet());
      for (int seen : orders.values()) {
         check(Math.abs(seen - 10000) < 600, "uneven shuffle: " + orders);
      }

      check(GroupMaker.describe(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"))).equals(
         Arrays.asList("Group 1 --- a, b", "Group 2 --- c")), "describe format");
      System.out.println("GroupMakerCheck passed");
   }

   private static void expectRefused(String list, int min, int max) {
      try {
         GroupMaker.makeGroups(list, min, max, new Random());
      } catch (IllegalArgumentException e) {
         return;
      }
      throw new AssertionError("accepted " + list + " " + min + "-" + max);
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
