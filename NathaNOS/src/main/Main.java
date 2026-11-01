package main;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
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
      // A window that cannot take focus is not raised when clicked, so the taskbar never covers the apps it opened.
      this.setFocusableWindowState(false);
      AppLauncher.keepDesktopOpenWhenAppsClose(this);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height);
      this.setVisible(visibility);
      this.setCursor(this.getToolkit().createCustomCursor(new ImageIcon("assets/cursor.png").getImage(), new Point(), "defaultCursor"));
      this.add(this.desktopPane);
      TaskList tasks = new TaskList(this);
      Launcher launcher = new Launcher(this, tasks);
      this.launcherWindow = new LauncherWindow(this, tasks);
      this.startMenu = new StartMenu(this, launcher);
      // The taskbar and its menu stay above internal app windows (Clock), as on a real desktop.
      this.desktopPane.add(this.launcherWindow, JLayeredPane.PALETTE_LAYER);
      this.desktopPane.add(this.startMenu, JLayeredPane.POPUP_LAYER);
      new DesktopShortcuts(this, launcher);
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
