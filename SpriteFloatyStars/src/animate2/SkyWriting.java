package animate2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mode 7, Sky writing: the stars spell a message in a 5 x 7 pixel font, wrapped onto as many lines as make the
 * letters biggest. Every star has a place in one of the letters' dots and glides to it, so typing a letter sends
 * stars swooping into it, and a pull or a fling scatters them until they find their letters again. With no message
 * the stars spread out over the sky.
 */
final class SkyWriting {
   static final int MAX_LENGTH = 40;
   static final String FIRST_MESSAGE = "HELLO";
   /** Each step a star closes this share of the way to its place, so it settles in about a second. */
   private static final double EASE_PER_STEP = 0.02;
   /** The message's colors from its left end to its right: gold, orange, rose, violet, sky blue. */
   private static final int[][] COLORS = {{255, 236, 160}, {255, 170, 90}, {240, 100, 140}, {170, 110, 240}, {120, 200, 255}};

   private final StarField field;
   /** Where the message is kept between runs, or null (Star Catcher's sky and the self-check keep nothing). */
   private final File file;
   private String text = FIRST_MESSAGE;
   private boolean writing;
   /** Each star's place, by its center (NaN: no place in the message, so it goes to its spot in the sky). */
   private double[] placeX = new double[0];
   private double[] placeY = new double[0];
   /** Each star's spot in the empty sky, its offset inside its dot and its twinkle, all as shares (0 to 1). */
   private double[] skyX = new double[0];
   private double[] skyY = new double[0];
   private double[] wobble = new double[0];
   private String laidOut;
   private int laidWidth;
   private int laidHeight;
   private int laidStars;
   private double dot;
   private double left;
   private double right;
   /** Where the caret goes: just after the last letter, on the last line. */
   private double caretX;
   private double caretY;

   SkyWriting(StarField field, File file) {
      this.field = field;
      this.file = file;
      if (file != null) {
         try (Reader reader = new FileReader(file)) {
            StringBuilder saved = new StringBuilder();
            for (int c; (c = reader.read()) >= 0;) {
               saved.append((char)c);
            }
            this.text = clean(saved.toString());
         } catch (IOException e) {
            // No message saved yet: the first one.
         }
      }
   }

   String text() {
      return this.text;
   }

   boolean writing() {
      return this.writing;
   }

   /** Enter: the keyboard writes the message until Enter (or Esc) again, which keeps it. */
   void startWriting() {
      this.writing = true;
   }

   void stopWriting() {
      this.writing = false;
      if (this.file != null) {
         try (Writer writer = new FileWriter(this.file)) {
            writer.write(this.text);
         } catch (IOException e) {
            // Could not save: the message lasts until the window closes.
         }
      }
   }

   /** A key typed while writing: a letter the font has goes on the end, Backspace takes the last one off. */
   void type(char c) {
      if (c == '\b') {
         this.text = this.text.isEmpty() ? "" : this.text.substring(0, this.text.length() - 1);
      } else if (this.text.length() < MAX_LENGTH && c >= ' ' && PixelFont.has(c)) {
         this.text = clean(this.text + c);
      }
   }

   /** Upper case, only what the font can draw, no more than MAX_LENGTH, and never two spaces in a row. */
   static String clean(String raw) {
      StringBuilder out = new StringBuilder();
      for (char c : raw.toUpperCase().toCharArray()) {
         boolean doubled = c == ' ' && (out.length() == 0 || out.charAt(out.length() - 1) == ' ');
         if (out.length() < MAX_LENGTH && PixelFont.has(c) && !doubled) {
            out.append(c);
         }
      }
      return out.toString();
   }

