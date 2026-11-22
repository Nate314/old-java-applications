package main;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

/** Checks where the icon card opens beside an icon, how its words wrap, and that every app has a line. */
public final class IconTipCheck {
   public static void main(String[] args) {
      Rectangle left = new Rectangle(12, 12, 96, 92);
      check(place(left, 1280, 720).equals(new Point(112, 12)), "a card opens right of an icon with room there");
      Rectangle right = new Rectangle(1180, 104, 96, 92);
      check(place(right, 1280, 720).equals(new Point(1176 - 250, 104)), "an icon at the right edge gets its card on the left");
      Rectangle low = new Rectangle(12, 650, 96, 92);
      check(place(low, 1280, 700).y == 700 - 150, "a card near the taskbar is lifted above it");
      Rectangle narrow = new Rectangle(100, 12, 96, 92);
      Point under = place(narrow, 300, 600);
      check(under.y == 104 && under.x >= 0 && under.x + 250 <= 300, "on a screen too narrow for either side the card opens under the icon, on the screen");
      Point over = place(new Rectangle(100, 500, 96, 92), 300, 600);
      check(over.y == 500 - 150, "and over it when there is no room under it");

      FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics().getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
      String about = IconTip.about("SynthesizerKeyboard");
      List<String> lines = AppIcon.wrap(about, metrics, 226, 3);
      check(lines.size() <= 3 && String.join(" ", lines).equals(about), "a line fits in three lines with no word lost");
      List<String> two = AppIcon.wrap("Ultimate Tic Tac Toe", metrics, 50, 2);
      check(two.size() == 2 && two.get(1).endsWith("..."), "icon labels still stop at two lines, the second cut");
      for (String app : new String[] {"BibleDownloader", "CircleSpawner", "Clock", "DinoGame", "FlappyBird", "GroupCreator", "Minesweeper",
            "MusicTheoryExpert", "NumberGuess", Launcher.PAINT, "RubiksCube2D", "ScreenSaver", "SimpleAlgorithms", "SpriteFloatyStars",
            "StockChecker", "SynthesizerKeyboard", "TankGame", "UltimateTicTacToe"}) {
         String line = IconTip.about(app);
         check(!line.contains(".jar") && String.join(" ", AppIcon.wrap(line, metrics, 226, 3)).equals(line), app + " has its own line, and it fits");
      }
      check(IconTip.about("MyGame").equals("MyGame.jar, an app from the assets folder."), "a jar added by hand is named by its file");
      System.out.println("IconTipCheck: all checks passed");
   }

   private static Point place(Rectangle icon, int screenWidth, int bottom) {
      return IconTip.place(icon, 250, 150, screenWidth, bottom);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
