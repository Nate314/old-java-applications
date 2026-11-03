package windowse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JButton;

/**
 * One shortcut's key on the keyboard. It is a button (focus, Space and Enter, screen reader name) that draws itself
 * as a keycap: its letters, the word for what it does underneath, blue when picked, pushed down while pressed.
 */
final class KeyCap extends JButton {
   static final Color ACCENT = new Color(0, 120, 215);
   static final Color ACCENT_TEXT = new Color(110, 186, 255);
   private static final Color ACCENT_SIDE = new Color(0, 78, 150);
   private static final Color LIT = new Color(96, 182, 255);
   private static final Color FACE = new Color(66, 70, 82);
   private static final Color FACE_HOVER = new Color(82, 87, 102);
   private static final Color SIDE = new Color(36, 38, 46);
   private static final Color TEXT = new Color(236, 239, 244);

   final Shortcut shortcut;
   private boolean picked;
   private boolean pushed;
   private boolean hover;

   /** onPick runs on a click (or Space/Enter); onPress on a double click. */
   KeyCap(Shortcut shortcut, Consumer<Shortcut> onPick, Consumer<Shortcut> onPress) {
      this.shortcut = shortcut;
      this.setText("");
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setToolTipText(shortcut.combo() + ": " + shortcut.does);
      this.addActionListener(event -> onPick.accept(shortcut));
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
               onPress.accept(shortcut);
            }
         }

         @Override
         public void mouseEntered(MouseEvent event) {
            KeyCap.this.hover = true;
            KeyCap.this.repaint();
         }

         @Override
         public void mouseExited(MouseEvent event) {
            KeyCap.this.hover = false;
            KeyCap.this.repaint();
         }
      });
      this.describe();
   }

   void setPicked(boolean picked) {
      if (this.picked != picked) {
         this.picked = picked;
         this.describe();
         this.repaint();
      }
   }

   /** Drawn pushed down: the key is being pressed right now. */
   void setPushed(boolean pushed) {
      if (this.pushed != pushed) {
         this.pushed = pushed;
         this.repaint();
      }
   }

   private void describe() {
      this.getAccessibleContext().setAccessibleName(
         this.shortcut.combo() + ", " + this.shortcut.title + ": " + this.shortcut.does + (this.picked ? ", picked" : ""));
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = this.getWidth();
      int h = this.getHeight();
      int down = this.pushed ? 3 : 0;
      Color face = this.pushed ? LIT : this.picked ? ACCENT : this.hover ? FACE_HOVER : FACE;
      g.setColor(this.picked || this.pushed ? ACCENT_SIDE : SIDE);
      g.fillRoundRect(0, 3, w, h - 3, 9, 9);
      g.setColor(face);
      g.fillRoundRect(0, down, w, h - 4, 9, 9);
      if (!this.picked && !this.pushed) {
         g.setColor(ACCENT);
         g.setStroke(new BasicStroke(1.5f));
         g.drawRoundRect(1, down + 1, w - 3, h - 7, 8, 8);
      }
      if (this.isFocusOwner()) {
         g.setColor(TEXT);
         g.setStroke(new BasicStroke(1f));
         g.drawRoundRect(4, down + 4, w - 9, h - 13, 6, 6);
      }
      boolean bright = this.picked || this.pushed;
      Color ink = bright ? Color.WHITE : TEXT;
      int cx = w / 2;
      int cy = (h - 4) / 2 + down;
      if (this.shortcut == Shortcut.START) {
         paintLogo(g, cx - 8, cy - 8 - (this.shortcut.caption.isEmpty() ? 0 : 5), 16, bright ? Color.WHITE : ACCENT_TEXT);
      } else if (this.shortcut.legend.isEmpty()) {
         paintArrow(g, this.shortcut, cx, cy - (this.shortcut.caption.isEmpty() ? 0 : 5), ink);
      } else {
         g.setColor(ink);
         g.setFont(new Font("SansSerif", Font.BOLD, 14));
         g.drawString(this.shortcut.legend, 8, 19 + down);
      }
      if (!this.shortcut.caption.isEmpty()) {
         g.setColor(bright ? Color.WHITE : ACCENT_TEXT);
         g.setFont(new Font("SansSerif", Font.PLAIN, 9));
         int width = g.getFontMetrics().stringWidth(this.shortcut.caption);
         int x = this.shortcut.legend.isEmpty() ? cx - width / 2 : 8;
         g.drawString(this.shortcut.caption, x, h - 10 + down);
      }
   }

   /** An arrow pointing the way the shortcut's arrow key does. */
   static void paintArrow(Graphics2D g, Shortcut shortcut, int cx, int cy, Color ink) {
      int[][] shape = {{0, -7}, {7, 0}, {3, 0}, {3, 7}, {-3, 7}, {-3, 0}, {-7, 0}};   // pointing up
      int[] xs = new int[shape.length];
      int[] ys = new int[shape.length];
      for (int i = 0; i < shape.length; i++) {
         int x = shape[i][0];
         int y = shape[i][1];
         switch (shortcut) {
            case DOWN: xs[i] = cx - x; ys[i] = cy - y; break;
            case LEFT: xs[i] = cx + y; ys[i] = cy - x; break;
            case RIGHT: xs[i] = cx - y; ys[i] = cy + x; break;
            default: xs[i] = cx + x; ys[i] = cy + y; break;
         }
      }
      g.setColor(ink);
      g.fillPolygon(xs, ys, shape.length);
   }

   /** The Windows logo: four squares. */
   static void paintLogo(Graphics2D g, int x, int y, int size, Color color) {
      int half = (size - 2) / 2;
      g.setColor(color);
      g.fillRect(x, y, half, half);
      g.fillRect(x + half + 2, y, half, half);
      g.fillRect(x, y + half + 2, half, half);
      g.fillRect(x + half + 2, y + half + 2, half, half);
   }
}
