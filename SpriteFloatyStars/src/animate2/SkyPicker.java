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
   /** An upright phone: two bigger tiles a row, four rows, so the skies and their words stay readable. */
   static final int NARROW_COLUMNS = 2;
   static final int NARROW_TILE_WIDTH = 176;
   static final int NARROW_TILE_HEIGHT = 100;
   static final int GAP = 8;
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
   private final int columns;
   private final int tileWidth;
   private final int tileHeight;
   private int hovered = -1;
   private long lastTick = System.nanoTime();

   SkyPicker(JComboBox<String> modes, Runnable open) {
      this(modes, open, false);
   }

   SkyPicker(JComboBox<String> modes, Runnable open, boolean narrow) {
      this.modes = modes;
      this.columns = narrow ? NARROW_COLUMNS : COLUMNS;
      this.tileWidth = narrow ? NARROW_TILE_WIDTH : TILE_WIDTH;
      this.tileHeight = narrow ? NARROW_TILE_HEIGHT : TILE_HEIGHT;
      for (int mode = 0; mode < this.skies.length; mode++) {
         boolean night = mode == StarField.ORBITS || mode == StarField.SKY_WRITING;
         this.skies[mode] = new StarField(mode, new Dimension(this.tileWidth, this.tileHeight), night ? 160 : 60, night ? 6 : 4, false);
         this.skies[mode].setSize(this.tileWidth, this.tileHeight);
         if (this.skies[mode].hintVisible()) {
            this.skies[mode].toggleHint(); // a tile is too small for Orbits' and Sky writing's help lines
         }
         this.frames[mode] = new BufferedImage(this.tileWidth, this.tileHeight, BufferedImage.TYPE_INT_RGB);
         // A few seconds in, so even a still tile (reduced motion) shows the burst spread and the letters formed.
         for (int frame = 0; frame < 150; frame++) {
            this.skies[mode].advance(1 / 50.0, this.tileWidth, this.tileHeight);
            if (frame >= 120) {
               this.render(mode); // the last frames lay down Orbits' trails
            }
         }
      }
      int rows = this.rows();
      // Room under the tiles for the line about a mode: one line beside four columns, two beside two.
      int caption = narrow ? 42 : 26;
      this.setPreferredSize(new Dimension(this.columns * this.tileWidth + (this.columns + 1) * GAP, rows * this.tileHeight + (rows + 1) * GAP + caption));
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
         this.skies[mode].advance(seconds, this.tileWidth, this.tileHeight);
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

   /** The seven tiles and the call to action fill this many rows. */
   private int rows() {
      return (this.skies.length + this.columns) / this.columns;
   }

   /** Left edge of the grid, centred in the panel. */
   private int left() {
      return Math.max(0, (this.getWidth() - (this.columns * this.tileWidth + (this.columns - 1) * GAP)) / 2);
   }

   /** The mode whose tile is under (x, y), or -1 between and outside the tiles. */
   final int tileAt(int x, int y) {
      int column = Math.floorDiv(x - this.left(), this.tileWidth + GAP);
      int row = Math.floorDiv(y - GAP, this.tileHeight + GAP);
      int inX = x - this.left() - column * (this.tileWidth + GAP);
      int inY = y - GAP - row * (this.tileHeight + GAP);
      int tile = row * this.columns + column;
      boolean inside = column >= 0 && column < this.columns && row >= 0 && row < this.rows() && inX < this.tileWidth && inY < this.tileHeight;
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
         int x = this.left() + (mode % this.columns) * (this.tileWidth + GAP);
         int y = GAP + (mode / this.columns) * (this.tileHeight + GAP);
         g.drawImage(this.frames[mode], x, y, null);
         String name = (mode + 1) + " " + StarField.MODE_NAMES[mode];
         FontMetrics metrics = g.getFontMetrics();
         g.setColor(new Color(0, 0, 30, 170));
         g.fillRoundRect(x + 4, y + this.tileHeight - metrics.getHeight() - 6, metrics.stringWidth(name) + 10, metrics.getHeight() + 2, 8, 8);
         g.setColor(Color.white);
         g.drawString(name, x + 9, y + this.tileHeight - 6 - metrics.getDescent());
         if (mode == selected || mode == this.hovered) {
            g.setColor(mode == selected ? GOLD : new Color(255, 255, 255, 200));
            g.drawRect(x - 1, y - 1, this.tileWidth + 1, this.tileHeight + 1);
            g.drawRect(x - 2, y - 2, this.tileWidth + 3, this.tileHeight + 3);
         }
      }
      int about = this.hovered >= 0 ? this.hovered : Math.max(0, selected);
      // The eighth cell holds the call to action, beside the last row's tiles.
      int cell = this.skies.length;
      int cellX = this.left() + (cell % this.columns) * (this.tileWidth + GAP);
      int cellY = GAP + (cell / this.columns) * (this.tileHeight + GAP);
      g.setColor(GOLD);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
      g.drawString("Pick a sky", cellX + 12, cellY + 34);
      g.setColor(new Color(200, 210, 240));
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      g.drawString("Click one to open it", cellX + 12, cellY + 56);
      g.setColor(Color.white);
      int width = this.columns * this.tileWidth + (this.columns - 1) * GAP;
      int lineY = cellY + this.tileHeight + GAP + 14;
      for (String line : StarField.wrap(g.getFontMetrics(), StarField.MODE_NAMES[about] + ": " + ABOUT[about], width)) {
         g.drawString(line, this.left(), lineY);
         lineY += g.getFontMetrics().getHeight();
      }
   }
}
