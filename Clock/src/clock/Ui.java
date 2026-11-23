package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Date;

/** Colors, fonts, buttons and time formats shared by the tabs. They keep the original window's cyan, black and red. */
final class Ui {
   static final Color BACKGROUND = new Color(0, 255, 255);
   static final Color INK = new Color(0, 0, 0);
   static final Color FACE = new Color(255, 255, 255);
   static final Color HAND = new Color(255, 0, 0);
   static final Color WEDGE = new Color(230, 30, 40);
   static final Color ALARM = new Color(255, 170, 190);
   static final Color FASTEST = new Color(0, 120, 40);
   static final Color SLOWEST = new Color(190, 0, 20);
   static final Color FAINT = new Color(0, 110, 120);
   static final String DIGITS = "Times New Roman";
   static final String LABELS = "SansSerif";

   /** The rim's chrome, lit from the top left, and the soft shadow a dial or hand casts down and to the right. */
   static final Color CHROME_LIGHT = new Color(252, 253, 255);
   static final Color CHROME_DARK = new Color(64, 74, 88);
   static final Color SHADOW = new Color(0, 40, 50, 64);
   private static final Color FACE_LOW = new Color(222, 229, 234);

   private Ui() {
   }

   /** The biggest font of the family, at most {@code maxSize}, that fits {@code text} in the box. */
   static Font fit(Graphics2D g, String family, String text, int width, int height, int maxSize) {
      int size = Math.max(1, Math.min(maxSize, height));
      Font font = new Font(family, Font.BOLD, size);
      while (size > 6 && g.getFontMetrics(font).stringWidth(text) > width) {
         size--;
         font = new Font(family, Font.BOLD, size);
      }
      return font;
   }

   /** Draws the text centered in the box, in the given font. */
   static void center(Graphics2D g, String text, Font font, Rectangle box) {
      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics(font);
      int x = box.x + (box.width - metrics.stringWidth(text)) / 2;
      int y = box.y + (box.height + metrics.getAscent() - metrics.getDescent()) / 2;
      g.drawString(text, x, y);
   }

   /** Draws a fitted line of text centered in the box. */
   static void line(Graphics2D g, String text, String family, Rectangle box) {
      center(g, text, fit(g, family, text, box.width, box.height, box.height), box);
   }

   /** A rounded button: black with cyan text when {@code primary} or selected, white with black text otherwise. */
   static void button(Graphics2D g, Rectangle box, String label, boolean primary) {
      int arc = Math.max(6, box.height / 2);
      g.setColor(primary ? INK : FACE);
      g.fillRoundRect(box.x, box.y, box.width, box.height, arc, arc);
      g.setColor(INK);
      g.setStroke(new BasicStroke(Math.max(1, box.height / 16f)));
      g.drawRoundRect(box.x, box.y, box.width, box.height, arc, arc);
      g.setColor(primary ? BACKGROUND : INK);
      Rectangle inner = new Rectangle(box.x + 4, box.y + box.height / 5, box.width - 8, box.height * 3 / 5);
      line(g, label, LABELS, inner);
   }

   /**
    * Fills a ring band between two radii, from {@code start} turns clockwise from 12 o'clock, {@code sweep} turns
    * long (one turn is the whole circle).
    */
   static void band(Graphics2D g, int cx, int cy, int outer, int inner, double start, double sweep) {
      if (sweep <= 0) {
         return;
      }
      int steps = Math.max(2, (int) Math.ceil(Math.min(1, sweep) * 120));
      int[] xs = new int[2 * (steps + 1)];
      int[] ys = new int[2 * (steps + 1)];
      for (int i = 0; i <= steps; i++) {
         double angle = 2 * Math.PI * (start + sweep * i / steps);
         xs[i] = cx + (int) Math.round(outer * Math.sin(angle));
         ys[i] = cy - (int) Math.round(outer * Math.cos(angle));
         xs[xs.length - 1 - i] = cx + (int) Math.round(inner * Math.sin(angle));
         ys[ys.length - 1 - i] = cy - (int) Math.round(inner * Math.cos(angle));
      }
      g.fillPolygon(xs, ys, xs.length);
   }

   /**
    * A dial's face as an object on the window: its shadow cast down and to the right, then the face, white shading to
    * pale grey at the bottom as a domed dial catches the light.
    */
   static void face(Graphics2D g, int cx, int cy, int r) {
      int drop = Math.max(2, r / 22);
      g.setColor(SHADOW);
      g.fillOval(cx - r + drop / 2, cy - r + drop, 2 * r, 2 * r);
      g.setPaint(new GradientPaint(cx, cy - r / 3, FACE, cx, cy + r, FACE_LOW));
      g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
      g.setColor(FACE);
   }

