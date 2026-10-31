# SpriteFloatyStars

A background program written in Java that animates colored dots ("stars") on a blue field. A small settings window picks the mode, window size, star count and star size, then opens the animation.

## Controls / usage

Settings window:

- **Mode**: Mode 1 to Mode 5.
  - Mode 1: white stars at random positions drift left and down, wrapping at the edges.
  - Mode 2: stars burst from the center and bounce off the edges, colored by position.
  - Mode 3: same burst and bounce, colored by direction (red, green, magenta or cyan).
  - Mode 4: bounce code with no star setup (see quirks).
  - Mode 5: like Mode 1, but overlapping stars average their velocities so they drift together.
- **X/Y**: animation window width and height (default 800 x 800).
- **Number**: star count (default 1000).
- **Size**: star diameter in px (default 20).
- **OK**: open a new animation window with those settings. Each click opens another window.

Closing any window exits the program. No command-line arguments.

## Code layout

- `src/animate2/Animate2Main.java`: entry point; shows the `SettingsFrame`.
- `src/animate2/SettingsFrame.java`: the settings window; parses the fields and opens an `AnimationFrame`.
- `src/animate2/AnimationFrame.java`: the animation window holding a `StarField`.
- `src/animate2/StarField.java`: star positions, velocities and colors; moves and draws them for each mode, then calls `repaint()` again.

## Running

From the repo root: `./run.ps1 SpriteFloatyStars` (or `./run.ps1 SpriteFloatyStars -Build` to build a jar).

## Known quirks

- The star arrays are sized from the field default of 1000 before the constructor sets `starCount`, so a Number above 1000 throws `ArrayIndexOutOfBoundsException` and no window opens.
- Mode 4 (index 3) never initializes positions or velocities and sets no star color, so every star sits at (0, 0) drawn in the background blue: a plain blue window.
- `AnimationFrame` checks Up / Down arrow codes in `keyTyped`, which never carries arrow key codes, so those keys do nothing. If they did, raising the count past the array size would crash.
- Animation speed depends on the machine: `paint()` calls `repaint()` with no delay.
- Mode 2's color divides y by the field width instead of the height.
- Non-numeric fields throw `NumberFormatException`.
