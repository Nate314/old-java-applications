package test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;

/**
 * The shortcuts the Windows key makes, as a shelf of keys to click: each one presses Win+that key. Holding the key
 * and typing a letter needs a keyboard, so this is how a touch screen or a mouse alone gets File Explorer, the
 * desktop, Run and task view, and the four snaps. With the keyboard focus (in the browser, by Tab), the arrow keys
 * pick a shortcut and Space or Enter presses it. The last row turns hover to press ({@link Dwell}) on and off for
 * the shelf and the key alike.
 */
final class ShortcutShelf extends JButton {
   private static final int[] CODES = {KeyEvent.VK_E, KeyEvent.VK_D, KeyEvent.VK_R, KeyEvent.VK_TAB,
      KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN};
   private static final String[] CAPS = {"E", "D", "R", "Tab", "←", "↑", "→", "↓"};
   private static final String[] NAMES = {"File Explorer", "Desktop", "Run", "Task view",
      "Snap left", "Fill the screen", "Snap right", "Restore or minimize"};
   /** The rows of one shortcut each; the arrows share the last row. */
   private static final int ROWS = 4;
   /** The row after the arrows, which turns hover to press on and off; it comes after the shortcuts. */
   static final int HOVER_ROW = CODES.length;
   static final String HOVER_NAME = "Hover to press";
   private static final String TIP = "Click a shortcut for Win+that key";
   private static final int PAD = 4;
   private static final int ROW = 26;
   /** The arrows' row is taller: each arrow shows a little screen with the window where that snap puts it. */
   private static final int ARROW_ROW = ROW + 14;
   static final int HEIGHT = 2 * PAD + (ROWS + 1) * ROW + ARROW_ROW;
   private static final Color FACE = new Color(48, 51, 61);
   private static final Color HOVER = new Color(66, 70, 82);
   private static final Color LIT = new Color(96, 182, 255);
   private static final Color CAP = new Color(0, 120, 215);
   private static final Color TEXT = new Color(236, 239, 244);
   private static final Color DIM = new Color(170, 176, 190);

   private final WindowsKey.Keys keys;
   private final BooleanSupplier dwellOn;
   private final Runnable toggleDwell;
   private final Dwell dwell;
   /** The big key, which shows each chord the shelf makes and, held, lights the shelf's keys; null for none. */
   private final WindowsKey key;
   /** The shortcut under the pointer, the one the mouse button holds, and the one the arrow keys picked; -1 for none. */
   private int hover = -1;
   private int held = -1;
   private int picked;

   ShortcutShelf(WindowsKey.Keys keys) {
      this(keys, () -> false, () -> { });
   }

   ShortcutShelf(WindowsKey.Keys keys, BooleanSupplier dwellOn, Runnable toggleDwell) {
      this(keys, dwellOn, toggleDwell, null);
   }

