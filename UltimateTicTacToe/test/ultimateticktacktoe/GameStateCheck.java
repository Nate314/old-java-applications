package ultimateticktacktoe;

import java.util.Random;

/** Self-check for the Ultimate Tic-Tac-Toe rules. Run with java -ea. */
public class GameStateCheck {
   public static void main(String[] args) {
      // Small board results.
      check("X".equals(GameState.result(new String[]{"X", "X", "X", "", "", "", "", "", ""})), "row win");
      check("O".equals(GameState.result(new String[]{"", "", "O", "", "O", "", "O", "", ""})), "diagonal win");
      check(GameState.result(new String[]{"X", "O", "", "", "", "", "", "", ""}) == null, "open board");
      check(GameState.DRAW.equals(GameState.result(new String[]{"X", "O", "X", "X", "O", "O", "O", "X", "X"})), "full board draws");
      // The line a win is struck through, for drawing.
      check(java.util.Arrays.equals(GameState.winningLine(new String[]{"", "", "O", "", "O", "", "O", "", ""}), new int[]{2, 4, 6}), "winning line");
      check(GameState.winningLine(new String[]{"X", "O", "X", "X", "O", "O", "O", "X", "X"}) == null, "no line in a draw");
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

      // Copies are independent, and legal moves follow the active board.
      game = new GameState();
      game.play(4, 0);
      GameState copy = game.copy();
      copy.play(0, 4);
      check(game.marks[0][4].isEmpty() && "O".equals(copy.marks[0][4]), "copy is independent");
      check(game.legalMoves().length == 9 && game.legalMoves()[0] == 0, "nine moves on board 0");
      check(game.lastMove == 4 * 9 && copy.lastMove == 4, "last move");
      check(new GameState().legalMoves().length == 81, "81 first moves");

      // Both computer levels take a game-winning move: X owns boards 0 and 1 and is sent to board 2 with 0 and 1 taken.
      game = new GameState();
      play(game,
         0, 3, 3, 0, 0, 4, 4, 0, 0, 5, 5, 0,
         1, 3, 3, 1, 1, 4, 4, 1, 1, 5,
         5, 2, 2, 3, 3, 2, 2, 4, 4, 2);
      check(game.xTurn && game.activeBoard == 2 && game.winner == null, "set up for the winning move");
      Random random = new Random(1);
      check(Computer.easyMove(game, random) == 2 * 9 + 5, "easy takes the win");
      check(Computer.hardMove(game, random, 50) == 2 * 9 + 5, "hard takes the win");
      // Hints: the search grown a slice at a time has chances for every legal move only, and picks the win.
      Computer.Think think = new Computer.Think(game);
      for (int slice = 0; slice < 5; slice++) {
         think.grow(random, 10);
      }
      double[] chances = think.chances();
      for (int move = 0; move < 81; move++) {
         boolean legal = game.canPlay(move / 9, move % 9);
         check(legal == !Double.isNaN(chances[move]), "a hint for move " + move + " only if it is legal");
         check(!legal || chances[move] >= 0 && chances[move] <= 1, "hint chances in range for " + move);
      }
      check(think.best() == 2 * 9 + 5 && chances[2 * 9 + 5] > 0.95, "the hint's best is the win, near certain");
      // Hard thinking on the board: each legal move's share of the search, the pick at 1, nothing for other cells.
      double[] attention = think.attention();
      for (int move = 0; move < 81; move++) {
         check(attention[move] >= 0 && attention[move] <= 1 && (attention[move] == 0 || game.canPlay(move / 9, move % 9)), "attention in range, legal moves only, for " + move);
      }
      check(attention[think.best()] == 1, "the most searched move has full attention");
      check(new Computer.Think(new GameState()).best() == -1, "no pick before any search");
      check(new Computer.Think(new GameState()).attention()[40] == 0, "no attention before any search");
      // A short search of an empty board: doubts are positive where there are chances, and shrink with more search.
      think = new Computer.Think(new GameState());
      think.grow(random, 5);
      double before = think.doubts()[40];
      think.grow(random, 100);
      check(before > 0 && think.doubts()[40] < before && Double.isNaN(think.doubts()[0]) == Double.isNaN(think.chances()[0]), "doubts shrink as the search grows");

      // Full games: every computer move is legal, and Hard (a short search) beats Easy far more often than not.
      int hardWins = 0;
      int games = 10;
      for (int round = 0; round < games; round++) {
         game = new GameState();
         boolean hardIsX = round % 2 == 0;
         while (game.winner == null) {
            boolean hardMoves = game.xTurn == hardIsX;
            int move = hardMoves ? Computer.hardMove(game, random, 30) : Computer.easyMove(game, random);
            check(game.play(move / 9, move % 9), "computer played an illegal move " + move);
         }
         if (game.winner.equals(hardIsX ? "X" : "O")) {
            hardWins++;
         }
      }
      check(hardWins >= 7, "hard won only " + hardWins + " of " + games + " games against easy");

      // Friend by link: a game's code replays to the same position, and only legal games are codes.
      game = new GameState();
      StringBuilder code = new StringBuilder();
      while (game.winner == null) {
         int move = Computer.easyMove(game, random);
         game.play(move / 9, move % 9);
         code.append(FriendLink.moveCode(move));
      }
      GameState replayed = FriendLink.replay(code.toString());
      check(replayed != null && game.winner.equals(replayed.winner) && replayed.lastMove == game.lastMove, "a whole game replays");
      for (int board = 0; board < 9; board++) {
         check(java.util.Arrays.equals(game.marks[board], replayed.marks[board]), "replayed marks on board " + board);
      }
      check(FriendLink.moveCode(4 * 9 + 4).equals("e5") && FriendLink.moveCode(0).equals("a1") && FriendLink.moveCode(80).equals("i9"), "move names");
      check(FriendLink.replay("").legalMoves().length == 81, "the empty code is a new game");
      check(FriendLink.replay("e5e5") == null, "a taken cell is not a code");
      check(FriendLink.replay("e5a1") == null, "a move off the active board is not a code");
      check(FriendLink.replay("e") == null && FriendLink.replay("j1") == null && FriendLink.replay("e0") == null, "malformed codes");
      check(FriendLink.replay(code + "a1") == null, "no moves after the game ends");
      check(FriendLink.codeIn("https://example.org/UltimateTicTacToe/index.html?game=e5a1&x=1#src").equals("e5a1"), "code in a link");
      check(FriendLink.codeIn(" E5 A1 ").equals("e5a1"), "a pasted code");
      check("https://example.org/UltimateTicTacToe/index.html".equals(FriendLink.pageOf("https://example.org/UltimateTicTacToe/index.html?embed#x")), "page of a link");
      check(FriendLink.pageOf("e5a1") == null, "a code is no page");
      check(FriendLink.message(null, "e5").equals("e5") && FriendLink.message("https://h/p", "e5").equals("https://h/p?game=e5"), "messages");

      // Review: every position of a whole game gets X's chances (exact at the end) and every move a grade.
      Review review = Review.of(code.toString());
      check(review != null && review.length() == code.length() / 2 && !review.done(), "a review of the whole game");
      check(Review.of("e5e5") == null, "no review of a broken code");
      check(review.grade(0) == null && Double.isNaN(review.loss(0)), "no grade before analysis");
      int steps = 0;
      while (review.analyseNext(random, 5)) {
         steps++;
      }
      check(steps > review.length() && steps <= 2 * review.length() + 1 && review.done(), "one or two steps per position");
      double end = "X".equals(game.winner) ? 1 : "O".equals(game.winner) ? 0 : 0.5;
      check(review.chances[review.length()] == end && review.better[review.length()] == -1, "the end is exact");
      int[] countX = review.count(true);
      int[] countO = review.count(false);
      int graded = 0;
      for (int k = 0; k < review.length(); k++) {
         check(review.chances[k] >= 0 && review.chances[k] <= 1, "chances in range at " + k);
         check(review.grade(k) != null && review.loss(k) >= 0, "graded move " + k);
         check(review.positions[k].canPlay(review.better[k] / 9, review.better[k] % 9), "the better move is legal at " + k);
      }
      for (int i = 0; i < countX.length; i++) {
         graded += countX[i] + countO[i];
      }
      check(graded == review.length(), "every move counted once");
      int bad = review.nextBad(-1);
      check(bad == -1 || review.grade(bad).bad(), "next mistake is one");
      check(Review.Grade.of(true, 0.9) == Review.Grade.BEST && Review.Grade.of(false, 0.05) == Review.Grade.GOOD
         && Review.Grade.of(false, 0.12) == Review.Grade.INACCURACY && Review.Grade.of(false, 0.2) == Review.Grade.MISTAKE
         && Review.Grade.of(false, 0.5) == Review.Grade.BLUNDER, "grade thresholds");

      // Missing a game-winning move: the review's better move is the win, and taking it is the best move.
      String setup = "a4d1a5e1a6f1b4d2b5e2b6f3c4d3c5e3";
      check(FriendLink.replay(setup) != null && FriendLink.replay(setup).activeBoard == 2, "review setup");
      for (String last : new String[]{"c7", "c6"}) {
         review = Review.of(setup + last);
         while (review.analyseNext(random, 20)) {
            // analyse every position
         }
         int k = review.length() - 1;
         check(review.better[k] == 2 * 9 + 5, "the better move wins the game");
         check("c6".equals(last) == (review.grade(k) == Review.Grade.BEST), "grade of " + last + ": " + review.grade(k));
      }

      checkLesson();
      checkSounds();
      System.out.println("GameStateCheck passed (hard beat easy " + hardWins + " of " + games + ")");
   }

