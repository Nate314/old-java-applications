package main;

import java.awt.Component;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;

public class GenericAppFrame extends JInternalFrame {
   public GenericAppFrame(String title, Component panel, boolean resizable, Image iconImage, Main screen) {
      super(title, resizable, true, false);
      this.setIconifiable(false);
      this.setDefaultCloseOperation(2);
      this.setVisible(true);
      this.add(panel);
      this.setFrameIcon(new ImageIcon(iconImage.getScaledInstance(20, 20, 4)));
      this.setCursor(screen.getCursor());
   }

   public void setPosition(int x, int y) {
      this.setLocation(x, y);
   }

   @Override
   public void setSize(int width, int height) {
      this.setBounds(this.getX(), this.getY(), width, height);
   }
}