   ShortcutShelf(WindowsKey.Keys keys, BooleanSupplier dwellOn, Runnable toggleDwell, WindowsKey key) {
      this.keys = keys;
      this.key = key;
      if (key != null) {
         key.heldChanged = this::repaint;
      }
      this.dwellOn = dwellOn;
      this.toggleDwell = toggleDwell;
      this.dwell = new Dwell(this, dwellOn, this::press);
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setPreferredSize(new Dimension(Test.WIDTH, HEIGHT));
      this.setToolTipText(TIP);
      this.name();
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               ShortcutShelf.this.held = ShortcutShelf.this.at(e.getX(), e.getY());
               ShortcutShelf.this.dwell.clicked();
               ShortcutShelf.this.repaint();
            }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               int i = ShortcutShelf.this.held;
               ShortcutShelf.this.held = -1;
               // Letting go off the shortcut it pressed calls it off, as with any button.
               if (i >= 0 && i == ShortcutShelf.this.at(e.getX(), e.getY())) {
                  ShortcutShelf.this.press(i);
               }
               ShortcutShelf.this.repaint();
            }
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            ShortcutShelf.this.hoverAt(e.getX(), e.getY());
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            ShortcutShelf.this.hoverAt(e.getX(), e.getY());
         }

         @Override
         public void mouseExited(MouseEvent e) {
            ShortcutShelf.this.hoverAt(-1, -1);
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_RIGHT) {
               ShortcutShelf.this.pick(1);
            } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_LEFT) {
               ShortcutShelf.this.pick(-1);
            }
         }

         @Override
         public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
               ShortcutShelf.this.press(ShortcutShelf.this.picked);
            }
         }
      });
   }

   static int count() {
      return CODES.length;
   }

   static int code(int i) {
      return CODES[i];
   }

   /** Win+the i-th shortcut: the Windows key goes down, the key is tapped, the Windows key comes up. */
   void send(int i) {
      this.keys.press(WindowsKey.WIN);
      this.keys.press(CODES[i]);
      this.keys.release(CODES[i]);
      this.keys.release(WindowsKey.WIN);
      if (this.key != null) {
         this.key.echo("Win+" + CAPS[i], i >= ROWS ? CODES[i] : 0);
      }
   }

   /** Whether the big key is held, so that every key on the shelf is one a key typed now would add to it. */
   boolean keyHeld() {
      return this.key != null && this.key.isDown();
   }

   /** A row pressed: Win+its key, or hover to press turned on or off. */
   void press(int i) {
      if (i == HOVER_ROW) {
         this.toggleDwell.run();
         this.name();
         this.repaint();
      } else {
         this.send(i);
      }
   }

   Dwell dwell() {
      return this.dwell;
   }

   /** How much bigger than the small key's shelf this one is (the window's size picks it). */
   double scale() {
      return this.getWidth() > 0 ? this.getWidth() / (double) Test.WIDTH : 1;
   }

   /**
    * Where the i-th shortcut is drawn, at the small size (see {@link #scale}): a whole row for the first four, a
    * quarter of the last row for an arrow.
    */
   Rectangle rect(int i) {
      int w = (int) Math.round(this.getWidth() / this.scale()) - 2 * PAD;
      if (i < ROWS || i == HOVER_ROW) {
         return new Rectangle(PAD, PAD + (i < ROWS ? i * ROW : ROWS * ROW + ARROW_ROW), w, ROW - 3);
      }
      int cap = (w - 3 * PAD) / 4;
      return new Rectangle(PAD + (i - ROWS) * (cap + PAD), PAD + ROWS * ROW + 2, cap, ARROW_ROW - 5);
   }

   /** The shortcut at (x, y) on the screen, or -1. */
   int at(int x, int y) {
      double s = this.scale();
      for (int i = 0; i <= HOVER_ROW; i++) {
         if (this.rect(i).contains((int) (x / s), (int) (y / s))) {
            return i;
         }
      }
      return -1;
   }

   int picked() {
      return this.picked;
   }

   private void hoverAt(int x, int y) {
      int i = this.at(x, y);
      this.dwell.at(i);
      if (i != this.hover) {
         this.hover = i;
         // The arrows' caps carry no names, so the one pointed at says what it does.
         this.setToolTipText(i == HOVER_ROW ? HOVER_NAME + ": resting the pointer on the key or a shortcut a second presses it"
            : i >= 0 ? "Win+" + CAPS[i] + ": " + NAMES[i] : TIP);
         this.repaint();
      }
   }

   private void pick(int step) {
      this.picked = (this.picked + step + HOVER_ROW + 1) % (HOVER_ROW + 1);
      this.name();
      this.repaint();
   }

   private void name() {
      String row = this.picked == HOVER_ROW ? HOVER_NAME + ", " + (this.dwellOn.getAsBoolean() ? "on" : "off")
         + ": resting the pointer on the key or a shortcut presses it"
         : "Win+" + CAPS[this.picked] + ", " + NAMES[this.picked];
      this.getAccessibleContext().setAccessibleName("Windows key shortcuts: " + row + ". Arrow keys for another, Space to press it");
   }

   /** The hover to press row: a switch, lit when on, and its name. */
   private void paintHoverRow(Graphics2D g, Rectangle r, boolean lit, Font nameFont) {
      boolean on = this.dwellOn.getAsBoolean();
      int sw = 20;
      int sh = r.height - 10;
      int sx = r.x + 3;
      int sy = r.y + 5;
      g.setColor(on ? CAP : WindowsKey.BACK);
      g.fillRoundRect(sx, sy, sw, sh, sh, sh);
      g.setColor(on || lit ? Color.WHITE : DIM);
      g.fillOval(on ? sx + sw - sh + 2 : sx + 2, sy + 2, sh - 4, sh - 4);
      g.setFont(nameFont);
      g.setColor(lit ? Color.WHITE : TEXT);
      g.drawString(HOVER_NAME, sx + sw + 5, r.y + (r.height + g.getFontMetrics().getAscent()) / 2 - 2);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(WindowsKey.BACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.scale(this.scale(), this.scale());
      Font capFont = new Font("SansSerif", Font.BOLD, 11);
      Font nameFont = new Font("SansSerif", Font.PLAIN, 11);
      boolean focus = this.isFocusOwner();
      boolean held = this.keyHeld();
      for (int i = 0; i <= HOVER_ROW; i++) {
         Rectangle r = this.rect(i);
         boolean lit = i == this.held;
         g.setColor(lit ? LIT : i == this.hover ? HOVER : FACE);
         g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
         // Hover to press: the row fills from the left while the pointer rests on it.
         int fill = (int) Math.round(r.width * this.dwell.progress(i));
         if (fill > 0 && !lit) {
            g.setColor(CAP);
            g.fillRoundRect(r.x, r.y, fill, r.height, 8, 8);
         }
         if (focus && i == this.picked) {
            g.setColor(TEXT);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
         }
         g.setFont(capFont);
         if (i == HOVER_ROW) {
            this.paintHoverRow(g, r, lit, nameFont);
            continue;
         }
         int capW = i < ROWS ? Math.max(18, g.getFontMetrics().stringWidth(CAPS[i]) + 8) : r.width;
         int base = r.y + (r.height + g.getFontMetrics().getAscent()) / 2 - 2;
         if (i < ROWS) {
            g.setColor(lit ? Color.WHITE : held ? LIT : CAP);
            g.fillRoundRect(r.x + 3, r.y + 3, capW, r.height - 6, 6, 6);
            if (held) {
               // The big key is down: these are the keys to type now.
               g.setColor(Color.WHITE);
               g.setStroke(new BasicStroke(1f));
               g.drawRoundRect(r.x + 3, r.y + 3, capW - 1, r.height - 7, 6, 6);
            }
            g.setColor(Color.WHITE);
            g.drawString(CAPS[i], r.x + 3 + (capW - g.getFontMetrics().stringWidth(CAPS[i])) / 2, base);
            g.setFont(nameFont);
            g.setColor(lit ? Color.WHITE : TEXT);
            g.drawString(NAMES[i], r.x + capW + 8, base);
         } else {
            if (held) {
               g.setColor(Color.WHITE);
               g.setStroke(new BasicStroke(1f));
               g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
            }
            // Where the window goes, over the arrow that sends it there.
            WindowsKey.drawSnap(g, CODES[i], r.x + 3, r.y + 4, r.width - 6, 13, lit || held || i == this.hover ? Color.WHITE : DIM,
               lit ? Color.WHITE : CAP);
            g.setColor(lit || held ? Color.WHITE : DIM);
            g.drawString(CAPS[i], r.x + (r.width - g.getFontMetrics().stringWidth(CAPS[i])) / 2, r.y + r.height - 5);
         }
      }
   }
}
