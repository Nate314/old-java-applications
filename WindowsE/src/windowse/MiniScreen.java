package windowse;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Draws a {@link Screen} small: wallpaper, the File Explorer windows in stacking order, the open menus, a taskbar. */
final class MiniScreen {
   private static final Color WALL = new Color(0, 72, 150);
   private static final Color BLOOM = new Color(40, 128, 214);
   private static final Color BLOOM_IN = new Color(120, 186, 245);
   private static final Color BAR = new Color(30, 32, 40);
   private static final Color FOLDER = new Color(244, 190, 60);
   private static final Color PANE = new Color(44, 47, 56);
   private static final Color TILE = new Color(86, 92, 108);
   private static final Color EDGE = new Color(150, 156, 170);
   private static final Color TITLE = new Color(228, 232, 240);
   private static final Color ROW = new Color(214, 218, 226);

   private MiniScreen() {
   }

   static void paint(Graphics2D g, Screen screen, int x, int y, int w, int h) {
      int bar = Math.max(8, h / 12);
      int workH = h - bar;
      g.setColor(WALL);
      g.fillRect(x, y, w, h);
      g.setColor(BLOOM);
      g.fillOval(x + w / 4, y + workH / 6, w / 2, workH * 5 / 6);
      g.setColor(BLOOM_IN);
      g.fillOval(x + w * 3 / 8, y + workH / 3, w / 4, workH / 2);

      List<Screen.Window> order = new ArrayList<>(screen.windows);
      order.sort((a, b) -> a.z - b.z);
      Screen.Window top = screen.top();
      if (screen.tasks) {
         paintTasks(g, order, x, y, w, workH);
      } else {
         for (Screen.Window win : order) {
            if (!win.min) {
               int[] r = place(win, w, workH);
               paintWindow(g, x + r[0], y + r[1], r[2], r[3], win == top);
            }
         }
      }
      if (screen.start) {
         int sw = w / 2, sh = workH * 3 / 5, sx = x + (w - sw) / 2, sy = y + workH - sh - 3;
         g.setColor(PANE);
         g.fillRoundRect(sx, sy, sw, sh, 6, 6);
         int tile = Math.max(4, sw / 9);
         for (int i = 0; i < 12; i++) {
            g.setColor(i % 5 == 0 ? KeyCap.ACCENT : TILE);
            g.fillRoundRect(sx + tile / 2 + (i % 6) * (tile * 4 / 3), sy + tile / 2 + (i / 6) * (tile * 4 / 3), tile, tile, 2, 2);
         }
      }
      if (screen.run) {
         int rw = w * 2 / 5, rh = workH / 4, rx = x + 4, ry = y + workH - rh - 4;
         paintWindow(g, rx, ry, rw, rh, true);
         g.setColor(Color.WHITE);
         g.fillRect(rx + 4, ry + rh / 2, rw - 8, Math.max(3, rh / 5));
      }

      g.setColor(BAR);
      g.fillRect(x, y + workH, w, bar);
      int icon = bar - 4;
      int n = Math.min(screen.windows.size(), 6);   // shortcut: one folder per window up to 6, as Windows groups them anyway
      int ix = x + (w - (n + 1) * (icon + 4)) / 2;
      KeyCap.paintLogo(g, ix, y + workH + 2, icon, KeyCap.ACCENT_TEXT);
      for (int i = 0; i < n; i++) {
         int fx = ix + (i + 1) * (icon + 4);
         g.setColor(FOLDER);
         g.fillRect(fx, y + workH + 3, icon, icon - 2);
         if (!screen.windows.get(i).min) {
            g.setColor(KeyCap.ACCENT_TEXT);
            g.fillRect(fx + 1, y + h - 2, icon - 2, 2);
         }
      }
   }

   /** Where a window sits on the work area: x, y, width, height. */
   private static int[] place(Screen.Window win, int w, int workH) {
      switch (win.snap) {
         case "left": return new int[] {0, 0, w / 2, workH};
         case "right": return new int[] {w / 2, 0, w - w / 2, workH};
         case "max": return new int[] {0, 0, w, workH};
         default:
            int ww = w * 11 / 20, wh = workH * 3 / 5, step = Math.max(3, w / 28);
            int room = Math.max(1, Math.min(w - ww, workH - wh) / step);   // cascades, starting over when the next would not fit
            int at = 2 + (win.slot % room) * step;
            return new int[] {at, at, ww, wh};
      }
   }

   private static void paintWindow(Graphics2D g, int x, int y, int w, int h, boolean front) {
      int title = Math.max(4, h / 9);
      g.setColor(Color.WHITE);
      g.fillRect(x, y, w, h);
      g.setColor(TITLE);
      g.fillRect(x, y, w, title);
      g.setColor(FOLDER);
      g.fillRect(x + 2, y + 1, Math.max(2, title - 2), Math.max(2, title - 2));
      g.setColor(ROW);
      for (int ry = y + title + 3; ry + 2 < y + h - 2; ry += 5) {
         g.fillRect(x + 4, ry, Math.max(2, w / 3), 2);
      }
      g.setColor(front ? KeyCap.ACCENT : EDGE);
      g.drawRect(x, y, w - 1, h - 1);
   }

   /** Task view: the windows as small cards (up to six). */
   private static void paintTasks(Graphics2D g, List<Screen.Window> order, int x, int y, int w, int h) {
      g.setColor(PANE);
      g.fillRect(x, y, w, h);
      int cw = w / 4, ch = h / 4, cols = 3;
      for (int i = 0; i < Math.min(order.size(), 6); i++) {
         int cx = x + cw / 4 + (i % cols) * (cw + cw / 4), cy = y + ch / 2 + (i / cols) * (ch + ch / 2);
         paintWindow(g, cx, cy, cw, ch, i == 0);
      }
   }
}
