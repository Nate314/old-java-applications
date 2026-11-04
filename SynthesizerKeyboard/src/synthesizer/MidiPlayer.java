package synthesizer;

import java.util.HashSet;
import java.util.Set;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

public final class MidiPlayer {
   private static final int VELOCITY = 100;
   private final Synthesizer synthesizer;
   private final MidiChannel[] channels;
   /** The notes sounding, and when the last one stopped, so the microphone can tell the keyboard's own sound from yours. */
   private final Set<Integer> sounding = new HashSet<>();
   private long lastOff;

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
      this.setInstrument(0, program);
   }

   /** Sets the instrument of one channel (the keys play on 0, the looper's layers on others). */
   public final void setInstrument(int channel, int program) {
      this.channels[channel].programChange(program);
   }

   /** Starts a note and stops it after the given time, using a one-shot Swing timer instead of waiting. */
   public final void playNote(int note, int millis) {
      this.noteOn(note);
      Timer release = new Timer(millis, event -> this.noteOff(note));
      release.setRepeats(false);
      release.start();
   }

   public final void noteOn(int note) {
      this.noteOn(0, note, VELOCITY);
   }

   public final void noteOff(int note) {
      this.noteOff(0, note);
   }

   public final void noteOn(int channel, int note, int velocity) {
      this.channels[channel].noteOn(note, velocity);
      this.sounding.add(channel << 7 | note);
   }

   public final void noteOff(int channel, int note) {
      this.channels[channel].noteOff(note);
      if (this.sounding.remove(channel << 7 | note)) {
         this.lastOff = System.currentTimeMillis();
      }
   }

   /** Whether no note has sounded for the last {@code millis} (a note's sound rings on a little after it stops). */
   public final boolean quietFor(long millis) {
      return this.sounding.isEmpty() && System.currentTimeMillis() - this.lastOff >= millis;
   }
}
