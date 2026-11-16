package flappyBird;

/** Self-check for fair crashes: the bird's round body against a pipe's shaft and flange. Run with java -ea. */
public final class HitboxCheck {
   public static void main(String[] args) {
      if (!HitboxCheck.class.desiredAssertionStatus()) {
         throw new IllegalStateException("Run with -ea");
      }
      float size = 100.0F;

      // The body sits where art/bird.svg draws it, and turns with the bird around the picture's middle.
      float[] body = Hitbox.body(0, 0, size, 0);
      assert near(body[0], 48) && near(body[1], 54) && near(body[2], 36) : "body as drawn";
      float[] dive = Hitbox.body(0, 0, size, 90);
      assert near(dive[0], 46) && near(dive[1], 48) : "a nose-down dive turns the body with the picture";

      // A bottom pipe 200 x 1000 at (0, 500): flange from y 500 to 573.3, full width; shaft inset 18.3 each side.
      float[][] bottom = Hitbox.pipe(0, 500, 200, 1000, false);
      assert near(bottom[1][1], 500) && near(bottom[1][3], 1000 * 88 / 1200F) && near(bottom[1][2], 200) : "flange at the top";
      assert near(bottom[0][0], 200 * 22 / 240F) && near(bottom[0][1], 500 + 1000 * 88 / 1200F) : "shaft under it, inset";

      // The bird's picture corner over the flange's corner, its round body clear of it: no crash (the old square did crash).
      float[] corner = Hitbox.body(-90, 410, size, 0);
      assert -90 + size > 0 && 410 + size > 500 : "the picture's square overlaps the pipe";
      assert Hitbox.touch(corner, bottom) == null : "an empty corner of the picture is not a crash";

      // The body dipping 5 px into the flange's top: a crash, touching the flange's top edge under the body's centre.
      float[] dip = Hitbox.body(50, 500 - 90 + 5, size, 0);
      float[] spot = Hitbox.touch(dip, bottom);
      assert spot != null && near(spot[0], 98) && near(spot[1], 500) : "touches the flange's top edge";

      // Beside the shaft, below the flange: the picture's see-through margin is not pipe, the shaft is.
      float[] margin = Hitbox.body(-84 + 10, 700, size, 0);
      assert Hitbox.touch(margin, bottom) == null : "the margin beside the shaft is not pipe";
      float[] shaft = Hitbox.body(-84 + 25, 700, size, 0);
      float[] side = Hitbox.touch(shaft, bottom);
      assert side != null && near(side[0], 200 * 22 / 240F) : "the shaft's side is";

      // A top pipe has its flange at the bottom (its open end).
      float[][] top = Hitbox.pipe(0, -500, 200, 1000, true);
      assert near(top[1][1], 500 - 1000 * 88 / 1200F) && near(top[0][1], -500) : "flange at a top pipe's open end";

      System.out.println("HitboxCheck passed");
   }

   private static boolean near(float a, float b) {
      return Math.abs(a - b) < 0.01F;
   }
}
