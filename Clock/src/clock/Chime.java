package clock;

import java.awt.Toolkit;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/** The timer's alarm: a falling-then-rising bell phrase on the MIDI synthesizer, repeated until stopped. */
final class Chime {
   private static final int TUBULAR_BELLS = 14;
   private static final int[] PHRASE = {84, 79, 76, 79, 84};
   private static final int NOTE_MILLIS = 220;
   private static final int PHRASE_MILLIS = 2400;
   private static final int REPEATS = 8;

   private MidiChannel channel;
   private boolean noSynth;
   private final Timer noteTimer = new Timer(NOTE_MILLIS, event -> this.nextNote());
   private final Timer phraseTimer = new Timer(PHRASE_MILLIS, event -> this.nextPhrase());
   private int note;
   private int phrases;
   private int sounding = -1;

   boolean isRinging() {
      return this.phraseTimer.isRunning();
   }

   /** Rings the phrase now and then every few seconds, {@code REPEATS} times in all. */
   void ring() {
      this.stop();
      this.phrases = 0;
      this.nextPhrase();
      this.phraseTimer.start();
   }

   void stop() {
      this.phraseTimer.stop();
      this.noteTimer.stop();
      this.release();
   }

   private void nextPhrase() {
      if (this.phrases++ >= REPEATS) {
         this.stop();
         return;
      }
      this.note = 0;
      this.nextNote();
      this.noteTimer.start();
   }

   private void nextNote() {
      this.release();
      if (this.note >= PHRASE.length) {
         this.noteTimer.stop();
         return;
      }
      MidiChannel bell = this.channel();
      if (bell == null) {
         if (this.note == 0) {
            Toolkit.getDefaultToolkit().beep();
         }
      } else {
         this.sounding = PHRASE[this.note];
         bell.noteOn(this.sounding, 110);
      }
      this.note++;
   }

   private void release() {
      if (this.sounding >= 0 && this.channel != null) {
         this.channel.noteOff(this.sounding);
      }
      this.sounding = -1;
   }

   /** The synthesizer's first channel, opened on the first ring; null when there is no synthesizer. */
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
