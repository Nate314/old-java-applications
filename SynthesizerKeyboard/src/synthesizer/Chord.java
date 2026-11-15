package synthesizer;

/**
 * Chord keys: each key plays a three-note chord built up from it. The white keys play the chords of C major (C, Dm,
 * Em, F, G, Am, Bdim, C), the black keys major chords (C#, D#, F#, G#, A#), so any key fits a song in C or a pop
 * change borrowed from outside it.
 */
public final class Chord {
   private static final int[] MAJOR = {0, 4, 7};
   private static final int[] MINOR = {0, 3, 7};
   private static final int[] DIMINISHED = {0, 3, 6};

   private Chord() {
   }

   /** The chord's notes above key {@code key}'s own, in semitones: root, third, fifth. */
   public static int[] intervals(int key) {
      return key == 2 || key == 4 || key == 9 ? MINOR : key == 11 ? DIMINISHED : MAJOR;
   }

   /** The keys that light for key {@code key}'s chord: notes past the high C show on the key of the same name below. */
   public static int[] keys(int key) {
      int[] intervals = intervals(key);
      int[] keys = new int[intervals.length];
      for (int tone = 0; tone < keys.length; tone++) {
         int up = key + intervals[tone];
         keys[tone] = up > 12 ? up - 12 : up;
      }
      return keys;
   }

   /** The chord's name, such as "C", "Dm" or "Bdim". */
   public static String name(int key) {
      int[] intervals = intervals(key);
      return Song.noteName(key) + (intervals == MINOR ? "m" : intervals == DIMINISHED ? "dim" : "");
   }
}
