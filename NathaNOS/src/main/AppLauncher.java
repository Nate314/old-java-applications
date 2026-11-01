package main;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JFrame;

/** Runs ordinary apps (ones without a public (Boolean) constructor) inside NathaNOS, in their own windows. */
final class AppLauncher {
   private AppLauncher() {
   }

   /** Starts an app the way the java launcher does: its main(String[]) on a thread of its own, with no arguments. */
   static void runMain(Class<?> app, String name) throws NoSuchMethodException {
      Method main = app.getMethod("main", String[].class);
      new Thread(() -> {
         try {
            main.invoke(null, (Object) new String[0]);
         } catch (InvocationTargetException exception) {
            System.err.println(name + " stopped: " + exception.getCause());
         } catch (IllegalAccessException exception) {
            System.err.println(name + " could not be started: " + exception);
         }
      }, name + " main").start();
   }

   /**
    * Apps close with EXIT_ON_CLOSE when run alone, which would also end NathaNOS. Every window opened after this
    * call, other than the desktop itself, closes only itself instead.
    */
   static void keepDesktopOpenWhenAppsClose(JFrame desktop) {
      Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
         if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof JFrame && event.getSource() != desktop) {
            JFrame frame = (JFrame)event.getSource();
            if (frame.getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE) {
               frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            }
         }
      }, AWTEvent.WINDOW_EVENT_MASK);
   }
}
