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
      // The night has run 1.5 s of its clock by now, so the bar is full to about 576 px.
      int full = draw(panel).getRGB(570, 16);
      check(isBlue(full), "a full clock bar is blue at its end " + Integer.toHexString(full));
      panel.catchAt(hole.x, hole.y + HUD);
      check(panel.game.secondsLeft() < Night.SECONDS, "the hole cost time");
      panel.advance(0.01);
      int lost = draw(panel).getRGB(570, 16);
      check(isRed(lost), "the lost seconds show red on the bar " + Integer.toHexString(lost));
      panel.advance(CatchPanel.DRAIN);
      int after = draw(panel).getRGB(570, 16);
      check(!isRed(after) && !isBlue(after), "then drain away " + Integer.toHexString(after));
      paused(panel, CatchPanel.WIDTH, CatchPanel.HEIGHT);
      upright(none);
      System.out.println("CatchFeelCheck passed");
   }

   /** On a phone the game stands upright at the screen's width: everything fits, and a tap still catches. */
   private static void upright(File none) {
      java.awt.Insets frame = new java.awt.Insets(30, 4, 4, 4);
      check(CatchPanel.sizeFor(new java.awt.Dimension(1280, 720), frame).equals(new java.awt.Dimension(640, 480)), "a wide screen keeps 640 x 480");
      check(CatchPanel.sizeFor(new java.awt.Dimension(400, 860), frame).equals(new java.awt.Dimension(392, 760)), "a phone gets its width, up to 760 tall");
      check(CatchPanel.sizeFor(new java.awt.Dimension(360, 560), frame).height == CatchPanel.NARROW_MIN_HEIGHT, "never too short for the cards");
      java.awt.Dimension size = CatchPanel.sizeFor(new java.awt.Dimension(368, 700), frame);
      CatchPanel panel = new CatchPanel(NightRecords.load(none), size);
      panel.stop();
      panel.random.setSeed(229);
      panel.setSize(size);
      draw(panel);
      java.awt.Rectangle field = new java.awt.Rectangle(size);
      for (int i = 0; i < panel.nightCards.length; i++) {
         check(field.contains(panel.nightCards[i]), "night card " + i + " fits " + panel.nightCards[i]);
         for (int j = 0; j < i; j++) {
            check(!panel.nightCards[i].intersects(panel.nightCards[j]), "night cards " + i + " and " + j + " apart");
         }
      }
      check(panel.nightCards[1].y == panel.nightCards[0].y && panel.nightCards[2].y > panel.nightCards[0].y, "two cards to a row");
      panel.start(Night.SUPERNOVA);
      panel.advance(1.5);
      check(panel.slotX(Night.SUPERNOVA.goal() - 1) + 7 < size.width - 60, "fifteen places in the count leave room for the count's number");
      panel.start(Night.DRIFT);
      panel.advance(1.5);
      CatchGame.Body gold = firstGold(panel.game);
      check(gold.x < size.width && gold.y + panel.countY() < size.height, "the stars stay on the upright field");
      panel.catchAt(gold.x, gold.y + 62);
      check(panel.game.caught() == 1, "a tap on a star in the upright field catches it");
      panel.advance(Night.SECONDS + 1);
      draw(panel);
      for (java.awt.Rectangle button : new java.awt.Rectangle[]{panel.againButton, panel.menuButton}) {
         check(button.width >= 90 && field.contains(button), "result button fits " + button);
      }
      check(!panel.againButton.intersects(panel.menuButton), "result buttons apart");
      paused(panel, size.width, size.height);
   }

   /** The pause button stops the clock and the stars; the card's buttons fit and resume, restart or go back to the nights, by tap alone. */
   private static void paused(CatchPanel panel, int width, int height) {
      java.awt.Rectangle field = new java.awt.Rectangle(width, height);
      java.awt.Rectangle pause = panel.pauseButton;
      check(pause.width >= 40 && pause.height >= 40 && field.contains(pause), "the pause button is big enough and on the field " + pause);
      panel.start(Night.CONFETTI);
      panel.advance(1.5);
      double clock = panel.game.secondsLeft();
      CatchGame.Body star = firstGold(panel.game);
      double starX = star.x;
      tap(panel, pause);
      panel.advance(5);
      check(panel.game.secondsLeft() == clock && star.x == starX, "paused, the clock and the stars hold still");
      draw(panel);
      java.awt.Rectangle[] buttons = {panel.resumeButton, panel.againButton, panel.menuButton};
      for (int i = 0; i < buttons.length; i++) {
         check(buttons[i].width >= 70 && field.contains(buttons[i]), "paused card button fits " + buttons[i]);
         for (int j = 0; j < i; j++) {
            check(!buttons[i].intersects(buttons[j]), "paused card buttons apart");
         }
      }
      tap(panel, panel.resumeButton);
      panel.advance(1);
      check(Math.abs(panel.game.secondsLeft() - (clock - 1)) < 1e-9, "Resume runs the clock again from where it stopped");
      tap(panel, pause);
      draw(panel);
      tap(panel, pause);
      check(panel.game.secondsLeft() > 0 && advanced(panel), "the pause button resumes too");
      tap(panel, pause);
      draw(panel);
      tap(panel, panel.againButton);
      check(panel.game.secondsLeft() == Night.SECONDS && panel.game.caught() == 0, "Again starts the night over");
      tap(panel, pause);
      draw(panel);
      tap(panel, panel.menuButton);
      CatchGame before = panel.game;
      tap(panel, panel.nightCards[0]);
      check(panel.game != before && panel.game.night() == Night.DRIFT, "Nights goes back to the menu, whose cards start a night");
   }

   private static boolean advanced(CatchPanel panel) {
      double clock = panel.game.secondsLeft();
      panel.advance(0.5);
      return panel.game.secondsLeft() < clock;
   }

   private static void tap(CatchPanel panel, java.awt.Rectangle target) {
      long now = System.currentTimeMillis();
      int x = (int)target.getCenterX();
      int y = (int)target.getCenterY();
      for (java.awt.event.MouseListener listener : panel.getMouseListeners()) {
         listener.mousePressed(new java.awt.event.MouseEvent(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, now, 0, x, y, 1, false));
      }
   }

   private static BufferedImage draw(CatchPanel panel) {
      BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
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
