package clock;

import java.util.ArrayList;
import java.util.List;

/** The Stopwatch tab: elapsed time against the wall clock (epoch milliseconds passed in) and its laps. */
final class Stopwatch {
   static final int MAX_LAPS = 99;

   /** When the current run started, while running; 0 otherwise. */
   private long startedAt;
   /** Time from earlier runs, before the last stop. */
   private long banked;
   /** Elapsed time at the end of each lap. */
   private final List<Long> laps = new ArrayList<>();

   boolean isRunning() {
      return this.startedAt > 0;
   }

   long startedAt() {
      return this.startedAt;
   }

   long banked() {
      return this.banked;
   }

   long elapsed(long now) {
      return this.banked + (this.isRunning() ? Math.max(0, now - this.startedAt) : 0);
   }

   /** Space or Start: starts or stops. */
   void toggle(long now) {
      if (this.isRunning()) {
         this.banked = this.elapsed(now);
         this.startedAt = 0;
      } else {
         this.startedAt = now;
      }
   }

   /** L or Lap: ends the current lap, while running. */
   void lap(long now) {
      if (this.isRunning() && this.laps.size() < MAX_LAPS) {
         this.laps.add(this.elapsed(now));
      }
   }

   /** R or Reset: clears the time and laps, once stopped. */
   void reset() {
      if (!this.isRunning()) {
         this.banked = 0;
         this.laps.clear();
      }
   }

   int lapCount() {
      return this.laps.size();
   }

   /** Elapsed time at the end of lap {@code index} (from 0). */
   long lapEnd(int index) {
      return this.laps.get(index);
   }

   /** How long lap {@code index} (from 0) took. */
   long lapTime(int index) {
      return this.laps.get(index) - (index > 0 ? this.laps.get(index - 1) : 0);
   }

   /** The lap under way: time since the last lap ended. */
   long currentLap(long now) {
      return this.elapsed(now) - (this.laps.isEmpty() ? 0 : this.laps.get(this.laps.size() - 1));
   }

   /** The fastest (or with {@code slowest}, the slowest) finished lap, or -1 with fewer than two laps. */
   int extremeLap(boolean slowest) {
      if (this.laps.size() < 2) {
         return -1;
      }
      int pick = 0;
      for (int i = 1; i < this.laps.size(); i++) {
         if (slowest ? this.lapTime(i) > this.lapTime(pick) : this.lapTime(i) < this.lapTime(pick)) {
            pick = i;
         }
      }
      return pick;
   }

   void restore(long startedAt, long banked, List<Long> lapEnds) {
      this.startedAt = Math.max(0, startedAt);
      this.banked = Math.max(0, banked);
      this.laps.clear();
      long last = 0;
      for (Long end : lapEnds) {
         if (end >= last && this.laps.size() < MAX_LAPS) {
            this.laps.add(end);
            last = end;
         }
      }
   }
}
