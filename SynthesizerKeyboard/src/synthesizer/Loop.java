package synthesizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A two-bar loop of up to four layers. Each layer is a line of notes on sixteenth-note steps (32 to the loop),
 * played by one MIDI instrument; a layer can be muted. The loop also has a tempo and a metronome click.
 */
public final class Loop {
   public static final int STEPS_PER_BEAT = 4;
   public static final int BEATS_PER_BAR = 4;
   public static final int STEPS_PER_BAR = STEPS_PER_BEAT * BEATS_PER_BAR;
   public static final int STEPS = 2 * STEPS_PER_BAR;
   public static final int MAX_LAYERS = 4;
   public static final int MAX_NOTES = 64;
   public static final int MIN_TEMPO = 60;
   public static final int MAX_TEMPO = 180;
   public static final int TEMPO_STEP = 4;
   public static final int DEFAULT_TEMPO = 96;

   /**
    * A note: the step it starts on (0 to 31), how many steps it lasts, and its MIDI note. A note recorded from the
    * second bar may ring on past the end of the loop into its first bar.
    */
   public static final class Note {
      public final int step;
      public final int length;
      public final int midi;

      Note(int step, int length, int midi) {
         this.step = step;
         this.length = length;
         this.midi = midi;
      }
   }

   /** One recorded line: the instrument (MIDI program) it plays on and its notes in the order they start. */
   public static final class Layer {
      public final int program;
      private final List<Note> notes;
      private boolean muted;

      Layer(int program, List<Note> notes) {
         this.program = program;
         this.notes = new ArrayList<>(notes);
         this.notes.sort((a, b) -> a.step - b.step);
      }

      public List<Note> notes() {
         return Collections.unmodifiableList(this.notes);
      }

      public boolean muted() {
         return this.muted;
      }

      /** The lowest and highest MIDI note in the layer. */
      public int lowest() {
         int low = Integer.MAX_VALUE;
         for (Note note : this.notes) {
            low = Math.min(low, note.midi);
         }
         return low;
      }

      public int highest() {
         int high = Integer.MIN_VALUE;
         for (Note note : this.notes) {
            high = Math.max(high, note.midi);
         }
         return high;
      }
   }

   private final List<Layer> layers = new ArrayList<>();
   private int tempo = DEFAULT_TEMPO;
   private boolean click = true;

   public List<Layer> layers() {
      return Collections.unmodifiableList(this.layers);
   }

   public boolean full() {
      return this.layers.size() >= MAX_LAYERS;
   }

   public int tempo() {
      return this.tempo;
   }

   /** Changes the tempo by {@code beats} per minute, within 60 to 180. */
   public void changeTempo(int beats) {
      this.tempo = Math.max(MIN_TEMPO, Math.min(MAX_TEMPO, this.tempo + beats));
   }

   /** How long one sixteenth-note step lasts at the tempo. */
   public double stepMillis() {
      return 60000.0 / this.tempo / STEPS_PER_BEAT;
   }

   public boolean click() {
      return this.click;
   }

   public void setClick(boolean click) {
      this.click = click;
   }

   /** Adds a layer, if there is room. */
   public boolean add(Layer layer) {
      if (this.full() || layer == null) {
         return false;
      }
      this.layers.add(layer);
      return true;
   }

   /** Removes the newest layer; false when there is none. */
   public boolean removeLast() {
      if (this.layers.isEmpty()) {
         return false;
      }
      this.layers.remove(this.layers.size() - 1);
      return true;
   }

   public void clear() {
      this.layers.clear();
   }

   public void toggleMute(int layer) {
      if (layer >= 0 && layer < this.layers.size()) {
         Layer l = this.layers.get(layer);
         l.muted = !l.muted;
      }
   }

   /**
    * One pass of recording. Positions are in steps from the step the take starts on, which is {@code offset} steps
    * into the loop (a take can start at either bar). A press up to half a step early counts as the first step, and
    * a press in the half step after the pass wraps round to it, so a note played right on the downbeat is kept.
    */
   public static final class Take {
      private final int offset;
      private final Map<Integer, Double> open = new HashMap<>();
      private final List<Note> notes = new ArrayList<>();

      public Take(int offset) {
         this.offset = Math.floorMod(offset, STEPS);
      }

      public int offset() {
         return this.offset;
      }

      /** Whether a press at {@code position} (steps from the start of the take) belongs to it. */
      public static boolean within(double position) {
         return position >= -0.5 && position < STEPS + 0.5;
      }

      public void press(int midi, double position) {
         if (within(position) && !this.open.containsKey(midi) && this.notes.size() + this.open.size() < MAX_NOTES) {
            this.open.put(midi, position);
         }
      }

