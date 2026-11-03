package windowse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * A small keyboard with the Windows shortcuts on it. The keys that do something with the Windows key are blue-edged
 * buttons ({@link KeyCap}); the rest are drawn, dimmed. Above it, the picked shortcut and what it does; while it is
 * being pressed, the Windows key stays down, the other key goes down with each press, and a count runs.
 */
final class ShortcutBoard extends JPanel {
   private static final int UNIT = 48;
   private static final int MARGIN = 12;
   private static final int PAD = 10;
   private static final int HEAD = 62;
   private static final int ROWS = 4;
   private static final int DECK_UNITS_X10 = 125;   // 12.5 keys wide
   private static final Color BACK = new Color(243, 245, 249);
   private static final Color DECK = new Color(28, 30, 36);
   private static final Color KEY = new Color(48, 51, 60);
   private static final Color KEY_SIDE = new Color(22, 23, 28);
   private static final Color KEY_TEXT = new Color(128, 133, 145);
   private static final Color INK = new Color(28, 32, 40);
   private static final Color MUTED = new Color(92, 98, 110);

   /** One key of a row: its label (null for a gap), its width in tenths of a key, and its shortcut if it has one. */
   private static final class Slot {
      final String label;
      final int tenths;
      final Shortcut shortcut;

      Slot(String label, int tenths, Shortcut shortcut) {
         this.label = label;
         this.tenths = tenths;
         this.shortcut = shortcut;
      }
   }

   private static Slot key(String label, int tenths) {
      return new Slot(label, tenths, null);
   }

   private static Slot cap(Shortcut shortcut, int tenths) {
      return new Slot(null, tenths, shortcut);
   }

   private static final Slot[][] LAYOUT = {
      {cap(Shortcut.TASKS, 15), key("Q", 10), key("W", 10), cap(Shortcut.EXPLORER, 10), cap(Shortcut.RUN, 10),
         key("T", 10), key("Y", 10), key("U", 10), key("I", 10), key("O", 10), key("P", 10), key("Bksp", 10)},
      {key("Caps", 17), key("A", 10), key("S", 10), cap(Shortcut.DESKTOP, 10), key("F", 10), key("G", 10),
         key("H", 10), key("J", 10), key("K", 10), key("L", 10), key("Enter", 18)},
      {key("Shift", 22), key("Z", 10), key("X", 10), key("C", 10), key("V", 10), key("B", 10), key("N", 10),
         key("M", 10), key("Shift", 13), cap(Shortcut.UP, 10), new Slot(null, 10, null)},
      {key("Ctrl", 12), cap(Shortcut.START, 13), key("Alt", 12), key("", 46), key("Alt", 12),
         cap(Shortcut.LEFT, 10), cap(Shortcut.DOWN, 10), cap(Shortcut.RIGHT, 10)},
   };

   private final Map<Shortcut, KeyCap> caps = new EnumMap<>(Shortcut.class);
   private final Timer release = new Timer(60, event -> this.letGo());
   private Shortcut picked;
   private int done;
   private int count;
   private boolean running;

   /** onPick runs when a key is clicked; onPress when one is double-clicked (pick it and press it). */
   ShortcutBoard(Consumer<Shortcut> onPick, Consumer<Shortcut> onPress) {
      this.setLayout(null);
      this.setPreferredSize(new Dimension(2 * MARGIN + 2 * PAD + DECK_UNITS_X10 * UNIT / 10,
         HEAD + 2 * PAD + ROWS * UNIT + MARGIN));
      this.release.setRepeats(false);
      for (int row = 0; row < LAYOUT.length; row++) {
         int tenths = 0;
         for (Slot slot : LAYOUT[row]) {
            if (slot.shortcut != null) {
               KeyCap cap = new KeyCap(slot.shortcut, onPick, onPress);
               int[] r = bounds(row, tenths, slot.tenths);
               cap.setBounds(r[0], r[1], r[2], r[3]);
               this.add(cap);
               this.caps.put(slot.shortcut, cap);
            }
            tenths += slot.tenths;
         }
      }
   }

   /** A key's x, y, width and height, from its row and where it starts and how wide it is, in tenths of a key. */
   private static int[] bounds(int row, int startTenths, int tenths) {
      int x = MARGIN + PAD + startTenths * UNIT / 10;
      int y = HEAD + PAD + row * UNIT;
      return new int[] {x + 2, y + 2, tenths * UNIT / 10 - 4, UNIT - 4};
   }

   void pick(Shortcut shortcut) {
      this.picked = shortcut;
      for (KeyCap cap : this.caps.values()) {
         cap.setPicked(cap.shortcut == shortcut);
      }
      this.repaint();
   }

