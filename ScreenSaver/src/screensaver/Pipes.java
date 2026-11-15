package screensaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plumbing: shiny pipes grow across a grid one cell at a time, turning at ball joints, without crossing one another.
 * A pipe that is boxed in or long enough ends and a new one starts in another color; when the screen is full enough
 * it is cleared and the pipes start again.
 */
class Pipes implements Saver {
   /** A pipe grows one cell this often, in milliseconds at normal speed. */
   static final double STEP_MILLIS = 70;
   private static final int ROWS = 16;
   private static final int MAX_LENGTH = 70;
   /** The screen is cleared once this share of the cells holds pipe. */
   private static final double FULL = 0.55;
   private static final int[] DX = {1, 0, -1, 0};
   private static final int[] DY = {0, 1, 0, -1};

   /** A pipe from one cell to the next, or a ball joint where the pipe turned (from == to). */
   private static final class Piece {
      final int fromX;
      final int fromY;
      final int toX;
      final int toY;
      final Color color;

      Piece(int fromX, int fromY, int toX, int toY, Color color) {
         this.fromX = fromX;
         this.fromY = fromY;
         this.toX = toX;
         this.toY = toY;
         this.color = color;
      }
   }

   private final List<Piece> pieces = new ArrayList<>();
   private boolean[][] used;
   private int columns;
   private int rows;
   private double cell;
   private double left;
   private double top;
   private Random random;
   private int headX;
   private int headY;
   private int direction;
   private int length;
   private int usedCount;
   private Color color;
   private double waited;
   /** Play (T): the cell the pointer holds down, which the growing pipe heads for, or null. */
   private int[] lead;

   @Override
   public String name() {
      return "Pipes";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.random = random;
      this.cell = Math.max(12, height / (double)ROWS);
      this.columns = Math.max(2, (int)(width / this.cell));
      this.rows = Math.max(2, (int)(height / this.cell));
      this.left = (width - this.columns * this.cell) / 2;
      this.top = (height - this.rows * this.cell) / 2;
      this.lead = null;
      this.clear();
   }

   private void clear() {
      this.pieces.clear();
      this.used = new boolean[this.columns][this.rows];
      this.usedCount = 0;
      this.waited = 0;
      this.startPipe();
   }

   /** Starts a pipe at a free cell, or clears the screen when none is left. */
   private void startPipe() {
      List<int[]> free = new ArrayList<>();
      for (int x = 0; x < this.columns; x++) {
         for (int y = 0; y < this.rows; y++) {
            if (!this.used[x][y]) {
               free.add(new int[] {x, y});
            }
         }
      }
      if (free.isEmpty()) {
         this.clear();
         return;
      }
      int[] start = free.get(this.random.nextInt(free.size()));
      this.begin(start);
   }

   private void begin(int[] start) {
      this.headX = start[0];
      this.headY = start[1];
      this.direction = this.random.nextInt(4);
      this.length = 0;
      this.color = Color.getHSBColor(this.random.nextFloat(), 0.75F, 0.95F);
      this.occupy(this.headX, this.headY);
      this.pieces.add(new Piece(this.headX, this.headY, this.headX, this.headY, this.color));
   }

   private void occupy(int x, int y) {
      this.used[x][y] = true;
      this.usedCount++;
   }

   private boolean free(int x, int y) {
      return x >= 0 && y >= 0 && x < this.columns && y < this.rows && !this.used[x][y];
   }

   /**
    * A press on a free cell starts a new pipe there (the one growing ends in a ball joint); while held, the pipe
    * grows toward the pointer and waits there.
    */
   @Override
   public void touch(Point at, boolean down) {
      int[] was = this.lead;
      this.lead = null;
      if (at == null || !down) {
         return;
      }
      int x = (int)Math.floor((at.x - this.left) / this.cell);
      int y = (int)Math.floor((at.y - this.top) / this.cell);
      this.lead = new int[] {Math.max(0, Math.min(this.columns - 1, x)), Math.max(0, Math.min(this.rows - 1, y))};
      if (was == null && this.free(this.lead[0], this.lead[1])) {
         this.pieces.add(new Piece(this.headX, this.headY, this.headX, this.headY, this.color));
         this.begin(this.lead);
      }
   }

   int[] head() {
      return new int[] {this.headX, this.headY};
   }

   /** Toward the held cell: the free way that closes the larger gap first, or -1 for none (or when there). */
   private int towardLead() {
      if (this.lead == null) {
         return -1;
      }
      int gx = this.lead[0] - this.headX;
      int gy = this.lead[1] - this.headY;
      int across = gx > 0 ? 0 : 2;
      int down = gy > 0 ? 1 : 3;
      int[] order = Math.abs(gx) >= Math.abs(gy) ? new int[] {across, down} : new int[] {down, across};
      for (int i = 0; i < 2; i++) {
         boolean gap = order[i] % 2 == 0 ? gx != 0 : gy != 0;
         if (gap && this.free(this.headX + DX[order[i]], this.headY + DY[order[i]])) {
            return order[i];
         }
      }
      return -1;
   }

