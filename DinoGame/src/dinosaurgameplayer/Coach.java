package dinosaurgameplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * After you crash: what would have got you past. A Runner plays a course the same way for the same keys, so the coach
 * plays your run again with your keys up to a moment before the crash and then tries other presses there (sooner,
 * later, held longer, a duck), nearest to what you did first, until one clears the obstacle you hit. Its line says
 * what to change, and a mark on the ground shows where the obstacle was when that press should have come.
 */
final class Coach {
   /** How far back the coach looks for a better press: half a second or so. */
   static final int WINDOW = 40;
   private static final int LONG_HOLD = 40;

   enum Fix { JUMP, DUCK, STAY }

   final Fix fix;
   /** What to do differently, in a line. */
   final String line;
   /** Where the obstacle's left edge was, on this crash's screen, when the press that clears it should have come. */
   final int markX;
   /** Where it was when you pressed (jump or duck), or -1 when you did not press near it. */
   final int pressX;
   /** The step that press comes at, and for a jump how many steps the key is held. */
   final int step;
   final int hold;
   /** The step of your press near the crash, or -1. */
   final int press;
   /** The course and your keys before the fix, then the fix: played on a Ghost, the run that clears it. */
   private final long course;
   private final int[] keys;

   private Coach(Fix fix, String line, int markX, int pressX, int step, int hold, int press, long course, int[] keys) {
      this.fix = fix;
      this.step = step;
      this.hold = hold;
      this.press = press;
      this.course = course;
      this.keys = keys;
      this.line = line;
      this.markX = markX;
      this.pressX = pressX;
   }

   /** What would have cleared the obstacle runner crashed into, or null when nothing near the crash does. */
   static Coach of(Runner runner) {
      return of(runner, false);
   }

   /** The same, with touch set naming the thumb pads (JUMP, DUCK) instead of the keys. */
   static Coach of(Runner runner, boolean touch) {
      Runner.Obstacle o = runner.crashedInto();
      if (o == null || runner.state() != Runner.State.CRASHED) {
         return null;
      }
      int crash = (int) runner.steps();
      int first = Math.max(0, crash - WINDOW);
      List<Integer> mine = runner.inputs();
      // Your last press near the crash, and how long you held it.
      int press = -1;
      int hold = LONG_HOLD;
      boolean ducked = false;
      for (int in : mine) {
         int step = in / 4;
         int key = in % 4;
         if (step < first) {
            continue;
         }
         if (key == Runner.PRESS) {
            press = step;
            hold = LONG_HOLD;
            ducked = false;
         } else if (key == Runner.RELEASE && press >= 0) {
            hold = Math.max(1, step - press);
         } else if (key == Runner.DUCK) {
            press = step;
            ducked = true;
         }
      }
      double clear = runner.distance() + o.x + o.w - Runner.DINO_X;
      boolean middle = o.kind == Runner.Kind.BIRD && o.lift == Runner.BIRD_HEIGHTS[1];
      boolean high = o.kind == Runner.Kind.BIRD && o.lift == Runner.BIRD_HEIGHTS[2];
      int around = press >= 0 ? press : crash - 15;
      List<int[]> tries = new ArrayList<>();   // { step, fix, hold }
      if (high && press >= 0) {
         tries.add(new int[] { press, Fix.STAY.ordinal(), 0 });
      }
      boolean duckFirst = middle && (ducked || press < 0);
      for (int pass = 0; pass < 2; pass++) {
         boolean duck = (pass == 0) == duckFirst;
         if (duck && !middle) {
            continue;
         }
         for (int d = 0; d <= WINDOW; d++) {
            for (int t : d == 0 ? new int[] { around } : new int[] { around - d, around + d }) {
               if (t < first || t >= crash) {
                  continue;
               }
               if (duck) {
                  tries.add(new int[] { t, Fix.DUCK.ordinal(), 0 });
               } else {
                  for (int h : hold == 1 ? new int[] { 1, LONG_HOLD } : new int[] { hold, LONG_HOLD, 1 }) {
                     tries.add(new int[] { t, Fix.JUMP.ordinal(), h });
                  }
               }
            }
         }
      }
      // Where the obstacle was at each step of the window (the distance run is the same whatever the keys).
      double[] ahead = new double[crash - first + 1];
      double run = 0;
      double speed = Runner.START_SPEED;
      for (int i = 0; i <= crash; i++) {
         if (i >= first) {
            ahead[i - first] = o.x + runner.distance() - run;
         }
         run += speed;
         speed = Math.min(Runner.MAX_SPEED, speed + Runner.ACCELERATION);
      }
      long budget = 1_500_000;   // shortcut: steps replayed at most (~60 ms on a desktop JVM), so a long run gives up sooner
      for (int[] t : tries) {
         int markX = (int) Math.round(ahead[t[0] - first]);
         if (markX > Runner.WIDTH - 10) {
            continue;
         }
         if ((budget -= crash) < 0) {
            break;
         }
         int[] keys = keys(mine, t);
         if (!clears(runner.course(), keys, clear)) {
            continue;
         }
         Fix fix = Fix.values()[t[1]];
         int pressX = press < 0 ? -1 : (int) Math.round(ahead[press - first]);
         return new Coach(fix, line(fix, t, press, ducked, hold, touch), markX, pressX, t[0], t[2], press, runner.course(), keys);
      }
      return null;
   }