   /** A run of count presses starts: the Windows key goes down and stays down until {@link #stopped()}. */
   void started(int count) {
      this.running = true;
      this.done = 0;
      this.count = count;
      this.caps.get(Shortcut.START).setPushed(this.picked != Shortcut.START);
      this.repaint();
   }

   /** One more press is done: the picked key goes down for a moment. */
   void tapped(int done) {
      this.done = done;
      this.caps.get(this.picked).setPushed(true);
      this.release.restart();
      this.repaint();
   }

   void stopped() {
      this.running = false;
      this.release.stop();
      for (KeyCap cap : this.caps.values()) {
         cap.setPushed(false);
      }
      this.repaint();
   }

   /** The press is over: the picked key comes back up (the Windows key stays down while it is held). */
   private void letGo() {
      this.caps.get(this.picked).setPushed(false);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      this.paintHead(g);

      int deckW = 2 * PAD + DECK_UNITS_X10 * UNIT / 10;
      g.setColor(DECK);
      g.fillRoundRect(MARGIN, HEAD, deckW, 2 * PAD + ROWS * UNIT, 16, 16);
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      for (int row = 0; row < LAYOUT.length; row++) {
         int tenths = 0;
         for (Slot slot : LAYOUT[row]) {
            if (slot.shortcut == null && slot.label != null) {
               int[] r = bounds(row, tenths, slot.tenths);
               g.setColor(KEY_SIDE);
               g.fillRoundRect(r[0], r[1] + 3, r[2], r[3] - 3, 9, 9);
               g.setColor(KEY);
               g.fillRoundRect(r[0], r[1], r[2], r[3] - 4, 9, 9);
               g.setColor(KEY_TEXT);
               g.drawString(slot.label, r[0] + 8, r[1] + 17);
            }
            tenths += slot.tenths;
         }
      }
   }

   /** The picked shortcut as keys ("[logo] + [E]"), its name and what it does; on the right, the count while running. */
   private void paintHead(Graphics2D g) {
      if (this.picked == null) {
         return;
      }
      int x = MARGIN + 2;
      int y = 14;
      x = chip(g, x, y, null) + 6;
      if (this.picked != Shortcut.START) {
         g.setColor(MUTED);
         g.setFont(new Font("SansSerif", Font.BOLD, 16));
         g.drawString("+", x, y + 21);
         x += 18;
         x = chip(g, x, y, this.picked);
      }
      x += 14;
      g.setColor(INK);
      g.setFont(new Font("SansSerif", Font.BOLD, 17));
      g.drawString(this.picked.title, x, y + 13);
      g.setColor(MUTED);
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      g.drawString(this.picked.combo() + " " + this.picked.does + ".", x, y + 31);

      String right = this.running ? "Pressing " + this.done + " of " + this.count : "Double-click a key to press it";
      g.setFont(new Font("SansSerif", this.running ? Font.BOLD : Font.PLAIN, this.running ? 14 : 11));
      g.setColor(this.running ? KeyCap.ACCENT : MUTED);
      FontMetrics fm = g.getFontMetrics();
      int rx = this.getWidth() - MARGIN - 2 - fm.stringWidth(right);
      if (rx > x + 230) {   // only where it clears the description
         g.drawString(right, rx, y + 13);
      }
   }

   /** A small key: the Windows logo (shortcut null) or the shortcut's key. Returns where it ends. */
   private static int chip(Graphics2D g, int x, int y, Shortcut shortcut) {
      int w = shortcut == null || shortcut.legend.isEmpty() ? 32 : Math.max(32, 16 + shortcut.legend.length() * 10);
      g.setColor(new Color(205, 210, 220));
      g.fillRoundRect(x, y + 2, w, 32, 8, 8);
      g.setColor(Color.WHITE);
      g.fillRoundRect(x, y, w, 30, 8, 8);
      g.setColor(new Color(170, 176, 190));
      g.drawRoundRect(x, y, w - 1, 30, 8, 8);
      if (shortcut == null) {
         KeyCap.paintLogo(g, x + 8, y + 7, 16, KeyCap.ACCENT);
      } else if (shortcut.legend.isEmpty()) {
         KeyCap.paintArrow(g, shortcut, x + w / 2, y + 15, INK);
      } else {
         g.setColor(INK);
         g.setFont(new Font("SansSerif", Font.BOLD, 15));
         FontMetrics fm = g.getFontMetrics();
         g.drawString(shortcut.legend, x + (w - fm.stringWidth(shortcut.legend)) / 2, y + 20);
      }
      return x + w;
   }
}
