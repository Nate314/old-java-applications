package animate2;

import java.awt.Dimension;

/** Self-check for the animation window's stars under the pointer: pull, fling, burst, wake, edges and modes. Run with java -ea. */
public class StarHandCheck {
   private static final int W = 800;
   private static final int H = 600;

   public static void main(String[] args) {
      for (int mode = 0; mode < StarField.MODE_NAMES.length; mode++) {
         // Holding pulls the stars in: after 3 s the average distance to the pointer has shrunk a lot.
         StarField field = field(mode);
         double before = meanDistance(field, 200, 150);
         field.startPull(200, 150);
         run(field, 3.0);
         double gathered = meanDistance(field, 200, 150);
         check(field.pulling() && gathered < before * 0.5, "mode " + mode + " pull gathers (" + (int)before + " -> " + (int)gathered + ")");
         // Letting go flings them back out.
         field.release(200, 150);
         check(!field.pulling(), "mode " + mode + " release ends the pull");
         run(field, 0.4);
         check(meanDistance(field, 200, 150) > gathered * 1.5, "mode " + mode + " fling spreads them");
         // Long after, every star is back on (or just past, for the wrapping modes) the field, kicks spent.
         run(field, 10.0);
         for (int star = 0; star < field.starCount; star++) {
            double x = field.centerX(star);
            double y = field.centerY(star);
            check(x >= -40 && x <= W + 40 && y >= -40 && y <= H + 40, "mode " + mode + " star " + star + " stays on the field");
         }
      }

      // A click bursts the nearby stars outward and leaves far ones alone.
      StarField field = field(0);
      double[] near = position(field, 0);
      field.burst(near[0] + 30, near[1], StarField.BURST);
      run(field, 0.05);
      double[] after = position(field, 0);
      check(after[0] < near[0] - 2, "burst pushes a near star away");
      // A release after a long hold flings harder than a plain click.
      field.startPull(400, 300);
      check(field.flingStrength() == StarField.BURST, "a click bursts at the plain strength");
      run(field, 0.75);
      double half = field.flingStrength();
      run(field, 1.0);
      check(half > StarField.BURST && field.flingStrength() > half, "a longer hold flings harder");
      double full = field.flingStrength();
      run(field, 1.0);
      check(field.flingStrength() == full, "the fling stops charging");
      field.release(400, 300);

      // Resting the pointer on a star nudges it aside; once the pointer leaves, nothing pushes.
      field = field(0);
      double[] star = position(field, 0);
      field.pointer(star[0] + 10, star[1], true);
      run(field, 0.3);
      check(Math.hypot(field.centerX(0) - star[0] - 10, field.centerY(0) - star[1]) > 12, "the wake nudges a star");

      // Modes change in place, keep the stars, and wrap round.
      field = field(2);
      field.addStar();
      int count = field.starCount;
      field.setMode(4);
      check(field.mode() == 4 && field.starCount == count, "mode change keeps the stars");
      field.setMode(field.mode() + 1);
      check(field.mode() == 0, "N wraps to mode 1");
      field.setMode(1);
      for (int s = 0; s < field.starCount; s++) {
         check(Math.abs(field.centerX(s) - W / 2.0) < 1 && Math.abs(field.centerY(s) - H / 2.0) < 1, "Burst mode starts in the middle");
      }

      // The help lines can be hidden and brought back.
      check(!field.hintVisible(), "no help on a sky");
      field.toggleHint();
      check(field.hintVisible(), "H shows the help");
      field.toggleHint();
      check(!field.hintVisible(), "H hides it again");
      field.toggleHint();
      run(field, 10.0);
      check(!field.hintVisible(), "the help fades by itself");

      System.out.println("StarHandCheck: all checks passed");
   }

   private static StarField field(int mode) {
      return new StarField(mode, new Dimension(W, H), 300, 20, false);
   }

   private static void run(StarField field, double seconds) {
      for (double t = 0; t < seconds; t += 0.015) {
         field.advance(0.015, W, H);
      }
   }

   private static double[] position(StarField field, int star) {
      return new double[]{field.centerX(star), field.centerY(star)};
   }

   private static double meanDistance(StarField field, double x, double y) {
      double sum = 0;
      for (int star = 0; star < field.starCount; star++) {
         sum += Math.hypot(field.centerX(star) - x, field.centerY(star) - y);
      }
      return sum / field.starCount;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
