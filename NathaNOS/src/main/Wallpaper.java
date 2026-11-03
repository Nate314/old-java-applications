package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The desktop backgrounds: the original photo (assets/background.jpg) and four painted scenes. A painted one is drawn
 * once into an image the size of the screen and then copied on every repaint. A picture painted in Paint can be the
 * wallpaper too: its fat pixels stretched to cover the screen (cut at the edges that do not fit), redrawn after it
 * changes.
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

   final String id;
   final String name;
   /** The Paint picture this wallpaper shows, or null for the photo and the painted scenes. */
   final Picture picture;
   private BufferedImage painted;
   private int paintedVersion;
   private BufferedImage swatch;

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

   /** Fills w x h with this wallpaper; the photo is drawn from the image the desktop already loaded. */
   void paint(Graphics g, int w, int h, Image photo, Component observer) {
      if (this.id.equals("photo")) {
         g.drawImage(photo, 0, 0, w, h, observer);
         return;
      }
      if (this.picture != null && this.paintedVersion != this.picture.version()) {
         this.painted = null; // the picture changed since it was last stretched over the screen
         this.paintedVersion = this.picture.version();
      }
      this.painted = this.scene(this.painted, w, h);
      g.drawImage(this.painted, 0, 0, observer);
   }

   /** A small preview of the wallpaper, for the menu. */
   void paintSwatch(Graphics g, int x, int y, int w, int h, Image photo, Component observer) {
      if (this.id.equals("photo")) {
         g.drawImage(photo, x, y, w, h, observer);
      } else {
         this.swatch = this.scene(this.swatch, w, h);
         g.drawImage(this.swatch, x, y, observer);
      }
      g.setColor(new Color(255, 255, 255, 120));
      g.drawRect(x, y, w - 1, h - 1);
   }

   /** The cached scene when it is already w x h, else the scene painted afresh at that size. */
   private BufferedImage scene(BufferedImage cached, int w, int h) {
      if (cached != null && cached.getWidth() == w && cached.getHeight() == h) {
         return cached;
      }
      BufferedImage image = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
      Graphics g = image.createGraphics();
      if (this.picture != null) {
         double scale = Math.max(w / (double)Picture.WIDTH, h / (double)Picture.HEIGHT);
         this.picture.paint(g, (w - Picture.WIDTH * scale) / 2, (h - Picture.HEIGHT * scale) / 2, scale, scale);
         g.dispose();
         return image;
      }
      AppIcon.smooth(g);
      this.paintScene(g, w, h);
      g.dispose();
      return image;
   }

   private void paintScene(Graphics g, int w, int h) {
      Random random = new Random(this.id.hashCode());
      switch (this.id) {
         case "night":
            gradient(g, 0, 0, w, h, new Color(6, 10, 32), new Color(52, 38, 92));
            int moon = Math.max(6, Math.min(w, h) / 9);
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
               if (x > w * 3 / 4 - 4 && x < w * 3 / 4 + moon * 4 / 3 + 4 && y > h / 6 - moon / 10 - 4 && y < h / 6 + moon + 4) {
                  continue; // no stars in front of the moon
               }
               g.setColor(new Color(255, 250, 235, alpha));
               g.fillOval(x, y, size, size);
            }
            hills(g, w, h, 0.80, 0.05, 1.3, random, new Color(24, 22, 54));
            hills(g, w, h, 0.90, 0.04, 2.1, random, new Color(10, 10, 28));
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
            hills(g, w, h, 0.66, 0.07, 1.1, random, new Color(196, 84, 112));
            hills(g, w, h, 0.76, 0.06, 1.7, random, new Color(124, 50, 104));
            hills(g, w, h, 0.88, 0.05, 2.6, random, new Color(58, 26, 70));
            break;
         case "ocean":
            gradient(g, 0, 0, w, h, new Color(0, 150, 170), new Color(2, 18, 52));
            for (int i = 0; i < 26; i++) {
               int x = random.nextInt(Math.max(1, w));
               g.setColor(new Color(200, 255, 250, 14 + random.nextInt(18)));
               int[] xs = {x, x + w / 10 + random.nextInt(Math.max(1, w / 8)), x + w / 4 + random.nextInt(Math.max(1, w / 6)), x + w / 6};
               int[] ys = {0, 0, h, h};
               g.fillPolygon(xs, ys, 4);
            }
            for (int i = 0; i < w * h / 9000; i++) {
               int size = 2 + random.nextInt(7);
               g.setColor(new Color(220, 255, 255, 40 + random.nextInt(70)));
               g.drawOval(random.nextInt(Math.max(1, w)), h / 3 + random.nextInt(Math.max(1, h * 2 / 3)), size, size);
            }
            hills(g, w, h, 0.90, 0.05, 3.0, random, new Color(2, 30, 50));
            break;
         default:
            gradient(g, 0, 0, w, h, new Color(4, 12, 24), new Color(8, 34, 52));
            for (int i = 0; i < w * h / 5000; i++) {
               g.setColor(new Color(230, 245, 255, 60 + random.nextInt(140)));
               g.fillRect(random.nextInt(Math.max(1, w)), random.nextInt(Math.max(1, h)), 1, 1);
            }
            ribbon(g, w, h, 0.30, new Color(70, 255, 160), random);
            ribbon(g, w, h, 0.42, new Color(120, 120, 255), random);
            ribbon(g, w, h, 0.36, new Color(60, 230, 200), random);
            hills(g, w, h, 0.86, 0.04, 2.4, random, new Color(4, 14, 22));
            break;
      }
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

   /** A curtain of light: thin columns along a wave, bright at the foot and fading upward. */
   private static void ribbon(Graphics g, int w, int h, double level, Color color, Random random) {
      double phase = random.nextDouble() * Math.PI * 2;
      int length = Math.max(4, h / 4);
      for (int x = 0; x < w; x += 3) {
         double t = x / (double)Math.max(1, w);
         int foot = (int)(h * (level + 0.08 * Math.sin(t * Math.PI * 3 + phase) + 0.03 * Math.sin(t * Math.PI * 11 + phase)));
         double strength = 0.5 + 0.5 * Math.sin(t * Math.PI * 7 + phase * 2);
         for (int band = 0; band < 6; band++) {
            int alpha = (int)(strength * 46 * (6 - band) / 6);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillRect(x, foot - length * (band + 1) / 6, 3, length / 6 + 1);
         }
      }
   }

   private static int mix(int a, int b, double t) {
      return (int)Math.round(a + (b - a) * t);
   }
}
