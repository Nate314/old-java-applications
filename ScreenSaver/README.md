# ScreenSaver

A full-screen gallery of ten screen savers written in Java. The first is the original: it fills the screen with black and adds randomly shaded green circles one at a time at random grid positions until the grid is full, then removes them one at a time in reverse order, and repeats. The others:

1. **Green circles**: the original.
2. **Green squares**: the same with squares.
3. **Bright circles**: the same in random bright colors.
4. **Starfield**: flying through space; stars stream out of the middle of the screen and streak past the edges.
5. **Mystify**: two four-cornered outlines whose corners bounce off the edges, each leaving a fading trail, in slowly changing colors.
6. **Pipes**: shiny pipes grow across a grid a cell at a time, turning at ball joints, never crossing. A pipe that is boxed in or long enough ends and a new one starts in another color; when about half the screen is pipe it is cleared.
7. **Bouncing logo**: an "OLD JAVA" logo drifts and bounces, changing color on every bounce. When it hits a corner exactly, the corner bursts into sparks and a counter says how many corners it has hit.
8. **Dot clock**: the time (12-hour) in green dots like the original's, with a row of 60 small dots for the seconds and the date underneath. Dots grow in as the digits change, and the whole clock drifts slowly so nothing stays in one place.
9. **Aquarium**: a painted tank of fish that are yours. They live in the settings file, so the same fish are there the next time, with their names. The first time the aquarium is shown three fish move in; on each later day it is shown, one more swims in from the side with a "new today" name tag, until there are 12 (one new fish per day you look in, however long you were away). There are six kinds (clownfish, blue tang, neon tetra, angelfish, goldfish, yellow tang), each new fish being of a kind the tank has fewest of. **F** drops a pinch of flakes and every fish goes for the nearest one; flakes they miss settle on the sand and fade. Fish grow from 60% to full size over a fortnight, a little faster for every flake eaten. The water follows the clock: sunlit from 7:00 to 19:00, dark blue from 20:30 to 5:30 (easing between), when the fish swim slower and keep low among the weed. Neon tetras keep together. Every name shows for a few seconds when the aquarium comes up and on **H**.
10. **Maze**: walking a brick maze in first person, at night, toward a gold orb hovering in the far corner. Each maze is 9 by 9 rooms with exactly one way between any two, carved at random. The walker keeps a hand on its left wall, so it tries dead ends on the way but always arrives. When it reaches the orb, the maze is shown from above for a few seconds: the way through in gold, the dead ends it tried in dim amber, and how many steps it took. Then a new maze.

By default it moves on to the next saver every 30 seconds. Moving the mouse wakes it: rather than closing, the saver shrinks back into its own little screen on a wall of all ten, live (the gallery, below, headed "You woke it"), so whoever moved the mouse sees what else there is to watch and can pick one with a click. Esc closes it.

The wall is also the screen saver's remote, so a mouse or a finger can do everything the keys do. Every little screen has a **Play** badge in its corner: clicking the screen watches that saver, clicking its Play badge picks it and plays with it at once (T, below), with the caption saying what the mouse does to it. Along the bottom are three buttons: **Each in turn** (watch them all again, from the one lit, every 30 seconds, as C does), **Music: off / on** (M; it plays once a saver is on screen, since the wall is quiet) and **Close** (Esc). While playing, a **Done** button in the top right corner goes back to the wall, which is how a phone, with no T key, stops playing.

**G** shows all ten at once, live, each on a small screen of its own in a grid (four across on a wide screen, two on a phone), with its number and name under it. Pick one with the arrow keys and Enter (or Space), its number, or a click (a tap in the browser): it grows from its little screen to the whole one and stays there, as a number key would. T picks the lit one to play with, as its Play badge does. **G** again goes back to the saver that was showing, cycling as before. In the grid the mouse does not close anything; it lights up the screen under it. Each saver is drawn at full screen size and scaled into its own picture, so it looks as it will when picked.

**T** turns a saver into a toy you can play with: the mouse (or a finger) no longer wakes it, and each saver answers it in its own way. Esc still closes, and T again (or the Done button) hands it back (the next mouse move, after a moment, wakes it to the wall as before). The caption's last line says what the mouse does to the saver showing.

