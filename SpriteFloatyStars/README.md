# SpriteFloatyStars

A background program written in Java that animates colored dots ("stars") on a blue field. A small settings window picks the mode, window size, star count and star size, then opens the animation.

## Controls / usage

Settings window:

- **Mode**: Mode 1 to Mode 5.
  - Mode 1: white stars at random positions drift left and down, wrapping at the edges.
  - Mode 2: stars burst from the center and bounce off the edges, colored by position.
  - Mode 3: same burst and bounce, colored by direction (red, green, magenta or cyan).
  - Mode 4: stars start at random positions and bounce off the edges, each in its own random color.
  - Mode 5: like Mode 1, but overlapping stars average their velocities so they drift together.
- **X/Y**: animation window width and height (default 800 x 800).
- **Number**: star count (default 1000, any size).
- **Size**: star diameter in px (default 20).
- **OK**: open a new animation window with those settings. Each click opens another window. If a field is not a positive whole number, the settings title says so and no window opens.

Animation window:

- **Up / Down arrows**: add or remove one star.

Closing any window exits the program. No command-line arguments.

## Code layout

- `src/animate2/Animate2Main.java`: entry point; shows the `SettingsFrame`.
- `src/animate2/SettingsFrame.java`: the settings window; parses the fields and opens an `AnimationFrame`.
- `src/animate2/AnimationFrame.java`: the animation window holding a `StarField`.
- `src/animate2/StarField.java`: star positions, velocities and colors; moves them on a 15 ms Swing timer (scaled by the time since the last tick, so the speed does not depend on the machine) and draws them for each mode.

## Running

From the repo root: `./run.ps1 SpriteFloatyStars` (or `./run.ps1 SpriteFloatyStars -Build` to build a jar).
