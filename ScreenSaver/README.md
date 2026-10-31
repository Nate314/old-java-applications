# ScreenSaver

A screen saver written in Java. It fills the screen with black and adds randomly shaded green circles one at a time at random grid positions until the grid is full, then removes them one at a time in reverse order, and repeats. It closes if you move the mouse.

## Controls / usage

- **Mouse**: moving, dragging, clicking, pressing, releasing or leaving the window exits.
- **Esc**: exit.
- **Left / Right**: slow down / speed up (multiplies or divides the 250 ms step delay by 1.1).
- **Up / Down**: shrink / grow the circles by a factor of 1.1.
- **0 to 9** (number row): set the mode. Only mode 0 draws anything.
- Keys act on key release. No command-line arguments.
- The window is undecorated and fills the screen. The grid cell size is the greatest common factor of the screen width and height (120 px on 1920x1080).

## Code layout

- `src/screensaver/Main.java`: the whole app: full-screen `JFrame`, input listeners, a repaint thread and the drawing code.

## Running

From the repo root: `./run.ps1 ScreenSaver` (or `./run.ps1 ScreenSaver -Build` to build a jar).

## Known quirks

- It loads a custom cursor from `cursor.png` in the working directory, but no `cursor.png` ships in `ScreenSaver/`.
- Modes 1 to 9 have no drawing code, so switching to them stops all drawing.
- Up / Down change the circle size but not the grid positions or tile count, so circles overlap or leave gaps.
- The speed and size steps truncate to `int`, so once the delay or size gets small enough, Left / Down no longer change it.
- The repaint thread busy-waits instead of sleeping.
