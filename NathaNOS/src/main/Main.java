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
   /** The original wallpaper photo, scaled to the screen. */
   final Image photo = new ImageIcon("assets/background.jpg").getImage().getScaledInstance(
      Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height, 4);
   public JDesktopPane desktopPane = new JDesktopPane() {
      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Main.this.wallpaper.paint(g, this.getWidth(), this.getHeight(), Main.this.photo, this);
      }
   };
   public LauncherWindow launcherWindow;
   public StartMenu startMenu;
   DesktopMenu desktopMenu;
   DesktopSettings settings = new DesktopSettings();
   Pictures pictures;
   Wallpaper wallpaper = Wallpaper.byId(Wallpaper.DEFAULT);

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
      // Read only now: in the browser, saved files come back once the app's first window exists.
      this.settings = DesktopSettings.load(DesktopSettings.FILE);
      this.pictures = Pictures.load(Pictures.FILE);
      this.wallpaper = this.findWallpaper(this.settings.wallpaper);
      // A Paint picture on the wallpaper changes with each stroke, and the photo comes back if it is deleted.
      this.pictures.onChange(() -> {
         if (this.wallpaper.picture != null && !this.pictures.contains(this.wallpaper.picture)) {
            this.setWallpaper(Wallpaper.byId(Wallpaper.DEFAULT));
         } else if (this.wallpaper.picture != null) {
            this.desktopPane.repaint();
         }
      });
      TaskList tasks = new TaskList(this);
      Launcher launcher = new Launcher(this, tasks);
      this.launcherWindow = new LauncherWindow(this, tasks);
      this.startMenu = new StartMenu(this, launcher);
      // The taskbar and its menu stay above internal app windows (Clock), as on a real desktop.
      this.desktopPane.add(this.launcherWindow, JLayeredPane.PALETTE_LAYER);
      this.desktopPane.add(this.startMenu, JLayeredPane.POPUP_LAYER);
      this.desktopMenu = new DesktopMenu(this, new DesktopShortcuts(this, launcher));
      this.desktopPane.add(this.desktopMenu, JLayeredPane.POPUP_LAYER);
      // The window was shown before the desktop was added, so lay it out now; and since it never takes focus,
      // raise it once, or it opens behind the window that started it.
      this.revalidate();
      this.repaint();
      if (visibility) {
         this.toFront();
      }
   }

   /** The wallpaper the id names (a scene, or a Paint picture still on the desktop), else the original photo. */
   private Wallpaper findWallpaper(String id) {
      Wallpaper wallpaper = Wallpaper.byId(id);
      if (wallpaper == null && id.startsWith(Wallpaper.PICTURE)) {
         Picture picture = this.pictures.find(id.substring(Wallpaper.PICTURE.length()));
         wallpaper = picture == null ? null : Wallpaper.of(picture);
      }
      return wallpaper != null ? wallpaper : Wallpaper.byId(Wallpaper.DEFAULT);
   }

   void setWallpaper(Wallpaper wallpaper) {
      this.wallpaper = wallpaper;
      this.settings.wallpaper = wallpaper.id;
      this.saveSettings();
      this.desktopPane.repaint();
   }

   void saveSettings() {
      this.settings.save(DesktopSettings.FILE);
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
