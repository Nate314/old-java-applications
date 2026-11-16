package clock;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

/**
 * The flip clock face: hours and minutes on big split-flap cards and the seconds on small ones, the way a bedside
 * flip clock shows the time. When a digit changes, the top half of its old card falls forward over the hinge and
 * shows the new card's bottom half. Each half card is drawn once into its own image, since the image's edge is the
 * only clipping the browser build has.
 */
final class FlipFace {
   static final Color CARD_TOP = new Color(46, 46, 52);
   static final Color CARD_BOTTOM = new Color(34, 34, 39);
   static final Color FIGURE = new Color(244, 242, 234);
   static final Color HINGE = new Color(8, 8, 10);
   /** How long a big card's flap takes to fall; the seconds' small cards fall faster. */
   static final int FALL_MS = 560;
   static final int SMALL_FALL_MS = 420;
   /** The half-card images are drawn at twice the size they are shown, to stay sharp on high-density screens. */
   private static final int SHARP = 2;
   /** The six cards: hour tens and ones, minute tens and ones (big), second tens and ones (small). */
   private final char[] shown = new char[6];
   private final char[] from = new char[6];
   private final long[] since = new long[6];
   private Cards big;
   private Cards small;

   /** The six figures for a time: the hour without its leading zero (a blank card) in 12-hour time, as flip clocks do. */
   static char[] figures(int hours, int minutes, int seconds, boolean twentyFour) {
      int hour = twentyFour ? hours : hours % 12 == 0 ? 12 : hours % 12;
      return new char[]{hour < 10 && !twentyFour ? ' ' : (char)('0' + hour / 10), (char)('0' + hour % 10),
            (char)('0' + minutes / 10), (char)('0' + minutes % 10), (char)('0' + seconds / 10), (char)('0' + seconds % 10)};
   }

   /**
    * How far a flap that started falling {@code elapsed} ms ago has got: from 1 (the old top half standing up) through 0
    * (edge on, over the hinge) to -1 (the new bottom half lying down, done). It falls faster as it goes, like a card
    * dropping under its own weight.
    */
   static double fold(long elapsed, int fallMs) {
      double p = Math.max(0, Math.min(1, elapsed / (double)fallMs));
      return Math.cos(Math.PI * p * p);
   }

   /** Points the cards at a time; a card whose figure changed starts to fall. The first call just sets them. */
   void set(char[] figures, long now) {
      for (int i = 0; i < 6; i++) {
         if (this.shown[i] == 0) {
            this.from[i] = figures[i];
            this.shown[i] = figures[i];
         } else if (this.shown[i] != figures[i]) {
            this.from[i] = this.shown[i];
            this.shown[i] = figures[i];
            this.since[i] = now;
         }
      }
   }