   /** The run with the fix, from its start: a slow replay of it shows the press that clears the obstacle. */
   Ghost fixed() {
      return new Ghost(this.course, 0, this.keys);
   }

   private static String line(Fix fix, int[] t, int press, boolean ducked, int hold, boolean touch) {
      if (fix == Fix.STAY) {
         return "That bird flies high: stay on the ground and run under it.";
      }
      if (fix == Fix.DUCK) {
         return ducked && t[0] < press ? "Duck " + seconds(press - t[0]) + " sooner, when it reaches the mark."
            : "Duck under that bird: hold " + (touch ? "DUCK" : "↓") + " when it reaches the mark.";
      }
      if (press < 0 || ducked) {
         return "Jump when it reaches the mark: " + (touch ? "tap JUMP." : "Space or ↑.");
      }
      if (t[0] < press) {
         return "You jumped " + seconds(press - t[0]) + " late: jump when it reaches the mark.";
      }
      if (t[0] > press) {
         return "You jumped " + seconds(t[0] - press) + " early: wait until it reaches the mark.";
      }
      return t[2] > hold ? "Right moment: hold " + (touch ? "JUMP" : "the key") + " longer to jump higher." : "Right moment: a quick tap lands sooner.";
   }

   /** Steps as seconds, to a twentieth. */
   static String seconds(int steps) {
      long hundredths = Math.max(5, Math.round(steps * Runner.STEP_MS / 50.0) * 5);
      return hundredths / 100 + "." + (hundredths % 100 < 10 ? "0" : "") + hundredths % 100 + " s";
   }

   /** Your keys before try[0], then the try's. */
   private static int[] keys(List<Integer> mine, int[] t) {
      List<Integer> keys = new ArrayList<>();
      for (int in : mine) {
         if (in / 4 < t[0]) {
            keys.add(in);
         }
      }
      int at = t[0] * 4;
      if (t[1] == Fix.JUMP.ordinal()) {
         keys.add(at + Runner.STAND);
         keys.add(at + Runner.PRESS);
         keys.add((t[0] + t[2]) * 4 + Runner.RELEASE);
      } else if (t[1] == Fix.DUCK.ordinal()) {
         keys.add(at + Runner.RELEASE);
         keys.add(at + Runner.DUCK);
      } else {
         keys.add(at + Runner.RELEASE);
         keys.add(at + Runner.STAND);
      }
      int[] inputs = new int[keys.size()];
      for (int i = 0; i < inputs.length; i++) {
         inputs[i] = keys.get(i);
      }
      return inputs;
   }

   /** Whether keys get the dinosaur on course to distance clear without a crash. */
   private static boolean clears(long course, int[] keys, double clear) {
      Ghost g = new Ghost(course, 0, keys);
      g.start();
      while (g.runner().state() == Runner.State.RUNNING && g.runner().distance() < clear) {
         g.step();
      }
      return g.runner().state() == Runner.State.RUNNING;
   }
}
