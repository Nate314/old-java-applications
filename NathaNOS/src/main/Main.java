package main;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.beans.PropertyVetoException;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main extends JFrame {
   public JDesktopPane desktopPane = new JDesktopPane() {
      ImageIcon icon = new ImageIcon("assets/background.jpg");
      Image image = this.icon.getImage();
      Image newimage = this.image.getScaledInstance(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height, 4);

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         g.drawImage(this.newimage, 0, 0, this);
      }
   };
   public LauncherWindow launcherWindow;
   public StartMenu startMenu;

   public Main(Boolean visibility) {
      this.setUndecorated(true);
      this.setDefaultCloseOperation(3);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height);
      this.setVisible(visibility);
      this.setCursor(this.getToolkit().createCustomCursor(new ImageIcon("assets/cursor.png").getImage(), new Point(), "defaultCursor"));
      this.add(this.desktopPane);
      this.launcherWindow = new LauncherWindow(this);
      this.startMenu = new StartMenu(this);
      this.desktopPane.add(this.launcherWindow);
      this.desktopPane.add(this.startMenu);

      try {
         this.desktopPane.getAllFrames()[0].setSelected(true);
      } catch (PropertyVetoException exception) {
         exception.printStackTrace();
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
