# RubiksCube2D

A 2D Rubik's Cube written in Java. The cube is shown as an unfolded net of six 3x3 faces. To rotate, click on a square and then use W, A, S or D to rotate in the correlated direction.

## Controls / usage

- **Click a square**: select that face and square.
- **W / A / S / D** (or the on-screen `w` `a` `s` `d` buttons): turn the row, column or ring through the selected square. The buttons send the same key presses through `java.awt.Robot`.
- **Mix** with **Moves** (default 1000) and **Secs** (default 0.5): scramble with that many random moves spread over that many seconds in total.
- No command-line arguments. The window is 550x700.

Face layout in the net: top blue, left white, front red, right yellow, bottom green, back orange (below bottom).

## Code layout

- `src/rubikscube/Main.java`: entry point; shows the `CubeFrame`.
- `src/rubikscube/CubeFrame.java`: the window; lays out the six faces, the w/a/s/d buttons and the Mix controls.
- `src/rubikscube/Cube.java`: holds the six `Face` objects and refreshes them.
- `src/rubikscube/Face.java`: one face's nine sticker buttons and colors; records which square was clicked.
- `src/rubikscube/Move.java`: applies a W/A/S/D move for the selected face and square by shifting stickers between faces.
- `src/rubikscube/MoveKeyDispatcher.java`: global key dispatcher that turns W/A/S/D presses into `Move`s.
- `src/rubikscube/Scrambler.java`: the Mix feature; picks random faces, squares and keys and sends them as key presses.
- `src/rubikscube/ColorPalette.java`: maps color names to `Color`s.

## Running

From the repo root: `./run.ps1 RubiksCube2D` (or `./run.ps1 RubiksCube2D -Build` to build a jar).

## Known quirks

- Moves are sent as real OS key presses via `Robot`, so they go to whatever window has focus.
- `Scrambler` runs on the Swing event thread and changes the selected face/square between key presses. The key events are only handled after it returns, so the moves likely all use the last selection.
- `Scrambler` picks squares 0 to 7 (`Math.random() * 8.0`) and keys from w/a/s only (`* 3.0`). Square 0 is not a real square and leads to an out-of-bounds index in `Move`; 8, 9 and `d` are never chosen.
- Non-numeric Moves or Secs throw `NumberFormatException`.
- Sticker buttons are fixed at 46 px on a 50 px grid regardless of cell size. `Face.refresh()` recomputes sticker positions but never uses them.
- `ColorPalette.colorFor` checks `red` twice.
