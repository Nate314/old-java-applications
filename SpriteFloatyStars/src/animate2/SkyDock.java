package animate2;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The row of buttons along the bottom of a stars' window: the sky's name between a previous and a next arrow, then
 * Trails, Music, Burst, Write (in Sky writing) and Help. Each one presses the key it is named by, so a finger or a
 * mouse reaches everything the keyboard does. The keys show on the buttons in a wide window, for the keyboard.
 */
final class SkyDock {
   /** Below this window width the buttons leave their keys off and may take two rows. */
   static final int NARROW = 560;
   private static final int GAP = 8;
   private static final int MARGIN = 12;
   private static final Color GOLD = new Color(255, 214, 90);

   /** One button: where it is, the key it presses, its words and whether it shows lit (on). */
   static final class Chip {
      final Rectangle box;
      final int key;
      final String label;
      final String keyName;
      final boolean lit;

      Chip(Rectangle box, int key, String label, String keyName, boolean lit) {
         this.box = box;
         this.key = key;
         this.label = label;
         this.keyName = keyName;
         this.lit = lit;
      }
   }

   private SkyDock() {
   }

   static Font font(int width) {
      return new Font(Font.SANS_SERIF, Font.BOLD, Math.max(13, Math.min(20, width / 40)));
   }

   /** Buttons at least 40 px tall, so a fingertip finds them. */
   static int chipHeight(FontMetrics metrics) {
      return Math.max(40, metrics.getHeight() + 16);
   }

   /** The buttons for a field of this size, in this state, laid out in rows from the bottom up, centred. */
   static List<Chip> layout(FontMetrics metrics, int width, int height, int mode, boolean trails, boolean music, boolean writing, boolean help) {
      boolean keys = width >= NARROW;
      List<Chip> chips = new ArrayList<>();
      chips.add(chip(metrics, KeyEvent.VK_LEFT, "\u25C0", "", false));
      chips.add(chip(metrics, KeyEvent.VK_RIGHT, (mode + 1) + " " + StarField.MODE_NAMES[mode] + "  \u25B6", "", false));
      chips.add(chip(metrics, KeyEvent.VK_T, "Trails", keys ? "T" : "", trails));
      chips.add(chip(metrics, KeyEvent.VK_S, "Music", keys ? "S" : "", music));
      chips.add(chip(metrics, KeyEvent.VK_SPACE, "Burst", keys ? "Space" : "", false));
      if (mode == StarField.SKY_WRITING) {
         chips.add(chip(metrics, KeyEvent.VK_ENTER, writing ? "Done" : "Write", keys ? "Enter" : "", writing));
      }
      chips.add(chip(metrics, KeyEvent.VK_H, "Help", keys ? "H" : "", help));
      // Fill rows left to right (the sky's arrow stays with its name), then centre each row; the last row sits at the bottom.
      List<List<Chip>> rows = new ArrayList<>();
      List<Chip> row = new ArrayList<>();
      int rowWidth = 0;
      for (Chip chip : chips) {
         if (!row.isEmpty() && chip.key != KeyEvent.VK_RIGHT && rowWidth + GAP + chip.box.width > width - 2 * MARGIN) {
            rows.add(row);
            row = new ArrayList<>();
            rowWidth = 0;
         }
         rowWidth += (row.isEmpty() ? 0 : GAP) + chip.box.width;
         row.add(chip);
      }
      rows.add(row);
      int chipHeight = chipHeight(metrics);
      int y = height - MARGIN - rows.size() * chipHeight - (rows.size() - 1) * GAP;
      for (List<Chip> line : rows) {
         int lineWidth = (line.size() - 1) * GAP;
         for (Chip chip : line) {
            lineWidth += chip.box.width;
         }
         int x = Math.max(MARGIN, (width - lineWidth) / 2);
         for (Chip chip : line) {
            chip.box.setLocation(x, y);
            x += chip.box.width + GAP;
         }
         y += chipHeight + GAP;
      }
      return chips;
   }

   private static Chip chip(FontMetrics metrics, int key, String label, String keyName, boolean lit) {
      return new Chip(new Rectangle(0, 0, chipWidth(metrics, label, keyName), chipHeight(metrics)), key, label, keyName, lit);
   }

   private static int chipWidth(FontMetrics metrics, String label, String keyName) {
      int width = metrics.stringWidth(label) + 28;
      return keyName.isEmpty() ? Math.max(40, width) : width + keyWidth(metrics, keyName) + 8;
   }

   private static int keyWidth(FontMetrics metrics, String keyName) {
      return metrics.stringWidth(keyName) * 3 / 4 + 12;
   }

   /** The top of the highest row, or {@code height} with no buttons. */
   static int top(List<Chip> chips, int height) {
      int top = height;
      for (Chip chip : chips) {
         top = Math.min(top, chip.box.y);
      }
      return top;
   }

   /** The key of the button under (x, y), or -1. */
   static int keyAt(List<Chip> chips, int x, int y) {
      for (Chip chip : chips) {
         if (chip.box.contains(x, y)) {
            return chip.key;
         }
      }
      return -1;
   }

   /** Draws the buttons at {@code alpha} (0 to 255); the one under the pointer is lighter. */
   static void draw(Graphics2D g2, List<Chip> chips, int alpha, double pointerX, double pointerY, boolean pointerOn) {
      if (alpha <= 0) {
         return;
      }
      Font font = g2.getFont();
      Font keyFont = font.deriveFont(Font.PLAIN, font.getSize2D() * 3 / 4);
      for (Chip chip : chips) {
         Rectangle box = chip.box;
         boolean over = pointerOn && box.contains(pointerX, pointerY);
         g2.setColor(chip.lit ? new Color(70, 56, 10, alpha * 4 / 5) : new Color(0, 0, 40, alpha * (over ? 9 : 7) / 10));
         g2.fillRoundRect(box.x, box.y, box.width, box.height, box.height, box.height);
         g2.setColor(chip.lit ? withAlpha(GOLD, alpha) : new Color(255, 255, 255, alpha * (over ? 9 : 4) / 10));
         g2.drawRoundRect(box.x, box.y, box.width - 1, box.height - 1, box.height, box.height);
         g2.setFont(font);
         FontMetrics metrics = g2.getFontMetrics();
         int baseline = box.y + (box.height - metrics.getHeight()) / 2 + metrics.getAscent();
         g2.setColor(chip.lit ? withAlpha(GOLD, alpha) : new Color(255, 255, 255, alpha));
         int textX = box.x + 14;
         if (chip.keyName.isEmpty() && chip.label.length() == 1) {
            textX = box.x + (box.width - metrics.stringWidth(chip.label)) / 2; // a lone arrow sits in the middle
         }
         g2.drawString(chip.label, textX, baseline);
         if (!chip.keyName.isEmpty()) {
            // The key, small in a keycap after the words.
            int keyX = textX + metrics.stringWidth(chip.label) + 8;
            g2.setFont(keyFont);
            FontMetrics small = g2.getFontMetrics();
            int keyWidth = keyWidth(metrics, chip.keyName);
            int keyHeight = small.getHeight() + 2;
            int keyY = box.y + (box.height - keyHeight) / 2;
            g2.setColor(new Color(255, 255, 255, alpha / 3));
            g2.drawRoundRect(keyX, keyY, keyWidth, keyHeight, 6, 6);
            g2.setColor(new Color(220, 228, 255, alpha * 4 / 5));
            g2.drawString(chip.keyName, keyX + (keyWidth - small.stringWidth(chip.keyName)) / 2, keyY + 1 + small.getAscent());
         }
      }
   }

   private static Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }
}
