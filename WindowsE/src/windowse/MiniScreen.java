package windowse;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Draws a {@link Screen} small: wallpaper, the File Explorer windows in stacking order, the open menus, a taskbar; with a {@link Motion}, gliding. */
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
      paint(g, screen, x, y, w, h, null, 0);
   }

   /** motion (null: none) glides each window and the Start menu to its place; now: the time in ms. */
   static void paint(Graphics2D g, Screen screen, int x, int y, int w, int h, Motion motion, long now) {
      int bar = Math.max(8, h / 12);
      int workH = h - bar;
      int icon = bar - 4;
      int n = Math.min(screen.windows.size(), 6);   // shortcut: one folder per window up to 6, as Windows groups them anyway
      int ix = (w - (n + 1) * (icon + 4)) / 2;
      Graphics2D in = (Graphics2D) g.create();
      in.clipRect(x, y, w, h);   // a menu gliding off the screen stays on it
      in.setColor(WALL);
      in.fillRect(x, y, w, h);
      in.setColor(BLOOM);
      in.fillOval(x + w / 4, y + workH / 6, w / 2, workH * 5 / 6);
      in.setColor(BLOOM_IN);
      in.fillOval(x + w * 3 / 8, y + workH / 3, w / 4, workH / 2);

      List<Screen.Window> order = new ArrayList<>(screen.windows);
      order.sort((a, b) -> a.z - b.z);
      Screen.Window top = screen.top();
      if (screen.tasks) {
         paintTasks(in, order, x, y, w, workH);
      } else {
         for (Screen.Window win : order) {
            // a minimized (or hidden) window's place is its taskbar icon, so it sinks there and grows from there
            int[] dock = {ix + (Math.min(win.slot, 5) + 1) * (icon + 4), workH + 2, icon, icon - 2};
            int[] r = win.min ? dock : place(win, w, workH);
            int[] at = motion == null ? r : motion.at(win.slot, r, dock, now);
            if (!win.min || !Arrays.equals(at, r)) {
               paintWindow(in, x + at[0], y + at[1], at[2], at[3], win == top);
            }
         }
      }
      int sw = w / 2, sh = workH * 3 / 5, sx = (w - sw) / 2;
      int[] menu = {sx, screen.start ? workH - sh - 3 : h, sw, sh};
      int[] m = motion == null ? menu : motion.at(-1, menu, null, now);
      if (m[1] < h) {   // open, or still sliding down out of sight
         paintStart(in, x + m[0], y + m[1], m[2], m[3]);
      }
      if (screen.run) {
         int rw = w * 2 / 5, rh = workH / 4, rx = x + 4, ry = y + workH - rh - 4;
         paintWindow(in, rx, ry, rw, rh, true);
         in.setColor(Color.WHITE);
         in.fillRect(rx + 4, ry + rh / 2, rw - 8, Math.max(3, rh / 5));
      }

      in.setColor(BAR);
      in.fillRect(x, y + workH, w, bar);
      KeyCap.paintLogo(in, x + ix, y + workH + 2, icon, KeyCap.ACCENT_TEXT);
      for (int i = 0; i < n; i++) {
         int fx = x + ix + (i + 1) * (icon + 4);
         in.setColor(FOLDER);
         in.fillRect(fx, y + workH + 3, icon, icon - 2);
         if (!screen.windows.get(i).min) {
            in.setColor(KeyCap.ACCENT_TEXT);
            in.fillRect(fx + 1, y + h - 2, icon - 2, 2);
         }
      }
      in.dispose();
   }

   private static void paintStart(Graphics2D g, int sx, int sy, int sw, int sh) {
      g.setColor(PANE);
      g.fillRoundRect(sx, sy, sw, sh, 6, 6);
      int tile = Math.max(4, sw / 9);
      for (int i = 0; i < 12; i++) {
         g.setColor(i % 5 == 0 ? KeyCap.ACCENT : TILE);
         g.fillRoundRect(sx + tile / 2 + (i % 6) * (tile * 4 / 3), sy + tile / 2 + (i / 6) * (tile * 4 / 3), tile, tile, 2, 2);
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
