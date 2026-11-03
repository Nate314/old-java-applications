package main;

import java.awt.Image;
import java.beans.PropertyVetoException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * The apps in assets/ and how to start them, plus NathaNOS's own Paint (in its alphabetical place); the desktop icons,
 * start menu and taskbar all launch through it.
 */
final class Launcher {
   static final String PAINT = "Paint";
   private final Main screen;
   private final TaskList tasks;
   private final FileGatherer fileGatherer = new FileGatherer();
   private final List<String> names = new ArrayList<>();
   /** Each app's Main-Class (null for Paint, or for a jar that failed to load). */
   private final List<Class<?>> applications = new ArrayList<>();
   private final List<Image> icons = new ArrayList<>();
   private final int paint;

   Launcher(Main screen, TaskList tasks) {
      this.screen = screen;
      this.tasks = tasks;
      Class<?>[] mains = this.fileGatherer.getAllJFrameClasses();
      List<String> jarNames = this.fileGatherer.applicationNames;
      for (int i = 0; i < jarNames.size(); i++) {
         String path = this.fileGatherer.iconPaths.get(i);
         this.names.add(jarNames.get(i));
         this.applications.add(mains[i]);
         this.icons.add(path == null ? null : new ImageIcon(path).getImage());
      }
      int at = 0;
      while (at < this.names.size() && this.names.get(at).compareToIgnoreCase(PAINT) < 0) {
         at++;
      }
      this.paint = at;
      this.names.add(at, PAINT);
      this.applications.add(at, null);
      this.icons.add(at, PaintWindow.icon());
   }

   int count() {
      return this.names.size();
   }

   String name(int app) {
      return this.names.get(app);
   }

   /** The app's icon file, or null when it has none (AppIcon then draws a tile). */
   Image icon(int app) {
      return this.icons.get(app);
   }

   /** Opens a picture from the desktop in a Paint window of its own. */
   void openPicture(Picture picture) {
      PaintWindow.open(this.screen, this.tasks, picture, this.icon(this.paint));
   }

   void launch(int app) {
      if (app == this.paint) {
         PaintWindow.open(this.screen, this.tasks, null, this.icon(app));
         return;
      }
      Class<?> main = this.applications.get(app);
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
