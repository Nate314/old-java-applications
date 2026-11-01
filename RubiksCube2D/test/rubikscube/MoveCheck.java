package rubikscube;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;

/** Self-check for cube moves and the solved check. Run with java -ea. */
public class MoveCheck {
   private static final String[] COLORS = {"blue", "red", "yellow", "green", "orange", "white"};
   private static final String[] KEYS = {"w", "a", "s", "d"};

   public static void main(String[] args) {
      Cube cube = new Cube(null, new JButton[6][9], 50, 50);
      check(cube.isSolved(), "new cube is solved");

      // Give every sticker a unique name so a move that copies or drops a sticker is caught.
      Face[] faces = cube.faces();
      for (int f = 0; f < 6; f++) {
         for (int i = 0; i < 9; i++) {
            faces[f].stickers[i] = f + ":" + i;
         }
      }
      List<String> start = snapshot(cube);

      for (String color : COLORS) {
         for (int cubie = 1; cubie <= 9; cubie++) {
            for (String key : KEYS) {
               String name = color + " " + cubie + " " + key;
               new Move(color, key, cubie, cube);
               List<String> after = snapshot(cube);
               check(!after.equals(start), name + " changed nothing");
               List<String> sorted = new ArrayList<>(after);
               List<String> sortedStart = new ArrayList<>(start);
               sorted.sort(null);
               sortedStart.sort(null);
               check(sorted.equals(sortedStart), name + " is not a permutation of the stickers");

               new Move(color, inverse(key), cubie, cube);
               check(snapshot(cube).equals(start), name + " is not undone by " + inverse(key));

               for (int turn = 0; turn < 4; turn++) {
                  new Move(color, key, cubie, cube);
               }
               check(snapshot(cube).equals(start), name + " four times is not the identity");
            }
         }
      }

      // A random scramble undone in reverse order returns to the start, and the solved check sees it.
      Cube solved = new Cube(null, new JButton[6][9], 50, 50);
      Random random = new Random(1);
      String[] colors = new String[200];
      String[] keys = new String[200];
      int[] cubies = new int[200];
      for (int i = 0; i < 200; i++) {
         colors[i] = COLORS[random.nextInt(6)];
         keys[i] = KEYS[random.nextInt(4)];
         cubies[i] = random.nextInt(9) + 1;
         new Move(colors[i], keys[i], cubies[i], solved);
      }
      check(!solved.isSolved(), "scrambled cube reads as solved");
      for (int i = 199; i >= 0; i--) {
         new Move(colors[i], inverse(keys[i]), cubies[i], solved);
      }
      check(solved.isSolved(), "unscrambled cube is not solved");

      // Mix's random moves only use valid squares (1 to 9) and never throw.
      Cube mixed = new Cube(null, new JButton[6][9], 50, 50);
      for (int i = 0; i < 2000; i++) {
         Scrambler.randomMove(mixed, random);
      }

      System.out.println("MoveCheck passed");
   }

   private static String inverse(String key) {
      return key.equals("w") ? "s" : key.equals("s") ? "w" : key.equals("a") ? "d" : "a";
   }

   private static List<String> snapshot(Cube cube) {
      List<String> all = new ArrayList<>();
      for (Face face : cube.faces()) {
         all.addAll(Arrays.asList(face.stickers));
      }
      return all;
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
