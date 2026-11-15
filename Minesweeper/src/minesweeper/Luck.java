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

   /** Notes a move on that cell; a certain one (0%) is not a guess and is not kept. */
   void add(int cell, double risk) {
      if (risk > 1e-9) {
         this.cells.add(cell);
         this.risks.add(risk);
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
