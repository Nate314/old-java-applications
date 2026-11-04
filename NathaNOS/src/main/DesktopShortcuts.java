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
import javax.swing.JLayeredPane;

/**
 * The app icons on the desktop, below every window, on a grid of cells, followed by an icon for each picture painted
 * in Paint (its thumbnail, kept up to date while it is painted). Click selects an icon, double-click opens the app or
 * the picture, and dragging an icon moves it to another cell (trading places with an icon already there); from the
 * first move on, every icon's cell is saved. Until then, and for an app or picture new to the desktop, icons fill the
 * free cells column by column, apps in alphabetical order and then the pictures. A click on the
 * empty desktop clears the selection and closes the menus; a right click opens the desktop menu.
 */
final class DesktopShortcuts {
   private static final Integer LAYER = -1;
   private static final int MAX_TILE_WIDTH = 96;
   private static final int TILE_HEIGHT = 92;
   private static final int ICON = 44;
   private static final int MARGIN = 12;
   /** How far the pointer moves before a press on an icon becomes a drag. */
   private static final int DRAG_START = 5;
   /** Saved places of picture icons are keyed by this and the picture's name, apart from the apps' names. */
   private static final String PICTURE_KEY = "picture:";
   private final Main screen;
   private final Launcher launcher;
   private final List<Shortcut> shortcuts = new ArrayList<>();
   private final int rows;
   private int columns;
   private int tileWidth;
   private final DragGhost ghost = new DragGhost();
   private Shortcut selected;

