# ScreenSaver

A full-screen gallery of eight screen savers written in Java. The first is the original: it fills the screen with black and adds randomly shaded green circles one at a time at random grid positions until the grid is full, then removes them one at a time in reverse order, and repeats. The others:

1. **Green circles**: the original.
2. **Green squares**: the same with squares.
3. **Bright circles**: the same in random bright colors.
4. **Starfield**: flying through space; stars stream out of the middle of the screen and streak past the edges.
5. **Mystify**: two four-cornered outlines whose corners bounce off the edges, each leaving a fading trail, in slowly changing colors.
6. **Pipes**: shiny pipes grow across a grid a cell at a time, turning at ball joints, never crossing. A pipe that is boxed in or long enough ends and a new one starts in another color; when about half the screen is pipe it is cleared.
7. **Bouncing logo**: an "OLD JAVA" logo drifts and bounces, changing color on every bounce. When it hits a corner exactly, the corner bursts into sparks and a counter says how many corners it has hit.
8. **Dot clock**: the time (12-hour) in green dots like the original's, with a row of 60 small dots for the seconds and the date underneath. Dots grow in as the digits change, and the whole clock drifts slowly so nothing stays in one place.

By default it moves on to the next saver every 30 seconds. It closes if you move the mouse.

## Controls / usage

- **N** or **Space** / **P**: next / previous saver.
- **1** to **8** (number row): show that saver and stop cycling, so it stays on screen.
- **C**: cycle on or off (every 30 seconds).
- **Left / Right**: slower / faster, by a factor of 1.1 per press (from 0.05x to 250x; at 250x the tiles step every millisecond, as the original's fastest). The moving savers stop speeding up at 8x.
- **Up / Down** (tile savers): shrink / grow the tiles by a factor of 1.1 (at least 10 px). The grid is rebuilt for the new size and the cycle starts over.
- **H**: show the caption with these keys again. It shows for 5 seconds when the screen saver starts, on every change of saver, and when the speed or cycling changes.
- **Esc**: close.
- **Mouse**: moving or dragging more than a few pixels, clicking, pressing, releasing or leaving the window closes it. Mouse moves in the first 1.5 seconds are ignored (showing the window can report one when the mouse has not moved), as is leaving the window then.
- Closing works like the window's close button would: run alone it exits, and inside NathaNOS only the screen saver closes.
- Keys act on key release. No command-line arguments.
- The window is undecorated and fills the screen. The tile size is the greatest common factor of the screen width and height (120 px on 1920x1080) when that gives between 4 and 12 rows, as the original; on other screens, where that factor is often a pixel or two, it is a ninth of the screen height. The grid is centered, so the edge tiles are cut evenly.
- The saver last shown, cycling, the speed and a changed tile size are kept in `~/.screensaver.txt` (lines like `saver 5`, `cycle on`, `speed 1.0`, `tiles 0.0`; 0 means the size from the screen), so the next run starts where this one left off.

## Code layout

- `src/screensaver/Main.java`: the full-screen `JFrame`, the input listeners, a 16 ms Swing timer that advances the current saver by the time elapsed (times the speed), the fade in from black on each change, and the caption. It paints through a `JPanel` content pane.
- `src/screensaver/Saver.java`: the interface each saver implements: `reset` to a screen size, `tick` by milliseconds, `paint`.
- `src/screensaver/Tiles.java` (the original and its two variants), `Starfield.java`, `Mystify.java`, `Pipes.java`, `BouncingLogo.java`, `DotClock.java`: the savers.
- `src/screensaver/Settings.java`: reads and writes `~/.screensaver.txt`.

The original jar set a custom cursor from `cursor.png`, which did not ship. The cursor is now only changed if a `cursor.png` is present in the working directory.

## Self-check

`test/screensaver/SaverCheck.java` checks the tile size rule, one tile per 250 ms filling and emptying the grid, that every saver runs and draws at 1920x1080 and 375x600, that Pipes clears the screen so it stays bounded, the dot clock's 12-hour face and digit dots, that the bouncing logo counts a corner hit but not a side bounce, and that the settings file round-trips and survives damaged lines. From `ScreenSaver/`:

```
javac -d ../out/check/ScreenSaver src/screensaver/*.java test/screensaver/*.java
java -ea -cp ../out/check/ScreenSaver screensaver.SaverCheck
```

## Running

From the repo root: `./run.ps1 ScreenSaver` (or `./run.ps1 ScreenSaver -Build` to build a jar).
