# ScreenSaver

A screen saver written in Java. It fills the screen with black and adds randomly shaded green circles one at a time at random grid positions until the grid is full, then removes them one at a time in reverse order, and repeats. It closes if you move the mouse.

## Controls / usage

- **Mouse**: moving or dragging more than a few pixels, clicking, pressing, releasing or leaving the window exits. The move event some systems send when the window first appears under the cursor is ignored, as is leaving the window in the first second.
- **Esc**: exit.
- **Left / Right**: slow down / speed up (multiplies or divides the 250 ms step delay by 1.1, between 1 ms and 5 s).
- **Up / Down**: shrink / grow the circles by a factor of 1.1 (at least 10 px). The grid is rebuilt for the new size and the cycle starts over.
- **0 / 1 / 2** (number row): set the mode. 0: green circles (the original). 1: green squares. 2: circles in random bright colors.
- Keys act on key release. No command-line arguments.
- The window is undecorated and fills the screen. The grid cell size is the greatest common factor of the screen width and height (120 px on 1920x1080).

## Code layout

- `src/screensaver/Main.java`: the whole app: full-screen `JFrame`, input listeners, a Swing timer that adds or removes one tile per step, and the drawing code.

The original jar set a custom cursor from `cursor.png`, which did not ship. The cursor is now only changed if a `cursor.png` is present in the working directory.

## Running

From the repo root: `./run.ps1 ScreenSaver` (or `./run.ps1 ScreenSaver -Build` to build a jar).
