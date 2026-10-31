# CircleSpawner

Enter a diameter and it fills a disk on screen with borderless blue windows. Each window is a rectangle whose corners sit on the circle, and the overlapping rectangles together form the disk. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Text field**: the diameter in pixels (default `100`).
- **"Create a circle with that diameter"**: spawns one window per pixel of diameter. Window `i` is `2 * i` wide and `2 * sqrt(r^2 - i^2)` tall, centered horizontally on the screen. Each window's title is its index.
- Every 10th window is always on top.
- The spawned windows have no title bar. Closing the "Circle Creator" window exits the app and removes them all.
- No command-line arguments.

## Code layout

- `src/circlewindow/Main.java`: entry point; creates a `CircleCreatorWindow`.
- `src/circlewindow/CircleCreatorWindow.java`: the 250x110 input window; computes slice sizes and spawns the windows.
- `src/circlewindow/CircleSliceWindow.java`: one undecorated blue window at a given position and size.

## Running

From the repo root: `./run.ps1 CircleSpawner` (or `./run.ps1 CircleSpawner -Build` to build a jar).

A non-integer diameter throws `NumberFormatException`.

## Known quirks

- The disk is centered at (`screenWidth / 2`, `screenHeight / 2 + radius / 2`), so it sits below the screen center by half the radius.
- One window is created per index up to the diameter, but only indexes up to the radius have a real height. Past that, `sqrt` of a negative number is `NaN`, which becomes a height of 0, so about half the windows are zero-height. Index 0 has a width of 0.
- Each click spawns a new set of windows on top of any earlier ones.
- Prints the creator window's size to stdout after each click.
- Unused: the creator window's key listener (all methods empty), the `getScreenSize()` call in `CircleSliceWindow`, and the always-true `sliceIndex < diameter` check.
