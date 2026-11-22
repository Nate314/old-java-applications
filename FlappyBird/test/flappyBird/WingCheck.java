package flappyBird;

/** Checks the wing's beat: a flap lifts it, sweeps it down past rest and back, a glide rests it and a dive folds it. */
public final class WingCheck {
   public static void main(String[] args) {
      assert Wing.flight(0.0F, 0.0F) == Wing.REST : "a glide rests the wing";
      assert Wing.flight(0.0F, Bird.NOSE_UP) == Wing.REST : "nose up after a flap, the wing still rests";
      assert Wing.flight(0.0F, Bird.NOSE_DOWN) == Wing.FOLDED : "a dive folds the wing back";
      assert Wing.flight(1.0F, 0.0F) == Wing.REST : "the flap starts from rest, no jump";
      // A flap's stretch eases from 1 by 0.06 a step (Bird.update); follow the wing through it.
      float highest = Wing.REST;
      float lowest = Wing.REST;
      boolean downAfterUp = false;
      float last = Wing.REST;
      for (float squash = 1.0F; squash > 0.0F; squash -= 0.06F) {
         float tilt = Wing.flight(squash, 0.0F);
         assert Math.abs(tilt - last) <= 40.0F : "the wing moves smoothly: " + last + " -> " + tilt;
         if (tilt > highest) {
            highest = tilt;
         }
         if (tilt < lowest) {
            lowest = tilt;
            downAfterUp = highest > Wing.REST + 30.0F;
         }
         last = tilt;
      }
      assert highest > Wing.REST + 30.0F : "a flap raises the wing: " + highest;
      assert lowest < Wing.REST - 25.0F && downAfterUp : "then sweeps it down, after the lift: " + lowest;
      assert Math.abs(last - Wing.REST) < 10.0F : "and it comes back to rest: " + last;
      assert Wing.hover(0.3, true) == Wing.REST : "reduced motion holds the title screen's wing still";
      assert Wing.hover(0.0, false) > Wing.REST && Wing.hover(0.25, false) < Wing.REST : "on the title screen it flutters";
      System.out.println("WingCheck passed");
   }
}
