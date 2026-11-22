package windowse;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.JButton;

/**
 * One step of the list, as a pill: pointing at it (or Space on it) shows on the little monitor the screen after it,
 * its x (or Delete on it) takes it off the list.
 */
final class StepPill extends JButton {
   static final Font FONT = new Font("SansSerif", Font.BOLD, 12);
   /** The x at the pill's right end. */
   static final int CROSS_W = 18;
   private static final Color EDGE = new Color(170, 176, 190);
   private static final Color INK = new Color(28, 32, 40);
   private static final Color CROSS = new Color(120, 126, 140);
   private static final Color PEEK = new Color(222, 236, 252);

   final int index;
   private final String text;
   private boolean lit;
   private boolean peeked;
   /** The press began on the x: the click takes the step off instead of showing it. */
   private boolean onCross;

   /** onPeek: the screen after this step is wanted (-1: no longer); onRemove: take it off. */
   StepPill(int index, int of, Step step, boolean pocket, IntConsumer onPeek, IntConsumer onRemove) {
      this.index = index;
      this.text = text(step, pocket);
      this.setText("");
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setToolTipText("Step " + (index + 1) + " of " + of + ": point at it to see the screen after it; its x takes it off");
      this.getAccessibleContext().setAccessibleName("Step " + (index + 1) + " of " + of + ", " + step.label()
         + ". Space shows the screen after it, Delete takes it off the list");
      this.addActionListener(event -> {
         if (this.onCross) {
            onRemove.accept(index);
         } else {
            onPeek.accept(index);
         }
         this.onCross = false;
      });
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            StepPill.this.onCross = event.getX() >= StepPill.this.getWidth() - CROSS_W - 2;
         }

         @Override
         public void mouseEntered(MouseEvent event) {
            onPeek.accept(index);
         }

         @Override
         public void mouseExited(MouseEvent event) {
            onPeek.accept(-1);
         }
      });
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_DELETE || event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
               onRemove.accept(index);
            }
         }
      });
   }

   /** What the pill says: "Win+E x3", or on the pocket pad, where every step fits on one row, "E x3" ("Win" alone stays). */
   static String text(Step step, boolean pocket) {
      return pocket ? step.label().replace("Win+", "") : step.label();
   }

   /** How wide a pill for this step is: its label, padding and the x. */
   static int width(FontMetrics fm, Step step, boolean pocket) {
      return fm.stringWidth(text(step, pocket)) + 18 + CROSS_W;
   }

   /** Lit: being pressed; peeked: its screen is on the little monitor. */
   void show(boolean lit, boolean peeked) {
      if (this.lit != lit || this.peeked != peeked) {
         this.lit = lit;
         this.peeked = peeked;
         this.repaint();
      }
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = this.getWidth();
      int h = this.getHeight();
      g.setColor(this.lit ? KeyCap.ACCENT : this.peeked ? PEEK : Color.WHITE);
      g.fillRoundRect(0, 0, w, h, h / 2, h / 2);
      g.setColor(this.lit || this.peeked ? KeyCap.ACCENT : EDGE);
      g.drawRoundRect(0, 0, w - 1, h - 1, h / 2, h / 2);
      if (this.isFocusOwner()) {
         g.drawRoundRect(2, 2, w - 5, h - 5, h / 2 - 2, h / 2 - 2);
      }
      g.setFont(FONT);
      g.setColor(this.lit ? Color.WHITE : INK);
      g.drawString(this.text, 9, h / 2 + 4);
      if (this.isEnabled()) {   // no x while the list is being pressed
         int cx = w - CROSS_W / 2 - 4;
         int cy = h / 2;
         g.setColor(this.peeked ? KeyCap.ACCENT : CROSS);
         g.drawLine(cx - 3, cy - 3, cx + 3, cy + 3);
         g.drawLine(cx - 3, cy + 3, cx + 3, cy - 3);
      }
   }
}
