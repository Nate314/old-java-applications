package synthesizer;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

public final class MidiPlayer {
   private static final int VELOCITY = 100;
   private final Synthesizer synthesizer;
   private final MidiChannel[] channels;

   public MidiPlayer() {
      try {
         this.synthesizer = MidiSystem.getSynthesizer();
         this.channels = this.synthesizer.getChannels();
         this.synthesizer.open();
      } catch (MidiUnavailableException e) {
         throw new RuntimeException(e);
      }
   }

   public final void setInstrument(int program) {
      this.channels[0].programChange(program);
   }

   /** Starts a note and stops it after the given time, using a one-shot Swing timer instead of waiting. */
   public final void playNote(int note, int millis) {
      this.noteOn(note);
      Timer release = new Timer(millis, event -> this.noteOff(note));
      release.setRepeats(false);
      release.start();
   }

   public final void noteOn(int note) {
      this.channels[0].noteOn(note, VELOCITY);
   }

   public final void noteOff(int note) {
      this.channels[0].noteOff(note);
   }
}
