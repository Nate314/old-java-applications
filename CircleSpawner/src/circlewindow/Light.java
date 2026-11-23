package circlewindow;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * The light on the shape: one see-through picture of a dome lit from the upper left (a soft white shine there,
 * shade toward the lower right), stretched over the whole shape's box. Every window draws the part of it that
 * falls on that window, so overlapping windows agree pixel for pixel and the shape reads as one lit object.
 */
final class Light {
   /** The picture's size; it is stretched (smoothly) over the shape, so a small one does. */
   static final int SIZE = 64;
   /** How dark the side turned away from the light gets, and how bright the shine is (0 to 1). */
   private static final double SHADE = 0.42;
   private static final double SHINE = 0.6;
   /** The light's direction, toward the upper left and the viewer, and the shine's halfway direction. */
   private static final double[] TOWARD = unit(-0.45, -0.55, 0.7);
   private static final double[] HALF = unit(TOWARD[0], TOWARD[1], TOWARD[2] + 1);
   static final BufferedImage IMAGE = picture();

   private Light() {
   }

   /** Draws the light stretched over box (in the window's coordinates); nothing for an empty box. */
   static void paint(Graphics g, int x, int y, int width, int height) {
      if (width <= 0 || height <= 0) {
         return;
      }
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(IMAGE, x, y, width, height, null);
   }

   /** The light at (u, v), each from -1 to 1 across the box, as {gray, alpha} from 0 to 255. */
   static int[] at(double u, double v) {
      double d = Math.sqrt(u * u + v * v);
      // On the dome inside the box's ellipse; past it, the dome's rim facing outward.
      double[] n = d < 1 ? new double[] {u, v, Math.sqrt(1 - d * d)} : new double[] {u / d, v / d, 0};
      double diffuse = Math.max(0, dot(n, TOWARD));
      double shade = SHADE * Math.pow(1 - diffuse, 1.3);
      double shine = SHINE * Math.pow(Math.max(0, dot(n, HALF)), 24);
      // White shine laid over black shade, as one see-through gray.
      double alpha = shine + shade * (1 - shine);
      int gray = alpha <= 0 ? 0 : (int)Math.round(255 * shine / alpha);
      return new int[] {gray, (int)Math.round(255 * alpha)};
   }

   private static BufferedImage picture() {
      BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
      WritableRaster raster = image.getRaster();
      for (int py = 0; py < SIZE; py++) {
         for (int px = 0; px < SIZE; px++) {
            int[] light = at(2 * (px + 0.5) / SIZE - 1, 2 * (py + 0.5) / SIZE - 1);
            raster.setPixel(px, py, new int[] {light[0], light[0], light[0], light[1]});
         }
      }
      return image;
   }

   private static double dot(double[] a, double[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   private static double[] unit(double x, double y, double z) {
      double length = Math.sqrt(x * x + y * y + z * z);
      return new double[] {x / length, y / length, z / length};
   }
}
