package dinosaurgameplayer;

import java.util.function.IntBinaryOperator;

/**
 * The original bot, playing the game in this window instead of Chrome's. It follows ObstacleDetector's rule exactly:
 * read the pixel just left of the red marker, and when its blue value is 83 hold the jump key for 65 ms, not
 * looking again until it lets go. It starts a run by holding the key for 70 ms, as the original did on launch.
 * The marker can be dragged to another spot; nothing else about the bot changes.
 */
final class PixelBot {
   /** Where the marker starts: 36 pixels ahead of the dinosaur's nose, 22 above the ground. */
   static final int DEFAULT_X = Runner.DINO_X + Runner.DINO_W + 36;
   static final int DEFAULT_Y = Runner.GROUND - 22;
   static final int MARKER = 10;
   /** After a crash it waits this long (3 s, time to read why), then runs again. */
   static final int RESTART_STEPS = 180;

   private int x = DEFAULT_X;
   private int y = DEFAULT_Y;
   private boolean on;
   private double holdLeft;
   private int jumps;

   boolean isOn() {
      return this.on;
   }

   /** Switches the bot on or off; switching off lets go of the key it may be holding. */
   void setOn(boolean on, Runner runner) {
      if (!on && this.holdLeft > 0) {
         this.holdLeft = 0;
         runner.release();
      }
      this.on = on;
   }

   int x() {
      return this.x;
   }

   int y() {
      return this.y;
   }

   /** Jumps this run. */
   int jumps() {
      return this.jumps;
   }

   void newRun() {
      this.jumps = 0;
   }

   /** Moves the marker, kept ahead of the dinosaur, inside the window and off the ground line and the score. */
   void moveTo(int x, int y) {
      this.x = Math.max(Runner.DINO_X + Runner.DINO_W + 1, Math.min(Runner.WIDTH - MARKER - 2, x));
      this.y = Math.max(Runner.GROUND - 110, Math.min(Runner.GROUND - 1, y));
   }

   boolean isAtDefault() {
      return this.x == DEFAULT_X && this.y == DEFAULT_Y;
   }

   boolean markerContains(int px, int py) {
      return px >= this.x - 6 && px < this.x + MARKER + 6 && py >= this.y - 6 && py < this.y + MARKER + 6;
   }

   /** One game step: frame gives the drawn frame's color at (x, y) as 0xRRGGBB. */
   void step(Runner runner, IntBinaryOperator frame) {
      if (!this.on) {
         return;
      }
      if (this.holdLeft > 0) {
         this.holdLeft -= Runner.STEP_MS;
         if (this.holdLeft <= 0) {
            runner.release();
         }
         return;
      }
      switch (runner.state()) {
         case READY:
            this.hold(runner, ObstacleDetector.START_HOLD_MS);
            break;
         case CRASHED:
            if (runner.idleSteps() >= RESTART_STEPS) {
               runner.restart();
            }
            break;
         default:
            if (ObstacleDetector.isObstacle(frame.applyAsInt(this.x - 1, this.y))) {
               Runner.Obstacle seen = runner.at(this.x - 1);
               if (seen != null) {
                  seen.botJumped = true;
               }
               this.jumps++;
               this.hold(runner, ObstacleDetector.HOLD_MS);
            }
      }
   }

   private void hold(Runner runner, int millis) {
      runner.press();
      this.holdLeft = millis;
   }

   /** Why a bot run ended, in a line, or "" when it was not the bot's run. */
   static String whyItCrashed(Runner runner) {
      return whyItCrashed(runner, false);
   }

   /** The same, knowing whether it was night (when the ink is pale, and the bot blind). */
   static String whyItCrashed(Runner runner, boolean night) {
      Runner.Obstacle o = runner.crashedInto();
      if (o == null) {
         return "";
      }
      String what = o.kind == Runner.Kind.BIRD ? "that bird" : "that cactus";
      if (!o.botJumped && night) {
         return "Night fell and the ink turned pale. Its pixel only knows blue " + ObstacleDetector.OBSTACLE_BLUE + ".";
      }
      if (!o.botJumped) {
         return o.kind == Runner.Kind.BIRD && o.lift > 20
            ? "Its pixel never saw " + what + ". It cannot duck. Try the red marker higher?"
            : "Its pixel never saw " + what + ". Drag the red marker onto its path.";
      }
      return runner.isRising()
         ? "It jumped too late. Drag the red marker right."
         : "It jumped too early. Drag the red marker left.";
   }
}
