package screensaver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
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
      checkMaze();
      checkGallery();
      checkPlay();
      checkMusic();
      checkCalm();

      System.out.println("SaverCheck passed");
   }

   /** Calm (less motion asked for): moving savers slowed right down, tiles kept from flickering, slow settings kept. */
   private static void checkCalm() {
      Saver stars = new Starfield();
      Saver circles = new Tiles(Tiles.GREEN_CIRCLES);
      check(Main.speedFor(stars, 1, true) == Main.CALM_MOTION_SPEED, "calm: the stars slowed at 1x");
      check(Main.speedFor(stars, 0.2, true) == 0.2, "calm: a slower setting stays");
      check(Main.speedFor(stars, 20, false) == Main.MAX_MOTION_SPEED, "not calm: the moving savers still stop at 8x");
      check(Main.speedFor(circles, 250, true) == Main.CALM_TILE_SPEED, "calm: tiles no faster than 4x");
      check(Main.speedFor(circles, 250, false) == 250, "not calm: tiles up to 250x");
      check(Main.speedText(1, 0.35, true).equals("1.0x (calm, for less motion: 0.35x)"), "calm: the caption says so");
      check(Main.speedText(20, 8, false).equals("20.0x"), "not calm: the 8x stop is not called calm");
      check(!Main.calm(), "the desktop JDK never asks for calm");
      // Calm Maze: the walk from above, with the lantern and the orb on it and the rooms walked lit.
      Maze maze = new Maze();
      maze.reset(900, 600, new Random(8));
      maze.tick(20_000);
      BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      maze.paintAbove(g, 900, 600);
      g.dispose();
      int lantern = 0;
      int orb = 0;
      int walked = 0;
      for (int y = 0; y < 600; y++) {
         for (int x = 0; x < 900; x++) {
            int rgb = image.getRGB(x, y) & 0xffffff;
            lantern += rgb == 0xfff8dc ? 1 : 0;
            orb += rgb == 0xffc43c ? 1 : 0;
            walked += rgb == 0x5c4622 ? 1 : 0;
         }
      }
      check(lantern > 0 && orb > 0 && walked > 0, "calm maze from above: lantern " + lantern + ", orb " + orb + ", walked " + walked);
   }

   /** The gallery (G): ten same-sized screens inside the window, under the header, never overlapping; keys move. */
   private static void checkGallery() {
      for (int[] size : new int[][] {{1920, 1080}, {375, 600}, {900, 600}, {400, 700}}) {
         java.awt.Rectangle[] cells = Gallery.layout(size[0], size[1], 10, 16 / 9.0);
         String at = " at " + size[0] + "x" + size[1];
         for (int i = 0; i < cells.length; i++) {
            java.awt.Rectangle c = cells[i];
            check(c.x >= 0 && c.y > Gallery.textSize(size[0], size[1]) * 2 && c.x + c.width <= size[0]
               && c.y + c.height + Gallery.textSize(size[0], size[1]) * 2 <= size[1], "screen " + i + " fits" + at + ": " + c);
            check(c.width == cells[0].width && c.height == cells[0].height && c.width > 40, "screens the same size" + at);
            for (int j = 0; j < i; j++) {
               check(!c.intersects(cells[j]), "screens " + j + " and " + i + " apart" + at);
            }
         }
         java.awt.Rectangle[] buttons = Gallery.buttons(size[0], size[1]);
         for (int k = 0; k < buttons.length; k++) {
            java.awt.Rectangle r = buttons[k];
            check(r.x >= 0 && r.x + r.width <= size[0] && r.y + r.height <= size[1] && r.height >= 22 && r.width >= 80,
               "button " + k + " inside the window and big enough to tap" + at + ": " + r);
            check(k == 0 || !r.intersects(buttons[k - 1]), "buttons apart" + at);
            for (java.awt.Rectangle c : cells) {
               check(r.y >= c.y + c.height + Gallery.textSize(size[0], size[1]) * 2, "button " + k + " under every screen and name" + at);
            }
         }
         for (java.awt.Rectangle c : cells) {
            java.awt.Rectangle b = Gallery.badge(c, Gallery.textSize(size[0], size[1]));
            check(c.contains(b) && b.height >= 16 && b.width >= 36, "the Play badge inside its screen and tappable" + at + ": " + b);
         }
         System.out.println("Gallery" + at + ": " + Gallery.columns(cells) + " across, " + cells[0].width + "x" + cells[0].height);
      }
      check(Gallery.columns(Gallery.layout(1920, 1080, 10, 16 / 9.0)) == 4, "four across on 1920x1080 (larger than five)");
      List<Saver> list = savers();
      for (Saver s : list) {
         s.reset(1920, 1080, new Random(5));
         s.tick(2000);
      }
      Gallery gallery = new Gallery(list, 0);
      BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      gallery.paint(g, 960, 540, 1920, 1080);
      g.dispose();
      for (int i = 0; i < 10; i++) {
         java.awt.Rectangle c = gallery.cell(i);
         check(gallery.at(c.x + c.width / 2, c.y + c.height / 2) == i, "a click on screen " + i + " picks it");
         check(litPixels(image.getSubimage(c.x, c.y, c.width, c.height)) > 0, list.get(i).name() + " shows on its little screen");
      }
      check(gallery.at(0, 0) == -1, "a click on the header picks nothing");
      for (int i = 0; i < 10; i++) {
         java.awt.Rectangle c = gallery.cell(i);
         java.awt.Rectangle b = Gallery.badge(c, Gallery.textSize(960, 540));
         check(gallery.playAt(b.x + b.width / 2, b.y + b.height / 2) == i, "a click on screen " + i + "'s Play badge plays with it");
         check(gallery.playAt(c.x + c.width / 4, c.y + c.height / 4) == -1, "the rest of screen " + i + " is for watching");
      }
      java.awt.Rectangle[] buttons = Gallery.buttons(960, 540);
      for (int k = 0; k < buttons.length; k++) {
         java.awt.Rectangle r = buttons[k];
         check(gallery.button(r.x + r.width / 2, r.y + r.height / 2) == k && gallery.at(r.x + r.width / 2, r.y + r.height / 2) == -1
            && gallery.playAt(r.x + r.width / 2, r.y + r.height / 2) == -1, "a click on button " + k + " presses only it");
         check(litPixels(image.getSubimage(r.x, r.y, r.width, r.height)) > 0, "button " + k + " is drawn");
      }
      check(gallery.button(0, 0) == -1, "a click on the header presses no button");
      gallery.move(-1, 0);
      check(gallery.selected == 9, "Left from the first wraps to the last");
      gallery.move(0, -1);
      int across = Gallery.columns(Gallery.layout(960, 540, 10, 16 / 9.0));
      check(gallery.selected == 9 - across, "Up goes a row up (" + across + " across): " + gallery.selected);
      gallery.move(0, -1);
      gallery.move(0, -1);
      check(gallery.selected == 9 % across, "Up on the top row stays: " + gallery.selected);
   }

   /** Play (T): what the pointer does to each saver, and that letting go or a reset puts it back. */
   private static void checkPlay() {
      double[] none = Saver.away(10, 10, null, 50, 20);
      double[] near = Saver.away(110, 100, new Point(100, 100), 50, 20);
      double[] far = Saver.away(200, 100, new Point(100, 100), 50, 20);
      check(none[0] == 0 && none[1] == 0 && far[0] == 0, "nothing is pushed with no pointer or out of reach");
      check(near[0] > 0 && Math.abs(near[1]) < 1e-9 && near[0] < 20, "a thing right of the pointer is pushed right: " + near[0]);
      check(Saver.away(100, 100, new Point(100, 100), 50, 20)[1] == -20, "on the pointer, pushed straight up the most");

      // The tile savers and the dot clock paint differently with the pointer over them, and the same after it goes.
      for (Saver s : Arrays.asList(new Tiles(Tiles.BRIGHT_CIRCLES), new DotClock(() -> new Date(2026 - 1900, 10, 2, 12, 34)))) {
         s.reset(900, 600, new Random(3));
         s.tick(60_000);
         BufferedImage before = shot(s);
         s.touch(new Point(450, 300), false);
         BufferedImage pushed = shot(s);
         s.touch(null, false);
         check(!same(before, pushed) && same(before, shot(s)), s.name() + ": the pointer pushes things aside, and they come back");
      }

      // Starfield: the stars stream out of a point most of the way to the pointer, then back to the middle.
      Starfield stars = new Starfield();
      stars.reset(1000, 600, new Random(1));
      stars.touch(new Point(900, 100), true);
      for (int i = 0; i < 200; i++) {
         stars.tick(16);
      }
      double[] c = stars.center();
      check(Math.abs(c[0] - 820) < 5 && Math.abs(c[1] - 140) < 5, "stars come from near the pointer: " + c[0] + "," + c[1]);
      stars.touch(null, false);
      for (int i = 0; i < 200; i++) {
         stars.tick(16);
      }
      c = stars.center();
      check(Math.abs(c[0] - 500) < 5 && Math.abs(c[1] - 300) < 5, "and from the middle once it goes");

      // Mystify: held, the corners speed up toward the pointer, at most three times their speed; let go, they slow back.
      Mystify mystify = new Mystify();
      mystify.reset(900, 600, new Random(2));
      mystify.touch(new Point(450, 300), true);
      mystify.tick(3000);
      boolean faster = false;
      boolean capped = true;
      for (int s = 0; s < 2; s++) {
         for (int k = 0; k < 4; k++) {
            faster |= mystify.speed(s, k) > mystify.cruise(s, k) * 1.5;
            capped &= mystify.speed(s, k) <= mystify.cruise(s, k) * 3 + 1e-9;
         }
      }
      check(faster && capped, "held, the corners speed up, up to three times");
      mystify.touch(null, false);
      mystify.tick(20_000);
      boolean back = true;
      for (int s = 0; s < 2; s++) {
         for (int k = 0; k < 4; k++) {
            back &= Math.abs(mystify.speed(s, k) - mystify.cruise(s, k)) < mystify.cruise(s, k) * 0.05;
         }
      }
      check(back, "let go, the corners slow back to their own speed");

      // Pipes: a press on a free cell starts a pipe there, and held, it grows to the pointer.
      Pipes pipes = new Pipes();
      pipes.reset(900, 600, new Random(4));
      pipes.touch(new Point(20, 20), true);
      check(pipes.head()[0] == 0 && pipes.head()[1] == 0, "a press starts a pipe at the top left cell");
      pipes.touch(new Point(880, 580), true);
      for (int i = 0; i < 60; i++) {
         pipes.tick(Pipes.STEP_MILLIS);
      }
      int[] head = pipes.head();
      check(head[0] == 23 && head[1] == 15, "held, the pipe grows to the pointer and waits there: " + head[0] + "," + head[1]);
      pipes.touch(null, false);

      // Bouncing logo: a press off it does nothing; on it, it is held and follows; thrown, it flies fast, then slows.
      BouncingLogo logo = new BouncingLogo();
      logo.reset(900, 600, new Random(6));
      logo.place(100, 50, 400, 300, 0.1, 0.08);
      double drift = Math.hypot(0.1, 0.08);
      logo.touch(new Point(10, 10), true);
      check(!logo.held(), "a press off the logo does not pick it up");
      logo.touch(new Point(10, 10), false);
      logo.touch(new Point(450, 320), true);
      check(logo.held(), "a press on the logo picks it up");
      for (int i = 1; i <= 10; i++) {
         logo.tick(16);
         logo.touch(new Point(450 - i * 10, 320 - i * 6), true);
      }
      check(Math.abs(logo.at()[0] - 300) < 1 && Math.abs(logo.at()[1] - 240) < 1, "held, the logo follows the pointer");
      logo.touch(new Point(350, 260), false);
      check(!logo.held() && logo.speed() > 0.4, "thrown, it flies fast: " + logo.speed());
      logo.tick(16);
      check(logo.at()[0] < 300, "the way it was thrown");
      for (int i = 0; i < 1000; i++) {
         logo.tick(16);
      }
      check(Math.abs(logo.speed() - logo.cruise()) < 1e-9, "then slows back to its drift");

      // Aquarium: one press drops one pinch of flakes, where it was.
      Aquarium tank = new Aquarium(new Tank(), () -> new Date(2026 - 1900, 10, 2, 12, 0), () -> { });
      tank.reset(900, 600, new Random(7));
      tank.touch(new Point(300, 300), true);
      int pinch = tank.flakeCount();
      tank.touch(new Point(310, 300), true);
      check(pinch > 0 && tank.flakeCount() == pinch, "a press drops one pinch, a drag no more");
      tank.touch(new Point(310, 300), false);
      tank.touch(new Point(600, 300), true);
      check(tank.flakeCount() == pinch * 2, "the next press drops another");

      // Maze: held, the walker takes three times the steps.
      Maze slow = new Maze();
      Maze quick = new Maze();
      slow.reset(900, 600, new Random(8));
      quick.reset(900, 600, new Random(8));
      quick.touch(new Point(1, 1), true);
      for (int i = 0; i < 300; i++) {
         slow.tick(16);
         quick.tick(16);
      }
      check(quick.steps > slow.steps * 2, "held, the walker hurries: " + quick.steps + " vs " + slow.steps);

      for (Saver s : savers()) {
         check(!Main.playHint(s).isEmpty(), s.name() + " says what the mouse does");
      }
   }

   private static BufferedImage shot(Saver s) {
      BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      s.paint(g);
      g.dispose();
      return image;
   }

   private static boolean same(BufferedImage a, BufferedImage b) {
      for (int y = 0; y < a.getHeight(); y++) {
         for (int x = 0; x < a.getWidth(); x++) {
            if (a.getRGB(x, y) != b.getRGB(x, y)) {
               return false;
            }
         }
      }
      return true;
   }

   /** Music (M): silent until switched on; then every saver rings notes of the one scale, never faster than its gap. */
   private static void checkMusic() {
      List<int[]> heard = new java.util.ArrayList<>();
      int[] open = new int[1];
      Music.listen(new Music.Voice() {
         @Override
         public void on(int part, int note, int velocity) {
            heard.add(new int[] {part, note, velocity});
            open[0]++;
         }

         @Override
         public void off(int part, int note) {
            open[0]--;
         }
      });
      List<Saver> list = savers();
      Music.live(false);
      for (Saver s : list) {
         s.reset(1920, 1080, new Random(3));
         for (int frame = 0; frame < 1000; frame++) {
            s.tick(16);
            Music.tick(16);
         }
      }
      check(heard.isEmpty(), "no music until M");
      Music.live(true);
      for (Saver s : list) {
         heard.clear();
         s.reset(1920, 1080, new Random(3));
         // The logo learns its size from its first paint.
         s.paint(new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB).createGraphics());
         for (int frame = 0; frame < 4000; frame++) {
            s.tick(16 * (s instanceof Tiles ? 40 : 1));
            Music.tick(16);
         }
         System.out.println("Music: " + s.name() + ", " + heard.size() + " notes in 64 s");
         check(!heard.isEmpty(), s.name() + " plays music");
         // 64 s of real time: even the beads at 40x speed (160 tiles a second) keep to one note per 0.06 s.
         check(heard.size() <= 64 / 0.06 + 40, s.name() + " is not a buzz: " + heard.size() + " notes");
         for (int[] n : heard) {
            check(n[1] >= 36 && n[1] <= 108 && java.util.Arrays.asList(0, 2, 4, 7, 9).contains(n[1] % 12),
               s.name() + " stays in the pentatonic scale: " + n[1]);
            check(n[2] > 0 && n[2] <= 127, "a fair loudness");
         }
      }
      check(Music.sounding() <= 32, "a bounded number of notes held");
      Music.live(false);
      check(Music.sounding() == 0 && open[0] == 0, "off lets every note go: " + open[0]);
      Music.listen(null);
   }

   private static List<Saver> savers() {
      return Arrays.asList(new Tiles(Tiles.GREEN_CIRCLES), new Tiles(Tiles.GREEN_SQUARES), new Tiles(Tiles.BRIGHT_CIRCLES),
         new Starfield(), new Mystify(), new Pipes(), new BouncingLogo(), new DotClock(Date::new),
         new Aquarium(new Tank(), Date::new, () -> { }), new Maze());
   }

   /** The maze: one way between any two rooms, and the left-hand walker always gets through, then a new maze. */
   private static void checkMaze() {
      Maze maze = new Maze();
      for (int seed = 0; seed < 30; seed++) {
         maze.reset(800, 600, new Random(seed));
         int openBlocks = 0;
         int links = 0;
         for (int y = 0; y < Maze.SIZE; y++) {
            for (int x = 0; x < Maze.SIZE; x++) {
               if (maze.open[y][x]) {
                  openBlocks++;
                  links += (x + 1 < Maze.SIZE && maze.open[y][x + 1] ? 1 : 0) + (y + 1 < Maze.SIZE && maze.open[y + 1][x] ? 1 : 0);
               }
            }
         }
         // A tree of rooms and doorways: every room carved, and one link fewer than blocks means no loops.
         check(openBlocks == Maze.ROOMS * Maze.ROOMS * 2 - 1, "every room carved: " + openBlocks);
         check(links == openBlocks - 1, "no loops in maze " + seed);
         check(maze.route[1][1] && maze.route[Maze.SIZE - 2][Maze.SIZE - 2], "the route joins start and goal");
         double walked = 0;
         while (!maze.showingMap() && walked < 4_000_000) {
            maze.tick(50);
            walked += 50;
         }
         check(maze.showingMap() && maze.atGoal(), "the walker reaches the orb in maze " + seed);
         check(maze.steps < Maze.SIZE * Maze.SIZE * 2, "and walks no block more than twice each way: " + maze.steps);
         maze.tick(Maze.MAP_MILLIS);
         check(!maze.showingMap() && maze.bx == 1 && maze.by == 1 && maze.steps == 0, "then a new maze from the start");
      }
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
