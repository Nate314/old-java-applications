# CircleSpawner

Builds shapes on screen out of borderless windows. The original app did one thing: enter a diameter and it filled a disk with blue windows, each a rectangle whose corners sit on the circle, so the overlapping rectangles together form the disk. That circle is still the first shape, and the same trick now draws a ring, a heart, a star or a word you type, in rainbow or sunset colors, growing in a few windows at a time. Click any window of the shape and it bursts apart. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Shape** (the first list): Circle (the original), Ring, Heart, Star or Word. Picking one builds it at once.
- **Size**: about how many pixels across the shape is (default `200`). It must be a whole number from 2 to the room below the creator window (the smaller of the screen width and the height under the window); otherwise the window says which sizes work. A word is that tall, or smaller if it would not fit across the screen.
- **Colors** (the second list): Rainbow (the default), Blue (the original's color) or Sunset. The circle's slices are colored in the order they appear, since they share one center; the other shapes change color from left to right.
- **Word**: up to 12 letters, digits or `! ? . - '` (other characters are left out) in a 5 x 7 pixel font. Enter in this field builds the word.
- **Build it** (or Enter in Size): builds the chosen shape again, replacing the one on screen.
- The shape is centered under the creator window, which sits at the top center of the screen, and appears a few windows per frame, all of it in under a second. The line under the buttons says how many windows it took ("Heart built from 21 windows").
- **Clicking any window of the shape** scatters it: every window flies out from the middle, falls and closes.
- The circle is built as before: window `i` (1 to the radius) is `2 * i` wide and `2 * sqrt(r^2 - i^2)` tall, its title is its index, and every 10th one is always on top. The other shapes are drawn on a grid (28 cells across, a word on its 7-pixel font) and covered with upright strips, each run of lit cells in a column joined to the same run in the column before it, so a heart takes about 20 windows rather than hundreds.
- The shape's windows have no title bar and never take the keyboard from the creator window. Closing the creator window exits the app and removes them all.
- No command-line arguments.

## Code layout

- `src/circlewindow/Main.java`: entry point; creates a `CircleCreatorWindow`.
- `src/circlewindow/CircleCreatorWindow.java`: the creator window; reads the controls, colors the pieces, builds a shape a few windows per frame and scatters it, each with a Swing timer.
- `src/circlewindow/Shapes.java`: turns a shape into the rectangles of its windows: the original circle slices, the ring, heart and star on a grid, and words.
- `src/circlewindow/PixelFont.java`: the 5 x 7 pixel font for words.
- `src/circlewindow/CircleSliceWindow.java`: one undecorated window of solid color at a given place and size; pressing it scatters the shape.

## Running

From the repo root: `./run.ps1 CircleSpawner` (or `./run.ps1 CircleSpawner -Build` to build a jar).
