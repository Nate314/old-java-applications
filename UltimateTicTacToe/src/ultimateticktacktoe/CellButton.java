package ultimateticktacktoe;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * One cell, painted on a dark board: its mark drawn in with a pen stroke, and its slice of anything drawn across its
 * whole board or the whole grid (the outline of a board you may play on, the line through a won board and the big
 * mark over it, the gold outline of a winning board). Each cell draws those shapes in board or grid coordinates and
 * keeps the part that falls on it, so the nine cells of a board show one shape between them.
 */
public final class CellButton extends JButton implements ActionListener {
   static final Color BACKGROUND = new Color(20, 26, 40);
   static final Color X_COLOR = new Color(77, 171, 247);
   static final Color O_COLOR = new Color(255, 107, 107);
   static final Color GOLD = new Color(255, 201, 71);
   private static final Color CELL = new Color(33, 41, 61);
   private static final Color CELL_OPEN = new Color(46, 58, 86);
   private static final Color CELL_LAST = new Color(66, 80, 114);
   private static final Color X_WON = new Color(26, 50, 82);
   private static final Color O_WON = new Color(76, 34, 46);
   private static final Color DRAWN = new Color(40, 42, 48);
   /** How long a mark takes to draw in, and the line through a won board, then the big mark over it. */
   static final int MARK_MS = 240;
   static final int LINE_MS = 220;
   static final int BIG_MARK_MS = 380;
   /** The gold outlines and fading of a finished game. */
   static final int GAME_OVER_MS = 500;

   /** Where a board sits in the big grid, or a square in its board, read out by screen readers. */
   static final String[] PLACES = {"top left", "top", "top right", "left", "middle", "right", "bottom left", "bottom", "bottom right"};

   private final GameFrame game;
   private final int boardIndex;
   private final int cellIndex;

   public CellButton(GameFrame game, int boardIndex, int cellIndex) {
      this.game = game;
      this.cellIndex = cellIndex;
      this.boardIndex = boardIndex;
      this.setText("");
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.addActionListener(this);
      // The mouse (or the keyboard cursor, below) previews a move: its ghost mark, and the board it sends the opponent to.
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent event) {
            CellButton.this.game.preview(CellButton.this.move(), false);
         }

