package dinosaurgameplayer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Self-check for the game, the in-window bot and the saved records. Run with java -ea (headless is fine). */
public final class RunnerCheck {
   public static void main(String[] args) throws Exception {
      if (!RunnerCheck.class.desiredAssertionStatus()) {
         throw new IllegalStateException("Run with -ea");
      }

      // The bot's rule looks at the blue channel only, as the original did.
      assert ObstacleDetector.isObstacle(0x535353);
      assert ObstacleDetector.isObstacle(0xFF0053) : "any color with blue 83";
      assert !ObstacleDetector.isObstacle(0x535354) && !ObstacleDetector.isObstacle(0xF7F7F7);
      assert !ObstacleDetector.isObstacle(RunnerPanel.PAPER.getRGB()) && !ObstacleDetector.isObstacle(RunnerPanel.MARKER.getRGB());

      // A tap is a short hop, a held key goes higher, and the run starts on the first press.
      Runner tap = new Runner(1);
      assert tap.state() == Runner.State.READY;
      tap.press();
      assert tap.state() == Runner.State.RUNNING && tap.isJumping();
      tap.step();
      tap.release();
      int tapPeak = peak(tap);
      Runner held = new Runner(1);
      held.press();
      int heldPeak = peak(held);
      assert tapPeak >= 30 && tapPeak < heldPeak : "tap " + tapPeak + ", held " + heldPeak;
      assert heldPeak > 58 : "a held jump clears a middle bird: " + heldPeak;

      // Ducking makes the dinosaur lower; not while in the air.
      Runner duck = new Runner(1);
      duck.press();
      duck.release();
      duck.duck(true);
      assert !duck.isDucking() : "no ducking in the air";
      while (duck.isJumping()) {
         duck.step();
      }
      duck.step();
      assert duck.isDucking() && duck.dinoTop() == Runner.GROUND - Runner.DUCK_H;

      // The same seed plays the same game.
      assert botRun(5, null) == botRun(5, null);

      // The bot plays well past the first birds with its marker where it starts, then a bird it cannot see ends it.
      int total = 0;
      for (long seed = 2; seed <= 7; seed++) {
         RunnerPanel p = panel(seed, null);
         p.setBot(true);
         run(p);
         assert p.runner().score() > 300 : "seed " + seed + ": bot scored " + p.runner().score();
         assert p.crashLines().length == 2 && p.crashLines()[0].startsWith("The bot scored ") && !p.crashLines()[1].isEmpty();
         total += p.runner().score();
      }
      assert total / 6 > 400 : "bot average " + total / 6;
      assert p2BotBest(3) > 0 : "a bot run sets the bot's best";

      // A marker on the far side of the window jumps much too early.
      assert botRun(4, new int[] { 600, PixelBot.DEFAULT_Y }) < botRun(4, null);
      // The marker stays ahead of the dinosaur and above the ground.
      PixelBot bot = new PixelBot();
      bot.moveTo(0, 999);
      assert bot.x() > Runner.DINO_X + Runner.DINO_W && bot.y() < Runner.GROUND;

      // Your runs count for your best; a run the bot joined counts for nobody.
      File file = File.createTempFile("dinogame", ".txt");
      file.delete();
      RunnerPanel you = new RunnerPanel(Records.load(file), 9);
      you.runner().press();
      run(you);
      Records saved = Records.load(file);
      assert saved.best == you.runner().score() && saved.best > 0 && saved.botBest == 0 : "your best " + saved.best;
      RunnerPanel team = new RunnerPanel(Records.load(file), 9);
      team.runner().press();
      team.step();
      team.setBot(true);
      run(team);
      assert team.crashLines()[0].startsWith("You and the bot") : team.crashLines()[0];
      assert Records.load(file).botBest == 0 && Records.load(file).best == saved.best;

      // An evolved bot's eye sees exactly the obstacles drawn into the frame, birds and cacti alike.
      Evolution.Dino strong = new Evolution.Dino(Brain.decode("144,146,65,117,120,299"), 1);
      BufferedImage img = new BufferedImage(Runner.WIDTH, RunnerPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
      int inked = 0;
      boolean sawBird = false;
      for (int s = 0; strong.running(); s++) {
         if (s % 29 == 0) {
            Graphics2D g = img.createGraphics();
            RunnerPanel.paintWorld(g, strong.runner);
            g.dispose();
            for (int y = Brain.MIN_Y; y <= Brain.MAX_Y; y++) {
               for (int x = Brain.MIN_X; x <= Brain.MAX_X; x++) {
                  boolean ink = ObstacleDetector.isObstacle(img.getRGB(x, y) & 0xFFFFFF);
                  assert ink == strong.runner.inkAt(x, y) : "step " + s + " pixel " + x + "," + y;
                  inked += ink ? 1 : 0;
               }
            }
            for (Runner.Obstacle o : strong.runner.obstacles()) {
               sawBird |= o.kind == Runner.Kind.BIRD && o.x < Runner.WIDTH - o.w;
            }
         }
         strong.step();
      }
      assert inked > 10000 && sawBird : "checked " + inked + " inked pixels";
      // That brain ducks under the middle birds and runs the whole course of seed 1.
      assert strong.finished() && strong.score() == Evolution.FINISH && strong.ducks() > 0 : "scored " + strong.score();

      // Evolution: generation 1 races the original bot and 19 random brains on one course; 40 generations
      // later the best brain runs much further than the original did.
      Evolution ev = new Evolution(5, null, 0, 0, new ArrayList<>());
      assert ev.generation() == 1 && ev.dinos().size() == Evolution.SIZE;
      assert ev.dinos().get(0).brain.encode().equals(Brain.original().encode());
      for (Evolution.Dino d : ev.dinos()) {
         assert d.runner.obstacles().isEmpty() && d.runner.state() == Runner.State.RUNNING;
      }
      int originalScore = -1;
      while (ev.generation() <= 40) {
         if (ev.step()) {
            assert ev.result() != null && ev.result()[0].startsWith("Generation " + ev.generation() + ": the best ran ");
            if (originalScore < 0) {
               originalScore = ev.dinos().get(0).score();
            }
         }
      }
      assert ev.history().size() == 40 && ev.championScore() == maxOf(ev.history());
      assert ev.championScore() > 2 * originalScore && ev.champion().ducks()
         : "record " + ev.championScore() + " vs the original's " + originalScore;
      // Carrying on from a saved champion: the next generation number, the champion runs first.
      Evolution more = new Evolution(6, ev.champion(), 40, ev.championScore(), ev.history());
      assert more.generation() == 41 && more.dinos().get(0).brain.encode().equals(ev.champion().encode());
      assert Brain.decode("1,2,3") == null && Brain.decode("a,b,c,d,e,f") == null;
      assert Brain.decode("0,0,0,999,999,-5").encode().equals(Brain.MIN_X + "," + Brain.MIN_Y + "," + Brain.MIN_MS + ","
         + Brain.MAX_X + "," + Brain.MAX_Y + ",0") : "numbers are kept in range";

      // In the window: E starts evolving where the file left off, and each generation is saved.
      File evoFile = File.createTempFile("dinogame", ".txt");
      evoFile.delete();
      RunnerPanel evo = new RunnerPanel(Records.load(evoFile), 3);
      evo.runner().press();
      evo.step();
      evo.setEvolving(true);
      assert evo.evolution() != null && evo.runner().state() == Runner.State.READY : "the run under way is dropped";
      for (int i = 0; i < 60 * 600 && Records.load(evoFile).generations < 2; i++) {
         evo.step();
      }
      Records evoSaved = Records.load(evoFile);
      assert evoSaved.generations == 2 && evoSaved.champion != null && evoSaved.chart.size() == 2
         && evoSaved.evolvedBest == maxOf(evoSaved.chart) : "saved " + evoSaved.generations;
      evo.setEvolving(false);
      assert evo.evolution() == null && evo.runner().state() == Runner.State.READY;
      evo.setEvolving(true);
      assert evo.evolution().generation() == 3 : "carries on at generation " + evo.evolution().generation();
      evoFile.delete();

      // Night falls at 700 for 12 s, fading in and out, and again at 1400; a new run starts by day.
      Sky sky = new Sky();
      sky.step(699);
      assert sky.darkness() == 0 && sky.nights() == 0;
      sky.step(700);
      assert sky.darkness() > 0 && sky.darkness() < 0.1 && sky.nights() == 1 : "dusk " + sky.darkness();
      for (int i = 0; i < Sky.NIGHT_STEPS / 2; i++) {
         sky.step(700 + i / 10);
      }
      assert sky.darkness() == 1 : "full night";
      for (int i = 0; i < Sky.NIGHT_STEPS / 2; i++) {
         sky.step(800);
      }
      assert sky.darkness() == 0 && sky.nightStep() == -1 : "dawn";
      sky.step(1399);
      assert sky.darkness() == 0;
      sky.step(1400);
      assert sky.darkness() > 0 && sky.nights() == 2 && sky.moonPhase() == 1;
      sky.reset();
      assert sky.darkness() == 0 && sky.nights() == 0;

      // From the first moment of dusk no color the game draws has blue 83, so the original bot is blind at night
      // and a fading sky never makes it jump.
      java.awt.Color[] colors = { RunnerPanel.INK, RunnerPanel.PAPER, RunnerPanel.MUTED, new java.awt.Color(0xD6D6D6),
         new java.awt.Color(0xBDBDBD), new java.awt.Color(0xEBEBEB) };
      for (java.awt.Color c : colors) {
         for (int i = 1; i <= 1000; i++) {
            assert !ObstacleDetector.isObstacle(Sky.tint(c, i / 1000.0).getRGB() & 0xFFFFFF) : c + " at " + i;
            assert !ObstacleDetector.isObstacle(Sky.mix(Sky.tint(RunnerPanel.PAPER, i / 1000.0), new java.awt.Color(0xE6E6E6), i / 1000.0).getRGB() & 0xFFFFFF);
         }
      }
      assert ObstacleDetector.isObstacle(Sky.tint(RunnerPanel.INK, 0).getRGB() & 0xFFFFFF) : "by day the bot sees";
      // Drawn: a course with birds under a whole night, dusk to dawn, shows the bot no obstacle anywhere it can look,
      // though the same frames by day are full of them.
      Evolution.Dino nightRun = new Evolution.Dino(Brain.decode("144,146,65,117,120,299"), 1);
      Sky nightSky = new Sky();
      int dayInk = 0;
      for (int s = 0; s < Sky.NIGHT_STEPS + 300 && nightRun.running(); s++) {
         nightSky.step(s == 150 ? Sky.NIGHT_EVERY : 0);
         if (s % 11 == 0) {
            Graphics2D g = img.createGraphics();
            RunnerPanel.paintWorld(g, nightRun.runner, nightSky);
            g.dispose();
            boolean night = nightSky.darkness() > 0;
            for (int y = Runner.GROUND - 110; y < Runner.GROUND; y++) {
               for (int x = Runner.DINO_X + Runner.DINO_W + 1; x < Runner.WIDTH; x++) {
                  boolean ink = ObstacleDetector.isObstacle(img.getRGB(x, y) & 0xFFFFFF);
                  assert !(night && ink) : "night step " + nightSky.nightStep() + " pixel " + x + "," + y;
                  dayInk += ink ? 1 : 0;
               }
            }
         }
         nightRun.step();
      }
      assert dayInk > 1000 && nightSky.nights() == 1 && nightSky.darkness() == 0 : "day ink " + dayInk;
      // A bot crash at night says why.
      Runner blind = new Runner(2);
      blind.press();
      blind.release();
      while (blind.state() == Runner.State.RUNNING) {
         blind.step();
      }
      assert PixelBot.whyItCrashed(blind, true).startsWith("Night fell") && !PixelBot.whyItCrashed(blind, false).startsWith("Night");

      // Dust: a jump kicks some up and the landing more, and it all stays behind the bot's marker.
      Dust dust = new Dust();
      Runner hop = new Runner(1);
      hop.press();
      dust.step(hop);
      assert dust.count() >= 5 : "take-off dust";
      while (hop.isJumping()) {
         hop.step();
         dust.step(hop);
         assert dust.right() < PixelBot.DEFAULT_X - 30 : "dust at " + dust.right();
      }
      assert dust.count() >= 8 : "landing dust " + dust.count();
      for (int i = 0; i < 60; i++) {
         hop.step();
         dust.step(hop);
      }
      assert dust.count() < 8 : "dust settles";

      // The file keeps both bests and the marker; damaged lines are skipped.
      Records r = Records.load(file);
      r.best = 812;
      r.botBest = 431;
      r.markerX = 150;
      r.markerY = 120;
      r.save();
      Records back = Records.load(file);
      assert back.best == 812 && back.botBest == 431 && back.markerX == 150 && back.markerY == 120;
      assert back.generations == 0 && back.champion == null : "no evolution saved yet";
      r.generations = 12;
      r.evolvedBest = 1500;
      r.champion = Brain.original();
      r.chart.add(700);
      r.chart.add(1500);
      r.save();
      back = Records.load(file);
      assert back.generations == 12 && back.evolvedBest == 1500 && back.champion.encode().equals(Brain.original().encode())
         && back.chart.equals(java.util.Arrays.asList(700, 1500)) && back.best == 812;
      assert back.sound : "sound starts on";
      back.sound = false;
      back.save();
      assert !Records.load(file).sound && Records.load(file).best == 812 : "sound=off is kept";
      try (FileOutputStream out = new FileOutputStream(file)) {
         out.write("best=oops\nbot=7\nmarker=1\n".getBytes(StandardCharsets.UTF_8));
      }
      Records broken = Records.load(file);
      assert broken.sound && broken.best == 0 && broken.botBest == 7 && broken.markerX == PixelBot.DEFAULT_X;
      file.delete();

      System.out.println("RunnerCheck passed (bot average " + total / 6 + ", evolved record " + ev.championScore() + ")");
      System.exit(0);
   }

   private static int maxOf(List<Integer> list) {
      int max = 0;
      for (int n : list) {
         max = Math.max(max, n);
      }
      return max;
   }

   private static int peak(Runner r) {
      int top = r.dinoTop();
      while (r.isJumping()) {
         r.step();
         top = Math.min(top, r.dinoTop());
      }
      return Runner.GROUND - Runner.DINO_H - top;
   }

   private static RunnerPanel panel(long seed, int[] marker) throws Exception {
      File file = File.createTempFile("dinogame", ".txt");
      file.delete();
      file.deleteOnExit();
      Records r = Records.load(file);
      if (marker != null) {
         r.markerX = marker[0];
         r.markerY = marker[1];
      }
      return new RunnerPanel(r, seed);
   }

   private static void run(RunnerPanel p) {
      for (int i = 0; i < 60 * 600 && p.runner().state() != Runner.State.CRASHED; i++) {
         p.step();
      }
      assert p.runner().state() == Runner.State.CRASHED : "the run ends";
   }

   private static int botRun(long seed, int[] marker) throws Exception {
      RunnerPanel p = panel(seed, marker);
      p.setBot(true);
      run(p);
      return p.runner().score();
   }

   private static int p2BotBest(long seed) throws Exception {
      File file = File.createTempFile("dinogame", ".txt");
      file.delete();
      RunnerPanel p = new RunnerPanel(Records.load(file), seed);
      p.setBot(true);
      run(p);
      int best = Records.load(file).botBest;
      file.delete();
      return best;
   }
}
