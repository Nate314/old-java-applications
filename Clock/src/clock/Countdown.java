package clock;

/**
 * The kitchen timer behind the Timer tab: a length of up to an hour set on the dial, counted down against the wall
 * clock (epoch milliseconds passed in), so it keeps time while the window is closed and reopened.
 */
final class Countdown {
   static final long MINUTE = 60_000L;
   static final long MAX = 60 * MINUTE;

   private long length = 5 * MINUTE;
   /** When it runs out, while running; 0 otherwise. */
   private long endsAt;
   /** Time left while ready or paused. */
   private long left = length;
   /** When it ran out, until dismissed; 0 otherwise. */
   private long finishedAt;

   long length() {
      return this.length;
   }

   long endsAt() {
      return this.endsAt;
   }

   long left() {
      return this.left;
   }

   long finishedAt() {
      return this.finishedAt;
   }

   boolean isRunning() {
      return this.endsAt > 0;
   }

   boolean isDone() {
      return this.finishedAt > 0;
   }

   /** Paused part way: not running, not done, and some time already gone. */
   boolean isPaused() {
      return !this.isRunning() && !this.isDone() && this.left > 0 && this.left < this.length;
   }

   long remaining(long now) {
      return this.isRunning() ? Math.max(0, this.endsAt - now) : this.isDone() ? 0 : this.left;
   }

   /** Sets the length (and the time left). A running timer keeps running from the new value. */
   void set(long millis, long now) {
      this.length = Math.max(0, Math.min(MAX, millis));
      this.finishedAt = 0;
      if (this.isRunning() && this.length > 0) {
         this.endsAt = now + this.length;
      } else {
         this.endsAt = 0;
         this.left = this.length;
      }
   }

   /** Up or Down: the time left rounded to a whole minute, plus or minus minutes. */
   void addMinutes(int minutes, long now) {
      long whole = (this.remaining(now) + MINUTE / 2) / MINUTE * MINUTE;
      this.set(whole + minutes * MINUTE, now);
   }

   /** Space or Start: starts, pauses or resumes; once done it only dismisses. */
   void toggle(long now) {
      if (this.isDone()) {
         this.dismiss();
      } else if (this.isRunning()) {
         this.left = this.remaining(now);
         this.endsAt = 0;
      } else if (this.left > 0) {
         this.endsAt = now + this.left;
      }
   }

   /** Back to the full length, stopped. */
   void reset() {
      this.endsAt = 0;
      this.finishedAt = 0;
      this.left = this.length;
   }

   /** Done and acknowledged: ready to go again at the same length. */
   void dismiss() {
      this.reset();
   }

   /** Call on every tick: true once, on the tick the timer runs out. */
   boolean update(long now) {
      if (this.isRunning() && now >= this.endsAt) {
         this.finishedAt = this.endsAt;
         this.endsAt = 0;
         this.left = 0;
         return true;
      }
      return false;
   }

   /** Restores what was saved. A timer that ran out while closed comes back done, with its finishing time. */
   void restore(long length, long endsAt, long left, long finishedAt) {
      this.length = Math.max(0, Math.min(MAX, length));
      this.endsAt = Math.max(0, endsAt);
      this.left = Math.max(0, Math.min(this.length, left));
      this.finishedAt = Math.max(0, finishedAt);
   }
}
