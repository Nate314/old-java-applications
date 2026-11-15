package rubikscube;

import java.util.Random;

/** Self-check for My cube: which painted cubes a real cube can be. Run with java -ea. */
public class RealCubeCheck {
   public static void main(String[] args) {
      String[] solved = new String[54];
      for (int s = 0; s < 54; s++) {
         solved[s] = CubeGeometry.FACE_COLORS[s / 9];
      }
      check(RealCube.problem(solved) == null, "a solved cube is real");

      Random random = new Random(5);
      String[] mixed = solved;
      for (int run = 0; run < 2000; run++) {
         String[] cube = solved.clone();
         for (int i = 0, turns = 1 + random.nextInt(60); i < turns; i++) {
            int id;
            do {
               id = random.nextInt(18);
            } while (id % 6 / 2 == 1);
            cube = apply(cube, id);
         }
         String problem = RealCube.problem(cube);
         check(problem == null, "run " + run + " is real, not: " + problem);
         if (run % 10 == 0) {
            // Teach me must take any cube that passes.
            check(Coach.plan(cube) != null, "the coach solves run " + run);
         }
         mixed = cube;
      }

      String[] gray = solved.clone();
      gray[0] = RealCube.NONE;
      gray[10] = RealCube.NONE;
      check(says(gray, "2 squares are still gray"), "unpainted squares are counted");

      String[] center = solved.clone();
      center[4] = "red";
      check(says(center, "centers stay"), "a center may not change");

      String[] count = mixed.clone();
      count[0] = count[0].equals("white") ? "yellow" : "white";
      check(says(count, "9 squares of each color"), "a tenth square of one color is caught");

      int[] corner = slot(new int[]{1, 1, 1});
      check(says(rotate(mixed, corner), "corner is twisted"), "a twisted corner is caught");
      check(says(swap(mixed, corner[1], corner[2]), "wrong way round"), "a mirrored corner is caught");

      int[] edge = slot(new int[]{0, 1, 1});
      check(says(swap(mixed, edge[0], edge[1]), "edge is flipped"), "a flipped edge is caught");

      int[] edge2 = slot(new int[]{1, 1, 0});
      String[] two = swapPieces(solved, edge, edge2);
      check(says(two, "Two pieces are swapped"), "two edges swapped is caught");
      int[] corner2 = slot(new int[]{-1, 1, 1});
      check(RealCube.problem(swapPieces(two, corner, corner2)) == null, "two edges and two corners swapped is real");

      String[] doubled = solved.clone();
      // The top front edge painted as a copy of the top right edge: two of one, none of the other.
      doubled[edge[0]] = solved[edge2[0]];
      doubled[edge[1]] = solved[edge2[1]];
      doubled[corner[0]] = solved[corner[0]];
      check(RealCube.problem(doubled) != null, "a doubled edge is caught");
      // The top front edge's red traded for the bottom front edge's green: blue and green, and red and red.
      String[] none = swap(solved, edge[1], slot(new int[]{0, -1, 1})[0]);
      check(says(none, "No edge of a cube is"), "an edge no cube has is caught");
      // Any two squares of different colors traded (one painted wrong both ways) is never a real cube.
      for (int run = 0; run < 2000; run++) {
         int a = random.nextInt(54);
         int b = random.nextInt(54);
         if (a % 9 != 4 && b % 9 != 4 && !mixed[a].equals(mixed[b])) {
            check(RealCube.problem(swap(mixed, a, b)) != null, "trading squares " + a + " and " + b + " is caught");
         }
      }
      System.out.println("RealCubeCheck passed");
   }

   private static boolean says(String[] cube, String words) {
      String problem = RealCube.problem(cube);
      if (problem == null || !problem.contains(words)) {
         System.out.println("Expected \"" + words + "\", got: " + problem);
         return false;
      }
      return true;
   }

   /** The stickers on the cubie at pos, top or bottom (else front or back) first, as {@link RealCube} reads them. */
   private static int[] slot(int[] pos) {
      int[] out = new int[3];
      int n = 0;
      int[][] normals = {{0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};
      for (int[] normal : normals) {
         int s = CubeGeometry.stickerAt(pos, normal);
         if (s >= 0) {
            out[n++] = s;
         }
      }
      return java.util.Arrays.copyOf(out, n);
   }

   private static String[] rotate(String[] cube, int[] slot) {
      String[] out = cube.clone();
      for (int i = 0; i < slot.length; i++) {
         out[slot[(i + 1) % slot.length]] = cube[slot[i]];
      }
      return out;
   }

   private static String[] swap(String[] cube, int a, int b) {
      String[] out = cube.clone();
      out[a] = cube[b];
      out[b] = cube[a];
      return out;
   }

   /** Two pieces trade places, each keeping which of its colors is on the top or bottom (or front or back). */
   private static String[] swapPieces(String[] cube, int[] a, int[] b) {
      String[] out = cube.clone();
      out[a[0]] = cube[b[0]];
      out[b[0]] = cube[a[0]];
      // The other stickers, matched so the piece keeps its turn: a corner's two others by RealCube's own order.
      String[] first = RealCubeCheck.ordered(cube, a);
      String[] second = RealCubeCheck.ordered(cube, b);
      int[] oa = RealCubeCheck.orderedSlot(a);
      int[] ob = RealCubeCheck.orderedSlot(b);
      for (int i = 0; i < oa.length; i++) {
         out[oa[i]] = second[i];
         out[ob[i]] = first[i];
      }
      return out;
   }

   private static String[] ordered(String[] cube, int[] slot) {
      int[] order = orderedSlot(slot);
      String[] out = new String[order.length];
      for (int i = 0; i < order.length; i++) {
         out[i] = cube[order[i]];
      }
      return out;
   }

   /** The slot's stickers in the order going round the cubie the same way for every corner. */
   private static int[] orderedSlot(int[] slot) {
      if (slot.length == 3 && det(CubeGeometry.NORMAL[slot[0]], CubeGeometry.NORMAL[slot[1]], CubeGeometry.NORMAL[slot[2]]) < 0) {
         return new int[]{slot[0], slot[2], slot[1]};
      }
      return slot;
   }

   private static int det(int[] a, int[] b, int[] c) {
      return a[0] * (b[1] * c[2] - b[2] * c[1]) - a[1] * (b[0] * c[2] - b[2] * c[0]) + a[2] * (b[0] * c[1] - b[1] * c[0]);
   }

   private static String[] apply(String[] cube, int id) {
      int[] dest = CubeGeometry.destinations(id);
      String[] out = new String[54];
      for (int s = 0; s < 54; s++) {
         out[dest[s]] = cube[s];
      }
      return out;
   }

   private static void check(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError(what);
      }
   }
}
