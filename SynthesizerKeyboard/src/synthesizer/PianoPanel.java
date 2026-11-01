package synthesizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Draws the 13 keys (C to the high C) with the computer key and the note name on each, shows which keys are held,
 * and lights the key a lesson is waiting for. Keys are numbered 0 to 12 in note order.
 */
public final class PianoPanel extends JPanel {
   public static final int KEYS = 13;
   /** The computer key that plays each key, in note order. */
   public static final String[] LETTERS = {"Z", "S", "X", "D", "C", "V", "G", "B", "H", "N", "J", "M", ","};
   private static final int[] WHITE_KEYS = {0, 2, 4, 5, 7, 9, 11, 12};
   /** Each black key and the white key (by position) it sits right of. */
   private static final int[] BLACK_KEYS = {1, 3, 6, 8, 10};
   private static final int[] BLACK_AFTER = {0, 1, 3, 4, 5};
   private static final Color BACKGROUND = new Color(24, 26, 34);
   private static final Color WHITE = new Color(250, 250, 246);
   private static final Color WHITE_PRESSED = new Color(175, 175, 175);
   private static final Color BLACK = new Color(16, 16, 20);
   private static final Color BLACK_PRESSED = new Color(80, 80, 88);
   private static final Color HINT = new Color(255, 196, 60);
   private static final Color HINT_BLACK = new Color(226, 152, 24);
   private static final Color LABEL = new Color(60, 60, 70);
   private static final Color LABEL_SOFT = new Color(140, 140, 150);

   private final boolean[] pressed = new boolean[KEYS];
   private int hint = -1;

   public PianoPanel() {
      this.setFocusable(false);
      this.setBackground(BACKGROUND);
   }

   public void setPressed(int key, boolean down) {
      this.pressed[key] = down;
      this.repaint();
   }

   /** Lights key {@code key} as the one to press next; -1 for none. */
   public void setHint(int key) {
      if (this.hint != key) {
         this.hint = key;
         this.repaint();
      }
   }

   public static boolean isBlack(int key) {
      for (int black : BLACK_KEYS) {
         if (black == key) {
            return true;
         }
      }
      return false;
   }

   private int whiteWidth() {
      return Math.max(1, (this.getWidth() - 4) / WHITE_KEYS.length);
   }

   private int blackHeight() {
      return this.getHeight() * 3 / 5;
   }

   private int blackX(int blackIndex) {
      int whiteWidth = this.whiteWidth();
      return 2 + (BLACK_AFTER[blackIndex] + 1) * whiteWidth - whiteWidth * 3 / 10;
   }

   /** The key at a point in this panel, or -1 for none. */
   public int keyAt(int x, int y) {
      int whiteWidth = this.whiteWidth();
      if (y < 0 || y >= this.getHeight()) {
         return -1;
      }
      if (y < this.blackHeight()) {
         for (int black = 0; black < BLACK_KEYS.length; black++) {
            int left = this.blackX(black);
            if (x >= left && x < left + whiteWidth * 3 / 5) {
               return BLACK_KEYS[black];
            }
         }
      }
      int white = (x - 2) / whiteWidth;
      return x < 2 || white >= WHITE_KEYS.length ? -1 : WHITE_KEYS[white];
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int width = this.getWidth();
      int height = this.getHeight();
      int whiteWidth = this.whiteWidth();
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, width, height);
      int letterSize = Math.max(12, Math.min(30, whiteWidth / 3));
      Font letterFont = new Font("SansSerif", Font.BOLD, letterSize);
      Font noteFont = new Font("SansSerif", Font.PLAIN, Math.max(10, letterSize * 3 / 5));

      for (int white = 0; white < WHITE_KEYS.length; white++) {
         int key = WHITE_KEYS[white];
         int x = 2 + white * whiteWidth;
         g.setColor(this.pressed[key] ? WHITE_PRESSED : key == this.hint ? HINT : WHITE);
         g.fillRoundRect(x + 1, -8, whiteWidth - 2, height + 6, 10, 10);
         this.drawLabels(g, key, x, whiteWidth, height - 10, letterFont, noteFont, LABEL, LABEL_SOFT);
      }

      int blackWidth = whiteWidth * 3 / 5;
      int blackHeight = this.blackHeight();
      for (int black = 0; black < BLACK_KEYS.length; black++) {
         int key = BLACK_KEYS[black];
         int x = this.blackX(black);
         g.setColor(this.pressed[key] ? BLACK_PRESSED : key == this.hint ? HINT_BLACK : BLACK);
         g.fillRoundRect(x, -6, blackWidth, blackHeight + 6, 8, 8);
         Color text = key == this.hint && !this.pressed[key] ? BLACK : WHITE;
         this.drawLabels(g, key, x, blackWidth, blackHeight - 8, letterFont, noteFont, text, text);
      }
   }

   /** The computer key's letter at the bottom of a key, with the note name above it. */
   private void drawLabels(Graphics2D g, int key, int x, int keyWidth, int baseline, Font letterFont, Font noteFont, Color letter, Color note) {
      g.setFont(letterFont);
      FontMetrics metrics = g.getFontMetrics();
      g.setColor(letter);
      g.drawString(LETTERS[key], x + (keyWidth - metrics.stringWidth(LETTERS[key])) / 2, baseline);
      int letterHeight = metrics.getAscent();
      g.setFont(noteFont);
      metrics = g.getFontMetrics();
      String name = Song.noteName(key);
      g.setColor(note);
      g.drawString(name, x + (keyWidth - metrics.stringWidth(name)) / 2, baseline - letterHeight - 4);
   }
}
