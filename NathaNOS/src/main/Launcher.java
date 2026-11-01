package main;

import java.awt.Image;
import java.beans.PropertyVetoException;
import java.lang.reflect.Constructor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/** The apps in assets/ and how to start them; the desktop icons, start menu and taskbar all launch through it. */
final class Launcher {
   private final Main screen;
   private final TaskList tasks;
   private final FileGatherer fileGatherer = new FileGatherer();
   private final Class<?>[] applications;
   private final Image[] icons;

   Launcher(Main screen, TaskList tasks) {
      this.screen = screen;
      this.tasks = tasks;
      this.applications = this.fileGatherer.getAllJFrameClasses();
      this.icons = new Image[this.count()];
      for (int i = 0; i < this.icons.length; i++) {
         String path = this.fileGatherer.iconPaths.get(i);
         this.icons[i] = path == null ? null : new ImageIcon(path).getImage();
      }
   }

   int count() {
      return this.fileGatherer.applicationNames.size();
   }

   String name(int app) {
      return this.fileGatherer.applicationNames.get(app);
   }

   /** The app's icon file, or null when it has none (AppIcon then draws a tile). */
   Image icon(int app) {
      return this.icons[app];
   }

   void launch(int app) {
      Class<?> main = this.applications[app];
      String name = this.name(app);
      if (main == null) {
         System.err.println(name + " could not be loaded.");
         return;
      }

      this.tasks.launching(name, this.icon(app));
      try {
         Constructor<?> embeddable;
         try {
            embeddable = main.getConstructor(Boolean.class);
         } catch (NoSuchMethodException notEmbeddable) {
            // Any other app runs as it would on its own, in its own windows.
            AppLauncher.runMain(main, name);
            return;
         }
         this.createNewWindow((JFrame)embeddable.newInstance(false), name, this.icon(app));
      } catch (ReflectiveOperationException | RuntimeException exception) {
         System.err.println(name + " failed to start: " + exception);
      }
   }

   /** Shows an embeddable app's content in an internal window on the desktop. */
   private void createNewWindow(JFrame window, String name, Image iconImage) {
      GenericAppFrame appWindow = new GenericAppFrame(window.getTitle(), window.getContentPane(), false, iconImage, this.screen);
      appWindow.setSize(window.getWidth(), window.getHeight());
      appWindow.setPosition(this.screen.getWidth() / 2 - window.getWidth() / 2, this.screen.getHeight() / 2 - window.getHeight() / 2);
      this.screen.desktopPane.add(appWindow);
      // The app's own frame was only a hidden donor for its content pane.
      window.dispose();
      this.tasks.add(appWindow, name, iconImage);

      try {
         appWindow.setSelected(true);
      } catch (PropertyVetoException exception) {
         exception.printStackTrace();
      }
   }
}
