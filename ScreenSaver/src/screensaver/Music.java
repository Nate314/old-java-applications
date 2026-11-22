package screensaver;

import java.util.Arrays;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

/**
 * M: each saver plays quiet music made by what it draws: a music box note for every bead that comes or goes, a
 * twinkle as a near star streaks past, vibes when a Mystify corner bounces, marimba as a pipe turns, a xylophone
 * bounce and a bell chord for a corner hit, the dot clock ticking its seconds and chiming its minutes, the fish
 * gulping flakes and bubbles popping, a harp step for every room the maze walker enters. Every note is from one
 * pentatonic scale, so any mix of them sounds right. Off when the saver starts and never saved, so the site's idle
 * screen saver stays silent. Savers ring through the static methods; Main says when anyone is listening.
 */
final class Music {
   /** Where the notes go: the MIDI synthesizer, or (in the self-check) a list. */
   interface Voice {
      void on(int part, int note, int velocity);

      void off(int part, int note);
   }

   static final int BEADS = 0, STARS = 1, MYSTIFY = 2, PIPES = 3, LOGO = 4, CLOCK = 5, TANK = 6, MAZE = 7;
   /** General MIDI programs, one channel per part: music box, celesta, vibes, marimba, xylophone, bells, kalimba, harp. */
   private static final int[] PROGRAMS = {10, 8, 11, 12, 13, 14, 108, 46};
   /** Each part's lowest note: its share 0 of the scale. */
   private static final int[] LOW = {60, 72, 60, 48, 60, 48, 60, 48};
   /** The least time between two of a part's notes, in seconds, so a fast saver does not turn into a buzz. */
   private static final double[] GAP = {0.06, 0.35, 0.12, 0.08, 0.05, 0.4, 0.18, 0.1};
   private static final int[] SCALE = {0, 2, 4, 7, 9};
   static final int NOTES = 11;
   private static final double HOLD_SECONDS = 0.6;
   private static final int SLOTS = 32;

   private static boolean live;
   private static Voice voice;
   private static boolean noSynth;
   private static double clock;
   private static final double[] last = new double[PROGRAMS.length];
   private static final int[] heldPart = new int[SLOTS];
   private static final int[] heldNote = new int[SLOTS];
   private static final double[] offAt = new double[SLOTS];
   private static int next;

   static {
      Arrays.fill(last, Double.NEGATIVE_INFINITY);
      Arrays.fill(heldNote, -1);
   }

   private Music() {
   }

   /** The self-check listens here instead of the synthesizer. */
   static void listen(Voice v) {
      voice = v;
   }

   /** Whether notes sound now: music on and a saver (not the gallery) on screen. Going quiet lets every note go. */
   static void live(boolean on) {
      live = on;
      if (!on) {
         release(Double.POSITIVE_INFINITY);
      }
   }

   static boolean live() {
      return live;
   }

   /** Runs r with no note ringing and without letting go of the ones sounding: the next saver, previewing in its corner, is quiet. */
   static void quietly(Runnable r) {
      boolean was = live;
      live = false;
      try {
         r.run();
      } finally {
         live = was;
      }
   }

   /** Advances the music's own clock by real (not sped up) milliseconds and lets go of notes that rang long enough. */
   static void tick(double millis) {
      clock += millis / 1000;
      release(clock);
   }

   /** The note a share (0 to 1) of the way up this part's scale. */
   static int note(int part, double share) {
      int step = (int)Math.round(Math.max(0, Math.min(1, share)) * (NOTES - 1));
      return LOW[part] + 12 * (step / SCALE.length) + SCALE[step % SCALE.length];
   }

   /**
    * Rings the note share of the way up the part's scale, loudness (0 to 1) hard, unless the music is off or the
    * part rang too recently. Returns whether it rang.
    */
   static boolean ring(int part, double share, double loudness) {
      if (!live || clock - last[part] < GAP[part]) {
         return false;
      }
      strike(part, note(part, share), loudness);
      return true;
   }

   /** Three notes of the scale at once from share up, whatever the part's gap: a corner hit, a minute, the orb. */
   static void chord(int part, double share, double loudness) {
      if (!live) {
         return;
      }
      int step = (int)Math.round(Math.max(0, Math.min(1, share)) * (NOTES - 5));
      for (int i = 0; i < 3; i++) {
         strike(part, note(part, (step + 2 * i) / (double)(NOTES - 1)), loudness);
      }
   }

   private static void strike(int part, int note, double loudness) {
      Voice out = voice();
      int slot = next;
      next = (next + 1) % SLOTS;
      if (heldNote[slot] >= 0 && out != null) {
         out.off(heldPart[slot], heldNote[slot]);
      }
      if (out != null) {
         out.on(part, note, 30 + (int)(70 * Math.max(0, Math.min(1, loudness))));
      }
      heldPart[slot] = part;
      heldNote[slot] = note;
      offAt[slot] = clock + HOLD_SECONDS;
      last[part] = clock;
   }

   private static void release(double until) {
      for (int slot = 0; slot < SLOTS; slot++) {
         if (heldNote[slot] >= 0 && until >= offAt[slot]) {
            if (voice != null) {
               voice.off(heldPart[slot], heldNote[slot]);
            }
            heldNote[slot] = -1;
         }
      }
   }

   /** Notes sounding now. */
   static int sounding() {
      int count = 0;
      for (int note : heldNote) {
         count += note >= 0 ? 1 : 0;
      }
      return count;
   }

   /** Opens the synthesizer on the first note; with none the savers carry on in silence. */
   private static Voice voice() {
      if (voice == null && !noSynth) {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] channels = synthesizer.getChannels();
            for (int i = 0; i < PROGRAMS.length; i++) {
               channels[i].programChange(PROGRAMS[i]);
            }
            voice = new Voice() {
               @Override
               public void on(int part, int note, int velocity) {
                  channels[part].noteOn(note, velocity);
               }

               @Override
               public void off(int part, int note) {
                  channels[part].noteOff(note);
               }
            };
         } catch (Exception e) {
            noSynth = true;
         }
      }
      return voice;
   }
}
