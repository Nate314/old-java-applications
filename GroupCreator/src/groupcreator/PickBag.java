package groupcreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Who has been picked this time around the class, in order. A pick draws only from the names not picked yet, so
 * everyone gets one turn before anyone gets a second; once everyone has had a turn, the next pick starts a new
 * time around. Names are matched as typed, and a name that leaves the class simply stops counting.
 */
public final class PickBag {
   private final List<String> picked = new ArrayList<>();

   /** The names picked this time around, oldest first, that are still in the class. */
   public List<String> picked(List<String> names) {
      List<String> still = new ArrayList<>();
      for (String name : picked) {
         if (names.contains(name) && !still.contains(name)) {
            still.add(name);
         }
      }
      return still;
   }

   /** The class's names (each once, in class order) not picked yet this time around. */
   public List<String> remaining(List<String> names) {
      List<String> left = distinct(names);
      left.removeAll(picked);
      return left;
   }

   /**
    * The names the next pick draws from: the remaining ones, or when everyone has had a turn, the whole class again
    * except the last one picked, so no one is called twice in a row (unless they are the whole class).
    */
   public List<String> pool(List<String> names) {
      List<String> left = remaining(names);
      if (!left.isEmpty()) {
         return left;
      }
      List<String> all = distinct(names);
      List<String> done = picked(names);
      if (all.size() > 1 && !done.isEmpty()) {
         all.remove(done.get(done.size() - 1));
      }
      return all;
   }

   /** Picks one of {@link #pool}, starting a new time around first when everyone has had a turn. Null for no class. */
   public String pick(List<String> names, Random random) {
      List<String> pool = pool(names);
      if (pool.isEmpty()) {
         return null;
      }
      if (remaining(names).isEmpty()) {
         picked.clear();
      }
      String name = pool.get(random.nextInt(pool.size()));
      picked.add(name);
      return name;
   }

   /** Puts the newest pick back (they were away), so it can come up again. Returns the name, or null. */
   public String putBack(List<String> names) {
      List<String> still = picked(names);
      if (still.isEmpty()) {
         return null;
      }
      String name = still.get(still.size() - 1);
      picked.remove(name);
      return name;
   }

   /** Forgets every pick: the whole class is on the wheel again. */
   public void clear() {
      picked.clear();
   }

   public boolean isEmpty() {
      return picked.isEmpty();
   }

   /** For the class file: the picks, oldest first, separated by commas. */
   String text() {
      return String.join(",", picked);
   }

   /** Reads {@link #text()} back. */
   void load(String text) {
      picked.clear();
      for (String name : GroupMaker.parseNames(text)) {
         if (!picked.contains(name)) {
            picked.add(name);
         }
      }
   }

   private static List<String> distinct(List<String> names) {
      List<String> once = new ArrayList<>();
      for (String name : names) {
         if (!once.contains(name)) {
            once.add(name);
         }
      }
      return once;
   }
}
