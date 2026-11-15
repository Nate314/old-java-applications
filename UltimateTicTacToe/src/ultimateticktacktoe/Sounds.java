package ultimateticktacktoe;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/**
 * The game heard on the MIDI synthesizer. Each cell of a small board has its own note of a pentatonic scale, rising in
 * reading order, so every game plays a tune: X on the piano, O on the guitar an octave lower. A won board plays the
 * three notes of its winning line as the line strikes through it, and a won game the three winning boards' notes. The
 * synthesizer opens on a background thread; until then (or without one, or for silent()) the game is quiet.
 */
final class Sounds {
   /** C major pentatonic, two octaves: a cell's (or a board's) index picks its note. */
   private static final int[] SCALE = {0, 2, 4, 7, 9, 12, 14, 16, 19};
   private static final int X_BASE = 60;
   private static final int O_BASE = 48;
   private static final int PIANO = 0;
   private static final int GUITAR = 24;

   private volatile MidiChannel[] channels;
   private boolean on;
   /** What a silent copy was asked to play, as "channel:note", for the self-check; null when playing for real. */
   final List<String> heard;

   private Sounds(boolean on, List<String> heard) {
      this.on = on;
      this.heard = heard;
   }

   static Sounds open(boolean on) {
      Sounds s = new Sounds(on, null);
      Thread opener = new Thread(() -> {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] all = synthesizer.getChannels();
            all[0].programChange(PIANO);
            all[1].programChange(GUITAR);
            s.channels = all;
         } catch (Exception e) {
            System.err.println("No sound: " + e.getMessage());
         }
      }, "tictactoe-sounds");
      opener.setDaemon(true);
      opener.start();
      return s;
   }

   /** Never makes a sound; lists what it was asked to play instead. */
   static Sounds silent(boolean on) {
      return new Sounds(on, new ArrayList<>());
   }

   boolean isOn() {
      return this.on;
   }

   void setOn(boolean on) {
      this.on = on;
   }

   /** The note of a cell (or, for the game's line, a board) by its index 0-8, in X's or O's octave. */
   static int note(int index, boolean x) {
      return (x ? X_BASE : O_BASE) + SCALE[index];
   }

   /** A mark placed on a cell, now. */
   void mark(int cell, boolean x) {
      this.note(x, note(cell, x), 80, 0, 260);
   }

   /** A line of three won: its notes one after another over {@code millis}, after {@code delay}, the last held. */
   void line(int[] line, boolean x, int delay, int millis, int octave) {
      for (int i = 0; i < 3; i++) {
         this.note(x, note(line[i], x) + octave, 90 + i * 10, delay + i * millis / 3, i == 2 ? 600 : 200);
      }
   }

   /** A board (or the game) ending without a winner: two soft falling notes. */
   void drawn(int delay) {
      this.note(false, O_BASE + 7, 60, delay, 160);
      this.note(false, O_BASE, 60, delay + 160, 300);
   }

   /** A click where the next move may not go: a short, soft low note. */
   void refused() {
      this.note(false, 36, 50, 0, 70);
   }

   private void note(boolean x, int note, int velocity, int delayMillis, int lengthMillis) {
      if (!this.on) {
         return;
      }
      int channel = x ? 0 : 1;
      if (this.heard != null) {
         this.heard.add(channel + ":" + note);
         return;
      }
      MidiChannel[] all = this.channels;
      if (all == null) {
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
