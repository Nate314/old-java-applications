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

      // The preview before round 1 says what Make groups deals, and refuses what it refuses.
      check(GroupMaker.plan(12, 2, 3).equals("4 to 6 groups of 2 or 3"), GroupMaker.plan(12, 2, 3));
      check(GroupMaker.plan(12, 3, 3).equals("4 groups of 3"), GroupMaker.plan(12, 3, 3));
      check(GroupMaker.plan(5, 2, 4).equals("2 groups of 2 or 3"), GroupMaker.plan(5, 2, 4));
      check(GroupMaker.plan(1, 1, 3).equals("1 group of 1"), GroupMaker.plan(1, 1, 3));
      check(GroupMaker.plan(10, 2, 5).equals("2 to 5 groups of 2 to 5"), GroupMaker.plan(10, 2, 5));
      for (int count = 0; count <= 20; count++) {
         for (int min = 0; min <= 5; min++) {
            for (int max = 0; max <= 6; max++) {
               boolean planned = true;
               try {
                  GroupMaker.plan(count, min, max);
               } catch (IllegalArgumentException e) {
                  planned = false;
               }
               check(planned == (count > 0 && min >= 1 && max >= min && GroupMaker.canSplit(count, min, max)),
                  "plan and Make groups disagree on " + count + " " + min + "-" + max);
            }
         }
      }

      // The size chips: pairs to sixes, each at least two groups, each one Make groups can deal.
      check(sizes(12).equals("2-2 3-3 4-4 5-6 6-6"), sizes(12));
      check(sizes(13).equals("2-3 3-4 4-5 6-7"), sizes(13));   // 13 in fives or sixes cannot be split
      check(sizes(3).isEmpty() && sizes(4).equals("2-2"), sizes(3) + "|" + sizes(4));
      check(GroupMaker.groupCount(12, 2, 2).equals("6 groups") && GroupMaker.groupCount(13, 2, 3).equals("5 to 6 groups"),
         GroupMaker.groupCount(13, 2, 3));
      for (int count = 0; count <= 40; count++) {
         for (int[] choice : GroupMaker.sizeChoices(count)) {
            check(GroupMaker.canSplit(count, choice[0], choice[1]) && count / choice[0] >= 2, "bad size chip for " + count);
         }
      }

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
      check(GroupMaker.describe(Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("d")), true).equals(
         Arrays.asList("Group 1 --- a (Leader), b (Scribe), c (Timer)", "Group 2 --- d")), "describe with roles");
      System.out.println("GroupMakerCheck passed");
   }

   private static String sizes(int count) {
      StringBuilder text = new StringBuilder();
      for (int[] choice : GroupMaker.sizeChoices(count)) {
         text.append(text.length() == 0 ? "" : " ").append(choice[0]).append('-').append(choice[1]);
      }
      return text.toString();
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
