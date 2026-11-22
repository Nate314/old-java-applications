package main;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Checks where new windows open: the middle first, then beside the open ones, clear of the icons where there is room. */
public final class PlacementCheck {
   public static void main(String[] args) {
      Rectangle room = new Rectangle(0, 0, 1568, 672);
      Rectangle icons = new Rectangle(12, 12, 240, 600);
      List<Rectangle> open = new ArrayList<>();

      Point first = Placement.spot(466, 530, room, open, icons);
      check(first.equals(new Point(551, 71)), "the first window opens in the exact middle: " + first);
      open.add(new Rectangle(first.x, first.y, 466, 530));

      Point second = Placement.spot(480, 480, room, open, icons);
      Rectangle secondAt = new Rectangle(second.x, second.y, 480, 480);
      check(!secondAt.intersects(open.get(0)) && !secondAt.intersects(icons), "the second opens beside it, clear of it and the icons: " + second);
      check(second.x >= first.x + 466, "on the side with room (right of the first)");
      check(second.y == (672 - 480) / 2, "level with the middle of the desktop");
      open.add(secondAt);

      Point third = Placement.spot(500, 400, room, open, icons);
      Rectangle thirdAt = new Rectangle(third.x, third.y, 500, 400);
      check(!thirdAt.intersects(open.get(0)) || !thirdAt.intersects(open.get(1)), "a third window on a full desktop covers at most one of the others");
      check(room.contains(thirdAt), "and stays on the desktop");

      Point big = Placement.spot(2000, 900, room, open, icons);
      check(big.equals(new Point(0, 0)), "a window bigger than the desktop opens at its corner");

      Point narrow = Placement.spot(360, 500, new Rectangle(0, 0, 400, 760), Arrays.asList(new Rectangle(20, 100, 360, 500)), new Rectangle(12, 12, 192, 600));
      check(narrow.x >= 0 && narrow.x + 360 <= 400, "on a phone it stays on the screen: " + narrow);
      System.out.println("PlacementCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
