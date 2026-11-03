package ultimateticktacktoe;

import java.util.Arrays;
import java.util.Random;

/**
 * A finished game, looked over move by move. The computer's Hard search runs on every position of the game, a little
 * at a time (see {@link #analyseNext}), and gives each one X's chances: the share of its random games from there that
 * X won, a draw counting half. The search's own pick in each position is kept as the better move. When a move was not
 * that pick, the position the pick would have led to is searched too, and the move is graded by how much lower the
 * mover's chances were after it than after the pick. Both of those positions have the same side to move, so the
 * search's leaning toward the side to move does not count against the move.
 */
final class Review {
   /** How a move compares with the search's pick, from best to worst; {@code loss} is the most chances it may cost. */
   enum Grade {
      BEST("Best move", "best moves", "!", 0),
      GOOD("Good", "good moves", "", 0.06),
      INACCURACY("Inaccuracy", "inaccuracies", "?!", 0.12),
      MISTAKE("Mistake", "mistakes", "?", 0.25),
      BLUNDER("Blunder", "blunders", "??", 1);

      final String label;
      /** The label of more than one, in lower case ("2 inaccuracies"). */
      final String plural;
      /** The mark a move gets in game notation. */
      final String sign;
      final double loss;

      Grade(String label, String plural, String sign, double loss) {
         this.label = label;
         this.plural = plural;
         this.sign = sign;
         this.loss = loss;
      }

      static Grade of(boolean best, double loss) {
         if (best) {
            return BEST;
         }
         for (Grade grade : values()) {
            if (grade != BEST && loss <= grade.loss) {
               return grade;
            }
         }
         return BLUNDER;
      }

      /** A mistake or a blunder: what Next mistake stops at. */
      boolean bad() {
         return this == MISTAKE || this == BLUNDER;
      }

      /** An inaccuracy or worse: shown with a better move. */
      boolean slip() {
         return this.ordinal() >= INACCURACY.ordinal();
      }
   }

   /** The moves, as board * 9 + cell. */
   final int[] moves;
   /** positions[k] is the game after its first k moves; positions[moves.length] is the end. */
   final GameState[] positions;
   /** X's chances in each position, or NaN until it is analysed; exact once the game is over. */
   final double[] chances;
   /** The search's pick in each position, or -1 (not analysed yet, or the game is over). */
   final int[] better;
   /** X's chances after the search's pick instead of move k, or NaN (not searched yet, or the move was the pick). */
   private final double[] instead;
   /** Positions analysed so far, from the start. */
   private int analysed;
   /** Moves whose pick's position has been searched (or needed no search), from the start. */
   private int compared;

   /** Replays a FriendLink code of a whole game; null if it is not one. */
   static Review of(String code) {
      return FriendLink.replay(code) == null ? null : new Review(code);
   }

   private Review(String code) {
      int count = code.length() / 2;
      this.moves = new int[count];
      this.positions = new GameState[count + 1];
      this.chances = new double[count + 1];
      this.better = new int[count + 1];
      this.instead = new double[count];
      this.positions[0] = new GameState();
      for (int k = 0; k < count; k++) {
         this.moves[k] = (code.charAt(2 * k) - 'a') * 9 + code.charAt(2 * k + 1) - '1';
         this.positions[k + 1] = this.positions[k].copy();
         this.positions[k + 1].play(this.moves[k] / 9, this.moves[k] % 9);
      }
      Arrays.fill(this.chances, Double.NaN);
      Arrays.fill(this.better, -1);
      Arrays.fill(this.instead, Double.NaN);
   }

   int length() {
      return this.moves.length;
   }

   boolean done() {
      return this.analysed > this.moves.length && this.compared == this.moves.length;
   }

   /** Positions analysed so far, from the start. */
   int analysed() {
      return this.analysed;
   }

   /**
    * Runs the next search of about {@code millis}: the pick's position for the earliest move that needs one once the
    * positions on both sides of it are known, else the next position of the game. False once all are done.
    */
   boolean analyseNext(Random random, long millis) {
      while (this.compared < this.analysed - 1) {
         int k = this.compared++;
         if (this.better[k] != this.moves[k]) {
            GameState after = this.positions[k].copy();
            after.play(this.better[k] / 9, this.better[k] % 9);
            this.instead[k] = xChances(after, random, millis);
            return true;
         }
      }
      if (this.analysed > this.moves.length) {
         return false;
      }
      int k = this.analysed++;
      GameState position = this.positions[k];
      if (position.winner == null) {
         Computer.Verdict verdict = Computer.analyse(position, random, millis);
         this.chances[k] = position.xTurn ? verdict.score : 1 - verdict.score;
         this.better[k] = verdict.move;
      } else {
         this.chances[k] = xChances(position, random, millis);
      }
      return true;
   }

   /** X's chances in a position: exact once the game is over, else from a search. */
   private static double xChances(GameState position, Random random, long millis) {
      if (position.winner != null) {
         return "X".equals(position.winner) ? 1 : "O".equals(position.winner) ? 0 : 0.5;
      }
      double score = Computer.analyse(position, random, millis).score;
      return position.xTurn ? score : 1 - score;
   }

   /** True if move {@code k} (0-based) was made by X. */
   boolean byX(int k) {
      return this.positions[k].xTurn;
   }

   /** The chances of the side that made move {@code k} after it. */
   double moverChancesAfter(int k) {
      return this.byX(k) ? this.chances[k + 1] : 1 - this.chances[k + 1];
   }

   /** The chances of the side that made move {@code k} after the search's pick instead (NaN when the move was the pick). */
   double moverChancesInstead(int k) {
      return this.byX(k) ? this.instead[k] : 1 - this.instead[k];
   }

   /** The chances move {@code k} cost the side that made it, against the search's pick (0 for the pick itself), or NaN until known. */
   double loss(int k) {
      if (this.better[k] < 0) {
         return Double.NaN;
      }
      if (this.better[k] == this.moves[k]) {
         return 0;
      }
      // NaN (and so not graded yet) until the pick's position and the next one are both searched.
      return Math.max(0, this.moverChancesInstead(k) - this.moverChancesAfter(k));
   }

   /** The grade of move {@code k}, or null until it is analysed. */
   Grade grade(int k) {
      double loss = this.loss(k);
      return Double.isNaN(loss) ? null : Grade.of(this.better[k] == this.moves[k], loss);
   }

   /** How many of one side's moves got each grade, indexed by Grade.ordinal(). */
   int[] count(boolean x) {
      int[] counts = new int[Grade.values().length];
      for (int k = 0; k < this.moves.length; k++) {
         Grade grade = this.grade(k);
         if (grade != null && this.byX(k) == x) {
            counts[grade.ordinal()]++;
         }
      }
      return counts;
   }

   /** The move that cost the most (the turning point), or -1 if none cost more than a Good move may. */
   int turningPoint() {
      int worst = -1;
      for (int k = 0; k < this.moves.length; k++) {
         double loss = this.loss(k);
         if (!Double.isNaN(loss) && loss > Grade.GOOD.loss && (worst < 0 || loss > this.loss(worst))) {
            worst = k;
         }
      }
      return worst;
   }

   /** The next mistake or blunder after move {@code k} (0-based), wrapping around; -1 if there is none. */
   int nextBad(int k) {
      for (int step = 1; step <= this.moves.length; step++) {
         int at = (k + step + this.moves.length) % this.moves.length;
         Grade grade = this.grade(at);
         if (grade != null && grade.bad()) {
            return at;
         }
      }
      return -1;
   }
}
