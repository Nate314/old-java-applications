package dinosaurgameplayer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
      java.awt.Color[] colors = { RunnerPanel.INK, RunnerPanel.PAPER, RunnerPanel.MUTED, RunnerPanel.CLOUD,
         RunnerPanel.DUST, RunnerPanel.STRIP_BG, RunnerPanel.SAND, RunnerPanel.SUN, RunnerPanel.DINO, Land.FAR, Land.SHADE, Land.NEAR,
         RunnerPanel.CACTUS, RunnerPanel.CACTUS_LIGHT, RunnerPanel.BIRD };
      for (java.awt.Color c : colors) {
         for (int i = 1; i <= 1000; i++) {
            assert !ObstacleDetector.isObstacle(Sky.tint(c, i / 1000.0).getRGB() & 0xFFFFFF) : c + " at " + i;
            assert !ObstacleDetector.isObstacle(Sky.mix(Sky.tint(RunnerPanel.PAPER, i / 1000.0), new java.awt.Color(0xE6E6E6), i / 1000.0).getRGB() & 0xFFFFFF);
         }
      }
      // By day the bot sees every obstacle color, and nothing else the game draws.
      for (java.awt.Color c : colors) {
         boolean obstacle = c == RunnerPanel.CACTUS || c == RunnerPanel.CACTUS_LIGHT || c == RunnerPanel.BIRD;
         assert ObstacleDetector.isObstacle(Sky.tint(c, 0).getRGB() & 0xFFFFFF) == obstacle : "by day " + c;
      }
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

      // The ghost: a run of yours plays again, jump for jump, from its course and keys.
      File gfile = File.createTempFile("dinogame", ".txt");
      gfile.delete();
      RunnerPanel gp = new RunnerPanel(Records.load(gfile), 11);
      gp.runner().press();
      runYou(gp);
      int mine = gp.runner().score();
      long crashStep = gp.runner().steps();
      Ghost kept = Records.load(gfile).ghost;
      assert kept != null && kept.score == mine && mine > 150 : "your run is kept as a ghost: " + mine;
      kept.start();
      while (kept.runner().state() == Runner.State.RUNNING) {
         kept.step();
      }
      assert kept.runner().score() == mine && kept.runner().steps() == crashStep : "the ghost crashes where you did";
      assert Ghost.decode(kept.encode()).encode().equals(kept.encode()) && Ghost.decode("1,2,9,3") == null && Ghost.decode("x") == null;
      // T: the whole run in one picture, played again from its keys: same score, its jumps, every row inside the game.
      key(gp, java.awt.event.KeyEvent.VK_T, true);
      RunPicture pic = gp.picture();
      assert pic != null && pic.you.score == mine && pic.ghost == null && pic.you.jumps > 5 && !pic.you.obstacles.isEmpty()
         : "the picture replays your run";
      assert pic.you.steps.size() == crashStep && pic.rowHeight() >= 150 / pic.shrink + 4 : pic.rows + " rows at 1/" + pic.shrink;
      int[] endAt = pic.at(pic.you.end());
      assert endAt[1] == pic.rows - 1 && endAt[0] <= Runner.WIDTH - 10 : "the crash is on the last row";
      BufferedImage shot = new BufferedImage(Runner.WIDTH, RunnerPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D sg = shot.createGraphics();
      gp.setSize(Runner.WIDTH, RunnerPanel.HEIGHT);
      gp.paint(sg);
      sg.dispose();
      int trail = 0;
      for (int y = 0; y < RunnerPanel.STRIP; y++) {
         for (int x = 0; x < Runner.WIDTH; x++) {
            trail += shot.getRGB(x, y) == RunPicture.TRAIL.getRGB() ? 1 : 0;
         }
      }
      assert trail > 300 : "the ribbon is drawn: " + trail;
      key(gp, java.awt.event.KeyEvent.VK_T, true);
      assert gp.picture() == null : "T again goes back";
      gp.setBot(true);
      key(gp, java.awt.event.KeyEvent.VK_T, true);
      assert gp.picture() == null : "no picture while the bot plays";
      gp.setBot(false);
      // The next run is a new course; G races the ghost on its own.
      gp.runner().restart();
      for (int i = 0; i < 200; i++) {
         gp.step();
      }
      assert gp.runner().course() != kept.course : "a new course each run";
      gp.toggleRace();
      assert gp.runner().state() == Runner.State.READY && gp.ghost() == null : "G drops the run under way";
      gp.runner().press();
      gp.step();
      assert gp.ghost() != null && gp.runner().course() == kept.course;
      run(gp);   // no keys this time: an early crash
      assert gp.runner().score() < mine && gp.crashLines()[1].equals("Your ghost runs on to " + mine + ". Watch where it jumps.")
         : gp.crashLines()[1];
      double before = gp.ghost().runner().distance();
      for (int i = 0; i < 30; i++) {
         gp.step();
      }
      assert gp.ghost().runner().distance() > before : "the ghost runs on after you crash";
      assert Records.load(gfile).ghost.score == mine : "a worse run keeps the ghost";
      // Raced, the picture has the ghost too, further along, and building it leaves the ghost where it is.
      double ghostAt = gp.ghost().runner().distance();
      key(gp, java.awt.event.KeyEvent.VK_T, true);
      assert gp.picture() != null && gp.picture().ghost.score == mine && gp.picture().ghost.end() > gp.picture().you.end()
         && gp.picture().you.score == gp.runner().score() && gp.ghost().runner().distance() == ghostAt;
      for (int i = 0; i < RunnerPanel.STRIP; i++) {
         gp.step();
      }
      key(gp, java.awt.event.KeyEvent.VK_SPACE, true);
      key(gp, java.awt.event.KeyEvent.VK_SPACE, false);
      assert gp.picture() == null && gp.runner().state() == Runner.State.RUNNING : "Space goes on: a new run";
      gp.toggleRace();
      gp.runner().press();
      gp.step();
      assert gp.ghost() == null && gp.runner().course() != kept.course : "G again: new courses";
      gfile.delete();

      // Two players (P): one course for both, started together, P2 ahead of P1 and clear of it; the further one wins
      // the round, and duel runs count for nobody's best.
      File dfile = File.createTempFile("dinogame", ".txt");
      dfile.delete();
      Records dr = Records.load(dfile);
      RunnerPanel dp = new RunnerPanel(dr, 11);
      dp.setBot(true);
      dp.togglePlayers();
      Duel duel = dp.duel();
      assert duel != null && !dp.bot().isOn() : "P turns the bot off";
      assert duel.p2.dinoX() >= Runner.DINO_X + Runner.DUCK_W : "P2 stands clear of P1";
      key(dp, java.awt.event.KeyEvent.VK_UP, true);
      assert duel.p1.state() == Runner.State.RUNNING && duel.p2.state() == Runner.State.RUNNING : "one key starts both";
      assert !duel.p1.isJumping() && !duel.p2.isJumping() : "the start is not a jump";
      assert duel.p1.course() == duel.p2.course();
      dp.step();
      key(dp, java.awt.event.KeyEvent.VK_W, true);
      assert duel.p1.isJumping() && !duel.p2.isJumping() : "W jumps P1 only";
      key(dp, java.awt.event.KeyEvent.VK_W, false);
      while (duel.p1.isJumping()) {
         dp.step();
      }
      runYou(dp);   // P1 plays, P2 never jumps
      for (int i = 0; i < 60 * 600 && duel.running(); i++) {
         dp.step();
      }
      assert duel.over() && duel.winner() == 1 && duel.wins(1) == 1 && duel.wins(2) == 0 : duel.p1.score() + " vs " + duel.p2.score();
      assert dp.crashLines()[0].equals("P1 wins, " + duel.p1.score() + " to " + duel.p2.score() + ".") : dp.crashLines()[0];
      assert duel.p2.crashedInto() != null && duel.p2.score() > 0;
      assert dr.best == 0 && dr.ghost == null && !dfile.exists() : "duel runs do not count";
      key(dp, java.awt.event.KeyEvent.VK_UP, true);
      assert duel.over() : "a held key does not start the next round at once";
      for (int i = 0; i < 30; i++) {
         dp.step();
      }
      long lastCourse = duel.p1.course();
      key(dp, java.awt.event.KeyEvent.VK_SPACE, true);
      dp.step();
      assert duel.running() && dp.crashLines().length == 0 && duel.p1.course() != lastCourse : "the next round, a new course";
      dp.togglePlayers();
      assert dp.duel() == null && dp.runner().state() == Runner.State.READY : "P again: one player";
      dp.runner().press();
      assert dp.runner().course() != duel.p2.course() : "back to new courses";
      // Neither jumps: both fall at the first obstacle, P2 first since it stands ahead, and that is a dead heat.
      dp.togglePlayers();
      key(dp, java.awt.event.KeyEvent.VK_SPACE, true);
      for (int i = 0; i < 60 * 60 && dp.duel().running(); i++) {
         dp.step();
      }
      assert dp.duel().over() && dp.duel().winner() == 0 && dp.duel().wins(1) + dp.duel().wins(2) == 0
         && dp.duel().p2.score() < dp.duel().p1.score() && dp.crashLines()[0].startsWith("A dead heat") : dp.crashLines()[0];

      // Play by ear: a player who never looks, only pressing on the beats (and holding as long as each rings), runs far.
      int earTotal = 0;
      int lateTotal = 0;
      for (long seed = 1; seed <= 40; seed++) {
         earTotal += earRun(seed, 0);
         lateTotal += earRun(seed, 2);
      }
      assert earTotal / 40 > 1000 : "ear player average " + earTotal / 40;
      assert lateTotal / 40 > 1000 : "an ear player up to 2 steps late averages " + lateTotal / 40;
      // Each cactus and low bird gets a low warning then the beat to act on, 14 or 15 steps later; a middle bird two high
      // ones; a high bird none. Every beat to act on comes before the obstacle reaches the dinosaur.
      Runner er = new Runner(25);
      er.press();
      java.util.Map<Runner.Obstacle, Long> warned = new java.util.HashMap<>();
      java.util.Set<String> heard = new java.util.HashSet<>();
      for (int i = 0; i < 60 * 60 && er.state() == Runner.State.RUNNING; i++) {
         er.step();
         for (Runner.Obstacle o : er.obstacles()) {
            assert o.lift != Runner.BIRD_HEIGHTS[2] || o.beats == 0 : "a high bird is silent";
            assert o.x > Runner.DINO_X + Runner.DINO_W || Ears.need(o) == 0 || o.beats == 2 : "beats come before the obstacle";
         }
         for (int[] b : Ears.beats(er)) {
            Runner.Obstacle o = null;
            for (Runner.Obstacle c : er.obstacles()) {
               boolean fresh = b[1] == 0 ? c.beats == 1 && !warned.containsKey(c) : c.beats == 2 && warned.containsKey(c) && warned.get(c) >= 0;
               if (fresh && Ears.need(c) == b[0]) {
                  o = c;
                  break;
               }
            }
            assert o != null : "a beat names an obstacle";
            assert b[0] == (o.kind == Runner.Kind.BIRD && o.lift == Runner.BIRD_HEIGHTS[1] ? Ears.DUCK : Ears.JUMP);
            if (b[1] == 0) {
               warned.put(o, er.steps());
            } else {
               assert Math.abs(er.steps() - warned.get(o) - Ears.BEAT + 0.5) <= 0.5 : "the beat to act on is " + (er.steps() - warned.get(o)) + " steps after the warning";
               assert b[1] > 0 && b[1] < 40 : "hold " + b[1];
               warned.put(o, -1L);
               heard.add(o.kind + (o.kind == Runner.Kind.BIRD ? " " + o.lift : ""));
            }
         }
         // A seeing player keeps this run going, so the course reaches the birds.
         Runner.Obstacle o = er.at(Runner.DINO_X);
         if (o != null && !er.isJumping() && Ears.need(o) == Ears.JUMP && o.x - Runner.DINO_X - Runner.DINO_W < er.speed() * 5) {
            er.press();
         } else if (er.isJumping()) {
            er.release();
         }
         er.duck(o != null && Ears.need(o) == Ears.DUCK && o.x - Runner.DINO_X - Runner.DINO_W < 60);
      }
      assert heard.contains("SMALL_CACTUS") && heard.contains("LARGE_CACTUS") : "heard " + heard;

      // L goes from off to beats to the blindfold and back, and is saved; the blindfold hides the obstacles on screen
      // during a run (the bot's frame still has them), and the crash shows them again.
      File earFile = File.createTempFile("dinogame", ".txt");
      earFile.delete();
      RunnerPanel ep = new RunnerPanel(Records.load(earFile), 3);
      key(ep, java.awt.event.KeyEvent.VK_L, true);
      assert Records.load(earFile).ears == Ears.Mode.CUES;
      key(ep, java.awt.event.KeyEvent.VK_L, true);
      assert Records.load(earFile).ears == Ears.Mode.BLINDFOLD && !ep.blindfolded() : "the blindfold goes on with the run";
      ep.setSize(Runner.WIDTH, RunnerPanel.HEIGHT);
      key(ep, java.awt.event.KeyEvent.VK_SPACE, true);
      key(ep, java.awt.event.KeyEvent.VK_SPACE, false);
      while (ep.runner().at(Runner.DINO_X + Runner.DINO_W) == null || ep.runner().at(Runner.DINO_X + Runner.DINO_W).x > 400) {
         ep.step();
      }
      assert ep.blindfolded() && inkInField(ep) == 0 : "blindfolded, no obstacle drawn";
      ep.setBot(true);
      ep.step();
      assert inkInField(ep) == 0 : "the screen stays blindfolded while the bot plays";
      run(ep);
      assert ep.runner().score() > 100 : "the bot still sees through the blindfold: " + ep.runner().score();
      assert !ep.blindfolded() && inkInField(ep) > 0 : "the crash shows what was hit";
      key(ep, java.awt.event.KeyEvent.VK_L, true);
      assert Records.load(earFile).ears == Ears.Mode.OFF && !Files.readString(earFile.toPath()).contains("ears=") : "off is not written";
      earFile.delete();

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

      coachChecks();
      guideChecks();
      padChecks();
      landChecks();
      System.out.println("RunnerCheck passed (bot average " + total / 6 + ", evolved record " + ev.championScore()
         + ", ear player average " + earTotal / 40 + ", " + lateTotal / 40 + " when up to 2 steps late)");
      System.exit(0);
   }

   /**
    * A run played by ear alone, from seed: on each beat to act on, jump 1 to late + 1 steps after it and hold as long as
    * the beat rings, or duck at once for as long. Stops at 3,000.
    */
   /** The coach after a crash: its fix is checked by playing it with the keys, and its lines and marks. */
   private static void coachChecks() throws Exception {
      assert Coach.seconds(6).equals("0.10 s") && Coach.seconds(1).equals("0.05 s") && Coach.seconds(60).equals("1.00 s");
      int late = 0;
      int early = 0;
      int ducks = 0;
      for (long seed = 1; seed <= 30; seed++) {
         // No key after the start: the first cactus ends it, and the coach says when to jump.
         Runner r = new Runner(seed);
         r.press();
         r.release();
         stepToCrash(r);
         int crash = (int) r.steps();
         Coach c = Coach.of(r);
         assert c != null && c.fix == Coach.Fix.JUMP && c.line.startsWith("Jump when it reaches the mark") && c.pressX == -1
            : "seed " + seed + ": " + (c == null ? null : c.line);
         assert c.markX > r.crashedInto().x && c.markX < Runner.WIDTH && c.step < crash;
         assert passes(seed, c) : "seed " + seed + ": the coach's jump clears it";
         // Pressed a moment too late or too soon: it says which, and how much.
         for (int at : new int[] { crash - 2, crash - 38 }) {
            Runner p = new Runner(seed);
            p.press();
            p.release();
            while (p.state() == Runner.State.RUNNING) {
               if (p.steps() == at) {
                  p.press();
               } else if (p.steps() == at + 1) {
                  p.release();
               }
               p.step();
            }
            Coach pc = Coach.of(p);
            if (pc != null && pc.line.contains(" late: ")) {
               late++;
               assert pc.step < at && pc.pressX >= 0 : pc.line;
            } else if (pc != null && pc.line.contains(" early: ")) {
               early++;
               assert pc.step > at : pc.line;
            }
         }
      }
      for (long seed = 1; seed <= 80; seed++) {
         // Never ducking or jumping a bird: a middle bird's coach says duck, and the duck clears it.
         Runner b = new Runner(seed);
         b.press();
         b.release();
         int held = 0;
         while (b.state() == Runner.State.RUNNING) {
            Runner.Obstacle o = b.at(Runner.DINO_X);
            double ahead = o == null ? 999 : o.x - Runner.DINO_X - Runner.DINO_W;
            if (o != null && o.kind != Runner.Kind.BIRD && !b.isJumping() && ahead < b.speed() * 5 && ahead > 0) {
               b.press();
               held = 14;
            } else if (held > 0 && --held == 0) {
               b.release();
            }
            b.step();
         }
         if (b.crashedInto() != null && b.crashedInto().lift == Runner.BIRD_HEIGHTS[1]) {
            Coach bc = Coach.of(b);
            assert bc != null && bc.fix == Coach.Fix.DUCK && bc.line.startsWith("Duck under that bird") : bc == null ? null : bc.line;
            assert passes(seed, b, bc);
            ducks++;
         }
      }
      assert late >= 10 && early >= 5 && ducks >= 3 : "late " + late + ", early " + early + ", ducks " + ducks;

      // In the window: the line joins the score, the mark is drawn under the ground, and the bot's crashes get none.
      RunnerPanel p = panel(11, null);
      p.runner().press();
      p.runner().release();
      run(p);
      Coach c = p.coach();
      assert c != null && p.crashLines()[0].endsWith(" " + p.runner().score() + ". " + c.line) : p.crashLines()[0];
      BufferedImage shot = new BufferedImage(Runner.WIDTH, RunnerPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D sg = shot.createGraphics();
      p.setSize(Runner.WIDTH, RunnerPanel.HEIGHT);
      for (int i = 0; i < 20; i++) {
         p.step();   // past the crash shake
      }
      p.paint(sg);
      sg.dispose();
      assert shot.getRGB(c.markX, Runner.GROUND + 9) == RunnerPanel.INK.getRGB() : "the mark under the ground";
      // The slow replay: past the shake the fix's run plays at half speed, its mark hollow until the fix presses and lit
      // from then on, the fix gets past what you hit, and the replay starts over. A touch screen's line names the pad.
      assert p.replayView() == null : "no replay during the shake";
      Runner.Obstacle hit = p.runner().crashedInto();
      double clear = p.runner().distance() + hit.x + hit.w - Runner.DINO_X;
      boolean hollow = false;
      boolean lit = false;
      boolean cleared = false;
      boolean looped = false;
      int frames = 0;
      long last = -1;
      for (int i = 0; i < 1200 && !looped; i++) {
         p.step();
         Runner v = p.replayView();
         if (v == null) {
            continue;
         }
         looped = last > v.steps();
         last = v.steps();
         frames++;
         cleared |= v.state() == Runner.State.RUNNING && v.distance() >= clear;
         Graphics2D rg = shot.createGraphics();
         p.paint(rg);
         rg.dispose();
         boolean filled = shot.getRGB(c.markX, Runner.GROUND + 9) == RunnerPanel.INK.getRGB();
         if (v.steps() < c.step) {
            hollow |= !filled;
         } else {
            lit |= filled;
         }
      }
      assert hollow && lit && cleared && looped : "replay: hollow " + hollow + ", lit " + lit + ", cleared " + cleared + ", looped " + looped;
      assert Coach.of(p.runner(), true).line.endsWith("tap JUMP.") : Coach.of(p.runner(), true).line;
      key(p, java.awt.event.KeyEvent.VK_SPACE, true);
      key(p, java.awt.event.KeyEvent.VK_SPACE, false);
      assert p.runner().state() == Runner.State.RUNNING && p.replayView() == null : "Space runs again during the replay";
      stepToCrash(p.runner());
      System.out.println("replay: " + frames + " steps a loop, the fix clears it");
      p.setBot(true);
      for (int i = 0; i < 600 && p.runner().state() != Runner.State.RUNNING; i++) {
         p.step();
      }
      run(p);
      assert p.coach() == null : "no coach for the bot";

      // Fast enough to run at a crash: a long run of good play, coached.
      RunnerPanel y = panel(3, null);
      y.runner().press();
      runYou(y);
      long t0 = System.nanoTime();
      Coach.of(y.runner());
      long ms = (System.nanoTime() - t0) / 1000000;
      System.out.println("coach: late " + late + ", early " + early + ", ducks " + ducks + "; after a "
         + y.runner().steps() + "-step run in " + ms + " ms");
   }

   /**
    * The mark that says when to jump or duck: a player who only follows it (tapping when it lights with TAP, holding
    * while it says HOLD, ducking while it says DUCK) gets well past the first cacti; it is drawn filled when lit; it goes once your best reaches 700,
    * and never shows for the bot.
    */
   private static void guideChecks() throws Exception {
      int reached = 0;
      int total = 0;
      for (long seed = 1; seed <= 10; seed++) {
         RunnerPanel p = panel(seed, null);
         Runner r = p.runner();
         assert p.guiding() && p.guide()[1] == 0 : "the ready screen shows the mark, unlit";
         r.press();
         r.release();
         boolean lit = false;
         boolean wasLit = false;
         long pressed = 0;
         BufferedImage shot = new BufferedImage(Runner.WIDTH, RunnerPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
         p.setSize(Runner.WIDTH, RunnerPanel.HEIGHT);
         while (r.state() == Runner.State.RUNNING && r.score() < RunnerPanel.GUIDE_UNTIL) {
            // As the screen says: tap when it lights with TAP, hold while it says HOLD, duck while it says DUCK.
            int[] g = p.guide();
            boolean jump = g[1] == 1 && g[0] == Ears.JUMP;
            if (jump && !r.isJumping() && !wasLit) {
               r.press();
               pressed = r.steps();
            } else if (!jump || g[2] == 0 && r.steps() - pressed >= 2) {
               r.release();   // a quick tap
            }
            wasLit = g[1] == 1;
            r.duck(g[1] == 1 && g[0] == Ears.DUCK);
            if (g[1] == 1 && !lit) {
               Graphics2D sg = shot.createGraphics();
               p.paint(sg);
               sg.dispose();
               assert shot.getRGB(RunnerPanel.guideX(r, g[0]), Runner.GROUND + 9) == RunnerPanel.INK.getRGB() : "the lit mark is filled";
               lit = true;
            }
            p.step();
         }
         total += r.score();
         reached += r.score() >= RunnerPanel.GUIDE_UNTIL ? 1 : 0;
         assert lit && r.score() > 150 : "seed " + seed + ": following the mark scored " + r.score();
      }
      assert reached >= 8 : "following the mark reached " + RunnerPanel.GUIDE_UNTIL + " on " + reached + " of 10";
      RunnerPanel fresh = panel(1, null);
      fresh.setBot(true);
      assert !fresh.guiding() : "never for the bot";
      fresh.setBot(false);
      assert fresh.guiding();
      File file = File.createTempFile("dinogame", ".txt");
      file.delete();
      file.deleteOnExit();
      Records past = Records.load(file);
      past.best = RunnerPanel.GUIDE_UNTIL;
      assert !new RunnerPanel(past, 1).guiding() : "gone once your best reaches the first night";
      System.out.println("guide: following the mark reached " + RunnerPanel.GUIDE_UNTIL + " on " + reached + " of 10 courses, average "
         + total / 10);
   }

   /**
    * The thumb pads (a phone held upright): only when asked for, under the game; a press on RUN starts the run, DUCK ducks
    * until let go, JUMP jumps, the lit mark fills its pad, a crash's pad says RUN AGAIN, and in two players they are P1 and P2.
    */
   /** The land behind a run: drawn, never ink, and the mesas slide an eighth as far as the ground. */
   private static void landChecks() {
      BufferedImage a = landAt(0);
      BufferedImage b = landAt(800);
      int far = 0;
      int near = 0;
      int same = 0;
      int row = Runner.GROUND - 30;
      for (int x = 0; x < Runner.WIDTH; x++) {
         for (int y = 0; y < Runner.GROUND; y++) {
            int c = a.getRGB(x, y);
            assert !ObstacleDetector.isObstacle(c & 0xFFFFFF) : "land the bot would see at " + x + "," + y;
            far += c == Land.FAR.getRGB() ? 1 : 0;
            near += c == Land.NEAR.getRGB() ? 1 : 0;
         }
         if (x >= 100) {
            same += a.getRGB(x, row) == b.getRGB(x - 100, row) ? 1 : 0;
         }
      }
      assert far > 2000 && near > 2000 : "mesas " + far + ", dunes " + near;
      assert same == Runner.WIDTH - 100 : "800 along the ground is 100 for the mesas, " + same;
   }

   private static BufferedImage landAt(double distance) {
      BufferedImage img = new BufferedImage(Runner.WIDTH, RunnerPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      g.setColor(RunnerPanel.PAPER);
      g.fillRect(0, 0, Runner.WIDTH, RunnerPanel.HEIGHT);
      Land.paint(g, distance, 0);
      g.dispose();
      return img;
   }

   private static void padChecks() throws Exception {
      assert panel(1, null).getPreferredSize().height == RunnerPanel.HEIGHT : "no pads unless asked for";
      File file = File.createTempFile("dinogame", ".txt");
      file.delete();
      file.deleteOnExit();
      RunnerPanel p = new RunnerPanel(Records.load(file), 1, Sounds.silent(false), true);
      int height = RunnerPanel.HEIGHT + RunnerPanel.PADS;
      assert p.getPreferredSize().height == height;
      p.setSize(Runner.WIDTH, height);
      Runner r = p.runner();
      int padY = RunnerPanel.HEIGHT + RunnerPanel.PADS / 2;
      int duckX = Runner.WIDTH / 4;
      int jumpX = Runner.WIDTH * 3 / 4;
      mouse(p, jumpX, padY, true);
      assert r.state() == Runner.State.RUNNING : "RUN starts the run";
      mouse(p, jumpX, padY, false);
      while (r.isJumping()) {
         p.step();
      }
      mouse(p, duckX, padY, true);
      p.step();
      assert r.isDucking() : "DUCK ducks while held";
      mouse(p, duckX, padY, false);
      p.step();
      assert !r.isDucking() : "and lets go";
      mouse(p, jumpX, padY, true);
      assert r.isJumping() : "JUMP jumps";
      mouse(p, jumpX, padY, false);
      BufferedImage shot = new BufferedImage(Runner.WIDTH, height, BufferedImage.TYPE_INT_RGB);
      // The lit mark fills the pad it asks for (sampled at the pad's top left, inside its border, clear of its words).
      boolean litPad = false;
      for (int i = 0; i < 600 && r.state() == Runner.State.RUNNING && !litPad; i++) {
         int[] g = p.guide();
         if (g[1] == 1 && !r.isJumping()) {
            Graphics2D sg = shot.createGraphics();
            p.paint(sg);
            sg.dispose();
            int x = g[0] == Ears.DUCK ? 30 : Runner.WIDTH / 2 + 24;
            litPad = shot.getRGB(x, RunnerPanel.HEIGHT + 20) == RunnerPanel.INK.getRGB()
               && shot.getRGB(g[0] == Ears.DUCK ? Runner.WIDTH / 2 + 24 : 30, RunnerPanel.HEIGHT + 20) == RunnerPanel.PAPER.getRGB();
         }
         p.step();
      }
      assert litPad : "the lit mark fills its pad, the other stays paper";
      stepToCrash(r);
      for (int i = 0; i < 30; i++) {
         p.step();
      }
      mouse(p, jumpX, padY, true);
      mouse(p, jumpX, padY, false);
      assert r.state() == Runner.State.RUNNING : "RUN AGAIN runs again";
      p.togglePlayers();
      mouse(p, duckX, padY, true);
      mouse(p, duckX, padY, false);
      assert p.duel().running() : "either pad starts both players";
      while (p.duel().p1.isJumping() || p.duel().p2.isJumping()) {
         p.step();
      }
      mouse(p, duckX, padY, true);
      assert p.duel().p1.isJumping() && !p.duel().p2.isJumping() : "the left pad is P1's jump";
      mouse(p, duckX, padY, false);
      System.out.println("pads: run, duck, jump, lit mark, run again and two players");
   }

   private static void mouse(RunnerPanel p, int x, int y, boolean down) {
      java.awt.event.MouseEvent e = new java.awt.event.MouseEvent(p, down ? java.awt.event.MouseEvent.MOUSE_PRESSED
         : java.awt.event.MouseEvent.MOUSE_RELEASED, 0, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1);
      for (java.awt.event.MouseListener l : p.getMouseListeners()) {
         if (down) {
            l.mousePressed(e);
         } else {
            l.mouseReleased(e);
         }
      }
   }

   private static void stepToCrash(Runner r) {
      while (r.state() == Runner.State.RUNNING) {
         r.step();
      }
   }

   /** Plays the start-only run of seed with the coach's jump: it gets past the cactus that ended it. */
   private static boolean passes(long seed, Coach c) {
      Runner r = new Runner(seed);
      r.press();
      r.release();
      Runner first = new Runner(seed);
      first.press();
      first.release();
      stepToCrash(first);
      double clear = first.distance() + first.crashedInto().x + first.crashedInto().w - Runner.DINO_X;
      while (r.state() == Runner.State.RUNNING && r.distance() < clear) {
         if (r.steps() == c.step) {
            r.press();
         } else if (r.steps() == c.step + c.hold) {
            r.release();
         }
         r.step();
      }
      return r.state() == Runner.State.RUNNING;
   }

   /** Plays crashed's keys up to the coach's duck, then ducks: it gets past the bird. */
   private static boolean passes(long seed, Runner crashed, Coach c) {
      double clear = crashed.distance() + crashed.crashedInto().x + crashed.crashedInto().w - Runner.DINO_X;
      List<Integer> keys = new ArrayList<>();
      for (int in : crashed.inputs()) {
         if (in / 4 < c.step) {
            keys.add(in);
         }
      }
      keys.add(c.step * 4 + Runner.RELEASE);
      keys.add(c.step * 4 + Runner.DUCK);
      Ghost g = new Ghost(crashed.course(), 0, keys.stream().mapToInt(Integer::intValue).toArray());
      g.start();
      while (g.runner().state() == Runner.State.RUNNING && g.runner().distance() < clear) {
         g.step();
      }
      return g.runner().state() == Runner.State.RUNNING;
   }

   private static int earRun(long seed, int late) {
      Runner r = new Runner(seed);
      r.press();
      r.release();
      java.util.Random lateness = new java.util.Random(seed);
      int hold = 0;
      int duck = 0;
      int wait = 0;
      int pendingHold = 0;
      while (r.state() == Runner.State.RUNNING && r.score() < 3000) {
         r.step();
         if (wait > 0 && --wait == 0) {
            r.duck(false);
            duck = 0;
            r.press();
            hold = pendingHold;
         }
         if (hold > 0 && --hold == 0) {
            r.release();
         }
         if (duck > 0 && --duck == 0) {
            r.duck(false);
         }
         for (int[] b : Ears.beats(r)) {
            if (b[1] == 0) {
               continue;
            }
            if (b[0] == Ears.DUCK) {
               r.duck(true);
               duck = b[1];
            } else {
               wait = 1 + lateness.nextInt(late + 1);
               pendingHold = b[1];
            }
         }
      }
      return r.score();
   }

   /** Obstacle-colored (blue 83) pixels the panel draws on screen ahead of the dinosaur, between the score and the ground. */
   private static int inkInField(RunnerPanel p) {
      BufferedImage img = new BufferedImage(Runner.WIDTH, RunnerPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      p.paint(g);
      g.dispose();
      int n = 0;
      for (int y = 50; y < Runner.GROUND; y++) {
         for (int x = Runner.DINO_X + Runner.DUCK_W + 8; x < Runner.WIDTH; x++) {
            n += ObstacleDetector.isObstacle(img.getRGB(x, y) & 0xFFFFFF) ? 1 : 0;
         }
      }
      return n;
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

   /** Plays like a person would, roughly: jumps cacti and low birds with held or tapped keys, ducks the middle birds. */
   private static void runYou(RunnerPanel p) {
      Runner r = p.runner();
      int held = 0;
      for (int i = 0; i < 60 * 600 && r.state() == Runner.State.RUNNING; i++) {
         Runner.Obstacle o = r.at(Runner.DINO_X);
         double ahead = o == null ? 999 : o.x - Runner.DINO_X - Runner.DINO_W;
         boolean duck = o != null && o.kind == Runner.Kind.BIRD && o.lift == Runner.BIRD_HEIGHTS[1] && ahead < 60;
         r.duck(duck);
         if (!duck && !r.isJumping() && o != null && o.lift < Runner.BIRD_HEIGHTS[1] && ahead < r.speed() * 5 && ahead > 0) {
            r.press();
            held = o.kind == Runner.Kind.LARGE_CACTUS ? 14 : 3;
         } else if (held > 0 && --held == 0) {
            r.release();
         }
         p.step();
      }
   }

   private static void key(RunnerPanel p, int code, boolean down) {
      java.awt.event.KeyEvent e = new java.awt.event.KeyEvent(p, down ? java.awt.event.KeyEvent.KEY_PRESSED : java.awt.event.KeyEvent.KEY_RELEASED,
         0, 0, code, java.awt.event.KeyEvent.CHAR_UNDEFINED);
      for (java.awt.event.KeyListener l : p.getKeyListeners()) {
         if (down) {
            l.keyPressed(e);
         } else {
            l.keyReleased(e);
         }
      }
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
