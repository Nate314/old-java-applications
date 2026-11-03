package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * The game review's chart, in place of the mode buttons: one column per position, rising in blue while X's chances are
 * above even and falling in coral while O's are, filling in as the positions are analysed. Moves graded an inaccuracy
 * or worse get a dot in their grade's color, the position on the board is outlined in white, and a click or a drag
 * jumps there.
 */
final class ReviewGraph extends JPanel {
   private static final Color CHART = new Color(33, 41, 61);
   private static final Color MIDDLE = new Color(90, 102, 130);
   private final GameFrame game;

   ReviewGraph(GameFrame game, Dimension size) {
      this.game = game;
      this.setPreferredSize(size);
      this.setBackground(CellButton.BACKGROUND);
      MouseAdapter jump = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            ReviewGraph.this.jump(event.getX());
         }

         @Override
         public void mouseDragged(MouseEvent event) {
            ReviewGraph.this.jump(event.getX());
         }
      };
      this.addMouseListener(jump);
      this.addMouseMotionListener(jump);
   }

   /** A grade's color, on the chart and on the board's badges. */
   static Color color(Review.Grade grade) {
      switch (grade) {
         case BEST: return new Color(92, 214, 150);
         case GOOD: return new Color(150, 162, 186);
         case INACCURACY: return new Color(255, 214, 92);
         case MISTAKE: return new Color(255, 150, 60);
         default: return new Color(240, 72, 72);
      }
   }

   private void jump(int x) {
      Review review = this.game.review();
      if (review != null) {
         this.game.reviewJump(Math.max(0, Math.min(review.length(), (x - this.left()) / Math.max(1, this.column(review)))), false);
      }
   }

   /**
    * Where X's chances sit on the chart: even in the middle, certain at the edges. The distance from even is stretched
    * (its square root), so the small leads of an opening still show on a chart this short.
    */
   private static int y(double chances, int top, int height) {
      double lead = chances - 0.5;
      double stretched = Math.signum(lead) * Math.sqrt(Math.abs(lead) * 2) / 2;
      return top + (int) Math.round((0.5 - stretched) * height);
   }

   private int left() {
      return 6;
   }

   private int column(Review review) {
      return Math.max(1, (this.getWidth() - 2 * this.left()) / (review.length() + 1));
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int w = this.getWidth();
      int h = this.getHeight();
      g.setColor(CellButton.BACKGROUND);
      g.fillRect(0, 0, w, h);
      Review review = this.game.review();
      if (review == null) {
         return;
      }
      int left = this.left();
      int column = this.column(review);
      int top = 4;
      int height = h - 8;
      int middle = top + height / 2;
      int right = left + column * (review.length() + 1);
      g.setColor(CHART);
      g.fillRoundRect(left - 3, top - 2, right - left + 6, height + 4, 8, 8);
      // X's chances: up from the middle in blue while above even, down in coral while below.
      for (int k = 0; k < review.analysed(); k++) {
         double chances = review.chances[k];
         int y = y(chances, top, height);
         g.setColor(chances >= 0.5 ? CellButton.X_COLOR : CellButton.O_COLOR);
         g.fillRect(left + k * column, Math.min(y, middle), Math.max(1, column - (column > 3 ? 1 : 0)), Math.max(1, Math.abs(y - middle)));
      }
      g.setColor(MIDDLE);
      g.drawLine(left, middle, right, middle);
      // The position on the board.
      int at = this.game.reviewAt();
      g.setColor(Color.WHITE);
      g.drawRect(left + at * column - 1, top - 1, column + 1, height + 1);
      // A dot for each inaccuracy, mistake and blunder, on the position the move led to.
      int dot = Math.max(5, Math.min(9, column + 2));
      for (int k = 0; k < review.length(); k++) {
         Review.Grade grade = review.grade(k);
         if (grade != null && grade.slip()) {
            int x = left + (k + 1) * column + column / 2;
            int y = y(review.chances[k + 1], top, height);
            y = Math.max(top + dot / 2, Math.min(top + height - dot / 2, y));
            g.setColor(CellButton.BACKGROUND);
            g.fillOval(x - dot / 2 - 1, y - dot / 2 - 1, dot + 2, dot + 2);
            g.setColor(color(grade));
            g.fillOval(x - dot / 2, y - dot / 2, dot, dot);
         }
      }
   }
}
