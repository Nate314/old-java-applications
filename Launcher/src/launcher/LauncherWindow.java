package launcher;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The arcade launcher: every app as a card with its picture, filtered by category or by typing, with a detail pane
 * that plays the selected app in its own process. Keys: type to search, arrows to move, Enter to play, Esc to clear.
 */
public final class LauncherWindow extends JFrame {
   private static final int TILE_MIN_W = 170;
   private static final int TILE_H = 206;
   private static final int GAP = 14;
   private static final int PAD = 22;
   private static final String[][] FILTERS = {
      {"all", "All"}, {"game", "Games"}, {"music", "Music"}, {"toy", "Toys"}, {"tool", "Tools"}, {"desktop", "Desktop only"}};

   private final List<Catalog.App> apps;
   private final PlayLog log;
   private final Runner runner;
   private final Path root;
   private final LongSupplier clock;
   private final Thumbs thumbs = new Thumbs();
   private final Random random = new Random();
   private final Set<String> running = new HashSet<>();
   private final List<Tile> tiles = new ArrayList<>();
   private final Map<String, Ui.FlatButton> chips = new LinkedHashMap<>();

   private final Ui.SearchField search;
   private final Grid grid = new Grid();
   private final JScrollPane scroll;
   private final Detail detail = new Detail();
   private final JLabel status = new JLabel();

   private String filter = "all";
   private Catalog.App selected;

   public LauncherWindow(List<Catalog.App> apps, PlayLog log, Runner runner, Path root, LongSupplier clock) {
      super("Old Java Applications");
      this.apps = apps;
      this.log = log;
      this.runner = runner;
      this.root = root;
      this.clock = clock;
      search = new Ui.SearchField("Search " + apps.size() + " apps");

      for (Catalog.App app : apps) {
         Tile t = new Tile(app);
         tiles.add(t);
         grid.add(t);
      }
      scroll = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setBorder(null);
      scroll.getViewport().setBackground(Ui.DESK);
      Ui.ThinScrollBarUI.apply(scroll.getVerticalScrollBar());

      JPanel content = new JPanel(new BorderLayout());
      content.setBackground(Ui.DESK);
      content.add(header(), BorderLayout.NORTH);
      content.add(scroll, BorderLayout.CENTER);
      content.add(detail, BorderLayout.EAST);
      content.add(statusBar(), BorderLayout.SOUTH);
      setContentPane(content);
      wireKeys();

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      setMinimumSize(new Dimension(780, 620));
      Rectangle screen = getGraphicsConfiguration().getBounds();
      setSize(Math.min(1100, screen.width - 40), Math.min(740, screen.height - 60));
      setLocationRelativeTo(null);

      Catalog.App last = null;
      for (Catalog.App a : apps) if (log.count(a.id) > 0 && (last == null || log.last(a.id) > log.last(last.id))) last = a;
      applyFilter();
      select(last != null ? last : apps.get(0), true);
      status(last != null ? "Welcome back. " + last.name + " is where you left off: press Enter to play it again."
         : "Pick an app and press Enter, or let the launcher pick one for you.");
   }

   // ---- building -------------------------------------------------------------------------------------------

   private JComponent header() {
      JLabel title = new JLabel("Old Java Applications");
      title.setFont(Ui.font(Font.BOLD, 26f));
      title.setForeground(Ui.TEXT);
      JLabel sub = new JLabel(apps.size() + " Swing apps recovered from old jars and Eclipse projects. "
         + "Type to search, arrows move, Enter plays.");
      sub.setFont(Ui.font(Font.PLAIN, 13.5f));
      sub.setForeground(Ui.MUTED);
      JPanel words = new JPanel();
      words.setOpaque(false);
      words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
      words.add(title);
      words.add(Box.createVerticalStrut(2));
      words.add(sub);

      JPanel top = new JPanel(new BorderLayout(16, 0));
      top.setOpaque(false);
      top.add(words, BorderLayout.CENTER);
      JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
      searchBox.setOpaque(false);
      searchBox.add(search);
      top.add(searchBox, BorderLayout.EAST);

      JPanel row = new JPanel(new BorderLayout());
      row.setOpaque(false);
      JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      chipRow.setOpaque(false);
      for (String[] f : FILTERS) {
         Ui.FlatButton chip = new Ui.FlatButton(f[1] + "  " + count(f[0]), false);
         chip.addActionListener(e -> {
            filter = f[0];
            applyFilter();
         });
         chips.put(f[0], chip);
         chipRow.add(chip);
         chipRow.add(Box.createHorizontalStrut(8));
      }
      row.add(chipRow, BorderLayout.CENTER);
      Ui.FlatButton pick = new Ui.FlatButton("Pick for me", false);
      pick.setMnemonic(KeyEvent.VK_K);
      pick.setToolTipText("Plays an app you have not launched yet (Alt+K)");
      pick.addActionListener(e -> pickForMe());
      row.add(pick, BorderLayout.EAST);

      JPanel header = new JPanel(new BorderLayout(0, 14));
      header.setBackground(Ui.DESK);
      header.setBorder(BorderFactory.createEmptyBorder(18, PAD, 12, PAD));
      header.add(top, BorderLayout.NORTH);
      header.add(row, BorderLayout.SOUTH);
      return header;
   }