- **Green circles, Green squares, Bright circles**: the tiles near the pointer slip aside and shrink, like a hand through beads; held down, it pushes wider and harder.
- **Starfield**: the stars stream out of a point most of the way to the pointer, so pointing steers the flight; hold the button for warp speed (five times as fast, longer streaks).
- **Mystify**: hold the button and every corner is pulled toward the pointer, up to three times its own speed, so the outlines wrap round it; let go and they drift back to their own speed.
- **Pipes**: a press on an empty cell starts a new pipe there; hold and drag, and the pipe grows toward the pointer one cell at a time and waits under it.
- **Bouncing logo**: grab the logo, drag it, and let go to throw it (at up to eight times its drift, slowing back down over a few seconds). Throw it into a corner and it counts.
- **Dot clock**: the dots scatter from the pointer and settle back when it moves on.
- **Aquarium**: a click drops a pinch of flakes where you click, and the fish come for it.
- **Maze**: hold the button and the walker hurries, three times as fast.

Play is not saved: the screen saver always starts out waking on the mouse.

**M** turns on music made by the saver on screen: each one plays its own instrument, and what it draws sets the notes. Every note is from one pentatonic scale (C major), so whatever rings together sounds right, and each instrument waits a moment between notes (from 0.05 s to 0.4 s, in real time), so a fast speed gives a busier tune rather than a buzz.

- **Green circles, Green squares, Bright circles**: a music box note for every tile that comes or goes, higher the nearer the top of the screen; leaving ones are softer.
- **Starfield**: a celesta twinkle as a near star streaks past, pitched by where round the middle it went; louder at warp speed (T).
- **Mystify**: vibes when a corner bounces, higher further up a side or further right along the top or bottom.
- **Pipes**: a marimba knock at every ball joint, higher nearer the top.
- **Bouncing logo**: a xylophone note on every bounce, pitched by the logo's new color, and a chord of bells on a corner hit.
- **Dot clock**: a low tick each second as the colon comes on, and a chord of bells when the digits change, higher later in the hour.
- **Aquarium**: a kalimba note when a fish eats a flake, and now and then a soft one as a bubble reaches the top (higher for a smaller bubble).
- **Maze**: a harp note for every room the walker enters, rising as it nears the orb's corner and softer in dead ends; a chord when it reaches the orb.

The music is off at every start and is not saved, so the site's idle screen saver (below) stays silent. It plays only for the saver on screen: the gallery (G) is quiet.

**Calm, for less motion.** In the browser, when the visitor's system asks for reduced motion (see [Reduced motion](../web/README.md#reduced-motion)), the screen saver calms down by itself, so the site's idle screen saver is easy to come back to. Starfield, Mystify, Pipes, the bouncing logo, the dot clock's drift, the aquarium and the maze go no faster than 0.35x (a slower setting stays), and the tile savers no faster than 4x, so a fast setting never makes the grid flicker. The speed line in the caption says so: `speed 1.0x (calm, for less motion: 0.35x)`. The Maze is watched from above instead of walked in first person: the brick maze lies still, rooms light up dim amber as the walker passes, it moves as a lantern from room to room, and the orb glows in its corner (the gold way through is still kept for the map at the end). A saver picked in the gallery (G) is simply there, without growing out of its little screen. On the desktop nothing changes, since the JDK does not answer the question.

## Controls / usage

