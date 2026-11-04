package main;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;

/** Finds the apps NathaNOS can launch: one per .jar in assets/, with its icon and Main-Class. */
public class FileGatherer {
   private List<String> jarFileNames = new ArrayList<>();
   public List<String> applicationNames = new ArrayList<>();
   /** assets/<name>.png for each app, or null when the app has no icon file. */
   public List<String> iconPaths = new ArrayList<>();

   public FileGatherer() {
      File[] listOfFiles = new File("assets").listFiles();
      if (listOfFiles == null) {
         System.err.println("No assets folder; run NathaNOS from its own folder.");
         return;
      }
      // Alphabetical, so the desktop and start menu list the apps in the same order on every file system.
      Arrays.sort(listOfFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

      for (File file : listOfFiles) {
         if (file.isFile() && file.getName().endsWith(".jar")) {
            String name = file.getName().substring(0, file.getName().length() - ".jar".length());
            this.jarFileNames.add(file.getName());
            this.applicationNames.add(name);
            this.iconPaths.add(findIcon(listOfFiles, name));
         }
      }
   }

   /** The path of assets/<name>.png matched ignoring case, so Clock.jar finds clock.png on any file system. */
   private static String findIcon(File[] files, String name) {
      for (File file : files) {
         if (file.isFile() && file.getName().equalsIgnoreCase(name + ".png")) {
            return "assets/" + file.getName();
         }
      }
      return null;
   }

   /** Each jar's Main-Class, or null for a jar that fails to load (the other apps still work). */
   public Class<?>[] getAllJFrameClasses() {
      Class<?>[] frameArray = new Class<?>[this.jarFileNames.size()];

      for (int i = 0; i < this.jarFileNames.size(); i++) {
         File jar = new File("assets", this.jarFileNames.get(i));
         try (JarFile jarFile = new JarFile(jar)) {
            URL[] urls = new URL[]{jar.getAbsoluteFile().toURI().toURL()};
            frameArray[i] = URLClassLoader.newInstance(urls).loadClass(jarFile.getManifest().getMainAttributes().getValue("Main-Class"));
         } catch (Exception exception) {
            System.err.println("Could not load " + jar + ": " + exception);
         }
      }

      return frameArray;
   }
}
