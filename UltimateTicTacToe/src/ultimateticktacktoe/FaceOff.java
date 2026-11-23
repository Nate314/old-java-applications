package ultimateticktacktoe;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Beside the board on a landscape screen, in the column's middle: the two sides face off, each a big tile with its mark,
 * its name and the small boards it has won. The side to move is lit and ringed in its color (the ring breathes while
 * the computer thinks), its mark drawing in afresh as the turn passes; when the game ends the winner is ringed in gold.
 */
final class FaceOff extends JPanel {
   private static final Color TILE = new Color(33, 41, 61);
   private static final Color TILE_LIT = new Color(56, 70, 102);
   private static final Color NAME = new Color(230, 235, 245);
   private static final Color QUIET = new Color(150, 162, 186);
   private static final int DRAW_IN_MS = 420;
   private static final int BREATH_MS = 1400;
   /** Below this height there is no room for the tiles, and nothing is drawn. */
   private static final int MIN_HEIGHT = 96;
   private final boolean still;
   private final Timer ticker = new Timer(40, event -> this.tick());
   private String xName = "X";
   private String oName = "O";
   private int xBoards;
   private int oBoards;
   /** "X" or "O" while the game goes on, else null. */
   private String toMove;
   /** "X", "O", GameState.DRAW, or null while the game goes on. */
   private String winner;
   private boolean thinking;
   private long turnAt;

   FaceOff(boolean still) {
      this.still = still;
      this.setBackground(CellButton.BACKGROUND);
      this.setOpaque(true);
   }

   /** Called on every refresh; the side to move's mark draws in again only when the turn has passed. */
   void show(String xName, String oName, String[] boardResults, String toMove, String winner, boolean thinking) {
      int x = 0;
      int o = 0;
      for (String result : boardResults) {
         x += "X".equals(result) ? 1 : 0;
         o += "O".equals(result) ? 1 : 0;
      }
      String lit = winner != null ? winner : toMove;
      String wasLit = this.winner != null ? this.winner : this.toMove;
      if (lit != null && !lit.equals(wasLit)) {
         this.turnAt = System.currentTimeMillis();
      }
      this.xName = xName;
      this.oName = oName;
      this.xBoards = x;
      this.oBoards = o;
      this.toMove = toMove;
      this.winner = winner;
      this.thinking = thinking;
      if (!this.still && (thinking || this.drawing())) {
         this.ticker.start();
      }
      this.repaint();
   }

   private boolean drawing() {
      return System.currentTimeMillis() - this.turnAt < DRAW_IN_MS;
   }

   private void tick() {
      if (!this.thinking && !this.drawing()) {
         this.ticker.stop();
      }
      this.repaint();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      int w = this.getWidth();
      int h = this.getHeight();
      g.setColor(CellButton.BACKGROUND);
      g.fillRect(0, 0, w, h);
      if (h < MIN_HEIGHT) {
         return;
      }
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int text = 44;   // the name and boards lines under each tile
      int tile = Math.min(Math.min(h - text - 16, (w - 72) / 2), 150);
      int top = (h - tile - text) / 2;
      int gap = Math.max(48, (w - 2 * tile) / 3);   // room for "draw" between the tiles
      int left = (w - 2 * tile - gap) / 2;
      this.side(g, "X", this.xName, this.xBoards, left, top, tile);
      this.side(g, "O", this.oName, this.oBoards, left + tile + gap, top, tile);
      String middle = GameState.DRAW.equals(this.winner) ? "draw" : "vs";
      g.setFont(new Font("Dialog", Font.BOLD, 15));
      FontMetrics metrics = g.getFontMetrics();
      g.setColor(QUIET);
      g.drawString(middle, (w - metrics.stringWidth(middle)) / 2, top + tile / 2 + metrics.getAscent() / 2 - 1);
   }

   private void side(Graphics2D g, String mark, String name, int boards, int x, int y, int tile) {
      boolean over = this.winner != null;
      boolean won = mark.equals(this.winner);
      boolean lit = over ? won : mark.equals(this.toMove);
      Color color = "X".equals(mark) ? CellButton.X_COLOR : CellButton.O_COLOR;
      int arc = Math.max(10, tile / 6);
      g.setColor(lit ? TILE_LIT : TILE);
      g.fillRoundRect(x, y, tile, tile, arc, arc);
      if (lit) {
         // Breathing while the computer thinks; steady otherwise.
         double breath = this.thinking && !this.still ? 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 2 * Math.PI / BREATH_MS) : 1;
         Color ring = won ? CellButton.GOLD : color;
         g.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), (int) (110 + 145 * breath)));
         float stroke = Math.max(3f, tile / 28f);
         g.setStroke(new BasicStroke(stroke));
         g.drawRoundRect(x + 1, y + 1, tile - 2, tile - 2, arc, arc);
      }
      double t = lit && !this.still ? Math.min(1, (System.currentTimeMillis() - this.turnAt) / (double) DRAW_IN_MS) : 1;
      CellButton.paintMark(g, mark, x + tile / 2.0, y + tile / 2.0, tile * 0.27, Math.max(4f, tile / 9f), t, 255);
      if (!lit) {
         // Dimmed under a veil of the tile, so the crossing of X's strokes does not show through as it would with a faint stroke.
         g.setColor(new Color(TILE.getRed(), TILE.getGreen(), TILE.getBlue(), over ? 190 : 140));
         g.fillRoundRect(x, y, tile, tile, arc, arc);
      }
      g.setFont(new Font("Dialog", Font.BOLD, 14));
      FontMetrics metrics = g.getFontMetrics();
      String label = won ? name + " won!" : name;
      g.setColor(won ? CellButton.GOLD : lit ? NAME : QUIET);
      g.drawString(label, x + (tile - metrics.stringWidth(label)) / 2, y + tile + 18);
      g.setFont(new Font("Dialog", Font.PLAIN, 12));
      metrics = g.getFontMetrics();
      String count = boards == 1 ? "1 board" : boards + " boards";
      g.setColor(QUIET);
      g.drawString(count, x + (tile - metrics.stringWidth(count)) / 2, y + tile + 36);
   }
}
