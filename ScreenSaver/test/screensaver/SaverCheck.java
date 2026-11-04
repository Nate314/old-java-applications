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

      checkTank();

      System.out.println("SaverCheck passed");
   }

   private static List<Saver> savers() {
      return Arrays.asList(new Tiles(Tiles.GREEN_CIRCLES), new Tiles(Tiles.GREEN_SQUARES), new Tiles(Tiles.BRIGHT_CIRCLES),
         new Starfield(), new Mystify(), new Pipes(), new BouncingLogo(), new DotClock(Date::new),
         new Aquarium(new Tank(), Date::new, () -> { }));
   }

   /** The aquarium's tank: arrivals by day, growth, feeding, and the saved lines. */
   private static void checkTank() throws Exception {
      check(Tank.civilDays(1970, 1, 1) == 0, "day 0 is 1970-01-01");
      check(Tank.civilDays(2026, 11, 2) == java.time.LocalDate.of(2026, 11, 2).toEpochDay(), "2026-11-02");
      check(Tank.civilDays(2024, 3, 1) - Tank.civilDays(2024, 2, 28) == 2, "2024 is a leap year");
      check(Tank.dayNumber(new Date(2026 - 1900, 10, 2, 23, 59)) == Tank.dayNumber(new Date(2026 - 1900, 10, 2, 0, 1)),
         "the day turns over at local midnight");

      Tank tank = new Tank();
      Random random = new Random(4);
      check(tank.arrive(100, random).size() == Tank.FIRST_FISH, "three fish on the first look");
      check(tank.arrive(100, random).isEmpty(), "none more the same day");
      check(tank.arrive(99, random).isEmpty(), "none when the clock goes back");
      check(tank.arrive(101, random).size() == 1, "one the next day");
      check(tank.arrive(140, random).size() == 1, "one after a long gap, not one per missed day");
      for (int day = 141; day < 160; day++) {
         tank.arrive(day, random);
      }
      check(tank.fish.size() == Tank.MAX_FISH, "the tank fills up: " + tank.fish.size());
      java.util.Set<String> names = new java.util.HashSet<>();
      int[] species = new int[Tank.SPECIES.length];
      for (Tank.Fish fish : tank.fish) {
         names.add(fish.name);
         species[fish.species]++;
      }
      check(names.size() == Tank.MAX_FISH, "every fish has its own name");
      for (int count : species) {
         check(count == Tank.MAX_FISH / Tank.SPECIES.length, "an even mix of species");
      }
      Tank.Fish first = tank.fish.get(0);
      check(Tank.growth(first, 100) == 0 && Tank.growth(first, 107) == 0.5 && Tank.growth(first, 200) == 1, "grows over a fortnight");
      first.meals = 8;
      check(Tank.growth(first, 100) == 2 / 14.0, "flakes help it grow");

      // Feeding: the flakes get eaten and counted against the fish that ate them.
      Tank fed = new Tank();
      int[] saves = {0};
      Aquarium aquarium = new Aquarium(fed, () -> new Date(2026 - 1900, 10, 2, 12, 0), () -> saves[0]++);
      aquarium.reset(1280, 720, new Random(9));
      check(fed.fish.size() == Tank.FIRST_FISH && saves[0] == 1, "the first look moves fish in and saves");
      aquarium.reset(1280, 720, new Random(9));
      check(fed.fish.size() == Tank.FIRST_FISH && saves[0] == 1, "showing it again the same day changes nothing");
      aquarium.feed();
      check(aquarium.flakeCount() > 0, "F drops flakes");
      for (int frame = 0; frame < 1500 && aquarium.flakeCount() > 0; frame++) {
         aquarium.tick(16);
      }
      int meals = 0;
      for (Tank.Fish fish : fed.fish) {
         meals += fish.meals;
      }
      check(meals > 0 && saves[0] > 1, "the fish eat the flakes: " + meals + " eaten");
      check(aquarium.flakeCount() == 0, "uneaten flakes do not stay forever");
      Aquarium night = new Aquarium(new Tank(), () -> new Date(2026 - 1900, 10, 2, 23, 0), () -> { });
      check(night.daylight() == 0 && aquarium.daylight() == 1, "dark at night, light at noon");

      // The tank is saved with the settings and read back; damaged fish lines are skipped.
      File file = File.createTempFile("screensaver-tank", ".txt");
      file.deleteOnExit();
      Settings settings = new Settings(file);
      settings.tank.fish.addAll(fed.fish);
      settings.tank.lastArrival = fed.lastArrival;
      settings.save();
      Settings loaded = new Settings(file);
      loaded.load();
      check(loaded.tank.fish.size() == fed.fish.size() && loaded.tank.lastArrival == fed.lastArrival, "fish round-trip");
      Tank.Fish back = loaded.tank.fish.get(0);
      check(back.name.equals(fed.fish.get(0).name) && back.meals == fed.fish.get(0).meals
         && back.species == fed.fish.get(0).species && back.born == fed.fish.get(0).born, "a fish keeps name, meals, species, day");
      try (java.io.FileWriter out = new java.io.FileWriter(file)) {
         out.write("saver 8\nfish 9 1 1 Bad\nfish x 1 1 Bad\nfish 1 20000 3 Fin\nfish 2 20000 0 Fin\nfishday nope\n");
      }
      Settings damaged = new Settings(file);
      damaged.load();
      check(damaged.saver == 8 && damaged.tank.fish.size() == 1 && damaged.tank.lastArrival == -1, "damaged fish lines are skipped");
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
