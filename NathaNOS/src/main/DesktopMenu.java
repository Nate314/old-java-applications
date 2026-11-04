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
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * The menu a right click on the empty desktop opens (a long press on a touch screen): pick a wallpaper from small
 * previews of each, or line the icons back up by name.
 */
final class DesktopMenu extends JInternalFrame {
   private static final Color BACKGROUND = new Color(26, 35, 54);
   private static final int WIDTH = 236;
   private static final int TITLE = 26;
   private static final int ROW = 40;
   private static final int SWATCH_WIDTH = 48;
   private static final int SWATCH_HEIGHT = 30;
   private final Main screen;
   private final List<Row> wallpaperRows = new ArrayList<>();

   DesktopMenu(Main screen, DesktopShortcuts shortcuts) {
      super("Desktop", false, false, false);
      this.screen = screen;
      this.setVisible(false);
      this.setBorder(null);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);

      JPanel panel = new JPanel();
      panel.setLayout(null);
      panel.setBackground(BACKGROUND);
      this.add(panel);

      int y = 4;
      panel.add(heading("Wallpaper", y));
      y += TITLE;
      for (Wallpaper wallpaper : Wallpaper.ALL) {
         Row row = new Row(wallpaper, wallpaper.name, () -> {
            this.hideMenu();
            screen.setWallpaper(wallpaper);
         });
         row.setBounds(4, y, WIDTH - 8, ROW);
         panel.add(row);
         this.wallpaperRows.add(row);
         y += ROW;
      }
      y += 6;
      JComponent divider = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            g.setColor(LauncherWindow.HOVER);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
         }
      };
      divider.setBounds(8, y, WIDTH - 16, 1);
      panel.add(divider);
      y += 6;
      Row arrange = new Row(null, "Arrange icons by name", () -> {
         this.hideMenu();
         shortcuts.arrangeByName();
      });
      arrange.setBounds(4, y, WIDTH - 8, ROW - 6);
      panel.add(arrange);
      y += ROW - 6 + 4;
      this.setSize(WIDTH, y);
   }

   /** Opens with its corner at (x, y), kept on the screen above the taskbar. */
   void showAt(int x, int y) {
      for (Row row : this.wallpaperRows) {
         row.on = row.wallpaper == this.screen.wallpaper;
      }
      int bottom = this.screen.getHeight() - this.screen.launcherWindow.getHeight();
      this.setLocation(Math.max(0, Math.min(x, this.screen.getWidth() - this.getWidth())), Math.max(0, Math.min(y, bottom - this.getHeight())));
      this.setVisible(true);
      this.moveToFront();
      this.repaint();
   }

   void hideMenu() {
      this.setVisible(false);
   }

   private static JComponent heading(String text, int y) {
      JComponent heading = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            AppIcon.smooth(g);
            g.setColor(new Color(150, 170, 200));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.drawString(text.toUpperCase(), 10, 17);
         }
      };
      heading.setBounds(4, y, WIDTH - 8, TITLE);
      return heading;
   }

   /** A menu row: a wallpaper preview (or none) and a label, lit under the mouse; the current wallpaper is ticked. */
   private final class Row extends JComponent {
      private final Wallpaper wallpaper;
      private final String label;
      private boolean hovered;
      private boolean on;

      Row(Wallpaper wallpaper, String label, Runnable action) {
         this.wallpaper = wallpaper;
         this.label = label;
         this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               Row.this.hovered = true;
               Row.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               Row.this.hovered = false;
               Row.this.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
               if (e.getButton() == MouseEvent.BUTTON1) {
                  action.run();
               }
            }
         });
      }

      @Override
      protected void paintComponent(Graphics g) {
         AppIcon.smooth(g);
         int w = this.getWidth();
         int h = this.getHeight();
         if (this.hovered || this.on) {
            g.setColor(this.hovered ? LauncherWindow.HOVER : LauncherWindow.ON);
            g.fillRoundRect(0, 1, w, h - 2, 8, 8);
         }
         int x = 10;
         if (this.wallpaper != null) {
            this.wallpaper.paintSwatch(g, x, (h - SWATCH_HEIGHT) / 2, SWATCH_WIDTH, SWATCH_HEIGHT, DesktopMenu.this.screen.photo, this);
            x += SWATCH_WIDTH + 10;
         }
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
         FontMetrics metrics = g.getFontMetrics();
         int baseline = (h + metrics.getAscent() - metrics.getDescent()) / 2;
         g.setColor(FlatButton.TEXT);
         g.drawString(AppIcon.fit(this.label, metrics, w - x - 30), x, baseline);
         if (this.on) {
            // A tick at the right end of the current wallpaper's row.
            g.setColor(new Color(0, 150, 255));
            int cx = w - 22;
            int cy = h / 2;
            g.fillPolygon(new int[] {cx - 6, cx - 3, cx + 6, cx + 8, cx - 3, cx - 8}, new int[] {cy, cy - 2, cy - 8, cy - 6, cy + 5, cy + 1}, 6);
         }
      }
   }
}
