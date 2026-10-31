# NumberGuess

A guess-the-number game: type a guess between 1 and 100 and the label tells you whether it is too low, too high or correct. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- **Text field**: type a whole number from 1 to 100.
- **SUBMIT** or **Enter**: the label says whether the guess is too low or too high and how many guesses you have made. Anything that is not a whole number gets a reminder instead.
- A correct guess shows the guess count and picks a new number, so you can play again straight away.
- The window is 300x200 px. Title "Number Guess".
- No command-line arguments.

## Code layout

- `src/numberguess/Main.java`: entry point and window; passes each guess to the game.
- `src/numberguess/Game.java`: picks the secret number and compares guesses.

## Running

From the repo root: `./run.ps1 NumberGuess` (or `./run.ps1 NumberGuess -Build` to build a jar).
