package circlewindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

/**
 * A grid of cells to draw your own shape on. Pressing an empty cell starts drawing, pressing a lit one starts erasing,
 * and dragging carries on the same way. With mirror on, every cell is matched on the other side of the middle.
 * A drawing can have several frames, a flipbook the windows play; the frame before the one shown is drawn faintly
 * under it (onion skin) so the next one can be drawn to match.
 */
final class DrawPad extends JPanel {
   static final int CELLS = 20;
   static final int MAX_FRAMES = 12;
   private static final int CELL = 8;
   private static final Color PAPER = new Color(250, 250, 246);
   private static final Color LINE = new Color(222, 222, 214);
   private static final Color MIDDLE = new Color(190, 200, 230);
   private static final Color INK = new Color(40, 44, 60);
   private static final Color ONION = new Color(214, 218, 236);

   private final List<boolean[][]> frames = new ArrayList<>();
   /** The frame shown, the one drawing changes. */
   private boolean[][] cells = new boolean[CELLS][CELLS];
   private int frame;
   private final Runnable onChange;
   private boolean mirror = true;
   private boolean drawing;
   /** Where the pointer was at the last press or drag, so a fast drag leaves no gaps. */
   private int lastX;
   private int lastY;

   DrawPad(Runnable onChange) {
      this.onChange = onChange;
      this.frames.add(this.cells);
      this.setPreferredSize(new Dimension(CELLS * CELL + 1, CELLS * CELL + 1));
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            int column = e.getX() / CELL;
            int row = e.getY() / CELL;
            if (onPad(column, row)) {
               DrawPad.this.drawing = !DrawPad.this.cells[row][column];
               DrawPad.this.stroke(e.getX(), e.getY(), e.getX(), e.getY());
            }
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            DrawPad.this.stroke(DrawPad.this.lastX, DrawPad.this.lastY, e.getX(), e.getY());
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
   }

   private static boolean onPad(int column, int row) {
      return column >= 0 && row >= 0 && column < CELLS && row < CELLS;
   }