   /**
    * Every How to play step starts as a live game with X to move, as many X's as O's and the last move O's (on the board
    * cell it sends X to); some legal move does the task, and every step but the "play where you were sent" one also
    * turns some legal move away.
    */
   private static void checkLesson() {
      for (int i = 0; i < Lesson.STEPS.length; i++) {
         Lesson.Step step = Lesson.STEPS[i];
         GameState start = step.start();
         int x = 0;
         int o = 0;
         for (String[] board : start.marks) {
            for (String mark : board) {
               x += "X".equals(mark) ? 1 : 0;
               o += "O".equals(mark) ? 1 : 0;
            }
         }
         check(start.winner == null && start.xTurn && x == o, "lesson step " + i + " position");
         if (start.lastMove >= 0) {
            int sentTo = start.lastMove % 9;
            check("O".equals(start.marks[start.lastMove / 9][sentTo]), "lesson step " + i + " last move is O's");
            check(start.activeBoard == (start.boardResults[sentTo] == null ? sentTo : -1), "lesson step " + i + " sent to the last move's board");
         }
         int right = 0;
         int wrong = 0;
         for (int move : start.legalMoves()) {
            String why = step.wrong(start, move);
            right += why == null ? 1 : 0;
            wrong += why == null ? 0 : 1;
         }
         check(right > 0, "lesson step " + i + " can be done");
         check(i == 1 ? wrong == 0 : wrong > 0, "lesson step " + i + " turns a move away");
      }
      check(Lesson.STEPS[4].wrong(Lesson.STEPS[4].start(), 5 * 9 + 1) != null, "sending O to its two in a row is turned away");
      check(Lesson.STEPS[5].wrong(Lesson.STEPS[5].start(), 8 * 9 + 8) == null, "the winning move wins the lesson game");
   }

