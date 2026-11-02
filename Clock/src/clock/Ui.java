package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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

   /** A timer's time left, rounded up to the second: "4:59", "60:00". */
   static String countdown(long millis) {
      return minutesSeconds((Math.max(0, millis) + 999) / 1000);
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
