package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The desktop backgrounds: the original photo (assets/background.jpg) and four painted scenes that move. A scene's
 * still parts (its sky, and its hills or seabed) are drawn once into images the size of the screen and copied on every
 * repaint, with what moves drawn between them. A picture painted in Paint can be the wallpaper too: its fat pixels
 * stretched to cover the screen (cut at the edges that do not fit), redrawn after it changes.
 */
final class Wallpaper {
   static final String DEFAULT = "photo";
   /** A Paint picture's wallpaper id is this followed by the picture's name. */
   static final String PICTURE = "picture:";
   static final Wallpaper[] ALL = {
      new Wallpaper("photo", "Original photo"),
      new Wallpaper("night", "Night sky"),
      new Wallpaper("sunset", "Sunset hills"),
      new Wallpaper("ocean", "Deep ocean"),
      new Wallpaper("aurora", "Aurora")
   };
   /** A shooting star crosses the night sky once in this many seconds, for SHOOT_FOR seconds. */
   static final double SHOOT_EVERY = 9;
   static final double SHOOT_FOR = 1.1;

   final String id;
   final String name;
   /** The Paint picture this wallpaper shows, or null for the photo and the painted scenes. */
   final Picture picture;
   private BufferedImage painted;
   private int paintedVersion;
   private BufferedImage swatch;
   /** A scene's sky and its see-through foreground, painted once per size. */
   private BufferedImage back;
   private BufferedImage front;
   /** The ocean's light rays in two sets, a little wider than the screen, swaying against each other. */
   private BufferedImage[] rays;
   /** The aurora's curtains, one column of each color at each of STRENGTHS brightnesses, stretched across as drawn. */
   private BufferedImage[][] curtains;
   private static final Color[] CURTAIN_COLORS = {new Color(70, 255, 160), new Color(120, 120, 255), new Color(60, 230, 200)};
   private static final double[] CURTAIN_LEVELS = {0.30, 0.42, 0.36};
   private static final int STRENGTHS = 12;

   private Wallpaper(String id, String name) {
      this(id, name, null);
   }

   private Wallpaper(String id, String name, Picture picture) {
      this.id = id;
      this.name = name;
      this.picture = picture;
   }

   static Wallpaper of(Picture picture) {
      return new Wallpaper(PICTURE + picture.name, picture.name, picture);
   }

   static Wallpaper byId(String id) {
      for (Wallpaper wallpaper : ALL) {
         if (wallpaper.id.equals(id)) {
            return wallpaper;
         }
      }
      return null;
   }

   /** True for the painted scenes, which move (stars twinkle, birds fly, bubbles rise, curtains ripple). */
   boolean moves() {
      return this.picture == null && !this.id.equals(DEFAULT);
   }

   /**
    * Fills w x h with this wallpaper as it is `seconds` into its motion; the photo is drawn from the image the desktop
    * already loaded.
    */
   void paint(Graphics g, int w, int h, Image photo, Component observer, double seconds) {
      if (this.id.equals(DEFAULT)) {
         g.drawImage(photo, 0, 0, w, h, observer);
         return;
      }
      if (this.picture != null) {
         if (this.paintedVersion != this.picture.version()) {
            this.painted = null; // the picture changed since it was last stretched over the screen
            this.paintedVersion = this.picture.version();
         }
         this.painted = this.scene(this.painted, w, h);
         g.drawImage(this.painted, 0, 0, observer);
         return;
      }
      this.layers(w, h);
      g.drawImage(this.back, 0, 0, observer);
      AppIcon.smooth(g);
      this.paintLife(g, w, h, seconds);
      g.drawImage(this.front, 0, 0, observer);
   }

   /** A small preview of the wallpaper, for the menu (a scene as it starts). */
   void paintSwatch(Graphics g, int x, int y, int w, int h, Image photo, Component observer) {
      if (this.id.equals(DEFAULT)) {
         g.drawImage(photo, x, y, w, h, observer);
      } else {
         this.swatch = this.scene(this.swatch, w, h);
         g.drawImage(this.swatch, x, y, observer);
      }
      g.setColor(new Color(255, 255, 255, 120));
      g.drawRect(x, y, w - 1, h - 1);
   }

