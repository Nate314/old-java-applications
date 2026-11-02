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
      if (names.isEmpty()) {
         throw new IllegalArgumentException("Enter at least one name.");
      }
      if (min < 1 || max < min) {
         throw new IllegalArgumentException("min must be at least 1 and max at least min.");
      }
      if (!canSplit(names.size(), min, max)) {
         throw new IllegalArgumentException("Can't split " + names.size() + " names into groups of " + min + " to " + max + ".");
      }

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

   /** One line per group: "Group 1 --- name, name". */
   public static List<String> describe(List<List<String>> groups) {
      List<String> lines = new ArrayList<>();
      for (int i = 0; i < groups.size(); i++) {
         lines.add("Group " + (i + 1) + " --- " + String.join(", ", groups.get(i)));
      }
      return lines;
   }
}
