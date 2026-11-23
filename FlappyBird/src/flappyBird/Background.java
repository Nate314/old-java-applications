package flappyBird;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The scenery, in depth: a still sky with the sun, then clouds, far hills, near hills with their trees and the
 * meadow, each scrolling slower the further away it is, and the ground at the pipes' pace. The layers are drawn in
 * code from the shapes of art/bg.svg, scaled by the window's height so they keep their shape in any window (a
 * stretched picture made tall narrow clouds on a phone), and tile sideways every 1.6 window heights.
 */
public final class Background {
   public static int groundY = Main.FRAME_HEIGHT * 7 / 8;
   /** Each layer's pace as a share of the pipes': clouds, far hills, near hills, meadow, then the ground. */
   static final float[] PACE = {0.06F, 0.18F, 0.38F, 0.62F, 1.0F};
   /** The width the art repeats at (1600 of its 1000 high). */
   static final int TILE = Math.max(1, Math.round(Main.FRAME_HEIGHT * 1.6F));
   public Image groundImage;
   private static BufferedImage sky;
   private static BufferedImage[] layers;
   /** How far each layer has scrolled, in pixels, kept within its tile's width. */
   private final float[] offsets = new float[PACE.length];

   public Background(Image groundImage) {
      this.groundImage = groundImage;
      this.scroll();
   }

   public final void scroll() {
      for (int i = 0; i < PACE.length; i++) {
         this.offsets[i] = (this.offsets[i] + Pipes.scrollSpeed * PACE[i]) % tileWidth(i);
      }
   }

   /** How far each layer has scrolled (for a rewind). */
   final float[] remember() {
      return this.offsets.clone();
   }

   /** Puts the layers back where {@link #remember} saw them. */
   final void recall(float[] memory) {
      System.arraycopy(memory, 0, this.offsets, 0, this.offsets.length);
   }

   private static int tileWidth(int layer) {
      return layer == PACE.length - 1 ? Main.FRAME_WIDTH : TILE;
   }

   /** The sky and the hills, behind the pipes. */
   final void paintBack(Graphics2D g) {
      if (layers == null) {
         paintLayers();
      }
      g.drawImage(sky, -Main.FRAME_WIDTH / 20, 0, null);
      for (int i = 0; i < layers.length; i++) {
         this.tile(g, layers[i], i, 0);
      }
   }

   /** The ground, in front of the pipes. */
   final void paintGround(Graphics2D g) {
      this.tile(g, this.groundImage, PACE.length - 1, groundY);
   }

   private void tile(Graphics2D g, Image image, int layer, int y) {
      // A little past both sides of the window, so the crash's shake never shows an edge.
      int margin = Main.FRAME_WIDTH / 20;
      int x = -Math.round(this.offsets[layer]);
      if (x > -margin) {
         x -= tileWidth(layer);
      }
      for (; x < Main.FRAME_WIDTH + margin; x += tileWidth(layer)) {
         g.drawImage(image, x, y, null);
      }
   }

   /** Draws each layer once, at the window's size, from the shapes in art/bg.svg (1600 by 1000). */
   private static void paintLayers() {
      int w = Main.FRAME_WIDTH + Main.FRAME_WIDTH / 10;   // the sky is drawn from FRAME_WIDTH / 20 left of the window
      int h = Main.FRAME_HEIGHT;
      float s = h / 1000.0F;
      sky = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = art(sky, 1.0F);
      g.setPaint(new GradientPaint(0, 0, new Color(0x6db7e8), 0, h * 0.55F, new Color(0xb9def0)));
      g.fillRect(0, 0, w, Math.round(h * 0.55F) + 1);
      g.setPaint(new GradientPaint(0, h * 0.55F, new Color(0xb9def0), 0, h * 0.8F, new Color(0xfbe3c4)));
      g.fillRect(0, Math.round(h * 0.55F), w, h);
      g.setPaint(new Color(0xfbe3c4));
      g.fillRect(0, Math.round(h * 0.8F), w, h);
      // The sun stays where it is as the hills go by; its glow fades out in rings.
      g.scale(s, s);
      float sunX = (Main.FRAME_WIDTH * 0.79F) / s;
      for (int r = 170; r > 102; r -= 4) {
         g.setColor(new Color(255, 217, 138, 16));
         g.fillOval(Math.round(sunX - r), 560 - r, r * 2, r * 2);
      }
      g.setColor(new Color(255, 217, 138, 210));
      g.fillOval(Math.round(sunX - 102), 458, 204, 204);
      g.setColor(new Color(0xffe9a8));
      g.fillOval(Math.round(sunX - 78), 482, 156, 156);
      g.dispose();

      layers = new BufferedImage[4];
      for (int i = 0; i < layers.length; i++) {
         layers[i] = new BufferedImage(TILE, h, BufferedImage.TYPE_INT_ARGB);
      }
      g = art(layers[0], s);
      g.setColor(new Color(255, 255, 255, 217));
      int[][] clouds = {
         {260, 190, 110, 34}, {220, 168, 56, 40}, {300, 160, 66, 50},
         {760, 300, 90, 26}, {730, 282, 44, 30}, {790, 276, 52, 38},
         {1330, 150, 120, 32}, {1290, 128, 60, 42}, {1375, 122, 70, 52},
      };
      for (int[] c : clouds) {
         g.fillOval(c[0] - c[2], c[1] - c[3], c[2] * 2, c[3] * 2);
      }
      g.dispose();
      // The SVG's smooth (S) curves, written out with their mirrored first control points.
      hill(layers[1], s, 0xa7cfb0, 700, new float[]{160, 610, 320, 610, 480, 680, 640, 750, 800, 760, 960, 660, 1120, 560, 1300, 600, 1600, 700});
      hill(layers[2], s, 0x7dbb86, 800, new float[]{200, 730, 380, 740, 560, 790, 740, 840, 920, 860, 1100, 780, 1280, 700, 1420, 740, 1600, 800});
      g = art(layers[2], s);
      g.setColor(new Color(0x4f8f5c));
      int[][] trees = {{296, 742, 10, 44, 26}, {356, 752, 8, 34, 20}, {1026, 770, 10, 44, 26}, {1446, 758, 8, 34, 20}};
      for (int[] t : trees) {
         g.fillRect(t[0], t[1], t[2], t[3]);
         g.fillOval(t[0] + t[2] / 2 - t[4], t[1] - 10 - t[4], t[4] * 2, t[4] * 2);
      }
      g.dispose();
      hill(layers[3], s, 0x5fa26c, 900, new float[]{260, 860, 520, 870, 800, 900, 1080, 930, 1340, 940, 1600, 900});
   }

   /** A hill from the left edge at height y through cubic curves (three points each), filled down to the bottom. */
   private static void hill(BufferedImage image, float s, int rgb, float y, float[] curves) {
      Graphics2D g = art(image, s);
      Path2D.Float path = new Path2D.Float();
      path.moveTo(0, y);
      for (int i = 0; i < curves.length; i += 6) {
         path.curveTo(curves[i], curves[i + 1], curves[i + 2], curves[i + 3], curves[i + 4], curves[i + 5]);
      }
      path.lineTo(1600, 1000);
      path.lineTo(0, 1000);
      path.closePath();
      g.setColor(new Color(rgb));
      g.fill(path);
      g.dispose();
   }

   private static Graphics2D art(BufferedImage image, float s) {
      Graphics2D g = image.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale(s, s);
      return g;
   }
}
