package rubikscube;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.IntConsumer;

/**
 * The My cube card, in place of the message and help: how to hold the cube, the six colors to paint with (the one
 * picked ringed, each with how many of its squares are left to paint), what is wrong if anything, and Cancel and
 * Teach me buttons. Painted by hand like the other cards.
 */
final class PaintPanel extends ClickCard {
   /** What a click asks for: 0 to 5 pick that color ({@link CubeGeometry#FACE_COLORS}), or one of these. */
   static final int CANCEL = -1;
   static final int DONE = -2;
   private static final Color WRONG = new Color(190, 40, 40);
   private String[] colors = new String[54];
   private int brush = 0;
   private String status = "";
   private boolean wrong = false;
   private boolean net = false;

   PaintPanel(IntConsumer onClick) {
      super(onClick);
   }

   /** The card for the cube's stickers as painted so far, the color picked and a status line (red when wrong). */
   void show(String[] colors, int brush, String status, boolean wrong, boolean net) {
      this.colors = colors;
      this.brush = brush;
      this.status = status;
      this.wrong = wrong;
      this.net = net;
      this.repaint();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int width = this.getWidth();
      int height = this.getHeight();
      CardPaint.card(g, width, height);
      this.clearTargets();
      int x = 9;
      int inner = width - 2 * x;
      int[] left = new int[6];
      int painted = 0;
      for (int f = 0; f < 6; f++) {
         left[f] = 9;
      }
      for (int s = 0; s < 54; s++) {
         int face = this.colors[s] == null ? -1 : CubeGeometry.faceIndex(this.colors[s]);
         if (face >= 0) {
            left[face]--;
            painted += s % 9 == 4 ? 0 : 1;
         }
      }
      g.setFont(new Font("Dialog", Font.BOLD, 10));
      g.setColor(CardPaint.MUTED);
      g.drawString("MY CUBE", x, 16);
      String tally = painted + " of 48 painted";
      g.drawString(tally, width - x - g.getFontMetrics().stringWidth(tally), 16);
      g.setColor(CardPaint.INK);
      g.setFont(new Font("Dialog", Font.BOLD, 13));
      int y = CardPaint.wrap(g, "Paint your real cube", x, 37, inner, 16, 2);
      g.setFont(new Font("Dialog", Font.PLAIN, 12));
      y = CardPaint.wrap(g, "Hold it with the blue center on top and red facing you. Pick a color (or 1-6), then click"
            + " each square to match. " + (this.net ? "The net unfolds it: the back is at the bottom."
            : "Drag around the cube to reach its other sides."), x, y + 17, inner, 15, 7);
      // The six colors: a swatch each, the picked one ringed, how many are left to paint under it.
      int size = 22;
      int gap = (inner - 6 * size) / 5;
      int top = y + 12;
      for (int f = 0; f < 6; f++) {
         int sx = x + f * (size + gap);
         Color color = CubeView3D.colorOf(CubeGeometry.FACE_COLORS[f]);
         if (f == this.brush) {
            g.setColor(CardPaint.INK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(sx - 3, top - 3, size + 5, size + 5, 9, 9);
            g.setStroke(new BasicStroke(1f));
         } else if (this.hover == f) {
            g.setColor(CardPaint.EDGE);
            g.drawRoundRect(sx - 3, top - 3, size + 5, size + 5, 9, 9);
         }
         g.setColor(color);
         g.fillRoundRect(sx, top, size, size, 6, 6);
         g.setColor(CubeView3D.mix(color, CardPaint.INK, 0.35));
         g.drawRoundRect(sx, top, size - 1, size - 1, 6, 6);
         g.setFont(new Font("Dialog", Font.BOLD, 10));
         String count = left[f] == 0 ? "✓" : String.valueOf(left[f]);
         g.setColor(left[f] < 0 ? WRONG : left[f] == 0 ? CardPaint.DONE : CardPaint.MUTED);
         g.drawString(count, sx + (size - g.getFontMetrics().stringWidth(count)) / 2, top + size + 13);
         this.target(sx - 3, top - 3, size + 6, size + 20, f);
      }
      int buttonY = height - 34;
      y = top + size + 18;
      g.setFont(new Font("Dialog", this.wrong ? Font.BOLD : Font.PLAIN, 11));
      g.setColor(this.wrong ? WRONG : CardPaint.MUTED);
      CardPaint.wrap(g, this.status, x, y + 13, inner, 13, Math.max(1, (buttonY - y - 4) / 13));
      int half = (inner - 6) / 2;
      this.button(g, x, buttonY, half, "Cancel", CANCEL, false);
      this.button(g, x + half + 6, buttonY, half, "Teach me", DONE, true);
   }
}
