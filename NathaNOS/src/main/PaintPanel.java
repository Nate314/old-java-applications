package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Paint: a Picture's fat pixels with a column of tools, the 16-color palette and New / Undo / Wallpaper / Delete.
 * The left button paints the first color, the right button the second. Every finished stroke is saved at once, so the
 * picture appears on the desktop (and changes there, and on the wallpaper, as it is drawn) without a Save step.
 */
final class PaintPanel extends JComponent {
   enum Tool {
      PENCIL("Pencil", "draw single pixels"),
      BRUSH("Brush", "paint with a round brush"),
      SPRAY("Spray can", "hold to spray dots"),
      ERASER("Eraser", "rub out to white"),
      LINE("Line", "drag from one end to the other"),
      BOX("Rectangle", "drag from corner to corner"),
      OVAL("Ellipse", "drag across it, corner to corner"),
      FILL("Fill", "click to fill an area of one color");

      final String label;
      final String help;

      Tool(String label, String help) {
         this.label = label;
         this.help = help;
      }
   }

   private static final String[] ACTIONS = {"New", "Undo", "Wallpaper", "Delete"};
   private static final Color BACKGROUND = new Color(26, 35, 54);
   private static final Color BUTTON = new Color(44, 58, 84);
   private static final Color LIT = new Color(0, 120, 215);
   private static final Color HINT = new Color(160, 178, 205);
   private static final int PAD = 8;
   private static final int BAR = 28;
   private static final int TOOL = 30;
   private static final int GAP = 4;
   private static final int TOOLS_WIDTH = 2 * TOOL + GAP;
   private static final int TOOLS_HEIGHT = 4 * TOOL + 3 * GAP;
   private static final int SWATCH = 18;
   private static final int STATUS = 18;
   private static final int UNDO_DEPTH = 40;

   private final Main screen;
   private final Pictures pictures;
   private final int scale;
   private final Rectangle canvas;
   private final Rectangle[] actionBoxes = new Rectangle[ACTIONS.length];
   private final Rectangle[] toolBoxes = new Rectangle[Tool.values().length];
   private final Rectangle[] swatchBoxes = new Rectangle[Picture.PALETTE.length];
   private final Rectangle colorsBox;
   private final Rectangle statusBox;
   private final Deque<byte[]> undo = new ArrayDeque<>();
   private final Random random = new Random();
   private final Timer spray = new Timer(40, e -> this.sprayAt(this.lastX, this.lastY));
   private final Runnable titleChanged;

   private Picture picture;
   private Tool tool = Tool.PENCIL;
   private int primary = Picture.BLACK;
   private int secondary = Picture.WHITE;
   /** The picture as it was when the stroke began: undo's copy, and what shape previews are drawn over. */
   private byte[] before;
   private int strokeColor;
   private int startX;
   private int startY;
   private int lastX;
   private int lastY;
   private int hoverX = -1;
   private int hoverY = -1;
   private Rectangle hovered;
   private String message;
   private boolean deleteArmed;

