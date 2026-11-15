package groupcreator;

/**
 * The work-time countdown the groups work against: a length in whole minutes (kept with the class), and while it
 * runs, the moment it ends. Times are passed in, so the board and the self-check share one clock.
 */
final class WorkTimer {
   static final int MIN = 1;
   static final int MAX = 60;
   private static final long MINUTE = 60_000;

   /** The length of a work time, in minutes. */
   int minutes = 10;
   private long start;
   private long end;
   private boolean rang;

   boolean running() {
      return end > 0;
   }

   void start(long now) {
      start = now;
      end = now + minutes * MINUTE;
      rang = false;
   }

   void stop() {
      end = 0;
   }

   /**
    * A minute more or less: on the length when idle, and while it runs on the time left too (never below zero), and
    * the length follows, so the next work time is as long. True when it changed.
    */
   boolean adjust(int delta, long now) {
      int length = Math.max(MIN, Math.min(MAX, minutes + delta));
      if (running()) {
         long newEnd = Math.max(now, Math.max(end, now) + delta * MINUTE);
         if (newEnd > now) {
            rang = false;
         }
         end = newEnd;
         minutes = length;
         return true;
      }
      boolean changed = length != minutes;
      minutes = length;
      return changed;
   }

   /** Milliseconds left: the whole length when idle. */
   long left(long now) {
      return running() ? Math.max(0, end - now) : minutes * MINUTE;
   }

   boolean up(long now) {
      return running() && now >= end;
   }

   /** How much of this work time has gone, from 0 to 1. */
   float gone(long now) {
      return running() && end > start ? Math.min(1f, (float) (now - start) / (end - start)) : 0f;
   }

   /** True once, the first time it is asked after the time is up: when to ring. */
   boolean ring(long now) {
      if (up(now) && !rang) {
         rang = true;
         return true;
      }
      return false;
   }

   /** "9:05": minutes and seconds, rounded up, so the clock reads 0:00 only when the time is up. */
   static String clock(long millis) {
      long seconds = (millis + 999) / 1000;
      return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
   }
}
