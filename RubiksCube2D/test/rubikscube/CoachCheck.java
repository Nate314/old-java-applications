package rubikscube;

import java.util.List;
import java.util.Random;

/** Self-check for Teach me (the beginner's method coach). Run with java -ea. */
public class CoachCheck {
   public static void main(String[] args) {
      String[] solved = new String[54];
      for (int s = 0; s < 54; s++) {
         solved[s] = CubeGeometry.FACE_COLORS[s / 9];
      }
      check(Coach.plan(solved).isEmpty(), "a solved cube needs no steps");

      // Clockwise as seen facing the layer: the turn of the top (+y) layer that carries the front's top row left.
      int up = Coach.faceTurn(new int[]{0, 1, 0}, true);
      check(Coach.clockwise(up), "faceTurn and clockwise agree");
      int frontTop = CubeGeometry.stickerAt(new int[]{0, 1, 1}, new int[]{0, 0, 1});
      int dest = CubeGeometry.destinations(up)[frontTop];
      check(CubeGeometry.NORMAL[dest][0] == -1, "U clockwise moves the front's top row to the left side");

      Random random = new Random(11);
      int longest = 0;
      long total = 0;
      long start = System.nanoTime();
      int runs = 3000;
      for (int run = 0; run < runs; run++) {
         String[] cube = solved.clone();
         // Any turns at all, middle layers too, so the centers end up anywhere.
         int mix = run < 20 ? run + 1 : 40 + random.nextInt(60);
         for (int i = 0; i < mix; i++) {
            cube = apply(cube, random.nextInt(18));
         }
         List<Coach.Step> steps = Coach.plan(cube);
         int stage = 0;
         int count = 0;
         for (Coach.Step step : steps) {
            check(step.stage >= stage, "stages come in order");
            check(!step.turns.isEmpty(), "every step has turns");
            check(!step.text.isEmpty(), "every step says what it is for");
            stage = step.stage;
            for (int id : step.turns) {
               check(id % 6 / 2 != 1, "only outer layers turn");
               cube = apply(cube, id);
               count++;
            }
         }
         check(isSolved(cube), "run " + run + " ends solved");
         longest = Math.max(longest, count);
         total += count;
      }
      long millis = (System.nanoTime() - start) / 1000000;
      System.out.println("CoachCheck passed: " + runs + " cubes, " + total / runs + " turns on average, longest "
            + longest + ", " + millis + " ms");
   }

   private static String[] apply(String[] cube, int id) {
      int[] dest = CubeGeometry.destinations(id);
      String[] out = new String[54];
      for (int s = 0; s < 54; s++) {
         out[dest[s]] = cube[s];
      }
      return out;
   }

   private static boolean isSolved(String[] cube) {
      for (int f = 0; f < 6; f++) {
         for (int i = 1; i < 9; i++) {
            if (!cube[f * 9 + i].equals(cube[f * 9])) {
               return false;
            }
         }
      }
      return true;
   }

   private static void check(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError(what);
      }
   }
}
