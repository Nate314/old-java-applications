package groupcreator;

import java.util.ArrayList;
import java.util.List;

/**
 * Today's roll call and the class's keep-apart rules. Names marked away sit out the groups and the wheel (their
 * history stays). A keep-apart rule asks the mix never to put two names in one group. Names are matched as typed;
 * a name that leaves the names field simply stops counting, and comes back with its marks if it is typed again.
 */
public final class ClassRules {
   private final List<String> away = new ArrayList<>();
   /** Each rule as a two-name array, the names in the order they were linked. */
   private final List<String[]> apart = new ArrayList<>();

   public boolean isAway(String name) {
      return away.contains(name);
   }

   public void toggleAway(String name) {
      if (!away.remove(name)) {
         away.add(name);
      }
   }

   /** Marks everyone here again. */
   public void everyoneHere() {
      away.clear();
   }

   /** The names (each once, in class order) that are not marked away. */
   public List<String> present(List<String> names) {
      List<String> here = new ArrayList<>();
      for (String name : names) {
         if (!away.contains(name) && !here.contains(name)) {
            here.add(name);
         }
      }
      return here;
   }

   /** How many of the class's names are marked away. */
   public int awayCount(List<String> names) {
      int count = 0;
      for (String name : away) {
         count += names.contains(name) ? 1 : 0;
      }
      return count;
   }

   public boolean apart(String a, String b) {
      return find(a, b) >= 0;
   }

   /** Adds the rule that a and b are kept apart, or removes it when there is one. Returns true when it was added. */
   public boolean toggleApart(String a, String b) {
      if (a.equals(b)) {
         return false;
      }
      int at = find(a, b);
      if (at >= 0) {
         apart.remove(at);
         return false;
      }
      apart.add(new String[] {a, b});
      return true;
   }

   /** The rules whose two names are both in {@code names}, oldest first. */
   public List<String[]> rules(List<String> names) {
      List<String[]> rules = new ArrayList<>();
      for (String[] pair : apart) {
         if (names.contains(pair[0]) && names.contains(pair[1])) {
            rules.add(pair);
         }
      }
      return rules;
   }

   /** The names {@code name} is kept apart from, among {@code names}. */
   public List<String> apartFrom(String name, List<String> names) {
      List<String> others = new ArrayList<>();
      for (String[] pair : rules(names)) {
         if (pair[0].equals(name)) {
            others.add(pair[1]);
         } else if (pair[1].equals(name)) {
            others.add(pair[0]);
         }
      }
      return others;
   }

   /** The rules that {@code groups} break: two kept-apart names in one group. */
   public List<String[]> broken(List<List<String>> groups) {
      List<String[]> pairs = new ArrayList<>();
      for (List<String> group : groups) {
         for (String[] pair : rules(group)) {
            pairs.add(pair);
         }
      }
      return pairs;
   }

   /** For the class file: the away names, separated by commas. */
   String awayText() {
      return String.join(",", away);
   }

   /** Reads {@link #awayText()} back. */
   void loadAway(String text) {
      away.clear();
      for (String name : GroupMaker.parseNames(text)) {
         if (!away.contains(name)) {
            away.add(name);
         }
      }
   }

   /** For the class file: one "a,b" entry per rule. */
   List<String> apartText() {
      List<String> lines = new ArrayList<>();
      for (String[] pair : apart) {
         lines.add(pair[0] + "," + pair[1]);
      }
      return lines;
   }

   /** Reads one entry of {@link #apartText()} back; anything but two different names is skipped. */
   void loadApart(String text) {
      List<String> pair = GroupMaker.parseNames(text);
      if (pair.size() == 2 && !apart(pair.get(0), pair.get(1))) {
         toggleApart(pair.get(0), pair.get(1));
      }
   }

   private int find(String a, String b) {
      for (int i = 0; i < apart.size(); i++) {
         String[] pair = apart.get(i);
         if (pair[0].equals(a) && pair[1].equals(b) || pair[0].equals(b) && pair[1].equals(a)) {
            return i;
         }
      }
      return -1;
   }
}
