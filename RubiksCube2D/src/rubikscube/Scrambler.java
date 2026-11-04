package rubikscube;

import java.util.Random;
import javax.swing.Timer;

/** The Mix feature: applies random moves to the cube, spread over the requested time on a Swing timer. */
public final class Scrambler {
   private static final String[] COLOR_NAMES = new String[]{"blue", "red", "yellow", "green", "orange", "white"};
   private static final String[] MOVE_KEYS = new String[]{"w", "a", "s", "d"};
   private final Random random = new Random();
   private final MoveHistory history;
   private final Cube cube;
   private final int moveCount;
   private final long durationMillis;
   private final Runnable onDone;
   private Timer timer;
   private long startMillis;
   private int movesDone = 0;

   Scrambler(int moveCount, double seconds, MoveHistory history, Cube cube, Runnable onDone) {
      this.moveCount = moveCount;
      this.durationMillis = Math.max(0L, (long)(seconds * 1000.0));
      this.history = history;
      this.cube = cube;
      this.onDone = onDone;
   }

   /** A random move as {face color, key, cubie}: any face, any of the nine squares, any of the four keys. */
   static String[] randomMove(Random random) {
      return new String[]{COLOR_NAMES[random.nextInt(6)], MOVE_KEYS[random.nextInt(4)], String.valueOf(random.nextInt(9) + 1)};
   }

   /** A random move that does not simply undo {@code previous} (which may be null). */
   static String[] randomMoveAfter(String[] previous, Random random) {
      while (true) {
         String[] move = randomMove(random);
         if (previous == null || !move[0].equals(previous[0]) || !move[2].equals(previous[2])
               || !move[1].equals(MoveHistory.inverse(previous[1]))) {
            return move;
         }
      }
   }

   /** Applies a {face color, key, cubie} move through the history. */
   static void apply(MoveHistory history, String[] move) {
      history.apply(move[0], move[1], Integer.parseInt(move[2]));
   }

   void start() {
      this.startMillis = System.currentTimeMillis();
      this.timer = new Timer(15, event -> this.tick());
      this.timer.start();
   }

   void stop() {
      if (this.timer != null) {
         this.timer.stop();
      }
   }

   private void tick() {
      long elapsed = System.currentTimeMillis() - this.startMillis;
      int due = elapsed >= this.durationMillis ? this.moveCount : (int)((long)this.moveCount * elapsed / this.durationMillis);
      while (this.movesDone < due) {
         apply(this.history, randomMove(this.random));
         this.movesDone++;
      }
      this.cube.refresh();
      if (this.movesDone >= this.moveCount) {
         this.timer.stop();
         this.onDone.run();
      }
   }
}
