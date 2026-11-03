package synthesizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;
import javax.swing.Timer;

/**
 * Plays a {@link Loop} round and round and records new layers into it. Time is read from the wall clock on a
 * quick Swing timer, so the beat does not drift however late the timer runs. The first take of a stopped loop gets
 * one bar of count-in clicks; a take while it plays starts at the next bar and lasts one pass. Each layer plays on
 * its own MIDI channel (1 to 4) with its own instrument; the keys stay on channel 0 and the click on 9.
 */
public final class Looper {
   /** Told when the loop moves on a step or changes, and when a take ends ({@code layer} -1 when nothing was played). */
   public interface Listener {
      void changed();

      void taken(int layer);
   }

   private static final int FIRST_CHANNEL = 1;
   private static final int CLICK_CHANNEL = 9;
   /** General MIDI hi and low wood block. */
   private static final int CLICK_HIGH = 76;
   private static final int CLICK_LOW = 77;
   private static final int VELOCITY = 100;
   private static final int TICK_MILLIS = 8;
   /** After this many steps missed at once (a hidden page's timers sleep), it skips ahead instead of playing them all. */
   private static final int MAX_CATCH_UP = 6;

   private final Loop loop;
   private final MidiPlayer midi;
   private final File file;
   private final IntSupplier instrument;
   private final Listener listener;
   private final Timer timer;
   private boolean playing;
   /** The wall-clock time of step 0 of the first pass; the count-in is before it. */
   private double origin;
   /** The last step played, counted from the first pass (negative in the count-in). */
   private int lastStep;
   private Loop.Take take;
   /** The step (counted like lastStep) the take starts on. */
   private int takeStart;
   /** Notes sounding: {channel, MIDI note, step to stop on}. */
   private final List<int[]> sounding = new ArrayList<>();

   public Looper(MidiPlayer midi, File file, IntSupplier instrument, Listener listener) {
      this.midi = midi;
      this.file = file;
      this.instrument = instrument;
      this.listener = listener;
      this.loop = Loop.load(file);
      for (int layer = 0; layer < this.loop.layers().size(); layer++) {
         this.midi.setInstrument(FIRST_CHANNEL + layer, this.loop.layers().get(layer).program);
      }
      this.timer = new Timer(TICK_MILLIS, event -> this.tick());
   }

   public Loop loop() {
      return this.loop;
   }

   public boolean playing() {
      return this.playing;
   }

   /** Whether a take is waiting to start or recording. */
   public boolean taking() {
      return this.take != null;
   }

   /** The step of the loop that is playing (0 to 31), or -1 when stopped or counting in. */
   public int step() {
      return this.playing && this.lastStep >= 0 ? Math.floorMod(this.lastStep, Loop.STEPS) : -1;
   }

   /** Beats until the take starts recording (0 once it records). */
   public int beatsToTake() {
      if (this.take == null) {
         return 0;
      }
      int steps = this.takeStart - this.lastStep;
      return steps <= 0 ? 0 : (steps + Loop.STEPS_PER_BEAT - 1) / Loop.STEPS_PER_BEAT;
   }

   /** The take's notes so far, held ones included, or an empty list. */
   public List<Loop.Note> takeNotes() {
      return this.take == null ? new ArrayList<>() : this.take.notesAt(this.position() - this.takeStart);
   }

   /** Where the take started in the loop (0 or 16). */
   public int takeOffset() {
      return this.take == null ? 0 : this.take.offset();
   }

   private double position() {
      return (System.currentTimeMillis() - this.origin) / this.loop.stepMillis();
   }

   /** Starts a take (with a bar of count-in if the loop is stopped), or calls off the one under way. */
   public void record() {
      if (this.take != null) {
         this.take = null;
         this.listener.changed();
         return;
      }
      if (this.loop.full()) {
         return;
      }
      if (!this.playing) {
         this.origin = System.currentTimeMillis() + Loop.STEPS_PER_BAR * this.loop.stepMillis();
         this.lastStep = -Loop.STEPS_PER_BAR - 1;
         this.takeStart = 0;
         this.start();
      } else {
         double position = this.position();
         int bar = (int)Math.floor(position / Loop.STEPS_PER_BAR) * Loop.STEPS_PER_BAR;
         this.takeStart = Math.max(0, position - bar < 0.5 ? bar : bar + Loop.STEPS_PER_BAR);
      }
      this.take = new Loop.Take(this.takeStart);
      this.listener.changed();
   }

   /** Plays the loop from the top, or stops it (and any take). */
   public void togglePlay() {
      if (this.playing) {
         this.stop();
      } else {
         this.origin = System.currentTimeMillis();
         this.lastStep = -1;
         this.start();
      }
      this.listener.changed();
   }

   private void start() {
      this.playing = true;
      this.timer.start();
      this.tick();
   }

   public void stop() {
      this.playing = false;
      this.take = null;
      this.timer.stop();
      this.silence(-1);
   }

