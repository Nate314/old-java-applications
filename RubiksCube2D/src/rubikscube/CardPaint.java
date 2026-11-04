package rubikscube;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * What the Teach me and Patterns cards share: their colors, words wrapped by hand (so they wrap the same on the
 * desktop and in the browser) and the turn chips, each in the color of the face to turn with an arrow for clockwise
 * or back.
 */
final class CardPaint {
   static final int CHIP = 18;
   static final int PITCH = 21;
   static final int MAX_CHIPS = 16;
   static final Color CARD = new Color(244, 247, 253);
   static final Color EDGE = new Color(206, 214, 232);
   static final Color INK = new Color(28, 32, 44);
   static final Color MUTED = new Color(104, 112, 132);
   static final Color DONE = new Color(46, 160, 90);
   static final Color NOW = new Color(40, 96, 220);
   static final Color LATER = new Color(214, 220, 234);

   private CardPaint() {
   }

   /** The card's rounded background. */
   static void card(Graphics2D g, int width, int height) {
      g.setColor(CARD);
      g.fillRoundRect(0, 0, width - 1, height - 1, 14, 14);
      g.setColor(EDGE);
      g.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);
   }

   /**
    * The chips, at most two rows of eight, scrolled so turn {@code now} shows: those before it faded, it ringed.
    * A {@code now} of -1 draws them all plain.
    */
   static void chips(Graphics2D g, int x, int y, List<String> colors, List<Boolean> clockwise, int now) {
      int count = colors.size();
      int first = Math.max(0, Math.min(now - 4, count - MAX_CHIPS));
      int last = Math.min(count, first + MAX_CHIPS);
      for (int i = first; i < last; i++) {
         int cx = x + (i - first) % 8 * PITCH;
         int cy = y + (i - first) / 8 * PITCH;
         String name = colors.get(i);
         Color face = CubeView3D.colorOf(name);
         boolean light = name.equals("white") || name.equals("yellow") || name.equals("orange");
         Color mark = light ? INK : Color.WHITE;
         if (i < now) {
            face = CubeView3D.mix(face, CARD, 0.7);
            mark = CubeView3D.mix(mark, CARD, 0.7);
         }
         if (i == now) {
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(cx - 2, cy - 2, CHIP + 3, CHIP + 3, 8, 8);
         }
         g.setColor(face);
         g.fillRoundRect(cx, cy, CHIP, CHIP, 6, 6);
         g.setStroke(new BasicStroke(1f));
         g.setColor(CubeView3D.mix(face, INK, 0.35));
         g.drawRoundRect(cx, cy, CHIP - 1, CHIP - 1, 6, 6);
         turnArrow(g, cx + CHIP / 2, cy + CHIP / 2, clockwise.get(i), mark);
      }
      if (count > MAX_CHIPS) {
         g.setFont(new Font("Dialog", Font.PLAIN, 10));
         g.setColor(MUTED);
         g.drawString((first > 0 ? "< " : "") + (last < count ? ">" : ""), x + 8 * PITCH - 2, y + 12);
      }
   }

   /** Rows the chips take: one per eight, at most two. */
   static int chipRows(int count) {
      return (Math.min(MAX_CHIPS, count) + 7) / 8;
   }

   /** A round arrow: clockwise or back (counterclockwise), its head at the top of the left or right side. */
   private static void turnArrow(Graphics2D g, int cx, int cy, boolean clockwise, Color color) {
      g.setColor(color);
      g.setStroke(new BasicStroke(1.6f));
      g.drawArc(cx - 5, cy - 5, 10, 10, 90, clockwise ? -270 : 270);
      int hx = clockwise ? cx - 5 : cx + 5;
      g.fillPolygon(new int[]{hx - 3, hx + 3, hx}, new int[]{cy + 1, cy + 1, cy - 4}, 3);
      g.setStroke(new BasicStroke(1f));
   }

   /** Draws text wrapped to the width, at most maxLines lines (the last ends in ... if cut); returns the last baseline. */
   static int wrap(Graphics2D g, String words, int x, int y, int width, int lineHeight, int maxLines) {
      List<String> lines = lines(g.getFontMetrics(), words, width);
      int baseline = y - lineHeight;
      for (int i = 0; i < lines.size() && i < maxLines; i++) {
         String line = lines.get(i);
         if (i == maxLines - 1 && lines.size() > maxLines) {
            line += "...";
         }
         baseline = y + i * lineHeight;
         g.drawString(line, x, baseline);
      }
      return baseline;
   }

   static List<String> lines(FontMetrics metrics, String words, int width) {
      List<String> lines = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (String word : words.split(" ")) {
         if (word.isEmpty()) {
            continue;
         }
         String tried = line.length() == 0 ? word : line + " " + word;
         if (line.length() > 0 && metrics.stringWidth(tried) > width) {
            lines.add(line.toString());
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(tried);
         }
      }
      if (line.length() > 0) {
         lines.add(line.toString());
      }
      return lines;
   }
}
