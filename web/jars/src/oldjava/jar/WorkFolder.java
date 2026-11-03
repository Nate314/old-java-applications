package oldjava.jar;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The Main-Class of a downloaded app jar whose app reads files from the folder it is started in (FlappyBird's
 * pictures, StockChecker's quotes, NathaNOS's desktop and the apps on it). It unpacks those files from the jar's
 * {@code workfolder/} into {@code ~/.oldjava/<App>/} and runs the app there in a second Java process, so the jar
 * works when it is double-clicked or started from any folder. The app's own data files ({@code .txt}, {@code .csv})
 * are unpacked once and then left alone, so what the app saved in them stays.
 */
public final class WorkFolder {
   private static final String PREFIX = "workfolder/";

   private WorkFolder() {
   }

   public static void main(String[] args) throws Exception {
      Properties app = new Properties();
      try (InputStream in = WorkFolder.class.getResourceAsStream("/workfolder.properties")) {
         app.load(in);
      }
      Path jar = Paths.get(WorkFolder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      Path dir = Paths.get(System.getProperty("user.home"), ".oldjava", app.getProperty("app")).toAbsolutePath();
      Files.createDirectories(dir);
      try (JarFile files = new JarFile(jar.toFile())) {
         for (Enumeration<JarEntry> e = files.entries(); e.hasMoreElements();) {
            JarEntry entry = e.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith(PREFIX)) {
               continue;
            }
            Path to = dir.resolve(name.substring(PREFIX.length())).normalize();
            boolean data = name.endsWith(".txt") || name.endsWith(".csv");
            if (!to.startsWith(dir) || data && Files.exists(to)) {
               continue;
            }
            Files.createDirectories(to.getParent());
            try (InputStream in = files.getInputStream(entry)) {
               Files.copy(in, to, StandardCopyOption.REPLACE_EXISTING);
            }
         }
      }
      // The same java (or javaw, when the jar was double-clicked) that is running now.
      String java = ProcessHandle.current().info().command()
         .orElse(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
      List<String> command = new ArrayList<>(List.of(java, "-cp", jar.toString(), app.getProperty("main")));
      command.addAll(Arrays.asList(args));
      Process process = new ProcessBuilder(command).directory(dir.toFile()).inheritIO().start();
      System.exit(process.waitFor());
   }
}
