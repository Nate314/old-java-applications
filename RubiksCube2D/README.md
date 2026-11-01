# RubiksCube2D

A 2D Rubik's Cube written in Java. The cube is shown as an unfolded net of six 3x3 faces. To rotate, click on a square and then use W, A, S or D to rotate in the correlated direction.

It is now a puzzle game with 20 levels. Level n starts from a solved cube and mixes it with n moves while you watch (each mixed square is ringed as it turns), then you turn it back. A fully mixed cube is out of reach for most people, so the levels start at one move and add one at a time.

## Controls / usage

- **Click a square**: select that face and square. The selected square shrinks inside a black ring.
- **W / A / S / D** (or the on-screen `w` `a` `s` `d` buttons): turn the row, column or ring through the selected square. Nothing happens until a square is selected. Moves are ignored while a mix or Solve is playing.
- **Levels** (bottom right): the level, its par (the number of mixing moves), your moves and time, and your best on that level. Solving at par or better wins 3 stars, within twice par plus 2 wins 2, otherwise 1. Using a hint caps the level at 1 star. Solving a level unlocks the next one; the total of stars out of 60 is shown under the buttons.
- **Prev** (P) / **Next** (N): play the level before or after (Next only up to the highest unlocked level). **Retry** (R): mix the same level again.
- **Undo** (Z): turns your last move back and takes it off the count.
- **Hint** (H): rings the square of the next move back toward solved and lights its key in green.
- **Solve**: plays the way back to solved, one move at a time. A level solved this way is not counted.
- **Mix** with **Moves** (default 1000) and **Secs** (default 0.5): free play, scrambling with that many random moves (any face, any square, any of the four directions) spread over that many seconds in total. Hint and Solve also work after a Mix.
- Typing in the Moves and Secs fields no longer turns the cube. The title bar shows the level and your move count. Non-numeric Moves or Secs show a message there instead of mixing.
- The best stars and fewest moves per level, and the level played last, are saved to `~/.rubikscube-levels.txt` (lines like `level=4` and `3=3,3` for level 3: 3 stars, 3 moves). The app opens on that level.
- No command-line arguments. The window is 550x700.

Hint and Solve know the way back because every move is recorded as a path of cube positions from the solved cube. A move that returns to a position already on the path (its inverse, or the fourth turn of one slice) cuts the path back there, so the hints never retrace a loop. A level's mix never simply undoes its previous move and always ends that many steps along the path from solved, which is the level's par.

Face layout in the net: top blue, left white, front red, right yellow, bottom green, back orange (below bottom).

## Code layout

- `src/rubikscube/Main.java`: entry point; shows the `CubeFrame`.
- `src/rubikscube/CubeFrame.java`: the window; lays out the six faces, the w/a/s/d buttons, the Mix controls and the level panel; applies and counts moves; plays a level's mix and Solve on a Swing timer; Hint and Undo.
- `src/rubikscube/Cube.java`: holds the six `Face` objects, refreshes them, resets them to solved, takes a snapshot of all 54 stickers and checks whether the cube is solved.
- `src/rubikscube/Face.java`: one face's nine sticker buttons and colors; reports which square was clicked and rings the selected one.
- `src/rubikscube/MoveHistory.java`: the path of positions from the solved cube; applies and records moves and gives the next move back (Hint, Solve).
- `src/rubikscube/LevelRecords.java`: the 20 levels' best stars and moves and the last level, read from and saved to `~/.rubikscube-levels.txt`; the star rule.
- `src/rubikscube/Move.java`: applies a W/A/S/D move for the selected face and square by shifting stickers between faces.
- `src/rubikscube/MoveKeyDispatcher.java`: global key dispatcher that turns W/A/S/D presses into moves and Z/H/R/N/P into the level keys, except while typing in a text field.
- `src/rubikscube/Scrambler.java`: the Mix feature; applies random moves through the `MoveHistory` on a Swing timer. Also makes the random moves for a level's mix.
- `src/rubikscube/ColorPalette.java`: maps color names to `Color`s.

## Running

From the repo root: `./run.ps1 RubiksCube2D` (or `./run.ps1 RubiksCube2D -Build` to build a jar).

## Self-check

`test/rubikscube/MoveCheck.java` checks that every face/square/key move is a permutation of the stickers, is undone by the opposite key and returns to the start after four turns, and that the solved check works. It also checks that following the hints from a 2000-move mix walks back to solved, that an inverse or four turns cut the path, that a level's mix never undoes its previous move, the star rule, and that level records keep the best stars and fewest moves, unlock levels in order and survive a reload. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.MoveCheck
```

The check found that `d` on the yellow and white faces turned the top or bottom face the wrong way (so `a` then `d` did not undo), which is now fixed.
