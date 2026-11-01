package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;

/**
 * A column-first grid of app icons on the desktop, below every window. Click selects an icon, double-click opens
 * the app, and a click on the empty desktop clears the selection and closes the start menu.
 */
final class DesktopShortcuts {
   private static final Integer LAYER = -1;
   private static final int TILE_WIDTH = 96;
   private static final int TILE_HEIGHT = 92;
   private static final int ICON = 44;
   private static final int MARGIN = 12;
   private final List<Shortcut> shortcuts = new ArrayList<>();
   private Shortcut selected;

   DesktopShortcuts(Main screen, Launcher launcher) {
      JDesktopPane desktop = screen.desktopPane;
      int rows = Math.max(1, (screen.getHeight() - screen.launcherWindow.getHeight() - 2 * MARGIN) / TILE_HEIGHT);
      for (int i = 0; i < launcher.count(); i++) {
         Shortcut shortcut = new Shortcut(launcher, i, screen);
         shortcut.setBounds(MARGIN + i / rows * TILE_WIDTH, MARGIN + i % rows * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
         desktop.add(shortcut, LAYER);
         this.shortcuts.add(shortcut);
      }
      desktop.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            DesktopShortcuts.this.select(null);
            screen.startMenu.hideMenu();
         }
      });
   }

   private void select(Shortcut shortcut) {
      Shortcut previous = this.selected;
      this.selected = shortcut;
      if (previous != null) {
         previous.repaint();
      }
      if (shortcut != null) {
         shortcut.repaint();
      }
   }

   private final class Shortcut extends JComponent {
      private final Launcher launcher;
      private final int app;
      private boolean hovered;

      Shortcut(Launcher launcher, int app, Main screen) {
         this.launcher = launcher;
         this.app = app;
         this.setCursor(screen.getCursor());
         this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
               DesktopShortcuts.this.select(Shortcut.this);
               screen.startMenu.hideMenu();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
               if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                  launcher.launch(app);
               }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
               Shortcut.this.hovered = true;
               Shortcut.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               Shortcut.this.hovered = false;
               Shortcut.this.repaint();
            }
         });
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         AppIcon.smooth(g);
         boolean isSelected = DesktopShortcuts.this.selected == this;
         if (isSelected || this.hovered) {
            g.setColor(isSelected ? new Color(0, 120, 215, 110) : new Color(255, 255, 255, 50));
            g.fillRoundRect(2, 2, TILE_WIDTH - 4, TILE_HEIGHT - 4, 10, 10);
         }
         String name = this.launcher.name(this.app);
         AppIcon.paint(g, this.launcher.icon(this.app), name, (TILE_WIDTH - ICON) / 2, 8, ICON);

         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
         FontMetrics metrics = g.getFontMetrics();
         List<String> lines = wrap(AppIcon.words(name), metrics, TILE_WIDTH - 8);
         int y = 8 + ICON + 4 + metrics.getAscent();
         for (String line : lines) {
            int x = (TILE_WIDTH - metrics.stringWidth(line)) / 2;
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(line, x + 1, y + 1);
            g.setColor(Color.WHITE);
            g.drawString(line, x, y);
            y += metrics.getHeight() - 2;
         }
      }
   }

   /** Up to two lines of words, the second cut with "..." if the rest does not fit. */
   private static List<String> wrap(String text, FontMetrics metrics, int width) {
      List<String> lines = new ArrayList<>();
      String line = "";
      String[] words = text.split(" ");
      for (int i = 0; i < words.length; i++) {
         String candidate = line.isEmpty() ? words[i] : line + " " + words[i];
         if (line.isEmpty() || metrics.stringWidth(candidate) <= width) {
            line = candidate;
         } else if (lines.isEmpty()) {
            lines.add(AppIcon.fit(line, metrics, width));
            line = words[i];
         } else {
            line = candidate;
         }
      }
      lines.add(AppIcon.fit(line, metrics, width));
      return lines;
   }
}
