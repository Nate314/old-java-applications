package groupcreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Self-check for the roll call and keep-apart rules: who sits out, what the mix keeps apart, and the class file. */
public class ClassRulesCheck {
   public static void main(String[] args) throws Exception {
      Random random = new Random(5);
      List<String> twelve = GroupMaker.parseNames(Roster.EXAMPLE);

      // Away names sit out; marking again brings them back; everyone here clears it.
      ClassRules rules = new ClassRules();
      rules.toggleAway("Bob");
      rules.toggleAway("Erin");
      check(rules.present(twelve).size() == 10 && !rules.present(twelve).contains("Bob"), "away names sit out");
      check(rules.awayCount(twelve) == 2, "two away");
      rules.toggleAway("Bob");
      check(rules.present(twelve).contains("Bob") && rules.awayCount(twelve) == 1, "marking again brings Bob back");
      rules.everyoneHere();
      check(rules.present(twelve).size() == 12, "everyone here");

      // Rules toggle, ignore order and self-links, and only count when both names are in the class.
      check(rules.toggleApart("Alice", "Bob") && rules.apart("Bob", "Alice"), "a rule either way round");
      check(!rules.toggleApart("Carol", "Carol") && rules.rules(twelve).size() == 1, "no rule with yourself");
      check(!rules.toggleApart("Bob", "Alice") && !rules.apart("Alice", "Bob"), "a second toggle removes it");
      rules.toggleApart("Alice", "Zed");
      check(rules.rules(twelve).isEmpty() && rules.apartFrom("Alice", twelve).isEmpty(), "a name not in the class does not count");

      // With sizes that allow it, the mix never puts a kept-apart pair together, on the first round or later ones.
      for (int trial = 0; trial < 300; trial++) {
         ClassRules apart = new ClassRules();
         List<String> shuffled = new ArrayList<>(twelve);
         java.util.Collections.shuffle(shuffled, random);
         // Six pairs, and one name kept apart from three others.
         for (int i = 0; i < 6; i++) {
            apart.toggleApart(shuffled.get(2 * i), shuffled.get(2 * i + 1));
         }
         apart.toggleApart(shuffled.get(0), shuffled.get(2));
         apart.toggleApart(shuffled.get(0), shuffled.get(4));
         PartnerHistory history = new PartnerHistory();
         for (int round = 0; round < 4; round++) {
            List<List<String>> groups = FreshMix.mix(twelve, 3, 3, history, apart, random);
            check(apart.broken(groups).isEmpty(), "trial " + trial + " round " + (round + 1) + " broke a rule: " + groups);
            check(sizes(groups, 3, 3) && count(groups) == 12, "legal groups of three");
            history.add(groups);
         }
      }

      // Away names are not dealt, and the mix of those here still keeps rules.
      ClassRules today = new ClassRules();
      today.toggleAway("Grace");
      today.toggleAway("Heidi");
      today.toggleApart("Alice", "Bob");
      List<String> here = today.present(twelve);
      for (int trial = 0; trial < 200; trial++) {
         List<List<String>> groups = FreshMix.mix(here, 2, 3, new PartnerHistory(), today, random);
         check(count(groups) == 10 && !flat(groups).contains("Grace") && !flat(groups).contains("Heidi"), "away names not dealt");
         check(today.broken(groups).isEmpty(), "Alice and Bob apart");
      }

      // When the sizes leave no other way (two names, one group), the rule is broken and reported.
      ClassRules two = new ClassRules();
      two.toggleApart("a", "b");
      List<List<String>> forced = FreshMix.mix(Arrays.asList("a", "b"), 2, 2, new PartnerHistory(), two, random);
      check(two.broken(forced).size() == 1, "a forced pair is reported");

      // The class file keeps away names and rules, and skips bad apart lines.
      File file = File.createTempFile("groupcreator-rules", ".txt");
      file.deleteOnExit();
      Roster roster = Roster.load(file);
      roster.rules.toggleAway("Dave");
      roster.rules.toggleApart("Alice", "Bob");
      roster.rules.toggleApart("Carol", "Erin");
      roster.save();
      check(roster.text().contains("away=Dave\n") && roster.text().contains("apart=Alice,Bob\napart=Carol,Erin\n"), "file lines");
      try (java.io.Writer writer = new java.io.FileWriter(file, true)) {
         writer.write("apart=Frank\napart=Grace,Grace\napart=Bob,Alice\n");
      }
      Roster back = Roster.load(file);
      check(back.rules.isAway("Dave") && back.rules.awayCount(twelve) == 1, "away read back");
      check(back.rules.rules(twelve).size() == 2 && back.rules.apart("Erin", "Carol"), "rules read back, bad lines skipped");

      System.out.println("ClassRulesCheck passed");
   }

   private static boolean sizes(List<List<String>> groups, int min, int max) {
      for (List<String> group : groups) {
         if (group.size() < min || group.size() > max) {
            return false;
         }
      }
      return true;
   }

   private static int count(List<List<String>> groups) {
      return flat(groups).size();
   }

   private static List<String> flat(List<List<String>> groups) {
      List<String> all = new ArrayList<>();
      for (List<String> group : groups) {
         all.addAll(group);
      }
      return all;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