   DesktopShortcuts(Main screen, Launcher launcher) {
      this.screen = screen;
      this.launcher = launcher;
      JDesktopPane desktop = screen.desktopPane;
      this.rows = Math.max(1, (screen.getHeight() - screen.launcherWindow.getHeight() - 2 * MARGIN) / TILE_HEIGHT);
      for (int i = 0; i < launcher.count(); i++) {
         Shortcut shortcut = new AppShortcut(i);
         desktop.add(shortcut, LAYER);
         this.shortcuts.add(shortcut);
      }
      this.ghost.setVisible(false);
      desktop.add(this.ghost, JLayeredPane.DRAG_LAYER);
      this.syncPictures();
      this.placeAll(); // syncPictures places the icons only when the pictures changed
      screen.pictures.onChange(this::syncPictures);
      desktop.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            DesktopShortcuts.this.select(null);
            screen.startMenu.hideMenu();
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
               screen.desktopMenu.showAt(e.getX(), e.getY());
            } else {
               screen.desktopMenu.hideMenu();
            }
         }
      });
   }

   /** Forgets every hand-placed icon and lines them all up alphabetically again. */
   void arrangeByName() {
      this.screen.settings.places.clear();
      this.screen.saveSettings();
      this.placeAll();
   }

   /** Adds an icon for each new picture and drops those of deleted ones; the rest just repaint their thumbnails. */
   private void syncPictures() {
      List<Picture> pictures = this.screen.pictures.all();
      boolean changed = false;
      for (Shortcut shortcut : new ArrayList<>(this.shortcuts)) {
         if (shortcut instanceof PictureShortcut && !pictures.contains(((PictureShortcut)shortcut).picture)) {
            this.shortcuts.remove(shortcut);
            this.screen.desktopPane.remove(shortcut);
            this.screen.settings.places.remove(shortcut.key);
            if (this.selected == shortcut) {
               this.selected = null;
            }
            changed = true;
         }
      }
      for (Picture picture : pictures) {
         if (this.pictureShortcut(picture) == null) {
            Shortcut shortcut = new PictureShortcut(picture);
            this.screen.desktopPane.add(shortcut, LAYER);
            this.shortcuts.add(shortcut);
            changed = true;
         }
      }
      if (changed) {
         this.placeAll();
         this.screen.desktopPane.repaint();
      } else {
         for (Shortcut shortcut : this.shortcuts) {
            if (shortcut instanceof PictureShortcut) {
               shortcut.repaint();
            }
         }
      }
   }

   private Shortcut pictureShortcut(Picture picture) {
      for (Shortcut shortcut : this.shortcuts) {
         if (shortcut instanceof PictureShortcut && ((PictureShortcut)shortcut).picture == picture) {
            return shortcut;
         }
      }
      return null;
   }

   /** Saved places first (if still on the grid and free), then the rest into the first free cells. */
   private void placeAll() {
      // Tiles narrow on a small screen so every column of icons fits across it.
      int needed = (Math.max(1, this.shortcuts.size()) + this.rows - 1) / this.rows;
      this.tileWidth = Math.max(48, Math.min(MAX_TILE_WIDTH, (this.screen.getWidth() - 2 * MARGIN) / needed));
      this.columns = Math.max(needed, (this.screen.getWidth() - 2 * MARGIN) / this.tileWidth);
      Shortcut[][] grid = new Shortcut[this.columns][this.rows];
      List<Shortcut> unplaced = new ArrayList<>();
      for (Shortcut shortcut : this.shortcuts) {
         int[] place = this.screen.settings.places.get(shortcut.key);
         if (place != null && place[0] < this.columns && place[1] < this.rows && grid[place[0]][place[1]] == null) {
            grid[place[0]][place[1]] = shortcut;
            shortcut.moveTo(place[0], place[1]);
         } else {
            unplaced.add(shortcut);
         }
      }
      int cell = 0;
      for (Shortcut shortcut : unplaced) {
         while (cell < this.columns * this.rows && grid[cell / this.rows][cell % this.rows] != null) {
            cell++;
         }
         if (cell < this.columns * this.rows) {
            grid[cell / this.rows][cell % this.rows] = shortcut;
         }
         // More icons than cells (a tiny screen) stack on the last cell rather than vanish.
         int at = Math.min(cell, this.columns * this.rows - 1);
         shortcut.moveTo(at / this.rows, at % this.rows);
      }
   }

   private Shortcut at(int column, int row) {
      for (Shortcut shortcut : this.shortcuts) {
         if (shortcut.column == column && shortcut.row == row) {
            return shortcut;
         }
      }
      return null;
   }

   /** Moves the icon to the cell under (x, y) on the desktop, trading places with an icon already there, and saves. */
   private void drop(Shortcut shortcut, int x, int y) {
      int column = Math.max(0, Math.min(this.columns - 1, (x - MARGIN) / this.tileWidth));
      int row = Math.max(0, Math.min(this.rows - 1, (y - MARGIN) / TILE_HEIGHT));
      Shortcut other = this.at(column, row);
      if (other == shortcut) {
         return;
      }
      if (other != null) {
         other.moveTo(shortcut.column, shortcut.row);
      }
      shortcut.moveTo(column, row);
      // Every icon keeps its cell from now on, so the gap a moved icon leaves stays a gap, as on a real desktop.
      for (Shortcut each : this.shortcuts) {
         this.screen.settings.places.put(each.key, new int[] {each.column, each.row});
      }
      this.screen.saveSettings();
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

   /** An app from the launcher: its icon or initials tile. */
   private final class AppShortcut extends Shortcut {
      private final int app;

      AppShortcut(int app) {
         super(DesktopShortcuts.this.launcher.name(app), AppIcon.words(DesktopShortcuts.this.launcher.name(app)));
         this.app = app;
      }

      @Override
      void open() {
         DesktopShortcuts.this.launcher.launch(this.app);
      }

      @Override
      void paintImage(Graphics g, int x, int y) {
         AppIcon.paint(g, DesktopShortcuts.this.launcher.icon(this.app), this.key, x, y, ICON);
      }
   }

   /** A picture from Paint: a small framed copy of it, opened in Paint by a double-click. */
   private final class PictureShortcut extends Shortcut {
      private final Picture picture;

      PictureShortcut(Picture picture) {
         super(PICTURE_KEY + picture.name, picture.name);
         this.picture = picture;
      }

      @Override
      void open() {
         DesktopShortcuts.this.launcher.openPicture(this.picture);
      }

      @Override
      void paintImage(Graphics g, int x, int y) {
         int w = ICON + 8;
         int h = w * Picture.HEIGHT / Picture.WIDTH;
         int left = x - 4;
         int top = y + (ICON - h) / 2;
         g.setColor(new Color(0, 0, 0, 90));
         g.fillRect(left + 2, top + 2, w + 2, h + 2);
         g.setColor(Color.WHITE);
         g.fillRect(left - 2, top - 2, w + 4, h + 4);
         g.drawImage(this.picture.thumbnail(), left, top, w, h, null);
      }
   }

   private abstract class Shortcut extends JComponent {
      /** The name its saved place is kept under. */
      final String key;
      private final String label;
      private int column;
      private int row;
      private boolean hovered;
      /** Where the press landed in the icon, and whether the pointer has since moved far enough to drag. */
      private int pressX;
      private int pressY;
      private boolean pressed;
      private boolean dragging;

      Shortcut(String key, String label) {
         this.key = key;
         this.label = label;
         this.setCursor(DesktopShortcuts.this.screen.getCursor());
         MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
               DesktopShortcuts.this.select(Shortcut.this);
               DesktopShortcuts.this.screen.startMenu.hideMenu();
               DesktopShortcuts.this.screen.desktopMenu.hideMenu();
               Shortcut.this.pressed = e.getButton() == MouseEvent.BUTTON1;
               Shortcut.this.pressX = e.getX();
               Shortcut.this.pressY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
               Shortcut shortcut = Shortcut.this;
               if (!shortcut.pressed) {
                  return;
               }
               if (!shortcut.dragging && Math.abs(e.getX() - shortcut.pressX) + Math.abs(e.getY() - shortcut.pressY) >= DRAG_START) {
                  shortcut.dragging = true;
                  DesktopShortcuts.this.ghost.carry(shortcut);
                  shortcut.repaint();
               }
               if (shortcut.dragging) {
                  DesktopShortcuts.this.ghost.follow(shortcut.getX() + e.getX() - shortcut.pressX, shortcut.getY() + e.getY() - shortcut.pressY);
               }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
               Shortcut shortcut = Shortcut.this;
               shortcut.pressed = false;
               if (shortcut.dragging) {
                  shortcut.dragging = false;
                  DesktopShortcuts.this.ghost.drop();
                  // The icon's middle decides the cell it lands in.
                  DesktopShortcuts.this.drop(shortcut, shortcut.getX() + e.getX() - shortcut.pressX + DesktopShortcuts.this.tileWidth / 2,
                     shortcut.getY() + e.getY() - shortcut.pressY + TILE_HEIGHT / 2);
                  shortcut.repaint();
               }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
               if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                  Shortcut.this.open();
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
         };
         this.addMouseListener(mouse);
         this.addMouseMotionListener(mouse);
      }

      abstract void open();

      /** The ICON-sized picture above the label, its top-left corner at (x, y). */
      abstract void paintImage(Graphics g, int x, int y);

      void moveTo(int column, int row) {
         this.column = column;
         this.row = row;
         this.setBounds(MARGIN + column * DesktopShortcuts.this.tileWidth, MARGIN + row * TILE_HEIGHT, DesktopShortcuts.this.tileWidth, TILE_HEIGHT);
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         if (this.dragging) {
            // The ghost carries the icon; its own cell shows a faint outline until it lands.
            g.setColor(new Color(255, 255, 255, 70));
            g.drawRoundRect(2, 2, this.getWidth() - 5, TILE_HEIGHT - 5, 10, 10);
            return;
         }
         boolean isSelected = DesktopShortcuts.this.selected == this;
         if (isSelected || this.hovered) {
            AppIcon.smooth(g);
            g.setColor(isSelected ? new Color(0, 120, 215, 110) : new Color(255, 255, 255, 50));
            g.fillRoundRect(2, 2, this.getWidth() - 4, TILE_HEIGHT - 4, 10, 10);
         }
         this.paintIcon(g);
      }

      void paintIcon(Graphics g) {
         AppIcon.smooth(g);
         int width = this.getWidth();
         this.paintImage(g, (width - ICON) / 2, 8);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
         FontMetrics metrics = g.getFontMetrics();
         List<String> lines = wrap(this.label, metrics, width - 8);
         int y = 8 + ICON + 4 + metrics.getAscent();
         for (String line : lines) {
            int x = (width - metrics.stringWidth(line)) / 2;
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(line, x + 1, y + 1);
            g.setColor(Color.WHITE);
            g.drawString(line, x, y);
            y += metrics.getHeight() - 2;
         }
      }
   }

   /** The icon being dragged, drawn above every window so it shows over whatever it passes. */
   private final class DragGhost extends JComponent {
      private Shortcut carried;

      void carry(Shortcut shortcut) {
         this.carried = shortcut;
         this.setBounds(shortcut.getBounds());
         this.setVisible(true);
         this.repaint();
      }

      void follow(int x, int y) {
         this.setLocation(x, y);
         DesktopShortcuts.this.screen.desktopPane.repaint();
      }

      void drop() {
         this.carried = null;
         this.setVisible(false);
         DesktopShortcuts.this.screen.desktopPane.repaint();
      }

      @Override
      protected void paintComponent(Graphics g) {
         if (this.carried == null) {
            return;
         }
         AppIcon.smooth(g);
         g.setColor(new Color(0, 120, 215, 90));
         g.fillRoundRect(2, 2, this.getWidth() - 4, TILE_HEIGHT - 4, 10, 10);
         this.carried.paintIcon(g);
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