      public void release(int midi, double position) {
         Double start = this.open.remove(midi);
         if (start != null) {
            this.add(midi, start, position);
         }
      }

      private void add(int midi, double from, double to) {
         int start = (int)Math.round(from);
         int step = Math.floorMod(start + this.offset, STEPS);
         int length = (int)Math.max(1, Math.round(to - from));
         // A note may ring to the end of the loop, but not round into its own start again.
         this.notes.add(new Note(step, Math.min(length, STEPS - Math.floorMod(start, STEPS)), midi));
      }

      /** The notes finished so far, and the ones still held as if released at {@code position}. */
      public List<Note> notesAt(double position) {
         List<Note> all = new ArrayList<>(this.notes);
         for (Map.Entry<Integer, Double> held : this.open.entrySet()) {
            int start = (int)Math.round(held.getValue());
            int length = (int)Math.max(1, Math.round(Math.min(position, STEPS) - held.getValue()));
            all.add(new Note(Math.floorMod(start + this.offset, STEPS), Math.min(length, STEPS - Math.floorMod(start, STEPS)), held.getKey()));
         }
         return all;
      }

      /** Ends the take (notes still held end with the pass): a layer on {@code program}, or null if nothing was played. */
      public Layer finish(int program) {
         for (Map.Entry<Integer, Double> held : new ArrayList<>(this.open.entrySet())) {
            this.add(held.getKey(), held.getValue(), STEPS);
         }
         this.open.clear();
         return this.notes.isEmpty() ? null : new Layer(program, this.notes);
      }
   }

   // ---- saving: tempo=96, click=1, then layer=program,muted,step.length.midi;... per layer ----

   public String encode() {
      StringBuilder text = new StringBuilder();
      text.append("tempo=").append(this.tempo).append('\n');
      text.append("click=").append(this.click ? 1 : 0).append('\n');
      for (Layer layer : this.layers) {
         text.append("layer=").append(layer.program).append(',').append(layer.muted ? 1 : 0).append(',');
         for (int i = 0; i < layer.notes.size(); i++) {
            Note note = layer.notes.get(i);
            text.append(i == 0 ? "" : ";").append(note.step).append('.').append(note.length).append('.').append(note.midi);
         }
         text.append('\n');
      }
      return text.toString();
   }

   /** Reads a loop written by encode; lines it does not understand are skipped. */
   public static Loop decode(String text) {
      Loop loop = new Loop();
      for (String line : text.split("\n")) {
         int split = line.indexOf('=');
         if (split < 0) {
            continue;
         }
         String key = line.substring(0, split).trim();
         String value = line.substring(split + 1).trim();
         try {
            if (key.equals("tempo")) {
               loop.tempo = DEFAULT_TEMPO;
               loop.changeTempo(Integer.parseInt(value) - DEFAULT_TEMPO);
            } else if (key.equals("click")) {
               loop.click = !value.equals("0");
            } else if (key.equals("layer")) {
               Layer layer = decodeLayer(value);
               if (layer != null) {
                  loop.add(layer);
               }
            }
         } catch (NumberFormatException e) {
            // Skip the line.
         }
      }
      return loop;
   }

   private static Layer decodeLayer(String value) {
      String[] parts = value.split(",", 3);
      if (parts.length < 3) {
         return null;
      }
      int program = Integer.parseInt(parts[0].trim());
      List<Note> notes = new ArrayList<>();
      for (String token : parts[2].split(";")) {
         String[] n = token.trim().split("\\.");
         if (n.length != 3 || notes.size() >= MAX_NOTES) {
            continue;
         }
         int step = Integer.parseInt(n[0]);
         int length = Integer.parseInt(n[1]);
         int midi = Integer.parseInt(n[2]);
         if (step >= 0 && step < STEPS && length >= 1 && length <= STEPS && midi >= 0 && midi < 128) {
            notes.add(new Note(step, length, midi));
         }
      }
      if (notes.isEmpty() || program < 0 || program > 127) {
         return null;
      }
      Layer layer = new Layer(program, notes);
      layer.muted = parts[1].trim().equals("1");
      return layer;
   }

   public static Loop load(File file) {
      StringBuilder text = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            text.append(line).append('\n');
         }
      } catch (IOException e) {
         // No loop yet.
      }
      return decode(text.toString());
   }

   public void save(File file) {
      try (Writer writer = new FileWriter(file)) {
         writer.write(this.encode());
      } catch (IOException e) {
         // Could not save: the loop lasts until the window closes.
      }
   }
}
