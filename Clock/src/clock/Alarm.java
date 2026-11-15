package clock;

import java.util.Date;

/**
 * The alarm, set like a wind-up alarm clock's fourth hand: a time of day that rings every day while it is on, with a
 * nine-minute snooze. It keeps the chimes quiet through the night before it rings.
 */
final class Alarm {
   static final int DAY = 24 * 60;
   /** Up and Down, and the hand, move it in steps of five minutes. */
   static final int STEP = 5;
   static final long SNOOZE = 9 * Countdown.MINUTE;
   /** An alarm found later than this (the computer slept, or the tab was asleep) is skipped, not rung. */
   static final long LATE = 10 * Countdown.MINUTE;
   /** The chimes keep quiet for this long before the alarm: the night. */
   static final long NIGHT = 8 * 60 * Countdown.MINUTE;

   /** Minutes after midnight, 0 to 1439. */
   private int minute = 7 * 60;
   private boolean on;
   /** When it next rings, or 0 until worked out on the first tick (so one missed while closed does not ring). */
   private long nextAt;
   private long snoozeUntil;
   private boolean ringing;

   int minute() {
      return this.minute;
   }

   boolean isOn() {
      return this.on;
   }

   boolean isRinging() {
      return this.ringing;
   }

   /** When a snooze rings again, or 0. */
   long snoozeUntil() {
      return this.snoozeUntil;
   }

   /** When it rings next (the snooze, if one is waiting), or 0 when off. */
   long nextAt(long now) {
      if (!this.on) {
         return 0;
      }
      if (this.nextAt == 0) {
         this.nextAt = next(this.minute, now);
      }
      return this.snoozeUntil > 0 ? Math.min(this.snoozeUntil, this.nextAt) : this.nextAt;
   }

   /** Sets the time (wrapping round the day) and turns it on. */
   void set(int minute, long now) {
      this.minute = ((minute % DAY) + DAY) % DAY;
      this.on = true;
      this.snoozeUntil = 0;
      this.ringing = false;
      this.nextAt = next(this.minute, now);
   }

   /** The hand points at {@code dialMinute} of a 12-hour dial: like a real alarm clock, the next time the hour comes round. */
   void setFromDial(int dialMinute, int nowMinute, long now) {
      int am = ((dialMinute % 720) + 720) % 720;
      int pm = am + 720;
      this.set(until(am, nowMinute) <= until(pm, nowMinute) ? am : pm, now);
   }

   /** Minutes from {@code nowMinute} until {@code minute} next comes round, 1 to 1440. */
   private static int until(int minute, int nowMinute) {
      int wait = ((minute - nowMinute) % DAY + DAY) % DAY;
      return wait == 0 ? DAY : wait;
   }

   void toggle(long now) {
      if (this.on) {
         this.on = false;
         this.snoozeUntil = 0;
         this.ringing = false;
         this.nextAt = 0;
      } else {
         this.set(this.minute, now);
      }
   }

   /** Silences it until tomorrow. */
   void stop() {
      this.ringing = false;
      this.snoozeUntil = 0;
   }

   void snooze(long now) {
      this.ringing = false;
      this.snoozeUntil = now + SNOOZE;
   }

   /** True once each time it (or a snooze) comes due; one found more than {@link #LATE} late is skipped. */
   boolean update(long now) {
      if (!this.on) {
         return false;
      }
      this.nextAt(now);
      boolean ring = false;
      if (this.snoozeUntil > 0 && now >= this.snoozeUntil) {
         ring = now - this.snoozeUntil < LATE;
         this.snoozeUntil = 0;
      }
      if (now >= this.nextAt) {
         ring |= now - this.nextAt < LATE;
         this.nextAt = next(this.minute, now);
      }
      if (ring) {
         this.ringing = true;
      }
      return ring;
   }

   /** True in the night before it rings, when the quarter chimes keep quiet. */
   boolean quiet(long now) {
      long at = this.nextAt(now);
      return this.ringing || at > 0 && at - now <= NIGHT;
   }

   /** Restores the saved alarm; a snooze is kept if it has not yet come due. */
   void restore(int minute, boolean on, long snoozeUntil) {
      this.minute = minute >= 0 && minute < DAY ? minute : this.minute;
      this.on = on;
      this.snoozeUntil = on ? snoozeUntil : 0;
      this.nextAt = 0;
   }

   /** The first time after {@code now} that the local clock reads {@code minute}, today or tomorrow. */
   @SuppressWarnings("deprecation")
   static long next(int minute, long now) {
      // Date's getters and constructor are local time on the desktop and in the browser, daylight saving included.
      Date today = new Date(now);
      for (int day = 0; ; day++) {
         long at = new Date(today.getYear(), today.getMonth(), today.getDate() + day, minute / 60, minute % 60).getTime();
         if (at > now) {
            return at;
         }
      }
   }

   /** "8 h 5 min", "45 min": how long until {@code at}, rounded up to the minute. */
   static String wait(long at, long now) {
      long minutes = (Math.max(0, at - now) + Countdown.MINUTE - 1) / Countdown.MINUTE;
      return minutes >= 60 ? minutes / 60 + " h " + minutes % 60 + " min" : minutes + " min";
   }
}
