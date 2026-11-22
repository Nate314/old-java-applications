package minesweeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * The line under the board, drawn in the board's own sand and grass: while playing, three chips (a flag and the mines
 * left, a clock whose hand sweeps with the time, the best time); when a game ends or the computer plays, one pill with
 * what happened, red after a mine and grass green after a win.
 */
final class Scoreboard extends JPanel {
   static final int PLAIN = 0;
   static final int WON = 1;
   static final int LOST = 2;
   private static final int HEIGHT = 30;
   private static final Color INK = new Color(60, 48, 30);
   private int minesLeft;
   private long elapsed;
   private long best;
   /** What happened, drawn instead of the chips; null while playing. */
   private String message;
   private int mood;

   Scoreboard() {
      this.setOpaque(false);
      this.setPreferredSize(new Dimension(240, HEIGHT));
   }

   void counters(int minesLeft, long elapsed, long best) {
      this.minesLeft = minesLeft;
      this.elapsed = elapsed;
      this.best = best;
      this.message = null;
      this.name(minesLeft + " mines left, time " + clock(elapsed) + ", best " + (best == 0 ? "none yet" : BestTimes.format(best)));
   }

   void say(String text, int mood) {
      this.message = text;
      this.mood = mood;
      this.name(text);
   }

   private void name(String text) {
      this.getAccessibleContext().setAccessibleName(text);
      this.repaint();
   }

   static String clock(long millis) {
      long seconds = millis / 1000;
      return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = this.getWidth();
      int h = this.getHeight();
      if (this.message != null) {
         Color fill = this.mood == LOST ? MineTile.HIT : this.mood == WON ? MineTile.GRASS_LIGHT : MineTile.SAND_LIGHT;
         Color edge = this.mood == LOST ? new Color(150, 30, 28) : this.mood == WON ? MineTile.GRASS_EDGE : MineTile.SAND_LINE;
         this.pill(g, 2, w - 4, fill, edge);
         this.text(g, this.message, 2, w - 4, this.mood == LOST ? Color.WHITE : this.mood == WON ? Color.BLACK : INK, 15);
         return;
      }
      // Three chips side by side, each a third of the width.
      int third = w / 3;
      int pad = 3;
      for (int i = 0; i < 3; i++) {
         this.pill(g, i * third + pad, third - 2 * pad, MineTile.SAND_LIGHT, MineTile.SAND_LINE);
      }
      int icon = h - 10;
      this.chip(g, pad, third - 2 * pad, icon, this.minesLeft + " left", (x, y) -> MineTile.drawFlag(g, x + icon / 2, y + icon / 2 + 2, icon * 3 / 2, 1));
      this.chip(g, third + pad, third - 2 * pad, icon, clock(this.elapsed), (x, y) -> this.paintClock(g, x, y, icon));
      this.chip(g, 2 * third + pad, third - 2 * pad, 0, this.best == 0 ? "Best: none yet" : "Best " + BestTimes.format(this.best), null);
   }

   private interface Icon {
      void paint(int x, int y);
   }

   /** An icon (if any, {@code icon} px square) and bold text, centered together in the span x..x+w. */
   private void chip(Graphics2D g, int x, int w, int icon, String text, Icon paint) {
      int gap = paint == null ? 0 : icon + 6;
      int size = 16;
      FontMetrics fm;
      do {
         g.setFont(new Font("Dialog", Font.BOLD, size));
         fm = g.getFontMetrics();
      } while (fm.stringWidth(text) + gap > w - 10 && --size > 10);
      int left = x + (w - fm.stringWidth(text) - gap) / 2;
      if (paint != null) {
         paint.paint(left, (this.getHeight() - icon) / 2);
      }
      g.setColor(INK);
      g.drawString(text, left + gap, (this.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
   }

   /** A rounded chip from x, w wide, the component's height less a pixel top and bottom. */
   private void pill(Graphics2D g, int x, int w, Color fill, Color edge) {
      int h = this.getHeight() - 3;
      g.setColor(fill);
      g.fillRoundRect(x, 1, w, h, h, h);
      g.setColor(edge);
      g.setStroke(new BasicStroke(1.5f));
      g.drawRoundRect(x, 1, w, h, h, h);
   }

   /** Bold text centered in the span x..x+w, its size cut (to 10 at the least) until it fits. */
   private void text(Graphics2D g, String text, int x, int w, Color color, int size) {
      FontMetrics fm;
      do {
         g.setFont(new Font("Dialog", Font.BOLD, size));
         fm = g.getFontMetrics();
      } while (fm.stringWidth(text) > w - 8 && --size > 10);
      g.setColor(color);
      g.drawString(text, x + (w - fm.stringWidth(text)) / 2, (this.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
   }

   /** A round clock face whose hand goes once round a minute. */
   private void paintClock(Graphics2D g, int x, int y, int d) {
      g.setColor(Color.WHITE);
      g.fillOval(x, y, d, d);
      g.setColor(MineTile.POLE);
      g.setStroke(new BasicStroke(Math.max(1.5f, d / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.drawOval(x, y, d, d);
      int cx = x + d / 2;
      int cy = y + d / 2;
      double a = (this.elapsed / 1000 % 60) / 60.0 * 2 * Math.PI;
      g.drawLine(cx, cy, cx + (int) Math.round(Math.sin(a) * d * 0.36), cy - (int) Math.round(Math.cos(a) * d * 0.36));
      g.setColor(MineTile.HIT);
      g.fillOval(cx - 2, cy - 2, 4, 4);
   }
}
