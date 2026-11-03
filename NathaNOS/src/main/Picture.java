package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * A Paint picture: WIDTH x HEIGHT pixels, each one of the 16 PALETTE colors. It draws itself as runs of same-colored
 * pixels, so it stays sharp (no blurring) at any size: fat pixels in Paint, the whole screen as a wallpaper.
 */
final class Picture {
   static final int WIDTH = 200;
   static final int HEIGHT = 125;
   /** The colors of the classic Windows Paint palette. */
   static final Color[] PALETTE = {
      new Color(0, 0, 0), new Color(127, 127, 127), new Color(136, 0, 21), new Color(237, 28, 36),
      new Color(255, 127, 39), new Color(255, 242, 0), new Color(34, 177, 76), new Color(0, 162, 232),
      new Color(255, 255, 255), new Color(195, 195, 195), new Color(185, 122, 87), new Color(255, 174, 201),
      new Color(255, 201, 14), new Color(181, 230, 29), new Color(63, 72, 204), new Color(163, 73, 164)
   };
   static final byte BLACK = 0;
   static final byte WHITE = 8;

   final String name;
   final byte[] pixels;
   /** Goes up with every change, so cached drawings of the picture know when to redraw. */
   private int version;
   private BufferedImage thumbnail;
   private int thumbnailVersion = -1;

   Picture(String name) {
      this(name, blank());
   }

   Picture(String name, byte[] pixels) {
      this.name = name;
      this.pixels = pixels;
   }

   static byte[] blank() {
      byte[] pixels = new byte[WIDTH * HEIGHT];
      Arrays.fill(pixels, WHITE);
      return pixels;
   }

   int version() {
      return this.version;
   }

   void changed() {
      this.version++;
   }

   int get(int x, int y) {
      return this.pixels[y * WIDTH + x];
   }

   /** Sets one pixel; a point off the picture is ignored, so tools can draw past the edges. */
   void set(int x, int y, int color) {
      if (x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
         this.pixels[y * WIDTH + x] = (byte)color;
      }
   }

   boolean isBlank() {
      for (byte pixel : this.pixels) {
         if (pixel != WHITE) {
            return false;
         }
      }
      return true;
   }

   /** Draws the picture with its top-left corner at (x, y), each pixel scaleX by scaleY screen pixels. */
   void paint(Graphics g, double x, double y, double scaleX, double scaleY) {
      for (int row = 0; row < HEIGHT; row++) {
         int top = (int)Math.floor(y + row * scaleY);
         int height = (int)Math.floor(y + (row + 1) * scaleY) - top;
         if (height <= 0) {
            continue;
         }
         int start = 0;
         for (int column = 1; column <= WIDTH; column++) {
            if (column == WIDTH || this.pixels[row * WIDTH + column] != this.pixels[row * WIDTH + start]) {
               int left = (int)Math.floor(x + start * scaleX);
               int right = (int)Math.floor(x + column * scaleX);
               g.setColor(PALETTE[this.pixels[row * WIDTH + start]]);
               g.fillRect(left, top, right - left, height);
               start = column;
            }
         }
      }
   }

   /** The picture at one screen pixel per pixel, redrawn only after a change. */
   BufferedImage thumbnail() {
      if (this.thumbnail == null || this.thumbnailVersion != this.version) {
         BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
         Graphics g = image.createGraphics();
         this.paint(g, 0, 0, 1, 1);
         g.dispose();
         this.thumbnail = image;
         this.thumbnailVersion = this.version;
      }
      return this.thumbnail;
   }

   /**
    * The pixels as text: one run per same-colored stretch (in reading order), a capital letter for the color (A is
    * PALETTE[0]) followed by the run's length in base 36 when it is longer than 1. A blank picture is "Ijag".
    */
   String encode() {
      StringBuilder text = new StringBuilder();
      int start = 0;
      for (int i = 1; i <= this.pixels.length; i++) {
         if (i == this.pixels.length || this.pixels[i] != this.pixels[start]) {
            text.append((char)('A' + this.pixels[start]));
            if (i - start > 1) {
               text.append(Integer.toString(i - start, 36));
            }
            start = i;
         }
      }
      return text.toString();
   }

   /** The pixels encode() wrote, or null when the text is damaged or not exactly one picture long. */
   static byte[] decode(String text) {
      byte[] pixels = new byte[WIDTH * HEIGHT];
      int at = 0;
      int i = 0;
      while (i < text.length()) {
         int color = text.charAt(i) - 'A';
         if (color < 0 || color >= PALETTE.length) {
            return null;
         }
         int digits = ++i;
         while (i < text.length() && Character.digit(text.charAt(i), 36) >= 0 && !Character.isUpperCase(text.charAt(i))) {
            i++;
         }
         int length;
         try {
            length = i == digits ? 1 : Integer.parseInt(text.substring(digits, i), 36);
         } catch (NumberFormatException damaged) {
            return null;
         }
         if (length <= 0 || at + length > pixels.length) {
            return null;
         }
         Arrays.fill(pixels, at, at + length, (byte)color);
         at += length;
      }
      return at == pixels.length ? pixels : null;
   }
}
