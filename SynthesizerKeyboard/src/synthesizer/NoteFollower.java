package synthesizer;

/**
 * Turns a stream of pitch readings (one every few hundredths of a second) into notes being started, the way a key
 * press starts one: a note counts once its pitch has held for a few readings, and only once however long it is held.
 * The same note counts again after a pause, an unpitched sound (a consonant), or a new attack after its sound fell
 * away (singing "la la la" on one note, or striking a fading string again). Notes are compared by pitch class, so a
 * reading that jumps an octave (a common slip of pitch finding) does not break a held note.
 */
public final class NoteFollower {
   /** Readings in a row a pitch must hold to count as a note. */
   static final int STEADY = 4;
   /** Readings in a row without a pitch that end the note. */
   static final int GAP = 3;
   /** A note's sound falling under this share of its loudest point lets a new attack on it count again. */
   private static final double FADED = 0.3;
   /** ...and a new attack is the sound coming back above this many times its quietest point since. */
   private static final double ATTACK = 2.5;

   private int candidate = -1;
   private int steady;
   private int gap;
   /** The note sounding (counted already), or -1. */
   private int current = -1;
   private double loudest;
   private double quietest;
   private boolean faded;

   /**
    * Takes one reading: the pitch in Hz (-1 for none) and the loudness. Returns the MIDI note that has just started,
    * or -1.
    */
   public int feed(double hz, double level) {
      if (hz <= 0 || level < PitchDetector.QUIET) {
         if (++this.gap >= GAP) {
            this.current = -1;
            this.candidate = -1;
            this.steady = 0;
         }
         return -1;
      }
      this.gap = 0;
      int note = PitchDetector.midiNote(hz);
      if (this.candidate >= 0 && same(note, this.candidate)) {
         this.steady++;
      } else {
         this.candidate = note;
         this.steady = 1;
      }
      if (this.current >= 0) {
         this.loudest = Math.max(this.loudest, level);
         if (!this.faded && level < this.loudest * FADED) {
            this.faded = true;
            this.quietest = level;
         } else if (this.faded) {
            this.quietest = Math.min(this.quietest, level);
            if (level > this.quietest * ATTACK) {
               // Struck again: the note must hold steady once more to count.
               this.current = -1;
               this.steady = 1;
            }
         }
      }
      if (this.steady >= STEADY && (this.current < 0 || !same(this.candidate, this.current))) {
         this.current = this.candidate;
         this.loudest = level;
         this.faded = false;
         return note;
      }
      return -1;
   }

   private static boolean same(int a, int b) {
      return Math.floorMod(a - b, 12) == 0;
   }
}
