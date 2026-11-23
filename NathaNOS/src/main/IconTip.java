package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * The card a click on a desktop icon opens beside it: the icon, its name, a line on what the app is and an Open
 * button, so a first-time visitor (or a finger that taps once) knows what each icon is and how to start it.
 */
final class IconTip extends JInternalFrame {
   private static final Color BACKGROUND = new Color(26, 35, 54);
   private static final Color MUTED = new Color(160, 178, 205);
   private static final Color ACCENT = new Color(0, 120, 215);
   private static final int MAX_WIDTH = 250;
   private static final int PAD = 12;
   private static final int ICON = 36;
   private static final int LINE = 16;
   private static final int LINES = 3;
   private static final int BUTTON = 34;
   private static final int GAP = 6;
   private static final String PICTURE = "A picture you painted in Paint. Open it to paint on.";
   /** What each app on the desktop is, in a line (from each app's own description on the site). */
   private static final Map<String, String> ABOUT = new HashMap<>();

   static {
      ABOUT.put("BibleDownloader", "A Bible reader: an open book in five translations, with daily reading plans.");
      ABOUT.put("CircleSpawner", "Circles, hearts, stars and words made of windows that morph and fling.");
      ABOUT.put("Clock", "An analog, digital or flip clock, with an alarm, a timer, a stopwatch and a world clock.");
      ABOUT.put("DinoGame", "A dinosaur runner, with a coach that shows the jump or duck that would have cleared it.");
      ABOUT.put("FlappyBird", "Flap through the pipes, then rewind a crash and fly on from there.");
      ABOUT.put("GroupCreator", "Deals a class into random groups, with partners they have not had yet.");
      ABOUT.put("Minesweeper", "Minesweeper where every board can be solved without a guess.");
      ABOUT.put("MusicTheoryExpert", "Ear training drills, a songwriter on the circle of fifths and a chord finder.");
      ABOUT.put("NumberGuess", "Find the hidden number, duel the computer, or let it read your mind.");
      ABOUT.put(Launcher.PAINT, "Paint pixel pictures. Each one lands on the desktop and can be the wallpaper.");
      ABOUT.put("RubiksCube2D", "Learn to solve a Rubik's cube, even your own, one turn at a time.");
      ABOUT.put("ScreenSaver", "Ten screen savers, from the original green circles to an aquarium and a maze.");
      ABOUT.put("SimpleAlgorithms", "Race sorting algorithms, watch searches find a way through a maze, and more.");
      ABOUT.put("SpriteFloatyStars", "A sky of stars to play with: orbits, sky writing and Star Catcher.");
      ABOUT.put("StockChecker", "A practice trading desk: $10,000 of play money against four computer traders.");
      ABOUT.put("SynthesizerKeyboard", "A synthesizer keyboard that teaches songs as the next key lights up.");
      ABOUT.put("TankGame", "A tank duel against the computer or a friend: first to three rounds wins.");
      ABOUT.put("UltimateTicTacToe", "Tic-tac-toe on nine boards: the cell you play picks the next board.");
   }

   private final Main screen;
   private final JComponent body;
   private Image icon;
   private String iconName;
   private String title;
   private String about;
   private Runnable open;

   IconTip(Main screen) {
      super("About", false, false, false);
      this.screen = screen;
      this.setVisible(false);
      this.setBorder(null);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);
      int width = Math.min(MAX_WIDTH, screen.getWidth() - 16);
      int top = PAD + ICON + GAP + LINES * LINE + GAP;
      this.setSize(width, top + BUTTON + PAD);

      JPanel panel = new JPanel();
      panel.setLayout(null);
      panel.setBackground(BACKGROUND);
      // A click on the card itself must not fall through to the desktop, which would close it.
      panel.addMouseListener(new MouseAdapter() {
      });
      this.add(panel);
      this.body = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            IconTip.this.paintBody(g, this.getWidth());
         }
      };
      this.body.setBounds(0, 0, width, top);
      panel.add(this.body);
      FlatButton button = new FlatButton(null, null, "Open", ACCENT, new Color(0, 140, 240), null, () -> {
         Runnable action = this.open;
         this.hideTip();
         if (action != null) {
            action.run();
         }
      });
      button.setBounds(PAD - 2, top, 96, BUTTON);
      panel.add(button);
      JComponent hint = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            AppIcon.smooth(g);
            g.setColor(MUTED);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(AppIcon.fit("or double-click it", metrics, this.getWidth()), 0, (this.getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
         }
      };
      hint.setBounds(PAD + 104, top, width - PAD - 104 - PAD, BUTTON);
      panel.add(hint);
   }

   /** What the app is, in a line; a jar added by hand gets a line naming its file. */
   static String about(String app) {
      String about = ABOUT.get(app);
      return about != null ? about : app + ".jar, an app from the assets folder.";
   }

   /** Opens beside the icon (desktop bounds) for an app; open starts it. */
   void showApp(Rectangle at, Image icon, String app, Runnable open) {
      this.show(at, icon, app, AppIcon.words(app), about(app), open);
   }

   /** Opens beside the icon of a picture from Paint; open opens it in Paint. */
   void showPicture(Rectangle at, String name, Runnable open) {
      this.show(at, PaintWindow.icon(), Launcher.PAINT, name, PICTURE, open);
   }

   private void show(Rectangle at, Image icon, String iconName, String title, String about, Runnable open) {
      this.icon = icon;
      this.iconName = iconName;
      this.title = title;
      this.about = about;
      this.open = open;
      this.setLocation(place(at, this.getWidth(), this.getHeight(), this.screen.getWidth(), this.screen.getHeight() - this.screen.launcherWindow.getHeight()));
      this.setVisible(true);
      this.moveToFront();
      this.repaint();
   }

   void hideTip() {
      this.open = null;
      this.setVisible(false);
   }

   /**
    * Where a w x h card goes for an icon at (desktop bounds): to its right, else its left, top edges level; failing
    * both (a screen narrower than icon and card), under it, else over it. Always kept on the screen above bottom.
    */
   static Point place(Rectangle at, int w, int h, int screenWidth, int bottom) {
      int x;
      int y = at.y;
      if (at.x + at.width + 4 + w <= screenWidth) {
         x = at.x + at.width + 4;
      } else if (at.x - 4 - w >= 0) {
         x = at.x - 4 - w;
      } else {
         x = at.x + (at.width - w) / 2;
         y = at.y + at.height + h <= bottom ? at.y + at.height : at.y - h;
      }
      x = Math.max(0, Math.min(x, screenWidth - w));
      y = Math.max(0, Math.min(y, bottom - h));
      return new Point(x, y);
   }

   private void paintBody(Graphics g, int width) {
      if (this.title == null) {
         return;
      }
      AppIcon.smooth(g);
      AppIcon.paint(g, this.icon, this.iconName, PAD, PAD, ICON);
      int x = PAD + ICON + 10;
      g.setColor(Color.WHITE);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(AppIcon.fit(this.title, metrics, width - x - PAD), x, PAD + (ICON + metrics.getAscent() - metrics.getDescent()) / 2);
      g.setColor(FlatButton.TEXT);
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
      metrics = g.getFontMetrics();
      List<String> lines = AppIcon.wrap(this.about, metrics, width - 2 * PAD, LINES);
      int y = PAD + ICON + GAP + metrics.getAscent();
      for (String line : lines) {
         g.drawString(line, PAD, y);
         y += LINE;
      }
   }
}