   private JComponent statusBar() {
      JPanel bar = new JPanel(new BorderLayout());
      bar.setBackground(Ui.PANEL);
      bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.LINE),
         BorderFactory.createEmptyBorder(7, PAD, 7, PAD)));
      status.setFont(Ui.font(Font.PLAIN, 12.5f));
      status.setForeground(Ui.MUTED);
      bar.add(status, BorderLayout.CENTER);
      return bar;
   }

   private void wireKeys() {
      search.getDocument().addDocumentListener(new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent e) {
            applyFilter();
         }

         @Override
         public void removeUpdate(DocumentEvent e) {
            applyFilter();
         }

         @Override
         public void changedUpdate(DocumentEvent e) {
            applyFilter();
         }
      });
      search.addActionListener(e -> play(selected));
      // Up and Down always move the selection; Left, Right, Home and End do too while the field is empty
      // (otherwise they move the caret).
      search.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            boolean empty = search.getText().isEmpty();
            int cols = grid.columns();
            switch (e.getKeyCode()) {
               case KeyEvent.VK_UP: move(-cols); break;
               case KeyEvent.VK_DOWN: move(cols); break;
               case KeyEvent.VK_LEFT: if (!empty) return; move(-1); break;
               case KeyEvent.VK_RIGHT: if (!empty) return; move(1); break;
               case KeyEvent.VK_HOME: if (!empty) return; move(-1000); break;
               case KeyEvent.VK_END: if (!empty) return; move(1000); break;
               case KeyEvent.VK_ESCAPE: if (empty) return; search.setText(""); break;
               default: return;
            }
            e.consume();
         }
      });
      // Enter plays from anywhere in the window, the search field included (its action above).
      getRootPane().registerKeyboardAction(e -> play(selected), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
         JComponent.WHEN_IN_FOCUSED_WINDOW);
   }

   private int count(String f) {
      int n = 0;
      for (Catalog.App a : apps) if (inFilter(a, f)) n++;
      return n;
   }

   private static boolean inFilter(Catalog.App a, String f) {
      return f.equals("all") || (f.equals("desktop") ? a.desktopOnly : a.category.equals(f));
   }

   // ---- state ---------------------------------------------------------------------------------------------

   /** Shows the tiles of the current filter that match the search, and keeps a visible app selected. */
   void applyFilter() {
      for (Map.Entry<String, Ui.FlatButton> c : chips.entrySet()) c.getValue().setSelected(c.getKey().equals(filter));
      String q = search.getText();
      for (Tile t : tiles) t.setVisible(inFilter(t.app, filter) && t.app.matches(q));
      grid.revalidate();
      grid.repaint();
      List<Catalog.App> shown = visible();
      if (shown.isEmpty()) select(null, false);
      else if (selected == null || !shown.contains(selected)) select(shown.get(0), true);
   }

   List<Catalog.App> visible() {
      List<Catalog.App> out = new ArrayList<>();
      for (Tile t : tiles) if (t.isVisible()) out.add(t.app);
      return out;
   }

   Catalog.App selected() {
      return selected;
   }

   String statusText() {
      return status.getText();
   }

   void select(Catalog.App app, boolean reveal) {
      selected = app;
      detail.show(app);
      for (Tile t : tiles) t.repaint();
      if (app != null && reveal) {
         SwingUtilities.invokeLater(() -> {
            for (Tile t : tiles) {
               if (t.app == app) {
                  Rectangle r = t.getBounds();
                  r.grow(0, GAP);
                  grid.scrollRectToVisible(r);
               }
            }
         });
      }
   }

   private void move(int delta) {
      List<Catalog.App> shown = visible();
      if (shown.isEmpty()) return;
      int i = Math.max(0, shown.indexOf(selected));
      int j = Math.max(0, Math.min(shown.size() - 1, i + delta));
      select(shown.get(j), true);
   }

   private void status(String text) {
      status.setText(text);
   }

   /**
    * Plays an app not launched yet, of the apps shown if any qualify, else of all. Desktop-only apps are never
    * picked: they drive the real desktop, or (Bible Downloader) fail.
    */
   void pickForMe() {
      List<Catalog.App> from = candidates(visible());
      if (from.isEmpty()) from = candidates(apps);
      if (from.isEmpty()) return;
      Catalog.App app = from.get(random.nextInt(from.size()));
      if (!visible().contains(app)) {
         filter = "all";
         search.setText("");
         applyFilter();
      }
      select(app, true);
      play(app);
   }

   /** The pickable apps among these: the ones never launched, or if none, all that are not running. */
   private List<Catalog.App> candidates(List<Catalog.App> among) {
      List<Catalog.App> fresh = new ArrayList<>();
      List<Catalog.App> any = new ArrayList<>();
      for (Catalog.App a : among) {
         if (a.desktopOnly || running.contains(a.id)) continue;
         any.add(a);
         if (log.count(a.id) == 0) fresh.add(a);
      }
      return fresh.isEmpty() ? any : fresh;
   }

   /** Starts the app in its own process; the apps that press real keys ask first. */
   void play(Catalog.App app) {
      if (app == null || running.contains(app.id)) return;
      if (app.warning != null) {
         int answer = JOptionPane.showConfirmDialog(this, app.warning + "\n\nStart " + app.name + " now?",
            app.name + " drives your real desktop", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
         if (answer != JOptionPane.OK_OPTION) return;
      }
      CompletableFuture<Integer> done;
      try {
         done = runner.start(app.id);
      } catch (IOException e) {
         status("Could not start " + app.name + ": " + e.getMessage());
         return;
      }
      log.launched(app.id, clock.getAsLong());
      running.add(app.id);
      status("Starting " + app.name + ": run.ps1 compiles it, then its window opens. You can start others meanwhile.");
      refresh();
      done.whenComplete((code, err) -> SwingUtilities.invokeLater(() -> {
         running.remove(app.id);
         if (err != null) status(app.name + " could not be watched: " + err.getMessage());
         else if (code == 0) status(app.name + " closed. " + log.describe(app.id, clock.getAsLong(), ZoneId.systemDefault()) + ".");
         else status(app.name + " stopped with exit code " + code + ". The console that started the launcher says why.");
         refresh();
      }));
   }

   private void refresh() {
      for (Tile t : tiles) t.repaint();
      detail.show(selected);
   }

   boolean isRunning(String id) {
      return running.contains(id);
   }

   // ---- the grid ------------------------------------------------------------------------------------------

   /** Lays the visible tiles out in as many columns as fit, stretched to fill the width. */
   private final class Grid extends JPanel implements Scrollable {
      Grid() {
         super(null);
         setBackground(Ui.DESK);
      }

      int columns() {
         int w = getWidth() > 0 ? getWidth() : 800;
         return Math.max(1, (w - 2 * PAD + GAP) / (TILE_MIN_W + GAP));
      }

      @Override
      public Dimension getPreferredSize() {
         int w = getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 800;
         int n = visible().size();
         int cols = Math.max(1, (w - 2 * PAD + GAP) / (TILE_MIN_W + GAP));
         int rows = Math.max(1, (n + cols - 1) / cols);
         return new Dimension(w, 2 * PAD - 8 + rows * TILE_H + (rows - 1) * GAP);
      }

      @Override
      public void doLayout() {
         int cols = columns();
         int tw = (getWidth() - 2 * PAD - (cols - 1) * GAP) / cols;
         int i = 0;
         for (Tile t : tiles) {
            if (!t.isVisible()) continue;
            t.setBounds(PAD + (i % cols) * (tw + GAP), PAD - 8 + (i / cols) * (TILE_H + GAP), tw, TILE_H);
            i++;
         }
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         if (!visible().isEmpty()) return;
         Graphics2D g2 = Ui.smooth(g);
         g2.setFont(Ui.font(Font.PLAIN, 15f));
         g2.setColor(Ui.MUTED);
         String text = "No app matches \"" + search.getText() + "\"" + (filter.equals("all") ? "." : " here. Try All.");
         g2.drawString(text, PAD, PAD + 30);
         g2.dispose();
      }

      @Override
      public Dimension getPreferredScrollableViewportSize() {
         return getPreferredSize();
      }

      @Override
      public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
         return 32;
      }

      @Override
      public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
         return r.height - 40;
      }

      @Override
      public boolean getScrollableTracksViewportWidth() {
         return true;
      }

      @Override
      public boolean getScrollableTracksViewportHeight() {
         return false;
      }
   }

   /** One app's card: picture, name, category and how often it was played. Click selects, double-click plays. */
   private final class Tile extends JComponent {
      final Catalog.App app;
      private boolean hover;

      Tile(Catalog.App app) {
         this.app = app;
         setToolTipText(app.hint);
         addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
               select(app, false);
               search.requestFocusInWindow();
               if (e.getClickCount() == 2) play(app);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
               hover = true;
               repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               hover = false;
               repaint();
            }
         });
      }

      @Override
      protected void paintComponent(Graphics g) {
         Graphics2D g2 = Ui.smooth(g);
         int w = getWidth();
         int h = getHeight();
         boolean sel = app == selected;
         g2.setColor(hover || sel ? Ui.TILE_HOVER : Ui.TILE);
         g2.fillRoundRect(0, 0, w, h, 16, 16);
         // picture well
         int ph = 124;
         g2.setColor(new Color(0x171a21));
         g2.fillRoundRect(6, 6, w - 12, ph, 12, 12);
         thumbs.draw(g2, app, 14, 12, w - 28, ph - 12);
         // badge on the picture
         if (isRunning(app.id)) badge(g2, w, "RUNNING", Ui.GO);
         else if (log.count(app.id) == 0) badge(g2, w, "NEW", Ui.ACCENT);
         // words
         g2.setFont(Ui.font(Font.BOLD, 15f));
         g2.setColor(Ui.TEXT);
         g2.drawString(fit(g2, app.name, w - 24), 12, ph + 30);
         int x = 12 + Ui.chip(g2, 11, ph + 40, Ui.categoryName(app.category), Ui.category(app.category));
         if (app.desktopOnly) Ui.chip(g2, x + 6, ph + 40, "Desktop only", Ui.MUTED);
         g2.setFont(Ui.font(Font.PLAIN, 12f));
         g2.setColor(isRunning(app.id) ? Ui.GO : Ui.MUTED);
         String line = isRunning(app.id) ? "Running now" : log.describe(app.id, clock.getAsLong(), ZoneId.systemDefault());
         g2.drawString(fit(g2, line, w - 24), 12, h - 12);
         if (sel) {
            g2.setColor(Ui.ACCENT);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
         }
         g2.dispose();
      }

      private void badge(Graphics2D g2, int w, String text, Color c) {
         g2.setFont(Ui.font(Font.BOLD, 10.5f));
         FontMetrics fm = g2.getFontMetrics();
         int bw = fm.stringWidth(text) + 14;
         g2.setColor(c);
         g2.fillRoundRect(w - bw - 12, 12, bw, 18, 18, 18);
         g2.setColor(Color.WHITE);
         g2.drawString(text, w - bw - 5, 12 + (18 + fm.getAscent() - fm.getDescent()) / 2);
      }
   }

   private static String fit(Graphics2D g, String s, int w) {
      FontMetrics fm = g.getFontMetrics();
      if (fm.stringWidth(s) <= w) return s;
      while (s.length() > 1 && fm.stringWidth(s + "...") > w) s = s.substring(0, s.length() - 1);
      return s.trim() + "...";
   }

   // ---- the detail pane -----------------------------------------------------------------------------------

   /** The selected app: a big picture, its name, origin, hint, any warning, its record and Play. */
   private final class Detail extends JPanel {
      private static final int W = 340;
      private Catalog.App app;
      private final JComponent picture = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            if (app == null) return;
            Graphics2D g2 = Ui.smooth(g);
            g2.setColor(new Color(0x12141a));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            thumbs.draw(g2, app, 12, 12, getWidth() - 24, getHeight() - 24);
            g2.dispose();
         }
      };
      private final JLabel name = new JLabel();
      private final JComponent tags = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            if (app == null) return;
            Graphics2D g2 = Ui.smooth(g);
            int x = Ui.chip(g2, 0, 0, Ui.categoryName(app.category), Ui.category(app.category)) + 6;
            x += Ui.chip(g2, x, 0, app.original ? "Original source" : "Decompiled jar", new Color(0x8fb4ff)) + 6;
            if (app.desktopOnly) Ui.chip(g2, x, 0, "Desktop only", Ui.MUTED);
            g2.dispose();
         }
      };
      private final JTextArea hint = wrapping();
      private final JTextArea warning = wrapping();
      private final JLabel record = new JLabel();
      private final Ui.FlatButton play = new Ui.FlatButton("Play", true);
      private final Ui.FlatButton readme = new Ui.FlatButton("Read me", false);
      private final Ui.FlatButton folder = new Ui.FlatButton("Open folder", false);

      Detail() {
         setBackground(Ui.PANEL);
         setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Ui.LINE),
            BorderFactory.createEmptyBorder(PAD, 20, PAD, 20)));
         setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
         setPreferredSize(new Dimension(W, 400));
         int inner = W - 41;
         fixed(picture, inner, 200);
         picture.setMinimumSize(new Dimension(inner, 0));   // the picture gives way first on a short window
         name.setFont(Ui.font(Font.BOLD, 22f));
         name.setForeground(Ui.TEXT);
         fixed(tags, inner, 20);
         hint.setFont(Ui.font(Font.PLAIN, 14f));
         hint.setForeground(new Color(0xc9cdd8));
         warning.setFont(Ui.font(Font.PLAIN, 13f));
         warning.setForeground(Ui.WARN);
         warning.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x6b4c1e)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
         warning.setOpaque(true);
         warning.setBackground(new Color(0x2a2116));
         record.setFont(Ui.font(Font.PLAIN, 12.5f));
         record.setForeground(Ui.MUTED);
         play.setMnemonic(KeyEvent.VK_P);
         play.setMaximumSize(new Dimension(inner, 46));
         play.setPreferredSize(new Dimension(inner, 46));
         play.addActionListener(e -> play(app));
         readme.setMnemonic(KeyEvent.VK_R);
         readme.addActionListener(e -> ReadmeView.show(LauncherWindow.this, app.name, root.resolve(app.id).resolve("README.md")));
         folder.setMnemonic(KeyEvent.VK_F);
         folder.addActionListener(e -> openFolder(app));
         JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
         buttons.setOpaque(false);
         buttons.add(readme);
         buttons.add(Box.createHorizontalStrut(8));
         buttons.add(folder);
         buttons.setMaximumSize(new Dimension(inner, 40));

         for (JComponent c : new JComponent[] {picture, name, tags, hint, warning, record, play, buttons}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
         }
         add(picture);
         add(Box.createVerticalStrut(16));
         add(name);
         add(Box.createVerticalStrut(8));
         add(tags);
         add(Box.createVerticalStrut(12));
         add(hint);
         add(Box.createVerticalStrut(12));
         add(warning);
         add(Box.createVerticalStrut(10));
         add(Box.createVerticalGlue());
         add(record);
         add(Box.createVerticalStrut(10));
         add(play);
         add(Box.createVerticalStrut(10));
         add(buttons);
      }

      private void fixed(JComponent c, int w, int h) {
         Dimension d = new Dimension(w, h);
         c.setPreferredSize(d);
         c.setMaximumSize(d);
         c.setMinimumSize(d);
      }

      void show(Catalog.App a) {
         app = a;
         boolean some = a != null;
         for (Component c : getComponents()) c.setVisible(some);
         if (!some) return;
         name.setText(a.name);
         hint.setText(a.hint);
         warning.setVisible(a.warning != null);
         if (a.warning != null) warning.setText("Heads up: " + a.warning);
         boolean on = running.contains(a.id);
         record.setText(on ? "Running now. Close its window to finish." : log.describe(a.id, clock.getAsLong(), ZoneId.systemDefault()));
         play.setText(on ? "Running..." : "Play " + a.name);
         play.setEnabled(!on);
         play.setFill(a.warning != null ? new Color(0xc9822a) : Ui.ACCENT);
         revalidate();
         repaint();
      }

      /** Read-only text that wraps at the pane's inner width and is as tall as its lines. */
      private JTextArea wrapping() {
         int inner = W - 41;
         JTextArea t = new JTextArea() {
            @Override
            public Dimension getPreferredSize() {
               setSize(inner, Short.MAX_VALUE);
               return new Dimension(inner, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMaximumSize() {
               return getPreferredSize();
            }

            @Override
            public Dimension getMinimumSize() {
               return getPreferredSize();
            }
         };
         t.setEditable(false);
         t.setFocusable(false);
         t.setOpaque(false);
         t.setLineWrap(true);
         t.setWrapStyleWord(true);
         return t;
      }
   }

   private void openFolder(Catalog.App app) {
      try {
         Desktop.getDesktop().open(root.resolve(app.id).toFile());
      } catch (IOException | UnsupportedOperationException | IllegalArgumentException e) {
         status("Could not open the folder: " + e.getMessage());
      }
   }
}
