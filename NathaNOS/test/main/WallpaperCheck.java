package main;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Checks the moving wallpapers: each scene moves, the same moment draws the same picture, and the timings hold. */
public final class WallpaperCheck {
   public static void main(String[] args) {
      for (Wallpaper wallpaper : Wallpaper.ALL) {
         boolean scene = !wallpaper.id.equals(Wallpaper.DEFAULT);
         check(wallpaper.moves() == scene, wallpaper.name + (scene ? " moves" : " holds still"));
         if (!scene) {
            continue;
         }
         int[] start = pixels(wallpaper, 0);
         check(Arrays.equals(start, pixels(wallpaper, 0)), wallpaper.name + ": the same moment draws the same picture");
         check(!Arrays.equals(start, pixels(wallpaper, 3)), wallpaper.name + ": three seconds on it has moved");
         // The hills or seabed stay in front: the bottom row never changes.
         int[] later = pixels(wallpaper, 7.5);
         check(Arrays.equals(Arrays.copyOfRange(start, W * (H - 1), W * H), Arrays.copyOfRange(later, W * (H - 1), W * H)), wallpaper.name + ": the foreground stays put");
      }
      check(Wallpaper.shooting(1.9) < 0 && Wallpaper.shooting(2.5) > 0, "the first shooting star comes two seconds in");
      check(Wallpaper.shooting(2 + Wallpaper.SHOOT_FOR + 0.1) < 0, "it is gone after its flight");
      check(Wallpaper.shooting(Wallpaper.SHOOT_EVERY + 2.5) > 0, "another comes a period later");
      double low = 1;
      double high = 0;
      for (double t = 0; t < 20; t += 0.01) {
         low = Math.min(low, Wallpaper.twinkle(t, 1.3, 0.4));
         high = Math.max(high, Wallpaper.twinkle(t, 1.3, 0.4));
      }
      check(low >= 0.15 && low < 0.2 && high > 0.99 && high <= 1, "a star twinkles between dim and full, never out");
      check(Wallpaper.bubbleFade(0, 700, 1000) == 0 && Wallpaper.bubbleFade(350, 700, 1000) == 1 && Wallpaper.bubbleFade(700, 700, 1000) == 0, "a bubble fades in off the seabed and out at the top");
      System.out.println("WallpaperCheck: all checks passed");
   }

   private static final int W = 320;
   private static final int H = 200;

   private static int[] pixels(Wallpaper wallpaper, double seconds) {
      BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
      Graphics g = image.createGraphics();
      wallpaper.paint(g, W, H, null, null, seconds);
      g.dispose();
      return image.getRGB(0, 0, W, H, null, 0, W);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
