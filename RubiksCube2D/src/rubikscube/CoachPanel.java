package rubikscube;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * The Teach me card: which of the method's seven stages the cube is at, what the step is for, its turns as chips in
 * the color of the face to turn (an arrow for clockwise or back, the one to make now ringed, those made faded) and
 * how to make the next one. Painted by hand so the text wraps the same on the desktop and in the browser.
 */
final class CoachPanel extends JPanel {
   private static final int CHIP = 18;
   private static final int PITCH = 21;
   private static final int MAX_CHIPS = 16;
   private static final Color CARD = new Color(244, 247, 253);
   private static final Color EDGE = new Color(206, 214, 232);
   private static final Color INK = new Color(28, 32, 44);
   private static final Color MUTED = new Color(104, 112, 132);
   private static final Color DONE = new Color(46, 160, 90);
   private static final Color NOW = new Color(40, 96, 220);
   private static final Color LATER = new Color(214, 220, 234);
   private int stage = -1;
   private String title = "";
   private String text = "";
   private String footer = "";
   private final List<String> chipColors = new ArrayList<>();
   private final List<Boolean> chipClockwise = new ArrayList<>();
   private int chipNow = 0;

   CoachPanel() {
      this.setLayout(null);
      this.setOpaque(false);
   }

   /** A card with no turns: waiting for a mix, or done. Stage -1 hides the stage bar, 7 fills it. */
   void show(int stage, String title, String text, String footer) {
      this.stage = stage;
      this.title = title;
      this.text = text;
      this.footer = footer;
      this.chipColors.clear();
      this.chipClockwise.clear();
      this.repaint();
   }

   /** A step: its turns as the colors of the faces to turn and their way, with turn {@code now} the next one. */
   void step(int stage, String text, List<String> colors, List<Boolean> clockwise, int now, String footer) {
      this.show(stage, "Step " + (stage + 1) + ": " + Coach.STAGES[stage], text, footer);
      this.chipColors.addAll(colors);
      this.chipClockwise.addAll(clockwise);
      this.chipNow = now;
      this.repaint();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int width = this.getWidth();
      int height = this.getHeight();
      g.setColor(CARD);
      g.fillRoundRect(0, 0, width - 1, height - 1, 14, 14);
      g.setColor(EDGE);
      g.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);
      int x = 9;
      int inner = width - 2 * x;
      g.setFont(new Font("Dialog", Font.BOLD, 10));
      g.setColor(MUTED);
      g.drawString("TEACH ME", x, 16);
      int y = 22;
      if (this.stage >= 0) {
         int gap = 3;
         int segment = (inner - 6 * gap) / 7;
         for (int i = 0; i < 7; i++) {
            g.setColor(i < this.stage ? DONE : i == this.stage ? NOW : LATER);
            g.fillRoundRect(x + i * (segment + gap), y, segment, 5, 4, 4);
         }
         y += 8;
      }
      g.setColor(INK);
      g.setFont(new Font("Dialog", Font.BOLD, 13));
      y = this.wrap(g, this.title, x, y + 15, inner, 16, 3);
      g.setFont(new Font("Dialog", Font.PLAIN, 12));
      g.setColor(INK);
      FontMetrics small = g.getFontMetrics(new Font("Dialog", Font.PLAIN, 11));
      int footerLines = this.lines(small, this.footer, inner).size();
      int chipRows = (Math.min(MAX_CHIPS, this.chipColors.size()) + 7) / 8;
      int room = height - y - footerLines * 13 - chipRows * PITCH - 16;
      y = this.wrap(g, this.text, x, y + 16, inner, 15, Math.max(1, room / 15));
      if (!this.chipColors.isEmpty()) {
         this.drawChips(g, x, y + 6);
      }
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      g.setColor(MUTED);
      this.wrap(g, this.footer, x, height - 8 - (footerLines - 1) * 13, inner, 13, footerLines);
   }

   /** The chips, at most two rows of eight, scrolled so the next turn shows. */
   private void drawChips(Graphics2D g, int x, int y) {
      int count = this.chipColors.size();
      int first = Math.max(0, Math.min(this.chipNow - 4, count - MAX_CHIPS));
      int last = Math.min(count, first + MAX_CHIPS);
      for (int i = first; i < last; i++) {
         int cx = x + (i - first) % 8 * PITCH;
         int cy = y + (i - first) / 8 * PITCH;
         Color face = CubeView3D.colorOf(this.chipColors.get(i));
         boolean light = this.chipColors.get(i).equals("white") || this.chipColors.get(i).equals("yellow")
               || this.chipColors.get(i).equals("orange");
         Color mark = light ? INK : Color.WHITE;
         if (i < this.chipNow) {
            face = CubeView3D.mix(face, CARD, 0.7);
            mark = CubeView3D.mix(mark, CARD, 0.7);
         }
         if (i == this.chipNow) {
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(cx - 2, cy - 2, CHIP + 3, CHIP + 3, 8, 8);
         }
         g.setColor(face);
         g.fillRoundRect(cx, cy, CHIP, CHIP, 6, 6);
         g.setStroke(new BasicStroke(1f));
         g.setColor(CubeView3D.mix(face, INK, 0.35));
         g.drawRoundRect(cx, cy, CHIP - 1, CHIP - 1, 6, 6);
         this.drawTurnArrow(g, cx + CHIP / 2, cy + CHIP / 2, this.chipClockwise.get(i), mark);
      }
      if (count > MAX_CHIPS) {
         g.setFont(new Font("Dialog", Font.PLAIN, 10));
         g.setColor(MUTED);
         g.drawString((first > 0 ? "< " : "") + (last < count ? ">" : ""), x + 8 * PITCH - 2, y + 12);
      }
   }

   /** A round arrow: clockwise or back (counterclockwise), its head at the top of the left or right side. */
   private void drawTurnArrow(Graphics2D g, int cx, int cy, boolean clockwise, Color color) {
      g.setColor(color);
      g.setStroke(new BasicStroke(1.6f));
      g.drawArc(cx - 5, cy - 5, 10, 10, 90, clockwise ? -270 : 270);
      int hx = clockwise ? cx - 5 : cx + 5;
      g.fillPolygon(new int[]{hx - 3, hx + 3, hx}, new int[]{cy + 1, cy + 1, cy - 4}, 3);
      g.setStroke(new BasicStroke(1f));
   }

   /** Draws text wrapped to the width, at most maxLines lines (the last ends in ... if cut); returns the last baseline. */
   private int wrap(Graphics2D g, String words, int x, int y, int width, int lineHeight, int maxLines) {
      List<String> lines = this.lines(g.getFontMetrics(), words, width);
      int baseline = y - lineHeight;
      for (int i = 0; i < lines.size() && i < maxLines; i++) {
         String line = lines.get(i);
         if (i == maxLines - 1 && lines.size() > maxLines) {
            line += "...";
         }
         baseline = y + i * lineHeight;
         g.drawString(line, x, baseline);
      }
      return baseline;
   }

   private List<String> lines(FontMetrics metrics, String words, int width) {
      List<String> lines = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (String word : words.split(" ")) {
         if (word.isEmpty()) {
            continue;
         }
         String tried = line.length() == 0 ? word : line + " " + word;
         if (line.length() > 0 && metrics.stringWidth(tried) > width) {
            lines.add(line.toString());
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(tried);
         }
      }
      if (line.length() > 0) {
         lines.add(line.toString());
      }
      return lines;
   }
}