   /**
    * The words of {@code text} on lines, the line length picked to make the dots biggest in a width x height field.
    * A trailing space is kept on the last line, so the caret moves on while writing.
    */
   static List<String> wrap(String text, double width, double height) {
      String[] words = text.trim().split(" ");
      int longest = 1;
      for (String word : words) {
         longest = Math.max(longest, word.length());
      }
      List<String> best = null;
      double bestDot = -1;
      for (int limit = longest; limit <= Math.max(longest, text.length()); limit++) {
         List<String> lines = new ArrayList<>();
         String line = "";
         for (String word : words) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > limit) {
               lines.add(line);
               line = word;
            } else {
               line = line.isEmpty() ? word : line + " " + word;
            }
         }
         lines.add(line);
         double size = dotSize(lines, width, height);
         if (size > bestDot) {
            best = lines;
            bestDot = size;
         }
      }
      if (text.endsWith(" ") && !text.trim().isEmpty()) {
         best.set(best.size() - 1, best.get(best.size() - 1) + " ");
      }
      return best;
   }

   /** The dot size that fits these lines into 90% of the width and 70% of the height (a letter and a gap are 6 dots wide, a line 9 tall). */
   static double dotSize(List<String> lines, double width, double height) {
      int columns = 1;
      for (String line : lines) {
         columns = Math.max(columns, line.length() * 6 - 1);
      }
      return Math.min(0.9 * width / columns, 0.7 * height / (lines.size() * 9 - 2));
   }

   /** Gives every star a place in the message, again whenever the message, the field or the number of stars changes. */
   private void layOut() {
      int width = this.field.fieldWidth();
      int height = this.field.fieldHeight();
      int stars = this.field.starCount;
      if (this.text.equals(this.laidOut) && width == this.laidWidth && height == this.laidHeight && stars == this.laidStars) {
         return;
      }
      this.laidOut = this.text;
      this.laidWidth = width;
      this.laidHeight = height;
      this.laidStars = stars;
      if (this.skyX.length < stars) {
         int old = this.skyX.length;
         this.skyX = Arrays.copyOf(this.skyX, Math.max(16, stars * 2));
         this.skyY = Arrays.copyOf(this.skyY, this.skyX.length);
         this.wobble = Arrays.copyOf(this.wobble, this.skyX.length);
         for (int star = old; star < this.skyX.length; star++) {
            this.skyX[star] = Math.random();
            this.skyY[star] = Math.random();
            this.wobble[star] = Math.random();
         }
      }
      this.placeX = new double[stars];
      this.placeY = new double[stars];
      Arrays.fill(this.placeX, Double.NaN);
      Arrays.fill(this.placeY, Double.NaN);

      // The lit dots, left to right.
      List<double[]> dots = new ArrayList<>();
      List<String> lines = this.text.isEmpty() ? Arrays.asList("") : wrap(this.text, width, height);
      this.dot = dotSize(this.text.isEmpty() ? Arrays.asList(FIRST_MESSAGE) : lines, width, height);
      // Centered in the top 88% of the field, clear of the help along the bottom.
      double top = (0.88 * height - (lines.size() * 9 - 2) * this.dot) / 2.0;
      this.left = width;
      this.right = 0;
      for (int row = 0; row < lines.size(); row++) {
         String line = lines.get(row);
         double lineLeft = (width - Math.max(0, line.trim().length() * 6 - 1) * this.dot) / 2.0;
         double lineTop = top + row * 9 * this.dot;
         for (int i = 0; i < line.length(); i++) {
            for (int dotRow = 0; dotRow < PixelFont.HEIGHT; dotRow++) {
               for (int column = 0; column < PixelFont.WIDTH; column++) {
                  if (PixelFont.lit(line.charAt(i), column, dotRow)) {
                     double x = lineLeft + (i * 6 + column + 0.5) * this.dot;
                     dots.add(new double[]{x, lineTop + (dotRow + 0.5) * this.dot});
                     this.left = Math.min(this.left, x);
                     this.right = Math.max(this.right, x);
                  }
               }
            }
         }
         this.caretX = lineLeft + line.length() * 6 * this.dot;
         this.caretY = lineTop;
      }
      if (dots.isEmpty()) {
         return;
      }
      dots.sort((a, b) -> Double.compare(a[0], b[0]));

      // Stars go to the dots in the order they stand from left to right, so they cross each other's paths as little as
      // they can, and each dot gets its share of the stars.
      Integer[] order = new Integer[stars];
      for (int star = 0; star < stars; star++) {
         order[star] = star;
      }
      Arrays.sort(order, (a, b) -> Double.compare(this.field.centerX(a), this.field.centerX(b)));
      for (int k = 0; k < stars; k++) {
         int star = order[k];
         double[] at = dots.get((int)((long)k * dots.size() / stars));
         double angle = this.wobble[star] * 2.0 * Math.PI * 7.0;
         double reach = 0.42 * this.dot * Math.sqrt((this.wobble[star] * 13.0) % 1.0);
         this.placeX[star] = at[0] + reach * Math.cos(angle);
         this.placeY[star] = at[1] + reach * Math.sin(angle);
      }
   }

   /** Where a star is headed now: its place in the message, swaying a little, or its spot in the sky. */
   private double[] goal(int star) {
      double clock = this.field.clock();
      if (Double.isNaN(this.placeX[star])) {
         return new double[]{this.skyX[star] * this.field.fieldWidth(), this.skyY[star] * this.field.fieldHeight()};
      }
      double sway = 0.12 * this.dot;
      double phase = this.wobble[star] * 40.0;
      return new double[]{this.placeX[star] + sway * Math.cos(clock * 1.3 + phase), this.placeY[star] + sway * Math.sin(clock * 1.7 + phase)};
   }

   /** Moves every star on by {@code steps} toward where it is headed (the pointer's kicks have already moved it). */
   void move(double steps) {
      this.layOut();
      double share = 1.0 - Math.pow(1.0 - EASE_PER_STEP, steps);
      for (int star = 0; star < this.field.starCount; star++) {
         double[] goal = this.goal(star);
         double x = this.field.centerX(star);
         double y = this.field.centerY(star);
         double nextX = x + (goal[0] - x) * share;
         double nextY = y + (goal[1] - y) * share;
         this.field.moveStar(star, nextX, nextY);
         // Star music: a star coming within half a dot of its place rings a note, higher further along the message.
         double near = 0.5 * this.dot;
         if (!Double.isNaN(this.placeX[star]) && Math.hypot(goal[0] - x, goal[1] - y) > near && Math.hypot(goal[0] - nextX, goal[1] - nextY) <= near) {
            this.field.ring((this.placeX[star] - this.left) / Math.max(1.0, this.right - this.left), 0.6, nextX, nextY);
         }
      }
   }

   /** True once every star in the message is within {@code slack} dots of its place. */
   boolean settled(double slack) {
      this.layOut();
      for (int star = 0; star < this.field.starCount; star++) {
         if (!Double.isNaN(this.placeX[star])
            && Math.hypot(this.field.centerX(star) - this.placeX[star], this.field.centerY(star) - this.placeY[star]) > slack * this.dot) {
            return false;
         }
      }
      return true;
   }

   boolean hasPlace(int star) {
      this.layOut();
      return !Double.isNaN(this.placeX[star]);
   }

   double placeX(int star) {
      this.layOut();
      return this.placeX[star];
   }

   double placeY(int star) {
      this.layOut();
      return this.placeY[star];
   }

   /** Stars small enough for their dots, colored along the message from gold to sky blue, each twinkling. */
   void drawStars(Graphics2D g2) {
      this.layOut();
      int size = Math.max(2, Math.min(this.field.starSize(), (int)(this.dot * 0.55)));
      double clock = this.field.clock();
      double span = Math.max(1.0, this.right - this.left);
      for (int star = 0; star < this.field.starCount; star++) {
         double x = this.field.centerX(star);
         double y = this.field.centerY(star);
         double along = Double.isNaN(this.placeX[star]) ? this.skyX[star] : (this.placeX[star] - this.left) / span;
         double at = Math.max(0.0, Math.min(1.0, along)) * (COLORS.length - 1);
         int low = Math.min(COLORS.length - 2, (int)at);
         double t = at - low;
         double glow = 0.7 + 0.3 * Math.sin(clock * 3.0 + this.wobble[star] * 60.0);
         g2.setColor(new Color(
            (int)((COLORS[low][0] + (COLORS[low + 1][0] - COLORS[low][0]) * t) * glow),
            (int)((COLORS[low][1] + (COLORS[low + 1][1] - COLORS[low][1]) * t) * glow),
            (int)((COLORS[low][2] + (COLORS[low + 1][2] - COLORS[low][2]) * t) * glow)));
         g2.fillOval((int)(x - size / 2.0), (int)(y - size / 2.0), size, size);
      }
   }

   /** While writing, a blinking caret after the last letter. */
   void drawCaret(Graphics2D g2) {
      if (this.writing && (int)(this.field.clock() * 2.0) % 2 == 0) {
         this.layOut();
         int width = Math.max(2, (int)(this.dot * 0.6));
         g2.setColor(new Color(255, 255, 255, 200));
         g2.fillRect((int)this.caretX, (int)this.caretY, width, (int)(this.dot * PixelFont.HEIGHT));
      }
   }
}
