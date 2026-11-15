package flappyBird;

/**
 * What counts as touching a pipe: the bird's round body, not the square picture around it, against the pipe as drawn
 * (a shaft, with a wider flange at its open end). A crash then only happens where the drawings really meet.
 */
final class Hitbox {
   /** The body circle in art/bird.svg (100 x 100): centre (48, 54), radius 36, as fractions of the picture. */
   static final float BODY_X = 0.48F;
   static final float BODY_Y = 0.54F;
   static final float BODY_R = 0.36F;
   /** In art/topPipe.svg (240 x 1200) the shaft runs from x 22 to 218; the flange is full width and 88 tall. */
   static final float SHAFT_INSET = 22.0F / 240.0F;
   static final float FLANGE = 88.0F / 1200.0F;

   private Hitbox() {
   }

   /**
    * The body as {centre x, centre y, radius} for a bird picture of size `size` whose top left is (x, y), drawn tilted
    * by `rotation` degrees clockwise around its middle (see GamePanel.drawBird).
    */
   static float[] body(float x, float y, float size, float rotation) {
      double a = Math.toRadians(rotation);
      double dx = (BODY_X - 0.5F) * size;
      double dy = (BODY_Y - 0.5F) * size;
      float cx = (float)(x + size / 2.0F + dx * Math.cos(a) - dy * Math.sin(a));
      float cy = (float)(y + size / 2.0F + dx * Math.sin(a) + dy * Math.cos(a));
      return new float[]{cx, cy, BODY_R * size};
   }

   /**
    * The two rectangles {x, y, width, height} of a pipe picture `width` x `height` at (x, y): its shaft and its flange,
    * at the bottom of a top pipe and the top of a bottom pipe.
    */
   static float[][] pipe(float x, float y, float width, float height, boolean top) {
      float flange = FLANGE * height;
      float inset = SHAFT_INSET * width;
      float flangeY = top ? y + height - flange : y;
      float shaftY = top ? y : y + flange;
      return new float[][]{{x + inset, shaftY, width - inset * 2.0F, height - flange}, {x, flangeY, width, flange}};
   }

   /** Where the body first touches the pipe (the pipe's nearest point to its centre), or null if it does not. */
   static float[] touch(float[] body, float[][] pipe) {
      float[] nearest = null;
      float best = body[2] * body[2];
      for (float[] r : pipe) {
         float px = Math.max(r[0], Math.min(body[0], r[0] + r[2]));
         float py = Math.max(r[1], Math.min(body[1], r[1] + r[3]));
         float d = (px - body[0]) * (px - body[0]) + (py - body[1]) * (py - body[1]);
         if (d < best) {
            best = d;
            nearest = new float[]{px, py};
         }
      }
      return nearest;
   }
}
