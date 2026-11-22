package circlewindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import javax.swing.JPanel;

/**
 * The Size track: press or drag along it and the shape on screen grows or shrinks as you go. The part left of the
 * knob is drawn in the chosen colors. Tab reaches it too: the arrow keys step the size, Page Up and Page Down take
 * big steps, Home and End go to the smallest and largest. The Size field beside it still takes a typed number.
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
      this.setToolTipText("Size: drag, or use the arrow keys, to grow or shrink the shape");
      this.setFocusable(true);
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            SizeSlider.this.requestFocusInWindow();
            this.follow(e);
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            this.follow(e);
         }

         private void follow(MouseEvent e) {
            SizeSlider.this.go(valueAt(e.getX(), SizeSlider.this.getWidth(), SizeSlider.this.max), moved);
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            SizeSlider.this.go(stepped(SizeSlider.this.value, e.getKeyCode(), SizeSlider.this.max), moved);
         }
      });
   }

   private void go(int to, IntConsumer moved) {
      // shortcut: the halo repaints here, as Swing does not repaint a panel on focus (the browser's AWT does);
      // on the desktop a Tab into the track shows it at the first key. Upgrade with a FocusListener once the shim has one.
      this.repaint();
      if (to != this.value) {
         this.value = to;
         moved.accept(to);
      }
   }

   /** The size a key moves value to: arrows a fiftieth of the track (at least 1), pages a tenth, Home/End the ends. */
   static int stepped(int value, int key, int max) {
      int step = Math.max(1, Math.round((max - MIN) / 50f));
      int to;
      switch (key) {
         case KeyEvent.VK_LEFT: case KeyEvent.VK_DOWN: to = value - step; break;
         case KeyEvent.VK_RIGHT: case KeyEvent.VK_UP: to = value + step; break;
         case KeyEvent.VK_PAGE_DOWN: to = value - 5 * step; break;
         case KeyEvent.VK_PAGE_UP: to = value + 5 * step; break;
         case KeyEvent.VK_HOME: to = MIN; break;
         case KeyEvent.VK_END: to = max; break;
         default: return value;
      }
      return Math.max(MIN, Math.min(max, to));
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
      if (this.isFocusOwner()) {   // a halo in the knob's color: the keys move this
         g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 110));
         g.fillOval(knob - KNOB / 2 - 2, mid - KNOB / 2 - 2, KNOB + 4, KNOB + 4);
      }
      g.setColor(Color.WHITE);
      g.fillOval(knob - KNOB / 2, mid - KNOB / 2, KNOB, KNOB);
      g.setColor(c);
      g.fillOval(knob - KNOB / 2 + 3, mid - KNOB / 2 + 3, KNOB - 6, KNOB - 6);
      g.setColor(new Color(90, 90, 90));
      g.drawOval(knob - KNOB / 2, mid - KNOB / 2, KNOB - 1, KNOB - 1);
   }
}
