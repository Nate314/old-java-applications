package launcher;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Puts apps in the Windows Start menu, in an "Old Java Applications" folder: one shortcut per app that runs
 * {@code run.ps1 old <App>} in a minimized console, with the app's picture as its icon, so Windows search finds
 * "Minesweeper" and Enter plays it without the launcher. The icons live in {@code out/start-icons}.
 */
final class StartMenu {
   static final String FOLDER = "Old Java Applications";
   /** Makes the shortcuts with WScript.Shell; the paths come in through the environment, so nothing is quoted. */
   private static final String SCRIPT = String.join("\n",
      "$ErrorActionPreference = 'Stop'",
      "$shell = New-Object -ComObject WScript.Shell",
      "$target = (Get-Command $env:OLDJAVA_SHELL).Source",
      "foreach ($row in $env:OLDJAVA_LINKS -split \"`n\") {",
      "  if (-not $row) { continue }",
      "  $f = $row -split \"`t\"",
      "  $s = $shell.CreateShortcut($f[0])",
      "  $s.TargetPath = $target",
      "  $s.Arguments = $f[1]",
      "  $s.WorkingDirectory = $env:OLDJAVA_ROOT",
      "  $s.IconLocation = $f[2] + ',0'",
      "  $s.Description = $f[3]",
      "  $s.WindowStyle = 7",   // minimized: run.ps1's console stays out of the way
      "  $s.Save()",
      "}");

   private final String shell;
   private final Path root;
   final Path folder;
   private final Path icons;
   private final Thumbs thumbs = new Thumbs();

   StartMenu(String shell, Path root, Path folder, Path icons) {
      this.shell = shell;
      this.root = root.toAbsolutePath().normalize();   // the shortcuts run from anywhere
      this.folder = folder;
      this.icons = icons;
   }

   /** This user's Start menu, or null off Windows (where there is none to add to). */
   static StartMenu forUser(String shell, Path root) {
      String appData = System.getenv("APPDATA");
      if (appData == null || !System.getProperty("os.name", "").startsWith("Windows")) return null;
      return new StartMenu(shell, root, Paths.get(appData, "Microsoft", "Windows", "Start Menu", "Programs", FOLDER),
         root.resolve("out/start-icons"));
   }

   /** The apps that press real keys stay out: a shortcut would start them without the launcher's warning. */
   static boolean allowed(Catalog.App a) {
      return !a.pressesKeys();
   }

   Path link(Catalog.App a) {
      return folder.resolve(a.name.replaceAll("[\\\\/:*?\"<>|]", "") + ".lnk");
   }

   boolean has(Catalog.App a) {
      return Files.isRegularFile(link(a));
   }

   /** run.ps1's arguments for the app, as the launcher runs it. */
   static String arguments(Path root, String id) {
      return "-NoProfile -ExecutionPolicy Bypass -File \"" + root.resolve("run.ps1") + "\" old " + id;
   }

   /** Writes each app's icon, then makes all the shortcuts in one PowerShell. Slow: call off the UI thread. */
   void add(List<Catalog.App> apps) throws IOException, InterruptedException {
      Files.createDirectories(folder);
      Files.createDirectories(icons);
      StringBuilder rows = new StringBuilder();
      for (Catalog.App a : apps) {
         if (!allowed(a)) continue;
         Path icon = icons.resolve(a.id + ".ico");
         Files.write(icon, ico(icon(thumbs.original(a), Ui.category(a.category))));
         String about = a.hint.replaceAll("\\s+", " ");
         rows.append(link(a)).append('\t').append(arguments(root, a.id)).append('\t').append(icon).append('\t')
            .append(about.length() > 200 ? about.substring(0, 197) + "..." : about).append('\n');
      }
      ProcessBuilder pb = new ProcessBuilder(shell, "-NoProfile", "-NonInteractive", "-EncodedCommand",
         Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE)));
      pb.environment().put("OLDJAVA_LINKS", rows.toString());
      pb.environment().put("OLDJAVA_SHELL", shell);
      pb.environment().put("OLDJAVA_ROOT", root.toString());
      pb.redirectErrorStream(true);
      Process p = pb.start();
      String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (p.waitFor() != 0) throw new IOException(out.isEmpty() ? "PowerShell could not make the shortcuts" : out);
   }

   /** Deletes the apps' shortcuts and icons, and the folder once it is empty. */
   void remove(List<Catalog.App> apps) throws IOException {
      for (Catalog.App a : apps) {
         Files.deleteIfExists(link(a));
         Files.deleteIfExists(icons.resolve(a.id + ".ico"));
      }
      try {
         Files.deleteIfExists(folder);
      } catch (DirectoryNotEmptyException ignored) {
         // something else is in it: leave it
      }
   }

   /** A 256 px icon: the app's picture filling a rounded square, ringed in its category's color. */
   static BufferedImage icon(BufferedImage picture, Color ring) {
      int n = 256;
      BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = Ui.smooth(img.getGraphics());
      RoundRectangle2D.Float square = new RoundRectangle2D.Float(8, 8, n - 16, n - 16, 56, 56);
      g.setColor(Ui.DESK);
      g.fill(square);
      g.clip(square);
      // cover: the middle of the picture, as large as fills the square
      double s = Math.max((double) (n - 16) / picture.getWidth(), (double) (n - 16) / picture.getHeight());
      int w = (int) Math.round(picture.getWidth() * s);
      int h = (int) Math.round(picture.getHeight() * s);
      g.drawImage(picture, (n - w) / 2, (n - h) / 2, w, h, null);
      g.setClip(null);
      g.setColor(ring);
      g.setStroke(new BasicStroke(12f));
      g.draw(new RoundRectangle2D.Float(14, 14, n - 28, n - 28, 48, 48));
      g.dispose();
      return img;
   }

   /** An .ico file holding one 256 px PNG, which Windows scales for the smaller sizes. */
   static byte[] ico(BufferedImage img) throws IOException {
      ByteArrayOutputStream png = new ByteArrayOutputStream();
      ImageIO.write(img, "png", png);
      ByteBuffer b = ByteBuffer.allocate(22 + png.size()).order(ByteOrder.LITTLE_ENDIAN);
      b.putShort((short) 0).putShort((short) 1).putShort((short) 1);   // reserved, type icon, one image
      b.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);        // 256 x 256 (0 means 256), no palette
      b.putShort((short) 1).putShort((short) 32).putInt(png.size()).putInt(22);
      b.put(png.toByteArray());
      return b.array();
   }
}
