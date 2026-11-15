package main;

import java.awt.Rectangle;

/** Checks where the window outlines fly: from the start rectangle to the end one, slowing as they land. */
public final class ZoomCheck {
   public static void main(String[] args) {
      Rectangle icon = new Rectangle(10, 20, 64, 64);
      Rectangle window = new Rectangle(400, 200, 600, 400);
      check(Zoom.at(icon, window, 0).equals(icon), "the first outline is the icon");
      check(Zoom.at(icon, window, Zoom.STEPS).equals(window), "the last outline is the window");
      check(Zoom.at(icon, window, Zoom.STEPS + 5).equals(window), "steps past the end stay on the window");
      Rectangle half = Zoom.at(icon, window, Zoom.STEPS / 2);
      check(half.x > (icon.x + window.x) / 2 && half.x < window.x, "halfway in steps is past halfway in distance");
      check(Zoom.at(window, icon, Zoom.STEPS).equals(icon), "minimizing lands on the button");
      System.out.println("ZoomCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