   void paint(Graphics2D g, Rectangle area, Main clock, String alarmLine, long now) {
      boolean twentyFour = clock.state.twentyFour;
      this.set(figures(clock.hours, clock.minutes, clock.seconds, twentyFour), now);
      boolean still = Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));

      // Two pairs of big cards and a pair of small ones: 4 + 2 * 0.55 card widths, 3 small gaps and 2 wide ones.
      int pad = Math.max(8, area.width / 20);
      int width = (int)Math.min((area.width - 2 * pad) / 5.68, area.height * 0.5 / 1.4);
      int height = (int)(width * 1.4) / 2 * 2;
      int smallWidth = width * 55 / 100;
      int smallHeight = height / 2 / 2 * 2;
      int gap = Math.max(2, width / 16);
      int wide = width / 4;
      int total = 4 * width + 2 * smallWidth + 3 * gap + 2 * wide;
      int x = area.x + (area.width - total) / 2;
      int top = area.y + (area.height - height) * 42 / 100;
      if (this.big == null || this.big.width != width || this.big.height != height) {
         this.big = new Cards(width, height);
      }
      if (this.small == null || this.small.width != smallWidth || this.small.height != smallHeight) {
         this.small = new Cards(smallWidth, smallHeight);
      }

      int[] lefts = {x, x + width + gap, x + 2 * width + gap + wide, x + 3 * width + 2 * gap + wide,
            x + 4 * width + 2 * gap + 2 * wide, x + 4 * width + 2 * gap + 2 * wide + smallWidth + gap};
      for (int i = 0; i < 6; i++) {
         boolean bigCard = i < 4;
         Cards cards = bigCard ? this.big : this.small;
         int cardTop = bigCard ? top : top + height - smallHeight;
         double fold = still ? -1 : fold(now - this.since[i], bigCard ? FALL_MS : SMALL_FALL_MS);
         this.card(g, cards, lefts[i], cardTop, this.from[i], this.shown[i], fold);
      }

      // The colon between the hours and the minutes.
      int dot = Math.max(3, width / 9);
      int colonX = lefts[1] + width + (gap + wide - dot) / 2;
      g.setColor(Ui.INK);
      g.fillOval(colonX, top + height * 3 / 10 - dot / 2, dot, dot);
      g.fillOval(colonX, top + height * 7 / 10 - dot / 2, dot, dot);

      // Over the seconds: am or pm in 12-hour time.
      int label = Math.max(10, smallHeight * 2 / 5);
      if (!twentyFour) {
         g.setColor(Ui.INK);
         Ui.line(g, clock.hours < 12 ? "AM" : "PM", Ui.LABELS,
               new Rectangle(lefts[4], top + height - smallHeight - label * 3 / 2, 2 * smallWidth + gap, label));
      }

      // Under the cards: the day and date, and when the alarm rings.
      int lineHeight = Math.max(12, height / 5);
      int under = top + height + lineHeight / 2;
      g.setColor(Ui.INK);
      String date = clock.dayNames[clock.dayOfWeek] + ", " + (clock.month + 1) + "/" + clock.dayOfMonth + "/" + clock.year;
      Ui.line(g, date, Ui.LABELS, new Rectangle(x, under, total, lineHeight));
      if (alarmLine != null) {
         g.setColor(ClockFace.BRASS);
         Ui.line(g, alarmLine, Ui.LABELS, new Rectangle(x, under + lineHeight * 3 / 2, total, lineHeight * 4 / 5));
      }
   }

   /** One card: the new top half and the old bottom half standing, and the falling flap between them. */
   private void card(Graphics2D g, Cards cards, int x, int y, char was, char now, double fold) {
      int half = cards.height / 2;
      int hinge = y + half;
      g.drawImage(cards.top(now), x, y, cards.width, half, null);
      g.drawImage(cards.bottom(fold > -1 ? was : now), x, hinge, cards.width, half, null);
      if (fold > 0) {
         int drop = (int)Math.round(half * fold);
         if (drop > 0) {
            g.drawImage(cards.top(was), x, hinge - drop, cards.width, drop, null);
            g.setColor(new Color(0, 0, 0, (int)(110 * (1 - fold))));
            g.fillRect(x, hinge - drop, cards.width, drop);
         }
      } else if (fold > -1) {
         int drop = (int)Math.round(half * -fold);
         if (drop > 0) {
            g.drawImage(cards.bottom(now), x, hinge, cards.width, drop, null);
            g.setColor(new Color(255, 255, 255, (int)(40 * (1 + fold))));
            g.fillRect(x, hinge, cards.width, drop);
         }
      }
      g.setColor(HINGE);
      int line = Math.max(1, cards.height / 70);
      g.fillRect(x, hinge - line / 2, cards.width, line);
      g.fillRect(x - 1, hinge - line * 2, Math.max(2, cards.width / 30), line * 4);
      g.fillRect(x + cards.width - Math.max(2, cards.width / 30) + 1, hinge - line * 2, Math.max(2, cards.width / 30), line * 4);
   }

   /** The half-card images for one card size: a blank card and the ten digits, each top and bottom, drawn when first asked. */
   private static final class Cards {
      final int width;
      final int height;
      private final BufferedImage[] tops = new BufferedImage[11];
      private final BufferedImage[] bottoms = new BufferedImage[11];

      Cards(int width, int height) {
         this.width = width;
         this.height = height;
      }

      BufferedImage top(char figure) {
         int i = index(figure);
         if (this.tops[i] == null) {
            this.tops[i] = this.draw(figure, true);
         }
         return this.tops[i];
      }

      BufferedImage bottom(char figure) {
         int i = index(figure);
         if (this.bottoms[i] == null) {
            this.bottoms[i] = this.draw(figure, false);
         }
         return this.bottoms[i];
      }

      private static int index(char figure) {
         return figure >= '0' && figure <= '9' ? figure - '0' : 10;
      }

      /** The whole card drawn into an image half its height, so only the top (or bottom) half lands in it. */
      private BufferedImage draw(char figure, boolean topHalf) {
         int w = this.width * SHARP;
         int h = this.height * SHARP;
         BufferedImage image = new BufferedImage(w, h / 2, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g = image.createGraphics();
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         int y = topHalf ? 0 : -h / 2;
         int arc = Math.max(4, w / 6);
         g.setColor(topHalf ? CARD_TOP : CARD_BOTTOM);
         g.fillRoundRect(0, y, w, h, arc, arc);
         if (figure != ' ') {
            g.setColor(FIGURE);
            Ui.center(g, String.valueOf(figure), new Font(Ui.LABELS, Font.BOLD, h * 88 / 100), new Rectangle(0, y, w, h));
         }
         g.dispose();
         return image;
      }
   }
}
