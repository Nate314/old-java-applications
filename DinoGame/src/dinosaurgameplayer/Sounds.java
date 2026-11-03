package dinosaurgameplayer;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/**
 * The game's beeps on the MIDI synthesizer: a blip for each jump, a two-note chime every 100 points, a rising run for
 * a new best and a low buzz for a crash. The synthesizer opens on a background thread so the first jump does not
 * wait for it; until then (or without one, or for silent()) the game is quiet.
 */
final class Sounds {
   private static final int XYLOPHONE = 13;
   private static final int GLOCKENSPIEL = 9;
   private static final int SAW_LEAD = 81;

   private volatile MidiChannel[] channels;
   private boolean on;

   private Sounds(boolean on) {
      this.on = on;
   }

   /** Sounds on the system synthesizer, on or muted to begin with. */
   static Sounds open(boolean on) {
      Sounds s = new Sounds(on);
      Thread opener = new Thread(() -> {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] all = synthesizer.getChannels();
            all[0].programChange(XYLOPHONE);
            all[1].programChange(GLOCKENSPIEL);
            all[2].programChange(SAW_LEAD);
            s.channels = all;
         } catch (Exception e) {
            System.err.println("No sound: " + e.getMessage());
         }
      }, "dino-sounds");
      opener.setDaemon(true);
      opener.start();
      return s;
   }

   /** Never makes a sound (for the self-check); the on/off switch still works. */
   static Sounds silent(boolean on) {
      return new Sounds(on);
   }

   boolean isOn() {
      return this.on;
   }

   void setOn(boolean on) {
      this.on = on;
   }

   void jump() {
      this.note(0, 84, 70, 0, 45);
   }

   void milestone() {
      this.note(1, 88, 85, 0, 80);
      this.note(1, 93, 95, 90, 200);
   }

   void newBest() {
      for (int i = 0; i < 4; i++) {
         this.note(1, 84 + i * 4, 70 + i * 6, i * 70, 90);
      }
   }

   void crash() {
      this.note(2, 40, 110, 0, 120);
      this.note(2, 35, 100, 110, 220);
   }

   /** Plays a note on a channel after delayMillis, for lengthMillis. */
   private void note(int channel, int note, int velocity, int delayMillis, int lengthMillis) {
      MidiChannel[] all = this.channels;
      if (all == null || !this.on) {
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
