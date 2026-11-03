package rubikscube;

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
      CardPaint.card(g, width, height);
      int x = 9;
      int inner = width - 2 * x;
      g.setFont(new Font("Dialog", Font.BOLD, 10));
      g.setColor(CardPaint.MUTED);
      g.drawString("TEACH ME", x, 16);
      int y = 22;
      if (this.stage >= 0) {
         int gap = 3;
         int segment = (inner - 6 * gap) / 7;
         for (int i = 0; i < 7; i++) {
            g.setColor(i < this.stage ? CardPaint.DONE : i == this.stage ? CardPaint.NOW : CardPaint.LATER);
            g.fillRoundRect(x + i * (segment + gap), y, segment, 5, 4, 4);
         }
         y += 8;
      }
      g.setColor(CardPaint.INK);
      g.setFont(new Font("Dialog", Font.BOLD, 13));
      y = CardPaint.wrap(g, this.title, x, y + 15, inner, 16, 3);
      g.setFont(new Font("Dialog", Font.PLAIN, 12));
      g.setColor(CardPaint.INK);
      FontMetrics small = g.getFontMetrics(new Font("Dialog", Font.PLAIN, 11));
      int footerLines = CardPaint.lines(small, this.footer, inner).size();
      int chipRows = CardPaint.chipRows(this.chipColors.size());
      int room = height - y - footerLines * 13 - chipRows * CardPaint.PITCH - 16;
      y = CardPaint.wrap(g, this.text, x, y + 16, inner, 15, Math.max(1, room / 15));
      if (!this.chipColors.isEmpty()) {
         CardPaint.chips(g, x, y + 6, this.chipColors, this.chipClockwise, this.chipNow);
      }
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      g.setColor(CardPaint.MUTED);
      CardPaint.wrap(g, this.footer, x, height - 8 - (footerLines - 1) * 13, inner, 13, footerLines);
   }
}
