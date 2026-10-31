# NumberGuess

A guess-the-number game: type a guess and the label tells you whether it is too low, too high or correct. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- **Text field**: type a whole number.
- **SUBMIT**: replaces the label with `too low`, `too high` or `correct`. Enter does not submit.
- The number stays the same until the program is restarted. There is no new-game button.
- The window is 300x200 px. Title "Number Guess".
- No command-line arguments.

## Code layout

- `src/numberguess/Main.java`: entry point and window; passes each guess to the game.
- `src/numberguess/Game.java`: picks the secret number and compares guesses.

## Running

From the repo root: `./run.ps1 NumberGuess` (or `./run.ps1 NumberGuess -Build` to build a jar).

## Known quirks

- The label says "Guess a number btwn 1 and 100", but the number comes from `nextInt(100)`, so it is 0 to 99.
- Input that is not a whole number throws an uncaught `NumberFormatException` and the label does not change.