   /** The cached picture or still scene when it is already w x h, else painted afresh at that size. */
   private BufferedImage scene(BufferedImage cached, int w, int h) {
      if (cached != null && cached.getWidth() == w && cached.getHeight() == h) {
         return cached;
      }
      BufferedImage image = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
      Graphics g = image.createGraphics();
      if (this.picture != null) {
         double scale = Math.max(w / (double)Picture.WIDTH, h / (double)Picture.HEIGHT);
         this.picture.paint(g, (w - Picture.WIDTH * scale) / 2, (h - Picture.HEIGHT * scale) / 2, scale, scale);
      } else {
         // Its own layers at this size; the desktop's are kept apart (they are the screen's size).
         Wallpaper still = new Wallpaper(this.id, this.name);
         still.paint(g, w, h, null, null, 0);
      }
      g.dispose();
      return image;
   }

   /** Paints the scene's sky (back) and its see-through foreground (front), once per size. */
   private void layers(int w, int h) {
      w = Math.max(1, w);
      h = Math.max(1, h);
      if (this.back != null && this.back.getWidth() == w && this.back.getHeight() == h) {
         return;
      }
      this.back = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      this.front = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics back = this.back.createGraphics();
      Graphics front = this.front.createGraphics();
      AppIcon.smooth(back);
      AppIcon.smooth(front);
      this.paintScene(back, front, w, h);
      back.dispose();
      front.dispose();
      if (this.id.equals("ocean")) {
         Random random = new Random(this.id.hashCode() * 31L + 7);
         int margin = sway(w);
         this.rays = new BufferedImage[2];
         for (int set = 0; set < 2; set++) {
            this.rays[set] = new BufferedImage(w + 2 * margin, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = this.rays[set].createGraphics();
            AppIcon.smooth(g);
            for (int i = 0; i < 13; i++) {
               int x = margin + random.nextInt(w);
               g.setColor(new Color(200, 255, 250, 14 + random.nextInt(18)));
               int[] xs = {x, x + w / 10 + random.nextInt(Math.max(1, w / 8)), x + w / 4 + random.nextInt(Math.max(1, w / 6)), x + w / 6};
               g.fillPolygon(xs, new int[] {0, 0, h, h}, 4);
            }
            g.dispose();
         }
      }
      if (this.id.equals("aurora")) {
         int length = curtainLength(h);
         int step = curtainStep(w);
         this.curtains = new BufferedImage[CURTAIN_COLORS.length][STRENGTHS];
         for (int c = 0; c < CURTAIN_COLORS.length; c++) {
            Color color = CURTAIN_COLORS[c];
            for (int k = 0; k < STRENGTHS; k++) {
               double strength = k / (double)(STRENGTHS - 1);
               this.curtains[c][k] = new BufferedImage(step, length, BufferedImage.TYPE_INT_ARGB);
               Graphics g = this.curtains[c][k].createGraphics();
               // Six bands, bright at the foot and fading upward.
               for (int band = 0; band < 6; band++) {
                  g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(strength * 46 * (6 - band) / 6)));
                  g.fillRect(0, length - length * (band + 1) / 6, step, length / 6 + 1);
               }
               g.dispose();
            }
         }
      }
   }

   private static int curtainLength(int h) {
      return Math.max(4, h / 4);
   }

   /** The width of a curtain's columns: at most about 240 across, so a big screen costs no more than a small one. */
   private static int curtainStep(int w) {
      return Math.max(3, w / 240);
   }

   /** How far the ocean's rays sway either way. */
   private static int sway(int w) {
      return w / 28 + 1;
   }

   private void paintScene(Graphics g, Graphics front, int w, int h) {
      Random random = new Random(this.id.hashCode());
      switch (this.id) {
         case "night":
            gradient(g, 0, 0, w, h, new Color(6, 10, 32), new Color(52, 38, 92));
            int moon = moon(w, h);
            g.setColor(new Color(255, 246, 214));
            g.fillOval(w * 3 / 4, h / 6, moon, moon);
            // The bite out of the crescent is the sky's own color at that height.
            g.setColor(new Color(14, 15, 42));
            g.fillOval(w * 3 / 4 + moon / 3, h / 6 - moon / 10, moon, moon);
            for (int i = 0; i < w * h / 2600; i++) {
               int alpha = 90 + random.nextInt(166);
               int size = random.nextInt(9) == 0 ? 3 : random.nextInt(2) + 1;
               int x = random.nextInt(Math.max(1, w));
               int y = random.nextInt(Math.max(1, h * 3 / 4));
               if (byMoon(x, y, w, h)) {
                  continue; // no stars in front of the moon
               }
               g.setColor(new Color(255, 250, 235, alpha));
               g.fillOval(x, y, size, size);
            }
            hills(front, w, h, 0.80, 0.05, 1.3, random, new Color(24, 22, 54));
            hills(front, w, h, 0.90, 0.04, 2.1, random, new Color(10, 10, 28));
            break;
         case "sunset":
            gradient(g, 0, 0, w, h * 2 / 3, new Color(54, 36, 112), new Color(255, 140, 90));
            gradient(g, 0, h * 2 / 3, w, h - h * 2 / 3, new Color(255, 140, 90), new Color(255, 200, 120));
            int sun = Math.max(8, Math.min(w, h) / 4);
            for (int ring = 3; ring >= 1; ring--) {
               g.setColor(new Color(255, 220, 150, 30));
               g.fillOval(w / 2 - sun / 2 - ring * sun / 6, h * 3 / 5 - sun / 2 - ring * sun / 6, sun + ring * sun / 3, sun + ring * sun / 3);
            }
            g.setColor(new Color(255, 236, 170));
            g.fillOval(w / 2 - sun / 2, h * 3 / 5 - sun / 2, sun, sun);
            hills(front, w, h, 0.66, 0.07, 1.1, random, new Color(196, 84, 112));
            hills(front, w, h, 0.76, 0.06, 1.7, random, new Color(124, 50, 104));
            hills(front, w, h, 0.88, 0.05, 2.6, random, new Color(58, 26, 70));
            break;
         case "ocean":
            gradient(g, 0, 0, w, h, new Color(0, 150, 170), new Color(2, 18, 52));
            hills(front, w, h, 0.90, 0.05, 3.0, random, new Color(2, 30, 50));
            break;
         default:
            gradient(g, 0, 0, w, h, new Color(4, 12, 24), new Color(8, 34, 52));
            for (int i = 0; i < w * h / 5000; i++) {
               g.setColor(new Color(230, 245, 255, 60 + random.nextInt(140)));
               g.fillRect(random.nextInt(Math.max(1, w)), random.nextInt(Math.max(1, h)), 1, 1);
            }
            hills(front, w, h, 0.86, 0.04, 2.4, random, new Color(4, 14, 22));
            break;
      }
   }

   /**
    * What moves, `seconds` in: twinkling stars and now and then a shooting star at night, a flock of birds over the
    * sunset, swaying light rays and rising bubbles in the ocean, rippling curtains in the aurora. Every place comes
    * from a fixed seed and the time alone, so the same moment always draws the same picture.
    */
   private void paintLife(Graphics g, int w, int h, double seconds) {
      Random random = new Random(this.id.hashCode() * 31L + 7);
      double t = seconds;
      switch (this.id) {
         case "night":
            for (int i = 0; i < Math.max(8, w * h / 24000); i++) {
               int x = random.nextInt(Math.max(1, w));
               int y = random.nextInt(Math.max(1, h * 3 / 5));
               double speed = 0.5 + random.nextDouble() * 1.8;
               double phase = random.nextDouble() * Math.PI * 2;
               int size = random.nextInt(4) == 0 ? 3 : 2;
               if (byMoon(x, y, w, h)) {
                  continue;
               }
               double glow = twinkle(t, speed, phase);
               g.setColor(new Color(255, 250, 235, (int)(30 + 225 * glow)));
               g.fillOval(x, y, size, size);
               if (size == 3 && glow > 0.8) {
                  g.drawLine(x - 2, y + 1, x + 4, y + 1);
                  g.drawLine(x + 1, y - 2, x + 1, y + 4);
               }
            }
            shootingStar(g, w, h, t);
            break;
         case "sunset":
            int span = Math.max(4, Math.min(w, h) / 55);
            double lap = w + 20.0 * span;
            for (int i = 0; i < 5; i++) {
               double size = span * (0.7 + 0.4 * random.nextDouble());
               int rank = (i + 1) / 2;
               double behind = rank * span * 3.2;
               double aside = (i % 2 == 1 ? -1 : 1) * rank * span * 1.6;
               // A V of five, the leader in front, crossing left to right in about 45 seconds; a fifth of the way in at first.
               double x = (t * w / 45 + w * 0.2 + 10 * span + lap - behind) % lap - 10 * span;
               double y = h * 0.3 + aside + Math.sin(t * 0.7 + i) * span * 0.6;
               bird(g, x, y, size, Math.sin(t * 5.5 + i * 1.9));
            }
            break;
         case "ocean":
            // Whole sets of rays sway rather than each ray: a few screen-sized copies cost far less than 26 big polygons.
            for (int set = 0; set < 2; set++) {
               g.drawImage(this.rays[set], (int)Math.round(-sway(w) * (1 + Math.sin(t * 0.22 + set * Math.PI))), 0, null);
            }
            double rise = h * 0.7;
            for (int i = 0; i < w * h / 9000; i++) {
               int size = 2 + random.nextInt(7);
               int alpha = 40 + random.nextInt(70);
               int x0 = random.nextInt(Math.max(1, w));
               double speed = h / 30.0 + random.nextDouble() * h / 14.0 + size * 2;
               double up = (t * speed + random.nextDouble() * rise) % rise;
               int x = x0 + (int)Math.round(Math.sin(t * 1.6 + i) * 3);
               int y = (int)Math.round(h * 0.95 - up);
               g.setColor(new Color(220, 255, 255, (int)(alpha * bubbleFade(up, rise, h))));
               g.drawOval(x, y, size, size);
            }
            break;
         default:
            for (int c = 0; c < CURTAIN_COLORS.length; c++) {
               this.ribbon(g, w, h, c, random.nextDouble() * Math.PI * 2, t);
            }
            break;
      }
   }

   /** A star's brightness from 0 to 1, rising and falling smoothly, never quite out. */
   static double twinkle(double t, double speed, double phase) {
      double s = 0.5 + 0.5 * Math.sin(t * speed + phase);
      return 0.15 + 0.85 * s * s;
   }

   /** A bubble `up` pixels over the seabed fades in as it leaves it and out before the top of its rise. */
   static double bubbleFade(double up, double rise, int h) {
      return Math.max(0, Math.min(1, Math.min(up / (h * 0.08), (rise - up) / (h * 0.15))));
   }

   /** How far into a shooting star's flight `t` is, from 0 to 1, or -1 while none is crossing; the first comes 2 s in. */
   static double shooting(double t) {
      double age = t - Math.floor(t / SHOOT_EVERY) * SHOOT_EVERY - 2;
      return age < 0 || age > SHOOT_FOR ? -1 : age / SHOOT_FOR;
   }

   /** A star streaking down across the upper sky, slowing as it goes, its tail fading behind it. */
   private static void shootingStar(Graphics g, int w, int h, double t) {
      double p = shooting(t);
      if (p < 0) {
         return;
      }
      Random random = new Random((long)Math.floor(t / SHOOT_EVERY) * 7919 + 13);
      double x0 = w * (0.08 + 0.55 * random.nextDouble());
      double y0 = h * (0.04 + 0.22 * random.nextDouble());
      double angle = Math.toRadians(18 + 22 * random.nextDouble());
      double travel = Math.max(w, h) * 0.32;
      double head = travel * (1 - (1 - p) * (1 - p));
      double tail = Math.min(head, travel * 0.3);
      double dx = Math.cos(angle);
      double dy = Math.sin(angle);
      double fade = p < 0.75 ? 1 : (1 - p) / 0.25;
      for (int s = 0; s < 8; s++) {
         double a = head - tail * (s + 1) / 8;
         double b = head - tail * s / 8;
         g.setColor(new Color(255, 250, 235, (int)(fade * 220 * (8 - s) / 8)));
         g.drawLine((int)(x0 + dx * a), (int)(y0 + dy * a), (int)(x0 + dx * b), (int)(y0 + dy * b));
      }
      g.setColor(new Color(255, 255, 255, (int)(fade * 255)));
      g.fillOval((int)(x0 + dx * head) - 1, (int)(y0 + dy * head) - 1, 3, 3);
   }

   /** A bird far off: two wings meeting at its body, raised or lowered as flap goes from 1 to -1. */
   private static void bird(Graphics g, double x, double y, double size, double flap) {
      g.setColor(new Color(58, 26, 70));
      int tip = (int)Math.round(y - size * 0.45 * flap);
      int bend = (int)Math.round(y - size * 0.15 - size * 0.1 * flap);
      int cx = (int)Math.round(x);
      int cy = (int)Math.round(y);
      int reach = (int)Math.round(size);
      for (int d = 0; d < 2; d++) {
         g.drawLine(cx - reach, tip + d, cx - reach / 2, bend + d);
         g.drawLine(cx - reach / 2, bend + d, cx, cy + d);
         g.drawLine(cx, cy + d, cx + reach / 2, bend + d);
         g.drawLine(cx + reach / 2, bend + d, cx + reach, tip + d);
      }
   }

   private static int moon(int w, int h) {
      return Math.max(6, Math.min(w, h) / 9);
   }

   /** True on and around the moon, where no star shows. */
   private static boolean byMoon(int x, int y, int w, int h) {
      int moon = moon(w, h);
      return x > w * 3 / 4 - 4 && x < w * 3 / 4 + moon * 4 / 3 + 4 && y > h / 6 - moon / 10 - 4 && y < h / 6 + moon + 4;
   }

   /** A vertical gradient from top to bottom, drawn in bands. */
   private static void gradient(Graphics g, int x, int y, int w, int h, Color top, Color bottom) {
      int bands = Math.max(1, Math.min(h, 160));
      for (int i = 0; i < bands; i++) {
         double t = bands == 1 ? 0 : i / (double)(bands - 1);
         g.setColor(new Color(mix(top.getRed(), bottom.getRed(), t), mix(top.getGreen(), bottom.getGreen(), t), mix(top.getBlue(), bottom.getBlue(), t)));
         int from = y + h * i / bands;
         g.fillRect(x, from, w, y + h * (i + 1) / bands - from);
      }
   }

   /** A rolling skyline of hills filled down to the bottom edge, around `level` (a fraction of the height). */
   private static void hills(Graphics g, int w, int h, double level, double height, double waves, Random random, Color color) {
      int steps = 48;
      int[] xs = new int[steps + 3];
      int[] ys = new int[steps + 3];
      double phase = random.nextDouble() * Math.PI * 2;
      for (int i = 0; i <= steps; i++) {
         double t = i / (double)steps;
         double wave = Math.sin(t * Math.PI * 2 * waves + phase) + 0.4 * Math.sin(t * Math.PI * 2 * waves * 2.7 + phase * 1.3);
         xs[i] = (int)Math.round(t * w);
         ys[i] = (int)Math.round(h * (level - height * wave));
      }
      xs[steps + 1] = w;
      ys[steps + 1] = h;
      xs[steps + 2] = 0;
      ys[steps + 2] = h;
      g.setColor(color);
      g.fillPolygon(xs, ys, steps + 3);
   }

   /**
    * A curtain of light along a wave, bright at the foot and fading upward: a column picture every few pixels, at the
    * brightness that part of the curtain has. The wave travels and the bright folds drift as `t` (seconds) grows.
    */
   private void ribbon(Graphics g, int w, int h, int c, double phase, double t) {
      int length = curtainLength(h);
      int step = curtainStep(w);
      for (int x = 0; x < w; x += step) {
         double u = x / (double)Math.max(1, w);
         int foot = (int)(h * (CURTAIN_LEVELS[c] + 0.08 * Math.sin(u * Math.PI * 3 + phase + t * 0.12) + 0.03 * Math.sin(u * Math.PI * 11 + phase - t * 0.35)));
         double strength = 0.5 + 0.5 * Math.sin(u * Math.PI * 7 + phase * 2 + t * 0.2);
         int k = (int)Math.round(strength * (STRENGTHS - 1));
         if (k > 0) {
            g.drawImage(this.curtains[c][k], x, foot - length, null);
         }
      }
   }

   private static int mix(int a, int b, double t) {
      return (int)Math.round(a + (b - a) * t);
   }
}
