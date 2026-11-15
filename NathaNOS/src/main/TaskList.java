package main;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
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
      /** Hidden by its taskbar button (or Show desktop), not by the app; the button stays until it is restored. */
      boolean minimized;

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
   private final Main desktop;
   private String launchingName = "App";
   private Image launchingIcon;
   /** The icon or start menu row the next app window flies out of, once. */
   private Rectangle launchingFrom;

   /** Starts listening for app frames; the desktop's own frame is left out. */
   TaskList(Main desktop) {
      this.desktop = desktop;
      Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
         if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof Frame && event.getSource() != desktop) {
            Frame frame = (Frame)event.getSource();
            // Borderless windows (ScreenSaver, CircleSpawner's slices) are not app windows a taskbar would list.
            if (!frame.isUndecorated()) {
               place(frame, desktop);
               this.added(new Task(frame, this.launchingName, this.launchingIcon));
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
   void launching(String name, Image icon, Rectangle from) {
      this.launchingName = name;
      this.launchingIcon = icon;
      this.launchingFrom = from;
   }

   void add(JInternalFrame frame, String name, Image icon) {
      this.added(new Task(frame, name, icon));
   }

   private void added(Task task) {
      this.tasks.add(task);
      if (this.launchingFrom != null) {
         // The window is already up; the outlines that show where it came from fly in under it.
         this.desktop.zoom.fly(this.launchingFrom, task.window.getBounds(), null);
         this.launchingFrom = null;
      }
   }

   /** What a taskbar button does: brings back a minimized window, minimizes the active one, or brings one to the front. */
   void press(Task task, Rectangle button) {
      if (task.minimized) {
         this.restore(task, button);
      } else if (task.isActive()) {
         this.minimize(task, button);
      } else {
         task.activate();
      }
   }

   void minimize(Task task, Rectangle button) {
      task.minimized = true;
      task.window.setVisible(false);
      this.desktop.zoom.fly(task.window.getBounds(), button, null);
   }

   void restore(Task task, Rectangle button) {
      this.desktop.zoom.fly(button, task.window.getBounds(), () -> {
         if (task.minimized && task.window.isDisplayable()) {
            task.minimized = false;
            task.window.setVisible(true);
            task.activate();
         }
      });
   }

   /** True while every window on the taskbar is minimized (Show desktop would bring them back). */
   boolean allMinimized(List<Task> showing) {
      for (Task task : showing) {
         if (!task.minimized) {
            return false;
         }
      }
      return !showing.isEmpty();
   }

   /** How many windows of the app are showing. */
   int countNamed(String name) {
      int count = 0;
      for (Task task : this.showing()) {
         if (task.name.equals(name)) {
            count++;
         }
      }
      return count;
   }

   /** The windows showing now or minimized, in the order they opened. Closed ones are forgotten. */
   List<Task> showing() {
      this.tasks.removeIf(task -> !task.window.isDisplayable());
      List<Task> showing = new ArrayList<>();
      for (Task task : this.tasks) {
         if (task.window.isVisible()) {
            task.minimized = false; // the app showed it again itself
         }
         if (task.window.isVisible() || task.minimized) {
            showing.add(task);
         }
      }
      return showing;
   }
}
