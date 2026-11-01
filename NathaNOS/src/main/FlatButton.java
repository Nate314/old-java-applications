package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** A flat taskbar or start menu button: an app icon and a label, lit while the mouse is over it or while it is "on". */
class FlatButton extends JComponent {
   static final Color TEXT = new Color(235, 241, 250);
   private final Image icon;
   private final String iconName;
   private final String label;
   private final Color background;
   private final Color hoverBackground;
   private final Color onBackground;
   private boolean hovered;
   private boolean on;

   /** iconName names the generated tile when icon is null; a null iconName means no icon at all. */
   FlatButton(Image icon, String iconName, String label, Color background, Color hoverBackground, Color onBackground, Runnable action) {
      this.icon = icon;
      this.iconName = iconName;
      this.label = label;
      this.background = background;
      this.hoverBackground = hoverBackground;
      this.onBackground = onBackground;
      this.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            FlatButton.this.hovered = true;
            FlatButton.this.repaint();
         }

         @Override
         public void mouseExited(MouseEvent e) {
            FlatButton.this.hovered = false;
            FlatButton.this.repaint();
         }

         @Override
         public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               action.run();
            }
         }
      });
   }

   void setOn(boolean on) {
      if (this.on != on) {
         this.on = on;
         this.repaint();
      }
   }

   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      AppIcon.smooth(g);
      int w = this.getWidth();
      int h = this.getHeight();
      Color fill = this.on ? this.onBackground : this.hovered ? this.hoverBackground : this.background;
      if (fill != null) {
         g.setColor(fill);
         g.fillRoundRect(2, 3, w - 4, h - 6, 8, 8);
      }
      if (this.on) {
         g.setColor(new Color(0, 150, 255));
         g.fillRoundRect(w / 2 - 12, h - 5, 24, 3, 3, 3);
      }
      int x = 10;
      if (this.iconName != null) {
         int size = Math.min(h - 14, 28);
         AppIcon.paint(g, this.icon, this.iconName, x, (h - size) / 2, size);
         x += size + 8;
      }
      g.setFont(this.getFont());
      g.setColor(TEXT);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(AppIcon.fit(this.label, metrics, w - x - 8), x, (h + metrics.getAscent() - metrics.getDescent()) / 2);
   }
}
