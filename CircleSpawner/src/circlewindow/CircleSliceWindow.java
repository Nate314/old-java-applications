package circlewindow;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** One undecorated window of the shape: a solid rectangle of color. Pressing it runs onPress. */
public final class CircleSliceWindow extends JFrame {
   /** Where it flies to when the shape is scattered, in pixels per frame. */
   double velocityX;
   double velocityY;
   double x;
   double y;
   private final JPanel panel = new JPanel();
   private Color color;

   public CircleSliceWindow(Rectangle bounds, Color color, String title, boolean onTop, Runnable onPress) {
      this.panel.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            onPress.run();
         }
      });
      this.setTitle(title);
      this.setUndecorated(true);
      this.add(this.panel);
      this.place(bounds, color);
      this.setAlwaysOnTop(onTop);
      // Popping up must not take the keyboard from the creator window while the shape is still appearing.
      this.setFocusableWindowState(false);
      this.setVisible(true);
   }

   Color color() {
      return this.color;
   }

   /** Moves and sizes the window to bounds and fills it with color. */
   void place(Rectangle bounds, Color color) {
      this.setBounds(bounds);
      this.x = bounds.x;
      this.y = bounds.y;
      if (!color.equals(this.color)) {
         this.color = color;
         this.panel.setBackground(color);
      }
   }
}
