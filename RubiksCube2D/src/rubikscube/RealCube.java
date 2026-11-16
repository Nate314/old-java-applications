package rubikscube;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether 54 painted sticker colors ({@link CubeGeometry} order, "none" for a square not painted yet) are a position
 * a real cube can be turned into, with its centers where they start, and if not, why, in words. A real cube has nine
 * squares of each color, every corner and edge once with its colors in a real piece's order, the corner twists
 * adding up to whole turns, an even number of flipped edges, and its corners and edges swapped the same number of
 * times over (both odd or both even).
 */
final class RealCube {
   static final String NONE = "none";
   /** Each corner slot's three stickers and each edge slot's two, the first on the top or bottom face (an edge in the
    * middle layer: on the front or back), a corner's other two going round it the same way for every corner. */
   private static final List<int[]> CORNERS = new ArrayList<>();
   private static final List<int[]> EDGES = new ArrayList<>();

   static {
      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               int zeros = (x == 0 ? 1 : 0) + (y == 0 ? 1 : 0) + (z == 0 ? 1 : 0);
               if (zeros > 1) {
                  continue;
               }
               int[] pos = {x, y, z};
               int[] first = y != 0 ? axis(1, y) : axis(2, z);
               List<int[]> others = new ArrayList<>();
               for (int k = 0; k < 3; k++) {
                  if (pos[k] != 0 && !(k == 1 && y != 0) && !(k == 2 && y == 0)) {
                     others.add(axis(k, pos[k]));
                  }
               }
               if (zeros == 0 && det(first, others.get(0), others.get(1)) < 0) {
                  others.add(others.remove(0));
               }
               int[] slot = new int[1 + others.size()];
               slot[0] = CubeGeometry.stickerAt(pos, first);
               for (int i = 0; i < others.size(); i++) {
                  slot[i + 1] = CubeGeometry.stickerAt(pos, others.get(i));
               }
               (zeros == 0 ? CORNERS : EDGES).add(slot);
            }
         }
      }
   }

   private RealCube() {
   }

   /** Why the cube cannot be real, or null when it can. */
   static String problem(String[] colors) {
      int unpainted = 0;
      int[] counts = new int[6];
      for (int s = 0; s < 54; s++) {
         int face = CubeGeometry.faceIndex(colors[s]);
         if (s % 9 == 4 && face != s / 9) {
            return "The centers stay as they are: hold the cube with " + CubeGeometry.FACE_COLORS[0] + " on top and "
                  + CubeGeometry.FACE_COLORS[1] + " in front.";
         }
         if (face < 0) {
            unpainted++;
         } else {
            counts[face]++;
         }
      }
      if (unpainted > 0) {
         return unpainted + (unpainted == 1 ? " square is" : " squares are") + " still gray: paint "
               + (unpainted == 1 ? "it" : "them") + " to match your cube.";
      }
      List<String> over = new ArrayList<>();
      List<String> under = new ArrayList<>();
      for (int f = 0; f < 6; f++) {
         if (counts[f] != 9) {
            (counts[f] > 9 ? over : under).add(counts[f] + " " + CubeGeometry.FACE_COLORS[f]);
         }
      }
      if (!over.isEmpty()) {
         return "A cube has 9 squares of each color. This one has " + String.join(" and ", over) + " but "
               + String.join(" and ", under) + ".";
      }
      int[] cornerOf = new int[8];
      String trouble = pieces(colors, CORNERS, cornerOf, "corner");
      if (trouble != null) {
         return trouble;
      }
      int[] edgeOf = new int[12];
      trouble = pieces(colors, EDGES, edgeOf, "edge");
      if (trouble != null) {
         return trouble;
      }
      int twist = 0;
      for (int c = 0; c < 8; c++) {
         twist += rotationOf(colors, CORNERS.get(c), CORNERS.get(cornerOf[c]));
      }
      int flips = 0;
      for (int e = 0; e < 12; e++) {
         flips += rotationOf(colors, EDGES.get(e), EDGES.get(edgeOf[e]));
      }
      String taken = " Turning never does that: check those squares (a cube taken apart and put back can get this way).";
      if (twist % 3 != 0) {
         return "One corner is twisted in its place." + taken;
      }
      if (flips % 2 != 0) {
         return "One edge is flipped in its place." + taken;
      }
      if (odd(cornerOf) != odd(edgeOf)) {
         return "Two pieces are swapped." + taken;
      }
      return null;
   }

   /**
    * Finds which piece sits in each slot ({@code pieceOf[slot]} is the slot it belongs in), or says what is wrong: a
    * set of colors no piece has, colors in an order no real piece has, or a piece found twice.
    */
   private static String pieces(String[] colors, List<int[]> slots, int[] pieceOf, String kind) {
      boolean[] seen = new boolean[slots.size()];
      for (int i = 0; i < slots.size(); i++) {
         int[] slot = slots.get(i);
         pieceOf[i] = -1;
         boolean sameSet = false;
         for (int j = 0; j < slots.size() && pieceOf[i] < 0; j++) {
            int[] home = slots.get(j);
            if (rotationOf(colors, slot, home) >= 0) {
               pieceOf[i] = j;
            } else if (sameColors(colors, slot, home)) {
               sameSet = true;
            }
         }
         String named = names(colors, slot);
         if (pieceOf[i] < 0) {
            return sameSet ? "The " + named + " corner has its colors the wrong way round: no real corner does. Check its"
                  + " three squares." : "No " + kind + " of a cube is " + named + ". Check that " + kind + "'s squares.";
         }
         if (seen[pieceOf[i]]) {
            return "Two " + kind + "s are " + named + ", so one of them has a wrong square.";
         }
         seen[pieceOf[i]] = true;
      }
      return null;
   }

   /** How far round the piece in {@code slot} is turned from sitting home in {@code home}, or -1 if it is not that piece. */
   private static int rotationOf(String[] colors, int[] slot, int[] home) {
      for (int k = 0; k < slot.length; k++) {
         boolean match = true;
         for (int i = 0; i < slot.length && match; i++) {
            match = colors[slot[(i + k) % slot.length]].equals(CubeGeometry.FACE_COLORS[home[i] / 9]);
         }
         if (match) {
            return k;
         }
      }
      return -1;
   }

   private static boolean sameColors(String[] colors, int[] slot, int[] home) {
      List<String> left = new ArrayList<>();
      for (int h : home) {
         left.add(CubeGeometry.FACE_COLORS[h / 9]);
      }
      for (int s : slot) {
         if (!left.remove(colors[s])) {
            return false;
         }
      }
      return true;
   }

   private static String names(String[] colors, int[] slot) {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < slot.length; i++) {
         text.append(i == 0 ? "" : i == slot.length - 1 ? " and " : ", ").append(colors[slot[i]]);
      }
      return text.toString();
   }

   /** Whether a permutation is odd (an odd number of swaps). */
   private static boolean odd(int[] permutation) {
      int cycles = 0;
      boolean[] visited = new boolean[permutation.length];
      for (int i = 0; i < permutation.length; i++) {
         if (!visited[i]) {
            cycles++;
            for (int j = i; !visited[j]; j = permutation[j]) {
               visited[j] = true;
            }
         }
      }
      return (permutation.length - cycles) % 2 == 1;
   }

   private static int[] axis(int k, int sign) {
      int[] v = new int[3];
      v[k] = sign;
      return v;
   }

   private static int det(int[] a, int[] b, int[] c) {
      return a[0] * (b[1] * c[2] - b[2] * c[1]) - a[1] * (b[0] * c[2] - b[2] * c[0]) + a[2] * (b[0] * c[1] - b[1] * c[0]);
   }
}
