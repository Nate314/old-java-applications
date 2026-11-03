package flappyBird;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/**
 * The game's sounds on the MIDI synthesizer: a tick for each flap, a two-note ding for each pipe passed, a thud for a
 * crash and a falling run of notes as the bird drops. The synthesizer opens on a background thread so the first flap
 * does not wait for it; until then (or without one) the game is silent.
 */
final class Sounds {
   private static final int XYLOPHONE = 13;
   private static final int GLOCKENSPIEL = 9;
   private static final int TAIKO = 116;
   private static final int PICCOLO = 72;

   private volatile MidiChannel[] channels;
   private boolean muted;

   Sounds() {
      Thread opener = new Thread(() -> {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] all = synthesizer.getChannels();
            all[0].programChange(XYLOPHONE);
            all[1].programChange(GLOCKENSPIEL);
            all[2].programChange(TAIKO);
            all[3].programChange(PICCOLO);
            this.channels = all;
         } catch (Exception e) {
            System.err.println("No sound: " + e.getMessage());
         }
      }, "flappy-sounds");
      opener.setDaemon(true);
      opener.start();
   }

   /** Toggles the sound; returns true when it is now on. */
   boolean toggle() {
      this.muted = !this.muted;
      return !this.muted;
   }

   void flap() {
      this.note(0, 88, 45, 0, 40);
   }

   void point() {
      this.note(1, 84, 80, 0, 90);
      this.note(1, 91, 95, 70, 220);
   }

   void hit() {
      this.note(2, 40, 120, 0, 150);
   }

   void fall() {
      this.note(3, 79, 60, 180, 90);
      this.note(3, 74, 55, 260, 90);
      this.note(3, 67, 50, 340, 160);
   }

   /** A quick falling run of bells, like a tape winding back. */
   void rewind() {
      for (int i = 0; i < 6; i++) {
         this.note(1, 96 - i * 4, 70 - i * 6, i * 45, 60);
      }
   }

   /** Plays a note on a channel after delayMillis, for lengthMillis. */
   private void note(int channel, int note, int velocity, int delayMillis, int lengthMillis) {
      MidiChannel[] all = this.channels;
      if (all == null || this.muted) {
         return;
      }
      MidiChannel ch = all[channel];
      later(delayMillis, () -> ch.noteOn(note, velocity));
      later(delayMillis + lengthMillis, () -> ch.noteOff(note));
   }

   private static void later(int millis, Runnable action) {
      if (millis <= 0) {
         action.run();
         return;
      }
      Timer timer = new Timer(millis, event -> action.run());
      timer.setRepeats(false);
      timer.start();
   }
}
