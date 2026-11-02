# CircleSpawner

Builds shapes on screen out of borderless windows. The original app did one thing: enter a diameter and it filled a disk with blue windows, each a rectangle whose corners sit on the circle, so the overlapping rectangles together form the disk. That circle is still the first shape, and the same trick now draws a ring, a heart, a star, a word you type or a picture you draw, in rainbow or sunset colors, growing in a few windows at a time. Pick another shape and the windows already on screen glide, stretch and change color into it instead of starting over; draw on the pad and the windows follow your pen. Click any window of the shape and it bursts apart. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Shape** (the first list): Circle (the original), Ring, Heart, Star, Word or Draw. Picking one turns the shape on screen into it (see Morphing), or builds it if nothing is showing.
- **Size**: about how many pixels across the shape is (default `200`). It must be a whole number from 2 to the room below the creator window (the smaller of the screen width and the height under the window); otherwise the window says which sizes work. A word is that tall, or smaller if it would not fit across the screen.
- **Colors** (the second list): Rainbow (the default), Blue (the original's color) or Sunset. The circle's slices are colored in the order they appear, since they share one center; the other shapes change color from left to right.
- **Word**: up to 12 letters, digits or `! ? . - '` (other characters are left out) in a 5 x 7 pixel font. Enter in this field builds the word.
- **Draw**: shows a 20 x 20 drawing pad beside the controls (under them when the screen is narrower than 560 pixels, which makes the window taller). Press an empty cell and drag to draw, press a filled one and drag to erase. Every change reshapes the windows on screen within a few frames. **Mirror** (on at first) copies each stroke onto the other side of the blue middle line, so faces, hearts and butterflies come out even. **Clear** empties the pad, and the windows shrink away. The pad starts with a smiling face. A drawing is the Size across (made smaller, rather than refused, when the taller window leaves less room under it).
- **Build it**: builds the chosen shape again from nothing, replacing the one on screen, growing in as at the start.
- **Morphing**: changing the shape, the colors, the size (Enter in Size) or the word (Enter in Word) while a shape is on screen moves its windows into the new one over about two thirds of a second. Each new window comes from the old window at the most similar place in its own shape (the same corner, side or middle, and a similar size), nearest pairs first. When the new shape needs more windows, the extra ones split off old ones; when it needs fewer, the old ones left over shrink into the nearest new window and close. While the shape is flying apart, a change builds the new shape from scratch.
- The shape is centered under the creator window, which sits at the top center of the screen, and appears a few windows per frame, all of it in under a second. The line under the buttons says how many windows it took ("Heart built from 21 windows").
- **Clicking any window of the shape** scatters it: every window flies out from the middle, falls and closes.
- The circle is built as before: window `i` (1 to the radius) is `2 * i` wide and `2 * sqrt(r^2 - i^2)` tall, its title is its index, and every 10th one is always on top. The other shapes are drawn on a grid (28 cells across, a word on its 7-pixel font) and covered with upright strips, each run of lit cells in a column joined to the same run in the column before it, so a heart takes about 20 windows rather than hundreds.
- The shape's windows have no title bar and never take the keyboard from the creator window. Closing the creator window exits the app and removes them all.
- The shape, size, colors, word, drawing and Mirror are saved in `~/.circlespawner.txt` (`key=value` lines; the drawing is 20 rows of `#` and `.` joined by `/`), so the app opens on what you had last.
- No command-line arguments.

## Code layout

- `src/circlewindow/Main.java`: entry point; creates a `CircleCreatorWindow`.
- `src/circlewindow/CircleCreatorWindow.java`: the creator window; reads the controls, colors the pieces, builds a shape a few windows per frame and scatters it, each with a Swing timer.
- `src/circlewindow/Shapes.java`: turns a shape into the rectangles of its windows: the original circle slices, the ring, heart and star on a grid, and words.
- `src/circlewindow/PixelFont.java`: the 5 x 7 pixel font for words.
- `src/circlewindow/CircleSliceWindow.java`: one undecorated window of solid color at a given place and size, which can be moved and recolored; pressing it scatters the shape.
- `src/circlewindow/Morph.java`: turns the windows of one shape into another: pairs old windows with new places, splits off or closes the difference, and moves them frame by frame.
- `src/circlewindow/DrawPad.java`: the drawing pad, with Mirror, Clear, the first smiling face and the saved form of a drawing.
- `src/circlewindow/Saved.java`: reads and writes `~/.circlespawner.txt`.

## Self-check

`test/circlewindow/MorphCheck.java` checks the morph pairing (the same places in two shapes pair up wherever the shapes are, every old window is reused when more are wanted, none is shared when fewer are), the drawn shape's size and place, an empty drawing, and that a saved drawing reads back. From `CircleSpawner/`:

```
javac -d ../out/check/CircleSpawner src/circlewindow/*.java test/circlewindow/*.java
java -ea -Djava.awt.headless=true -cp ../out/check/CircleSpawner circlewindow.MorphCheck
```

## Running

From the repo root: `./run.ps1 CircleSpawner` (or `./run.ps1 CircleSpawner -Build` to build a jar).
