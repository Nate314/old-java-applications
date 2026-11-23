package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * All the savers at once (G): each runs live on a small screen of its own in a grid, like a shop wall of monitors.
 * Each is painted at full screen size, scaled into its own image, so nothing spills into its neighbors.
 */
class Gallery {
   static final Color GREEN = new Color(140, 255, 140);
   static final double ZOOM_MILLIS = 350;

   private final List<Saver> savers;
   int selected;
   /** Opened by the mouse waking the saver rather than by G: the header says so. */
   boolean woke;
   /** Whether the music is on, for its button's label. */
   boolean music;
   /** The buttons along the bottom, so a mouse or a finger can do what the keys do. */
   static final int TURNS = 0, MUSIC = 1, CLOSE = 2;
   private Rectangle[] buttons = new Rectangle[0];
   private Rectangle[] cells = new Rectangle[0];
   private BufferedImage[] images;
   private int laidWidth = -1;
   private int laidHeight = -1;
   private int textSize;

   Gallery(List<Saver> savers, int selected) {
      this.savers = savers;
      this.selected = selected;
   }

   static int textSize(int width, int height) {
      return Math.max(11, Math.min(22, Math.min(width / 40, height / 30)));
   }

   /**
    * Where count screens of this aspect (width / height) go on a width x height window: under a two-line header, in
    * the column count that makes them largest, with room for a name under each, centered (a short last row too).
    */
   static Rectangle[] layout(int width, int height, int count, double aspect) {
      int size = textSize(width, height);
      int margin = Math.max(8, Math.min(width, height) / 40);
      int header = size * 3 + margin;
      int label = size * 2;
      int areaWidth = width - margin * 2;
      int areaHeight = height - header - margin - footer(size, margin);
      int bestCols = 1;
      double best = 0;
      for (int cols = 1; cols <= count; cols++) {
         int rows = (count + cols - 1) / cols;
         double cellWidth = Math.min((areaWidth - margin * (cols - 1.0)) / cols,
            ((areaHeight - margin * (rows - 1.0)) / rows - label) * aspect);
         if (cellWidth > best) {
            best = cellWidth;
            bestCols = cols;
         }
      }
      int cols = bestCols;
      int rows = (count + cols - 1) / cols;
      int cellWidth = Math.max(1, (int)best);
      int cellHeight = Math.max(1, (int)(best / aspect));
      int gridHeight = rows * (cellHeight + label) + (rows - 1) * margin;
      int top = header + Math.max(0, (areaHeight - gridHeight) / 2);
      Rectangle[] cells = new Rectangle[count];
      for (int i = 0; i < count; i++) {
         int row = i / cols;
         int inRow = Math.min(cols, count - row * cols);
         int rowWidth = inRow * cellWidth + (inRow - 1) * margin;
         int left = (width - rowWidth) / 2;
         cells[i] = new Rectangle(left + (i % cols) * (cellWidth + margin), top + row * (cellHeight + label + margin), cellWidth, cellHeight);
      }
      return cells;
   }

   /** The height kept for the row of buttons along the bottom. */
   static int footer(int size, int margin) {
      return size * 2 + margin;
   }

   /** The buttons (TURNS, MUSIC, CLOSE): one centered row along the bottom of a width x height window. */
   static Rectangle[] buttons(int width, int height) {
      int size = textSize(width, height);
      int margin = Math.max(8, Math.min(width, height) / 40);
      int w = Math.min((width - margin * 4) / 3, size * 9);
      int left = (width - w * 3 - margin * 2) / 2;
      Rectangle[] r = new Rectangle[3];
      for (int i = 0; i < 3; i++) {
         r[i] = new Rectangle(left + i * (w + margin), height - margin - size * 2, w, size * 2);
      }
      return r;
   }

   /** The Play badge in the bottom right corner of a screen: a press on it plays with that saver (T) instead of watching. */
   static Rectangle badge(Rectangle c, int size) {
      int h = Math.max(size * 3 / 2, Math.min(c.height / 3, size * 2));
      int w = Math.min(c.width / 2, h * 5 / 2);
      return new Rectangle(c.x + c.width - w - 3, c.y + c.height - h - 3, w, h);
   }

   /** The button at (x, y) (TURNS, MUSIC or CLOSE), or -1. */
   int button(int x, int y) {
      for (int i = 0; i < this.buttons.length; i++) {
         if (this.buttons[i].contains(x, y)) {
            return i;
         }
      }
      return -1;
   }

   /** The saver whose Play badge is at (x, y), or -1. */
   int playAt(int x, int y) {
      for (int i = 0; i < this.cells.length; i++) {
         if (badge(this.cells[i], this.textSize).contains(x, y)) {
            return i;
         }
      }
      return -1;
   }

   /** The columns in this layout: how many cells share the first row. */
   static int columns(Rectangle[] cells) {
      int cols = 0;
      while (cols < cells.length && cells[cols].y == cells[0].y) {
         cols++;
      }
      return cols;
   }

   Rectangle cell(int index) {
      return this.cells.length > index ? this.cells[index] : null;
   }

