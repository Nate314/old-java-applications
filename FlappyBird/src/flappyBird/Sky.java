package flappyBird;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * The time of day, which turns as pipes are passed: the sunrise the art is drawn at, noon at 10 pipes (Bronze),
 * sunset at 20 (Silver), night with stars and a moon at 30 (Gold), and sunrise again at 40 (Platinum).
 * A phase is pipes / 10, so 1 is noon and 4 is the next sunrise.
 */
final class Sky {
   static final int PIPES_PER_PHASE = 10;
   /** Per time of day: the wash over the sky at its top and at the horizon, the dimming of the pipes and ground (RGBA each), and how bright the stars are. */
   private static final int[][] KEYS = {
      {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
      {50, 130, 230, 80, 140, 205, 255, 70, 0, 0, 0, 0, 0},
      {70, 40, 130, 130, 255, 110, 50, 140, 90, 30, 40, 45, 0},
      {6, 10, 38, 230, 24, 34, 86, 195, 10, 16, 48, 115, 255},
   };
   private static final int STARS = 70;
   /** Each star's x and y as fractions of the window (the upper sky only, above the hills) and its size, 1 to 3. */
   private static final float[][] STAR_AT = new float[STARS][3];

   static {
      Random random = new Random(1937);
      for (float[] star : STAR_AT) {
         star[0] = random.nextFloat();
         star[1] = random.nextFloat() * 0.55F;
         star[2] = 1 + random.nextInt(3);
      }
   }

   private Sky() {
   }

   /** The 13 numbers of {@link #KEYS} at any phase, blended between the two times of day it falls between. */
   static int[] at(float phase) {
      int count = KEYS.length;
      float wrapped = ((phase % count) + count) % count;
      int from = (int)wrapped % count;
      int[] a = KEYS[from];
      int[] b = KEYS[(from + 1) % count];
      float t = wrapped - (int)wrapped;
      int[] out = new int[13];
      for (int color = 0; color < 12; color += 4) {
         // Blend premultiplied, so fading in from a clear wash does not pass through black.
         float alphaA = a[color + 3] / 255.0F;
         float alphaB = b[color + 3] / 255.0F;
         float alpha = alphaA + (alphaB - alphaA) * t;
         for (int c = 0; c < 3; c++) {
            float premultiplied = a[color + c] * alphaA + (b[color + c] * alphaB - a[color + c] * alphaA) * t;
            out[color + c] = alpha > 0.0F ? Math.round(premultiplied / alpha) : 0;
         }
         out[color + 3] = Math.round(alpha * 255.0F);
      }
      out[12] = Math.round(a[12] + (b[12] - a[12]) * t);
      return out;
   }

   /** The wash over the sky (in bands down to the horizon), then the stars and the moon. Calm: the stars hold still. */
   static void paintSky(Graphics2D g, float phase, int width, int height, int horizon, boolean calm) {
      int[] sky = at(phase);
      if (sky[3] > 0 || sky[7] > 0) {
         int bands = 16;
         for (int band = 0; band < bands; band++) {
            float t = band / (float)(bands - 1);
            int top = horizon * band / bands;
            int bottom = horizon * (band + 1) / bands;
            g.setColor(new Color(mix(sky[0], sky[4], t), mix(sky[1], sky[5], t), mix(sky[2], sky[6], t), mix(sky[3], sky[7], t)));
            // Wider than the window, so the crash's shake never shows an edge.
            g.fillRect(-width / 10, band == 0 ? -height / 10 : top, width * 6 / 5, bottom - (band == 0 ? -height / 10 : top));
         }
      }
      int stars = sky[12];
      if (stars <= 0) {
         return;
      }
      double now = System.currentTimeMillis() / 1000.0;
      float scale = Math.max(1.0F, height / 400.0F);
      for (int i = 0; i < STARS; i++) {
         float[] star = STAR_AT[i];
         double twinkle = calm ? 0.8 : 0.6 + 0.4 * Math.sin(now * (1.5 + i % 5 * 0.4) + i * 1.7);
         g.setColor(new Color(255, 250, 225, (int)(stars * twinkle)));
         int size = Math.round(star[2] * scale);
         g.fillRect(Math.round(star[0] * width), Math.round(star[1] * height), size, size);
      }
      int moonX = width / 5;
      int moonY = height / 7;
      int r = height / 16;
      g.setColor(new Color(255, 250, 220, stars / 6));
      g.fillOval(moonX - r * 2, moonY - r * 2, r * 4, r * 4);
      g.setColor(new Color(245, 240, 215, stars));
      g.fillOval(moonX - r, moonY - r, r * 2, r * 2);
      g.setColor(new Color(205, 200, 180, stars));
      g.fillOval(moonX - r / 2, moonY - r / 3, r / 2, r / 2);
      g.fillOval(moonX + r / 5, moonY + r / 4, r / 3, r / 3);
   }

   /** Dims the pipes and the ground for the time of day (the bird is drawn after, so it stays bright). */
   static void paintDim(Graphics2D g, float phase, int width, int height) {
      int[] sky = at(phase);
      if (sky[11] > 0) {
         g.setColor(new Color(sky[8], sky[9], sky[10], sky[11]));
         g.fillRect(-width / 10, -height / 10, width * 6 / 5, height * 6 / 5);
      }
   }

   private static int mix(int a, int b, float t) {
      return Math.round(a + (b - a) * t);
   }
}
