# Minesweeper

A Minesweeper game with three boards, a clock, best times and MIDI sound. Revealing a safe tile plays a note that climbs and falls in pitch; hitting a mine reveals all mines and plays a chord; clearing the board flags every mine and plays a rising run of notes.

## Look

The tiles draw themselves (`MineTile.paintComponent`) instead of being plain colored buttons with a letter on them:

- Hidden tiles are raised squares of grass in two checkered greens, with a light top edge and a shadow under them. The one under the mouse lights up, and it sinks while the button is held.
- Revealed tiles are flat sand, also checkered, so an opened area reads as one patch of ground. Numbers are drawn in the classic colors: 1 blue, 2 green, 3 red, 4 purple, then orange, teal, gray and silver.
- A flag is a red pennant on a pole, planted with a small bounce; a question mark is white on the grass.
- Mines are black with eight spikes and a shine. The one you hit sits on red.
- Openings ripple: each tile pops off (shrinks and fades) 22 ms after the ring of tiles inside it, so a big opening spreads out from the click. A lost game bursts the mines in outward from the one hit, keeps the flags that were right and crosses out the wrong ones; a won game plants a flag on every mine, outward from the last click.
- The keyboard cursor is a blue ring on the tile.

The original colors (bright green hidden tiles, blue revealed ones with a white number, red `b` for a mine, cyan `f` for a flag) can still be played on the site's "as recovered" page (see Then and now in `web/README.md`).

## Controls / usage

- **Level buttons** (top of the window): Beginner (9x9, 10 mines), Classic (15x15, 18 mines, the original game's board) and Expert (16x16, 40 mines). The current level is lit. Picking one starts a new game on it. Keys **1**, **2** and **3** do the same, and **F2** starts a new game on the current level.
- **Left click**: reveal a tile. Blank tiles also reveal their neighbors.
- **Left click on a number** (or Space on it): once as many of its neighbors are flagged as the number says, its other hidden neighbors are revealed at once ("chording"). A wrong flag means one of them is a mine, so that loses the game, as in the classic game. While the flags do not add up yet, the hidden neighbors glow briefly instead, to show what it would open. The first click is always safe: mines are placed after it, away from the clicked tile and its neighbors. Flagged tiles cannot be revealed until unflagged.
- **Right click** (in the browser on a touch screen, a long press): cycle a hidden tile through flag, question mark and back.
- The status line under the board shows how many mines are left (mines minus flags), the time since the first click and the best time on that level. The title bar shows the mines left too. Both say when you win (with your time) or hit a mine, and on a win the status line also says whether the time is a new best.
- **Any click after the game ends**: start a new game.
- **Keyboard play**: Tab into the board (in the browser), or start playing on the first tile (on the desktop, where it has the focus). The **arrow keys** move a cursor from tile to tile (the tile under it has a blue ring), **Space** or **Enter** reveals it, and **F** (or the menu key, or Shift+F10) cycles its flag like a right click. A new game started from the keyboard keeps the cursor where it was. A mouse click hides the cursor until a key is used again.
- **Screen readers** hear each tile's state and place ("hidden, row 3, column 5", "2 mines near, ...", "flag, ...", "mine, ...", and after a loss "mine, hit, ..." and "wrong flag, ..."), through Swing's accessible names (the Java Access Bridge on the desktop, `aria-label` in the browser). In the browser the title bar's changes are read out too: the mines left after a flag, a mine hit, a win.
- **Shift+arrow keys**: move the window by 30 px per press (plain arrows did this before keyboard play; they still do while the window itself, not a tile, has the focus).
- **Esc**: close the window, like its close button. Run alone that exits; inside NathaNOS it closes only Minesweeper.
- No command-line arguments. The window is about 3/4 of the screen height tall; a new game opens where the last window was.
- The best time on each level and the level played last are stored in `.minesweeper-best.txt` in your home folder, so the next start opens on that level.

## Code layout

- `src/minesweeper/Main.java`: entry point; creates a `GameFrame`.
- `src/minesweeper/GameFrame.java`: the window; builds the level buttons, the grid of tiles and the status line, holds game state, and handles reveals, loss, win, the clock and the mine counter.
- `src/minesweeper/Level.java`: the three board sizes and mine counts.
- `src/minesweeper/BestTimes.java`: reads and writes the best times and the last level.
- `src/minesweeper/MineFieldGenerator.java`: places mines at random and computes neighbor counts; also the flood reveal, opening around a number, and the win check.
- `src/minesweeper/MineTile.java`: a tile button that paints itself and its animations; forwards left clicks and Space or Enter to the game, cycles flags on a right click or F, moves the keyboard cursor with the arrow keys, and names itself for screen readers. `GameFrame` repaints the tiles that are animating every 16 ms until they are done.
- `src/minesweeper/SoundPlayer.java`: plays notes and chords through the Java MIDI synthesizer, timed with Swing timers.
- `src/minesweeper/WindowMoveKeyListener.java`: Esc, level keys, F2 and moving the window with the arrow keys (keys a tile has used are skipped).

## Running

From the repo root: `./run.ps1 Minesweeper` (or `./run.ps1 Minesweeper -Build` to build a jar).

## Self-check

`test/minesweeper/MineFieldCheck.java` checks board generation on every level, the flood reveal, opening around a number (including a wrong flag), the win check and the best times file. From `Minesweeper/`:

```
javac -d ../out/check/Minesweeper src/minesweeper/*.java test/minesweeper/*.java
java -ea -cp ../out/check/Minesweeper minesweeper.MineFieldCheck
```
