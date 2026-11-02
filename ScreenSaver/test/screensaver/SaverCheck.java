package screensaver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

/** Self-check for the savers, the dot clock's digits, the corner hit and the settings file. Run with java -ea. */
public class SaverCheck {
   public static void main(String[] args) throws Exception {
      // Tile size: the original's greatest common factor where it gives sensible rows, otherwise about 9 rows.
      check(Tiles.autoSize(1920, 1080) == 120, "1920x1080 keeps the original 120 px");
      check(Tiles.autoSize(1366, 768) == 85, "1366x768 (factor 2) gets about 9 rows: " + Tiles.autoSize(1366, 768));
      check(Tiles.autoSize(400, 300) == 33, "400x300 (factor 100, only 3 rows) gets about 9 rows");

      // A tile cycle fills the grid one tile per 250 ms, then empties it.
      Tiles tiles = new Tiles(Tiles.GREEN_CIRCLES);
      tiles.reset(1920, 1080, new Random(1));
      check(tiles.tileCount() == 16 * 9, "16 x 9 tiles on 1920x1080: " + tiles.tileCount());
      tiles.tick(249);
      check(tiles.revealedCount() == 0, "nothing before 250 ms");
      tiles.tick(1);
      check(tiles.revealedCount() == 1, "one tile at 250 ms");
      tiles.tick(250.0 * (tiles.tileCount() - 1));
      check(tiles.revealedCount() == tiles.tileCount(), "full after one step per tile");
      tiles.tick(250.0 * 10);
      check(tiles.revealedCount() == tiles.tileCount() - 10, "then emptying");
      tiles.setSize(5);
      check(tiles.size() == Tiles.MIN_SIZE && tiles.revealedCount() == 0, "size floor, and a new cycle");

      // Every saver runs and paints at a desktop size and a phone size without throwing, and draws something.
      for (int[] size : new int[][] {{1920, 1080}, {375, 600}}) {
         for (Saver saver : savers()) {
            saver.reset(size[0], size[1], new Random(7));
            BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
            boolean drew = false;
            for (int frame = 0; frame < 3000; frame++) {
               saver.tick(16 * (frame % 50 == 0 ? 6 : 1));
               if (frame % 100 == 99) {
                  Graphics2D g = image.createGraphics();
                  g.setColor(Color.BLACK);
                  g.fillRect(0, 0, size[0], size[1]);
                  saver.paint(g);
                  g.dispose();
                  drew |= litPixels(image) > 0;
               }
            }
            check(drew, saver.name() + " draws something at " + size[0] + "x" + size[1]);
         }
      }

      // Pipes clear the screen when it fills, so the list of pieces stays bounded.
      Pipes pipes = new Pipes();
      pipes.reset(1920, 1080, new Random(3));
      int most = 0;
      for (int i = 0; i < 20000; i++) {
         pipes.tick(Pipes.STEP_MILLIS);
         most = Math.max(most, pipes.pieceCount());
      }
      check(most > 50 && most < 2000, "pipes grow and get cleared: at most " + most + " pieces");

      // The dot clock's face and digits.
      check(DotClock.face(new Date(2026 - 1900, 10, 1, 9, 5)).equals(" 9:05"), "9:05 with a blank tens digit");
      check(DotClock.face(new Date(2026 - 1900, 10, 1, 0, 30)).equals("12:30"), "half past midnight is 12:30");
      check(DotClock.face(new Date(2026 - 1900, 10, 1, 23, 59)).equals("11:59"), "11:59 PM");
      String face = "12:34";
      check(!DotClock.dotOn(face, 0, 0, true) && DotClock.dotOn(face, 1, 0, true), "the 1's top dot is in the middle");
      check(DotClock.dotOn(face, 8, 1, true) && !DotClock.dotOn(face, 8, 2, true), "colon dots at rows 1 and 3");
      check(!DotClock.dotOn(face, 8, 1, false), "the colon blinks");
      check(!DotClock.dotOn(" 9:05", 0, 0, true), "a blank digit has no dots");
      for (int c = 0; c < DotClock.COLUMNS; c++) {
         for (int r = 0; r < 5; r++) {
            check(!DotClock.dotOn("88:88", c, r, true) || DotClock.hasDot(c, r), "a lit dot is never in a gap");
         }
      }
      check(!DotClock.hasDot(3, 0) && !DotClock.hasDot(13, 4) && !DotClock.hasDot(8, 2), "gaps have no dots");
      for (String[] digit : DotClock.DIGITS) {
         check(digit.length == 5 && digit[0].length() == 3, "digits are 3 x 5");
      }

      // The logo aimed straight at a corner counts one corner hit; a plain bounce counts none.
      BouncingLogo logo = new BouncingLogo();
      logo.reset(800, 600, new Random(5));
      logo.place(100, 50, 5, 5, -0.1, -0.1);
      logo.tick(100);
      check(logo.corners() == 1, "one corner hit: " + logo.corners());
      logo.place(100, 50, 5, 300, -0.1, 0.1);
      logo.tick(100);
      check(logo.corners() == 1, "a side bounce is not a corner");

      // Settings round-trip and survive damaged lines.
      File file = File.createTempFile("screensaver", ".txt");
      file.deleteOnExit();
      Settings settings = new Settings(file);
      settings.saver = 5;
      settings.cycle = false;
      settings.speed = 2.5;
      settings.tileSize = 64;
      settings.save();
      Settings loaded = new Settings(file);
      loaded.load();
      check(loaded.saver == 5 && !loaded.cycle && loaded.speed == 2.5 && loaded.tileSize == 64, "settings round-trip");
      try (java.io.FileWriter out = new java.io.FileWriter(file)) {
         out.write("saver x\nspeed 99999\ngarbage\ncycle off\n");
      }
      Settings damaged = new Settings(file);
      damaged.load();
      check(damaged.saver == 0 && damaged.speed == Main.MAX_SPEED && !damaged.cycle, "damaged lines keep defaults");
      Settings missing = new Settings(new File(file.getPath() + ".missing"));
      missing.load();
      check(missing.cycle && missing.speed == 1.0, "no file: defaults");

      System.out.println("SaverCheck passed");
   }

   private static List<Saver> savers() {
      return Arrays.asList(new Tiles(Tiles.GREEN_CIRCLES), new Tiles(Tiles.GREEN_SQUARES), new Tiles(Tiles.BRIGHT_CIRCLES),
         new Starfield(), new Mystify(), new Pipes(), new BouncingLogo(), new DotClock(Date::new));
   }

   private static int litPixels(BufferedImage image) {
      int count = 0;
      for (int x = 0; x < image.getWidth(); x += 3) {
         for (int y = 0; y < image.getHeight(); y += 3) {
            if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
               count++;
            }
         }
      }
      return count;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
