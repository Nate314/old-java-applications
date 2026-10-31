package main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.jar.JarFile;
import javax.swing.JFrame;

public class FileGatherer {
   private ArrayList<String> jarFileNames = new ArrayList<>();
   public ArrayList<String> applicationNames = new ArrayList<>();
   public ArrayList<String> iconPaths = new ArrayList<>();

   public FileGatherer() {
      this.iconPaths.add("assets/NathaNOS.png");
      this.applicationNames.add("MENU");
      File folder = new File("assets");
      File[] listOfFiles = folder.listFiles();

      for (int i = 0; i < listOfFiles.length; i++) {
         if (listOfFiles[i].isFile()) {
            System.out.println("File " + listOfFiles[i].getName());
         } else if (listOfFiles[i].isDirectory()) {
            System.out.println("Directory " + listOfFiles[i].getName());
         }

         if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".jar")) {
            this.jarFileNames.add(listOfFiles[i].getName());
            this.applicationNames.add(listOfFiles[i].getName().replace(".jar", ""));
            this.iconPaths.add("assets/" + listOfFiles[i].getName().replace(".jar", "") + ".png");
         }
      }
   }

   public JFrame getAJFrame(int id) {
      try {
         URL[] urls = new URL[]{new URL("file:///" + new File("").getAbsolutePath() + "/assets/Clock1.0.jar")};
         return (JFrame)URLClassLoader.newInstance(urls).loadClass("clock.Main").getConstructor(Boolean.class).newInstance(false);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException | IOException | InstantiationException var3) {
         var3.printStackTrace();
         return null;
      }
   }

   public JFrame[] getAllJFrames() {
      try {
         JFrame[] frameArray = new JFrame[this.jarFileNames.size()];

         for (int i = 0; i < this.jarFileNames.size(); i++) {
            URL[] urls = new URL[]{new URL("file:///" + new File("").getAbsolutePath() + "/assets/" + this.jarFileNames.get(i))};
            frameArray[i] = (JFrame)URLClassLoader.newInstance(urls).loadClass("clock.Main").getConstructor(Boolean.class).newInstance(false);
            System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaa -- " + frameArray[i]);
         }

         return frameArray;
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException | IOException | InstantiationException var4) {
         var4.printStackTrace();
         return null;
      }
   }

   public Class[] getAllJFrameClasses() {
      try {
         Class[] frameArray = new Class[this.jarFileNames.size()];

         for (int i = 0; i < this.jarFileNames.size(); i++) {
            URL[] urls = new URL[]{new URL("file:///" + new File("").getAbsolutePath() + "/assets/" + this.jarFileNames.get(i))};
            frameArray[i] = URLClassLoader.newInstance(urls)
               .loadClass(new JarFile("assets/" + this.jarFileNames.get(i)).getManifest().getMainAttributes().getValue("Main-Class"));
         }

         return frameArray;
      } catch (SecurityException | ClassNotFoundException | IOException | IllegalArgumentException var4) {
         var4.printStackTrace();
         return null;
      }
   }
}
