package circlewindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import javax.swing.JPanel;

/**
 * The Size track: press or drag along it and the shape on screen grows or shrinks as you go. The part left of the
 * knob is drawn in the chosen colors. The Size field beside it still takes a typed number (and the keyboard).
 */
final class SizeSlider extends JPanel {
   static final int MIN = 10;
   private static final int KNOB = 14;
   private static final int TRACK = 6;
   private int max = 400;
   private int value = 200;
   private final DoubleFunction<Color> shade;

   /** shade: the chosen colors along the track (0 left, 1 right); moved: called with each new size dragged to. */
   SizeSlider(DoubleFunction<Color> shade, IntConsumer moved) {
      this.shade = shade;
      this.setPreferredSize(new Dimension(150, KNOB + 4));
      this.setToolTipText("Drag to grow or shrink the shape");
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            this.follow(e);
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            this.follow(e);
         }

         private void follow(MouseEvent e) {
            int to = valueAt(e.getX(), SizeSlider.this.getWidth(), SizeSlider.this.max);
            if (to != SizeSlider.this.value) {
               SizeSlider.this.value = to;
               SizeSlider.this.repaint();
               moved.accept(to);
            }
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
   }

   /** Shows a size (from the field, kept on the track) and the most the screen has room for. */
   void show(int size, int most) {
      this.max = Math.max(MIN + 1, most);
      this.value = Math.max(MIN, Math.min(this.max, size));
      this.repaint();
   }

   /** The size at x on a track width wide: MIN at the knob's leftmost center, max at its rightmost. */
   static int valueAt(int x, int width, int max) {
      int span = Math.max(1, width - KNOB);
      double t = Math.max(0, Math.min(1, (x - KNOB / 2.0) / span));
      return (int)Math.round(MIN + t * (max - MIN));
   }

   /** Where the knob's center is for a size. */
   static int xOf(int value, int width, int max) {
      return KNOB / 2 + (int)Math.round((width - KNOB) * (value - MIN) / (double)Math.max(1, max - MIN));
   }

   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      int w = this.getWidth();
      int mid = this.getHeight() / 2;
      int knob = xOf(this.value, w, this.max);
      g.setColor(new Color(150, 150, 150));
      g.fillRoundRect(KNOB / 2, mid - TRACK / 2, w - KNOB, TRACK, TRACK, TRACK);
      for (int x = KNOB / 2; x < knob; x++) {
         g.setColor(this.shade.apply((x - KNOB / 2) / (double)Math.max(1, w - KNOB)));
         g.fillRect(x, mid - TRACK / 2, 1, TRACK);
      }
      Color c = this.shade.apply((knob - KNOB / 2) / (double)Math.max(1, w - KNOB));
      g.setColor(Color.WHITE);
      g.fillOval(knob - KNOB / 2, mid - KNOB / 2, KNOB, KNOB);
      g.setColor(c);
      g.fillOval(knob - KNOB / 2 + 3, mid - KNOB / 2 + 3, KNOB - 6, KNOB - 6);
      g.setColor(new Color(90, 90, 90));
      g.drawOval(knob - KNOB / 2, mid - KNOB / 2, KNOB - 1, KNOB - 1);
   }
}
