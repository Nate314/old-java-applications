package launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Entry point. Run from the repo root (run.ps1 does). The optional argument is the PowerShell to run run.ps1 with;
 * run.ps1 passes the one it runs in, and the default is Windows PowerShell.
 */
public final class Main {
   private Main() {
   }

   public static void main(String[] args) {
      Path root = Paths.get("").toAbsolutePath();
      String shell = args.length > 0 ? args[0] : "powershell";
      SwingUtilities.invokeLater(() -> {
         List<Catalog.App> apps;
         try {
            apps = Catalog.load(root);
         } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Run the launcher from the repo root: could not read " + e.getMessage(),
               "Old Java Applications", JOptionPane.ERROR_MESSAGE);
            return;
         }
         PlayLog log = new PlayLog(Paths.get(System.getProperty("user.home"), ".oldjava-launcher.txt"));
         LauncherWindow window = new LauncherWindow(apps, log, Runner.powerShell(shell, root), root, System::currentTimeMillis);
         try {
            window.setIconImage(ImageIO.read(root.resolve("web/site/icons/icon-192.png").toFile()));   // the site's icon
         } catch (IOException ignored) {
            // keep Java's default icon
         }
         window.setVisible(true);
      });
   }
}
