# CircleSpawner

Enter a diameter and it fills a disk on screen with borderless blue windows. Each window is a rectangle whose corners sit on the circle, and the overlapping rectangles together form the disk. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Text field**: the diameter in pixels (default `100`).
- **"Create a circle with that diameter"**: spawns one window per pixel of radius, centered on the screen. Window `i` (1 to the radius) is `2 * i` wide and `2 * sqrt(r^2 - i^2)` tall. Each window's title is its index. Clicking again replaces the previous circle.
- The diameter must be a whole number from 2 to the smaller screen dimension; otherwise the creator window's title says so.
- Every 10th window is always on top.
- The spawned windows have no title bar. Closing the "Circle Creator" window exits the app and removes them all.
- No command-line arguments.

## Code layout

- `src/circlewindow/Main.java`: entry point; creates a `CircleCreatorWindow`.
- `src/circlewindow/CircleCreatorWindow.java`: the 250x110 input window; computes slice sizes and spawns the windows.
- `src/circlewindow/CircleSliceWindow.java`: one undecorated blue window at a given position and size.

## Running

From the repo root: `./run.ps1 CircleSpawner` (or `./run.ps1 CircleSpawner -Build` to build a jar).