- **N** or **Space** / **P**: next / previous saver.
- **1** to **9**, and **0** for the tenth (number row): show that saver and stop cycling, so it stays on screen.
- **G**: all the savers at once, to pick one (see above). In it, the arrow keys (or N and P) move, Enter, Space, a number or a click picks to watch, T or a click on a Play badge picks to play with, C or Each in turn cycles from the lit one, M or the Music button turns the music on or off, G goes back and Esc or Close closes. Picking from the wall always starts out watching unless it was by Play (or T).
- **T**: play with the saver (see above), or stop.
- **M**: music on or off (see above).
- **C**: cycle on or off (every 30 seconds).
- **Left / Right**: slower / faster, by a factor of 1.1 per press (from 0.05x to 250x; at 250x the tiles step every millisecond, as the original's fastest). The moving savers stop speeding up at 8x.
- **Up / Down** (tile savers): shrink / grow the tiles by a factor of 1.1 (at least 10 px). The grid is rebuilt for the new size and the cycle starts over.
- **F** (Aquarium): feed the fish. It shows for 5 seconds when the screen saver starts, on every change of saver, and when the speed or cycling changes.
- **Esc**: close.
- **Mouse** (unless playing, T): moving or dragging more than a few pixels, clicking, pressing, releasing or leaving the window wakes it to the wall of all ten (G); the release of a press that woke it does not pick the screen under it. Mouse moves in the first 1.5 seconds, and for 1.5 seconds after a pick, are ignored (showing the window can report one when the mouse has not moved), as is leaving the window then.
- Closing (Esc) works like the window's close button would: run alone it exits, and inside NathaNOS only the screen saver closes.
- Keys act on key release. No command-line arguments.
- The window is undecorated and fills the screen. The tile size is the greatest common factor of the screen width and height (120 px on 1920x1080) when that gives between 4 and 12 rows, as the original; on other screens, where that factor is often a pixel or two, it is a ninth of the screen height. The grid is centered, so the edge tiles are cut evenly.
- The saver last shown, cycling, the speed and a changed tile size are kept in `~/.screensaver.txt` (lines like `saver 5`, `cycle on`, `speed 1.0`, `tiles 0.0`; 0 means the size from the screen), so the next run starts where this one left off. The Aquarium's fish are kept there too: `fishday 20394` (the day number the last fish moved in) and one `fish <kind> <day it moved in> <flakes eaten> <name>` line each, like `fish 0 20394 7 Pip` (kinds in the order above, from 0).

## Code layout

- `src/screensaver/Main.java`: the full-screen `JFrame`, the input listeners, a 16 ms Swing timer that advances the current saver by the time elapsed (times the speed), the fade in from black on each change, and the caption. It paints through a `JPanel` content pane.
- `src/screensaver/Saver.java`: the interface each saver implements: `reset` to a screen size, `tick` by milliseconds, `paint`, and `touch` for the pointer in play (T), with `away`, the push the tiles and dot clock share.
- `src/screensaver/Tiles.java` (the original and its two variants), `Starfield.java`, `Mystify.java`, `Pipes.java`, `BouncingLogo.java`, `DotClock.java`, `Aquarium.java`, `Maze.java`: the savers.
- `src/screensaver/Music.java`: the music (M): the scale, each saver's instrument and gap between notes, and the MIDI synthesizer, opened on the first note. Savers ring it as things happen; `Main` says when it is listening.
- `src/screensaver/Gallery.java`: the grid of all the savers (G): its layout, the selection, the Play badges and the buttons along the bottom, and painting each saver scaled into its own image.
- `src/screensaver/Settings.java`: reads and writes `~/.screensaver.txt`, with the tank's lines.

The original jar set a custom cursor from `cursor.png`, which did not ship. The cursor is now only changed if a `cursor.png` is present in the working directory.

## Self-check

`test/screensaver/SaverCheck.java` checks the tile size rule, one tile per 250 ms filling and emptying the grid, that every saver runs and draws at 1920x1080 and 375x600, that Pipes clears the screen so it stays bounded, the dot clock's 12-hour face and digit dots, that the bouncing logo counts a corner hit but not a side bounce, and that the settings file round-trips and survives damaged lines. For the Aquarium: day numbers (local midnight, leap years), three fish on the first look, none more that day or when the clock goes back, one on a later day however long the gap, a full tank of 12 with unique names and an even mix of kinds, growth, flakes eaten and counted, flakes not staying forever, day and night, and fish lines round-tripping and damaged ones skipped. For the Maze, over 30 mazes: every room carved with no loops, the route joining start and orb, the walker always reaching the orb, and a new maze after the map. For the gallery: ten same-sized screens inside the window and under its header, none overlapping, at 1920x1080, 900x600, 375x600 and 400x700; the Play badge inside each screen and the three buttons under every screen and name, apart and big enough to tap, at those sizes; a click on a badge or a button pressing only it; every saver showing on its little screen; a click on each screen picking it; and the arrow keys moving through the grid. For play (T): the push away from the pointer; the tiles and dot clock drawn aside under the pointer and back after; the stars streaming from near the pointer and back from the middle; Mystify's corners speeding up when held (at most three times) and slowing back; a pipe started at a press and grown to the pointer, waiting there; the logo picked up only by a press on it, following the pointer, thrown fast the way it moved and slowing back to its drift; one pinch of flakes per press; the maze walker hurrying; and every saver saying what the mouse does. For music (M): no note until it is on; then every saver rings notes, all in the pentatonic scale and at a fair loudness, none faster than its gap (the tiles at 40x speed), and turning it off lets every note go. For calm: the moving savers held to 0.35x and the tiles to 4x only when calm, slower settings kept, the caption naming it, the desktop JDK never calm, and the Maze from above showing the lantern, the orb and the rooms walked. From `ScreenSaver/`:

```
javac -d ../out/check/ScreenSaver src/screensaver/*.java test/screensaver/*.java
java -ea -cp ../out/check/ScreenSaver screensaver.SaverCheck
```

## On the web site

The web site uses this app as its own screen saver: a page left alone for 3 minutes shows the saver last picked here, so a visitor who picked the Aquarium comes back from a break to their own fish (see [The site's screen saver](../web/README.md#the-sites-screen-saver)). Its title screen on the site turns that on and off.

## Running

From the repo root: `./run.ps1 old ScreenSaver` (or `./run.ps1 old ScreenSaver -Build` to build a jar).
