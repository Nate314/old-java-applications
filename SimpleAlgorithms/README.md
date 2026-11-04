# SimpleAlgorithms

The author's hand-written math estimates (a square root, sine and cosine from a Taylor series, a factorial, a power and a division) and a Tower of Hanoi move printer. Original Eclipse source from the author's own files (only `src/` was kept).

It started as a console program that printed a few results. It still prints them, then opens the **Algorithm Lab**: a window that shows each algorithm working, step by step.

## Controls / usage

On start, `main` prints the original output, in order:

- `sqrt(42) ~= ...` (Newton's method)
- `sin(pi/4) ~= ...` and `cos(pi/4) ~= ...` (4 Taylor terms each, about 0.70711)
- `5! = 120` and `5^3 = 125.0`
- each step of the division search, then `4/2 = ...`

Then the Algorithm Lab window opens. Its tabs (click them, or press 1 to 6) are:

- **Sorting race**: six sorts (bubble, selection, insertion, merge, quicksort and heapsort) on the same numbers, side by side. The window opens on it, already racing. Each tick every sort gets the same number of steps: a compare or a write of one value is one step, a swap is two. Bars light orange for a compare and red for a swap or write. When a sort finishes, a green check runs along its bars and it gets its place (1st for the fewest steps; equal runs share a place). At the end the page says which sort won, in how many steps, and how many times as many the slowest took. Each sort shows its compares and writes, and how its work grows for random numbers (n² or n log n).
  - Click a sort to listen to it (Quicksort at first): each tick plays a vibraphone note pitched by the value it just touched, and the green check plays a rising run. M: sound on or off.
  - Space: Play or Pause (Again once all are done, on the same numbers). S or Right: one tick's step, paused. R: the same numbers again. N: new numbers. O: the order (Random, Nearly sorted, Reversed, Few values: four heights). B: 16, 32, 64 or 128 numbers. F: speed (Slow, Normal, Fast, Turbo; the speeds are for 32 numbers and scale with the count, so a race takes about as long).
  - Some things it shows: insertion sort wins on nearly sorted numbers and bubble sort stops early there; quicksort here takes the last value as its pivot, so reversed or few-valued numbers make it slow; selection sort writes little (at most one swap per place) however many compares it makes.
- **Hanoi** (Tower of Hanoi): plays the moves `Hanoi.moves` produces (the same list `Hanoi.solve` prints) on 1 to 8 disks. For each move it shows the move number in binary: the lowest 1 bit names the disk that moves, which is the idea the loop is built on, and the line `solve` prints for it (`Move 2 left`). Click a peg to lift its top disk and another to drop it; a disk can't go on a smaller one. After a hand move the page says how many moves are left at best and what the best next move is, and Step and Play finish the puzzle from there. When solved it says whether that took the fewest possible moves (2^n - 1).
  - Space: Play or Pause. S or Right: one move. R: reset. `-` and `+` (or `=`): fewer or more disks. F: speed (Slow, Normal, Fast, Turbo).
- **Square root**: steps through `MyMath.squareRootSteps`, every guess `estimateSquareRoot` makes. Each guess a is drawn as an a by n / a rectangle, whose area is always n; averaging its two sides makes the next guess, so the rectangle turns into the green square of side sqrt(n). A table lists the guesses with their correct digits in green. Type a number (up to 12 characters) to change n; Backspace deletes.
  - Space: Play or Pause. S or Right: next guess. B or Left: previous guess. N: next example (42, 2, 10, 1000, 0.5, 123456789).
- **Sine & cosine**: plots `estimateSine` or `estimateCosine` with 1 to 10 terms (red) against the real curve (green), with the sums of fewer terms fading behind. Point at the plot (or click it, or use Left and Right) to pick x; the side lists each term at x, their sum, the real value and the error. It opens on sin(pi/4) with 4 terms, the line the original program prints.
  - Space: Play (adds one term at a time, from 1 to 10). `-` / Down and `+` / Up: fewer or more terms. C: sine or cosine. P: back to x = pi/4.
- **Pathfinding**: a 29 by 19 grid you draw on, and five ways to search it for a way from the start (S) to the goal (G): breadth-first, depth-first, Dijkstra, greedy best-first and A*. Steps go up, down, left or right; a step onto open ground costs 1 and onto mud 5. The page plays the picked search cell by cell: pale blue cells were looked at early and deeper blue ones later, yellow cells are seen but not yet looked at, orange is the one being looked at, then the way it found is drawn in red. Beside the grid a table runs all five on the same map and lists how many cells each looked at, and the steps and cost of its way, with the best in each column in green; a line under it says how the picked search works. Drawing reruns every search at once, so the table and the drawn way change while you drag.
  - Drag on the grid to draw walls (or mud: D switches the Walls button to Mud); pressing a cell that already is one rubs it out. Drag S or G to move them. Click a search in the table, or press A, to pick it and watch it from the start.
  - Space: Play or Pause (Again once done). S or Right: one cell, paused. M: a new random maze (with some loops and mud patches). X: the example map. C: clear the walls and mud. F: speed (Slow, Normal, Fast, Turbo).
  - The example it opens on shows the differences: the goal sits behind a cup that opens toward the start, and a band of mud lies between them with a dry way round at the top. Breadth-first finds the fewest steps (36) by wading through the mud (cost 48); Dijkstra and A* walk round it (38 steps, cost 38), A* after looking at 184 cells to Dijkstra's 454; Greedy looks at the fewest cells but walks into the cup and through the mud; depth-first wanders 98 steps.
  - The map and the picked search are saved in `~/.simplealgorithms-paths.txt` (one line per row: `.` open, `#` wall, `~` mud, `S`, `G`), so a drawn map is still there next time.
- **Printout**: the lines `main` printed at start, as a terminal shows them. H adds what `Hanoi.solve(3, true)` prints (`main` has a commented-out call to `solve(5, true)`).

On the Square root tab, the digit keys type the number, so switch tabs there by clicking.

## Code layout

- `src/simplealgorithms/Main.java`: entry point; prints the results above (`printout()` builds the lines) and opens the Algorithm Lab.
- `src/simplealgorithms/MyMath.java`: square root (`squareRootSteps` lists every guess), sine and cosine (one shared Taylor series), power, factorial, and add/subtract/multiply/divide built from simpler operations. `divide` can hand its steps to a callback instead of printing them.
- `src/simplealgorithms/Hanoi.java`: `moves` is the original move loop, collecting `Move`s (disk, right or left); `solve` prints them (`Move <disk> left/right`).
- `src/simplealgorithms/HanoiTower.java`: the pegs, legal moves, and the shortest solution from any position (`nextMove`, `movesLeft`).
- `src/simplealgorithms/Sorts.java`: the six sorts and the four orders of numbers. Each sort runs on its own copy and records every compare, swap and write as a step.
- `src/simplealgorithms/SortRace.java`: plays the recorded steps of every sort against one step clock, and works out the places.
- `src/simplealgorithms/SortPage.java` and `SortSound.java`: the Sorting race tab, and its notes on the MIDI synthesizer (opened on a background thread; silent without one).
- `src/simplealgorithms/Grid.java`: the Pathfinding map (open, wall and mud cells, start and goal), the example map, the maze maker, and saving.
- `src/simplealgorithms/Paths.java`: the five searches. Each records the order it looked at cells in and when it first saw each one, so the page can replay it.
- `src/simplealgorithms/PathPage.java`: the Pathfinding tab.
- `src/simplealgorithms/LabPanel.java`: the window's graph-paper panel: tabs, the button bar, and routing of clicks, drags, keys and a 30 ms tick to the page showing.
- `src/simplealgorithms/Page.java`: base class of the six pages, with the shared colors, fonts and number formatting.
- `src/simplealgorithms/HanoiPage.java`, `RootPage.java`, `TaylorPage.java`, `PrintoutPage.java`: the other four tabs.
- `src/simplealgorithms/Conversions.java`: empty class.

## Running

From the repo root: `./run.ps1 SimpleAlgorithms` (or `./run.ps1 SimpleAlgorithms -Build` to build a jar).

## Self-check

`test/simplealgorithms/MyMathCheck.java` checks the sine, cosine, square root, factorial, power and division results against `java.lang.Math`. `test/simplealgorithms/HanoiCheck.java` plays `Hanoi.moves` for 1 to 10 disks and checks every move is legal, matches the shortest solution and ends with the tower on the right peg, and that from scrambled positions the solver finishes in exactly `movesLeft` moves. `test/simplealgorithms/PathCheck.java` runs the five searches on 300 random maps and checks each finds a way exactly when there is one, never looks at a wall or a cell twice, and that its way is a real one whose cost adds up; that breadth-first finds the fewest steps and Dijkstra and A* the cheapest way (against a slow search that tries every step until nothing improves), with A* looking at no more cells than Dijkstra; the lessons of the example map above; that 100 random mazes all have a way through; and that a saved map loads back. `test/simplealgorithms/SortCheck.java` plays back each sort's recorded steps for every order and several counts and checks they sort the numbers, checks the lessons above (insertion sort wins on nearly sorted numbers, merge sort beats bubble sort by far on random ones, quicksort is slow on reversed ones), and that the race gives every sort the same steps and places the cheapest first. From `SimpleAlgorithms/`:

```
javac -d ../out/check/SimpleAlgorithms src/simplealgorithms/*.java test/simplealgorithms/*.java
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.MyMathCheck
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.HanoiCheck
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.SortCheck
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.PathCheck
```

## Fixed bugs

- `Hanoi.solve` always printed `Move 0 right` for the smallest disk. That only works for an even number of disks: with an odd number the other lines then name moves that are not allowed (for 3 disks the second line, `Move 1 right`, puts disk 1 on disk 0). The smallest disk now follows the same rule as the other even-numbered disks, so every tower ends on the right peg.
- `estimateSquareRoot` looped until the guess equalled n / guess exactly, which never happens for 0 (the guess halves down to 0, and 0 / 0 is NaN, which equals nothing). It now stops after 100 guesses.

## Known quirks

- `factorial` returns a `long`, so it overflows past 20!. Sine and cosine need at most 19! at the lab's 10 terms.
