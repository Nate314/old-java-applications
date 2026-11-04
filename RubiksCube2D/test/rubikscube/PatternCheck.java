package rubikscube;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks Patterns: that every pattern reads as quarter turns the app's keys can make, comes back to solved when its
 * turns are taken back, and looks like what it says (checkerboard, spots, plus signs, superflip), and that the
 * patterns made are saved and read back.
 */
public final class PatternCheck {
   private PatternCheck() {
   }

   public static void main(String[] args) throws IOException {
      check(Patterns.parse("R U2 F'").equals(Arrays.asList(
            Coach.faceTurn(new int[]{1, 0, 0}, true), Coach.faceTurn(new int[]{0, 1, 0}, true),
            Coach.faceTurn(new int[]{0, 1, 0}, true), Coach.faceTurn(new int[]{0, 0, 1}, false))), "notation");
      Set<String> keys = new HashSet<>();
      for (Patterns.Pattern pattern : Patterns.ALL) {
         check(keys.add(pattern.key), "unique key " + pattern.key);
         check(Patterns.byKey(pattern.key) == pattern, "byKey " + pattern.key);
         int[] made = Patterns.apply(Patterns.solved(), pattern.turns);
         check(!Arrays.equals(made, Patterns.solved()), pattern.name + " changes the cube");
         List<Integer> back = new ArrayList<>();
         for (int turn : pattern.turns) {
            back.add(Patterns.inverse(turn));
            check(hasKey(turn), pattern.name + ": every turn is a W/A/S/D move");
         }
         Collections.reverse(back);
         check(Arrays.equals(Patterns.apply(made, back), Patterns.solved()), pattern.name + " turns back to solved");
         for (int face = 0; face < 6; face++) {
            check(made[face * 9 + 4] == face, pattern.name + " keeps the centers");
         }
      }
      check(Patterns.ALL.size() == 7, "seven patterns (keys 1 to 7)");
      int[] board = made("checkerboard");
      int[] spots = made("spots");
      int[] plus = made("plus");
      int[] flip = made("superflip");
      for (int face = 0; face < 6; face++) {
         int center = face * 9 + 4;
         Set<Integer> edges = new HashSet<>();
         Set<Integer> corners = new HashSet<>();
         for (int index : new int[]{1, 3, 5, 7}) {
            edges.add(board[face * 9 + index]);
            check(spots[face * 9 + index] != spots[center], "spots: edges are not the center's color");
            check(plus[face * 9 + index] == plus[center], "plus: edges are the center's color");
            check(flip[face * 9 + index] != face, "superflip: every edge flipped");
         }
         for (int index : new int[]{0, 2, 6, 8}) {
            corners.add(board[face * 9 + index]);
            check(spots[face * 9 + index] == spots[face * 9 + 1], "spots: the ring is one color");
            check(plus[face * 9 + index] != plus[center], "plus: corners are another color");
            check(flip[face * 9 + index] == face, "superflip: corners home");
         }
         check(corners.equals(Collections.singleton(board[center])), "checkerboard: corners match the center");
         check(edges.size() == 1 && !edges.contains(board[center]), "checkerboard: edges are one other color");
      }
      // Made patterns are saved by key, in order, and unknown keys are dropped.
      File file = File.createTempFile("rubikscube-patterns", ".txt");
      file.deleteOnExit();
      LevelRecords records = LevelRecords.load(file);
      check(records.patternsMade().isEmpty(), "no patterns made at first");
      records.madePattern("spots");
      records.madePattern("superflip");
      records.madePattern("spots");
      check(new ArrayList<>(LevelRecords.load(file).patternsMade()).equals(Arrays.asList("spots", "superflip")),
            "patterns made survive a reload");
      try (Writer writer = new FileWriter(file)) {
         writer.write("level=3\npatterns=plus,nonsense, checkerboard\n1=3,1\n2=3,2\n");
      }
      LevelRecords read = LevelRecords.load(file);
      check(new ArrayList<>(read.patternsMade()).equals(Arrays.asList("plus", "checkerboard")), "unknown pattern keys dropped");
      check(read.stars(2) == 3 && read.lastLevel() == 3, "levels still read beside patterns");
      System.out.println("PatternCheck passed: " + Patterns.ALL.size() + " patterns");
   }

   private static int[] made(String key) {
      return Patterns.apply(Patterns.solved(), Patterns.byKey(key).turns);
   }

   /** True when some face, square and W/A/S/D key make that layer turn (so the app can point at it). */
   private static boolean hasKey(int turn) {
      for (String color : CubeGeometry.FACE_COLORS) {
         for (int cubie = 1; cubie <= 9; cubie++) {
            for (String key : CubeGeometry.KEYS) {
               if (CubeGeometry.turnOf(color, key, cubie).id() == turn) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private static void check(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError("PatternCheck failed: " + what);
      }
   }
}
