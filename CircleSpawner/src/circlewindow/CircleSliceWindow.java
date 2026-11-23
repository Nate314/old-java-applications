package circlewindow;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * One undecorated window of the shape: a rectangle of color with its part of the shape's light on it (see Light).
 * The hand hears its presses and drags.
 */
public final class CircleSliceWindow extends JFrame {
   double x;
   double y;
   /** The box the light is stretched over, from this window's corner: it moves with the window when carried. */
   private int lightX;
   private int lightY;
   private int lightWidth;
   private int lightHeight;
   private final JPanel panel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Light.paint(g, CircleSliceWindow.this.lightX, CircleSliceWindow.this.lightY,
            CircleSliceWindow.this.lightWidth, CircleSliceWindow.this.lightHeight);
      }
   };
   private Color color;

   /** light: the whole shape's box on screen, or null for none until the window is placed with one. */
   public CircleSliceWindow(Rectangle bounds, Color color, Rectangle light, String title, boolean onTop, MouseAdapter hand) {
      this.panel.addMouseListener(hand);
      this.panel.addMouseMotionListener(hand);
      this.setTitle(title);
      this.setUndecorated(true);
      this.add(this.panel);
      this.place(bounds, color, light != null ? light : new Rectangle());
      this.setAlwaysOnTop(onTop);
      // Popping up must not take the keyboard from the creator window while the shape is still appearing.
      this.setFocusableWindowState(false);
      this.setVisible(true);
   }

   Color color() {
      return this.color;
   }

   /** The box on screen the light is stretched over. */
   Rectangle light() {
      return new Rectangle(this.getX() + this.lightX, this.getY() + this.lightY, this.lightWidth, this.lightHeight);
   }

   /** Moves and sizes the window to bounds and fills it with color, the light staying where it is on screen. */
   void place(Rectangle bounds, Color color) {
      this.place(bounds, color, this.light());
   }

   /** Moves and sizes the window to bounds and fills it with color, with the light over the box light on screen. */
   void place(Rectangle bounds, Color color, Rectangle light) {
      this.setBounds(bounds);
      this.x = bounds.x;
      this.y = bounds.y;
      if (!color.equals(this.color)) {
         this.color = color;
         this.panel.setBackground(color);
      }
      int lx = light.x - bounds.x;
      int ly = light.y - bounds.y;
      if (lx != this.lightX || ly != this.lightY || light.width != this.lightWidth || light.height != this.lightHeight) {
         this.lightX = lx;
         this.lightY = ly;
         this.lightWidth = light.width;
         this.lightHeight = light.height;
         this.panel.repaint();
      }
   }
}
