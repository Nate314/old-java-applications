package animate2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Self-check for Mode 6, Orbits: stars keep their orbits, suns are placed, dragged and taken away. Run with java -ea. */
public class OrbitsCheck {
   private static final int W = 800;
   private static final int H = 600;

   public static void main(String[] args) {
      // One sun in the middle, every star on a circle round it, and the trails on.
      StarField field = field();
      Orbits orbits = field.orbits();
      check(orbits.suns() == 1 && orbits.sunX(0) == W / 2.0 && orbits.sunY(0) == H / 2.0, "Orbits starts with a sun in the middle");
      check(field.trails(), "Orbits turns the trails on");
      check(field.hintVisible(), "Orbits shows its help");
      double[] radius = new double[field.starCount];
      double[] angle = new double[field.starCount];
      for (int star = 0; star < field.starCount; star++) {
         radius[star] = distance(field, star, W / 2.0, H / 2.0);
         angle[star] = Math.atan2(field.centerY(star) - H / 2.0, field.centerX(star) - W / 2.0);
         check(radius[star] < 0.5 * H, "star " + star + " starts within the disk");
      }

      // Five seconds on, the stars have gone round (some way) but stayed at about their distance.
      run(field, 5.0);
      double drift = 0;
      double turned = 0;
      for (int star = 0; star < field.starCount; star++) {
         drift += Math.abs(distance(field, star, W / 2.0, H / 2.0) - radius[star]) / radius[star];
         double now = Math.atan2(field.centerY(star) - H / 2.0, field.centerX(star) - W / 2.0);
         turned += Math.abs(Math.atan2(Math.sin(now - angle[star]), Math.cos(now - angle[star])));
      }
      drift /= field.starCount;
      turned /= field.starCount;
      check(drift < 0.15, "a lone sun's stars keep their orbits (drift " + drift + ")");
      check(turned > 0.5, "the stars go round (" + turned + " radians on average)");
      check(orbits.swallowed() < field.starCount / 20, "few stars fall in on their own (" + orbits.swallowed() + ")");

      // A click places a sun; a hold does not.
      field.startPull(150, 120);
      field.release(152, 121);
      check(orbits.suns() == 2 && orbits.sunX(1) == 152, "a click places a sun where it let go");
      field.startPull(600, 450);
      run(field, 1.0);
      field.release(600, 450);
      check(orbits.suns() == 2, "a long hold flings, it places no sun");
      field.startPull(600, 450);
      field.release(660, 450);
      check(orbits.suns() == 2, "a drag places no sun");

      // Four at most: a fifth takes the place of the oldest.
      field.startPull(650, 120);
      field.release(650, 120);
      field.startPull(150, 480);
      field.release(150, 480);
      check(orbits.suns() == Orbits.MAX_SUNS, "four suns");
      field.startPull(650, 480);
      field.release(650, 480);
      check(orbits.suns() == Orbits.MAX_SUNS && orbits.sunX(0) == 152 && orbits.sunX(3) == 650 && orbits.sunY(3) == 480,
         "a fifth sun replaces the oldest");

      // A second sun bends the paths, and nothing flies off to nowhere.
      run(field, 8.0);
      for (int star = 0; star < field.starCount; star++) {
         double x = field.centerX(star);
         double y = field.centerY(star);
         check(!Double.isNaN(x) && !Double.isNaN(y) && Math.hypot(x - W / 2.0, y - H / 2.0) <= 1.5 * W + 10, "star " + star + " stays near");
      }

      // Dragging a sun moves it; dropping it at the edge takes it away; so does a right click.
      check(orbits.grab(orbits.sunX(0) + 5, orbits.sunY(0)), "a press on a sun grabs it");
      orbits.dragTo(405, 300);
      orbits.drop();
      check(orbits.sunX(0) == 400 && orbits.sunY(0) == 300 && orbits.suns() == 4, "a dragged sun stays where it is let go");
      check(!orbits.grab(400, 200), "no sun to grab in empty sky");
      check(orbits.grab(400, 300), "grab it again");
      orbits.dragTo(W - 3, 300);
      orbits.drop();
      check(orbits.suns() == 3, "a sun let go at the edge is taken away");
      check(orbits.removeAt(orbits.sunX(0), orbits.sunY(0)) && orbits.suns() == 2, "a right click takes a sun away");
      check(!orbits.removeAt(5, 5), "a right click on empty sky takes nothing");

      // A sun dropped on a star swallows it, and the star starts again round a sun; the count stays.
      int before = orbits.swallowed();
      int count = field.starCount;
      orbits.addSun(field.centerX(0), field.centerY(0));
      run(field, 0.05);
      check(orbits.swallowed() > before && field.starCount == count, "a sun swallows a star and it starts again");

      // With no suns left the stars drift, wrapping round the edges.
      while (orbits.suns() > 0) {
         orbits.removeAt(orbits.sunX(0), orbits.sunY(0));
      }
      run(field, 5.0);
      for (int star = 0; star < field.starCount; star++) {
         double x = field.centerX(star);
         double y = field.centerY(star);
         check(x >= 0 && x <= W && y >= 0 && y <= H, "with no suns star " + star + " wraps round the field");
      }

      // T turns trails off and on, in any mode; drawing works both ways, and a sun shows bright on the night sky.
      field.setMode(StarField.ORBITS);
      field.setSize(W, H);
      BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
      for (int frame = 0; frame < 3; frame++) {
         run(field, 0.015);
         paint(field, image);
      }
      check(new Color(image.getRGB(W / 2, H / 2)).getRed() > 240, "the sun is drawn in the middle");
      check(new Color(image.getRGB(3, 3)).getBlue() < 60, "the Orbits sky is dark");
      field.toggleTrails();
      check(!field.trails(), "T turns the trails off");
      paint(field, image);
      field.setMode(0);
      check(!field.trails(), "other modes keep the trails as set");
      paint(field, image);
      check(countColor(image, Color.blue) > W * H / 10, "Drift is drawn on its own blue");
      field.toggleTrails();
      check(field.trails(), "T turns them on in Drift too");
      paint(field, image);

      System.out.println("OrbitsCheck: all checks passed");
   }

   private static StarField field() {
      return new StarField(StarField.ORBITS, new Dimension(W, H), 300, 20, false);
   }

   private static void paint(StarField field, BufferedImage image) {
      Graphics2D g = image.createGraphics();
      field.paint(g);
      g.dispose();
   }

   private static int countColor(BufferedImage image, Color color) {
      int count = 0;
      for (int y = 0; y < image.getHeight(); y += 4) {
         for (int x = 0; x < image.getWidth(); x += 4) {
            count += (image.getRGB(x, y) & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF) ? 16 : 0;
         }
      }
      return count;
   }

   private static void run(StarField field, double seconds) {
      for (double t = 0; t < seconds; t += 0.015) {
         field.advance(0.015, W, H);
      }
   }

   private static double distance(StarField field, int star, double x, double y) {
      return Math.hypot(field.centerX(star) - x, field.centerY(star) - y);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
