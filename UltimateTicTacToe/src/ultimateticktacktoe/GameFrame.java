package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JFrame;

public final class GameFrame extends JFrame {
   private static final Color OPEN_COLOR = new Color(0, 255, 0);
   private static final Color ACTIVE_COLOR = new Color(255, 255, 0);
   private static final Color X_WON_COLOR = new Color(0, 255, 255);
   private static final Color O_WON_COLOR = new Color(255, 175, 175);
   private static final Color DRAW_COLOR = new Color(170, 170, 170);
   public GameState state = new GameState();
   public SmallBoard[] boards = new SmallBoard[]{
      new SmallBoard(this, 0), new SmallBoard(this, 1), new SmallBoard(this, 2), new SmallBoard(this, 3), new SmallBoard(this, 4), new SmallBoard(this, 5), new SmallBoard(this, 6), new SmallBoard(this, 7), new SmallBoard(this, 8)
   };

   public GameFrame() {
      this.setDefaultCloseOperation(3);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4, Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4);
      this.setLayout(null);
      this.setResizable(false);
      int boardWidth = this.getWidth() / 3;
      int boardHeight = this.getHeight() / 3;

      for (int board = 0; board < this.boards.length; board++) {
         this.boards[board].setBounds(board % 3 * boardWidth, board / 3 * boardHeight, boardWidth - 2, boardHeight - 2);
         this.add(this.boards[board]);
         this.boards[board].layoutCells();
      }

      this.setSize(this.getWidth() + 50, this.getHeight() + 50);
      this.refresh();
      this.setVisible(true);
   }

   /** A cell was clicked: play there, or start over if the game has ended. */
   public final void cellClicked(int board, int cell) {
      if (this.state.winner != null) {
         this.state = new GameState();
      } else if (!this.state.play(board, cell)) {
         return;
      }
      this.refresh();
   }

   /** Redraws every cell from the game state: marks, won/drawn boards and the boards the next move may use. */
   private void refresh() {
      for (int board = 0; board < this.boards.length; board++) {
         String result = this.state.boardResults[board];
         boolean playable = result == null && this.state.winner == null && (this.state.activeBoard == -1 || this.state.activeBoard == board);
         Color color = "X".equals(result) ? X_WON_COLOR
            : "O".equals(result) ? O_WON_COLOR
            : GameState.DRAW.equals(result) ? DRAW_COLOR
            : playable ? ACTIVE_COLOR
            : OPEN_COLOR;
         for (int cell = 0; cell < 9; cell++) {
            this.boards[board].cells[cell].show(this.state.marks[board][cell], color);
         }
      }

      String winner = this.state.winner;
      this.setTitle(winner == null ? "Ultimate Tic-Tac-Toe: " + (this.state.xTurn ? "X" : "O") + " to move"
         : GameState.DRAW.equals(winner) ? "Ultimate Tic-Tac-Toe: draw. Click to play again."
         : "Ultimate Tic-Tac-Toe: " + winner + " wins! Click to play again.");
   }
}
