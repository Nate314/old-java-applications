package stockchecker;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/** The opening and closing bell: a few strikes of a tubular bell on the MIDI synthesizer. Silent without one. */
final class Bell {
   private static final int TUBULAR_BELLS = 14;
   private static final int NOTE = 76;
   private final Timer strike = new Timer(380, event -> this.strike());
   private MidiChannel channel;
   private boolean noSynth;
   private int left;

   void ring(int strikes) {
      this.left = strikes;
      this.strike();
      this.strike.start();
   }

   private void strike() {
      MidiChannel bell = this.channel();
      if (bell != null) {
         bell.noteOff(NOTE);
      }
      if (this.left <= 0) {
         this.strike.stop();
         return;
      }
      this.left--;
      if (bell != null) {
         bell.noteOn(NOTE, 100);
      }
   }

   private MidiChannel channel() {
      if (this.channel == null && !this.noSynth) {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            this.channel = synthesizer.getChannels()[0];
            this.channel.programChange(TUBULAR_BELLS);
         } catch (Exception e) {
            this.noSynth = true;
         }
      }
      return this.channel;
   }
}