   /**
    * Each cell has its own note, rising in reading order with no repeats, O an octave under X; a won line plays its three
    * notes in order (the game's an octave up); off is silent; and sound=off is kept in the record file.
    */
   private static void checkSounds() {
      for (int cell = 1; cell < 9; cell++) {
         check(Sounds.note(cell, true) > Sounds.note(cell - 1, true), "notes rise in reading order");
         check(Sounds.note(cell, false) == Sounds.note(cell, true) - 12, "O an octave under X");
      }
      Sounds sounds = Sounds.silent(true);
      sounds.mark(4, true);
      sounds.mark(4, false);
      sounds.line(new int[]{2, 4, 6}, false, 0, 300, 12);
      check(sounds.heard.equals(java.util.Arrays.asList("0:69", "1:57", "1:64", "1:69", "1:74")), "notes heard " + sounds.heard);
      sounds.setOn(false);
      sounds.mark(0, true);
      sounds.refused();
      check(sounds.heard.size() == 5, "off is silent");
      try {
         java.io.File file = java.io.File.createTempFile("tictactoe", ".txt");
         file.deleteOnExit();
         Record record = Record.load(file);
         check(record.soundOn(), "sound starts on");
         record.setSoundOn(false);
         check(!Record.load(file).soundOn(), "sound=off is kept");
         record.setSoundOn(true);
         check(Record.load(file).soundOn(), "sound on again");
      } catch (java.io.IOException e) {
         throw new AssertionError(e);
      }
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