   PaintPanel(Main screen, Picture picture, Runnable titleChanged) {
      this.screen = screen;
      this.pictures = screen.pictures;
      this.picture = picture;
      this.titleChanged = titleChanged;
      // Fat pixels as big as the desktop allows (up to 4 screen pixels each), leaving room for the window and taskbar.
      int room = screen.getHeight() - screen.launcherWindow.getHeight() - 2 * PAD - BAR - 2 * SWATCH - STATUS - 80;
      this.scale = Math.max(1, Math.min(4, Math.min((screen.getWidth() - TOOLS_WIDTH - 60) / Picture.WIDTH, room / Picture.HEIGHT)));

      int canvasX = PAD + TOOLS_WIDTH + PAD;
      int canvasY = PAD + BAR + PAD;
      this.canvas = new Rectangle(canvasX, canvasY, Picture.WIDTH * this.scale, Picture.HEIGHT * this.scale);
      int width = canvasX + this.canvas.width + PAD;
      int buttonWidth = (width - 2 * PAD - (ACTIONS.length - 1) * 6) / ACTIONS.length;
      for (int i = 0; i < ACTIONS.length; i++) {
         this.actionBoxes[i] = new Rectangle(PAD + i * (buttonWidth + 6), PAD, buttonWidth, BAR);
      }
      for (int i = 0; i < this.toolBoxes.length; i++) {
         this.toolBoxes[i] = new Rectangle(PAD + i % 2 * (TOOL + GAP), canvasY + i / 2 * (TOOL + GAP), TOOL, TOOL);
      }
      int paletteY = canvasY + Math.max(this.canvas.height, TOOLS_HEIGHT) + PAD;
      this.colorsBox = new Rectangle(PAD, paletteY, 2 * SWATCH + 2, 2 * SWATCH + 2);
      for (int i = 0; i < this.swatchBoxes.length; i++) {
         this.swatchBoxes[i] = new Rectangle(PAD + 2 * SWATCH + 10 + i % 8 * (SWATCH + 2), paletteY + i / 8 * (SWATCH + 2), SWATCH, SWATCH);
      }
      this.statusBox = new Rectangle(PAD, paletteY + 2 * SWATCH + 2 + 6, width - 2 * PAD, STATUS);
      this.setPreferredSize(new Dimension(width, this.statusBox.y + STATUS + PAD));
      this.setSize(this.getPreferredSize());
      this.setFocusable(true);
      this.setCursor(screen.getCursor());

      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            PaintPanel.this.requestFocus();
            PaintPanel.this.pressed(e.getX(), e.getY(), e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger() || e.isMetaDown());
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            PaintPanel.this.dragged(e.getX(), e.getY());
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            PaintPanel.this.released();
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            PaintPanel.this.moved(e.getX(), e.getY());
         }

