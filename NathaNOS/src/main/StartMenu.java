package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * The start menu: the NathaNOS logo, every app in assets/ sorted under Games, Music, Toys and Tools, Restart and Shut
 * Down, and (where the screen has room) a pane beside the list saying what the app under the pointer is. Opens above
 * the MENU button.
 */
public class StartMenu extends JInternalFrame {
   private static final Color BACKGROUND = new Color(26, 35, 54);
   private static final Color PANE = new Color(34, 46, 70);
   private static final Color MUTED = new Color(160, 178, 205);
   private static final int HEADER = 56;
   static final int ROW = 34;
   static final int HEADING = 24;
   private static final int MAX_COLUMN = 220;
   private static final int MIN_COLUMN = 160;
   private static final int PANE_WIDTH = 240;
   private static final int FOOTER = ROW + 9;
   /** The kinds in menu order, as the site's gallery sorts the apps; a jar added by hand goes under Other. */
   static final String[] KINDS = {"Games", "Music", "Toys", "Tools", "Other"};
   private static final Map<String, String> KIND = new HashMap<>();

   static {
      for (String game : new String[] {"DinoGame", "FlappyBird", "Minesweeper", "NumberGuess", "RubiksCube2D", "SpriteFloatyStars", "TankGame", "UltimateTicTacToe"}) {
         KIND.put(game, "Games");
      }
      KIND.put("MusicTheoryExpert", "Music");
      KIND.put("SynthesizerKeyboard", "Music");
      KIND.put("CircleSpawner", "Toys");
      KIND.put(Launcher.PAINT, "Toys");
      KIND.put("ScreenSaver", "Toys");
      for (String tool : new String[] {"BibleDownloader", "Clock", "GroupCreator", "SimpleAlgorithms", "StockChecker"}) {
         KIND.put(tool, "Tools");
      }
   }

   private final Launcher launcher;
   private final Image logo = new ImageIcon("assets/NathaNOS.png").getImage();
   /** The app the pane describes, or -1 for the pane's welcome. */
   private int pointed = -1;
   private JComponent pane;

   public StartMenu(Main screen, Launcher launcher) {
      super("Start Menu", false, false, false);
      this.launcher = launcher;
      this.setVisible(false);
      this.setBorder(null);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);

      List<Integer> order = new ArrayList<>();
      List<String> labels = new ArrayList<>();
      for (String kind : KINDS) {
         boolean headed = false;
         for (int i = 0; i < launcher.count(); i++) {
            if (kind(launcher.name(i)).equals(kind)) {
               if (!headed) {
                  order.add(-1);
                  labels.add(kind);
                  headed = true;
               }
               order.add(i);
               labels.add(launcher.name(i));
            }
         }
      }
      int room = screen.getHeight() - screen.launcherWindow.getHeight() - HEADER - FOOTER - 8;
      boolean[] headings = new boolean[order.size()];
      for (int i = 0; i < headings.length; i++) {
         headings[i] = order.get(i) < 0;
      }
      List<Point> places = flow(headings, room);
      int columns = 1;
      int tallest = 0;
      for (int i = 0; i < places.size(); i++) {
         columns = Math.max(columns, places.get(i).x + 1);
         tallest = Math.max(tallest, places.get(i).y + (headings[i] ? HEADING : ROW));
      }
      // The pane comes only when the columns keep a readable width beside it; long names are cut with "...".
      boolean withPane = screen.getWidth() - 8 - PANE_WIDTH >= columns * MIN_COLUMN;
      int paneWidth = withPane ? PANE_WIDTH : 0;
      int column = Math.min(MAX_COLUMN, (screen.getWidth() - 8 - paneWidth) / columns);
      int width = columns * column + 8 + paneWidth;
      int height = HEADER + 4 + tallest + FOOTER + 4;
      this.setSize(width, height);
      this.setLocation(0, screen.getHeight() - screen.launcherWindow.getHeight() - height);

      JPanel panel = new JPanel();
      panel.setLayout(null);
      panel.setBackground(BACKGROUND);
      this.add(panel);

