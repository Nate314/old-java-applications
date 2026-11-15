package test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * A Windows key on the screen that works like the one on a keyboard: it goes down when the mouse button does and
 * comes up when it does, so a click opens Start and holding it while pressing a key on the keyboard (E, D, R, an
 * arrow) makes Win+that key. Sliding off it while held, left, right, up or down, makes Win+that arrow when it is let go, so a
 * window can be snapped with one finger and no keyboard. Space or Enter, when it has the keyboard focus, taps it.
 */
final class WindowsKey extends JButton {
   /** Where the presses go: java.awt.Robot in the app, a list in the self-check. */
   interface Keys {
      void press(int keyCode);

      void release(int keyCode);
   }

   static final int WIN = KeyEvent.VK_WINDOWS;
   private static final Color ACCENT = new Color(0, 120, 215);
   private static final Color ACCENT_TEXT = new Color(110, 186, 255);
   private static final Color ACCENT_SIDE = new Color(0, 78, 150);
   private static final Color LIT = new Color(96, 182, 255);
   private static final Color FACE = new Color(66, 70, 82);
   private static final Color FACE_HOVER = new Color(82, 87, 102);
   private static final Color SIDE = new Color(36, 38, 46);
   private static final Color TEXT = new Color(236, 239, 244);
   static final Color BACK = new Color(28, 30, 36);
   private static final int MARGIN = 8;
   /** How far the pointer must slide from where it pressed the key before the release makes Win+an arrow. */
   static final int SLIDE = 28;

   private final Keys keys;
   private boolean down;
   private boolean hover;
   private int pressX;
   private int pressY;
   /** The arrow the pointer has slid toward while the key is held (KeyEvent.VK_LEFT and so on), or 0. */
   private int slide;

   WindowsKey(Keys keys) {
      this.keys = keys;
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setToolTipText("Click for Start. Hold it and press a key for Win+that key, or slide off it for Win+that arrow.");
      this.getAccessibleContext().setAccessibleName("Windows key: click for Start, or hold it and press a key for Win+that key, or slide off it for Win+that arrow");
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               WindowsKey.this.pressX = e.getX();
               WindowsKey.this.pressY = e.getY();
               WindowsKey.this.slide = 0;
               WindowsKey.this.setDown(true);
            }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               WindowsKey.this.slideTo(e.getX(), e.getY());
               int arrow = WindowsKey.this.slide;
               if (arrow != 0 && WindowsKey.this.down) {
                  WindowsKey.this.keys.press(arrow);
                  WindowsKey.this.keys.release(arrow);
               }
               WindowsKey.this.slide = 0;
               WindowsKey.this.setDown(false);
            }
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            WindowsKey.this.slideTo(e.getX(), e.getY());
         }

         @Override
         public void mouseEntered(MouseEvent e) {
            WindowsKey.this.hover = true;
            WindowsKey.this.repaint();
         }

         @Override
         public void mouseExited(MouseEvent e) {
            WindowsKey.this.hover = false;
            WindowsKey.this.repaint();
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
               WindowsKey.this.tap();
            }
         }
      });
   }

   /** Down presses the Windows key, up lets go of it; a second down (or up) in a row does nothing. */
   void setDown(boolean down) {
      if (this.down == down) {
         return;
      }
      this.down = down;
      if (down) {
         this.keys.press(WIN);
      } else {
         this.keys.release(WIN);
      }
      this.repaint();
   }

   /** Which arrow a slide to (x, y) from the press makes: the longer way it went, once past SLIDE; back near the key, none. */
   void slideTo(int x, int y) {
      if (!this.down) {
         return;
      }
      int dx = x - this.pressX;
      int dy = y - this.pressY;
      int arrow = Math.max(Math.abs(dx), Math.abs(dy)) < SLIDE ? 0
         : Math.abs(dx) >= Math.abs(dy) ? (dx < 0 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT)
         : (dy < 0 ? KeyEvent.VK_UP : KeyEvent.VK_DOWN);
      if (arrow != this.slide) {
         this.slide = arrow;
         this.repaint();
      }
   }

   int slide() {
      return this.slide;
   }

   boolean isDown() {
      return this.down;
   }

   void tap() {
      this.setDown(true);
      this.setDown(false);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      // Drawn at the small size and scaled, so a bigger key is the same key, bigger.
      double s = this.getWidth() / (double) Test.WIDTH;
      g.scale(s, s);
      g.translate(MARGIN, MARGIN);
      int w = (int) Math.round(this.getWidth() / s) - 2 * MARGIN;
      int h = (int) Math.round(this.getHeight() / s) - 2 * MARGIN;
      int sink = this.down ? 4 : 0;
      g.setColor(this.down ? ACCENT_SIDE : SIDE);
      g.fillRoundRect(0, 5, w, h - 5, 16, 16);
      g.setColor(this.down ? LIT : this.hover ? FACE_HOVER : FACE);
      g.fillRoundRect(0, sink, w, h - 6, 16, 16);
      if (!this.down) {
         g.setColor(ACCENT);
         g.setStroke(new BasicStroke(2f));
         g.drawRoundRect(1, 1, w - 3, h - 9, 14, 14);
      }
      if (this.isFocusOwner()) {
         g.setColor(TEXT);
         g.setStroke(new BasicStroke(1f));
         g.drawRoundRect(6, sink + 6, w - 13, h - 19, 10, 10);
      }
      int size = Math.min(w, h) / 3;
      int x = (w - size) / 2;
      int y = (h - 6) / 2 - size / 2 - 8 + sink;
      int half = (size - 3) / 2;
      g.setColor(this.down ? Color.WHITE : ACCENT_TEXT);
      g.fillRect(x, y, half, half);
      g.fillRect(x + half + 3, y, half, half);
      g.fillRect(x, y + half + 3, half, half);
      g.fillRect(x + half + 3, y + half + 3, half, half);
      String caption = !this.down ? "Start" : this.slide == 0 ? "+ a key" : "+ " + "←↑→↓".charAt(this.slide - KeyEvent.VK_LEFT);
      g.setColor(this.down ? Color.WHITE : TEXT);
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.drawString(caption, (w - g.getFontMetrics().stringWidth(caption)) / 2, y + size + 22);
   }
}