   /** The saver whose screen or name is at (x, y), or -1. */
   int at(int x, int y) {
      for (int i = 0; i < this.cells.length; i++) {
         Rectangle c = this.cells[i];
         if (x >= c.x && x < c.x + c.width && y >= c.y && y < c.y + c.height + this.textSize * 2) {
            return i;
         }
      }
      return -1;
   }

   /** Arrow keys: dx moves along a row (wrapping through the list), dy a row up or down where there is one. */
   void move(int dx, int dy) {
      int count = this.savers.size();
      if (dx != 0) {
         this.selected = (this.selected + dx + count) % count;
      } else {
         int next = this.selected + dy * Math.max(1, columns(this.cells));
         if (next >= 0 && next < count) {
            this.selected = next;
         }
      }
   }

   void paint(Graphics2D g, int width, int height, int screenWidth, int screenHeight) {
      if (width != this.laidWidth || height != this.laidHeight) {
         this.laidWidth = width;
         this.laidHeight = height;
         this.textSize = textSize(width, height);
         this.cells = layout(width, height, this.savers.size(), screenWidth / (double)screenHeight);
         this.images = new BufferedImage[this.cells.length];
         this.buttons = buttons(width, height);
      }
      int size = this.textSize;
      int margin = Math.max(8, Math.min(width, height) / 40);
      Font bold = new Font(Font.SANS_SERIF, Font.BOLD, size);
      Font plain = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(10, size * 4 / 5));
      g.setFont(bold);
      g.setColor(GREEN);
      String title = "All " + this.savers.size() + " screen savers, live";
      if (this.woke && g.getFontMetrics().stringWidth("You woke it: " + title) <= width - margin * 2) {
         title = "You woke it: " + title;
      }
      g.drawString(title, margin, margin + size);
      g.setFont(plain);
      g.setColor(new Color(200, 200, 200));
      String keys = "Click one to watch it, or its Play to play with it   Arrows, Enter, T: the same by keys   G: back   Esc: close";
      if (g.getFontMetrics().stringWidth(keys) > width - margin * 2) {
         keys = "Click one to watch it, or its Play to play with it";
      }
      if (g.getFontMetrics().stringWidth(keys) > width - margin * 2) {
         keys = "Tap one to watch, its Play to play";
      }
      g.drawString(keys, margin, margin + size * 2 + size / 3);
      for (int i = 0; i < this.cells.length; i++) {
         Rectangle c = this.cells[i];
         if (this.images[i] == null) {
            this.images[i] = new BufferedImage(c.width, c.height, BufferedImage.TYPE_INT_RGB);
         }
         Graphics2D small = this.images[i].createGraphics();
         small.setColor(Color.BLACK);
         small.fillRect(0, 0, c.width, c.height);
         small.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         small.scale(c.width / (double)screenWidth, c.height / (double)screenHeight);
         this.savers.get(i).paint(small);
         small.dispose();
         g.drawImage(this.images[i], c.x, c.y, null);
         boolean on = i == this.selected;
         g.setStroke(new BasicStroke(on ? 3 : 1));
         g.setColor(on ? GREEN : new Color(70, 70, 70));
         g.drawRect(c.x - (on ? 2 : 1), c.y - (on ? 2 : 1), c.width + (on ? 3 : 1), c.height + (on ? 3 : 1));
         g.setFont(on ? bold : plain);
         g.setColor(on ? GREEN : new Color(190, 190, 190));
         String name = (i + 1) % 10 + "  " + this.savers.get(i).name();
         FontMetrics metrics = g.getFontMetrics();
         while (name.length() > 4 && metrics.stringWidth(name) > c.width) {
            name = name.substring(0, name.length() - 2) + ".";
         }
         g.drawString(name, c.x + (c.width - metrics.stringWidth(name)) / 2, c.y + c.height + size * 3 / 2);
         paintButton(g, badge(c, size), "Play", on, plain);
      }
      String[] labels = {"Each in turn", this.music ? "Music: on" : "Music: off", "Close"};
      for (int i = 0; i < this.buttons.length; i++) {
         paintButton(g, this.buttons[i], labels[i], i == MUSIC && this.music, bold);
      }
      g.setStroke(new BasicStroke(1));
   }

   /** A rounded button: lit (green, dark text) or plain (a dark glass with a grey edge), its label centered and fitted. */
   static void paintButton(Graphics2D g, Rectangle r, String label, boolean lit, Font font) {
      g.setColor(lit ? GREEN : new Color(20, 24, 20, 215));
      g.fillRoundRect(r.x, r.y, r.width, r.height, r.height / 2, r.height / 2);
      g.setStroke(new BasicStroke(1));
      g.setColor(lit ? GREEN : new Color(140, 160, 140));
      g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, r.height / 2, r.height / 2);
      int size = font.getSize();
      g.setFont(font);
      while (size > 8 && g.getFontMetrics().stringWidth(label) > r.width - 6) {
         g.setFont(font = font.deriveFont((float)--size));
      }
      FontMetrics m = g.getFontMetrics();
      g.setColor(lit ? Color.BLACK : new Color(225, 235, 225));
      g.drawString(label, r.x + (r.width - m.stringWidth(label)) / 2, r.y + (r.height - m.getHeight()) / 2 + m.getAscent());
   }
}
