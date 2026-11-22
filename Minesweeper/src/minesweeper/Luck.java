package minesweeper;

import java.util.ArrayList;
import java.util.List;

/**
 * The guesses a player made in one game: each tile opened that the numbers did not show to be safe, with its chance
 * of being a mine at the moment it was opened. Those chances are each read from the board as it stood then, so the
 * chance of surviving them all is their product.
 */
public final class Luck {
   /** Cells (x * height + y) guessed on, and the chance each was a mine, in the order played. */
   final List<Integer> cells = new ArrayList<>();
   final List<Double> risks = new ArrayList<>();
   /** Moves made while Odds gave up on the board (too open to read), so whether they were guesses is not known. */
   int unread;

   /** Notes a move on that cell; a certain one (0%) is not a guess and is not kept. */
   void add(int cell, double risk) {
      if (risk > 1e-9) {
         this.cells.add(cell);
         this.risks.add(risk);
      }
   }

   /** The last move is taken back (a mine undone): forgets it, as noted with that risk (-1 when Odds could not read it). */
   void takeBack(double risk) {
      if (risk < 0) {
         this.unread--;
      } else if (risk > 1e-9) {
         this.cells.remove(this.cells.size() - 1);
         this.risks.remove(this.risks.size() - 1);
      }
   }

   int guesses() {
      return this.risks.size();
   }

   /** The chance of coming through the first {@code count} guesses. */
   double survival(int count) {
      double chance = 1;
      for (int i = 0; i < count; i++) {
         chance *= 1 - this.risks.get(i);
      }
      return chance;
   }

   /** After a win: what can honestly be said about guessing, or null when every move was counted and none was a guess. */
   String clearedLine() {
      int n = this.guesses();
      String some = n + (n == 1 ? " guess" : " guesses");
      if (this.unread == 0) {
         return n == 0 ? null : "Cleared, with " + some + ".";
      }
      return (n == 0 ? "Cleared, no guesses" : "Cleared, with " + some) + " in the moves counted.";
   }

   /** After a win with moves Odds could not read and no guesses to tag: says so. */
   String unreadLine() {
      return this.unread + (this.unread == 1 ? " move was" : " moves were") + " on a board too open to count.";
   }

   /** After a win: how lucky the guesses were, or null with none. */
   String winLine() {
      int n = this.guesses();
      if (n == 0) {
         return null;
      }
      return n == 1 ? "Odds your guess came off: " + Odds.percent(this.survival(1)) + ". It is tagged in purple."
         : "Odds your " + n + " guesses all came off: " + Odds.percent(this.survival(n)) + ". Each is tagged in purple.";
   }

   /** After a mine: the guess that lost (the last one) and the ones survived before it. */
   String[] lossLines() {
      int n = this.guesses();
      double risk = this.risks.get(n - 1);
      return new String[]{risk > 1 - 1e-9 ? "The numbers showed that one was a mine." : "That one was a guess: " + Odds.percent(risk) + " likely a mine.",
         n == 1 ? "Tagged in purple, with its chance of being a mine."
            : "Guess " + n + ". Odds " + (n == 2 ? "the guess" : "the " + (n - 1) + " guesses") + " before it came off: "
               + Odds.percent(this.survival(n - 1)) + "."};
   }
}
