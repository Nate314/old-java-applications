package animate2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The settings window's seven skies: each mode plays live in a small tile, so the visitor sees what a mode does
 * before opening it. Pointing at a tile names it below; a click picks that mode in the Mode list and opens it.
 * With reduced motion asked for, the tiles hold still.
 */
@SuppressWarnings("serial")
final class SkyPicker extends JPanel {
   static final int COLUMNS = 4;
   static final int TILE_WIDTH = 132;
   static final int TILE_HEIGHT = 92;
   static final int GAP = 8;
   private static final int CAPTION = 26;
   private static final Color BACKGROUND = new Color(10, 14, 40);
   private static final Color GOLD = new Color(255, 214, 90);
   static final String[] ABOUT = {
      "White stars drift left and down, wrapping round the edges.",
      "Stars burst from the middle and bounce, colored by where they are.",
      "The same burst, each star colored by the way it is going.",
      "Every star its own color, bouncing off the edges.",
      "Stars that meet share their speed, so they drift together.",
      "Click to place up to four suns; the stars fall round them.",
      "Type a message and the stars swoop in to spell it.",
   };
   private final StarField[] skies = new StarField[StarField.MODE_NAMES.length];
   private final BufferedImage[] frames = new BufferedImage[this.skies.length];
   private final JComboBox<String> modes;
   private int hovered = -1;
   private long lastTick = System.nanoTime();

   SkyPicker(JComboBox<String> modes, Runnable open) {
      this.modes = modes;
      for (int mode = 0; mode < this.skies.length; mode++) {
         boolean night = mode == StarField.ORBITS || mode == StarField.SKY_WRITING;
         this.skies[mode] = new StarField(mode, new Dimension(TILE_WIDTH, TILE_HEIGHT), night ? 160 : 60, night ? 6 : 4, false);
         this.skies[mode].setSize(TILE_WIDTH, TILE_HEIGHT);
         if (this.skies[mode].hintVisible()) {
            this.skies[mode].toggleHint(); // a tile is too small for Orbits' and Sky writing's help lines
         }
         this.frames[mode] = new BufferedImage(TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_INT_RGB);
         // A few seconds in, so even a still tile (reduced motion) shows the burst spread and the letters formed.
         for (int frame = 0; frame < 150; frame++) {
            this.skies[mode].advance(1 / 50.0, TILE_WIDTH, TILE_HEIGHT);
            if (frame >= 120) {
               this.render(mode); // the last frames lay down Orbits' trails
            }
         }
      }
      this.setPreferredSize(new Dimension(COLUMNS * TILE_WIDTH + (COLUMNS + 1) * GAP, 2 * TILE_HEIGHT + 3 * GAP + CAPTION));
      modes.addActionListener(event -> this.repaint());
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            int tile = SkyPicker.this.tileAt(e.getX(), e.getY());
            if (tile != SkyPicker.this.hovered) {
               SkyPicker.this.hovered = tile;
               SkyPicker.this.repaint();
            }
         }

         @Override
         public void mouseExited(MouseEvent e) {
            SkyPicker.this.hovered = -1;
            SkyPicker.this.repaint();
         }

         @Override
         public void mouseClicked(MouseEvent e) {
            int tile = SkyPicker.this.tileAt(e.getX(), e.getY());
            if (tile >= 0) {
               modes.setSelectedIndex(tile);
               open.run();
            }
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      new Timer(30, event -> this.tick()).start();
   }

   private void tick() {
      long now = System.nanoTime();
      double seconds = Math.min(0.1, (now - this.lastTick) / 1.0E9);
      this.lastTick = now;
      // Hidden behind Star Catcher, or holding still for reduced motion: nothing to move.
      if (!this.isShowing() || Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"))) {
         return;
      }
      for (int mode = 0; mode < this.skies.length; mode++) {
         this.skies[mode].advance(seconds, TILE_WIDTH, TILE_HEIGHT);
         this.render(mode);
      }
      this.repaint();
   }

   private void render(int mode) {
      Graphics2D g = this.frames[mode].createGraphics();
      this.skies[mode].paint(g);
      g.dispose();
   }

   final BufferedImage frame(int mode) {
      return this.frames[mode];
   }

   /** Left edge of the grid, centred in the panel. */
   private int left() {
      return Math.max(0, (this.getWidth() - (COLUMNS * TILE_WIDTH + (COLUMNS - 1) * GAP)) / 2);
   }

   /** The mode whose tile is under (x, y), or -1 between and outside the tiles. */
   final int tileAt(int x, int y) {
      int column = Math.floorDiv(x - this.left(), TILE_WIDTH + GAP);
      int row = Math.floorDiv(y - GAP, TILE_HEIGHT + GAP);
      int inX = x - this.left() - column * (TILE_WIDTH + GAP);
      int inY = y - GAP - row * (TILE_HEIGHT + GAP);
      int tile = row * COLUMNS + column;
      boolean inside = column >= 0 && column < COLUMNS && row >= 0 && row < 2 && inX < TILE_WIDTH && inY < TILE_HEIGHT;
      return inside && tile < this.skies.length ? tile : -1;
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      int selected = this.modes.getSelectedIndex();
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
      for (int mode = 0; mode < this.skies.length; mode++) {
         int x = this.left() + (mode % COLUMNS) * (TILE_WIDTH + GAP);
         int y = GAP + (mode / COLUMNS) * (TILE_HEIGHT + GAP);
         g.drawImage(this.frames[mode], x, y, null);
         String name = (mode + 1) + " " + StarField.MODE_NAMES[mode];
         FontMetrics metrics = g.getFontMetrics();
         g.setColor(new Color(0, 0, 30, 170));
         g.fillRoundRect(x + 4, y + TILE_HEIGHT - metrics.getHeight() - 6, metrics.stringWidth(name) + 10, metrics.getHeight() + 2, 8, 8);
         g.setColor(Color.white);
         g.drawString(name, x + 9, y + TILE_HEIGHT - 6 - metrics.getDescent());
         if (mode == selected || mode == this.hovered) {
            g.setColor(mode == selected ? GOLD : new Color(255, 255, 255, 200));
            g.drawRect(x - 1, y - 1, TILE_WIDTH + 1, TILE_HEIGHT + 1);
            g.drawRect(x - 2, y - 2, TILE_WIDTH + 3, TILE_HEIGHT + 3);
         }
      }
      int about = this.hovered >= 0 ? this.hovered : Math.max(0, selected);
      // The eighth cell holds the call to action, beside the last row's tiles.
      int cellX = this.left() + 3 * (TILE_WIDTH + GAP);
      int cellY = GAP + TILE_HEIGHT + GAP;
      g.setColor(GOLD);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
      g.drawString("Pick a sky", cellX + 12, cellY + 34);
      g.setColor(new Color(200, 210, 240));
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      g.drawString("Click one to open it", cellX + 12, cellY + 56);
      g.setColor(Color.white);
      g.drawString(StarField.MODE_NAMES[about] + ": " + ABOUT[about], this.left(), cellY + TILE_HEIGHT + GAP + 14);
   }
}