   /** The dial's rim: a chrome ring {@code width} wide centred on radius {@code r}, with a shaded lip inside it. */
   static void rim(Graphics2D g, int cx, int cy, int r, int width) {
      g.setStroke(new BasicStroke(width));
      g.setPaint(new GradientPaint(cx - r, cy - r, CHROME_LIGHT, cx + r / 2, cy + r, CHROME_DARK));
      g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
      int lip = r - width / 2;
      g.setStroke(new BasicStroke(Math.max(1f, width / 4f)));
      g.setPaint(new GradientPaint(cx - r, cy - r, CHROME_DARK, cx + r / 2, cy + r, CHROME_LIGHT));
      g.drawOval(cx - lip, cy - lip, 2 * lip, 2 * lip);
      int edge = r + width / 2;
      g.setColor(INK);
      g.setStroke(new BasicStroke(1f));
      g.drawOval(cx - edge, cy - edge, 2 * edge, 2 * edge);
   }

   /** The glass over a dial: a glint across its upper half, fading out towards the middle. */
   static void glass(Graphics2D g, int cx, int cy, int r) {
      g.setPaint(new GradientPaint(cx, cy - r, new Color(255, 255, 255, 120), cx, cy - r / 8, new Color(255, 255, 255, 0)));
      g.fillOval(cx - r * 4 / 5, cy - r * 23 / 25, r * 8 / 5, r * 4 / 5);
      g.setColor(INK);
   }

   /** Splits {@code box} across into {@code count} equal parts with {@code gap} pixels between them. */
   static Rectangle[] row(Rectangle box, int count, int gap) {
      Rectangle[] parts = new Rectangle[count];
      int width = (box.width - gap * (count - 1)) / count;
      for (int i = 0; i < count; i++) {
         parts[i] = new Rectangle(box.x + i * (width + gap), box.y, width, box.height);
      }
      return parts;
   }

   static Rectangle hit(Rectangle[] boxes, int x, int y) {
      if (boxes != null) {
         for (Rectangle box : boxes) {
            if (box != null && box.contains(x, y)) {
               return box;
            }
         }
      }
      return null;
   }

   static int indexOf(Rectangle[] boxes, Rectangle box) {
      for (int i = 0; boxes != null && i < boxes.length; i++) {
         if (boxes[i] == box) {
            return i;
         }
      }
      return -1;
   }

   static String twoDigits(long value) {
      return value < 10 ? "0" + value : String.valueOf(value);
   }

   /** A timer's time left, rounded up to the second: "4:59", "1:00:00". */
   static String countdown(long millis) {
      long seconds = (Math.max(0, millis) + 999) / 1000;
      return seconds >= 3600 ? seconds / 3600 + ":" + twoDigits(seconds / 60 % 60) + ":" + twoDigits(seconds % 60) : minutesSeconds(seconds);
   }

   /** Whole seconds as minutes and seconds: "4:59". */
   static String minutesSeconds(long seconds) {
      return seconds / 60 + ":" + twoDigits(seconds % 60);
   }

   /** A stopwatch time to the hundredth: "0:07.42", "1:02:03.04". */
   static String stopwatch(long millis) {
      long hundredths = Math.max(0, millis) / 10;
      long seconds = hundredths / 100;
      String rest = twoDigits(seconds % 60) + "." + twoDigits(hundredths % 100);
      long minutes = seconds / 60;
      return minutes >= 60 ? minutes / 60 + ":" + twoDigits(minutes % 60) + ":" + rest : minutes + ":" + rest;
   }

   /** A gap between two times, to the hundredth: "1.20 s" under a minute, else as the stopwatch shows it. */
   static String seconds(long millis) {
      long hundredths = Math.max(0, millis) / 10;
      return hundredths < 6000 ? hundredths / 100 + "." + twoDigits(hundredths % 100) + " s" : stopwatch(millis);
   }

   /** A wall clock time in local time: "3:42 pm" or "15:42". */
   @SuppressWarnings("deprecation")
   static String wallTime(long epochMillis, boolean twentyFour) {
      // Date's getters read local time on the desktop and in the browser, where Calendar is UTC.
      Date date = new Date(epochMillis);
      int hours = date.getHours();
      String minutes = twoDigits(date.getMinutes());
      if (twentyFour) {
         return twoDigits(hours) + ":" + minutes;
      }
      return (hours % 12 == 0 ? 12 : hours % 12) + ":" + minutes + (hours < 12 ? " am" : " pm");
   }
}
