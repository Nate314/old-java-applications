package ultimateticktacktoe;

/**
 * How to play (L): six set positions on the real board, each with one rule and one task for X. A move that does the
 * task is played and explained; one that does not is turned away with the reason, so every rule is learned by doing it.
 */
final class Lesson {
   /** Why a move misses the task, or null when it does it; {@code after} is the position once the move is played. */
   interface Check {
      String wrong(GameState after, int move);
   }

   static final class Step {
      /** The rule this step teaches, for the line under the status. */
      final String rule;
      /** What to do, for the status line. */
      final String task;
      /** Said once the task is done. */
      final String done;
      private final String board;
      private final int active;
      private final int last;
      private final Check check;

      Step(String rule, String task, String done, String board, int active, int last, Check check) {
         this.rule = rule;
         this.task = task;
         this.done = done;
         this.board = board;
         this.active = active;
         this.last = last;
         this.check = check;
      }

      /** A fresh copy of the step's position, X to move. */
      GameState start() {
         GameState state = new GameState();
         String[] boards = this.board.split(" ");
         for (int b = 0; b < 9; b++) {
            for (int c = 0; c < 9; c++) {
               char mark = boards[b].charAt(c);
               state.marks[b][c] = mark == '.' ? "" : String.valueOf(mark);
            }
            state.boardResults[b] = GameState.result(state.marks[b]);
         }
         state.winner = GameState.result(state.boardResults);
         state.activeBoard = this.active;
         state.lastMove = this.last;
         return state;
      }

      /** Null if the move does the task, else why not; an illegal move is turned away before this is asked. */
      String wrong(GameState before, int move) {
         GameState after = before.copy();
         after.play(move / 9, move % 9);
         return this.check.wrong(after, move);
      }
   }

   private static final String EMPTY = "......... ......... ......... ......... ......... ......... ......... ......... .........";

   static final Step[] STEPS = {
      new Step("The square you pick sends them to that board.", "Send O to the middle board.",
         "Your middle square sent O to the middle board.", EMPTY, -1, -1,
         (after, move) -> move % 9 == 4 ? null : "Sends O to the " + CellButton.PLACES[move % 9] + ". Pick a middle square."),
      new Step("You must play on the board you were sent to.", "O sent you top right. Play there.",
         "Right. Now O must play where your square points.",
         "......... ......... ..O...... ......... ..X...... ......... ......... ......... .........", 2, 20,
         (after, move) -> null),
      new Step("Three in a row wins a small board.", "Win the top left board.",
         "The board is yours: X owns it now.",
         "XX.O..... O........ ......... ......... ......... ......... ......... ......... .........", 0, 9,
         (after, move) -> "X".equals(after.boardResults[0]) ? null : "Not yet: finish the row of two X's."),
      new Step("Sent to a finished board? Play on any open board.", "You were sent to a won board. Win the bottom right.",
         "Any open board was yours, so you took the bottom right.",
         "....O.... ......... O........ ......... XO..X.O.X ......... O........ ......... X...X....", -1, 4,
         (after, move) -> "X".equals(after.boardResults[8]) ? null : "Any board is open: finish the bottom right one."),
      new Step("Look where your move sends them.", "O can win the top board. Do not send O there.",
         "Safe: O cannot win a board from where you sent them.",
         ".X....... OO....... ......... .......X. ......... ....X.... ......... .....O... .........", 5, 68,
         (after, move) -> givesBoard(after) ? "That sends O to win the " + CellButton.PLACES[move % 9] + " board." : null),
      new Step("Three boards in a row wins the game.", "Win the game in one move.",
         "You win! Press Play to take on the computer.",
         "XXXOO.... OO....... ........O ......... XOO.X...X ......... ......... ......... O.X..X...", 8, 26,
         (after, move) -> "X".equals(after.winner) ? null : "Finish the bottom right board: three in a row."),
   };

   private Lesson() {
   }

   /** True if the side to move can win a small board at once. */
   static boolean givesBoard(GameState state) {
      for (int move : state.legalMoves()) {
         GameState next = state.copy();
         next.play(move / 9, move % 9);
         if (next.boardResults[move / 9] != null && !GameState.DRAW.equals(next.boardResults[move / 9])) {
            return true;
         }
      }
      return false;
   }
}
