package synthesizer;

import java.util.Arrays;

/**
 * The backing under a lesson: chords worked out from the song's own notes, so the five songs and any tune a friend
 * sends play over a band. The song is cut into stretches of two beats (each starts at the first note on or after a
 * two-beat line); a stretch takes the chord that holds the most of its notes' beats, from the chords of C major (C,
 * F, G, Am, Dm, Em, most usual first) and the major chords on the black keys, and keeps the chord before it when
 * that one holds as many.
 */
public final class Backing {
   private static final double STRETCH = 2;
   /** The chords tried, by their key (see Chord); a tie goes to the earlier one. */
   private static final int[] CANDIDATES = {0, 5, 7, 9, 2, 4, 1, 3, 6, 8, 10};

   private Backing() {
   }

   /** For each note of {@code song}, the key of the chord that starts with it, or -1 where the chord before goes on. */
   public static int[] chords(Song song) {
      int[] chords = new int[song.length()];
      Arrays.fill(chords, -1);
      int current = -1;
      int first = 0;
      while (first < song.length()) {
         double end = Math.floor(song.start(first) / STRETCH) * STRETCH + STRETCH;
         double[] beats = new double[12];
         int next = first;
         while (next < song.length() && (next == first || song.start(next) < end)) {
            beats[song.key(next) % 12] += song.beats(next);
            next++;
         }
         int best = CANDIDATES[0];
         for (int chord : CANDIDATES) {
            if (held(chord, beats) > held(best, beats)) {
               best = chord;
            }
         }
         if (current < 0 || held(current, beats) < held(best, beats)) {
            chords[first] = best;
            current = best;
         }
         first = next;
      }
      return chords;
   }

   /** The key of the chord sounding at note {@code note}: the last one that started at or before it. */
   public static int at(int[] chords, int note) {
      for (int each = Math.min(note, chords.length - 1); each >= 0; each--) {
         if (chords[each] >= 0) {
            return chords[each];
         }
      }
      return -1;
   }

   /** The MIDI notes that play chord {@code chord}: its root two octaves below the keys, then the chord one below. */
   public static int[] notes(int chord, int middleC) {
      int[] intervals = Chord.intervals(chord);
      int[] notes = new int[intervals.length + 1];
      notes[0] = middleC - 24 + chord;
      for (int tone = 0; tone < intervals.length; tone++) {
         notes[tone + 1] = middleC - 12 + chord + intervals[tone];
      }
      return notes;
   }

   /** How many of the stretch's beats ({@code beats} by pitch class) chord {@code chord} holds. */
   private static double held(int chord, double[] beats) {
      double held = 0;
      for (int interval : Chord.intervals(chord)) {
         held += beats[(chord + interval) % 12];
      }
      return held;
   }
}
