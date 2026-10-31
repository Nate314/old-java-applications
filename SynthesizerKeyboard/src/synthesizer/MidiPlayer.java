package synthesizer;

import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public final class MidiPlayer {
   private final Synthesizer synthesizer;
   private final MidiChannel[] channels;
   private Map noteNumbers = new HashMap();
   private String[] noteNames = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
   private KeyboardWindow window;

   public MidiPlayer(KeyboardWindow window) {
      this.window = window;

      try {
         this.synthesizer = MidiSystem.getSynthesizer();
         this.channels = this.synthesizer.getChannels();
         this.synthesizer.open();
      } catch (MidiUnavailableException e) {
         throw new RuntimeException(e);
      }

      for (int note = -36; note < 132; note++) {
         this.noteNumbers.put(this.noteNames[Math.abs(note) % 12] + note / 12, note + 12);
      }
   }

   public final void setInstrument(int unusedChannel, int program) {
      this.channels[0].programChange(program);
   }

   public final void playNote(int note, int unusedHoldTicks, int unusedRestTicks, int unusedBendAmount, int unusedBendSteps, int unusedPressure) {
      byte channel = 0;
      byte bendAmount = 0;
      byte pressure = 0;
      byte restTicks = 10;
      short holdTicks = 150;
      this.noteOn(channel, note);
      if (bendAmount != 0) {
         bendAmount = bendAmount;
         short bendSteps = holdTicks;
         MidiPlayer player = this;
         this.channels[0].setChannelPressure(pressure);

         for (int remainingBend = bendAmount; remainingBend > 0; remainingBend -= bendAmount / bendSteps) {
            player.channels[0].setPitchBend(player.channels[0].getPitchBend() - bendAmount / bendSteps);
            player.waitTicks(1L);
         }
      } else {
         this.waitTicks((long)holdTicks);
      }

      this.noteOff(channel, note);
      this.waitTicks((long)restTicks);
   }

   public final void noteOn(int channel, int note) {
      this.channels[channel].noteOn(note, 1000);
   }

   public final void noteOff(int channel, int note) {
      this.channels[channel].noteOff(note);
   }

   private void waitTicks(long ticks) {
      long deadline = ticks + this.window.tickCount;

      while (this.window.tickCount < deadline) {
         try {
            Thread.sleep(1L);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
}
