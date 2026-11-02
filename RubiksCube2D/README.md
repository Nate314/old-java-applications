# RubiksCube2D

A 2D Rubik's Cube written in Java. The cube was shown as an unfolded net of six 3x3 faces. To rotate, click on a square and then use W, A, S or D to rotate in the correlated direction.

It now opens on a 3D cube drawn in perspective, whose layers turn on screen as each move is made: drag a square to turn its row, column or ring, and drag around the cube to look at its other sides. The flat net is one click (or V) away and works as before.

It is now a puzzle game with 20 levels. Level n starts from a solved cube and mixes it with n moves while you watch (each mixed square is ringed as it turns), then you turn it back. A fully mixed cube is out of reach for most people, so the levels start at one move and add one at a time.

## Controls / usage

- **3D cube / Flat net** (top right, or V): switch between the two views. Both show the same cube, and every control below works in both. The choice is saved (see below).
- **Drag a square** (3D cube): turns the row, column or ring through that square the way the drag goes, as if the square were pushed with a finger. The move is counted like a key press.
- **Drag around the cube** (3D cube), or the **arrow keys**: turns the whole cube to look at another side. Moves never depend on how the cube is turned.
- **Click a square**: select that face and square. The selected square shrinks inside a black ring (in 3D, inside a white ring).
- **W / A / S / D** (or the on-screen `w` `a` `s` `d` buttons): turn the row, column or ring through the selected square. Nothing happens until a square is selected. Moves are ignored while a mix or Solve is playing.
- **Levels** (bottom right): the level, its par (the number of mixing moves), your moves and time, and your best on that level. Solving at par or better wins 3 stars, within twice par plus 2 wins 2, otherwise 1. Using a hint caps the level at 1 star. Solving a level unlocks the next one; the total of stars out of 60 is shown under the buttons.
- **Prev** (P) / **Next** (N): play the level before or after (Next only up to the highest unlocked level). **Retry** (R): mix the same level again.
- **Undo** (Z): turns your last move back and takes it off the count.
- **Hint** (H): rings the square of the next move back toward solved and lights its key in green. In 3D an arrow on the square shows which way to drag it, and the cube turns by itself to show the square when it is on a side out of view.
- **Solve**: plays the way back to solved, one move at a time. A level solved this way is not counted.
- **Mix** with **Moves** (default 1000) and **Secs** (default 0.5): free play, scrambling with that many random moves (any face, any square, any of the four directions) spread over that many seconds in total. Hint and Solve also work after a Mix.
- Typing in the Moves and Secs fields no longer turns the cube. The title bar shows the level and your move count. Non-numeric Moves or Secs show a message there instead of mixing.
- The best stars and fewest moves per level, the level played last and the view are saved to `~/.rubikscube-levels.txt` (lines like `level=4` and `3=3,3` for level 3: 3 stars, 3 moves, and `view=net` once the flat net is picked). The app opens on that level and in that view.
- No command-line arguments. The window is 550x700.

Hint and Solve know the way back because every move is recorded as a path of cube positions from the solved cube. A move that returns to a position already on the path (its inverse, or the fourth turn of one slice) cuts the path back there, so the hints never retrace a loop. A level's mix never simply undoes its previous move and always ends that many steps along the path from solved, which is the level's par.

Face layout in the net: top blue, left white, front red, right yellow, bottom green, back orange (below bottom).

## A real cube

Drawing the cube in 3D showed that the recovered moves were not a real cube's. They shifted strips of stickers between faces by hand, and 144 of the 216 moves (6 faces, 9 squares, 4 keys) left some stickers in a different spot on their new face than a turning layer would: A on the top row put the front's three stickers onto the left face in reverse order. So a corner's three colors came apart, and the net could reach positions no Rubik's Cube can. With the faces folded up the way the net lays them out, no way of turning or flipping each face makes all 216 moves real turns (all 262,144 combinations were tried, and the same with left and right swapped).

The moves are now the quarter turns of a real cube (`CubeGeometry`): the net is folded around the front face, and W/A/S/D turn the one layer through the selected square that carries it up, left, down or right as the net shows it. Each sticker still lands on the same face as before, so every key moves the same row, column or ring the same way, and only the order of stickers within a strip changed. Levels, par, hints, Undo and Solve work as before.

## Code layout

- `src/rubikscube/Main.java`: entry point; shows the `CubeFrame`.
- `src/rubikscube/CubeFrame.java`: the window; lays out the 3D cube or the six faces of the net, the w/a/s/d buttons, the Mix controls and the level panel for the view shown; applies and counts moves; plays a level's mix and Solve on a Swing timer; Hint and Undo.
- `src/rubikscube/CubeView3D.java`: the 3D cube. Draws the 26 cubies in perspective with simple lighting, slab by slab along the turning axis from far to near, so a turning layer is drawn correctly in front of or behind the rest; animates each turn (240 ms, quicker when more are waiting, and skipped to the latest position when a Mix runs far ahead); turns the view on a drag or arrow key; maps a click to a square and a drag to the key whose move carries that square most nearly along the drag on screen; draws the selection ring and the hint arrow.
- `src/rubikscube/CubeGeometry.java`: where each sticker of the net sits on the cube (cubie position and the direction its face points), the layer turn of every W/A/S/D move, and applying a turn to the stickers.
- `src/rubikscube/Cube.java`: holds the six `Face` objects, refreshes them, resets them to solved, takes a snapshot of all 54 stickers and checks whether the cube is solved; tells the 3D view about every turn and reset.
- `src/rubikscube/Face.java`: one face's nine sticker buttons and colors; reports which square was clicked and rings the selected one.
- `src/rubikscube/MoveHistory.java`: the path of positions from the solved cube; applies and records moves and gives the next move back (Hint, Solve).
- `src/rubikscube/LevelRecords.java`: the 20 levels' best stars and moves and the last level, read from and saved to `~/.rubikscube-levels.txt`; the star rule.
- `src/rubikscube/Move.java`: applies a W/A/S/D move for the selected face and square as a layer turn (see A real cube).
- `src/rubikscube/MoveKeyDispatcher.java`: global key dispatcher that turns W/A/S/D presses into moves, Z/H/R/N/P into the level keys, V into the view switch and the arrow keys into turning the 3D cube, except while typing in a text field.
- `src/rubikscube/Scrambler.java`: the Mix feature; applies random moves through the `MoveHistory` on a Swing timer. Also makes the random moves for a level's mix.
- `src/rubikscube/ColorPalette.java`: maps color names to `Color`s.

## Running

From the repo root: `./run.ps1 RubiksCube2D` (or `./run.ps1 RubiksCube2D -Build` to build a jar).

## Self-check

`test/rubikscube/MoveCheck.java` checks that every face/square/key move is a permutation of the stickers, is undone by the opposite key and returns to the start after four turns, that after 500 random moves the stickers that shared a cubie still share one (the recovered moves fail this), that each key carries its square the way it points in the net, and that the solved check works. It also checks that following the hints from a 2000-move mix walks back to solved, that an inverse or four turns cut the path, that a level's mix never undoes its previous move, the star rule, and that level records keep the best stars and fewest moves, unlock levels in order and survive a reload. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.MoveCheck
```

The check found that `d` on the yellow and white faces turned the top or bottom face the wrong way (so `a` then `d` did not undo), which is now fixed.
