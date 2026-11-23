# RubiksCube2D

A 2D Rubik's Cube written in Java. The cube was shown as an unfolded net of six 3x3 faces. To rotate, click on a square and then use W, A, S or D to rotate in the correlated direction.

It now opens on a 3D cube drawn in perspective, whose layers turn on screen as each move is made: drag a square to turn its row, column or ring, and drag around the cube to look at its other sides. The flat net is one click (or V) away and works as before.

It is a puzzle game with 20 levels, and it teaches you to solve a fully mixed cube: Teach me shows every turn of the beginner's method, layer by layer, from whatever position the cube is in. Patterns turns the cube into a toy: seven famous patterns (a checkerboard, spots, plus signs, a cube in a cube, the superflip) to watch made or make yourself, one guided turn at a time.

Level n starts from a solved cube and mixes it with n moves while you watch (each mixed square is ringed as it turns), then you turn it back. A fully mixed cube is out of reach for most people, so the levels start at one move and add one at a time.

## Controls / usage

- **3D cube / Flat net** (top right, or V): switch between the two views. Both show the same cube, and every control below works in both. The choice is saved (see below).
- **Drag a square** (3D cube): turns the row, column or ring through that square the way the drag goes, as if the square were pushed with a finger. The layer follows the pointer while you hold it, so you see which layer you have before it counts: let go past half a quarter turn (or flick it) and the turn finishes and is counted like a key press; let go short of that and it springs back, with no move made. Drag back past the start to turn it the other way. While a mix or Solve is playing, or a turn is still showing, the drag makes its move at once as before.
- **Show me how** (level 1, 3D cube, until level 1 has been solved once): when the mix ends, a fingertip on the cube pulls the layer that turns it back part way (by the square of that layer facing you most squarely), the finger riding on the square, and lets it spring back, over and over, so a first-time player sees the drag before trying it. It stops at the first press on the cube (or a move, a card or the flat net). It is not a hint: the stars are not capped. With reduced motion the hint arrow stands still on that square instead.
- **Turn arrows** (3D cube): every turn the app makes, or a W/A/S/D key or Undo makes, draws a curved white arrow over the squares of the layer that turns, its head running ahead with the layer, and it stays a moment after the turn and fades, so a level's mix, a Solve or a pattern's Show me can be followed turn by turn. A turn made by dragging has none (the hand made it), nor do turns that come less than a tenth of a second apart (a long Mix), where arrows would only flicker.
- **Drag around the cube** (3D cube), or the **arrow keys**: turns the whole cube to look at another side. Moves never depend on how the cube is turned.
- **Mirrors** (3D cube): the sides turned away from you are shown again at the left and right edges of the view, so the whole cube can be read without turning it (see Mirrors below). **Click a mirror** to turn that side round to you. **X** turns the mirrors off or on; the choice is saved.
- **Click a square**: select that face and square. The selected square shrinks inside a black ring (in 3D, inside a white ring).
- **W / A / S / D** (or the on-screen `w` `a` `s` `d` buttons): turn the row, column or ring through the selected square. Nothing happens until a square is selected. Moves are ignored while a mix or Solve is playing.
- **Levels** (bottom right): the level, its par (the number of mixing moves), your moves and time, and your best on that level. Solving at par or better wins 3 stars, within twice par plus 2 wins 2, otherwise 1. Using a hint caps the level at 1 star. Solving a level unlocks the next one; the total of stars out of 60 is shown under the buttons. The moment a level is solved, the 3D cube takes a slow lap while the stars won pop in one after another on a plate over it, with "Level 3 solved!" under them; free play shows "Solved!" alone. It fades after about 4 seconds, and a press on the cube or the next move ends it at once. With reduced motion the cube stays still and the stars show at once.
- **The round strip** (3D cube, levels only): a pill at the foot of the cube tells the level's round as it goes. While the mix plays it says "Watch the mix" and fills a pip for each mixing move; then it says "Your turn" and fills a pip for each move you make, with the stars a solve now would win beside them: three while you are within par, past par a +1, +2 in amber and two stars, and once past twice par plus 2 the count turns red and one star is left (a Hint caps them at one). Undo takes a pip back off. Once you solve the level it becomes a blue **Next: level 4** button (click it, or N) that starts the next level's mix, or says Solved on the last level or one not yet unlocked past. It is not shown after Solve, in free play, Patterns, My cube or the Timer. The cube keeps the strip's room free under it whether or not it shows, so it never jumps.
- **Prev** (P) / **Next** (N): play the level before or after (Next only up to the highest unlocked level). **Retry** (R): mix the same level again.
- **Undo** (Z): turns your last move back and takes it off the count.
- **Teach me** (top left, or T): coaches you through solving the cube from where it is, the way people learn to (see Teach me below). Click it again (**Stop teaching**) to go back to the message and help.
- **Patterns** (top, or G): the Patterns card in place of the message and help (see Patterns below). **1** to **7** pick a pattern, the same number again plays Show me, **M** is Make it and **Backspace** goes back to the list.
- **My cube** (top, or C): paint the cube to match a real one in your hands, then Teach me solves that one (see My cube below). **1** to **6** pick the color to paint with while it is up; C again (or Cancel) puts the cube back as it was.
- **Timer** (top, or K): a speed timer for solving a real cube, the way cubers time themselves (see Timer below). K or Timer again closes it.
- **Hint** (H): rings the square of the next move back toward solved and lights its key in green. In 3D an arrow on the square shows which way to drag it, and the cube turns by itself to show the square when it is on a side out of view.
- **Solve**: plays the way back to solved, one move at a time. A level solved this way is not counted. After a long Mix it solves the cube layer by layer the Teach me way instead, when that takes fewer moves than turning back every move of the mix (about 150 against up to 1000). Hint gives that way's next move too.
- **Mix** with **Moves** (default 1000) and **Secs** (default 0.5): free play, scrambling with that many random moves (any face, any square, any of the four directions) spread over that many seconds in total. Hint and Solve also work after a Mix.
- Typing in the Moves and Secs fields no longer turns the cube. The title bar shows the level and your move count. Non-numeric Moves or Secs show a message there instead of mixing.
- The best stars and fewest moves per level, the level played last, the view and the patterns made by hand are saved to `~/.rubikscube-levels.txt` (lines like `level=4` and `3=3,3` for level 3: 3 stars, 3 moves, `view=net` once the flat net is picked, `mirrors=off` while the mirrors are off, and `patterns=spots,checkerboard`, and `times=` with the Timer's latest 100 times in milliseconds, oldest first). The app opens on that level and in that view.
- No command-line arguments. The window is 550x700. On a screen narrower than 600 px (a phone held upright, in the browser) it is as wide as the screen instead of being scaled down: the row of buttons spans the top, the cube takes the width and whatever height the screen leaves (at least 300 px), the message or card sits at the left under it, and the level panel, Mix and the w/a/s/d keys stack at the right. The flat net is drawn in the cube's place there, with the same controls under it. The layout is chosen at start and doesn't follow a rotation.
- On a landscape screen shorter than 740 px (a laptop's browser, where the 550x700 window used to be scaled down to fit), the window fits the screen instead: the cube takes the full height at the left, half as big again as it was at 900x600, and the same controls as on a phone stand in a column at its right (the card or message beside the level panel, Mix and the w/a/s/d keys). The flat net is drawn in the cube's place there too. A taller screen keeps the 550x700 window.
- The 3D cube's squares are glossy: each is lit brighter at its upper left corner and darker toward the lower right, with a sheen that flares into a glint as a side turns to catch the light, so turning the cube or a layer reads as a solid object. Its shadow on the floor widens as a corner turns toward you and narrows face on.

Hint and Solve know the way back because every move is recorded as a path of cube positions from the solved cube. A move that returns to a position already on the path (its inverse, or the fourth turn of one slice) cuts the path back there, so the hints never retrace a loop. A level's mix never simply undoes its previous move and always ends that many steps along the path from solved, which is the level's par.

Face layout in the net: top blue, left white, front red, right yellow, bottom green, back orange (below bottom).

## Mirrors

A cube drawn in 3D shows three of its six sides. Real cubers turn the cube in their hands to check the rest; on screen that meant dragging the cube round between moves. Now the three hidden sides are shown too, as small mirrors beside the cube.

- Each side turned away from you is drawn a little way beyond the cube, smaller, as a mirror standing behind that side would show it, so it reads back to front, in perspective. Then it is moved out to the left or right edge of the view: a side behind and to the left stands at the left, one behind and to the right at the right, and the side underneath (or on top, when you look from below) at the bottom (or top) of the right edge, under the row of buttons.
- A mirror fades in as its side turns away and is gone once the side faces you. When you turn the cube so that a hidden side moves from one edge to the other, its mirror glides across behind the cube.
- A click (or tap) on a mirror turns the cube to show that side, as Hint does for a square out of view.
- The mirrors show where every square is after the last move; while a layer is turning they wait for it to finish.
- X (only in the 3D cube) turns them off and on, saved as `mirrors=off` in `~/.rubikscube-levels.txt` while they are off. The flat net has no hidden sides and no mirrors.

## Teach me

Teach me solves the cube from any position, a long Mix included, by the beginner's (layer by layer) method, in seven stages: a white cross, the white corners, the middle layer, a yellow cross, the yellow edges matched to their centers, the yellow corners moved to their spots, then twisted. A card in place of the message and help shows the stage (a bar of seven), what the step is for in words, with the colors of the cube's own centers ("Put the white-blue-red corner between the blue and red centers..."), and the step's turns as chips: each in the color of the face to turn, with an arrow for clockwise or back, the turn to make now ringed and those made faded. On the cube the square to drag is ringed with an arrow (and its W/A/S/D key lit, as for Hint), and under the chips the card says the turn in words: "Turn 2 of 8: yellow clockwise."

- At each step the 3D cube turns to show it with yellow on top and the step's front side toward you (standing it up first if yellow was elsewhere), so the card's "with green in front" is the side you see. Looking around between turns is left alone.
- Make the turn shown and the coach moves on to the next. Make any other move (or Undo) and it works the steps out again from where the cube is now, so you can go your own way and come back.
- The white corners, the middle layer and the last layer use the method's usual move patterns, which the card spells out by color: repeat right, top, right back, top back to drop a corner in; eight moves to swing an edge into the middle layer; front, right, top, right back, top back, front back for the yellow cross; right, top, right back, top, right, top twice, right back to move the yellow edges; a three corner swap; and right back, bottom back, right, bottom, repeated, to twist each yellow corner (the white layers come apart while you twist and come back with the last corner). The cross is the shortest way to bring each white edge home. A whole solve takes about 150 quarter turns.
- The faces are the colors of the centers wherever they are, so a cube whose centers a middle-layer move has carried around is solved the same way.
- A cube fewer turns from solved than the method would take (a level's short mix) is not worth the whole method: the card says so, and the arrow turns the mix back one turn at a time. Mix a cube (any number of moves) to learn the method. Coaching a level counts as using a hint (one star at most). Nothing new is saved.

## My cube

For someone holding a mixed-up real cube: copy it into the app and be taken through solving it.

- My cube turns every square gray except the six centers. Hold the real cube with the blue center on top and the red center facing you (the colors are the usual ones: white opposite yellow, blue opposite green, red opposite orange), pick a color on the card (or press 1 to 6: blue, red, yellow, green, orange, white) and click each square to match. In 3D, drag around the cube to reach its other sides; in the flat net the back face is the one at the bottom. A click on a center only says that the centers stay as they are.
- The card counts the squares painted (of 48) and, under each color, how many of its nine are left to paint (a tick when all nine are, in red below zero).
- Once no square is gray, the card checks that the painting is a cube that turning could make (`RealCube`): nine of each color, every corner and edge a real piece's colors, each piece once, no corner with its colors in mirror order, the corners' twists adding up to whole turns, an even number of flipped edges, and as many swaps among the corners as among the edges (both odd or both even). If not, it says what is wrong in red ("A cube has 9 squares of each color. This one has 10 white but 8 yellow.", "Two edges are blue and red, so one of them has a wrong square.", "One corner is twisted in its place. Turning never does that...").
- **Teach me** on the card makes the painting the cube and turns Teach me on (see above). The level panel says Your cube; Hint and Solve take the coach's way, since there are no recorded moves to turn back. Make each turn the card shows on the real cube too, and both end solved. **Cancel** puts back the cube from before painting. While painting, the level and move controls wait. Nothing new is saved.

## Timer

For timing your solves of a real cube, like the timers cubers use.

- The card gives a scramble of 20 face turns in the usual notation (U D F B R L for the up, down, front, back, right and left faces, ' for a turn back, 2 for twice), never the same face twice in a row nor three turns on one axis. Hold a solved cube with the blue center on top and red facing you and make the turns. The cube on screen is scrambled the same way from solved, so you can check yours against it, or solve it there when there is no cube in your hands (Hint, Solve and Teach me work on it as after a Mix).
- Hold **Space** (or press and hold the time on the card, on a touch screen): the time turns red, then green after 0.3 s. Let go to start. Press Space again (any key, or a tap on the card) to stop. Letting go before it is green does not start it, and a key held down counts once.
- Each time is kept and a new scramble comes up at once. The card shows the best time, the average of the last 5 and of the last 12 with the best and worst of them left out (Ao5, Ao12, as cubers count them), and the last five times. **Delete last** drops the latest time (a start by mistake); **New scramble** skips to another one.
- Times are cut to hundredths, not rounded (12.349 s shows as 12.34). There is no 15 second inspection countdown and no +2 or DNF marks.

## Patterns



Drawing the cube in 3D showed that the recovered moves were not a real cube's. They shifted strips of stickers between faces by hand, and 144 of the 216 moves (6 faces, 9 squares, 4 keys) left some stickers in a different spot on their new face than a turning layer would: A on the top row put the front's three stickers onto the left face in reverse order. So a corner's three colors came apart, and the net could reach positions no Rubik's Cube can. With the faces folded up the way the net lays them out, no way of turning or flipping each face makes all 216 moves real turns (all 262,144 combinations were tried, and the same with left and right swapped).

The moves are now the quarter turns of a real cube (`CubeGeometry`): the net is folded around the front face, and W/A/S/D turn the one layer through the selected square that carries it up, left, down or right as the net shows it. Each sticker still lands on the same face as before, so every key moves the same row, column or ring the same way, and only the order of stickers within a strip changed. Levels, par, hints, Undo and Solve work as before.

## Code layout

- `src/rubikscube/Main.java`: entry point; shows the `CubeFrame`.
- `src/rubikscube/CubeFrame.java`: the window; lays out the 3D cube or the six faces of the net, the w/a/s/d buttons, the Mix controls and the level panel for the view shown; applies and counts moves; plays a level's mix and Solve on a Swing timer; Hint and Undo.
- `src/rubikscube/CubeView3D.java`: the 3D cube. Teach me can stand it with another side up (a whole-cube turn applied before the view's own). Draws the 26 cubies in perspective with simple lighting, slab by slab along the turning axis from far to near, so a turning layer is drawn correctly in front of or behind the rest; animates each turn (240 ms, quicker when more are waiting, and skipped to the latest position when a Mix runs far ahead); turns the view on a drag or arrow key; maps a click to a square and a drag to the key whose move carries that square most nearly along the drag on screen, then turns that layer with the pointer until it is let go (made a move past half way, sprung back short of it); draws the selection ring, the hint arrow and the turn arrow round a turning layer; draws the mirrors of the hidden sides and turns a clicked one to the front. Plays Show me how: a fingertip pulling a layer part way and letting it spring back.
- `src/rubikscube/CubeGeometry.java`: where each sticker of the net sits on the cube (cubie position and the direction its face points), the layer turn of every W/A/S/D move, and applying a turn to the stickers.
- `src/rubikscube/Cube.java`: holds the six `Face` objects, refreshes them, resets them to solved, takes a snapshot of all 54 stickers and checks whether the cube is solved; tells the 3D view about every turn and reset.
- `src/rubikscube/Face.java`: one face's nine sticker buttons and colors; reports which square was clicked and rings the selected one.
- `src/rubikscube/Coach.java`: Teach me's solver: the beginner's method from any position as steps (stage, words, the side to hold in front, quarter turns). The white cross comes from a breadth-first search over where the cross edges can be; the other stages place one piece at a time (the one needing fewest turns first) or try the stage's move pattern from each side until the stage is done.
- `src/rubikscube/CoachPanel.java`: the Teach me card: the stage bar, wrapped words, the turn chips and the turn in words, painted by hand so it wraps the same on the desktop and in the browser.
- `src/rubikscube/CardPaint.java`: what the Teach me, Patterns and My cube cards share: their colors, words wrapped by hand, the turn chips and the buttons.
- `src/rubikscube/Patterns.java`: the seven patterns as face notation, read into quarter turns, and applying turns to a cube's stickers.
- `src/rubikscube/PatternPanel.java`: the Patterns card: the list with a small cube drawn wearing each pattern, and one pattern's card with its chips, status and Show me / Make it buttons.
- `src/rubikscube/RealCube.java`: My cube's check: whether 54 painted colors are a position a real cube can be turned into, and if not, why in words.
- `src/rubikscube/PaintPanel.java`: the My cube card: how to hold the cube, the six colors with the squares left of each, the check's word, and Cancel and Teach me.
- `src/rubikscube/SolveTimer.java`: the Timer: Space held, let go and pressed again as a state, the scramble, the time format and the averages.
- `src/rubikscube/TimerPanel.java`: the Timer card: the scramble, the time (the pad to hold on a touch screen), the averages and the last times.
- `src/rubikscube/ClickCard.java`: what the Patterns and My cube cards share: places to click laid out at each paint, the one under the mouse, and their buttons.
- `src/rubikscube/MoveHistory.java`: the path of positions from the solved cube; applies and records moves and gives the next move back (Hint, Solve).
- `src/rubikscube/LevelRecords.java`: the 20 levels' best stars and moves, the last level, the patterns made and the Timer's times, read from and saved to `~/.rubikscube-levels.txt`; the star rule.
- `src/rubikscube/Move.java`: applies a W/A/S/D move for the selected face and square as a layer turn (see A real cube).
- `src/rubikscube/MoveKeyDispatcher.java`: global key dispatcher that turns W/A/S/D presses into moves, Z/H/R/N/P into the level keys, T into Teach me, C into My cube (1 to 6 then pick its colors), K into the Timer (Space then holds, starts and stops it), G, 1 to 7, M and Backspace into Patterns, V into the view switch, X into the mirrors and the arrow keys into turning the 3D cube, except while typing in a text field.
- `src/rubikscube/Scrambler.java`: the Mix feature; applies random moves through the `MoveHistory` on a Swing timer. Also makes the random moves for a level's mix.
- `src/rubikscube/ColorPalette.java`: maps color names to `Color`s.

## Running

From the repo root: `./run.ps1 old RubiksCube2D` (or `./run.ps1 old RubiksCube2D -Build` to build a jar).

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

`test/rubikscube/RealCubeCheck.java` checks My cube's check: that a solved cube and 2000 cubes mixed with up to 60 outer turns pass (and Teach me solves every tenth one), and that gray squares, a changed center, a tenth square of one color, a twisted corner, a corner in mirror order, a flipped edge, two edges swapped, a doubled edge and an edge no cube has are each caught and named, while two edges and two corners swapped together pass; and that trading any two squares of different colors on a mixed cube is always caught. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.RealCubeCheck
```

`test/rubikscube/TimerCheck.java` checks the Timer: that 2000 scrambles have 20 turns, never one face twice in a row or three turns on one axis, and mix the cube; that a tap shorter than the hold does not start it, a held key's repeats count once, letting go after the hold starts it and the next press stops it with the right time; the time format (cut, not rounded, minutes past 60 s); Ao5 leaving out the best and worst; and that the latest 100 times are kept, Delete last drops one, and they survive a reload beside the level records. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.TimerCheck
```

`test/rubikscube/DragCheck.java` checks that a drag on the 3D cube takes the layer by hand: it follows the pointer, a slow drag let go short of half way makes no move and springs back, past half way it makes the move and the turn goes on from where the drag left it, dragged back past half way it turns the other way, a quick flick counts, a move the window does not take springs back, and while the window takes no moves the drag asks for the move at once. It checks the turn arrows: for every square of the side facing you and each key, the arrow stays in the view and runs the way that square moves; a key's or a mix's turn draws it and it fades after the turn, and a drag's turn draws none. It also checks that Show me how pulls the layer part way and lets it spring back without making a move, and that a press on the cube stops it, and that a solve's stars show in gold over the cube (two stars more gold than one), pop in a beat after the solve unless still, go by themselves, and end at a press. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -Djava.awt.headless=true -cp ../out/check/RubiksCube2D rubikscube.DragCheck
```

`test/rubikscube/MirrorCheck.java` checks the mirrors: from 48 points of view (eight turns round, six heights) every hidden side has a mirror, each inside the view and under the row of buttons, clear of the cube's middle and of the other mirrors; a click on the mirror of the side underneath turns that side to the front; X's setting hides them; and `mirrors=off` is saved, read back beside the level records and dropped when they are turned on again. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -Djava.awt.headless=true -cp ../out/check/RubiksCube2D rubikscube.MirrorCheck
```

`test/rubikscube/StripCheck.java` checks the round strip: on the desktop, phone and wide layouts it shows Your turn after a mix, inside the view and clear of the cube; it fills a pip per mixing move with no stars yet, starts at three stars and no moves, counts a move past par (two stars) and Undo takes it off; a solve at par turns it into Next: level 2 with three stars, and a click on that button starts level 2's mix; 6 moves on par 2 leave two stars and 7 one; Solve takes it away. It needs a display (it makes the window without showing it) and writes level records to `~`, so give it a scratch home. From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -Duser.home=../out/check/RubiksCube2D/home -cp ../out/check/RubiksCube2D rubikscube.StripCheck
```

`test/rubikscube/LayoutCheck.java` checks the phone layout: on screens 360 to 599 px wide the window is as wide as the screen and fits its height, and in both views every control lies inside the window, clear of the others and below the cube; the desktop window is still 550x700 with its buttons where they were. It checks the wide layout too: on landscape screens from 760x700 to 1920x739 the window fits the screen, the cube takes its height at the left and every control lies inside the window, clear of the others and of the cube; a 1366x768 screen keeps the desktop window. It needs a display (it makes the window without showing it). From `RubiksCube2D/`:

```
javac -d ../out/check/RubiksCube2D src/rubikscube/*.java test/rubikscube/*.java
java -ea -cp ../out/check/RubiksCube2D rubikscube.LayoutCheck
```

The check found that `d` on the yellow and white faces turned the top or bottom face the wrong way (so `a` then `d` did not undo), which is now fixed.