   int pieceCount() {
      return this.pieces.size();
   }

   @Override
   public void tick(double millis) {
      this.waited += millis;
      int steps = 0;
      while (this.waited >= STEP_MILLIS && steps < 200) {
         this.waited -= STEP_MILLIS;
         this.grow();
         steps++;
      }
      if (steps == 200) {
         this.waited = 0;
      }
   }

   /** Grows the current pipe one cell: mostly straight on, sometimes turning, ending when boxed in. */
   private void grow() {
      if (this.usedCount >= FULL * this.columns * this.rows) {
         this.clear();
         return;
      }
      if (this.lead != null && this.lead[0] == this.headX && this.lead[1] == this.headY) {
         // There: the pipe waits under the pointer for it to move on.
         return;
      }
      int next = this.towardLead();
      if (next < 0) {
         next = this.direction;
      }
      // Heading for the pointer takes no random turns.
      if (!this.free(this.headX + DX[next], this.headY + DY[next]) || this.lead == null && this.random.nextInt(5) == 0) {
         // Try a turn to one side, then the other; going back the way it came is never possible.
         int turn = this.random.nextBoolean() ? 1 : 3;
         next = (this.direction + turn) % 4;
         if (!this.free(this.headX + DX[next], this.headY + DY[next])) {
            next = (this.direction + 4 - turn) % 4;
         }
         if (!this.free(this.headX + DX[next], this.headY + DY[next])) {
            next = this.direction;
         }
      }
      int x = this.headX + DX[next];
      int y = this.headY + DY[next];
      if (!this.free(x, y) || this.length >= MAX_LENGTH && this.lead == null) {
         this.pieces.add(new Piece(this.headX, this.headY, this.headX, this.headY, this.color));
         this.startPipe();
         return;
      }
      if (next != this.direction) {
         this.pieces.add(new Piece(this.headX, this.headY, this.headX, this.headY, this.color));
      }
      Piece last = this.pieces.get(this.pieces.size() - 1);
      if (next == this.direction && last.toX == this.headX && last.toY == this.headY && (last.fromX != last.toX || last.fromY != last.toY)) {
         // Straight on: lengthen the last pipe, so a straight run is drawn in one stroke, without seams.
         this.pieces.set(this.pieces.size() - 1, new Piece(last.fromX, last.fromY, x, y, this.color));
      } else {
         this.pieces.add(new Piece(this.headX, this.headY, x, y, this.color));
      }
      this.occupy(x, y);
      this.headX = x;
      this.headY = y;
      this.direction = next;
      this.length++;
   }

   private int px(int x) {
      return (int)(this.left + (x + 0.5) * this.cell);
   }

   private int py(int y) {
      return (int)(this.top + (y + 0.5) * this.cell);
   }

   @Override
   public void paint(Graphics2D g) {
      float thick = (float)(this.cell * 0.5);
      BasicStroke body = new BasicStroke(thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
      BasicStroke shine = new BasicStroke(Math.max(1F, thick * 0.28F), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
      int ball = (int)(this.cell * 0.62);
      int glint = Math.max(2, ball / 4);
      // Shine runs along the upper or left side of each pipe, as if lit from the top left.
      int offset = (int)(thick * 0.18);
      BasicStroke core = new BasicStroke(thick * 0.7F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
      // Pipes first, then the ball joints over their ends; pipes never share a cell, so nothing else overlaps.
      for (Piece p : this.pieces) {
         int x1 = this.px(p.fromX);
         int y1 = this.py(p.fromY);
         int x2 = this.px(p.toX);
         int y2 = this.py(p.toY);
         if (x1 == x2 && y1 == y2) {
            continue;
         }
         g.setStroke(body);
         g.setColor(p.color.darker());
         g.drawLine(x1, y1, x2, y2);
         g.setStroke(core);
         g.setColor(p.color);
         g.drawLine(x1, y1, x2, y2);
         g.setStroke(shine);
         g.setColor(new Color(255, 255, 255, 120));
         if (y1 == y2) {
            g.drawLine(x1, y1 - offset, x2, y2 - offset);
         } else {
            g.drawLine(x1 - offset, y1, x2 - offset, y2);
         }
      }
      for (Piece p : this.pieces) {
         if (p.fromX != p.toX || p.fromY != p.toY) {
            continue;
         }
         int x = this.px(p.fromX);
         int y = this.py(p.fromY);
         g.setColor(p.color.darker());
         g.fillOval(x - ball / 2, y - ball / 2, ball, ball);
         g.setColor(p.color);
         g.fillOval(x - ball / 2 + 2, y - ball / 2 + 2, ball - 4, ball - 4);
         g.setColor(new Color(255, 255, 255, 170));
         g.fillOval(x - ball / 4, y - ball / 4, glint, glint);
      }
      g.setStroke(new BasicStroke(1));
   }
}
