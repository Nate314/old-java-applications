package groupcreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Self-check for the roles: which places have one, that rotation keeps the groups, and that roles go round. */
public class RolesCheck {
   public static void main(String[] args) throws Exception {
      Random random = new Random(7);

      // A group of one has no role; the first five places have the five roles in order, and later places none.
      check(Roles.of(0, 1) == -1, "a group of one has no Leader");
      check(Roles.of(0, 2) == 0 && Roles.of(1, 2) == 1, "a pair is a Leader and a Scribe");
      check(Roles.of(4, 6) == 4 && Roles.of(5, 6) == -1, "a sixth place has no role");

      PartnerHistory history = new PartnerHistory();
      history.add(groups("a,b,c|d,e"));
      history.add(groups("b,a|c,d,e"));
      history.add(groups("a|b,c"));
      check(Roles.roundsAs(history, "a", 0).equals(Arrays.asList(1)), "a led round 1 only (alone in round 3 is no role)");
      check(Roles.roundsAs(history, "a", 1).equals(Arrays.asList(2)), "a was Scribe in round 2");
      check(Roles.roundsAs(history, "e", 2).equals(Arrays.asList(2)), "e was Timer in round 2");

      // Rotation only reorders: every group keeps its names.
      List<String> twelve = GroupMaker.parseNames(Roster.EXAMPLE);
      for (int trial = 0; trial < 100; trial++) {
         List<List<String>> mixed = FreshMix.mix(twelve, 2, 5, history, random);
         List<List<String>> rotated = Roles.rotate(mixed, history, random);
         check(rotated.size() == mixed.size(), "same number of groups");
         for (int g = 0; g < mixed.size(); g++) {
            List<String> a = new ArrayList<>(mixed.get(g));
            List<String> b = new ArrayList<>(rotated.get(g));
            Collections.sort(a);
            Collections.sort(b);
            check(a.equals(b), "group " + g + " changed: " + mixed.get(g) + " to " + rotated.get(g));
         }
      }

      // A team of four kept together for four rounds: each person has each role once.
      List<String> four = Arrays.asList("w", "x", "y", "z");
      for (int trial = 0; trial < 300; trial++) {
         PartnerHistory team = new PartnerHistory();
         for (int round = 0; round < 4; round++) {
            team.add(Roles.rotate(Collections.singletonList(four), team, random));
         }
         for (String name : four) {
            for (int role = 0; role < 4; role++) {
               check(Roles.roundsAs(team, name, role).size() == 1, name + " was not " + Roles.NAMES[role] + " exactly once");
            }
         }
      }

      // Twelve in threes for three fresh rounds (each has all three roles once at best): far fewer repeated roles than a plain mix.
      int rotatedRepeats = 0;
      int plainRepeats = 0;
      for (int trial = 0; trial < 50; trial++) {
         PartnerHistory withRoles = new PartnerHistory();
         PartnerHistory plain = new PartnerHistory();
         for (int round = 0; round < 3; round++) {
            List<List<String>> a = Roles.rotate(FreshMix.mix(twelve, 3, 3, withRoles, random), withRoles, random);
            rotatedRepeats += repeats(a, withRoles);
            withRoles.add(a);
            List<List<String>> b = FreshMix.mix(twelve, 3, 3, plain, random);
            plainRepeats += repeats(b, plain);
            plain.add(b);
         }
      }
      check(rotatedRepeats * 3 < plainRepeats, "rotated rounds repeated " + rotatedRepeats + " roles, plain ones " + plainRepeats);

      // The class file keeps roles=off, and leaves the line out when the roles are shown.
      File file = File.createTempFile("groupcreator", ".txt");
      file.deleteOnExit();
      Roster roster = Roster.load(file);
      check(roster.roles && !roster.text().contains("roles="), "roles are shown by default");
      roster.roles = false;
      roster.save();
      check(!Roster.load(file).roles, "roles=off round trips");

      System.out.println("RolesCheck passed");
   }

   /** How many role holders in groups had the same role in an earlier round of history. */
   private static int repeats(List<List<String>> groups, PartnerHistory history) {
      int count = 0;
      for (List<String> group : groups) {
         for (int k = 0; k < group.size(); k++) {
            int role = Roles.of(k, group.size());
            if (role >= 0 && !Roles.roundsAs(history, group.get(k), role).isEmpty()) {
               count++;
            }
         }
      }
      return count;
   }

   private static List<List<String>> groups(String text) {
      List<List<String>> groups = new ArrayList<>();
      for (String group : text.split("\\|")) {
         groups.add(GroupMaker.parseNames(group));
      }
      return groups;
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
