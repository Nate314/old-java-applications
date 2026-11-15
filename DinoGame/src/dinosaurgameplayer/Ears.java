package dinosaurgameplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Play by ear (L): every obstacle you must jump or duck sounds two beats as it comes, a warning and then the beat to
 * act on, so the game can be played without looking at it. A low pitch asks for a jump (cacti and low birds), a high
 * one for a duck (middle birds); high birds, which you run under, are silent. The beat to act on is timed from the
 * obstacle's place and the speed: a held jump is then highest over the middle of the obstacle.
 */
final class Ears {
   enum Mode { OFF, CUES, BLINDFOLD }

   static final int JUMP = 1;
   static final int DUCK = 2;
   /** Steps from the warning beat to the beat to act on (1/4 s). */
   static final int BEAT = 15;
   /**
    * A jump goes up this many steps before the obstacle reaches the dinosaur's body: early enough for a player who
    * hears the beat a little late, and the self-check's ear player still clears long runs.
    */
   private static final double JUMP_LEAD = 10;
   /** A duck goes down this many steps before a bird reaches the dinosaur's head. */
   private static final double DUCK_LEAD = 4;
   /** The jump key is let go this many steps before the obstacle has passed under the dinosaur's body. */
   private static final double LET_GO = 13;

   private Ears() {
   }

   /** What o asks of the dinosaur: JUMP, DUCK, or 0 for a high bird (run under it). */
   static int need(Runner.Obstacle o) {
      if (o.kind != Runner.Kind.BIRD || o.lift == Runner.BIRD_HEIGHTS[0]) {
         return JUMP;
      }
      return o.lift == Runner.BIRD_HEIGHTS[1] ? DUCK : 0;
   }

   /** Steps from now to the beat to act on for o. */
   static double stepsToAct(Runner r, Runner.Obstacle o) {
      // The body's front edge (x + 30) meets the obstacle's hit box (x + 3); a bird meets the head (x + 39).
      return need(o) == DUCK ? (o.x - (r.dinoX() + 36)) / r.speed() - DUCK_LEAD : (o.x - (r.dinoX() + 27)) / r.speed() - JUMP_LEAD;
   }

   /** How many steps to hold the key from the beat to act on: the beat sounds that long. */
   static int holdSteps(Runner r, Runner.Obstacle o) {
      // Until the obstacle's hit box has passed the body (x + 4) or, ducking, the whole ducked dinosaur.
      double under = need(o) == DUCK ? (o.w + Runner.DUCK_W) / r.speed() : (o.w + 20) / r.speed();
      return (int) Math.max(1, Math.round(need(o) == DUCK ? DUCK_LEAD + under : JUMP_LEAD + under - LET_GO));
   }

   /**
    * The beats due this step, after the runner has stepped: each is { need, 0 } for the warning or { need, steps } for
    * the beat to act on, holding the key that many steps. Each obstacle sounds each beat once.
    */
   static List<int[]> beats(Runner r) {
      List<int[]> due = new ArrayList<>();
      if (r.state() != Runner.State.RUNNING) {
         return due;
      }
      for (Runner.Obstacle o : r.obstacles()) {
         int need = need(o);
         if (need == 0 || o.beats >= 2) {
            continue;
         }
         double t = stepsToAct(r, o);
         if (o.beats == 0 && t <= BEAT) {
            o.beats = 1;
            due.add(new int[] { need, 0 });
         } else if (o.beats == 1 && t <= 0) {
            o.beats = 2;
            due.add(new int[] { need, holdSteps(r, o) });
         }
      }
      return due;
   }

   static Mode next(Mode m) {
      return Mode.values()[(m.ordinal() + 1) % Mode.values().length];
   }

   /** The saved form, ears=..., or null when off. */
   static String encode(Mode m) {
      return m == Mode.OFF ? null : m.name().toLowerCase();
   }

   static Mode decode(String s) {
      for (Mode m : Mode.values()) {
         if (m.name().equalsIgnoreCase(s.trim())) {
            return m;
         }
      }
      return Mode.OFF;
   }
}
