# ScreenSaver

A full-screen gallery of nine screen savers written in Java. The first is the original: it fills the screen with black and adds randomly shaded green circles one at a time at random grid positions until the grid is full, then removes them one at a time in reverse order, and repeats. The others:

1. **Green circles**: the original.
2. **Green squares**: the same with squares.
3. **Bright circles**: the same in random bright colors.
4. **Starfield**: flying through space; stars stream out of the middle of the screen and streak past the edges.
5. **Mystify**: two four-cornered outlines whose corners bounce off the edges, each leaving a fading trail, in slowly changing colors.
6. **Pipes**: shiny pipes grow across a grid a cell at a time, turning at ball joints, never crossing. A pipe that is boxed in or long enough ends and a new one starts in another color; when about half the screen is pipe it is cleared.
7. **Bouncing logo**: an "OLD JAVA" logo drifts and bounces, changing color on every bounce. When it hits a corner exactly, the corner bursts into sparks and a counter says how many corners it has hit.
8. **Dot clock**: the time (12-hour) in green dots like the original's, with a row of 60 small dots for the seconds and the date underneath. Dots grow in as the digits change, and the whole clock drifts slowly so nothing stays in one place.
9. **Aquarium**: a painted tank of fish that are yours. They live in the settings file, so the same fish are there the next time, with their names. The first time the aquarium is shown three fish move in; on each later day it is shown, one more swims in from the side with a "new today" name tag, until there are 12 (one new fish per day you look in, however long you were away). There are six kinds (clownfish, blue tang, neon tetra, angelfish, goldfish, yellow tang), each new fish being of a kind the tank has fewest of. **F** drops a pinch of flakes and every fish goes for the nearest one; flakes they miss settle on the sand and fade. Fish grow from 60% to full size over a fortnight, a little faster for every flake eaten. The water follows the clock: sunlit from 7:00 to 19:00, dark blue from 20:30 to 5:30 (easing between), when the fish swim slower and keep low among the weed. Neon tetras keep together. Every name shows for a few seconds when the aquarium comes up and on **H**.

By default it moves on to the next saver every 30 seconds. It closes if you move the mouse.

## Controls / usage

- **N** or **Space** / **P**: next / previous saver.
- **1** to **9** (number row): show that saver and stop cycling, so it stays on screen.
- **C**: cycle on or off (every 30 seconds).
- **Left / Right**: slower / faster, by a factor of 1.1 per press (from 0.05x to 250x; at 250x the tiles step every millisecond, as the original's fastest). The moving savers stop speeding up at 8x.
- **Up / Down** (tile savers): shrink / grow the tiles by a factor of 1.1 (at least 10 px). The grid is rebuilt for the new size and the cycle starts over.
- **F** (Aquarium): feed the fish. It shows for 5 seconds when the screen saver starts, on every change of saver, and when the speed or cycling changes.
- **Esc**: close.
- **Mouse**: moving or dragging more than a few pixels, clicking, pressing, releasing or leaving the window closes it. Mouse moves in the first 1.5 seconds are ignored (showing the window can report one when the mouse has not moved), as is leaving the window then.
- Closing works like the window's close button would: run alone it exits, and inside NathaNOS only the screen saver closes.
- Keys act on key release. No command-line arguments.
- The window is undecorated and fills the screen. The tile size is the greatest common factor of the screen width and height (120 px on 1920x1080) when that gives between 4 and 12 rows, as the original; on other screens, where that factor is often a pixel or two, it is a ninth of the screen height. The grid is centered, so the edge tiles are cut evenly.
- The saver last shown, cycling, the speed and a changed tile size are kept in `~/.screensaver.txt` (lines like `saver 5`, `cycle on`, `speed 1.0`, `tiles 0.0`; 0 means the size from the screen), so the next run starts where this one left off. The Aquarium's fish are kept there too: `fishday 20394` (the day number the last fish moved in) and one `fish <kind> <day it moved in> <flakes eaten> <name>` line each, like `fish 0 20394 7 Pip` (kinds in the order above, from 0).

## Code layout

- `src/screensaver/Main.java`: the full-screen `JFrame`, the input listeners, a 16 ms Swing timer that advances the current saver by the time elapsed (times the speed), the fade in from black on each change, and the caption. It paints through a `JPanel` content pane.
- `src/screensaver/Saver.java`: the interface each saver implements: `reset` to a screen size, `tick` by milliseconds, `paint`.
- `src/screensaver/Tiles.java` (the original and its two variants), `Starfield.java`, `Mystify.java`, `Pipes.java`, `BouncingLogo.java`, `DotClock.java`, `Aquarium.java`: the savers.
- `src/screensaver/Settings.java`: reads and writes `~/.screensaver.txt`, with the tank's lines.

The original jar set a custom cursor from `cursor.png`, which did not ship. The cursor is now only changed if a `cursor.png` is present in the working directory.

## Self-check

`test/screensaver/SaverCheck.java` checks the tile size rule, one tile per 250 ms filling and emptying the grid, that every saver runs and draws at 1920x1080 and 375x600, that Pipes clears the screen so it stays bounded, the dot clock's 12-hour face and digit dots, that the bouncing logo counts a corner hit but not a side bounce, and that the settings file round-trips and survives damaged lines. For the Aquarium: day numbers (local midnight, leap years), three fish on the first look, none more that day or when the clock goes back, one on a later day however long the gap, a full tank of 12 with unique names and an even mix of kinds, growth, flakes eaten and counted, flakes not staying forever, day and night, and fish lines round-tripping and damaged ones skipped. From `ScreenSaver/`:

```
javac -d ../out/check/ScreenSaver src/screensaver/*.java test/screensaver/*.java
java -ea -cp ../out/check/ScreenSaver screensaver.SaverCheck
```

## On the web site

The web site uses this app as its own screen saver: a page left alone for 3 minutes shows the saver last picked here, so a visitor who picked the Aquarium comes back from a break to their own fish (see [The site's screen saver](../web/README.md#the-sites-screen-saver)). Its title screen on the site turns that on and off.

## Running

From the repo root: `./run.ps1 ScreenSaver` (or `./run.ps1 ScreenSaver -Build` to build a jar).
