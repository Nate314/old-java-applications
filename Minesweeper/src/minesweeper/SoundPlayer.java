package minesweeper;

import java.util.ArrayDeque;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/** Plays notes and chords with Swing timers, so the event thread never sleeps. */
public final class SoundPlayer {
   private static final int VELOCITY = 127;
   private static final int NOTE_MILLIS = 40;
   private static final int CHORD_MILLIS = 750;
   private static final int BEND_CENTER = 8192;
   private static final int BEND_AMOUNT = 4096;
   private final Synthesizer synthesizer;
   private final MidiChannel[] channels;
   private final ArrayDeque<Integer> noteQueue = new ArrayDeque<>();
   private final Timer noteTimer;
   private int playingNote = -1;

   public SoundPlayer() {
      try {
         this.synthesizer = MidiSystem.getSynthesizer();
         this.channels = this.synthesizer.getChannels();
         this.synthesizer.open();
      } catch (MidiUnavailableException exception) {
         throw new RuntimeException(exception);
      }

      this.noteTimer = new Timer(NOTE_MILLIS, event -> this.playNextNote());
   }

   /** Queues a short note; queued notes play one after another like the original arpeggio. */
   public final void queueNote(int note) {
      this.noteQueue.add(note);
      if (!this.noteTimer.isRunning()) {
         this.playNextNote();
         this.noteTimer.start();
      }
   }

   private void playNextNote() {
      if (this.playingNote >= 0) {
         this.channels[0].noteOff(this.playingNote);
         this.playingNote = -1;
      }
      Integer next = this.noteQueue.poll();
      if (next == null) {
         this.noteTimer.stop();
      } else {
         this.playingNote = next;
         this.channels[0].noteOn(next, VELOCITY);
      }
   }

   /** Plays the notes together while bending the pitch down, then releases them and recenters the bend. */
   public final void playChord(int[] notes) {
      this.noteQueue.clear();
      this.channels[0].setChannelPressure(127);
      this.channels[0].setPitchBend(BEND_CENTER);
      for (int note : notes) {
         this.channels[0].noteOn(note, VELOCITY);
      }

      long start = System.currentTimeMillis();
      Timer bendTimer = new Timer(15, null);
      bendTimer.addActionListener(event -> {
         long elapsed = System.currentTimeMillis() - start;
         if (elapsed >= CHORD_MILLIS) {
            bendTimer.stop();
            for (int note : notes) {
               this.channels[0].noteOff(note);
            }
            this.channels[0].setPitchBend(BEND_CENTER);
         } else {
            this.channels[0].setPitchBend(BEND_CENTER - (int)(BEND_AMOUNT * elapsed / CHORD_MILLIS));
         }
      });
      bendTimer.start();
   }
}
