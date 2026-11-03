# RubiksCube2D

A 2D Rubik's Cube written in Java. The cube was shown as an unfolded net of six 3x3 faces. To rotate, click on a square and then use W, A, S or D to rotate in the correlated direction.

It now opens on a 3D cube drawn in perspective, whose layers turn on screen as each move is made: drag a square to turn its row, column or ring, and drag around the cube to look at its other sides. The flat net is one click (or V) away and works as before.

It is a puzzle game with 20 levels, and it teaches you to solve a fully mixed cube: Teach me shows every turn of the beginner's method, layer by layer, from whatever position the cube is in. Patterns turns the cube into a toy: seven famous patterns (a checkerboard, spots, plus signs, a cube in a cube, the superflip) to watch made or make yourself, one guided turn at a time.

Level n starts from a solved cube and mixes it with n moves while you watch (each mixed square is ringed as it turns), then you turn it back. A fully mixed cube is out of reach for most people, so the levels start at one move and add one at a time.

## Controls / usage

- **3D cube / Flat net** (top right, or V): switch between the two views. Both show the same cube, and every control below works in both. The choice is saved (see below).
- **Drag a square** (3D cube): turns the row, column or ring through that square the way the drag goes, as if the square were pushed with a finger. The move is counted like a key press.
- **Drag around the cube** (3D cube), or the **arrow keys**: turns the whole cube to look at another side. Moves never depend on how the cube is turned.
- **Click a square**: select that face and square. The selected square shrinks inside a black ring (in 3D, inside a white ring).
- **W / A / S / D** (or the on-screen `w` `a` `s` `d` buttons): turn the row, column or ring through the selected square. Nothing happens until a square is selected. Moves are ignored while a mix or Solve is playing.
- **Levels** (bottom right): the level, its par (the number of mixing moves), your moves and time, and your best on that level. Solving at par or better wins 3 stars, within twice par plus 2 wins 2, otherwise 1. Using a hint caps the level at 1 star. Solving a level unlocks the next one; the total of stars out of 60 is shown under the buttons.
- **Prev** (P) / **Next** (N): play the level before or after (Next only up to the highest unlocked level). **Retry** (R): mix the same level again.
- **Undo** (Z): turns your last move back and takes it off the count.
- **Teach me** (top left, or T): coaches you through solving the cube from where it is, the way people learn to (see Teach me below). Click it again (**Stop teaching**) to go back to the message and help.
- **Patterns** (top, or G): the Patterns card in place of the message and help (see Patterns below). **1** to **7** pick a pattern, the same number again plays Show me, **M** is Make it and **Backspace** goes back to the list.
- **Hint** (H): rings the square of the next move back toward solved and lights its key in green. In 3D an arrow on the square shows which way to drag it, and the cube turns by itself to show the square when it is on a side out of view.
- **Solve**: plays the way back to solved, one move at a time. A level solved this way is not counted. After a long Mix it solves the cube layer by layer the Teach me way instead, when that takes fewer moves than turning back every move of the mix (about 150 against up to 1000). Hint gives that way's next move too.
- **Mix** with **Moves** (default 1000) and **Secs** (default 0.5): free play, scrambling with that many random moves (any face, any square, any of the four directions) spread over that many seconds in total. Hint and Solve also work after a Mix.
- Typing in the Moves and Secs fields no longer turns the cube. The title bar shows the level and your move count. Non-numeric Moves or Secs show a message there instead of mixing.
- The best stars and fewest moves per level, the level played last, the view and the patterns made by hand are saved to `~/.rubikscube-levels.txt` (lines like `level=4` and `3=3,3` for level 3: 3 stars, 3 moves, `view=net` once the flat net is picked, and `patterns=spots,checkerboard`). The app opens on that level and in that view.
- No command-line arguments. The window is 550x700.

Hint and Solve know the way back because every move is recorded as a path of cube positions from the solved cube. A move that returns to a position already on the path (its inverse, or the fourth turn of one slice) cuts the path back there, so the hints never retrace a loop. A level's mix never simply undoes its previous move and always ends that many steps along the path from solved, which is the level's par.

Face layout in the net: top blue, left white, front red, right yellow, bottom green, back orange (below bottom).

## Teach me

Teach me solves the cube from any position, a long Mix included, by the beginner's (layer by layer) method, in seven stages: a white cross, the white corners, the middle layer, a yellow cross, the yellow edges matched to their centers, the yellow corners moved to their spots, then twisted. A card in place of the message and help shows the stage (a bar of seven), what the step is for in words, with the colors of the cube's own centers ("Put the white-blue-red corner between the blue and red centers..."), and the step's turns as chips: each in the color of the face to turn, with an arrow for clockwise or back, the turn to make now ringed and those made faded. On the cube the square to drag is ringed with an arrow (and its W/A/S/D key lit, as for Hint), and under the chips the card says the turn in words: "Turn 2 of 8: yellow clockwise."

