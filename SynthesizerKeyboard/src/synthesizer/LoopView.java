package synthesizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

/**
 * The looper's lanes, between the strip and the keys: a ruler of the two bars, then one lane per layer with its
 * sound and its notes drawn like a piano roll (higher notes higher), and a lane for the next take. The step playing
 * is lit and notes glow while they sound. It takes no room while the looper is off.
 */
public final class LoopView extends JPanel {
   public static final int HEIGHT = 168;
   private static final int MARGIN = 10;
   private static final int RULER = 18;
   private static final int LANE = 34;
   private static final Color BACKGROUND = new Color(24, 27, 36);
   private static final Color LANE_BACK = new Color(36, 40, 53);
   private static final Color GRID = new Color(56, 61, 80);
   private static final Color BAR_LINE = new Color(98, 104, 128);
   private static final Color NOW = new Color(255, 255, 255, 34);
   private static final Color TEXT = new Color(238, 240, 246);
   private static final Color SOFT = new Color(160, 166, 184);
   private static final Color RECORD = new Color(232, 72, 72);
   private static final Color[] COLORS = {
      new Color(255, 196, 60), new Color(96, 164, 255), new Color(110, 214, 140), new Color(240, 120, 200)
   };
   private static final Font LABEL = new Font("SansSerif", Font.BOLD, 13);
   private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
   private static final Font BIG = new Font("SansSerif", Font.BOLD, 22);
   /** The General MIDI sound families, eight programs each. */
   private static final String[] FAMILIES = {
      "Piano", "Bells", "Organ", "Guitar", "Bass", "Strings", "Ensemble", "Brass",
      "Reed", "Pipe", "Synth lead", "Synth pad", "Synth effects", "World", "Percussive", "Effects"
   };

   private Looper looper;
   /** The sound the next take will be recorded with. */
   private int instrument;

   public LoopView() {
      this.setFocusable(false);
      this.setPreferredSize(new Dimension(0, 0));
   }

   /** The family name and number of a General MIDI program, such as "Strings 48". */
   public static String sound(int program) {
      return FAMILIES[Math.max(0, Math.min(127, program)) / 8] + " " + program;
   }

   /** Shows the looper's lanes, or takes no room for null. */
   public void setLooper(Looper looper) {
      this.looper = looper;
      this.setPreferredSize(new Dimension(0, looper == null ? 0 : HEIGHT));
      this.revalidate();
      this.repaint();
   }

   public void setInstrument(int instrument) {
      this.instrument = instrument;
      this.repaint();
   }

   private boolean narrow() {
      return this.getWidth() < 520;
   }

   private int labelWidth() {
      return this.narrow() ? 30 : 118;
   }

   private int gridLeft() {
      return MARGIN + this.labelWidth() + 6;
   }

   private double cell() {
      return (this.getWidth() - MARGIN - this.gridLeft()) / (double)Loop.STEPS;
   }

   /** The lane at a point: a layer's index, the next take's lane (the number of layers), or -1. */
   public int laneAt(int x, int y) {
      if (this.looper == null || y < RULER || x < MARGIN || x > this.getWidth() - MARGIN) {
         return -1;
      }
      int lane = (y - RULER) / LANE;
      return lane < Loop.MAX_LAYERS && lane <= this.looper.loop().layers().size() ? lane : -1;
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      if (this.looper == null) {
         return;
      }
      this.paintRuler(g);
      List<Loop.Layer> layers = this.looper.loop().layers();
      for (int lane = 0; lane < Loop.MAX_LAYERS; lane++) {
         int top = RULER + lane * LANE;
         if (lane < layers.size()) {
            this.paintLayer(g, lane, top, layers.get(lane));
         } else if (lane == layers.size()) {
            this.paintNext(g, lane, top);
         } else {
            this.paintLaneBack(g, top, new Color(30, 33, 44));
         }
      }
      int step = this.looper.step();
      if (step >= 0) {
         int x = this.gridLeft() + (int)(step * this.cell());
         g.setColor(NOW);
         g.fillRect(x, RULER, (int)Math.ceil(this.cell()), Loop.MAX_LAYERS * LANE - 4);
         g.setColor(TEXT);
         g.drawLine(x, RULER - 4, x, RULER + Loop.MAX_LAYERS * LANE - 4);
      }
   }

   /** Beat numbers over the grid, bar numbers bold. */
   private void paintRuler(Graphics2D g) {
      double cell = this.cell();
      int step = this.looper.step();
      for (int beat = 0; beat < Loop.STEPS / Loop.STEPS_PER_BEAT; beat++) {
         int x = this.gridLeft() + (int)(beat * Loop.STEPS_PER_BEAT * cell);
         boolean bar = beat % Loop.BEATS_PER_BAR == 0;
         boolean now = step >= 0 && step / Loop.STEPS_PER_BEAT == beat;
         g.setFont(bar ? LABEL : SMALL);
         g.setColor(now ? COLORS[0] : bar ? TEXT : SOFT);
         // A narrow window has room for the bar's number alone.
         String barName = (this.narrow() ? "" : "Bar ") + (beat / Loop.BEATS_PER_BAR + 1);
         g.drawString(bar ? barName : String.valueOf(beat % Loop.BEATS_PER_BAR + 1), x + 3, RULER - 5);
      }
      if (this.narrow()) {
         return;
      }
      g.setFont(SMALL);
      g.setColor(SOFT);
      String tempo = this.looper.loop().tempo() + " bpm" + (this.looper.loop().click() ? "" : ", no click");
      g.drawString(tempo, MARGIN, RULER - 5);
   }

