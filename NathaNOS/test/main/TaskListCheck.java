package main;

import java.lang.ref.WeakReference;
import java.util.List;
import javax.swing.JInternalFrame;

/** Checks that a closed window leaves the taskbar and Restart's list, so nothing in NathaNOS keeps it in memory. */
public final class TaskListCheck {
   /** A window that is open until closed; headless Java has no real desktop to show one on. */
   private static final class FakeWindow extends JInternalFrame {
      boolean open = true;

      FakeWindow() {
         this.setVisible(true);
         this.setBounds(10, 20, 300, 200);
      }

      @Override
      public boolean isDisplayable() {
         return this.open;
      }
   }

   public static void main(String[] args) throws InterruptedException {
      TaskList tasks = new TaskList(null);
      FakeWindow kept = new FakeWindow();
      tasks.add(kept, Launcher.PAINT, null);
      WeakReference<FakeWindow> closed = openAndClose(tasks);
      List<TaskList.Task> showing = tasks.showing();
      check(showing.size() == 1 && showing.get(0).window == kept, "the taskbar lists only the window still open");
      for (int i = 0; i < 50 && closed.get() != null; i++) {
         System.gc();
         Thread.sleep(20);
      }
      check(closed.get() == null, "the closed window is freed");
      check(tasks.countNamed(Launcher.PAINT) == 1, "Paint counts one window");
      List<java.awt.Rectangle> were = tasks.closeAll();
      check(were.size() == 1 && were.get(0).equals(kept.getBounds()), "Restart still closes the open window, outline where it was");
      System.out.println("TaskListCheck: all checks passed");
   }

   /** Opens a second Paint window and closes it, keeping no reference to it here. */
   private static WeakReference<FakeWindow> openAndClose(TaskList tasks) {
      FakeWindow window = new FakeWindow();
      tasks.add(window, Launcher.PAINT, null);
      check(tasks.countNamed(Launcher.PAINT) == 2, "both Paint windows are on the taskbar");
      window.open = false;
      return new WeakReference<>(window);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
