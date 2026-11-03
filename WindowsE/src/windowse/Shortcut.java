package windowse;

import java.awt.event.KeyEvent;

/** The Windows key shortcuts the keyboard offers: the key pressed with the Windows key, and what Windows does then. */
enum Shortcut {
   START(0, "", "Start", "Start", "opens the Start menu, or closes it", 1),
   EXPLORER(KeyEvent.VK_E, "E", "Explorer", "File Explorer", "opens a File Explorer window", 10),
   DESKTOP(KeyEvent.VK_D, "D", "Desktop", "Show desktop", "hides every window, and again brings them back", 1),
   RUN(KeyEvent.VK_R, "R", "Run", "Run", "opens the Run box", 1),
   TASKS(KeyEvent.VK_TAB, "Tab", "Task view", "Task view", "shows every open window", 1),
   LEFT(KeyEvent.VK_LEFT, "", "Snap", "Snap left", "snaps the window in front to the left half", 1),
   RIGHT(KeyEvent.VK_RIGHT, "", "Snap", "Snap right", "snaps the window in front to the right half", 1),
   UP(KeyEvent.VK_UP, "", "Max", "Maximize", "maximizes the window in front", 1),
   DOWN(KeyEvent.VK_DOWN, "", "", "Restore or minimize", "restores the window in front, or minimizes it", 1);

   /** The key pressed while the Windows key is held, or 0 for the Windows key alone. */
   final int keyCode;
   /** The letters on the key ("" for the Windows key and the arrows, which are drawn). */
   final String legend;
   /** The small word printed under the letters. */
   final String caption;
   final String title;
   final String does;
   /** How many times the field offers when the shortcut is picked. */
   final int times;

   Shortcut(int keyCode, String legend, String caption, String title, String does, int times) {
      this.keyCode = keyCode;
      this.legend = legend;
      this.caption = caption;
      this.title = title;
      this.does = does;
      this.times = times;
   }

   /** How it is written: "Win+E", "Win+Tab", "Win+Left", "Win". */
   String combo() {
      switch (this) {
         case START: return "Win";
         case LEFT: return "Win+Left";
         case RIGHT: return "Win+Right";
         case UP: return "Win+Up";
         case DOWN: return "Win+Down";
         default: return "Win+" + this.legend;
      }
   }
}
