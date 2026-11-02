# SimpleAlgorithms

The author's hand-written math estimates (a square root, sine and cosine from a Taylor series, a factorial, a power and a division) and a Tower of Hanoi move printer. Original Eclipse source from the author's own files (only `src/` was kept).

It started as a console program that printed a few results. It still prints them, then opens the **Algorithm Lab**: a window that shows each algorithm working, step by step.

## Controls / usage

On start, `main` prints the original output, in order:

- `sqrt(42) ~= ...` (Newton's method)
- `sin(pi/4) ~= ...` and `cos(pi/4) ~= ...` (4 Taylor terms each, about 0.70711)
- `5! = 120` and `5^3 = 125.0`
- each step of the division search, then `4/2 = ...`

Then the Algorithm Lab window opens. Its tabs (click them, or press 1 to 4) are:

- **Tower of Hanoi**: plays the moves `Hanoi.moves` produces (the same list `Hanoi.solve` prints) on 1 to 8 disks. For each move it shows the move number in binary: the lowest 1 bit names the disk that moves, which is the idea the loop is built on, and the line `solve` prints for it (`Move 2 left`). Click a peg to lift its top disk and another to drop it; a disk can't go on a smaller one. After a hand move the page says how many moves are left at best and what the best next move is, and Step and Play finish the puzzle from there. When solved it says whether that took the fewest possible moves (2^n - 1).
  - Space: Play or Pause. S or Right: one move. R: reset. `-` and `+` (or `=`): fewer or more disks. F: speed (Slow, Normal, Fast, Turbo).
- **Square root**: steps through `MyMath.squareRootSteps`, every guess `estimateSquareRoot` makes. Each guess a is drawn as an a by n / a rectangle, whose area is always n; averaging its two sides makes the next guess, so the rectangle turns into the green square of side sqrt(n). A table lists the guesses with their correct digits in green. Type a number (up to 12 characters) to change n; Backspace deletes.
  - Space: Play or Pause. S or Right: next guess. B or Left: previous guess. N: next example (42, 2, 10, 1000, 0.5, 123456789).
- **Sine & cosine**: plots `estimateSine` or `estimateCosine` with 1 to 10 terms (red) against the real curve (green), with the sums of fewer terms fading behind. Point at the plot (or click it, or use Left and Right) to pick x; the side lists each term at x, their sum, the real value and the error. It opens on sin(pi/4) with 4 terms, the line the original program prints.
  - Space: Play (adds one term at a time, from 1 to 10). `-` / Down and `+` / Up: fewer or more terms. C: sine or cosine. P: back to x = pi/4.
- **Printout**: the lines `main` printed at start, as a terminal shows them. H adds what `Hanoi.solve(3, true)` prints (`main` has a commented-out call to `solve(5, true)`).

On the Square root tab, the digit keys type the number, so switch tabs there by clicking.

## Code layout

- `src/simplealgorithms/Main.java`: entry point; prints the results above (`printout()` builds the lines) and opens the Algorithm Lab.
- `src/simplealgorithms/MyMath.java`: square root (`squareRootSteps` lists every guess), sine and cosine (one shared Taylor series), power, factorial, and add/subtract/multiply/divide built from simpler operations. `divide` can hand its steps to a callback instead of printing them.
- `src/simplealgorithms/Hanoi.java`: `moves` is the original move loop, collecting `Move`s (disk, right or left); `solve` prints them (`Move <disk> left/right`).
- `src/simplealgorithms/HanoiTower.java`: the pegs, legal moves, and the shortest solution from any position (`nextMove`, `movesLeft`).
- `src/simplealgorithms/LabPanel.java`: the window's graph-paper panel: tabs, the button bar, and routing of clicks, keys and a 30 ms tick to the page showing.
- `src/simplealgorithms/Page.java`: base class of the four pages, with the shared colors, fonts and number formatting.
- `src/simplealgorithms/HanoiPage.java`, `RootPage.java`, `TaylorPage.java`, `PrintoutPage.java`: the four tabs.
- `src/simplealgorithms/Conversions.java`: empty class.

## Running

From the repo root: `./run.ps1 SimpleAlgorithms` (or `./run.ps1 SimpleAlgorithms -Build` to build a jar).

## Self-check

`test/simplealgorithms/MyMathCheck.java` checks the sine, cosine, square root, factorial, power and division results against `java.lang.Math`. `test/simplealgorithms/HanoiCheck.java` plays `Hanoi.moves` for 1 to 10 disks and checks every move is legal, matches the shortest solution and ends with the tower on the right peg, and that from scrambled positions the solver finishes in exactly `movesLeft` moves. From `SimpleAlgorithms/`:

```
javac -d ../out/check/SimpleAlgorithms src/simplealgorithms/*.java test/simplealgorithms/*.java
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.MyMathCheck
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.HanoiCheck
```

## Fixed bugs

- `Hanoi.solve` always printed `Move 0 right` for the smallest disk. That only works for an even number of disks: with an odd number the other lines then name moves that are not allowed (for 3 disks the second line, `Move 1 right`, puts disk 1 on disk 0). The smallest disk now follows the same rule as the other even-numbered disks, so every tower ends on the right peg.
- `estimateSquareRoot` looped until the guess equalled n / guess exactly, which never happens for 0 (the guess halves down to 0, and 0 / 0 is NaN, which equals nothing). It now stops after 100 guesses.

## Known quirks

- `factorial` returns a `long`, so it overflows past 20!. Sine and cosine need at most 19! at the lab's 10 terms.
