package rubikscube;

import java.util.Random;
import javax.swing.Timer;

/** The Mix feature: applies random moves directly to the cube, spread over the requested time on a Swing timer. */
public final class Scrambler {
   private static final String[] COLOR_NAMES = new String[]{"blue", "red", "yellow", "green", "orange", "white"};
   private static final String[] MOVE_KEYS = new String[]{"w", "a", "s", "d"};
   private final Random random = new Random();
   private final Cube cube;
   private final int moveCount;
   private final long durationMillis;
   private final Runnable onDone;
   private Timer timer;
   private long startMillis;
   private int movesDone = 0;

   Scrambler(int moveCount, double seconds, Cube cube, Runnable onDone) {
      this.moveCount = moveCount;
      this.durationMillis = Math.max(0L, (long)(seconds * 1000.0));
      this.cube = cube;
      this.onDone = onDone;
   }

   /** Applies one random move: any face, any of the nine squares, any of the four keys. */
   static void randomMove(Cube cube, Random random) {
      new Move(COLOR_NAMES[random.nextInt(6)], MOVE_KEYS[random.nextInt(4)], random.nextInt(9) + 1, cube);
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
         randomMove(this.cube, this.random);
         this.movesDone++;
      }
      this.cube.refresh();
      if (this.movesDone >= this.moveCount) {
         this.timer.stop();
         this.onDone.run();
      }
   }
}
