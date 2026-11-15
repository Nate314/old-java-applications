package windowse;

import java.util.ArrayList;
import java.util.List;

/** One step of a list of shortcuts: a shortcut and how many times to press it. */
final class Step {
   /** The most times a sent list may press one step (a friend's link is not trusted further). */
   static final int MOST_TIMES = 999;
   final Shortcut shortcut;
   final int times;

   Step(Shortcut shortcut, int times) {
      this.shortcut = shortcut;
      this.times = times;
   }

   /** How it is written on the list: "Win+E x3", "Win+Left". */
   String label() {
      return this.shortcut.combo() + (this.times > 1 ? " x" + this.times : "");
   }

   /** A list as a friend's link carries it: "Ex10-Left-E-Right" (each combo without "Win+", the Windows key alone "Win"). */
   static String code(List<Step> steps) {
      StringBuilder s = new StringBuilder();
      for (Step step : steps) {
         s.append(s.length() == 0 ? "" : "-").append(step.label().replace("Win+", "").replace(" ", ""));
      }
      return s.toString();
   }

   /** The list a code stands for (any case), or null when it is not one: an unknown key, a bad count, or too many steps. */
   static List<Step> parse(String code) {
      List<Step> steps = new ArrayList<>();
      for (String part : code.trim().split("-", -1)) {
         int x = part.toLowerCase().indexOf('x');
         int times = x < 0 ? 1 : WindowsEWindow.parseCount(part.substring(x + 1));
         Shortcut shortcut = byName(x < 0 ? part : part.substring(0, x));
         if (shortcut == null || times < 1 || times > MOST_TIMES || steps.size() == WindowsEWindow.MAX_STEPS) {
            return null;
         }
         steps.add(new Step(shortcut, times));
      }
      return steps;
   }

   private static Shortcut byName(String name) {
      for (Shortcut s : Shortcut.values()) {
         if (s.combo().replace("Win+", "").equalsIgnoreCase(name)) {
            return s;
         }
      }
      return null;
   }
}
