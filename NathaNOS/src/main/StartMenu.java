package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

public class StartMenu extends JInternalFrame implements ActionListener {
   private Main screen;
   private JPanel panel;
   private JButton shutDown = new JButton("Shut Down");

   public StartMenu(Main screen) {
      super("Start Menu", false, false, false);
      this.setVisible(false);
      this.setBorder(null);
      this.setFrameIcon(new ImageIcon(this.createSolidColoredImage(0, 150, 255)));
      this.setSize(screen.getWidth() / 4, screen.getHeight() / 2);
      this.setLocation(0, this.getHeight() - screen.launcherWindow.getHeight());
      this.setCursor(screen.getCursor());
      this.screen = screen;
      this.panel = new JPanel() {
         ImageIcon icon = new ImageIcon("assets/NathaNOS.png");
         Image image = this.icon.getImage();
         int dimension;
         Image newimage;

         {
            this.dimension = screen.getWidth() / 4 < screen.getHeight() / 2 ? screen.getWidth() / 4 : screen.getHeight() / 2;
            this.newimage = this.image.getScaledInstance(this.dimension, this.dimension, 4);
         }

         @Override
         protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(this.newimage, 0, 0, this);
         }
      };
      this.panel.setBackground(new Color(0, 150, 255));
      this.add(this.panel);
      this.shutDown.addActionListener(this);
      this.panel.add(this.shutDown);
   }

   private Image createSolidColoredImage(int r, int g, int b) {
      int width = 483;
      int height = 483;
      BufferedImage image = new BufferedImage(width, height, 1);
      WritableRaster raster = image.getRaster();
      int[] myColor = new int[]{r, g, b};

      for (int h = 0; h < height; h++) {
         for (int w = 0; w < width; w++) {
            raster.setPixel(w, h, myColor);
         }
      }

      return image;
   }

   public void toggleVisibility() {
      if (this.isVisible()) {
         this.setVisible(false);
         this.setSize(this.screen.getWidth() / 4, this.screen.getHeight() / 2);
         this.setLocation(0, this.getHeight() - this.screen.launcherWindow.getHeight());
      } else {
         this.setVisible(true);
      }
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == this.shutDown) {
         System.exit(0);
      }
   }
}
