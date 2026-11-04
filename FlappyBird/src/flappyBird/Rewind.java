package flappyBird;

import java.util.ArrayList;
import java.util.List;

/**
 * The last few seconds of a run, one moment per game step, so a crash can be rewound and flown again from a little
 * before it. Taking moments back off the tape also keeps the path the bird flew into the crash, so the game can show
 * where it went wrong.
 */
final class Rewind {
   /** The game at one step: the step count and what the bird, pipes and background remembered. */
   static final class Moment {
      final int steps;
      final float[] bird;
      final float[] pipes;
      final float[] background;

      Moment(int steps, float[] bird, float[] pipes, float[] background) {
         this.steps = steps;
         this.bird = bird;
         this.pipes = pipes;
         this.background = background;
      }
   }

   private final Moment[] tape;
   private int newest = -1;
   private int count = 0;
   /** The path into the last crash as {step, bird height}, newest (the crash) first. */
   private final List<float[]> path = new ArrayList<>();

   /** A tape that keeps the latest {@code capacity} moments. */
   Rewind(int capacity) {
      this.tape = new Moment[Math.max(1, capacity)];
   }

   void record(Moment moment) {
      this.newest = (this.newest + 1) % this.tape.length;
      this.tape[this.newest] = moment;
      this.count = Math.min(this.count + 1, this.tape.length);
   }

   int size() {
      return this.count;
   }

   /** Starts a rewind: the path of the flight being rewound starts over. */
   void start() {
      this.path.clear();
   }

   /** Takes the newest moment off the tape (null once it is empty) and adds it to the path. */
   Moment back() {
      if (this.count == 0) {
         return null;
      }
      Moment moment = this.tape[this.newest];
      this.tape[this.newest] = null;
      this.newest = (this.newest - 1 + this.tape.length) % this.tape.length;
      this.count--;
      this.path.add(new float[]{moment.steps, moment.bird[0]});
      return moment;
   }

   /** The path the bird flew into the last crash, as {step, bird height}, the crash first; empty before a rewind. */
   List<float[]> path() {
      return this.path;
   }

   /** Empties the tape and forgets the path (a new run). */
   void clear() {
      while (this.count > 0) {
         this.back();
      }
      this.path.clear();
   }
}
