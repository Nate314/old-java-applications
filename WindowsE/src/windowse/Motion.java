package windowse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The little monitor's motion: each window (and the Start menu) glides from where it was drawn to where the screen
 * now puts it, easing out, so a new window grows out of the taskbar, a snap slides to its half and a minimize sinks
 * back to its icon. A new target mid-glide starts from where the item is drawn then, so presses 100 ms apart chain.
 */
final class Motion {
   static final int MILLIS = 220;

   private static final class Track {
      int[] from;
      int[] to;
      long start;
   }

   private final Map<Integer, Track> tracks = new HashMap<>();
   private final Set<Integer> seen = new HashSet<>();

   /** Where item key is drawn at now (ms), on its way to target; an item not seen before starts at from (null: target). */
   int[] at(int key, int[] target, int[] from, long now) {
      this.seen.add(key);
      Track t = this.tracks.get(key);
      if (t == null) {
         t = new Track();
         t.from = from == null ? target : from;
         t.to = target;
         t.start = now;
         this.tracks.put(key, t);
      } else if (!Arrays.equals(t.to, target)) {
         t.from = where(t, now);
         t.to = target;
         t.start = now;
      }
      return where(t, now);
   }

   /** After a frame: forgets items that frame did not draw, and says whether anything is still on its way. */
   boolean settle(long now) {
      this.tracks.keySet().retainAll(this.seen);
      this.seen.clear();
      for (Track t : this.tracks.values()) {
         if (now - t.start < MILLIS && !Arrays.equals(t.from, t.to)) {
            return true;
         }
      }
      return false;
   }

   private static int[] where(Track t, long now) {
      double p = Math.min(1, Math.max(0, (now - t.start) / (double) MILLIS));
      double e = 1 - (1 - p) * (1 - p) * (1 - p);   // ease out: quick to leave, gentle to land
      int[] r = new int[4];
      for (int i = 0; i < 4; i++) {
         r[i] = (int) Math.round(t.from[i] + (t.to[i] - t.from[i]) * e);
      }
      return r;
   }
}