         @Override
         public void mouseExited(MouseEvent e) {
            PaintPanel.this.moved(-1, -1);
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown()) {
               PaintPanel.this.undo();
            } else if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() < KeyEvent.VK_1 + Tool.values().length) {
               PaintPanel.this.choose(Tool.values()[e.getKeyCode() - KeyEvent.VK_1]);
            }
         }
      });
   }

   Picture picture() {
      return this.picture;
   }

   /** "Picture 2", with a note while it has never been saved. */
   String title() {
      return "Paint - " + this.picture.name + (this.pictures.contains(this.picture) ? "" : " (new)");
   }

   // ---- input ----

   private void pressed(int x, int y, boolean right) {
      for (int i = 0; i < ACTIONS.length; i++) {
         if (this.actionBoxes[i].contains(x, y)) {
            this.action(ACTIONS[i]);
            return;
         }
      }
      for (int i = 0; i < this.toolBoxes.length; i++) {
         if (this.toolBoxes[i].contains(x, y)) {
            this.choose(Tool.values()[i]);
            return;
         }
      }
      for (int i = 0; i < this.swatchBoxes.length; i++) {
         if (this.swatchBoxes[i].contains(x, y)) {
            if (right) {
               this.secondary = i;
            } else {
               this.primary = i;
            }
            this.say(null);
            return;
         }
      }
      if (this.colorsBox.contains(x, y)) {
         int swap = this.primary;
         this.primary = this.secondary;
         this.secondary = swap;
         this.say("Swapped the two colors.");
         return;
      }
      if (!this.canvas.contains(x, y)) {
         return;
      }
      this.deleteArmed = false;
      this.message = null;
      int px = this.pixelX(x);
      int py = this.pixelY(y);
      this.before = this.picture.pixels.clone();
      this.strokeColor = this.tool == Tool.ERASER ? Picture.WHITE : right ? this.secondary : this.primary;
      this.startX = this.lastX = px;
      this.startY = this.lastY = py;
      switch (this.tool) {
         case FILL:
            fill(this.picture, px, py, this.strokeColor);
            this.finish();
            return;
         case SPRAY:
            this.sprayAt(px, py);
            this.spray.start();
            return;
         default:
            this.drawTo(px, py);
      }
   }

   private void dragged(int x, int y) {
      this.hoverX = this.pixelX(x);
      this.hoverY = this.pixelY(y);
      if (this.before == null) {
         this.moved(x, y);
         return;
      }
      this.drawTo(this.hoverX, this.hoverY);
   }

   private void released() {
      if (this.before != null) {
         this.finish();
      }
   }

   private void moved(int x, int y) {
      Rectangle over = null;
      for (Rectangle[] boxes : new Rectangle[][] {this.actionBoxes, this.toolBoxes, this.swatchBoxes}) {
         for (Rectangle box : boxes) {
            if (box.contains(x, y)) {
               over = box;
            }
         }
      }
      boolean onCanvas = this.canvas.contains(x, y);
      this.hoverX = onCanvas ? this.pixelX(x) : -1;
      this.hoverY = onCanvas ? this.pixelY(y) : -1;
      this.hovered = over;
      this.repaint();
   }

   private int pixelX(int x) {
      return Math.floorDiv(x - this.canvas.x, this.scale);
   }

   private int pixelY(int y) {
      return Math.floorDiv(y - this.canvas.y, this.scale);
   }

   // ---- tools ----

   private void drawTo(int px, int py) {
      switch (this.tool) {
         case LINE:
         case BOX:
         case OVAL:
            // A shape follows the pointer: drawn afresh over the picture as it was when the drag began.
            System.arraycopy(this.before, 0, this.picture.pixels, 0, this.before.length);
            if (this.tool == Tool.LINE) {
               line(this.picture, this.startX, this.startY, px, py, 0, this.strokeColor);
            } else if (this.tool == Tool.BOX) {
               box(this.picture, this.startX, this.startY, px, py, this.strokeColor);
            } else {
               oval(this.picture, this.startX, this.startY, px, py, this.strokeColor);
            }
            break;
         case SPRAY:
            this.sprayAt(px, py);
            break;
         default:
            int radius = this.tool == Tool.PENCIL ? 0 : this.tool == Tool.BRUSH ? 2 : 3;
            line(this.picture, this.lastX, this.lastY, px, py, radius, this.strokeColor);
      }
      this.lastX = px;
      this.lastY = py;
      this.repaint();
   }

   private void sprayAt(int px, int py) {
      this.lastX = px;
      this.lastY = py;
      for (int i = 0; i < 14; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2;
         double distance = Math.sqrt(this.random.nextDouble()) * 7;
         this.picture.set(px + (int)Math.round(Math.cos(angle) * distance), py + (int)Math.round(Math.sin(angle) * distance), this.strokeColor);
      }
      this.repaint();
   }

   /** Ends a stroke: keeps the old picture for Undo and saves the new one, unless nothing changed. */
   private void finish() {
      this.spray.stop();
      byte[] old = this.before;
      this.before = null;
      if (Arrays.equals(old, this.picture.pixels)) {
         this.repaint();
         return;
      }
      this.undo.push(old);
      while (this.undo.size() > UNDO_DEPTH) {
         this.undo.removeLast();
      }
      this.save();
   }

   private void save() {
      this.picture.changed();
      boolean wasNew = !this.pictures.contains(this.picture);
      if (!this.pictures.put(this.picture)) {
         this.say("The desktop is full (" + Pictures.MAX + " pictures). Delete one to keep this picture.");
      } else if (wasNew) {
         this.say(this.picture.name + " is on the desktop now. Every stroke is saved.");
      }
      this.titleChanged.run();
      this.repaint();
   }

   private void choose(Tool tool) {
      this.tool = tool;
      this.say(null);
   }

   private void action(String action) {
      boolean armed = this.deleteArmed;
      this.deleteArmed = false;
      switch (action) {
         case "New":
            this.open(new Picture(this.pictures.nextName()));
            this.say("A new picture. It is saved to the desktop with its first stroke.");
            break;
         case "Undo":
            this.undo();
            break;
         case "Wallpaper":
            if (this.picture.isBlank()) {
               this.say("Paint something first, then make it the wallpaper.");
            } else if (this.pictures.contains(this.picture) || this.pictures.put(this.picture)) {
               this.screen.setWallpaper(Wallpaper.of(this.picture));
               this.say(this.picture.name + " is the wallpaper now; it changes as you paint.");
               this.titleChanged.run();
            } else {
               this.say("The desktop is full (" + Pictures.MAX + " pictures). Delete one first.");
            }
            break;
         default:
            if (!this.pictures.contains(this.picture)) {
               this.open(new Picture(this.picture.name));
               this.say("Cleared. This picture was never saved.");
            } else if (!armed) {
               this.deleteArmed = true;
               this.say("Press Delete again to delete " + this.picture.name + " from the desktop.");
            } else {
               String name = this.picture.name;
               this.pictures.remove(this.picture);
               this.open(new Picture(this.pictures.nextName()));
               this.say(name + " was deleted.");
            }
      }
      this.repaint();
   }

   private void open(Picture picture) {
      this.picture = picture;
      this.undo.clear();
      this.titleChanged.run();
   }

   private void undo() {
      if (this.undo.isEmpty()) {
         this.say("Nothing to undo.");
         return;
      }
      byte[] old = this.undo.pop();
      System.arraycopy(old, 0, this.picture.pixels, 0, old.length);
      if (this.pictures.contains(this.picture)) {
         this.save();
      } else {
         this.picture.changed();
      }
      this.say("Undone. " + (this.undo.isEmpty() ? "That was the first stroke." : this.undo.size() + " more to undo."));
   }

   private void say(String message) {
      this.message = message;
      this.repaint();
   }

   // ---- drawing on the picture ----

   /** A line of round dots of the radius (0 is single pixels), Bresenham style. */
   static void line(Picture picture, int x0, int y0, int x1, int y1, int radius, int color) {
      int dx = Math.abs(x1 - x0);
      int dy = -Math.abs(y1 - y0);
      int sx = x0 < x1 ? 1 : -1;
      int sy = y0 < y1 ? 1 : -1;
      int error = dx + dy;
      while (true) {
         dot(picture, x0, y0, radius, color);
         if (x0 == x1 && y0 == y1) {
            return;
         }
         int twice = 2 * error;
         if (twice >= dy) {
            error += dy;
            x0 += sx;
         }
         if (twice <= dx) {
            error += dx;
            y0 += sy;
         }
      }
   }

   private static void dot(Picture picture, int x, int y, int radius, int color) {
      for (int dy = -radius; dy <= radius; dy++) {
         for (int dx = -radius; dx <= radius; dx++) {
            if (dx * dx + dy * dy <= radius * radius + radius) {
               picture.set(x + dx, y + dy, color);
            }
         }
      }
   }

   static void box(Picture picture, int x0, int y0, int x1, int y1, int color) {
      line(picture, x0, y0, x1, y0, 0, color);
      line(picture, x1, y0, x1, y1, 0, color);
      line(picture, x1, y1, x0, y1, 0, color);
      line(picture, x0, y1, x0, y0, 0, color);
   }

   /** The ellipse that fits the box with corners (x0, y0) and (x1, y1), as joined short lines. */
   static void oval(Picture picture, int x0, int y0, int x1, int y1, int color) {
      double cx = (x0 + x1) / 2.0;
      double cy = (y0 + y1) / 2.0;
      double rx = Math.abs(x1 - x0) / 2.0;
      double ry = Math.abs(y1 - y0) / 2.0;
      int steps = Math.max(12, (int)((rx + ry) * 3));
      int px = (int)Math.round(cx + rx);
      int py = (int)Math.round(cy);
      for (int i = 1; i <= steps; i++) {
         double angle = i * Math.PI * 2 / steps;
         int x = (int)Math.round(cx + rx * Math.cos(angle));
         int y = (int)Math.round(cy + ry * Math.sin(angle));
         line(picture, px, py, x, y, 0, color);
         px = x;
         py = y;
      }
   }

   /** Fills the area of one color around (x, y), up to its edges (diagonal gaps do not leak). */
   static void fill(Picture picture, int x, int y, int color) {
      if (x < 0 || y < 0 || x >= Picture.WIDTH || y >= Picture.HEIGHT) {
         return;
      }
      int target = picture.get(x, y);
      if (target == color) {
         return;
      }
      int[] stack = new int[Picture.WIDTH * Picture.HEIGHT];
      int size = 0;
      stack[size++] = y * Picture.WIDTH + x;
      picture.pixels[y * Picture.WIDTH + x] = (byte)color;
      while (size > 0) {
         int at = stack[--size];
         int ax = at % Picture.WIDTH;
         int ay = at / Picture.WIDTH;
         int[][] next = {{ax - 1, ay}, {ax + 1, ay}, {ax, ay - 1}, {ax, ay + 1}};
         for (int[] n : next) {
            if (n[0] >= 0 && n[1] >= 0 && n[0] < Picture.WIDTH && n[1] < Picture.HEIGHT && picture.get(n[0], n[1]) == target) {
               picture.pixels[n[1] * Picture.WIDTH + n[0]] = (byte)color;
               stack[size++] = n[1] * Picture.WIDTH + n[0];
            }
         }
      }
   }

   // ---- painting ----

   @Override
   protected void paintComponent(Graphics g) {
      AppIcon.smooth(g);
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
      FontMetrics metrics = g.getFontMetrics();
      for (int i = 0; i < ACTIONS.length; i++) {
         Rectangle box = this.actionBoxes[i];
         boolean warn = ACTIONS[i].equals("Delete") && this.deleteArmed;
         g.setColor(warn ? new Color(160, 40, 50) : this.hovered == box ? LauncherWindow.ON : BUTTON);
         g.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
         g.setColor(FlatButton.TEXT);
         String label = AppIcon.fit(ACTIONS[i], metrics, box.width - 6);
         g.drawString(label, box.x + (box.width - metrics.stringWidth(label)) / 2, box.y + (box.height + metrics.getAscent() - metrics.getDescent()) / 2);
      }
      for (int i = 0; i < this.toolBoxes.length; i++) {
         Rectangle box = this.toolBoxes[i];
         Tool each = Tool.values()[i];
         g.setColor(each == this.tool ? LIT : this.hovered == box ? LauncherWindow.ON : BUTTON);
         g.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
         paintToolIcon(g, each, box.x, box.y, Picture.PALETTE[this.primary]);
      }

      // The picture, with a frame and the pixel under the pointer outlined.
      g.setColor(Color.BLACK);
      g.fillRect(this.canvas.x - 2, this.canvas.y - 2, this.canvas.width + 4, this.canvas.height + 4);
      this.picture.paint(g, this.canvas.x, this.canvas.y, this.scale, this.scale);
      if (this.hoverX >= 0 && this.hoverY >= 0 && this.hoverX < Picture.WIDTH && this.hoverY < Picture.HEIGHT && this.scale >= 2) {
         int radius = this.tool == Tool.BRUSH ? 2 : this.tool == Tool.ERASER ? 3 : 0;
         int under = this.picture.get(this.hoverX, this.hoverY);
         g.setColor(under == Picture.BLACK || under == 14 || under == 2 ? Color.WHITE : Color.BLACK);
         g.drawRect(this.canvas.x + (this.hoverX - radius) * this.scale, this.canvas.y + (this.hoverY - radius) * this.scale,
            (2 * radius + 1) * this.scale - 1, (2 * radius + 1) * this.scale - 1);
      }

      // The two colors (click to swap) and the palette.
      Rectangle c = this.colorsBox;
      g.setColor(Picture.PALETTE[this.secondary]);
      g.fillRect(c.x + SWATCH / 2 + 2, c.y + SWATCH / 2 + 2, SWATCH + 2, SWATCH + 2);
      g.setColor(Color.WHITE);
      g.drawRect(c.x + SWATCH / 2 + 2, c.y + SWATCH / 2 + 2, SWATCH + 1, SWATCH + 1);
      g.setColor(Picture.PALETTE[this.primary]);
      g.fillRect(c.x, c.y, SWATCH + 2, SWATCH + 2);
      g.setColor(Color.WHITE);
      g.drawRect(c.x, c.y, SWATCH + 1, SWATCH + 1);
      for (int i = 0; i < this.swatchBoxes.length; i++) {
         Rectangle box = this.swatchBoxes[i];
         g.setColor(Picture.PALETTE[i]);
         g.fillRect(box.x, box.y, box.width, box.height);
         if (i == this.primary || this.hovered == box) {
            g.setColor(Color.WHITE);
            g.drawRect(box.x - 1, box.y - 1, box.width + 1, box.height + 1);
         }
      }

      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      metrics = g.getFontMetrics();
      g.setColor(HINT);
      String status = this.message != null ? this.message
         : Arrays.asList(this.swatchBoxes).contains(this.hovered) ? "Click: the left button's color. Right-click: the right button's."
         : this.tool.label + ": " + this.tool.help + ".";
      g.drawString(AppIcon.fit(status, metrics, this.statusBox.width), this.statusBox.x, this.statusBox.y + metrics.getAscent());
   }

   /** A small drawing of the tool; the first color shows where the pencil, brush, spray and fill put paint. */
   private static void paintToolIcon(Graphics g, Tool tool, int x, int y, Color ink) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.translate(x, y);
      g2.setStroke(new BasicStroke(2f));
      Color light = new Color(235, 241, 250);
      switch (tool) {
         case PENCIL:
            g2.setColor(new Color(245, 190, 60));
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(20, 9, 11, 18);
            g2.setColor(ink);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(10, 19, 8, 22);
            g2.setColor(new Color(240, 140, 160));
            g2.setStroke(new BasicStroke(5f));
            g2.drawLine(22, 7, 21, 8);
            break;
         case BRUSH:
            g2.setColor(new Color(185, 122, 87));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(22, 6, 15, 14);
            g2.setColor(light);
            g2.fillOval(11, 13, 6, 6);
            g2.setColor(ink);
            g2.fillOval(6, 17, 8, 7);
            break;
         case SPRAY:
            g2.setColor(light);
            g2.fillRoundRect(15, 11, 8, 13, 3, 3);
            g2.fillRect(17, 8, 4, 3);
            g2.setColor(ink);
            int[][] dots = {{6, 9}, {9, 6}, {11, 10}, {7, 13}, {12, 7}, {5, 6}};
            for (int[] dot : dots) {
               g2.fillRect(dot[0], dot[1], 2, 2);
            }
            break;
         case ERASER:
            g2.setColor(new Color(240, 140, 160));
            g2.fillPolygon(new int[] {6, 16, 24, 14}, new int[] {17, 7, 13, 23}, 4);
            g2.setColor(light);
            g2.fillPolygon(new int[] {6, 11, 19, 14}, new int[] {17, 12, 18, 23}, 4);
            break;
         case LINE:
            g2.setColor(light);
            g2.drawLine(7, 22, 23, 8);
            break;
         case BOX:
            g2.setColor(light);
            g2.drawRect(7, 9, 16, 12);
            break;
         case OVAL:
            g2.setColor(light);
            g2.drawOval(6, 9, 18, 12);
            break;
         default:
            // A tipped paint bucket pouring the first color.
            g2.setColor(light);
            g2.fillPolygon(new int[] {8, 17, 22, 13}, new int[] {12, 6, 14, 20}, 4);
            g2.setColor(ink);
            g2.fillPolygon(new int[] {17, 22, 24, 22}, new int[] {6, 14, 20, 21}, 4);
            g2.fillOval(20, 19, 5, 5);
            break;
      }
      g2.dispose();
   }
}