   private void paintLaneBack(Graphics2D g, int top, Color color) {
      g.setColor(color);
      g.fillRoundRect(MARGIN, top, this.getWidth() - 2 * MARGIN, LANE - 4, 8, 8);
      double cell = this.cell();
      for (int step = 0; step <= Loop.STEPS; step += Loop.STEPS_PER_BEAT) {
         int x = this.gridLeft() + (int)(step * cell);
         g.setColor(step % Loop.STEPS_PER_BAR == 0 ? BAR_LINE : GRID);
         g.drawLine(x, top + 2, x, top + LANE - 6);
      }
   }

   private void paintLayer(Graphics2D g, int lane, int top, Loop.Layer layer) {
      Color color = COLORS[lane];
      this.paintLaneBack(g, top, LANE_BACK);
      this.paintLabel(g, lane, top, layer.muted() ? SOFT : color,
         this.narrow() ? null : sound(layer.program), layer.muted() ? "muted" : null);
      Color notes = layer.muted() ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 70) : color;
      this.paintNotes(g, top, layer.notes(), notes, layer.muted() ? -1 : this.looper.step());
   }

   /** The next take's lane: what Space will do, or the take itself as it records. */
   private void paintNext(Graphics2D g, int lane, int top) {
      boolean taking = this.looper.taking();
      this.paintLaneBack(g, top, taking ? new Color(70, 36, 42) : new Color(30, 33, 44));
      this.paintLabel(g, lane, top, taking ? RECORD : SOFT, this.narrow() ? null : sound(this.instrument), taking ? "recording" : null);
      int left = this.gridLeft() + 8;
      int baseline = top + LANE / 2 + 2;
      if (!taking) {
         g.setFont(SMALL);
         g.setColor(SOFT);
         String hint = lane == 0
            ? "Space: record two bars. A bar of clicks counts you in; it then loops while you add layers."
            : "Space: add layer " + (lane + 1) + " at the next bar. Up / Down picks its sound, Left / Right its octave.";
         this.fitString(g, hint, left, baseline, this.getWidth() - MARGIN - left);
         return;
      }
      int beats = this.looper.beatsToTake();
      if (beats > 0) {
         g.setFont(BIG);
         g.setColor(RECORD);
         String count = String.valueOf(beats);
         g.drawString(count, left, top + LANE - 9);
         g.setFont(SMALL);
         g.setColor(TEXT);
         this.fitString(g, "Get ready: play when the count runs out", left + g.getFontMetrics(BIG).stringWidth(count) + 34,
            baseline, this.getWidth() - MARGIN - left - 60);
         return;
      }
      this.paintNotes(g, top, this.looper.takeNotes(), RECORD, -1);
   }

   /** A lane's number in its colour, then its sound and a note such as "muted". */
   private void paintLabel(Graphics2D g, int lane, int top, Color color, String sound, String note) {
      g.setColor(color);
      g.fillOval(MARGIN + 6, top + 8, 14, 14);
      g.setFont(LABEL);
      g.setColor(BACKGROUND);
      String number = String.valueOf(lane + 1);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(number, MARGIN + 13 - metrics.stringWidth(number) / 2, top + 20);
      if (sound == null) {
         return;
      }
      g.setColor(note != null ? color : TEXT);
      this.fitString(g, sound, MARGIN + 26, top + (note != null ? 13 : 20), this.labelWidth() - 26);
      if (note != null) {
         g.setFont(SMALL);
         g.drawString(note, MARGIN + 26, top + 26);
      }
   }

   /** Notes as bars across the lane, higher notes higher; a note sounding at {@code step} glows white. */
   private void paintNotes(Graphics2D g, int top, List<Loop.Note> notes, Color color, int step) {
      if (notes.isEmpty()) {
         return;
      }
      int low = Integer.MAX_VALUE;
      int high = Integer.MIN_VALUE;
      for (Loop.Note note : notes) {
         low = Math.min(low, note.midi);
         high = Math.max(high, note.midi);
      }
      double cell = this.cell();
      int room = LANE - 4 - 10;
      for (Loop.Note note : notes) {
         int y = top + 3 + (high == low ? room / 2 : (high - note.midi) * room / (high - low));
         boolean lit = step >= 0 && Math.floorMod(step - note.step, Loop.STEPS) < note.length;
         g.setColor(lit ? Color.WHITE : color);
         // A note from the second bar may ring on into the first: draw the part past the end at the start.
         int end = note.step + note.length;
         this.bar(g, note.step, Math.min(end, Loop.STEPS), y, cell);
         if (end > Loop.STEPS) {
            this.bar(g, 0, end - Loop.STEPS, y, cell);
         }
      }
   }

   private void bar(Graphics2D g, int from, int to, int y, double cell) {
      int x = this.gridLeft() + (int)(from * cell) + 1;
      int width = Math.max(3, (int)((to - from) * cell) - 2);
      g.fillRoundRect(x, y, width, 6, 5, 5);
   }

   private void fitString(Graphics2D g, String text, int x, int y, int room) {
      FontMetrics metrics = g.getFontMetrics();
      if (metrics.stringWidth(text) > room) {
         while (text.length() > 3 && metrics.stringWidth(text + "...") > room) {
            text = text.substring(0, text.length() - 1);
         }
         text = text.trim() + "...";
      }
      if (room > 20) {
         g.drawString(text, x, y);
      }
   }
}
