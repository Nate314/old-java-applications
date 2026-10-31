package ultimateticktacktoe;

/** Self-check for the Ultimate Tic-Tac-Toe rules. Run with java -ea. */
public class GameStateCheck {
   public static void main(String[] args) {
      // Small board results.
      check("X".equals(GameState.result(new String[]{"X", "X", "X", "", "", "", "", "", ""})), "row win");
      check("O".equals(GameState.result(new String[]{"", "", "O", "", "O", "", "O", "", ""})), "diagonal win");
      check(GameState.result(new String[]{"X", "O", "", "", "", "", "", "", ""}) == null, "open board");
      check(GameState.DRAW.equals(GameState.result(new String[]{"X", "O", "X", "X", "O", "O", "O", "X", "X"})), "full board draws");
      // Draws on the big board do not count as a line.
      check(GameState.result(new String[]{GameState.DRAW, GameState.DRAW, GameState.DRAW, null, null, null, null, null, null}) == null, "draw line");

      // The first move is free, then the cell picked sends the opponent to that board.
      GameState game = new GameState();
      check(game.play(4, 0), "first move anywhere");
      check(game.activeBoard == 0, "sent to board 0");
      check(!game.play(5, 0), "must play on board 0");
      check(!game.play(4, 0), "cannot reuse a cell");
      check(game.play(0, 4), "O plays on board 0");

      // X wins board 0 through cells 0, 1 and 2; then a player sent to board 0 may move anywhere.
      game = new GameState();
      play(game, 0, 1, 1, 0, 0, 2, 2, 0, 0, 0);
      check("X".equals(game.boardResults[0]), "X won board 0");
      check(game.activeBoard == -1, "sent to a won board means free move");
      check(!game.canPlay(0, 4), "won board is closed");

      // X takes boards 0, 1 and 2 for the overall win.
      game = new GameState();
      play(game,
         0, 3, 3, 0, 0, 4, 4, 0, 0, 5, 5, 0, // X takes 0 by its middle row; O's last move is sent to 0, now free
         1, 3, 3, 1, 1, 4, 4, 1, 1, 5,       // X takes 1
         5, 2, 2, 3, 3, 2, 2, 4, 4, 2, 2, 5); // X takes 2
      check("X".equals(game.boardResults[0]) && "X".equals(game.boardResults[1]) && "X".equals(game.boardResults[2]), "X owns the top row");
      check("X".equals(game.winner), "X wins the game, got " + game.winner);
      check(!game.canPlay(8, 8), "no moves after the game ends");

      System.out.println("GameStateCheck passed");
   }

   /** Plays (board, cell) pairs, failing if any is illegal. */
   private static void play(GameState game, int... moves) {
      for (int i = 0; i < moves.length; i += 2) {
         check(game.play(moves[i], moves[i + 1]), "illegal move " + moves[i] + "," + moves[i + 1] + " at step " + i / 2);
      }
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
