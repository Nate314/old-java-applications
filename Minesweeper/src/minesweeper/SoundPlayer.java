package minesweeper;

import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public final class SoundPlayer {
   private final Synthesizer synthesizer;
   private final MidiChannel[] channels;
   private Map noteNumbers = new HashMap();
   private String[] noteNames = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

   public SoundPlayer() {
      try {
         this.synthesizer = MidiSystem.getSynthesizer();
         this.channels = this.synthesizer.getChannels();
         this.synthesizer.open();
      } catch (MidiUnavailableException exception) {
         throw new RuntimeException(exception);
      }

      for (int note = 0; note < 100; note++) {
         this.noteNumbers.put(this.noteNames[note % 12] + note / 12, note + 12);
      }
   }

   public final void playNote(int note, int durationArg, int releaseDelayArg, int pressureArg, int bendAmountArg) {
      byte bendAmount = 0;
      byte pressure = 0;
      byte releaseDelay = 2;
      byte duration = 20;
      this.channels[0].noteOn(note, 1000);
      if (bendAmount != 0) {
         this.bendPitch(duration, bendAmount, pressure);
      } else {
         sleepMillis((long)duration);
      }

      sleepMillis((long)duration);
      this.channels[0].noteOff(note);
      sleepMillis((long)releaseDelay);
   }

   public final void playChord(int[] notes, int durationArg, int releaseDelayArg, int pressureArg, int bendAmountArg) {
      short bendAmount = 4096;
      byte pressure = 127;
      byte releaseDelay = 10;
      short duration = 750;
      int[] chordNotes = notes;
      SoundPlayer player = this;

      for (int i = 0; i < chordNotes.length; i++) {
         player.channels[0].noteOn(chordNotes[i], 1000);
      }

      if (bendAmount != 0) {
         player.bendPitch(duration, bendAmount, pressure);
      } else {
         sleepMillis((long)duration);
      }

      for (int i = 0; i < chordNotes.length; i++) {
         player.channels[0].noteOff(chordNotes[i]);
      }

      sleepMillis((long)releaseDelay);
   }

   private void bendPitch(int duration, int bendAmount, int pressureThenRemaining) {
      this.channels[0].setChannelPressure(pressureThenRemaining);
      pressureThenRemaining = bendAmount;

      while (pressureThenRemaining > 0) {
         this.channels[0].setPitchBend(this.channels[0].getPitchBend() - bendAmount / duration);
         sleepMillis(1L);
         pressureThenRemaining -= bendAmount / duration;
      }
   }

   private static void sleepMillis(long millis) {
      try {
         long wakeTime = System.currentTimeMillis() + millis;

         while (true) {
            try {
               Thread.sleep(wakeTime - System.currentTimeMillis());
            } catch (InterruptedException ignored) {
            }
         }
      } catch (IllegalArgumentException finished) {
      }
   }
}
