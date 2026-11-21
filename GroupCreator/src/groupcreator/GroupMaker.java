package groupcreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Splits a comma-separated list of names into random groups whose sizes all lie between min and max. */
public final class GroupMaker {
   private GroupMaker() {
   }

   /** Throws IllegalArgumentException, with a message for the user, when the names cannot be split that way. */
   public static List<List<String>> makeGroups(String list, int min, int max, Random random) {
      return makeGroups(parseNames(list), min, max, random);
   }

   /** The same, for names already split; the list is not changed. */
   public static List<List<String>> makeGroups(List<String> roster, int min, int max, Random random) {
      List<String> names = new ArrayList<>(roster);
      check(names.size(), min, max);

      Collections.shuffle(names, random);
      List<List<String>> groups = new ArrayList<>();
      int start = 0;
      while (start < names.size()) {
         int left = names.size() - start;
         // Pick a size at random among those that still let the remaining names split evenly.
         List<Integer> sizes = new ArrayList<>();
         for (int size = min; size <= Math.min(max, left); size++) {
            if (canSplit(left - size, min, max)) {
               sizes.add(size);
            }
         }
         int size = sizes.get(random.nextInt(sizes.size()));
         groups.add(new ArrayList<>(names.subList(start, start + size)));
         start += size;
      }
      return groups;
   }

   /** Throws, with what to fix, when count names cannot be dealt into groups of min to max. */
   static void check(int count, int min, int max) {
      if (count == 0) {
         throw new IllegalArgumentException("Enter at least one name.");
      }
      if (min < 1 || max < min) {
         throw new IllegalArgumentException("min must be at least 1 and max at least min.");
      }
      if (!canSplit(count, min, max)) {
         throw new IllegalArgumentException("Can't split " + count + " names into groups of " + min + " to " + max + ".");
      }
   }

   /**
    * What Make groups will deal for count names, before it is pressed: "4 to 6 groups of 2 or 3". Throws, as
    * {@link #check} does, when it cannot.
    */
   static String plan(int count, int min, int max) {
      check(count, min, max);
      // Every number of groups from the fewest to the most works: k groups hold k * min to k * max names.
      int fewest = (count + max - 1) / max;
      int most = count / min;
      // A group can have s names when the other count - s names still split.
      int small = max;
      int big = min;
      for (int s = min; s <= Math.min(max, count); s++) {
         if (canSplit(count - s, min, max)) {
            small = Math.min(small, s);
            big = Math.max(big, s);
         }
      }
      String groups = fewest == most ? fewest + (fewest == 1 ? " group" : " groups") : fewest + " to " + most + " groups";
      String sizes = small == big ? String.valueOf(small) : big == small + 1 ? small + " or " + big : small + " to " + big;
      return groups + " of " + sizes;
   }

   /** The trimmed, non-empty names of a comma-separated list, in order. */
   public static List<String> parseNames(String list) {
      List<String> names = new ArrayList<>();
      for (String name : list.split(",")) {
         if (!name.trim().isEmpty()) {
            names.add(name.trim());
         }
      }
      return names;
   }

   /** True when count names fit in some number of groups of min to max names (zero names need no groups). */
   static boolean canSplit(int count, int min, int max) {
      int fewestGroups = (count + max - 1) / max;
      return fewestGroups * min <= count;
   }

   /**
    * An earlier round's groups again, for the names here now: names not here leave their group, groups left empty
    * go, and each name the round did not have joins the smallest group (a new group when there is none).
    */
   static List<List<String>> again(List<List<String>> round, List<String> here) {
      List<List<String>> groups = new ArrayList<>();
      List<String> placed = new ArrayList<>();
      for (List<String> old : round) {
         List<String> group = new ArrayList<>();
         for (String name : old) {
            if (here.contains(name) && !placed.contains(name)) {
               group.add(name);
               placed.add(name);
            }
         }
         if (!group.isEmpty()) {
            groups.add(group);
         }
      }
      for (String name : here) {
         if (placed.contains(name)) {
            continue;
         }
         if (groups.isEmpty()) {
            groups.add(new ArrayList<>());
         }
         List<String> smallest = groups.get(0);
         for (List<String> group : groups) {
            if (group.size() < smallest.size()) {
               smallest = group;
            }
         }
         smallest.add(name);
         placed.add(name);
      }
      return groups;
   }

   /** One line per group: "Group 1 --- name, name". */
   public static List<String> describe(List<List<String>> groups) {
      return describe(groups, false);
   }

   /** The same lines, with each member's role after their name ("Alice (Leader), Bob (Scribe)") when roles is set. */
   public static List<String> describe(List<List<String>> groups, boolean roles) {
      List<String> lines = new ArrayList<>();
      for (int i = 0; i < groups.size(); i++) {
         List<String> group = groups.get(i);
         List<String> names = new ArrayList<>();
         for (int k = 0; k < group.size(); k++) {
            int role = roles ? Roles.of(k, group.size()) : -1;
            names.add(role < 0 ? group.get(k) : group.get(k) + " (" + Roles.NAMES[role] + ")");
         }
         lines.add("Group " + (i + 1) + " --- " + String.join(", ", names));
      }
      return lines;
   }
}
