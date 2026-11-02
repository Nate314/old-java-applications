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

      // Every move is a real cube's: after any moves, the stickers that shared a cubie still share one (the recovered
      // moves broke this), and the square a key moves goes the way the key points in the net.
      Cube real = new Cube(null, new JButton[6][9], 50, 50);
      Face[] realFaces = real.faces();
      for (int s = 0; s < 54; s++) {
         realFaces[s / 9].stickers[s % 9] = String.valueOf(s);
      }
      Random realRandom = new Random(7);
      for (int i = 0; i < 500; i++) {
         new Move(COLORS[realRandom.nextInt(6)], KEYS[realRandom.nextInt(4)], realRandom.nextInt(9) + 1, real);
      }
      int[] at = new int[54];
      for (int s = 0; s < 54; s++) {
         at[Integer.parseInt(realFaces[s / 9].stickers[s % 9])] = s;
      }
      for (int s = 0; s < 54; s++) {
         for (int t = 0; t < 54; t++) {
            if (Arrays.equals(CubeGeometry.POS[s], CubeGeometry.POS[t])) {
               check(Arrays.equals(CubeGeometry.POS[at[s]], CubeGeometry.POS[at[t]]), "stickers " + s + " and " + t + " came apart");
            }
         }
      }
      for (int f = 0; f < 6; f++) {
         for (int cubie = 1; cubie <= 9; cubie++) {
            for (String key : KEYS) {
               CubeGeometry.Turn turn = CubeGeometry.turnOf(COLORS[f], key, cubie);
               int s = f * 9 + cubie - 1;
               // The middle of the sticker, doubled to stay whole: 2 * position + normal.
               int[] before = new int[3];
               for (int k = 0; k < 3; k++) {
                  before[k] = 2 * CubeGeometry.POS[s][k] + CubeGeometry.NORMAL[s][k];
               }
               int[] after = CubeGeometry.rotate(before, turn.axis, turn.dir);
               int[] along = CubeGeometry.keyDirection(f, key);
               int dot = 0;
               for (int k = 0; k < 3; k++) {
                  dot += (after[k] - before[k]) * along[k];
               }
               // The center square of a face stays put on an outer turn; every other square moves along.
               check(dot > 0 || Arrays.equals(before, after), COLORS[f] + " " + cubie + " " + key + " does not go the way of the key");
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
      MoveHistory mixHistory = new MoveHistory(mixed);
      for (int i = 0; i < 2000; i++) {
         Scrambler.apply(mixHistory, Scrambler.randomMove(random));
      }

      // Following the hints from any mix walks back to the start, never taking more steps than the path is long.
      int depth = mixHistory.depth();
      check(depth > 0 && depth <= 2000, "mix path length " + depth);
      for (int step = 0; step < depth; step++) {
         Scrambler.apply(mixHistory, mixHistory.hint());
      }
      check(mixHistory.depth() == 0 && mixHistory.hint() == null && mixed.isSolved(), "hints did not walk back to solved");

      // A move and its inverse, or four turns of one slice, cut the path back instead of growing it.
      MoveHistory history = new MoveHistory(mixed);
      history.apply("red", "w", 1);
      history.apply("red", "s", 1);
      check(history.depth() == 0, "inverse did not cut the path");
      for (int turn = 0; turn < 4; turn++) {
         history.apply("blue", "a", 5);
      }
      check(history.depth() == 0 && mixed.isSolved(), "four turns did not cut the path");
      history.apply("green", "d", 3);
      check(history.depth() == 1 && history.hint()[1].equals("a"), "hint is not the inverse of the last move");

      // A level's mix never simply undoes the move before it, and reset puts the stickers back.
      String[] previous = null;
      for (int i = 0; i < 2000; i++) {
         String[] move = Scrambler.randomMoveAfter(previous, random);
         check(previous == null || !(move[0].equals(previous[0]) && move[2].equals(previous[2])
               && move[1].equals(MoveHistory.inverse(previous[1]))), "level mix undid its previous move");
         previous = move;
      }
      mixed.reset();
      check(mixed.isSolved() && mixed.snapshot().equals(new Cube(null, new JButton[6][9], 50, 50).snapshot()), "reset did not restore the start");

      // Stars: 3 at par, 2 within twice par plus 2, 1 beyond or with a hint.
      check(LevelRecords.starsFor(4, 4, false) == 3 && LevelRecords.starsFor(4, 3, false) == 3, "par stars");
      check(LevelRecords.starsFor(4, 10, false) == 2 && LevelRecords.starsFor(4, 11, false) == 1, "two and one stars");
      check(LevelRecords.starsFor(4, 4, true) == 1, "a hint caps the stars at one");

      // Records keep the best stars and fewest moves, unlock levels in order and survive a reload.
      try {
         java.io.File file = java.io.File.createTempFile("rubikscube-levels", ".txt");
         file.deleteOnExit();
         LevelRecords records = LevelRecords.load(file);
         check(records.unlocked() == 1 && records.lastLevel() == 1, "fresh records unlock level 1");
         records.record(1, 2, 3);
         records.record(1, 3, 5);
         records.record(2, 1, 9);
         records.setLastLevel(3);
         LevelRecords reloaded = LevelRecords.load(file);
         check(reloaded.stars(1) == 3 && reloaded.moves(1) == 3, "best stars and fewest moves kept separately");
         check(reloaded.unlocked() == 3 && reloaded.lastLevel() == 3 && reloaded.totalStars() == 4, "reload: unlocked, last level, total");
         reloaded.setLastLevel(LevelRecords.LEVELS);
         check(LevelRecords.load(file).lastLevel() == 3, "last level is limited to the unlocked one");
      } catch (java.io.IOException e) {
         throw new AssertionError(e);
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