      JComponent header = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            AppIcon.smooth(g);
            g.setColor(new Color(0, 120, 215));
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            AppIcon.paint(g, StartMenu.this.logo, "NathaNOS", 12, 8, HEADER - 16);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.drawString("NathaNOS", HEADER + 4, HEADER / 2 + 2);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString(launcher.count() + " apps", HEADER + 4, HEADER / 2 + 18);
         }
      };
      header.setBounds(0, 0, width, HEADER);
      panel.add(header);

      for (int i = 0; i < order.size(); i++) {
         int x = 4 + places.get(i).x * column;
         int y = HEADER + 4 + places.get(i).y;
         if (headings[i]) {
            JComponent heading = new Heading(labels.get(i));
            heading.setBounds(x, y, column, HEADING);
            panel.add(heading);
            continue;
         }
         int app = order.get(i);
         FlatButton[] made = new FlatButton[1];
         FlatButton row = new FlatButton(launcher.icon(app), launcher.name(app), AppIcon.words(launcher.name(app)), null, LauncherWindow.HOVER, null, () -> {
            this.hideMenu();
            launcher.launch(app, made[0]);
         });
         made[0] = row;
         row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               StartMenu.this.point(app);
            }
         });
         row.setBounds(x, y, column, ROW);
         panel.add(row);
      }

      if (withPane) {
         this.pane = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
               StartMenu.this.paintPane(g, this.getWidth(), this.getHeight());
            }
         };
         this.pane.setBounds(width - PANE_WIDTH, HEADER, PANE_WIDTH, height - HEADER - FOOTER - 2);
         panel.add(this.pane);
      }

      JComponent divider = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            g.setColor(LauncherWindow.HOVER);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
         }
      };
      divider.setBounds(8, height - FOOTER, width - 16, 1);
      panel.add(divider);
      int half = (width - 8) / 2;
      FlatButton restart = new FlatButton(null, null, "Restart", null, LauncherWindow.HOVER, null, () -> screen.power.begin(true));
      restart.setBounds(4, height - ROW - 4, half, ROW);
      panel.add(restart);
      FlatButton shutDown = new FlatButton(null, null, "Shut Down", null, new Color(160, 40, 50), null, () -> screen.power.begin(false));
      shutDown.setBounds(4 + half, height - ROW - 4, half, ROW);
      panel.add(shutDown);
   }

   /** The kind an app is listed under. */
   static String kind(String app) {
      String kind = KIND.get(app);
      return kind != null ? kind : "Other";
   }

   /**
    * Where each entry goes, top to bottom then into the next column: (column, y within it) for each entry, headings
    * HEADING tall and apps ROW tall, no column taller than room unless one entry alone is. A kind that fits a column
    * starts a new one rather than split, and a heading never ends a column.
    */
   static List<Point> flow(boolean[] headings, int room) {
      List<Point> places = new ArrayList<>();
      int column = 0;
      int y = 0;
      for (int i = 0; i < headings.length; i++) {
         boolean heading = headings[i];
         int need = heading ? HEADING + ROW : ROW;
         if (heading) {
            int whole = HEADING;
            for (int j = i + 1; j < headings.length && !headings[j]; j++) {
               whole += ROW;
            }
            need = whole <= room ? whole : need;
         }
         if (y > 0 && y + need > room) {
            column++;
            y = 0;
         }
         places.add(new Point(column, y));
         y += heading ? HEADING : ROW;
      }
      return places;
   }

   private void point(int app) {
      if (this.pointed != app) {
         this.pointed = app;
         if (this.pane != null) {
            this.pane.repaint();
         }
      }
   }

   /** The pane: the pointed app's icon, name, kind and what it is, or before any, what the menu holds. */
   private void paintPane(Graphics g, int w, int h) {
      AppIcon.smooth(g);
      g.setColor(PANE);
      g.fillRect(0, 0, w, h);
      int pad = 16;
      int icon = 64;
      boolean welcome = this.pointed < 0;
      String name = welcome ? "NathaNOS" : this.launcher.name(this.pointed);
      AppIcon.paint(g, welcome ? this.logo : this.launcher.icon(this.pointed), name, pad, pad, icon);
      int y = pad + icon + 24;
      g.setColor(Color.WHITE);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
      FontMetrics metrics = g.getFontMetrics();
      for (String line : AppIcon.wrap(welcome ? "Pick an app" : AppIcon.words(name), metrics, w - 2 * pad, 2)) {
         g.drawString(line, pad, y);
         y += 22;
      }
      g.setColor(MUTED);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
      g.drawString(welcome ? "WELCOME" : kind(name).toUpperCase(), pad, y);
      y += 24;
      g.setColor(FlatButton.TEXT);
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
      metrics = g.getFontMetrics();
      String about = welcome ? "Point at an app to see what it is, then click it to open it." : IconTip.about(name);
      int lines = Math.max(1, (h - y - 34) / 19);
      for (String line : AppIcon.wrap(about, metrics, w - 2 * pad, lines)) {
         g.drawString(line, pad, y);
         y += 19;
      }
      if (!welcome) {
         g.setColor(MUTED);
         g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
         g.drawString("Click to open", pad, Math.min(y + 12, h - pad));
      }
   }

   public void toggleVisibility() {
      this.point(-1);
      this.setVisible(!this.isVisible());
      if (this.isVisible()) {
         this.moveToFront();
      }
   }

   public void hideMenu() {
      this.setVisible(false);
   }

   /** A kind's name over its apps, in small capitals with a rule after it. */
   private static final class Heading extends JComponent {
      private final String kind;

      Heading(String kind) {
         this.kind = kind;
      }

      @Override
      protected void paintComponent(Graphics g) {
         AppIcon.smooth(g);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
         FontMetrics metrics = g.getFontMetrics();
         String text = this.kind.toUpperCase();
         int baseline = this.getHeight() - 7;
         g.setColor(MUTED);
         g.drawString(text, 10, baseline);
         g.setColor(LauncherWindow.HOVER);
         int x = 10 + metrics.stringWidth(text) + 8;
         g.fillRect(x, baseline - 4, Math.max(0, this.getWidth() - x - 10), 1);
      }
   }
}
