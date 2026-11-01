# RubiksCube2D

A 2D Rubik's Cube written in Java. The cube is shown as an unfolded net of six 3x3 faces. To rotate, click on a square and then use W, A, S or D to rotate in the correlated direction.

## Controls / usage

- **Click a square**: select that face and square.
- **W / A / S / D** (or the on-screen `w` `a` `s` `d` buttons): turn the row, column or ring through the selected square. Nothing happens until a square is selected.
- **Mix** with **Moves** (default 1000) and **Secs** (default 0.5): scramble with that many random moves (any face, any square, any of the four directions) spread over that many seconds in total. Moves are ignored while mixing.
- The title bar counts your moves since the last Mix and shows when the cube is solved. Non-numeric Moves or Secs show a message there instead of mixing.
- No command-line arguments. The window is 550x700.

Face layout in the net: top blue, left white, front red, right yellow, bottom green, back orange (below bottom).

## Code layout

- `src/rubikscube/Main.java`: entry point; shows the `CubeFrame`.
- `src/rubikscube/CubeFrame.java`: the window; lays out the six faces, the w/a/s/d buttons and the Mix controls; applies and counts moves.
- `src/rubikscube/Cube.java`: holds the six `Face` objects, refreshes them and checks whether the cube is solved.
- `src/rubikscube/Face.java`: one face's nine sticker buttons and colors; records which square was clicked.
- `src/rubikscube/Move.java`: applies a W/A/S/D move for the selected face and square by shifting stickers between faces.
- `src/rubikscube/MoveKeyDispatcher.java`: global key dispatcher that turns W/A/S/D presses into `Move`s.
- `src/rubikscube/Scrambler.java`: the Mix feature; applies random moves directly to the cube on a Swing timer.
- `src/rubikscube/ColorPalette.java`: maps color names to `Color`s.

## Running

From the repo root: `./run.ps1 RubiksCube2D` (or `./run.ps1 RubiksCube2D -Build` to build a jar).

## Self-check

`test/rubikscube/MoveCheck.java` checks that every face/square/key move is a permutation of the stickers, is undone by the opposite key and returns to the start after four turns, and that the solved check works. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.MoveCheck
```

The check found that `d` on the yellow and white faces turned the top or bottom face the wrong way (so `a` then `d` did not undo), which is now fixed.
