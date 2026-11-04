package dinosaurgameplayer;

import java.util.Random;

/**
 * An evolved bot's whole mind: the original bot's rule with its numbers left open. A jump eye (a pixel) and how
 * long to hold the jump key when an obstacle covers it, and an optional duck eye and how long to hold Down. The
 * original bot is one of these: its pixel, 65 ms, and no duck eye.
 */
final class Brain {
   static final int MIN_X = Runner.DINO_X + Runner.DINO_W + 1;
   static final int MAX_X = Runner.WIDTH - 12;
   static final int MIN_Y = Runner.GROUND - 110;
   static final int MAX_Y = Runner.GROUND - 1;
   static final int MIN_MS = 20;
   static final int MAX_MS = 300;
   /** Where a new duck eye opens: the heights a middle bird flies at. */
   private static final int DUCK_TOP = Runner.GROUND - 60;
   private static final int DUCK_BOTTOM = Runner.GROUND - 30;

   final int jumpX;
   final int jumpY;
   final int jumpMs;
   final int duckX;
   final int duckY;
   /** 0: no duck eye. */
   final int duckMs;

   Brain(int jumpX, int jumpY, int jumpMs, int duckX, int duckY, int duckMs) {
      this.jumpX = clamp(jumpX, MIN_X, MAX_X);
      this.jumpY = clamp(jumpY, MIN_Y, MAX_Y);
      this.jumpMs = clamp(jumpMs, MIN_MS, MAX_MS);
      this.duckX = clamp(duckX, MIN_X, MAX_X);
      this.duckY = clamp(duckY, MIN_Y, MAX_Y);
      this.duckMs = duckMs <= 0 ? 0 : clamp(duckMs, MIN_MS, MAX_MS);
   }

   /** The original bot: the pixel just left of its marker where it starts, 65 ms, never ducks. */
   static Brain original() {
      return new Brain(PixelBot.DEFAULT_X - 1, PixelBot.DEFAULT_Y, ObstacleDetector.HOLD_MS, MIN_X, MIN_Y, 0);
   }

   static Brain random(Random r) {
      return new Brain(between(r, MIN_X, 320), between(r, Runner.GROUND - 70, MAX_Y), between(r, 30, 200),
         between(r, MIN_X, 320), between(r, DUCK_TOP, DUCK_BOTTOM), r.nextInt(4) > 0 ? between(r, 60, 300) : 0);
   }

   boolean ducks() {
      return this.duckMs > 0;
   }

   /** A child: each number from one parent or the other, then a few of them nudged (or the duck eye switched). */
   Brain child(Brain other, Random r) {
      int jx = mutate(r, pick(r, this.jumpX, other.jumpX), 24);
      int jy = mutate(r, pick(r, this.jumpY, other.jumpY), 12);
      int jms = mutate(r, pick(r, this.jumpMs, other.jumpMs), 30);
      boolean duckParent = r.nextBoolean();
      Brain d = duckParent ? this : other;
      int dx = mutate(r, d.duckX, 24);
      int dy = mutate(r, d.duckY, 12);
      int dms = d.duckMs > 0 ? mutate(r, d.duckMs, 40) : 0;
      if (r.nextInt(10) == 0) {
         if (dms > 0) {
            dms = 0;   // loses the duck eye
         } else {
            // grows one, somewhere a bird would cross it
            dx = between(r, MIN_X, 320);
            dy = between(r, DUCK_TOP, DUCK_BOTTOM);
            dms = between(r, 60, 300);
         }
      }
      return new Brain(jx, jy, jms, dx, dy, dms);
   }

   /** Six numbers, for ~/.dinogame.txt. */
   String encode() {
      return this.jumpX + "," + this.jumpY + "," + this.jumpMs + "," + this.duckX + "," + this.duckY + "," + this.duckMs;
   }

   /** The brain encode wrote, or null when the text is damaged. */
   static Brain decode(String text) {
      String[] n = text.split(",");
      if (n.length != 6) {
         return null;
      }
      try {
         int[] v = new int[6];
         for (int i = 0; i < 6; i++) {
            v[i] = Integer.parseInt(n[i].trim());
         }
         return new Brain(v[0], v[1], v[2], v[3], v[4], v[5]);
      } catch (NumberFormatException e) {
         return null;
      }
   }

   private static int pick(Random r, int a, int b) {
      return r.nextBoolean() ? a : b;
   }

   /** One time in three, moves the number by a bell curve of the given spread. */
   private static int mutate(Random r, int value, int spread) {
      return r.nextInt(3) == 0 ? value + (int) Math.round(r.nextGaussian() * spread) : value;
   }

   private static int between(Random r, int lo, int hi) {
      return lo + r.nextInt(hi - lo + 1);
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
