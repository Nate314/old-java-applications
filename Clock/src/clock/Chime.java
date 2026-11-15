package clock;

import java.awt.Toolkit;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/**
 * The timer's alarm: a falling-then-rising bell phrase on the MIDI synthesizer, repeated until stopped. Focus
 * sessions ring shorter phrases on the same bell, and the clock's quarter chimes toll once ({@link #toll}).
 */
final class Chime {
   private static final int TUBULAR_BELLS = 14;
   private static final int[] PHRASE = {84, 79, 76, 79, 84};
   /** Focus is over: a falling phrase, rung twice. */
   static final int[] BREAK_TIME = {84, 79, 76, 72};
   /** The break is over: the same notes rising. */
   static final int[] BACK_TO_WORK = {72, 76, 79, 84};
   private static final int NOTE_MILLIS = 220;
   private static final int PHRASE_MILLIS = 2400;
   private static final int REPEATS = 8;

   private MidiChannel channel;
   private boolean noSynth;
   private final Timer noteTimer = new Timer(NOTE_MILLIS, event -> this.nextNote());
   private final Timer phraseTimer = new Timer(PHRASE_MILLIS, event -> this.nextPhrase());
   private int[] phrase = PHRASE;
   private int repeats = REPEATS;
   private int note;
   private int phrases;
   private int sounding = -1;
   /** When the last bell was struck, and which; the face draws a ripple for it. */
   long struckAt;
   int struck;

   boolean isRinging() {
      return this.phraseTimer.isRunning();
   }

   /** Rings the phrase now and then every few seconds, {@code REPEATS} times in all. */
   void ring() {
      this.ring(PHRASE, REPEATS);
   }

   /** Rings {@code phrase} now and then every few seconds, {@code repeats} times in all. */
   void ring(int[] phrase, int repeats) {
      this.ring(phrase, repeats, NOTE_MILLIS, PHRASE_MILLIS);
   }

   /** Rings {@code steps} once, one every {@code stepMillis}; see {@link Westminster#REST} and {@link Westminster#HUSH}. */
   void toll(int[] steps, int stepMillis) {
      this.ring(steps, 1, stepMillis, (steps.length + 1) * stepMillis);
   }

   private void ring(int[] phrase, int repeats, int noteMillis, int phraseMillis) {
      this.stop();
      this.noteTimer.setDelay(noteMillis);
      this.noteTimer.setInitialDelay(noteMillis);
      this.phraseTimer.setDelay(phraseMillis);
      this.phraseTimer.setInitialDelay(phraseMillis);
      this.phrase = phrase;
      this.repeats = repeats;
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
      if (this.phrases++ >= this.repeats) {
         this.stop();
         return;
      }
      this.note = 0;
      this.nextNote();
      this.noteTimer.start();
   }

   private void nextNote() {
      if (this.note < this.phrase.length && this.phrase[this.note] < 0) {
         if (this.phrase[this.note++] == Westminster.HUSH) {
            this.release();
         }
         return;
      }
      this.release();
      if (this.note >= this.phrase.length) {
         this.noteTimer.stop();
         return;
      }
      MidiChannel bell = this.channel();
      if (bell == null) {
         if (this.note == 0) {
            Toolkit.getDefaultToolkit().beep();
         }
      } else {
         this.sounding = this.phrase[this.note];
         bell.noteOn(this.sounding, 110);
      }
      this.struckAt = System.currentTimeMillis();
      this.struck = this.phrase[this.note];
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
