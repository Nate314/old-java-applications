package groupcreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Self-check for the fresh mix, the partner history and the class file. Run with java -ea. */
public class FreshMixCheck {
   public static void main(String[] args) throws Exception {
      Random random = new Random(7);

      // With no history the mix is a plain shuffle with legal sizes.
      List<String> twelve = GroupMaker.parseNames(Roster.EXAMPLE);
      for (int i = 0; i < 200; i++) {
         List<List<String>> groups = FreshMix.mix(twelve, 2, 3, new PartnerHistory(), random);
         checkSplit(groups, twelve, 2, 3);
      }

      // Four people in pairs: the three rounds of a round robin, with no pair repeated, every time.
      List<String> four = Arrays.asList("a", "b", "c", "d");
      for (int trial = 0; trial < 300; trial++) {
         PartnerHistory history = new PartnerHistory();
         for (int round = 0; round < 3; round++) {
            List<List<String>> groups = FreshMix.mix(four, 2, 2, history, random);
            checkSplit(groups, four, 2, 2);
            check(history.repeats(groups).isEmpty(), "round " + (round + 1) + " of 4 in pairs repeated a pair");
            history.add(groups);
         }
         // A fourth round has to repeat, and repeats only pairs met once.
         List<List<String>> fourth = FreshMix.mix(four, 2, 2, history, random);
         check(history.repeats(fourth).size() == 2, "4th round should repeat exactly 2 pairs");
      }

      // Twelve in threes, then in threes again: the second round never repeats a pair.
      for (int trial = 0; trial < 100; trial++) {
         PartnerHistory history = new PartnerHistory();
         history.add(FreshMix.mix(twelve, 3, 3, history, random));
         List<List<String>> second = FreshMix.mix(twelve, 3, 3, history, random);
         checkSplit(second, twelve, 3, 3);
         check(history.repeats(second).isEmpty(), "12 in threes repeated a pair in round 2: " + second);
      }
      // Nine in threes over four rounds can meet everyone once (an affine plane); the search finds it most times.
      List<String> nine = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i");
      int perfect = 0;
      for (int trial = 0; trial < 50; trial++) {
         PartnerHistory history = new PartnerHistory();
         int repeats = 0;
         for (int round = 0; round < 4; round++) {
            List<List<String>> groups = FreshMix.mix(nine, 3, 3, history, random);
            repeats += history.repeats(groups).size();
            history.add(groups);
         }
         perfect += repeats == 0 ? 1 : 0;
      }
      check(perfect >= 25, "nine in threes: only " + perfect + " of 50 four-round runs had no repeats");

      // The fresh mix never costs more than the plain shuffle it starts from would on average.
      PartnerHistory earlier = new PartnerHistory();
      for (int round = 0; round < 6; round++) {
         earlier.add(GroupMaker.makeGroups(twelve, 2, 3, random));
      }
      int fresh = 0;
      int plain = 0;
      for (int i = 0; i < 100; i++) {
         fresh += earlier.repeats(FreshMix.mix(twelve, 2, 3, earlier, random)).size();
         plain += earlier.repeats(GroupMaker.makeGroups(twelve, 2, 3, random)).size();
      }
      check(fresh < plain, "fresh mix repeated " + fresh + " pairs, a plain shuffle " + plain);

      // Impossible splits are still refused.
      try {
         FreshMix.mix(Arrays.asList("a", "b", "c", "d", "e"), 3, 4, earlier, random);
         throw new AssertionError("accepted 5 names in groups of 3 to 4");
      } catch (IllegalArgumentException expected) {
         // as makeGroups
      }

      // History counts and round numbers.
      PartnerHistory history = new PartnerHistory();
      history.add(groups("a,b|c,d"));
      history.add(groups("a,c|b,d"));
      history.add(groups("a,b,c|d"));
      check(history.together("a", "b") == 2 && history.together("b", "a") == 2, "a and b twice");
      check(history.together("a", "a") == 0, "nobody pairs with themselves");
      check(history.roundsTogether("a", "c").equals(Arrays.asList(2, 3)), "a and c in rounds 2 and 3");
      check(history.first(1).together("a", "c") == 0, "first(1) has only round 1");
      history.replaceLast(groups("a,d|b,c"));
      check(history.size() == 3 && history.together("a", "d") == 1 && history.together("a", "b") == 1, "replaceLast");

      // The class file keeps the fields and the rounds.
      File file = File.createTempFile("groupcreator", ".txt");
      file.deleteOnExit();
      Roster roster = Roster.load(file);
      check(roster.names.equals(Roster.EXAMPLE) && roster.history.isEmpty(), "empty file gives the example class");
      roster.names = "Ann, Bo,Cy";
      roster.min = "1";
      roster.max = "2";
      roster.history.add(groups("Ann,Bo|Cy"));
      roster.history.add(groups("Ann|Bo,Cy"));
      roster.save();
      Roster back = Roster.load(file);
      check(back.names.equals("Ann, Bo,Cy") && back.min.equals("1") && back.max.equals("2"), "fields round trip");
      check(back.history.size() == 2 && back.history.round(2).equals(groups("Ann|Bo,Cy")), "rounds round trip");
      check(back.text().equals(roster.text()), "same text");

      System.out.println("FreshMixCheck passed");
   }

   private static List<List<String>> groups(String text) {
      List<List<String>> groups = new ArrayList<>();
      for (String group : text.split("\\|")) {
         groups.add(GroupMaker.parseNames(group));
      }
      return groups;
   }

   private static void checkSplit(List<List<String>> groups, List<String> names, int min, int max) {
      List<String> seen = new ArrayList<>();
      for (List<String> group : groups) {
         check(group.size() >= min && group.size() <= max, "group size " + group.size());
         seen.addAll(group);
      }
      List<String> sorted = new ArrayList<>(names);
      sorted.sort(null);
      seen.sort(null);
      check(seen.equals(sorted), "names lost or duplicated: " + groups);
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
