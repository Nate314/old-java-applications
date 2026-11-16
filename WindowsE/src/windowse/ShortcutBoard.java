package windowse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * A small keyboard with the Windows shortcuts on it. The keys that do something with the Windows key are blue-edged
 * buttons ({@link KeyCap}); the rest are drawn, dimmed. Above it, the picked shortcut and what it does, and the list
 * of steps DO IT!!! presses; while they are pressed, the Windows key and the other key go down with each press, the
 * step being pressed lights up and a count runs.
 */
final class ShortcutBoard extends JPanel {
   private static final int MARGIN = 12;
   private static final int PAD = 10;
   private static final int BEZEL = 5;
   private static final Color BACK = new Color(243, 245, 249);
   private static final Color DECK = new Color(28, 30, 36);
   private static final Color KEY = new Color(48, 51, 60);
   private static final Color KEY_SIDE = new Color(22, 23, 28);
   private static final Color KEY_TEXT = new Color(128, 133, 145);
   private static final Color INK = new Color(28, 32, 40);
   private static final Color MUTED = new Color(92, 98, 110);
   private static final int HEAD_Y = 14;
   /** The count's slot after the keys: the "x" and the field (or, while a run goes, the step's count). */
   private static final int COUNT_W = 58;
   private static final int TIMES_W = 20;

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

   /** The shortcut keys alone, three by three, for a narrow screen (a phone): the Windows key, then the arrows below. */
   private static final Slot[][] POCKET = {
      {cap(Shortcut.START, 10), cap(Shortcut.EXPLORER, 10), cap(Shortcut.RUN, 10)},
      {cap(Shortcut.TASKS, 10), cap(Shortcut.UP, 10), cap(Shortcut.DESKTOP, 10)},
      {cap(Shortcut.LEFT, 10), cap(Shortcut.DOWN, 10), cap(Shortcut.RIGHT, 10)},
   };

   /** Pocket: the 3x3 pad of bigger keys and a smaller monitor, about 380 px wide instead of about 860. */
   final boolean pocket;
   private final Slot[][] layout;
   private final int unit;
   private final int head;
   private final int stepsY;
   private final int deckW;
   private final int screenW;
   private final int screenH;
   private final int monitorX;
   private final int monitorW;
   private final Map<Shortcut, KeyCap> caps = new EnumMap<>(Shortcut.class);
   private final Timer release = new Timer(60, event -> this.letGo());
   private Shortcut picked;
   private List<Step> shown = new ArrayList<>();
   private List<Step> run = new ArrayList<>();
   private Shortcut current;
   private final JComponent count;
   private int done;
   private int total;
   private boolean running;
   private boolean dry;
   /** The screen after the plan, and while a run goes (and after it, until the plan changes) the screen it made. */
   private Screen after = new Screen();
   private Screen live;

   /**
    * onPick runs when a key is clicked; onPress when one is double-clicked (pick it and press it). count: the field
    * for how many times, placed after the picked shortcut's keys so the head reads "Win + E x 10".
    */
   ShortcutBoard(Consumer<Shortcut> onPick, Consumer<Shortcut> onPress, boolean pocket, JComponent count) {
      this.pocket = pocket;
      this.count = count;
      this.add(count);   // first, so it has the keyboard when the window opens
      this.layout = pocket ? POCKET : LAYOUT;
      this.unit = pocket ? 56 : 48;
      this.head = pocket ? 122 : 92;   // pocket: what it does gets its own lines under the chips
      this.stepsY = pocket ? 86 : 56;
      this.deckW = 2 * PAD + (pocket ? 30 : 125) * this.unit / 10;   // 3 keys, or 12.5
      this.screenW = pocket ? 144 : 192;
      this.screenH = pocket ? 90 : 120;
      this.monitorX = MARGIN + this.deckW + (pocket ? 12 : 16);
      this.monitorW = this.screenW + 2 * BEZEL;
      this.setLayout(null);
      this.setPreferredSize(new Dimension(this.monitorX + this.monitorW + MARGIN,
         this.head + 2 * PAD + this.layout.length * this.unit + MARGIN));
      this.release.setRepeats(false);
      for (int row = 0; row < this.layout.length; row++) {
         int tenths = 0;
         for (Slot slot : this.layout[row]) {
            if (slot.shortcut != null) {
               KeyCap cap = new KeyCap(slot.shortcut, onPick, onPress);
               int[] r = this.bounds(row, tenths, slot.tenths);
               cap.setBounds(r[0], r[1], r[2], r[3]);
               this.add(cap);
               this.caps.put(slot.shortcut, cap);
            }
            tenths += slot.tenths;
         }
      }
   }

   /** A key's x, y, width and height, from its row and where it starts and how wide it is, in tenths of a key. */
   int[] bounds(int row, int startTenths, int tenths) {
      int x = MARGIN + PAD + startTenths * this.unit / 10;
      int y = this.head + PAD + row * this.unit;
      return new int[] {x + 2, y + 2, tenths * this.unit / 10 - 4, this.unit - 4};
   }

