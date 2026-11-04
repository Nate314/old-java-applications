package circlewindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.JPanel;

/**
 * A grid of cells to draw your own shape on. Pressing an empty cell starts drawing, pressing a lit one starts erasing,
 * and dragging carries on the same way. With mirror on, every cell is matched on the other side of the middle.
 */
final class DrawPad extends JPanel {
   static final int CELLS = 20;
   private static final int CELL = 8;
   private static final Color PAPER = new Color(250, 250, 246);
   private static final Color LINE = new Color(222, 222, 214);
   private static final Color MIDDLE = new Color(190, 200, 230);
   private static final Color INK = new Color(40, 44, 60);

   private final boolean[][] cells = new boolean[CELLS][CELLS];
   private final Runnable onChange;
   private boolean mirror = true;
   private boolean drawing;
   /** Where the pointer was at the last press or drag, so a fast drag leaves no gaps. */
   private int lastX;
   private int lastY;

   DrawPad(Runnable onChange) {
      this.onChange = onChange;
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

   /** Rows of '#' (lit) and '.' joined by '/', as saved. */
   String text() {
      StringBuilder out = new StringBuilder();
      for (int row = 0; row < CELLS; row++) {
         if (row > 0) {
            out.append('/');
         }
         for (int column = 0; column < CELLS; column++) {
            out.append(this.cells[row][column] ? '#' : '.');
         }
      }
      return out.toString();
   }

   /** Reads a drawing saved by text(); anything missing stays empty. */
   void setText(String text) {
      String[] rows = text.split("/");
      for (int row = 0; row < CELLS; row++) {
         for (int column = 0; column < CELLS; column++) {
            this.cells[row][column] = row < rows.length && column < rows[row].length() && rows[row].charAt(column) == '#';
         }
      }
      this.repaint();
   }

   /** The first drawing: a smiling face. */
   void smile() {
      double middle = (CELLS - 1) / 2.0;
      for (int row = 0; row < CELLS; row++) {
         for (int column = 0; column < CELLS; column++) {
            double u = column - middle;
            double v = row - middle;
            boolean face = Math.hypot(u, v) <= middle + 0.3;
            boolean eye = Math.abs(Math.abs(u) - 3.5) <= 1.0 && v >= -5.5 && v <= -2.5;
            double r = Math.hypot(u, v - 0.5);
            boolean mouth = v >= 2.5 && r >= 4.6 && r <= 6.4 && Math.abs(u) <= 5.5;
            this.cells[row][column] = face && !eye && !mouth;
         }
      }
      this.repaint();
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
