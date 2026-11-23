package flappyBird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * The bird's wing, drawn in code over bird.png (which has none) so it can beat: each flap raises it and sweeps it down
 * hard, it rests along the body in a glide, folds back in a dive, flutters on the title screen and hangs limp after a
 * crash. Its tilt is worked out from the bird's own state (the flap's stretch and the dive), so a rewind puts it back too.
 */
final class Wing {
   /** Tilts in degrees; a positive tilt raises the wing's tip. REST is where bird.svg's old wing lay. */
   static final float REST = -18.0F;
   static final float RAISED = 55.0F;
   static final float DOWN = -60.0F;
   static final float FOLDED = 12.0F;
   static final float LIMP = 35.0F;

   private Wing() {
   }

   /** In flight: a flap (squash 1, easing to 0) lifts the wing, sweeps it down past rest and back; a dive folds it. */
   static float flight(float squash, float rotation) {
      if (squash > 0.0F) {
         float t = 1.0F - squash;
         if (t < 0.2F) {
            return lerp(REST, RAISED, t / 0.2F);
         }
         return t < 0.6F ? lerp(RAISED, DOWN, (t - 0.2F) / 0.4F) : lerp(DOWN, REST, (t - 0.6F) / 0.4F);
      }
      float dive = Math.max(0.0F, Math.min(1.0F, (rotation - 20.0F) / (Bird.NOSE_DOWN - 20.0F)));
      return lerp(REST, FOLDED, dive);
   }

   /** On the title screen: two beats in each 0.8 s bob (turn counts bobs), held at rest with reduced motion. */
   static float hover(double turn, boolean calm) {
      return calm ? REST : REST + 38.0F * (float)Math.cos(turn * 4.0 * Math.PI);
   }

   private static float lerp(float from, float to, float t) {
      return from + (to - from) * t;
   }

   /**
    * Draws the wing at a tilt, in a graphics already moved to the bird's centre and turned with it. Its root is just
    * behind the eye, and it reaches back over the body, in the 100-unit drawing of bird.svg scaled to the bird.
    */
   static void draw(Graphics2D g2, float tilt) {
      Graphics2D g = (Graphics2D)g2.create();
      double unit = Bird.WIDTH / 100.0;
      g.scale(unit, unit);
      g.translate(2.0, 5.0);
      g.rotate(Math.toRadians(tilt));   // the tip points back (-x), so turning clockwise (y down) raises it
      // A teardrop: round at the root, tapering to the tip; a shade under it, its darker edge and two feather lines.
      Path2D.Float wing = new Path2D.Float();
      wing.moveTo(2.0, -9.0);
      wing.quadTo(-22.0, -15.0, -46.0, -1.0);
      wing.quadTo(-24.0, 15.0, 0.0, 11.0);
      wing.quadTo(9.0, 1.0, 2.0, -9.0);
      wing.closePath();
      g.translate(1.0, 3.0);
      g.setColor(new Color(10, 70, 64, 70));
      g.fill(wing);
      g.translate(-1.0, -3.0);
      g.setColor(new Color(29, 143, 130));
      g.fill(wing);
      g.setStroke(new BasicStroke(2.5F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.setColor(new Color(21, 107, 98));
      g.draw(wing);
      g.drawLine(-32, 3, -12, 4);
      g.drawLine(-24, 8, -8, 8);
      g.dispose();
   }
}