- At each step the 3D cube turns to show it with yellow on top and the step's front side toward you (standing it up first if yellow was elsewhere), so the card's "with green in front" is the side you see. Looking around between turns is left alone.
- Make the turn shown and the coach moves on to the next. Make any other move (or Undo) and it works the steps out again from where the cube is now, so you can go your own way and come back.
- The white corners, the middle layer and the last layer use the method's usual move patterns, which the card spells out by color: repeat right, top, right back, top back to drop a corner in; eight moves to swing an edge into the middle layer; front, right, top, right back, top back, front back for the yellow cross; right, top, right back, top, right, top twice, right back to move the yellow edges; a three corner swap; and right back, bottom back, right, bottom, repeated, to twist each yellow corner (the white layers come apart while you twist and come back with the last corner). The cross is the shortest way to bring each white edge home. A whole solve takes about 150 quarter turns.
- The faces are the colors of the centers wherever they are, so a cube whose centers a middle-layer move has carried around is solved the same way.
- A cube fewer turns from solved than the method would take (a level's short mix) is not worth the whole method: the card says so, and the arrow turns the mix back one turn at a time. Mix a cube (any number of moves) to learn the method. Coaching a level counts as using a hint (one star at most). Nothing new is saved.

## Patterns



Drawing the cube in 3D showed that the recovered moves were not a real cube's. They shifted strips of stickers between faces by hand, and 144 of the 216 moves (6 faces, 9 squares, 4 keys) left some stickers in a different spot on their new face than a turning layer would: A on the top row put the front's three stickers onto the left face in reverse order. So a corner's three colors came apart, and the net could reach positions no Rubik's Cube can. With the faces folded up the way the net lays them out, no way of turning or flipping each face makes all 216 moves real turns (all 262,144 combinations were tried, and the same with left and right swapped).

The moves are now the quarter turns of a real cube (`CubeGeometry`): the net is folded around the front face, and W/A/S/D turn the one layer through the selected square that carries it up, left, down or right as the net shows it. Each sticker still lands on the same face as before, so every key moves the same row, column or ring the same way, and only the order of stickers within a strip changed. Levels, par, hints, Undo and Solve work as before.

## Code layout

- `src/rubikscube/Main.java`: entry point; shows the `CubeFrame`.
- `src/rubikscube/CubeFrame.java`: the window; lays out the 3D cube or the six faces of the net, the w/a/s/d buttons, the Mix controls and the level panel for the view shown; applies and counts moves; plays a level's mix and Solve on a Swing timer; Hint and Undo.
- `src/rubikscube/CubeView3D.java`: the 3D cube. Teach me can stand it with another side up (a whole-cube turn applied before the view's own). Draws the 26 cubies in perspective with simple lighting, slab by slab along the turning axis from far to near, so a turning layer is drawn correctly in front of or behind the rest; animates each turn (240 ms, quicker when more are waiting, and skipped to the latest position when a Mix runs far ahead); turns the view on a drag or arrow key; maps a click to a square and a drag to the key whose move carries that square most nearly along the drag on screen; draws the selection ring and the hint arrow.
- `src/rubikscube/CubeGeometry.java`: where each sticker of the net sits on the cube (cubie position and the direction its face points), the layer turn of every W/A/S/D move, and applying a turn to the stickers.
- `src/rubikscube/Cube.java`: holds the six `Face` objects, refreshes them, resets them to solved, takes a snapshot of all 54 stickers and checks whether the cube is solved; tells the 3D view about every turn and reset.
- `src/rubikscube/Face.java`: one face's nine sticker buttons and colors; reports which square was clicked and rings the selected one.
- `src/rubikscube/Coach.java`: Teach me's solver: the beginner's method from any position as steps (stage, words, the side to hold in front, quarter turns). The white cross comes from a breadth-first search over where the cross edges can be; the other stages place one piece at a time (the one needing fewest turns first) or try the stage's move pattern from each side until the stage is done.
- `src/rubikscube/CoachPanel.java`: the Teach me card: the stage bar, wrapped words, the turn chips and the turn in words, painted by hand so it wraps the same on the desktop and in the browser.
- `src/rubikscube/CardPaint.java`: what the Teach me and Patterns cards share: their colors, words wrapped by hand and the turn chips.
- `src/rubikscube/Patterns.java`: the seven patterns as face notation, read into quarter turns, and applying turns to a cube's stickers.
- `src/rubikscube/PatternPanel.java`: the Patterns card: the list with a small cube drawn wearing each pattern, and one pattern's card with its chips, status and Show me / Make it buttons.
- `src/rubikscube/MoveHistory.java`: the path of positions from the solved cube; applies and records moves and gives the next move back (Hint, Solve).
- `src/rubikscube/LevelRecords.java`: the 20 levels' best stars and moves, the last level and the patterns made, read from and saved to `~/.rubikscube-levels.txt`; the star rule.
- `src/rubikscube/Move.java`: applies a W/A/S/D move for the selected face and square as a layer turn (see A real cube).
- `src/rubikscube/MoveKeyDispatcher.java`: global key dispatcher that turns W/A/S/D presses into moves, Z/H/R/N/P into the level keys, T into Teach me, G, 1 to 7, M and Backspace into Patterns, V into the view switch and the arrow keys into turning the 3D cube, except while typing in a text field.
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

`test/rubikscube/CoachCheck.java` checks Teach me: that a solved cube needs no steps, that its turns are clockwise the way it says, and that on 3000 cubes mixed with up to 100 random turns (middle layers too, so the centers move) its steps come in stage order, turn only outer layers and end with the cube solved. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.CoachCheck
```

`test/rubikscube/PatternCheck.java` checks Patterns: that the notation reads into the right quarter turns, that every pattern's turns can be made with W/A/S/D (so Make it can point at them), keep the centers and turn back to solved, that the checkerboard, six spots, plus signs and superflip look the way their names say on every face, and that the patterns made are saved, read back and survive beside the level records. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.PatternCheck
```

The check found that `d` on the yellow and white faces turned the top or bottom face the wrong way (so `a` then `d` did not undo), which is now fixed.
