package clock;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Focus tab's sessions, the Pomodoro way: focus for a set time, then a short break that starts by itself, and a
 * long break after every fourth session. Counted against the wall clock (epoch milliseconds passed in), like the
 * timer, so a session keeps going while the window is closed. Every finished minute of focus is logged by local day.
 */
final class Focus {
   static final long MINUTE = Countdown.MINUTE;
   /** Sessions before a long break. */
   static final int SET = 4;
   /** Focus and short break minutes per plan; a long break is three short ones. */
   static final int[][] PLANS = {{25, 5}, {50, 10}, {15, 3}};
   static final int FOCUS = 0;
   static final int SHORT = 1;
   static final int LONG = 2;
   /** What {@link #update} saw happen. */
   static final int NONE = 0;
   static final int FOCUS_DONE = 1;
   static final int BREAK_DONE = 2;
   /** Days of history kept. */
   static final int DAYS = 14;

   private int plan;
   private int phase = FOCUS;
   /** Sessions finished in this set, 0 to {@link #SET}. */
   private int done;
   /** When the phase ends, while running; 0 otherwise. */
   private long endsAt;
   /** Time left while ready or paused. */
   private long left = this.length(FOCUS);
   /** When the last phase {@link #update} saw end ended. */
   private long eventAt;
   /** Day (yyyymmdd) to {minutes focused, sessions finished}. */
   private final TreeMap<Integer, int[]> log = new TreeMap<>();

   int plan() {
      return this.plan;
   }

   int phase() {
      return this.phase;
   }

   int done() {
      return this.done;
   }

   long endsAt() {
      return this.endsAt;
   }

   long left() {
      return this.left;
   }

   long eventAt() {
      return this.eventAt;
   }

   boolean isRunning() {
      return this.endsAt > 0;
   }

   /** Stopped part way through a phase. */
   boolean isPaused() {
      return !this.isRunning() && this.left < this.length(this.phase);
   }

   boolean onBreak() {
      return this.phase != FOCUS;
   }

   long length(int phase) {
      int[] minutes = PLANS[this.plan];
      return (phase == FOCUS ? minutes[0] : phase == SHORT ? minutes[1] : 3 * minutes[1]) * MINUTE;
   }

   long remaining(long now) {
      return this.isRunning() ? Math.max(0, this.endsAt - now) : this.left;
   }

   /** Start, pause or resume the current phase. */
   void toggle(long now) {
      if (this.isRunning()) {
         this.left = this.remaining(now);
         this.endsAt = 0;
      } else {
         this.endsAt = now + this.left;
      }
   }

   /**
    * Ends the phase early. Minutes already focused are logged, but the session does not count towards the set.
    * The next phase runs if this one was running.
    */
   void skip(long now) {
      boolean running = this.isRunning();
      if (this.phase == FOCUS) {
         this.credit(now, this.length(FOCUS) - this.remaining(now), false);
         this.phase = SHORT;
      } else {
         this.endBreak();
      }
      this.left = this.length(this.phase);
      this.endsAt = running ? now + this.left : 0;
   }

   /** Back to the first session of a set, ready. The log stays. */
   void reset() {
      this.phase = FOCUS;
      this.done = 0;
      this.endsAt = 0;
      this.left = this.length(FOCUS);
   }

   /** The next plan of lengths; the current phase starts over at its new length, stopped. */
   void nextPlan() {
      this.plan = (this.plan + 1) % PLANS.length;
      this.endsAt = 0;
      this.left = this.length(this.phase);
   }

   /**
    * Call on every tick. A focus session that runs out is logged and its break starts at once; a break that runs out
    * leaves the next session ready. Catches up on everything that happened while the window was closed, and returns
    * the last thing that happened ({@link #eventAt} says when).
    */
   int update(long now) {
      int event = NONE;
      while (this.isRunning() && now >= this.endsAt) {
         long at = this.endsAt;
         if (this.phase == FOCUS) {
            this.credit(at, this.length(FOCUS), true);
            this.done = Math.min(SET, this.done + 1);
            this.phase = this.done >= SET ? LONG : SHORT;
            this.endsAt = at + this.length(this.phase);
            event = FOCUS_DONE;
         } else {
            this.endBreak();
            this.endsAt = 0;
            this.left = this.length(FOCUS);
            event = BREAK_DONE;
         }
         this.eventAt = at;
      }
      return event;
   }

   private void endBreak() {
      if (this.phase == LONG) {
         this.done = 0;
      }
      this.phase = FOCUS;
   }

   private void credit(long at, long focused, boolean session) {
      int minutes = (int) (Math.max(0, focused) / MINUTE);
      if (minutes == 0 && !session) {
         return;
      }
      int[] day = this.log.get(day(at));
      if (day == null) {
         day = new int[2];
         this.log.put(day(at), day);
      }
      day[0] += minutes;
      if (session) {
         day[1]++;
      }
      this.trim();
   }

   private void trim() {
      while (this.log.size() > DAYS) {
         this.log.remove(this.log.firstKey());
      }
   }

   int minutesOn(int day) {
      int[] entry = this.log.get(day);
      return entry == null ? 0 : entry[0];
   }

   int sessionsOn(int day) {
      int[] entry = this.log.get(day);
      return entry == null ? 0 : entry[1];
   }

   Map<Integer, int[]> log() {
      return this.log;
   }

   /** The local day of an epoch time, as yyyymmdd. */
   @SuppressWarnings("deprecation")
   static int day(long millis) {
      // Date's getters read local time on the desktop and in the browser, where Calendar is UTC.
      Date date = new Date(millis);
      return (date.getYear() + 1900) * 10000 + (date.getMonth() + 1) * 100 + date.getDate();
   }

   /** Noon on the local day of {@code millis}: whole days back from it land on the right days whatever the clocks do. */
   @SuppressWarnings("deprecation")
   static long noon(long millis) {
      Date date = new Date(millis);
      long sinceMidnight = ((date.getHours() * 60L + date.getMinutes()) * 60 + date.getSeconds()) * 1000 + (millis % 1000 + 1000) % 1000;
      return millis - sinceMidnight + 12 * 60 * MINUTE;
   }

   /** Restores what was saved; out-of-range values are clamped. */
   void restore(int plan, int phase, int done, long endsAt, long left) {
      this.plan = plan >= 0 && plan < PLANS.length ? plan : 0;
      this.phase = phase == SHORT || phase == LONG ? phase : FOCUS;
      this.done = Math.max(0, Math.min(SET, done));
      this.endsAt = Math.max(0, endsAt);
      this.left = left < 0 ? this.length(this.phase) : Math.min(this.length(this.phase), left);
   }

   void restoreDay(int day, int minutes, int sessions) {
      if (day > 0 && minutes >= 0 && sessions >= 0) {
         this.log.put(day, new int[]{minutes, sessions});
         this.trim();
      }
   }
}