   void pick(Shortcut shortcut) {
      this.picked = shortcut;
      this.count.setBounds(chipsEnd(shortcut) + TIMES_W, HEAD_Y, COUNT_W, 30);
      for (KeyCap cap : this.caps.values()) {
         cap.setPicked(cap.shortcut == shortcut);
      }
      this.repaint();
   }

   /** The list of steps under the picked shortcut. */
   void steps(List<Step> steps) {
      this.shown = steps;
      this.repaint();
   }

   /** What DO IT!!! would press now; the little screen shows what that does. */
   void plan(List<Step> plan) {
      this.after = Screen.after(plan);
      this.live = null;
      this.repaint();
   }

   /** The little screen's words for what the plan does (empty plan: the count is not a number). */
   String afterSummary() {
      return this.after.summary();
   }

   /** A run of these steps starts (the list itself, or one step of the picked key); dry: only on the little screen. */
   void started(List<Step> run, boolean dry) {
      this.running = true;
      this.dry = dry;
      this.live = new Screen();
      this.run = run;
      this.done = 0;
      this.total = 0;
      for (Step step : run) {
         this.total += step.times;
      }
      this.count.setVisible(false);   // the head shows the step being pressed, and its count, in its place
      this.repaint();
   }

   /** One more press is done: the Windows key and the key pressed with it go down for a moment. */
   void tapped(int done) {
      this.done = done;
      this.letGo();
      this.current = this.run.get(this.stepAt(done - 1)).shortcut;
      this.live.apply(this.current);
      this.caps.get(Shortcut.START).setPushed(true);
      this.caps.get(this.current).setPushed(true);
      this.release.restart();
      this.repaint();
   }

   /** Which step of the run press number tap (from 0) belongs to. */
   private int stepAt(int tap) {
      int i = 0;
      for (int left = tap; i < this.run.size() - 1 && left >= this.run.get(i).times; i++) {
         left -= this.run.get(i).times;
      }
      return i;
   }

   void stopped() {
      this.running = false;
      this.current = null;
      this.count.setVisible(true);
      this.release.stop();
      for (KeyCap cap : this.caps.values()) {
         cap.setPushed(false);
      }
      this.repaint();
   }

