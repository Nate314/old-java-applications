# NumberGuess

A guess-the-number game, grown from the original one-label version (type a guess from 1 to 100, read "too low" or "too high") into five ranges with par, stars and a number line, plus a mode where the computer guesses your number. Original Eclipse source from the author's own files (only `src/` was kept).

## How it plays

Tabs along the top pick the range: **1-10**, **1-100**, **1-1,000**, **1-10,000** and **1-1,000,000**, then **You pick**.

- Each range has a par: the most guesses a search that always guesses the middle of what is left can need (4, 7, 10, 14 and 20). You get par + 3 guesses; run out and the number is given away.
- Finding it within par is three stars, within par + 2 two stars, and in the last guesses one star. Two stars opens the next range (a locked tab shows a padlock and says what opens it). Each tab shows its best stars.
- Two number lines show what you know. The top one is the whole range, with what is still possible lit in green and a marker for each guess (blue too low, red too high, green right; the latest is labelled). The lit part slides as it shrinks and opens out into the lower line, which shows only what is still possible: one cell per number once few enough are left. Clicking the lower line guesses the number under the pointer (shown in a bubble), so even 1-1,000,000 can be played with the mouse alone.
- A guess outside the range, or one your earlier answers already rule out, is not counted; the line under the verdict says why.
- Dots under the verdict count the guesses: grey up to par, orange for the three spare ones.
- The guesses so far sit at the bottom as chips with an up or down arrow. When the number is found the stars show beside them, with "New best!" when it took fewer guesses than before.

**You pick** turns the game around: think of a number from 1 to 100, and the computer guesses it, always in the middle of what is left, so it never needs more than 7 guesses. Answer with **Lower**, **That's it!** or **Higher**. If your answers leave no number possible, it says so ("You said higher than 41 and lower than 42. Did your number change?").

The fewest guesses for each range is saved to `.numberguess-record.txt` in the user's home folder, one `LEVEL=stars,guesses` line per range won (for example `HUNDRED=3,6`).

## Controls / usage

- **Text field** and **SUBMIT** or **Enter**: guess a whole number (commas are allowed, as in `250,000`). After the number is found or given away, Enter starts a new one (in the next range, if that win just opened it).
- **Mouse**: click a tab to switch range; click the lower number line to guess the number under the pointer, or after the game to start a new one.
- **Page Up / Page Down**: previous / next tab.
- **You pick**: **Up** or **H** for higher, **Down** or **L** for lower, **Enter**, **Space**, **C** or **Y** for "that's it". After the round, any of these starts again.
- The window is about 616x425 px (sized to fit the board). Title "Number Guess". The window opens on the first open range without three stars.
- No command-line arguments.

## Code layout

- `src/numberguess/Main.java`: entry point and window; the board, the original text field and SUBMIT button, and the You pick buttons and keys.
- `src/numberguess/Board.java`: draws the tabs, verdict, guess dots, both number lines and the chips, and handles the mouse.
- `src/numberguess/Game.java`: picks the secret number and narrows what is still possible with each guess.
- `src/numberguess/Level.java`: the five ranges, their par, guess limit and stars.
- `src/numberguess/MindReader.java`: You pick; guesses the middle and notices answers that contradict each other.
- `src/numberguess/Records.java`: reads and writes the fewest guesses per range, and which ranges are open.

## Running

From the repo root: `./run.ps1 NumberGuess` (or `./run.ps1 NumberGuess -Build` to build a jar).

## Self-check

`test/numberguess/GameCheck.java` checks each range's par and stars, that guessing the middle always wins within par (and finds 1 and the top number), that out-of-range and ruled-out guesses are not counted, running out of guesses, that You pick finds every number from 1 to 100 within 7 and catches contradicting answers, and that the saved records keep the fewest guesses, skip bad lines and open ranges in order. From `NumberGuess/`:

```powershell
javac -d ../out/check/NumberGuess src/numberguess/*.java test/numberguess/*.java
java -ea -cp ../out/check/NumberGuess numberguess.GameCheck
```
