package rubikscube;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Famous patterns people make on a solved cube, each a short list of face turns in the usual notation (U D F B R L
 * for the up, down, front, back, right and left faces, ' for a turn back, 2 for twice). Pattern makers hold the cube
 * with any side up, so the turns are taken with the cube's top face up and its front face in front.
 */
final class Patterns {
   static final List<Pattern> ALL = Collections.unmodifiableList(Arrays.asList(
         new Pattern("spots", "Six spots", "U D' R L' F B' U D'",
               "Each face one color with a spot of another in the middle, like a die."),
         new Pattern("checkerboard", "Checkerboard", "R2 L2 U2 D2 F2 B2",
               "Every face a checkerboard of two colors: each face turned twice."),
         new Pattern("anaconda", "Anaconda", "L U B' U' R L' B R' F B' D R D' F'",
               "Stripes snake from face to face all round the cube."),
         new Pattern("cubeincube", "Cube in a cube", "F L F U' R U F2 L2 U' L' B D' B' L2 U",
               "A small cube seems to sit in one corner of the big one."),
         new Pattern("plus", "Plus signs", "R2 L' D F2 R' D' R' L U' D R D B2 R' U D2",
               "A plus sign of another color on every face."),
         new Pattern("nested", "Nested cubes", "U' L' U' F' R2 B' R F U B2 U B' L U' F U R F'",
               "Cube in a cube in a cube: three, one inside the next, in one corner."),
         new Pattern("superflip", "Superflip", "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2",
               "Every piece home, every edge flipped. That takes 20 face turns, half turns counted once: no"
                     + " position needs more.")));

   /** One pattern: its saved key, name, turns in face notation and a line about it. */
   static final class Pattern {
      final String key;
      final String name;
      final String notation;
      final String about;
      /** The pattern's quarter turns (layer turn ids, see {@link CubeGeometry.Turn#id}). */
      final List<Integer> turns;

      Pattern(String key, String name, String notation, String about) {
         this.key = key;
         this.name = name;
         this.notation = notation;
         this.about = about;
         this.turns = Collections.unmodifiableList(parse(notation));
      }
   }

   private Patterns() {
   }

   /** The quarter turns of face notation such as {@code "R U2 F'"}: a half turn is two quarter turns. */
   static List<Integer> parse(String notation) {
      List<Integer> turns = new ArrayList<>();
      for (String token : notation.trim().split("\\s+")) {
         int[] normal;
         switch (token.charAt(0)) {
            case 'U': normal = new int[]{0, 1, 0}; break;
            case 'D': normal = new int[]{0, -1, 0}; break;
            case 'F': normal = new int[]{0, 0, 1}; break;
            case 'B': normal = new int[]{0, 0, -1}; break;
            case 'R': normal = new int[]{1, 0, 0}; break;
            case 'L': normal = new int[]{-1, 0, 0}; break;
            default: throw new IllegalArgumentException("Not a face turn: " + token);
         }
         int id = Coach.faceTurn(normal, !token.endsWith("'"));
         turns.add(id);
         if (token.endsWith("2")) {
            turns.add(id);
         }
      }
      return turns;
   }

   /** The turn that undoes a quarter turn. */
   static int inverse(int id) {
      return id ^ 1;
   }

   /** The face (index in {@link CubeGeometry#FACE_COLORS}) of every sticker of a solved cube. */
   static int[] solved() {
      int[] state = new int[54];
      for (int s = 0; s < 54; s++) {
         state[s] = s / 9;
      }
      return state;
   }

   /** The stickers after those turns. */
   static int[] apply(int[] state, List<Integer> turns) {
      int[] now = state.clone();
      for (int id : turns) {
         int[] dest = CubeGeometry.destinations(id);
         int[] before = now.clone();
         for (int s = 0; s < 54; s++) {
            now[dest[s]] = before[s];
         }
      }
      return now;
   }

   static Pattern byKey(String key) {
      for (Pattern pattern : ALL) {
         if (pattern.key.equals(key)) {
            return pattern;
         }
      }
      return null;
   }
}
