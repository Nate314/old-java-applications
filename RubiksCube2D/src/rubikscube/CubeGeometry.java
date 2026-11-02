package rubikscube;

/**
 * The net folded into a real cube. Sticker {@code s = face * 9 + index} uses the face order of {@link Cube#faces()}
 * and the index of {@link Face#stickers}. {@link #POS} is its cubie (coordinates -1 to 1: x right, y up, z toward
 * the front face) and {@link #NORMAL} points out of its face. A W/A/S/D move is the quarter turn of one layer that
 * carries the selected square up, left, down or right as the net shows it ({@link #turnOf}).
 */
final class CubeGeometry {
   static final String[] FACE_COLORS = {"blue", "red", "yellow", "green", "orange", "white"};
   static final String[] KEYS = {"w", "a", "s", "d"};
   /** The net cell (row * 3 + column, 1 based) of each sticker index, per face: the layouts {@link Cube} uses. */
   private static final int[][] LAYOUTS = {
      {9, 8, 7, 6, 5, 4, 3, 2, 1}, {1, 2, 3, 4, 5, 6, 7, 8, 9}, {7, 4, 1, 8, 5, 2, 9, 6, 3},
      {1, 2, 3, 4, 5, 6, 7, 8, 9}, {1, 2, 3, 4, 5, 6, 7, 8, 9}, {3, 6, 9, 2, 5, 8, 1, 4, 7}};
   /** Per face: its normal, then the 3D directions of one net row down and one net column right on it. The net is
    * folded around the front face: top above it, bottom below it and the back below the bottom, left and right. */
   private static final int[][][] FRAMES = {
      {{0, 1, 0}, {0, 0, 1}, {1, 0, 0}},
      {{0, 0, 1}, {0, -1, 0}, {1, 0, 0}},
      {{1, 0, 0}, {0, -1, 0}, {0, 0, -1}},
      {{0, -1, 0}, {0, 0, -1}, {1, 0, 0}},
      {{0, 0, -1}, {0, 1, 0}, {1, 0, 0}},
      {{-1, 0, 0}, {0, -1, 0}, {0, 0, 1}}};
   static final int[][] POS = new int[54][];
   static final int[][] NORMAL = new int[54][];
   /** For each of the 18 turns (see {@link Turn#id}), where each sticker goes. */
   private static final int[][] DEST = new int[18][54];

   /** A quarter turn of one layer: the cubies whose {@code axis} coordinate is {@code layer}, by 90 degrees about
    * that axis, counterclockwise seen from its positive end when {@code dir} is 1. */
   static final class Turn {
      final int axis;
      final int layer;
      final int dir;

      Turn(int axis, int layer, int dir) {
         this.axis = axis;
         this.layer = layer;
         this.dir = dir;
      }

      int id() {
         return this.axis * 6 + (this.layer + 1) * 2 + (this.dir > 0 ? 1 : 0);
      }

      boolean moves(int sticker) {
         return POS[sticker][this.axis] == this.layer;
      }
   }

   static {
      for (int face = 0; face < 6; face++) {
         int[][] frame = FRAMES[face];
         for (int index = 0; index < 9; index++) {
            int cell = LAYOUTS[face][index] - 1;
            int down = cell / 3 - 1;
            int right = cell % 3 - 1;
            int[] pos = new int[3];
            for (int k = 0; k < 3; k++) {
               pos[k] = frame[0][k] + down * frame[1][k] + right * frame[2][k];
            }
            POS[face * 9 + index] = pos;
            NORMAL[face * 9 + index] = frame[0];
         }
      }
      for (int axis = 0; axis < 3; axis++) {
         for (int layer = -1; layer <= 1; layer++) {
            for (int dir = -1; dir <= 1; dir += 2) {
               Turn turn = new Turn(axis, layer, dir);
               for (int s = 0; s < 54; s++) {
                  DEST[turn.id()][s] = turn.moves(s) ? stickerAt(rotate(POS[s], axis, dir), rotate(NORMAL[s], axis, dir)) : s;
               }
            }
         }
      }
   }

   private CubeGeometry() {
   }

   static int faceIndex(String color) {
      for (int face = 0; face < 6; face++) {
         if (FACE_COLORS[face].equals(color)) {
            return face;
         }
      }
      return -1;
   }

   /** The sticker at that cubie position on the face with that normal, or -1. */
   static int stickerAt(int[] pos, int[] normal) {
      for (int s = 0; s < 54; s++) {
         if (same(POS[s], pos) && same(NORMAL[s], normal)) {
            return s;
         }
      }
      return -1;
   }

   private static boolean same(int[] a, int[] b) {
      return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
   }

   /** {@code v} turned a quarter about {@code axis}, counterclockwise seen from its positive end when dir is 1. */
   static int[] rotate(int[] v, int axis, int dir) {
      int a = (axis + 1) % 3;
      int b = (axis + 2) % 3;
      int[] out = v.clone();
      out[a] = -dir * v[b];
      out[b] = dir * v[a];
      return out;
   }

   /** The 3D direction a key moves a square of that face in the net: W up, A left, S down, D right. */
   static int[] keyDirection(int face, String key) {
      int[][] frame = FRAMES[face];
      switch (key) {
         case "w": return negate(frame[1]);
         case "s": return frame[1];
         case "a": return negate(frame[2]);
         default: return frame[2];
      }
   }

   private static int[] negate(int[] v) {
      return new int[]{-v[0], -v[1], -v[2]};
   }

   /** The layer turn of a W/A/S/D move on square {@code cubie} (1 to 9) of a face, or null for an unknown face. */
   static Turn turnOf(String faceColor, String key, int cubie) {
      int face = faceIndex(faceColor);
      if (face < 0 || cubie < 1 || cubie > 9) {
         return null;
      }
      int sticker = face * 9 + cubie - 1;
      int[] normal = NORMAL[sticker];
      int[] along = keyDirection(face, key);
      // The axis lies along neither the face's normal nor the direction of travel.
      int axis = 0;
      while (normal[axis] != 0 || along[axis] != 0) {
         axis++;
      }
      // Turning about +axis carries a point on the face along (axis x normal); pick the sign that goes along.
      int a = (axis + 1) % 3;
      int b = (axis + 2) % 3;
      int[] swept = new int[3];
      swept[a] = -normal[b];
      swept[b] = normal[a];
      int dir = swept[0] * along[0] + swept[1] * along[1] + swept[2] * along[2] > 0 ? 1 : -1;
      return new Turn(axis, POS[sticker][axis], dir);
   }

   /** Turns the stickers of {@code faces} (in {@link Cube#faces()} order) by one layer turn. */
   static void apply(Turn turn, Face[] faces) {
      String[] before = new String[54];
      for (int s = 0; s < 54; s++) {
         before[s] = faces[s / 9].stickers[s % 9];
      }
      int[] dest = DEST[turn.id()];
      for (int s = 0; s < 54; s++) {
         faces[dest[s] / 9].stickers[dest[s] % 9] = before[s];
      }
   }
}
