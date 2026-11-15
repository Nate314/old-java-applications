package windowse;

import java.util.ArrayList;
import java.util.List;

/**
 * What the shortcuts do to a screen, from a clear desktop: the File Explorer windows Win+E opens (cascaded, snapped,
 * maximized, minimized or hidden by Win+D) and the Start menu, Run box and task view. It follows the site's pretend
 * Windows, which follows Windows: the arrows move the File Explorer window in front, Left or Right from the other
 * half puts it back, Down puts a snapped or maximized window back and minimizes one that is not.
 */
final class Screen {
   /** As many File Explorer windows as the pretend Windows opens at once. */
   static final int MAX_WINDOWS = 40;

   static final class Window {
      /** Its place in the cascade: the how-many-th window opened. */
      final int slot;
      int z;
      /** "", "left", "right" or "max". */
      String snap = "";
      boolean min;

      Window(int slot) {
         this.slot = slot;
      }
   }

   final List<Window> windows = new ArrayList<>();
   boolean start;
   boolean run;
   boolean tasks;
   /** Whether an arrow was pressed with no File Explorer window to move. */
   boolean missed;
   private final List<Window> peeked = new ArrayList<>();   // hidden by Win+D, to bring back
   private int front;

   /** The screen after pressing these steps on a clear desktop. */
   static Screen after(List<Step> steps) {
      Screen screen = new Screen();
      for (Step step : steps) {
         // past ~2 x 40 presses of one key the screen only flips between two states (the toggles), so keep the parity
         int times = step.times > 200 ? 200 + step.times % 2 : step.times;
         for (int i = 0; i < times; i++) {
            screen.apply(step.shortcut);
         }
      }
      return screen;
   }

   void apply(Shortcut shortcut) {
      switch (shortcut) {
         case START: this.start = !this.start; break;
         case EXPLORER:
            if (this.windows.size() < MAX_WINDOWS) {
               Window w = new Window(this.windows.size());
               this.windows.add(w);
               w.z = ++this.front;
            }
            break;
         case DESKTOP: this.showDesktop(); break;
         case RUN: this.run = true; break;
         case TASKS: this.tasks = !this.tasks; break;
         default: this.snap(shortcut);
      }
   }

   /** The window in front, or null when none shows. */
   Window top() {
      Window best = null;
      for (Window w : this.windows) {
         if (!w.min && (best == null || w.z > best.z)) {
            best = w;
         }
      }
      return best;
   }

   boolean hidden(Window w) {
      return this.peeked.contains(w);
   }

   private void showDesktop() {
      List<Window> up = new ArrayList<>();
      for (Window w : this.windows) {
         if (!w.min) {
            up.add(w);
         }
      }
      if (!up.isEmpty() || this.start || this.run || this.tasks) {
         this.start = this.run = this.tasks = false;
         for (Window w : up) {
            w.min = true;
         }
         this.peeked.addAll(up);
      } else {
         for (Window w : this.peeked) {
            w.min = false;
            w.z = ++this.front;
         }
         this.peeked.clear();
      }
   }

   private void snap(Shortcut shortcut) {
      Window w = this.top();
      if (w == null) {
         this.missed = true;
         return;
      }
      String now = w.snap;
      if (shortcut == Shortcut.DOWN && now.isEmpty()) {
         w.min = true;
         return;
      }
      w.snap = shortcut == Shortcut.DOWN ? "" : shortcut == Shortcut.UP ? "max"
         : now.equals(shortcut == Shortcut.LEFT ? "right" : "left") ? "" : shortcut == Shortcut.LEFT ? "left" : "right";
      w.z = ++this.front;
   }

   /** In words: "2 File Explorer windows: 1 on the left half, 1 on the right half." and what else is open. */
   String summary() {
      List<String> parts = new ArrayList<>();
      int n = this.windows.size();
      if (n > 0) {
         int left = 0, right = 0, max = 0, min = 0, hid = 0;
         for (Window w : this.windows) {
            if (this.hidden(w)) {
               hid++;
            } else if (w.min) {
               min++;
            } else if (w.snap.equals("left")) {
               left++;
            } else if (w.snap.equals("right")) {
               right++;
            } else if (w.snap.equals("max")) {
               max++;
            }
         }
         List<String> how = new ArrayList<>();
         count(how, left, "on the left half");
         count(how, right, "on the right half");
         count(how, max, "maximized");
         count(how, min, "minimized");
         count(how, hid, "hidden by Win+D");
         parts.add(n + " File Explorer window" + (n == 1 ? "" : "s") + (how.isEmpty() ? "" : ": " + String.join(", ", how)));
      }
      if (this.start) {
         parts.add("the Start menu open");
      }
      if (this.run) {
         parts.add("the Run box open");
      }
      if (this.tasks) {
         parts.add("task view open");
      }
      String s = parts.isEmpty() ? "Nothing open" : String.join("; ", parts);
      s = Character.toUpperCase(s.charAt(0)) + s.substring(1) + ".";
      return this.missed ? s + " The arrows found no File Explorer window to move." : s;
   }

   private static void count(List<String> how, int n, String what) {
      if (n > 0) {
         how.add(n + " " + what);
      }
   }
}
