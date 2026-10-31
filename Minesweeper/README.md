# Minesweeper

A 15x15 Minesweeper game with MIDI sound. Revealing a safe tile plays a note that climbs and falls in pitch; hitting a mine reveals all mines and plays a chord.

## Controls / usage

- **Left click**: reveal a tile. Blank tiles also reveal their neighbors.
- **Right click**: cycle a hidden tile through flag (`f`), question mark (`?`) and back.
- **Any click after the game ends**: start a new game.
- **Arrow keys**: move the window by 30 px per event.
- **Esc**: exit.
- No command-line arguments. The window is about 3/4 of the screen height square.

## Code layout

- `src/minesweeper/Main.java`: entry point; creates a `GameFrame`.
- `src/minesweeper/GameFrame.java`: the window; builds the grid of tiles and holds game state.
- `src/minesweeper/MineFieldGenerator.java`: places mines at random and computes neighbor counts.
- `src/minesweeper/MineTile.java`: a tile button; handles reveal, flood reveal, flags, loss and the end-of-game check.
- `src/minesweeper/SoundPlayer.java`: plays notes and chords through the Java MIDI synthesizer.
- `src/minesweeper/WindowMoveKeyListener.java`: Esc and arrow-key handling.

## Running

From the repo root: `./run.ps1 Minesweeper` (or `./run.ps1 Minesweeper -Build` to build a jar).

## Known quirks

- 18 mine positions are drawn at random with no duplicate check, so a board can have fewer than 18 mines.
- The mine-placement loop stops one cell short (`rows * columns - 1`), so the bottom-right cell is never a mine and its value is left `null`. If a mine sits next to it, `Integer.parseInt(null)` throws and `GameFrame` hides itself and opens a new game.
- The flood reveal's neighbor list covers indexes 0 to 7 of a 3x3 block, so it includes the tile itself and skips the bottom-right diagonal. Edge masking also clears the matching row/column pair, so some edge neighbors are skipped too.
- The game counts as over when every tile has non-empty text, and flags count, so flagging every tile ends the game. There is no win message.
- The key handler runs on key pressed and key released, so one arrow press moves the window twice (60 px).
- `SoundPlayer.playNote` and `playChord` ignore their duration/delay/pressure/bend arguments and use hard-coded values. `noteNumbers` is filled but never read.
- `MineFieldGenerator` prints the row count to stdout once per mine. `MineTile.unused` is never read.
- The window title is "AWT Minesweeper" although it uses Swing.