         @Override
         public void mouseExited(MouseEvent event) {
            CellButton.this.game.endPreview(CellButton.this.move());
         }
      });
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            CellButton.this.keyPressed(event);
         }
      });
   }

   /** The cell as board * 9 + cell, the way GameState numbers moves. */
   int move() {
      return this.boardIndex * 9 + this.cellIndex;
   }

   /** The arrow keys move across the whole 9 x 9 grid; Enter plays, like Space. */
   private void keyPressed(KeyEvent event) {
      if (this.game.reviewKey(event.getKeyCode())) {
         event.consume();   // R, and the review's keys while one is open
         return;
      }
      int row = this.boardIndex / 3 * 3 + this.cellIndex / 3;
      int column = this.boardIndex % 3 * 3 + this.cellIndex % 3;
      switch (event.getKeyCode()) {
         case KeyEvent.VK_LEFT: column--; break;
         case KeyEvent.VK_RIGHT: column++; break;
         case KeyEvent.VK_UP: row--; break;
         case KeyEvent.VK_DOWN: row++; break;
         case KeyEvent.VK_ENTER:
            this.doClick();
            event.consume();
            return;
         case KeyEvent.VK_W:
            this.game.toggleWatch();
            event.consume();
            return;
         default:
            return;
      }
      event.consume();
      if (row >= 0 && row < 9 && column >= 0 && column < 9) {
         this.game.focusCell(row / 3 * 3 + column / 3, row % 3 * 3 + column % 3);
      }
   }

   /**
    * Names the cell for screen readers: its mark, whether the next move may go there, and where it is ("empty, open,
    * top left board, middle square"; "O, last move, ..."), and redraws it.
    */
   final void show(String mark, boolean open, boolean last) {
      this.getAccessibleContext().setAccessibleName((mark.isEmpty() ? "empty" + (open ? ", open" : "") : mark + (last ? ", last move" : ""))
         + ", " + PLACES[this.boardIndex] + " board, " + PLACES[this.cellIndex] + " square");
      this.repaint();
   }

   @Override
   public final void actionPerformed(ActionEvent e) {
      this.game.cellClicked(this.boardIndex, this.cellIndex);
   }

   // ---- drawing ----

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      GameState state = this.game.state;
      long now = System.currentTimeMillis();
      int w = this.getWidth();
      int h = this.getHeight();
      // This cell's corner in its board, and the board's size: board-wide shapes are drawn from (-bx, -by).
      int bx = this.getX();
      int by = this.getY();
      int bw = w * 3;   // the cells' part of the board (any leftover pixel at its edge is background)
      int bh = h * 3;
      String result = state.boardResults[this.boardIndex];
      String mark = state.marks[this.boardIndex][this.cellIndex];
      boolean playable = this.game.playable(this.boardIndex);
      boolean last = state.lastMove == this.move();

      g.setColor(BACKGROUND);
      g.fillRect(0, 0, w, h);
      int pad = Math.max(2, w / 16);
      int arc = Math.max(4, w / 4);
      long wonAt = this.game.boardWonAt[this.boardIndex];
      double wonT = wonAt == 0 ? 1 : progress(now, wonAt + LINE_MS, BIG_MARK_MS);   // the big mark follows the line
      Color tile = "X".equals(result) ? mix(CELL, X_WON, wonT)
         : "O".equals(result) ? mix(CELL, O_WON, wonT)
         : GameState.DRAW.equals(result) ? DRAWN
         : playable ? CELL_OPEN
         : CELL;
      g.setColor(last ? CELL_LAST : tile);
      g.fillRoundRect(pad / 2, pad / 2, w - pad, h - pad, arc, arc);

      // The mark, drawn in; faded under the big mark of a won board.
      if (!mark.isEmpty()) {
         int alpha = result == null || GameState.DRAW.equals(result) ? 255 : (int) (255 - 165 * wonT);
         this.paintMark(g, mark, w / 2.0, h / 2.0, Math.min(w, h) * 0.26, Math.max(2f, Math.min(w, h) / 9f),
            progress(now, this.game.placedAt[this.move()], MARK_MS), alpha);
      } else if (this.game.previewMove() == this.move()) {
         this.paintMark(g, state.xTurn ? "X" : "O", w / 2.0, h / 2.0, Math.min(w, h) * 0.26, Math.max(2f, Math.min(w, h) / 9f), 1, 90);
      } else if (this.game.reviewBetter() == this.move()) {
         // Review: where the search would have played instead, a faint mark of the side that moved in a gold ring.
         this.paintMark(g, state.xTurn ? "O" : "X", w / 2.0, h / 2.0, Math.min(w, h) * 0.26, Math.max(2f, Math.min(w, h) / 9f), 1, 110);
         g.setColor(GOLD);
         g.setStroke(new BasicStroke(Math.max(2f, w / 16f)));
         g.drawRoundRect(pad, pad, w - 2 * pad - 1, h - 2 * pad - 1, arc, arc);
      }
      Review.Grade grade = last ? this.game.reviewGrade() : null;
      if (grade != null && grade != Review.Grade.GOOD) {
         // Review: the move's grade as a badge in the tile's corner ("!" best, "?!" inaccuracy, "?" mistake, "??" blunder).
         int d = Math.max(12, w / 3);
         int x = w - d - pad / 2;
         int y = pad / 2;
         g.setColor(BACKGROUND);
         g.fillOval(x - 1, y - 1, d + 2, d + 2);
         g.setColor(ReviewGraph.color(grade));
         g.fillOval(x, y, d, d);
         g.setColor(BACKGROUND);
         g.setFont(new Font("Dialog", Font.BOLD, Math.max(9, d * 3 / 5)));
         FontMetrics metrics = g.getFontMetrics();
         g.drawString(grade.sign, x + (d - metrics.stringWidth(grade.sign)) / 2, y + (d + metrics.getAscent() - metrics.getDescent()) / 2);
      }

      // Board-wide shapes, in the board's coordinates.
      g.translate(-bx, -by);
      float boardStroke = Math.max(2f, bw / 40f);
      boolean[] targets = this.game.previewTargets();
      if (playable) {
         // The boards the next move may go on, outlined in the color of the side to move.
         this.outline(g, bw, bh, withAlpha(state.xTurn ? X_COLOR : O_COLOR, 220), boardStroke);
      }
      if (targets != null && targets[this.boardIndex]) {
         // Where the previewed move would send the opponent: outlined in their color, inside the first outline.
         this.outline(g, bw, bh, withAlpha(state.xTurn ? O_COLOR : X_COLOR, 160), boardStroke * 0.75f, boardStroke * 2);
      }
      if (result != null && !GameState.DRAW.equals(result)) {
         int[] line = GameState.winningLine(state.marks[this.boardIndex]);
         if (line != null) {
            this.strike(g, line, bw / 3.0, bh / 3.0, withAlpha(Color.WHITE, (int) (230 - 150 * wonT)), Math.max(2f, bw / 36f),
               progress(now, wonAt, LINE_MS));
         }
         this.paintMark(g, result, bw / 2.0, bh / 2.0, Math.min(bw, bh) * 0.32, Math.max(3f, Math.min(bw, bh) / 11f), wonT, 240);
         if (wonT > 0 && wonT < 1) {
            this.paintMark(g, result, bw / 2.0, bh / 2.0, Math.min(bw, bh) * 0.32, Math.max(3f, Math.min(bw, bh) / 11f) * 2.2f, wonT,
               (int) (70 * (1 - wonT)));   // a glow that fades as the mark lands
         }
      }

      // The end of the game: gold around the winning boards, the rest fade back.
      if (state.winner != null) {
         double t = progress(now, this.game.gameWonAt, GAME_OVER_MS);
         int[] line = GameState.DRAW.equals(state.winner) ? null : GameState.winningLine(state.boardResults);
         boolean winning = line != null && (line[0] == this.boardIndex || line[1] == this.boardIndex || line[2] == this.boardIndex);
         if (winning) {
            this.outline(g, bw, bh, withAlpha(GOLD, (int) (255 * t)), boardStroke * 1.4f);
         } else {
            g.setColor(withAlpha(BACKGROUND, (int) (150 * t)));
            g.fillRect(0, 0, bw, bh);
         }
      }
      g.translate(bx, by);

      if (this.game.cursorMove() == this.move()) {
         // The keyboard cursor (the browser also rings the focused cell).
         g.setColor(Color.WHITE);
         g.setStroke(new BasicStroke(2f));
         g.drawRoundRect(pad / 2 + 1, pad / 2 + 1, w - pad - 2, h - pad - 2, arc, arc);
      }
   }

   /** Draws X or O centered on (cx, cy), {@code t} of the way drawn in: X stroke by stroke, O round from the top. */
   private void paintMark(Graphics2D g, String mark, double cx, double cy, double r, float stroke, double t, int alpha) {
      if (t <= 0 || alpha <= 0) {
         return;
      }
      g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      if ("X".equals(mark)) {
         g.setColor(withAlpha(X_COLOR, alpha));
         double first = ease(Math.min(1, t * 2));
         double second = ease(Math.max(0, t * 2 - 1));
         line(g, cx - r, cy - r, cx - r + 2 * r * first, cy - r + 2 * r * first);
         if (second > 0) {
            line(g, cx + r, cy - r, cx + r - 2 * r * second, cy - r + 2 * r * second);
         }
      } else {
         g.setColor(withAlpha(O_COLOR, alpha));
         double sweep = ease(t);
         if (sweep >= 1) {
            g.drawOval((int) Math.round(cx - r), (int) Math.round(cy - r), (int) Math.round(2 * r), (int) Math.round(2 * r));
            return;
         }
         int steps = Math.max(2, (int) (40 * sweep));
         double px = cx;
         double py = cy - r;
         for (int i = 1; i <= steps; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * sweep * i / steps;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            line(g, px, py, x, y);
            px = x;
            py = y;
         }
      }
   }

   /** The line through the three squares of a won board, {@code t} of the way drawn, a little past the end squares. */
   private void strike(Graphics2D g, int[] line, double cellW, double cellH, Color color, float stroke, double t) {
      double x1 = (line[0] % 3 + 0.5) * cellW;
      double y1 = (line[0] / 3 + 0.5) * cellH;
      double x2 = (line[2] % 3 + 0.5) * cellW;
      double y2 = (line[2] / 3 + 0.5) * cellH;
      double dx = (x2 - x1) * 0.18;
      double dy = (y2 - y1) * 0.18;
      x1 -= dx;
      y1 -= dy;
      x2 += dx;
      y2 += dy;
      double e = ease(t);
      g.setColor(color);
      g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      line(g, x1, y1, x1 + (x2 - x1) * e, y1 + (y2 - y1) * e);
   }

   /** A rounded outline just inside the board's edge, {@code inset} further in. */
   private void outline(Graphics2D g, int bw, int bh, Color color, float stroke) {
      this.outline(g, bw, bh, color, stroke, 0);
   }

   private void outline(Graphics2D g, int bw, int bh, Color color, float stroke, float inset) {
      g.setColor(color);
      g.setStroke(new BasicStroke(stroke));
      int i = (int) Math.ceil(stroke / 2 + inset);
      int arc = Math.max(8, bw / 8);
      g.drawRoundRect(i, i, bw - 2 * i - 1, bh - 2 * i - 1, arc, arc);
   }

   private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
      g.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
   }

   /** 0 before {@code start}, 1 once {@code ms} have passed; 1 when nothing was started (start 0). */
   static double progress(long now, long start, int ms) {
      return start == 0 ? 1 : Math.max(0, Math.min(1, (now - start) / (double) ms));
   }

   private static double ease(double t) {
      return 1 - (1 - t) * (1 - t);
   }

   private static Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
   }

   private static Color mix(Color from, Color to, double t) {
      return new Color((int) (from.getRed() + (to.getRed() - from.getRed()) * t), (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t),
         (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t));
   }
}