   /** A key went down or up while looping: it goes into the take, if one is recording. */
   public void press(int midiNote) {
      if (this.take != null) {
         this.take.press(midiNote, this.position() - this.takeStart);
      }
   }

   public void release(int midiNote) {
      if (this.take != null) {
         this.take.release(midiNote, this.position() - this.takeStart);
      }
   }

   public void toggleMute(int layer) {
      if (layer < this.loop.layers().size()) {
         this.loop.toggleMute(layer);
         this.silence(FIRST_CHANNEL + layer);
         this.save();
         this.listener.changed();
      }
   }

   /** Calls off the take, or else removes the newest layer; false when there was nothing to undo. */
   public boolean undo() {
      if (this.take != null) {
         this.take = null;
      } else if (this.loop.removeLast()) {
         this.silence(FIRST_CHANNEL + this.loop.layers().size());
         this.save();
      } else {
         return false;
      }
      this.listener.changed();
      return true;
   }

   public void clear() {
      this.stop();
      this.loop.clear();
      this.save();
      this.listener.changed();
   }

   /** Changes the tempo, keeping the place in the loop. */
   public void changeTempo(int beats) {
      double position = this.position();
      this.loop.changeTempo(beats);
      this.origin = System.currentTimeMillis() - position * this.loop.stepMillis();
      this.save();
      this.listener.changed();
   }

   public void toggleClick() {
      this.loop.setClick(!this.loop.click());
      this.save();
      this.listener.changed();
   }

   private void save() {
      this.loop.save(this.file);
   }

   private void tick() {
      if (!this.playing) {
         return;
      }
      double position = this.position();
      int now = (int)Math.floor(position);
      if (now - this.lastStep > MAX_CATCH_UP) {
         this.lastStep = now - 1;
      }
      boolean moved = false;
      while (this.lastStep < now) {
         this.lastStep++;
         this.play(this.lastStep);
         moved = true;
      }
      if (this.take != null && position >= this.takeStart + Loop.STEPS + 0.5) {
         this.endTake();
      } else if (moved) {
         this.listener.changed();
      }
   }

   /** Plays step {@code step}: ends notes that are over, clicks on the beat, and starts the layers' notes. */
   private void play(int step) {
      for (Iterator<int[]> it = this.sounding.iterator(); it.hasNext();) {
         int[] note = it.next();
         if (note[2] <= step) {
            this.midi.noteOff(note[0], note[1]);
            it.remove();
         }
      }
      boolean beat = Math.floorMod(step, Loop.STEPS_PER_BEAT) == 0;
      boolean bar = Math.floorMod(step, Loop.STEPS_PER_BAR) == 0;
      if (step < 0) {
         if (beat) {
            this.sound(CLICK_CHANNEL, bar ? CLICK_HIGH : CLICK_LOW, bar ? 120 : 90, step + 1);
         }
         return;
      }
      if (beat && (this.loop.click() || this.take != null && step < this.takeStart)) {
         this.sound(CLICK_CHANNEL, bar ? CLICK_HIGH : CLICK_LOW, bar ? 110 : 75, step + 1);
      }
      int inLoop = Math.floorMod(step, Loop.STEPS);
      List<Loop.Layer> layers = this.loop.layers();
      for (int layer = 0; layer < layers.size(); layer++) {
         this.playLayer(layer, inLoop, step);
      }
   }

   private void playLayer(int layer, int inLoop, int step) {
      Loop.Layer l = this.loop.layers().get(layer);
      if (l.muted()) {
         return;
      }
      for (Loop.Note note : l.notes()) {
         if (note.step == inLoop) {
            this.sound(FIRST_CHANNEL + layer, note.midi, VELOCITY, step + note.length);
         }
      }
   }

   private void sound(int channel, int note, int velocity, int until) {
      for (Iterator<int[]> it = this.sounding.iterator(); it.hasNext();) {
         int[] playing = it.next();
         if (playing[0] == channel && playing[1] == note) {
            this.midi.noteOff(channel, note);
            it.remove();
         }
      }
      this.midi.noteOn(channel, note, velocity);
      this.sounding.add(new int[]{channel, note, until});
   }

   /** Stops the notes sounding on {@code channel}, or on every channel for -1. */
   private void silence(int channel) {
      for (Iterator<int[]> it = this.sounding.iterator(); it.hasNext();) {
         int[] note = it.next();
         if (channel < 0 || note[0] == channel) {
            this.midi.noteOff(note[0], note[1]);
            it.remove();
         }
      }
   }

   /** The pass is over: the take becomes the newest layer, which joins in at once. */
   private void endTake() {
      Loop.Layer layer = this.take.finish(this.instrument.getAsInt());
      this.take = null;
      if (layer == null || !this.loop.add(layer)) {
         this.listener.taken(-1);
         return;
      }
      int index = this.loop.layers().size() - 1;
      this.midi.setInstrument(FIRST_CHANNEL + index, layer.program);
      this.save();
      this.playLayer(index, Math.floorMod(this.lastStep, Loop.STEPS), this.lastStep);
      this.listener.taken(index);
   }
}