   /** The press is over: the keys come back up. */
   private void letGo() {
      for (KeyCap cap : this.caps.values()) {
         cap.setPushed(false);
      }
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      this.paintHead(g);
      this.paintSteps(g);

      this.paintMonitor(g);
      g.setColor(DECK);
      g.fillRoundRect(MARGIN, this.head, this.deckW, 2 * PAD + this.layout.length * this.unit, 16, 16);
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      for (int row = 0; row < this.layout.length; row++) {
         int tenths = 0;
         for (Slot slot : this.layout[row]) {
            if (slot.shortcut == null && slot.label != null) {
               int[] r = this.bounds(row, tenths, slot.tenths);
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
      Step step = this.running ? this.run.get(this.stepAt(Math.max(0, this.done - 1))) : null;
      Shortcut head = step != null ? step.shortcut : this.picked;
      if (head == null) {
         return;
      }
      int y = HEAD_Y;
      int x = chip(g, MARGIN + 2, y, null) + 6;
      g.setFont(new Font("SansSerif", Font.BOLD, 16));
      if (head != Shortcut.START) {
         g.setColor(MUTED);
         g.drawString("+", x, y + 21);
         chip(g, x + 18, y, head);
      }
      x = chipsEnd(head);
      g.setColor(MUTED);
      g.setFont(new Font("SansSerif", Font.BOLD, 20));
      g.drawString("\u00d7", x + 4, y + 22);
      if (step != null) {
         g.setColor(KeyCap.ACCENT);
         g.drawString(String.valueOf(step.times), x + TIMES_W + 6, y + 21);
      }
      x += TIMES_W + COUNT_W + 14;
      g.setColor(INK);
      Font title = new Font("SansSerif", Font.BOLD, 17);
      g.setFont(title);
      if (g.getFontMetrics().stringWidth(head.title) > this.getWidth() - MARGIN - x) {   // pocket: "Restore or minimize"
         g.setFont(title.deriveFont(14f));
      }
      g.drawString(head.title, x, y + 13);
      g.setColor(MUTED);
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      String does = head.combo() + " " + head.does + ".";
      if (this.pocket) {   // under the keys, the width of the window, in up to two lines
         List<String> lines = wrap(does, g.getFontMetrics(), this.getWidth() - 2 * MARGIN - 4, 2);
         for (int i = 0; i < lines.size(); i++) {
            g.drawString(lines.get(i), MARGIN + 2, y + 50 + 14 * i);
         }
      } else {
         g.drawString(does, x, y + 31);
      }

      String right = this.running ? (this.dry ? "Preview " : "Pressing ") + this.done + " of " + this.total : "Double-click a key to press it";
      g.setFont(new Font("SansSerif", this.running ? Font.BOLD : Font.PLAIN, this.running ? 14 : 11));
      g.setColor(this.running ? KeyCap.ACCENT : MUTED);
      FontMetrics fm = g.getFontMetrics();
      int rx = this.getWidth() - MARGIN - 2 - fm.stringWidth(right);
      if (rx > x + 230) {   // only where it clears the description
         g.drawString(right, rx, y + 13);
      }
   }

   /**
    * The list of steps as pills joined by arrows, the one being pressed lit; past the right edge, "+N more". Empty, a
    * line says what the list is for.
    */
   private void paintSteps(Graphics2D g) {
      int x = MARGIN + 2;
      int y = this.stepsY;
      g.setColor(MUTED);
      g.setFont(new Font("SansSerif", Font.BOLD, 11));
      g.drawString("STEPS", x, y + 16);
      x += 48;
      if (this.shown.isEmpty()) {
         g.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g.drawString(this.pocket ? "Add step: a list to press in order."
            : "Add step lists the picked key; DO IT!!! then presses the list in order.", x, y + 16);
         return;
      }
      int lit = this.running && this.run == this.shown && this.done > 0 ? this.stepAt(this.done - 1) : -1;
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      FontMetrics fm = g.getFontMetrics();
      int right = this.getWidth() - MARGIN - 2;
      for (int i = 0; i < this.shown.size(); i++) {
         String text = this.shown.get(i).label();
         int w = fm.stringWidth(text) + 18;
         if (x + w > right - (i < this.shown.size() - 1 ? 60 : 0)) {
            g.setColor(MUTED);
            g.drawString("+" + (this.shown.size() - i) + " more", x, y + 16);
            break;
         }
         g.setColor(i == lit ? KeyCap.ACCENT : Color.WHITE);
         g.fillRoundRect(x, y, w, 24, 12, 12);
         g.setColor(i == lit ? KeyCap.ACCENT : new Color(170, 176, 190));
         g.drawRoundRect(x, y, w - 1, 23, 12, 12);
         g.setColor(i == lit ? Color.WHITE : INK);
         g.drawString(text, x + 9, y + 16);
         x += w;
         if (i < this.shown.size() - 1) {
            g.setColor(MUTED);
            g.drawString(">", x + 5, y + 16);
            x += 18;
         }
      }
   }

   /** Beside the keyboard, a little monitor showing what the presses do, and under it the same in words. */
   private void paintMonitor(Graphics2D g) {
      Screen screen = this.live != null ? this.live : this.after;
      int x = this.monitorX;
      int y = this.head;
      int mw = this.monitorW;
      int sh = this.screenH;
      g.setColor(DECK);
      g.fillRoundRect(x, y, mw, sh + 2 * BEZEL, 10, 10);
      g.fillRect(x + mw / 2 - 10, y + sh + 2 * BEZEL, 20, 10);
      g.fillRoundRect(x + mw / 2 - 36, y + sh + 2 * BEZEL + 10, 72, 5, 4, 4);
      MiniScreen.paint(g, screen, x + BEZEL, y + BEZEL, this.screenW, sh);

      String label = this.running ? (this.dry ? "PREVIEW, NO KEYS PRESSED" : "PRESSING")
         : this.live != null ? (this.dry ? "PREVIEW" : "WHAT IT DID") : "AFTER DO IT!!!";
      g.setColor(this.running ? KeyCap.ACCENT : MUTED);
      g.setFont(new Font("SansSerif", Font.BOLD, 11));
      int ty = y + sh + 2 * BEZEL + 30;
      g.drawString(label, x, ty);
      g.setColor(INK);
      g.setFont(new Font("SansSerif", Font.PLAIN, 11));
      FontMetrics fm = g.getFontMetrics();
      List<String> lines = wrap(screen.summary(), fm, mw + MARGIN - 2, this.pocket ? 4 : 3);
      for (int i = 0; i < lines.size(); i++) {
         g.drawString(lines.get(i), x, ty + 15 * (i + 1));
      }
   }

   /** Text broken into lines of at most width pixels, at most max lines (the last one ending "..." if cut). */
   static List<String> wrap(String text, FontMetrics fm, int width, int max) {
      List<String> lines = new ArrayList<>();
      String line = "";
      for (String word : text.split(" ")) {
         String next = line.isEmpty() ? word : line + " " + word;
         if (fm.stringWidth(next) <= width || line.isEmpty()) {
            line = next;
         } else {
            lines.add(line);
            line = word;
         }
      }
      lines.add(line);
      if (lines.size() > max) {
         lines = new ArrayList<>(lines.subList(0, max));
         lines.set(max - 1, lines.get(max - 1) + "...");
      }
      return lines;
   }

   /** Where the head's keys for shortcut end: the Windows logo, then "+" and its own key. */
   private static int chipsEnd(Shortcut shortcut) {
      int logo = MARGIN + 2 + chipWidth(null);
      return shortcut == Shortcut.START ? logo : logo + 6 + 18 + chipWidth(shortcut);
   }

   private static int chipWidth(Shortcut shortcut) {
      return shortcut == null || shortcut.legend.isEmpty() ? 32 : Math.max(32, 16 + shortcut.legend.length() * 10);
   }

   /** A small key: the Windows logo (shortcut null) or the shortcut's key. Returns where it ends. */
   private static int chip(Graphics2D g, int x, int y, Shortcut shortcut) {
      int w = chipWidth(shortcut);
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
