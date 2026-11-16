package animate2;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** Self-check for how Star Catcher shows a catch and a black hole: the star flies into the count, the lost seconds drain off the bar. Run with java -ea. */
public class CatchFeelCheck {
   private static final int HUD = 46;

   public static void main(String[] args) {
      File none = new File(System.getProperty("java.io.tmpdir"), "catch-feel-check-none.txt");
      none.delete();
      CatchPanel panel = new CatchPanel(NightRecords.load(none));
      panel.stop(); // the check moves time itself
      panel.random.setSeed(229); // the same stars every run, so the clicks below always find the star or hole they aim at
      panel.setSize(CatchPanel.WIDTH, CatchPanel.HEIGHT);

      // A catch: its place in the count stays dark while the star is in flight, the star is on its path, then the place lights.
      panel.start(Night.DRIFT);
      panel.advance(1.5);
      CatchGame.Body gold = firstGold(panel.game);
      double x = gold.x;
      double y = gold.y + HUD;
      panel.catchAt(x, y);
      check(panel.game.caught() == 1, "caught");
      panel.advance(0.01);
      int slot = panel.slotX(0);
      check(!isGold(draw(panel).getRGB(slot, 23)), "the count's first star is dark while the caught star flies");
      panel.advance(CatchPanel.FLY * 0.5 - 0.01);
      double[] p = panel.flightPoint(x, y, 0, 0.5);
      check(p[1] < y && p[1] > 23, "half way, the star is between where it was caught and the count");
      check(isGold(draw(panel).getRGB((int)p[0], (int)p[1])), "the flying star is drawn on its path");
      double[] end = panel.flightPoint(x, y, 0, 1.0);
      check(Math.abs(end[0] - slot) < 1e-9 && Math.abs(end[1] - 23) < 1e-9, "the flight ends in the count's first place");
      panel.advance(CatchPanel.FLY);
      check(isGold(draw(panel).getRGB(slot, 23)), "the count's first star lights once the star lands");
      panel.advance(CatchPanel.POP);
      check(isGold(draw(panel).getRGB(slot, 23)), "and stays lit");

      // A black hole: the seconds it took stay on the clock bar in red, then drain away.
      panel.start(Night.BURST);
      panel.advance(1.5);
      CatchGame.Body hole = null;
      for (CatchGame.Body body : panel.game.bodies()) {
         if (body.kind == CatchGame.Kind.HOLE) {
            hole = body;
         }
      }
      // The night has run 1.5 s of its clock by now, so the bar is full to about 620 px.
      int full = draw(panel).getRGB(610, 16);
      check(isBlue(full), "a full clock bar is blue at its end " + Integer.toHexString(full));
      panel.catchAt(hole.x, hole.y + HUD);
      check(panel.game.secondsLeft() < Night.SECONDS, "the hole cost time");
      panel.advance(0.01);
      int lost = draw(panel).getRGB(610, 16);
      check(isRed(lost), "the lost seconds show red on the bar " + Integer.toHexString(lost));
      panel.advance(CatchPanel.DRAIN);
      int after = draw(panel).getRGB(610, 16);
      check(!isRed(after) && !isBlue(after), "then drain away " + Integer.toHexString(after));
      System.out.println("CatchFeelCheck passed");
   }

   private static BufferedImage draw(CatchPanel panel) {
      BufferedImage image = new BufferedImage(CatchPanel.WIDTH, CatchPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      panel.paint(g);
      g.dispose();
      return image;
   }

   private static CatchGame.Body firstGold(CatchGame game) {
      for (CatchGame.Body body : game.bodies()) {
         if (body.kind == CatchGame.Kind.GOLD) {
            return body;
         }
      }
      throw new AssertionError("no gold star");
   }

   private static boolean isGold(int rgb) {
      return (rgb >> 16 & 255) > 200 && (rgb >> 8 & 255) > 140 && (rgb & 255) < 120;
   }

   private static boolean isRed(int rgb) {
      return (rgb >> 16 & 255) > 180 && (rgb >> 8 & 255) < 120 && (rgb & 255) < 140;
   }

   private static boolean isBlue(int rgb) {
      return (rgb >> 16 & 255) < 140 && (rgb & 255) > 200;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
