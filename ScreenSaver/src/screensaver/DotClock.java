package screensaver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Date;
import java.util.Random;
import java.util.function.Supplier;

/**
 * The time in green dots, like the original's circles: a dot grows in when a digit changes, a row of 60 small dots
 * fills with the seconds, and the date sits underneath. The whole clock drifts slowly round the screen, so nothing
 * stays in one place, which is what a screen saver is for.
 */
class DotClock implements Saver {
   /** Digits 0 to 9, three dots wide and five high, one string per row. */
   static final String[][] DIGITS = {
      {"###", "#.#", "#.#", "#.#", "###"},
      {".#.", "##.", ".#.", ".#.", "###"},
      {"###", "..#", "###", "#..", "###"},
      {"###", "..#", "###", "..#", "###"},
      {"#.#", "#.#", "###", "..#", "..#"},
      {"###", "#..", "###", "..#", "###"},
      {"###", "#..", "###", "#.#", "###"},
      {"###", "..#", "..#", "..#", "..#"},
      {"###", "#.#", "###", "#.#", "###"},
      {"###", "#.#", "###", "..#", "###"},
   };
   /** Columns: four digits of three, a colon of one, and a one-column gap between each. */
   static final int COLUMNS = 17;
   private static final double GROW_MILLIS = 350;
   private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
   private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August",
      "September", "October", "November", "December"};

   private final Supplier<Date> now;
   private int width;
   private int height;
   private int dot;
   private int clockWidth;
   private int clockHeight;
   private double x;
   private double y;
   private double dx;
   private double dy;
   /** [column][row]: whether the dot is lit, how long it has been lit, and its shade of green. */
   private boolean[][] lit = new boolean[COLUMNS][5];
   private double[][] litFor = new double[COLUMNS][5];
   private int[][] shade = new int[COLUMNS][5];
   private Font font;

   /** Reads the time from this; Date's getters give local time on the desktop and in the browser. */
   DotClock(Supplier<Date> now) {
      this.now = now;
   }

   @Override
   public String name() {
      return "Dot clock";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.width = width;
      this.height = height;
      this.dot = (int)Math.max(4, Math.min(width * 0.6 / COLUMNS, height * 0.3 / 5));
      this.font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(12, this.dot * 2 / 3));
      this.clockWidth = COLUMNS * this.dot;
      // Five rows of digits, a gap, the seconds row, then the date.
      this.clockHeight = this.dot * 5 + this.dot + this.dot / 2 + this.font.getSize() * 2;
      this.x = random.nextDouble() * Math.max(1, width - this.clockWidth);
      this.y = random.nextDouble() * Math.max(1, height - this.clockHeight);
      double speed = Math.max(width, height) / 60000.0;
      this.dx = speed * (random.nextBoolean() ? 1 : -1);
      this.dy = speed * 0.7 * (random.nextBoolean() ? 1 : -1);
      for (int c = 0; c < COLUMNS; c++) {
         for (int r = 0; r < 5; r++) {
            this.lit[c][r] = false;
            this.shade[c][r] = random.nextInt(255);
         }
      }
   }

   /** The "H:MM" (12-hour clock) characters, five of them, a space for the missing tens of hours. */
   static String face(Date date) {
      int hours = date.getHours() % 12;
      if (hours == 0) {
         hours = 12;
      }
      int minutes = date.getMinutes();
      return (hours < 10 ? " " : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
   }

   /** Whether the dot at this column and row is lit for this face. */
   static boolean dotOn(String face, int column, int row, boolean colonOn) {
      int start = 0;
      for (int i = 0; i < face.length(); i++) {
         char ch = face.charAt(i);
         int w = ch == ':' ? 1 : 3;
         if (column >= start && column < start + w) {
            if (ch == ':') {
               return colonOn && (row == 1 || row == 3);
            }
            return ch != ' ' && DIGITS[ch - '0'][row].charAt(column - start) == '#';
         }
         start += w + 1;
      }
      return false;
   }

   /** Whether a dot belongs here at all: not in the gaps between characters, and only the colon's two dots. */
   static boolean hasDot(int column, int row) {
      if (column == 8) {
         return row == 1 || row == 3;
      }
      return column != 3 && column != 7 && column != 9 && column != 13;
   }

   @Override
   public void tick(double millis) {
      this.x += this.dx * millis;
      this.y += this.dy * millis;
      if (this.x < 0 || this.x + this.clockWidth > this.width) {
         this.dx = -this.dx;
         this.x = Math.max(0, Math.min(this.width - this.clockWidth, this.x));
      }
      if (this.y < 0 || this.y + this.clockHeight > this.height) {
         this.dy = -this.dy;
         this.y = Math.max(0, Math.min(this.height - this.clockHeight, this.y));
      }
      Date date = this.now.get();
      String face = face(date);
      boolean colonOn = date.getTime() % 1000 < 500;
      for (int c = 0; c < COLUMNS; c++) {
         for (int r = 0; r < 5; r++) {
            boolean on = dotOn(face, c, r, colonOn);
            if (on && !this.lit[c][r]) {
               this.litFor[c][r] = 0;
            }
            this.lit[c][r] = on;
            this.litFor[c][r] += millis;
         }
      }
   }

   @Override
   public void paint(Graphics2D g) {
      int left = (int)this.x;
      int top = (int)this.y;
      for (int c = 0; c < COLUMNS; c++) {
         for (int r = 0; r < 5; r++) {
            int cx = left + c * this.dot + this.dot / 2;
            int cy = top + r * this.dot + this.dot / 2;
            int size = this.dot - Math.max(2, this.dot / 8);
            if (this.lit[c][r]) {
               double grown = Math.min(1, this.litFor[c][r] / GROW_MILLIS);
               int s = (int)(size * grown);
               g.setColor(new Color(this.shade[c][r], 255, this.shade[c][r]));
               g.fillOval(cx - s / 2, cy - s / 2, s, s);
            } else if (hasDot(c, r)) {
               g.setColor(new Color(0, 34, 0));
               g.fillOval(cx - size / 2, cy - size / 2, size, size);
            }
         }
      }
      Date date = this.now.get();
      int seconds = date.getSeconds();
      int small = Math.max(2, this.clockWidth / 60);
      int rowY = top + this.dot * 5 + this.dot / 2;
      for (int s = 0; s < 60; s++) {
         g.setColor(s <= seconds ? new Color(120, 255, 120) : new Color(0, 40, 0));
         int d = Math.max(2, small - 2);
         g.fillOval(left + s * this.clockWidth / 60, rowY, d, d);
      }
      g.setFont(this.font);
      FontMetrics metrics = g.getFontMetrics();
      String line = DAYS[date.getDay()] + ", " + MONTHS[date.getMonth()] + " " + date.getDate()
         + (date.getHours() < 12 ? " \u00b7 AM" : " \u00b7 PM");
      g.setColor(new Color(150, 220, 150));
      g.drawString(line, left + (this.clockWidth - metrics.stringWidth(line)) / 2, rowY + small + this.font.getSize() * 3 / 2);
   }
}
