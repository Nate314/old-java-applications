package ultimateticktacktoe;

import java.util.Arrays;

/** The rules of Ultimate Tic-Tac-Toe, independent of the UI. */
public final class GameState {
   public static final String DRAW = "draw";
   private static final int[][] LINES = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};
   /** marks[board][cell] is "", "X" or "O". */
   public final String[][] marks = new String[9][9];
   /** boardResults[board] is null while open, else "X", "O" or DRAW. */
   public final String[] boardResults = new String[9];
   /** The board the next move must be on, or -1 for any open board. */
   public int activeBoard = -1;
   public boolean xTurn = true;
   /** Null while the game is on, else "X", "O" or DRAW. */
   public String winner = null;

   public GameState() {
      for (String[] board : this.marks) {
         Arrays.fill(board, "");
      }
   }

   public boolean canPlay(int board, int cell) {
      return this.winner == null
         && this.boardResults[board] == null
         && (this.activeBoard == -1 || this.activeBoard == board)
         && this.marks[board][cell].isEmpty();
   }

   /** Places the current player's mark; returns false if the move is not allowed. */
   public boolean play(int board, int cell) {
      if (!this.canPlay(board, cell)) {
         return false;
      }
      this.marks[board][cell] = this.xTurn ? "X" : "O";
      this.boardResults[board] = result(this.marks[board]);
      this.winner = result(this.boardResults);
      // Sent to a finished board: the next player may move on any open board.
      this.activeBoard = this.boardResults[cell] == null ? cell : -1;
      this.xTurn = !this.xTurn;
      return true;
   }

   /** "X" or "O" if they own a line, DRAW if every square is taken (or drawn) without one, else null. */
   static String result(String[] squares) {
      for (int[] line : LINES) {
         String first = squares[line[0]];
         if (("X".equals(first) || "O".equals(first)) && first.equals(squares[line[1]]) && first.equals(squares[line[2]])) {
            return first;
         }
      }
      for (String square : squares) {
         if (square == null || square.isEmpty()) {
            return null;
         }
      }
      return DRAW;
   }
}
