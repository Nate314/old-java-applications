# Minesweeper

A 15x15 Minesweeper game with 18 mines and MIDI sound. Revealing a safe tile plays a note that climbs and falls in pitch; hitting a mine reveals all mines and plays a chord.

## Controls / usage

- **Left click**: reveal a tile. Blank tiles also reveal their neighbors. The first click is always safe: mines are placed after it, away from the clicked tile and its neighbors. Flagged tiles cannot be revealed until unflagged.
- **Right click**: cycle a hidden tile through flag (`f`), question mark (`?`) and back.
- The title bar shows how many mines are left (mines minus flags), and says when you win or hit a mine.
- **Any click after the game ends**: start a new game.
- **Arrow keys**: move the window by 30 px per press.
- **Esc**: exit.
- No command-line arguments. The window is about 3/4 of the screen height square.

## Code layout

- `src/minesweeper/Main.java`: entry point; creates a `GameFrame`.
- `src/minesweeper/GameFrame.java`: the window; builds the grid of tiles, holds game state, and handles reveals, loss, win and the mine counter.
- `src/minesweeper/MineFieldGenerator.java`: places mines at random and computes neighbor counts; also the flood reveal and win check.
- `src/minesweeper/MineTile.java`: a tile button; forwards left clicks to the game and cycles flags.
- `src/minesweeper/SoundPlayer.java`: plays notes and chords through the Java MIDI synthesizer, timed with Swing timers.
- `src/minesweeper/WindowMoveKeyListener.java`: Esc and arrow-key handling.

## Running

From the repo root: `./run.ps1 Minesweeper` (or `./run.ps1 Minesweeper -Build` to build a jar).

## Self-check

`test/minesweeper/MineFieldCheck.java` checks board generation, the flood reveal and the win check. From `Minesweeper/`:

```
javac -d ../out/check/Minesweeper src/minesweeper/*.java test/minesweeper/*.java
java -ea -cp ../out/check/Minesweeper minesweeper.MineFieldCheck
```
