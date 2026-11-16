package dinosaurgameplayer;

import java.util.Random;

/**
 * Two players on one keyboard (P): two dinosaurs race the same course side by side, P1 at the usual place, P2 a little
 * ahead of it. A Runner plays the same course the same way whatever the keys, so both meet the same obstacles, P2 a
 * moment sooner. The round ends when both have crashed; the one that fell further along the course wins it. Wins are kept for the
 * session only, and duel runs count for nobody's best.
 */
final class Duel {
   /** P2 stands this far right of P1: clear of P1's ducking body, so the two never overlap. */
   static final int P2_X = Runner.DINO_X + 70;

   final Runner p1;
   final Runner p2;
   private final Random courses;
   private final int[] wins = new int[2];
   private boolean scored;

   Duel(Runner p1, long seed) {
      this.p1 = p1;
      this.p2 = new Runner(seed, P2_X);
      this.courses = new Random(seed);
   }

   /** A new round: both run at once on one new course. */
   void start() {
      long course = this.courses.nextLong();
      this.p1.setCourse(course);
      this.p2.setCourse(course);
      this.p1.restart();
      this.p2.restart();
      this.scored = false;
   }

   /** Back to the ready screen (a round under way is dropped). */
   void ready() {
      this.p2.duck(false);
      this.p2.ready();
   }

   boolean running() {
      return this.p1.state() == Runner.State.RUNNING || this.p2.state() == Runner.State.RUNNING;
   }

   /** Both crashed: the round is over. */
   boolean over() {
      return this.p1.state() == Runner.State.CRASHED && this.p2.state() == Runner.State.CRASHED;
   }

   /** The runner whose course the screen shows: the one in front (P1 on a tie). */
   Runner view() {
      return this.p2.distance() > this.p1.distance() ? this.p2 : this.p1;
   }

   /**
    * 1 or 2 for the round's winner, 0 for a dead heat (only once the round is over). P2 stands ahead, so it reaches each
    * obstacle first: the round is judged by which obstacle each fell at, along the course, and the same one is a tie.
    */
   int winner() {
      double a = reached(this.p1);
      double b = reached(this.p2);
      return a == b ? 0 : a > b ? 1 : 2;
   }

   /** Where on the course r crashed: the obstacle it hit (o.x + distance stays the same for an obstacle as it scrolls). */
   private static double reached(Runner r) {
      Runner.Obstacle o = r.crashedInto();
      return o == null ? r.distance() + r.dinoX() : o.x + r.distance();
   }

   /** Counts the round once, as soon as it is over. */
   void score() {
      if (this.over() && !this.scored) {
         this.scored = true;
         if (this.winner() > 0) {
            this.wins[this.winner() - 1]++;
         }
      }
   }

   int wins(int player) {
      return this.wins[player - 1];
   }

   /** What the game over screen says about the round. */
   String[] lines() {
      int w = this.winner();
      int a = this.p1.score();
      int b = this.p2.score();
      String first = w == 0 ? "A dead heat: both fell at the same obstacle."
         : "P" + w + " wins, " + Math.max(a, b) + " to " + Math.min(a, b) + ".";
      return new String[] { first, "Rounds won: P1 " + this.wins[0] + ", P2 " + this.wins[1] + ". Two-player runs set no best." };
   }
}