   /**
    * Sets every cell on the line between two pointer positions (and their mirrors) to the current stroke, and
    * tells the window once if anything changed.
    */
   private void stroke(int fromX, int fromY, int toX, int toY) {
      this.lastX = toX;
      this.lastY = toY;
      int steps = Math.max(1, Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY)) * 2 / CELL);
      boolean changed = false;
      for (int i = 0; i <= steps; i++) {
         int column = Math.floorDiv(fromX + (toX - fromX) * i / steps, CELL);
         int row = Math.floorDiv(fromY + (toY - fromY) * i / steps, CELL);
         if (onPad(column, row)) {
            changed |= this.set(column, row);
            if (this.mirror) {
               changed |= this.set(CELLS - 1 - column, row);
            }
         }
      }
      if (changed) {
         this.repaint();
         this.onChange.run();
      }
   }

   private boolean set(int column, int row) {
      if (this.cells[row][column] == this.drawing) {
         return false;
      }
      this.cells[row][column] = this.drawing;
      return true;
   }

   boolean[][] cells() {
      boolean[][] copy = new boolean[CELLS][];
      for (int row = 0; row < CELLS; row++) {
         copy[row] = this.cells[row].clone();
      }
      return copy;
   }

   int frame() {
      return this.frame;
   }

   int frameCount() {
      return this.frames.size();
   }

   /** Shows frame index (wrapping round), without telling the window. */
   void goTo(int index) {
      this.frame = Math.floorMod(index, this.frames.size());
      this.cells = this.frames.get(this.frame);
      this.repaint();
   }

   /** Adds a copy of the frame shown just after it and shows the copy; false when the flipbook is full. */
   boolean addFrame() {
      if (this.frames.size() >= MAX_FRAMES) {
         return false;
      }
      this.frames.add(this.frame + 1, this.cells());
      this.goTo(this.frame + 1);
      return true;
   }

   /** Removes the frame shown and shows the one before it; the last frame is never removed. */
   boolean deleteFrame() {
      if (this.frames.size() < 2) {
         return false;
      }
      this.frames.remove(this.frame);
      this.goTo(this.frame - 1);
      return true;
   }

   boolean mirror() {
      return this.mirror;
   }

   void setMirror(boolean mirror) {
      this.mirror = mirror;
      this.repaint();
   }

   void clear() {
      for (boolean[] row : this.cells) {
         Arrays.fill(row, false);
      }
      this.repaint();
      this.onChange.run();
   }

   /** Each frame as rows of '#' (lit) and '.' joined by '/', the frames joined by '|', as saved. */
   String text() {
      StringBuilder out = new StringBuilder();
      for (boolean[][] grid : this.frames) {
         if (out.length() > 0) {
            out.append('|');
         }
         for (int row = 0; row < CELLS; row++) {
            if (row > 0) {
               out.append('/');
            }
            for (int column = 0; column < CELLS; column++) {
               out.append(grid[row][column] ? '#' : '.');
            }
         }
      }
      return out.toString();
   }

   /** Reads a drawing saved by text() (one frame in older saves), showing its first frame; anything missing stays empty. */
   void setText(String text) {
      this.frames.clear();
      for (String part : text.split("\\|", -1)) {
         if (this.frames.size() == MAX_FRAMES) {
            break;
         }
         String[] rows = part.split("/");
         boolean[][] grid = new boolean[CELLS][CELLS];
         for (int row = 0; row < CELLS; row++) {
            for (int column = 0; column < CELLS; column++) {
               grid[row][column] = row < rows.length && column < rows[row].length() && rows[row].charAt(column) == '#';
            }
         }
         this.frames.add(grid);
      }
      this.goTo(0);
   }

   /** The first drawing: a smiling face, and a second frame where it winks, so Play has something to show. */
   void smile() {
      this.frames.clear();
      this.frames.add(face(false));
      this.frames.add(face(true));
      this.goTo(0);
   }

   private static boolean[][] face(boolean wink) {
      boolean[][] grid = new boolean[CELLS][CELLS];
      double middle = (CELLS - 1) / 2.0;
      for (int row = 0; row < CELLS; row++) {
         for (int column = 0; column < CELLS; column++) {
            double u = column - middle;
            double v = row - middle;
            boolean face = Math.hypot(u, v) <= middle + 0.3;
            boolean eye = Math.abs(Math.abs(u) - 3.5) <= 1.0 && v >= -5.5 && v <= -2.5;
            if (wink && u > 0) {
               // The right eye shut: one dash across where it was.
               eye = Math.abs(u - 3.5) <= 1.0 && v == -3.5;
            }
            double r = Math.hypot(u, v - 0.5);
            boolean mouth = v >= 2.5 && r >= 4.6 && r <= 6.4 && Math.abs(u) <= 5.5;
            grid[row][column] = face && !eye && !mouth;
         }
      }
      return grid;
   }

   @Override
   protected void paintComponent(Graphics g) {
      int size = CELLS * CELL;
      g.setColor(PAPER);
      g.fillRect(0, 0, size, size);
      g.setColor(LINE);
      for (int i = 0; i <= CELLS; i++) {
         g.drawLine(i * CELL, 0, i * CELL, size);
         g.drawLine(0, i * CELL, size, i * CELL);
      }
      if (this.frames.size() > 1) {
         boolean[][] before = this.frames.get(Math.floorMod(this.frame - 1, this.frames.size()));
         g.setColor(ONION);
         for (int row = 0; row < CELLS; row++) {
            for (int column = 0; column < CELLS; column++) {
               if (before[row][column]) {
                  g.fillRect(column * CELL, row * CELL, CELL, CELL);
               }
            }
         }
      }
      g.setColor(INK);
      for (int row = 0; row < CELLS; row++) {
         for (int column = 0; column < CELLS; column++) {
            if (this.cells[row][column]) {
               g.fillRect(column * CELL, row * CELL, CELL, CELL);
            }
         }
      }
      if (this.mirror) {
         g.setColor(MIDDLE);
         g.drawLine(size / 2, 0, size / 2, size);
      }
   }
}
