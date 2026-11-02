# Minesweeper

A Minesweeper game with three boards, a clock, best times and MIDI sound. Revealing a safe tile plays a note that climbs and falls in pitch; hitting a mine reveals all mines and plays a chord; clearing the board flags every mine and plays a rising run of notes.

## Controls / usage

- **Level buttons** (top of the window): Beginner (9x9, 10 mines), Classic (15x15, 18 mines, the original game's board) and Expert (16x16, 40 mines). The current level is lit. Picking one starts a new game on it. Keys **1**, **2** and **3** do the same, and **F2** starts a new game on the current level.
- **Left click**: reveal a tile. Blank tiles also reveal their neighbors. The first click is always safe: mines are placed after it, away from the clicked tile and its neighbors. Flagged tiles cannot be revealed until unflagged.
- **Right click** (in the browser on a touch screen, a long press): cycle a hidden tile through flag (`f`), question mark (`?`) and back.
- The status line under the board shows how many mines are left (mines minus flags), the time since the first click and the best time on that level. The title bar shows the mines left too. Both say when you win (with your time) or hit a mine, and on a win the status line also says whether the time is a new best.
- **Any click after the game ends**: start a new game.
- **Keyboard play**: Tab into the board (in the browser), or start playing on the first tile (on the desktop, where it has the focus). The **arrow keys** move a cursor from tile to tile (the tile under it is drawn darker), **Space** or **Enter** reveals it, and **F** (or the menu key, or Shift+F10) cycles its flag like a right click. A new game started from the keyboard keeps the cursor where it was. A mouse click hides the cursor until a key is used again.
- **Screen readers** hear each tile's state and place ("hidden, row 3, column 5", "2 mines near, ...", "flag, ...", "mine, ..."), through Swing's accessible names (the Java Access Bridge on the desktop, `aria-label` in the browser). In the browser the title bar's changes are read out too: the mines left after a flag, a mine hit, a win.
- **Shift+arrow keys**: move the window by 30 px per press (plain arrows did this before keyboard play; they still do while the window itself, not a tile, has the focus).
- **Esc**: close the window, like its close button. Run alone that exits; inside NathaNOS it closes only Minesweeper.
- No command-line arguments. The window is about 3/4 of the screen height tall; a new game opens where the last window was.
- The best time on each level and the level played last are stored in `.minesweeper-best.txt` in your home folder, so the next start opens on that level.

## Code layout

- `src/minesweeper/Main.java`: entry point; creates a `GameFrame`.
- `src/minesweeper/GameFrame.java`: the window; builds the level buttons, the grid of tiles and the status line, holds game state, and handles reveals, loss, win, the clock and the mine counter.
- `src/minesweeper/Level.java`: the three board sizes and mine counts.
- `src/minesweeper/BestTimes.java`: reads and writes the best times and the last level.
- `src/minesweeper/MineFieldGenerator.java`: places mines at random and computes neighbor counts; also the flood reveal and win check.
- `src/minesweeper/MineTile.java`: a tile button; forwards left clicks and Space or Enter to the game, cycles flags on a right click or F, moves the keyboard cursor with the arrow keys, and names itself for screen readers.
- `src/minesweeper/SoundPlayer.java`: plays notes and chords through the Java MIDI synthesizer, timed with Swing timers.
- `src/minesweeper/WindowMoveKeyListener.java`: Esc, level keys, F2 and moving the window with the arrow keys (keys a tile has used are skipped).

## Running

From the repo root: `./run.ps1 Minesweeper` (or `./run.ps1 Minesweeper -Build` to build a jar).

## Self-check

`test/minesweeper/MineFieldCheck.java` checks board generation on every level, the flood reveal, the win check and the best times file. From `Minesweeper/`:

```
javac -d ../out/check/Minesweeper src/minesweeper/*.java test/minesweeper/*.java
java -ea -cp ../out/check/Minesweeper minesweeper.MineFieldCheck
```
