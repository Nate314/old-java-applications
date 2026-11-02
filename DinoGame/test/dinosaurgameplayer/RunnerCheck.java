package dinosaurgameplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

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

      // The file keeps both bests and the marker; damaged lines are skipped.
      Records r = Records.load(file);
      r.best = 812;
      r.botBest = 431;
      r.markerX = 150;
      r.markerY = 120;
      r.save();
      Records back = Records.load(file);
      assert back.best == 812 && back.botBest == 431 && back.markerX == 150 && back.markerY == 120;
      try (FileOutputStream out = new FileOutputStream(file)) {
         out.write("best=oops\nbot=7\nmarker=1\n".getBytes(StandardCharsets.UTF_8));
      }
      Records broken = Records.load(file);
      assert broken.best == 0 && broken.botBest == 7 && broken.markerX == PixelBot.DEFAULT_X;
      file.delete();

      System.out.println("RunnerCheck passed (bot average " + total / 6 + ")");
      System.exit(0);
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
