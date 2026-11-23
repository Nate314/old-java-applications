package synthesizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * The strip over the keys that shows the keyboard's whole reach: a small piano from C-1 to C9 (MIDI 0 to 120) with
 * the octave the keys play framed in gold and named ("C4"), and the notes held lit where they sound. A press on the
 * small keys moves the frame there (a drag slides it), and the arrows at each end step it an octave, like Left / Right.
 */
public final class RangeStrip extends JPanel {
   public static final int HEIGHT = 30;
   /** The octave shifts the keys reach: the frame's low C is MIDI 60 + 12 * shift. */
   public static final int LOWEST = -5;
   public static final int HIGHEST = 4;
   private static final int TOP_NOTE = 120;
   private static final int WHITES = 71;
   private static final int ARROW = 36;
   private static final int[] WHITE_AT = {0, -1, 1, -1, 2, 3, -1, 4, -1, 5, -1, 6};
   private static final Color BACKGROUND = new Color(20, 22, 30);
   private static final Color WHITE = new Color(214, 216, 224);
   private static final Color WHITE_OUT = new Color(104, 108, 124);
   private static final Color BLACK = new Color(22, 22, 28);
   private static final Color FRAME = new Color(255, 196, 60);
   private static final Color HELD = new Color(110, 150, 240);
   private static final Color CHIP = new Color(52, 57, 74);
   private static final Color TEXT = new Color(238, 240, 246);
   private static final Color SOFT = new Color(88, 94, 112);
   private static final Color LABEL = new Color(16, 18, 28);
   private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 11);

   private int shift;
   private final boolean[] held = new boolean[TOP_NOTE + 1];

   public RangeStrip() {
      this.setFocusable(false);
      this.setPreferredSize(new Dimension(0, HEIGHT));
   }

   public void setShift(int shift) {
      this.shift = shift;
      this.repaint();
   }

   /** Lights the MIDI notes sounding from the keys (notes off the strip are skipped). */
   public void setHeld(int[] notes) {
      java.util.Arrays.fill(this.held, false);
      for (int note : notes) {
         if (note >= 0 && note <= TOP_NOTE) {
            this.held[note] = true;
         }
      }
      this.repaint();
   }

   /** The name of the frame's low C, such as "C4" for no shift. */
   static String name(int shift) {
      return "C" + (4 + shift);
   }

   /** -1 or 1 for a point on the left or right arrow, 0 elsewhere. */
   public int arrowAt(int x) {
      return x < ARROW ? -1 : x >= this.getWidth() - ARROW ? 1 : 0;
   }

   /** The shift that centres the frame on the small keys at {@code x} (the nearest octave the keys reach). */
   public int shiftAt(int x) {
      double white = (x - this.left()) / this.whiteWidth();
      int octave = (int)Math.round((white - 4) / 7);
      return Math.max(LOWEST, Math.min(HIGHEST, octave - 5));
   }

   private int left() {
      return ARROW + 4;
   }

   private double whiteWidth() {
      return Math.max(1, this.getWidth() - 2 * this.left()) / (double)WHITES;
   }

   /** The left edge of the white key at {@code white} (0 for C-1), in pixels. */
   private int whiteX(int white) {
      return this.left() + (int)Math.round(white * this.whiteWidth());
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int width = this.getWidth();
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, width, HEIGHT);
      int top = 4;
      int bottom = HEIGHT - 4;
      int low = 60 + 12 * this.shift;
      for (int note = 0; note <= TOP_NOTE; note++) {
         int white = WHITE_AT[note % 12];
         if (white >= 0) {
            int x = this.whiteX(note / 12 * 7 + white);
            boolean inside = note >= low && note <= low + 12;
            g.setColor(this.held[note] ? HELD : inside ? WHITE : WHITE_OUT);
            g.fillRect(x, top, Math.max(1, this.whiteX(note / 12 * 7 + white + 1) - x - 1), bottom - top);
         }
      }
      int blackWidth = Math.max(1, (int)Math.round(this.whiteWidth() * 0.6));
      for (int note = 0; note <= TOP_NOTE; note++) {
         if (WHITE_AT[note % 12] < 0) {
            int x = this.whiteX(note / 12 * 7 + WHITE_AT[note % 12 - 1] + 1) - blackWidth / 2;
            g.setColor(this.held[note] ? HELD : BLACK);
            g.fillRect(x, top, blackWidth, (bottom - top) * 3 / 5);
         }
      }
      // The frame round the octave the keys play, named at its foot when the name fits.
      int frameLeft = this.whiteX((5 + this.shift) * 7) - 2;
      int frameRight = this.whiteX((5 + this.shift) * 7 + 8) + 1;
      g.setColor(FRAME);
      g.drawRoundRect(frameLeft, 1, frameRight - frameLeft, HEIGHT - 3, 6, 6);
      g.drawRoundRect(frameLeft + 1, 2, frameRight - frameLeft - 2, HEIGHT - 5, 5, 5);
      g.setFont(LABEL_FONT);
      FontMetrics metrics = g.getFontMetrics();
      String name = name(this.shift);
      int tag = metrics.stringWidth(name) + 8;
      if (tag + 4 < frameRight - frameLeft) {
         // A gold tag on the frame's foot, so the name reads over the small keys.
         int tagLeft = (frameLeft + frameRight - tag) / 2;
         g.fillRoundRect(tagLeft, HEIGHT - 15, tag, 13, 6, 6);
         g.setColor(LABEL);
         g.drawString(name, tagLeft + 4, HEIGHT - 5);
      }
      this.paintArrow(g, 2, -1, this.shift > LOWEST);
      this.paintArrow(g, width - ARROW + 2, 1, this.shift < HIGHEST);
   }

   /** An arrow chip {@code ARROW - 4} wide at {@code x}, pointing left (-1) or right (1), dim at the end of the reach. */
   private void paintArrow(Graphics2D g, int x, int way, boolean on) {
      int chip = ARROW - 4;
      g.setColor(CHIP);
      g.fillRoundRect(x, 3, chip, HEIGHT - 6, 10, 10);
      g.setColor(on ? TEXT : SOFT);
      int middle = x + chip / 2;
      int tip = middle + way * 5;
      int back = middle - way * 4;
      g.fillPolygon(new int[]{tip, back, back}, new int[]{HEIGHT / 2, HEIGHT / 2 - 6, HEIGHT / 2 + 6}, 3);
   }
}
