package rubikscube;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The speed timer for a real cube, the way cubers time themselves: a scramble in face notation, then hold Space
 * until the time turns green, let go to start, and press again to stop. A tap shorter than {@link #HOLD_MILLIS} does
 * not start it, and a key held down (its repeats) counts as one press. Also the scramble and the averages.
 */
final class SolveTimer {
   static final long HOLD_MILLIS = 300;
   static final int SCRAMBLE_TURNS = 20;
   private static final String FACES = "UDFBRL";
   private static final String[] SUFFIXES = {"", "'", "2"};

   enum State { IDLE, HOLDING, RUNNING }

   private State state = State.IDLE;
   /** When the hold began (HOLDING) or the solve began (RUNNING). */
   private long since = 0L;
   private boolean down = false;
   private long last = -1L;

   State state() {
      return this.state;
   }

   /** Space or the pad pressed. Returns true when this press stopped a solve (its time is {@link #last}). */
   boolean press(long now) {
      if (this.down) {
         return false;
      }
      this.down = true;
      if (this.state == State.RUNNING) {
         this.last = now - this.since;
         this.state = State.IDLE;
         return true;
      }
      this.state = State.HOLDING;
      this.since = now;
      return false;
   }

   /** Space or the pad let go: starts the solve if it was held long enough. */
   void release(long now) {
      this.down = false;
      if (this.state == State.HOLDING) {
         boolean held = now - this.since >= HOLD_MILLIS;
         this.state = held ? State.RUNNING : State.IDLE;
         this.since = now;
      }
   }

   /** Held long enough that letting go starts the solve (the time shows green). */
   boolean ready(long now) {
      return this.state == State.HOLDING && now - this.since >= HOLD_MILLIS;
   }

   /** The running time, or the last solve's (-1 before any). */
   long shown(long now) {
      return this.state == State.RUNNING ? now - this.since : this.last;
   }

   long last() {
      return this.last;
   }

   void forgetLast() {
      this.last = -1L;
   }

   /** Back to waiting, with no solve under way (the card closed). */
   void cancel() {
      this.state = State.IDLE;
      this.down = false;
   }

   /**
    * A scramble of {@link #SCRAMBLE_TURNS} face turns such as {@code "R U' F2"}: never the same face twice in a row,
    * nor three turns on one axis (R L R), which would waste turns.
    */
   static String scramble(Random random) {
      List<String> turns = new ArrayList<>();
      int previous = -1;
      int before = -1;
      while (turns.size() < SCRAMBLE_TURNS) {
         int face = random.nextInt(6);
         boolean sameAxisTwice = previous >= 0 && before >= 0 && face / 2 == previous / 2 && face / 2 == before / 2;
         if (face == previous || sameAxisTwice) {
            continue;
         }
         turns.add(FACES.charAt(face) + SUFFIXES[random.nextInt(3)]);
         before = previous;
         previous = face;
      }
      return String.join(" ", turns);
   }

   /** A time as cubers write it, hundredths cut off rather than rounded: 9.87, 1:02.35. */
   static String format(long millis) {
      long hundredths = millis / 10;
      long seconds = hundredths / 100;
      String fraction = "." + (hundredths % 100 < 10 ? "0" : "") + hundredths % 100;
      if (seconds < 60) {
         return seconds + fraction;
      }
      return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60 + fraction;
   }

   /** The average of the last n times with the best and the worst left out (Ao5, Ao12), or -1 with fewer than n. */
   static long average(List<Long> times, int n) {
      if (times.size() < n || n < 3) {
         return -1L;
      }
      List<Long> recent = new ArrayList<>(times.subList(times.size() - n, times.size()));
      Collections.sort(recent);
      long sum = 0L;
      for (int i = 1; i < n - 1; i++) {
         sum += recent.get(i);
      }
      return sum / (n - 2);
   }

   static long best(List<Long> times) {
      return times.isEmpty() ? -1L : Collections.min(times);
   }
}
