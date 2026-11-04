package animate2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Random;

/** Self-check for Star Catcher's nights, catching, black holes, the clock, combos and saved records. Run with java -ea. */
public class CatchCheck {
   private static final double W = 640;
   private static final double H = 434;

   public static void main(String[] args) throws IOException {
      check(Night.starsFor(10.0) == 3 && Night.starsFor(9.9) == 2 && Night.starsFor(5.0) == 2 && Night.starsFor(0.1) == 1, "stars");
      for (Night night : Night.values()) {
         CatchGame game = new CatchGame(night, W, H, new Random(night.ordinal()));
         long gold = game.bodies().stream().filter(b -> b.kind == CatchGame.Kind.GOLD).count();
         long holes = game.bodies().stream().filter(b -> b.kind == CatchGame.Kind.HOLE).count();
         check(gold == night.gold() && holes == night.holes(), night + " bodies");
         // Bodies stay inside the field for a whole night (the clock runs out after 30 s).
         for (int i = 0; i < 3000 && game.state() == CatchGame.State.PLAYING; i++) {
            game.step(0.015);
            for (CatchGame.Body body : game.bodies()) {
               double r = body.radius();
               check(body.x >= -r && body.x <= W + r && body.y >= -r && body.y <= H + r, night + " stays inside");
            }
         }
         check(game.state() == CatchGame.State.LOST && game.secondsLeft() == 0.0 && game.stars() == 0, night + " clock runs out");
         check(game.bodies().size() == night.gold() + night.holes(), night + " keeps its bodies");
         check(night.life() == 0 || game.twinkledOut() > 0, night + " stars twinkle out");
         check(night.life() > 0 || game.twinkledOut() == 0, night + " stars stay");
         check(night.pull() > 0 || game.swallowed() == 0, night + " no pull");
      }

      // Catching every gold star at once wins with three stars, and a combo adds time.
      Night night = Night.BURST;
      CatchGame game = new CatchGame(night, W, H, new Random(7));
      for (int i = 0; i < night.goal(); i++) {
         CatchGame.Body gold = firstGold(game);
         check(game.click(gold.x + 10, gold.y + 10) == CatchGame.Hit.GOLD, "catch within reach");
         game.step(0.2);
      }
      check(game.state() == CatchGame.State.CAUGHT && game.caught() == night.goal() && game.stars() == 3, "caught");
      double expected = Night.SECONDS - 0.2 * (night.goal() - 1) + CatchGame.COMBO_BONUS * (night.goal() - 2);
      check(Math.abs(game.secondsLeft() - expected) < 1e-9, "combo bonus " + game.secondsLeft() + " vs " + expected);
      check(game.click(0, 0) == CatchGame.Hit.NONE, "no clicks after the night");

      // A miss and a black hole; a combo needs catches close together.
      game = new CatchGame(night, W, H, new Random(8));
      CatchGame.Body hole = firstHole(game);
      CatchGame.Body gold = firstGold(game);
      double far = gold.x + CatchGame.GOLD_RADIUS + CatchGame.CATCH_SLOP + 1;
      if (Math.hypot(hole.x - far, hole.y - gold.y) > CatchGame.HOLE_RADIUS && nearestGold(game, far, gold.y) > CatchGame.GOLD_RADIUS + CatchGame.CATCH_SLOP) {
         check(game.click(far, gold.y) == CatchGame.Hit.MISS, "just out of reach misses");
      }
      check(game.click(hole.x, hole.y) == CatchGame.Hit.HOLE || nearestGold(game, hole.x, hole.y) <= CatchGame.GOLD_RADIUS + CatchGame.CATCH_SLOP, "hole");
      check(Math.abs(game.secondsLeft() - (Night.SECONDS - CatchGame.HOLE_PENALTY)) < 1e-9, "hole costs time");
      for (int i = 0; i < 3; i++) {
         gold = firstGold(game);
         game.click(gold.x, gold.y);
         game.step(CatchGame.COMBO_GAP + 0.1);
      }
      check(game.combo() == 1, "slow catches are no combo");
      check(Math.abs(game.secondsLeft() - (Night.SECONDS - CatchGame.HOLE_PENALTY - 3 * (CatchGame.COMBO_GAP + 0.1))) < 1e-9, "no bonus");
      // Black holes take the clock to zero at most.
      for (int i = 0; i < 20 && game.state() == CatchGame.State.PLAYING; i++) {
         hole = firstHole(game);
         if (nearestGold(game, hole.x, hole.y) > CatchGame.GOLD_RADIUS + CatchGame.CATCH_SLOP) {
            game.click(hole.x, hole.y);
         }
         game.step(0.5);
      }
      check(game.state() == CatchGame.State.LOST && game.secondsLeft() == 0.0, "holes run the clock out");

      // Supernova's black holes swallow stars left alone.
      game = new CatchGame(Night.SUPERNOVA, W, H, new Random(3));
      int swallowed = 0;
      for (int seed = 0; seed < 5; seed++) {
         game = new CatchGame(Night.SUPERNOVA, W, H, new Random(seed));
         while (game.state() == CatchGame.State.PLAYING) {
            game.step(0.015);
         }
         swallowed += game.swallowed();
      }
      check(swallowed > 0, "supernova swallows stars");

      // Records keep the most time left, skip bad lines and open nights in order.
      File file = Files.createTempFile("nights", ".txt").toFile();
      try (Writer writer = new FileWriter(file)) {
         writer.write("DRIFT=3,142\nBURST=2,abc\nNOPE=1,10\nCOMPASS=1\n");
      }
      NightRecords records = NightRecords.load(file);
      check(records.bestTenths(Night.DRIFT) == 142 && records.stars(Night.DRIFT) == 3, "loaded");
      check(!records.isCaught(Night.BURST) && !records.isCaught(Night.COMPASS), "bad lines skipped");
      check(records.isOpen(Night.DRIFT) && records.isOpen(Night.BURST) && !records.isOpen(Night.COMPASS), "open in order");
      check(records.suggested() == Night.BURST && records.totalStars() == 3, "suggested");
      game = winWith(Night.BURST, 30);
      check(records.add(game) && records.stars(Night.BURST) == 2 && records.isOpen(Night.COMPASS), "won burst");
      check(!records.add(winWith(Night.BURST, 35)), "worse run not kept");
      check(records.add(winWith(Night.BURST, 0)) && records.stars(Night.BURST) == 3, "better run kept");
      CatchGame lost = new CatchGame(Night.COMPASS, W, H, new Random(1));
      lost.step(40);
      check(!records.add(lost), "lost night not kept");
      NightRecords again = NightRecords.load(file);
      check(again.bestTenths(Night.BURST) == records.bestTenths(Night.BURST) && again.bestTenths(Night.DRIFT) == 142, "saved");
      String text = new String(Files.readAllBytes(file.toPath()));
      check(text.contains("DRIFT=3,142") && text.contains("BURST=3,"), "file format: " + text);
      file.delete();
      System.out.println("CatchCheck passed");
   }

   /** Catches BURST's stars one by one, waiting {@code tenths} tenths between catches (so a long wait is no combo). */
   private static CatchGame winWith(Night night, int tenths) {
      CatchGame game = new CatchGame(night, W, H, new Random(tenths));
      while (game.state() == CatchGame.State.PLAYING) {
         CatchGame.Body gold = firstGold(game);
         game.click(gold.x, gold.y);
         game.step(tenths / 10.0);
      }
      return game;
   }

   private static CatchGame.Body firstGold(CatchGame game) {
      return game.bodies().stream().filter(b -> b.kind == CatchGame.Kind.GOLD).findFirst().get();
   }

   private static CatchGame.Body firstHole(CatchGame game) {
      return game.bodies().stream().filter(b -> b.kind == CatchGame.Kind.HOLE).findFirst().get();
   }

   private static double nearestGold(CatchGame game, double x, double y) {
      return game.bodies().stream().filter(b -> b.kind == CatchGame.Kind.GOLD).mapToDouble(b -> Math.hypot(b.x - x, b.y - y)).min().getAsDouble();
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
