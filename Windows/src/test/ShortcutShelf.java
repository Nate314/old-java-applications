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
import javax.swing.JButton;

/**
 * The shortcuts the Windows key makes, as a shelf of keys to click: each one presses Win+that key. Holding the key
 * and typing a letter needs a keyboard, so this is how a touch screen or a mouse alone gets File Explorer, the
 * desktop, Run and task view, and the four snaps. With the keyboard focus (in the browser, by Tab), the arrow keys
 * pick a shortcut and Space or Enter presses it.
 */
final class ShortcutShelf extends JButton {
   private static final int[] CODES = {KeyEvent.VK_E, KeyEvent.VK_D, KeyEvent.VK_R, KeyEvent.VK_TAB,
      KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN};
   private static final String[] CAPS = {"E", "D", "R", "Tab", "←", "↑", "→", "↓"};
   private static final String[] NAMES = {"File Explorer", "Desktop", "Run", "Task view",
      "Snap left", "Fill the screen", "Snap right", "Restore or minimize"};
   /** The rows of one shortcut each; the arrows share the last row. */
   private static final int ROWS = 4;
   private static final int PAD = 4;
   private static final int ROW = 26;
   static final int HEIGHT = 2 * PAD + (ROWS + 1) * ROW;
   private static final Color FACE = new Color(48, 51, 61);
   private static final Color HOVER = new Color(66, 70, 82);
   private static final Color LIT = new Color(96, 182, 255);
   private static final Color CAP = new Color(0, 120, 215);
   private static final Color TEXT = new Color(236, 239, 244);
   private static final Color DIM = new Color(170, 176, 190);

   private final WindowsKey.Keys keys;
   /** The shortcut under the pointer, the one the mouse button holds, and the one the arrow keys picked; -1 for none. */
   private int hover = -1;
   private int held = -1;
   private int picked;

   ShortcutShelf(WindowsKey.Keys keys) {
      this.keys = keys;
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setPreferredSize(new Dimension(Test.WIDTH, HEIGHT));
      this.setToolTipText("Click a shortcut for Win+that key");
      this.name();
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               ShortcutShelf.this.held = ShortcutShelf.this.at(e.getX(), e.getY());
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
                  ShortcutShelf.this.send(i);
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
               ShortcutShelf.this.send(ShortcutShelf.this.picked);
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
   }

   /** Where the i-th shortcut is drawn: a whole row for the first four, a quarter of the last row for an arrow. */
   Rectangle rect(int i) {
      int w = this.getWidth() - 2 * PAD;
      if (i < ROWS) {
         return new Rectangle(PAD, PAD + i * ROW, w, ROW - 3);
      }
      int cap = (w - 3 * PAD) / 4;
      return new Rectangle(PAD + (i - ROWS) * (cap + PAD), PAD + ROWS * ROW + 2, cap, ROW - 5);
   }

   /** The shortcut at (x, y), or -1. */
   int at(int x, int y) {
      for (int i = 0; i < CODES.length; i++) {
         if (this.rect(i).contains(x, y)) {
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
      if (i != this.hover) {
         this.hover = i;
         this.repaint();
      }
   }

   private void pick(int step) {
      this.picked = (this.picked + step + CODES.length) % CODES.length;
      this.name();
      this.repaint();
   }

   private void name() {
      this.getAccessibleContext().setAccessibleName("Windows key shortcuts: Win+" + CAPS[this.picked] + ", "
         + NAMES[this.picked] + ". Arrow keys for another, Space to press it");
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(WindowsKey.BACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      Font capFont = new Font("SansSerif", Font.BOLD, 11);
      Font nameFont = new Font("SansSerif", Font.PLAIN, 11);
      boolean focus = this.isFocusOwner();
      for (int i = 0; i < CODES.length; i++) {
         Rectangle r = this.rect(i);
         boolean lit = i == this.held;
         g.setColor(lit ? LIT : i == this.hover ? HOVER : FACE);
         g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
         if (focus && i == this.picked) {
            g.setColor(TEXT);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
         }
         g.setFont(capFont);
         int capW = i < ROWS ? Math.max(18, g.getFontMetrics().stringWidth(CAPS[i]) + 8) : r.width;
         int base = r.y + (r.height + g.getFontMetrics().getAscent()) / 2 - 2;
         if (i < ROWS) {
            g.setColor(lit ? Color.WHITE : CAP);
            g.fillRoundRect(r.x + 3, r.y + 3, capW, r.height - 6, 6, 6);
            g.setColor(Color.WHITE);
            g.drawString(CAPS[i], r.x + 3 + (capW - g.getFontMetrics().stringWidth(CAPS[i])) / 2, base);
            g.setFont(nameFont);
            g.setColor(lit ? Color.WHITE : TEXT);
            g.drawString(NAMES[i], r.x + capW + 8, base);
         } else {
            g.setColor(lit ? Color.WHITE : DIM);
            g.drawString(CAPS[i], r.x + (r.width - g.getFontMetrics().stringWidth(CAPS[i])) / 2, base);
         }
      }
   }
}
