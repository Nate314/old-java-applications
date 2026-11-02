package groupcreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every round of groups made so far, oldest first, and from them who has been in a group with whom. Names are
 * matched by their text, so a name typed the same way in a later roster is the same person.
 */
public final class PartnerHistory {
   private final List<List<List<String>>> rounds = new ArrayList<>();
   /** Times together per pair, keyed by {@link #key}; rebuilt when the rounds change. */
   private Map<String, Integer> counts;

   public int size() {
      return rounds.size();
   }

   public boolean isEmpty() {
      return rounds.isEmpty();
   }

   /** Round {@code number}, counted from 1. */
   public List<List<String>> round(int number) {
      return rounds.get(number - 1);
   }

   /** The newest round, or an empty list before the first one. */
   public List<List<String>> last() {
      return rounds.isEmpty() ? Collections.<List<String>>emptyList() : rounds.get(rounds.size() - 1);
   }

   public void add(List<List<String>> groups) {
      rounds.add(copy(groups));
      counts = null;
   }

   /** Replaces the newest round (adds it when there is none). */
   public void replaceLast(List<List<String>> groups) {
      if (!rounds.isEmpty()) {
         rounds.remove(rounds.size() - 1);
      }
      add(groups);
   }

   public void clear() {
      rounds.clear();
      counts = null;
   }

   /** A history of only the first {@code count} rounds. */
   public PartnerHistory first(int count) {
      PartnerHistory history = new PartnerHistory();
      for (int i = 0; i < count && i < rounds.size(); i++) {
         history.add(rounds.get(i));
      }
      return history;
   }

   /** How many rounds put {@code a} and {@code b} in one group. */
   public int together(String a, String b) {
      if (a.equals(b)) {
         return 0;
      }
      if (counts == null) {
         counts = new HashMap<>();
         for (List<List<String>> round : rounds) {
            for (List<String> group : round) {
               for (int i = 0; i < group.size(); i++) {
                  for (int j = i + 1; j < group.size(); j++) {
                     if (!group.get(i).equals(group.get(j))) {
                        counts.merge(key(group.get(i), group.get(j)), 1, Integer::sum);
                     }
                  }
               }
            }
         }
      }
      Integer count = counts.get(key(a, b));
      return count == null ? 0 : count;
   }

   /** The numbers (from 1) of the rounds that put {@code a} and {@code b} in one group. */
   public List<Integer> roundsTogether(String a, String b) {
      List<Integer> numbers = new ArrayList<>();
      if (a.equals(b)) {
         return numbers;
      }
      for (int r = 0; r < rounds.size(); r++) {
         for (List<String> group : rounds.get(r)) {
            if (group.contains(a) && group.contains(b)) {
               numbers.add(r + 1);
               break;
            }
         }
      }
      return numbers;
   }

   /** The pairs that share a group in {@code groups} and already shared one in this history, as two-name arrays. */
   public List<String[]> repeats(List<List<String>> groups) {
      List<String[]> pairs = new ArrayList<>();
      for (List<String> group : groups) {
         for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
               if (together(group.get(i), group.get(j)) > 0) {
                  pairs.add(new String[] {group.get(i), group.get(j)});
               }
            }
         }
      }
      return pairs;
   }

   private static String key(String a, String b) {
      return a.compareTo(b) < 0 ? a + '\n' + b : b + '\n' + a;
   }

   static List<List<String>> copy(List<List<String>> groups) {
      List<List<String>> copy = new ArrayList<>();
      for (List<String> group : groups) {
         copy.add(new ArrayList<>(group));
      }
      return copy;
   }
}
