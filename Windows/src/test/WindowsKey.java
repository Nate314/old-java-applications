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
 * arrow) makes Win+that key. Space or Enter, when it has the keyboard focus, taps it.
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

   private final Keys keys;
   private boolean down;
   private boolean hover;

   WindowsKey(Keys keys) {
      this.keys = keys;
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setToolTipText("Click for Start. Hold it and press a key for Win+that key.");
      this.getAccessibleContext().setAccessibleName("Windows key: click for Start, or hold it and press a key for Win+that key");
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               WindowsKey.this.setDown(true);
            }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               WindowsKey.this.setDown(false);
            }
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
      });
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
      g.translate(MARGIN, MARGIN);
      int w = this.getWidth() - 2 * MARGIN;
      int h = this.getHeight() - 2 * MARGIN;
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
      String caption = this.down ? "+ a key" : "Start";
      g.setColor(this.down ? Color.WHITE : TEXT);
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.drawString(caption, (w - g.getFontMetrics().stringWidth(caption)) / 2, y + size + 22);
   }
}
