package main;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JInternalFrame;

/** The app windows open on the desktop, for the taskbar: internal app windows and the decorated frames apps open. */
final class TaskList {
   static final class Task {
      final Component window;
      final String name;
      final Image icon;

      Task(Component window, String name, Image icon) {
         this.window = window;
         this.name = name;
         this.icon = icon;
      }

      boolean isActive() {
         return this.window instanceof Window ? ((Window)this.window).isActive() : ((JInternalFrame)this.window).isSelected();
      }

      void activate() {
         if (this.window instanceof Window) {
            ((Window)this.window).toFront();
            this.window.requestFocus();
         } else {
            JInternalFrame frame = (JInternalFrame)this.window;
            frame.moveToFront();
            try {
               frame.setSelected(true);
            } catch (PropertyVetoException ignored) {
               // The frame stays as it was.
            }
         }
      }
   }

   private final List<Task> tasks = new ArrayList<>();
   private String launchingName = "App";
   private Image launchingIcon;

   /** Starts listening for app frames; the desktop's own frame is left out. */
   TaskList(Main desktop) {
      Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
         if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof Frame && event.getSource() != desktop) {
            Frame frame = (Frame)event.getSource();
            // Borderless windows (ScreenSaver, CircleSpawner's slices) are not app windows a taskbar would list.
            if (!frame.isUndecorated()) {
               place(frame, desktop);
               this.tasks.add(new Task(frame, this.launchingName, this.launchingIcon));
            }
         }
      }, AWTEvent.WINDOW_EVENT_MASK);
   }

   /** Most apps open their window in the top-left corner, over the desktop icons; a window manager centers them. */
   private static void place(Frame frame, Main desktop) {
      if (frame.getX() == 0 && frame.getY() == 0) {
         int room = desktop.getHeight() - desktop.launcherWindow.getHeight();
         frame.setLocation(Math.max(0, (desktop.getWidth() - frame.getWidth()) / 2), Math.max(0, (room - frame.getHeight()) / 2));
      }
   }

   /** Frames opened from now on belong to this app (apps open their frames on threads of their own, later). */
   void launching(String name, Image icon) {
      this.launchingName = name;
      this.launchingIcon = icon;
   }

   void add(JInternalFrame frame, String name, Image icon) {
      this.tasks.add(new Task(frame, name, icon));
   }

   /** The windows showing now, in the order they opened. Closed ones are forgotten. */
   List<Task> showing() {
      this.tasks.removeIf(task -> !task.window.isDisplayable());
      List<Task> showing = new ArrayList<>();
      for (Task task : this.tasks) {
         if (task.window.isVisible()) {
            showing.add(task);
         }
      }
      return showing;
   }
}
