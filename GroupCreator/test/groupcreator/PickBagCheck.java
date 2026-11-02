package groupcreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Self-check for the name wheel's picks: one turn each before anyone's second, put back, and the class file. */
public class PickBagCheck {
   public static void main(String[] args) throws Exception {
      Random random = new Random(11);
      List<String> twelve = GroupMaker.parseNames(Roster.EXAMPLE);

      // Twelve picks give twelve different names, and the thirteenth starts a new time around.
      for (int trial = 0; trial < 200; trial++) {
         PickBag bag = new PickBag();
         List<String> seen = new ArrayList<>();
         for (int i = 0; i < 12; i++) {
            List<String> pool = bag.pool(twelve);
            check(pool.size() == 12 - i, "pool shrinks by one per pick");
            String name = bag.pick(twelve, random);
            check(pool.contains(name) && !seen.contains(name), "picked " + name + " twice in one time around");
            seen.add(name);
         }
         check(bag.remaining(twelve).isEmpty() && bag.picked(twelve).equals(seen), "everyone had a turn, in order");
         check(bag.pool(twelve).size() == 12, "after everyone, the whole class is back on the wheel");
         String next = bag.pick(twelve, random);
         check(bag.picked(twelve).equals(Arrays.asList(next)), "a new time around starts with the next pick");
      }

      // Every name is equally likely to be first.
      Map<String, Integer> first = new HashMap<>();
      for (int i = 0; i < 12000; i++) {
         first.merge(new PickBag().pick(twelve, random), 1, Integer::sum);
      }
      for (String name : twelve) {
         int count = first.getOrDefault(name, 0);
         check(count > 850 && count < 1150, name + " came first " + count + " times in 12000");
      }

      // Put back returns the newest pick to the wheel; an empty bag has nothing to put back.
      List<String> four = Arrays.asList("a", "b", "c", "d");
      PickBag bag = new PickBag();
      check(bag.putBack(four) == null, "nothing to put back");
      String a = bag.pick(four, random);
      String b = bag.pick(four, random);
      check(b.equals(bag.putBack(four)), "put back the newest");
      check(bag.picked(four).equals(Arrays.asList(a)) && bag.remaining(four).size() == 3, "only the first pick is left");
      bag.clear();
      check(bag.isEmpty() && bag.pool(four).equals(four), "start over puts everyone back");

      // A name that leaves the class stops counting; duplicates are one slice; no class, no pick.
      bag.pick(Arrays.asList("x"), random);
      check(bag.picked(four).isEmpty() && bag.remaining(four).equals(four), "a name not in the class is ignored");
      check(new PickBag().pool(Arrays.asList("a", "b", "a")).equals(Arrays.asList("a", "b")), "duplicates count once");
      check(new PickBag().pick(new ArrayList<>(), random) == null, "no names, no pick");

      // The class file keeps the picks, in order, and a file without them has none.
      File file = File.createTempFile("groupcreator", ".txt");
      file.deleteOnExit();
      Roster roster = Roster.load(file);
      roster.save();
      check(!roster.text().contains("picked="), "no picked= line before the first pick");
      roster.picks.pick(twelve, random);
      roster.picks.pick(twelve, random);
      List<String> picked = roster.picks.picked(twelve);
      roster.save();
      Roster back = Roster.load(file);
      check(back.picks.picked(twelve).equals(picked), "picks round trip in order");
      check(back.text().equals(roster.text()), "same text");

      System.out.println("PickBagCheck passed");
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
