# DinoGame

A bot for a dinosaur jumping game. It watches one screen pixel and presses Space when that pixel's color matches an obstacle. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`), where the jar was named `DinoGame1920x1080.jar`.

## Controls / usage

- Launch it with the game visible on screen. After 2.5 s it shows two 10x10 always-on-top marker squares, presses Space once to start the game, then checks the sensor pixel every 1 ms.
- **Red marker**: the sensor, at (`screenWidth / 2.44`, `screenHeight / 4.5`). The pixel it reads is one to the left of the marker, so the marker does not cover it.
- **Green marker**: at (`screenWidth / 2.44`, `screenHeight / 5.35`). It is only drawn; nothing reads that position.
- When the sensor pixel's blue value is exactly 83, it holds Space for 65 ms.
- There is no stop control and the markers have no title bar. Stop the process (for example Ctrl+C in the terminal that launched it).
- No command-line arguments.

## Code layout

- `src/dinosaurgameplayer/DinosaurGamePlayer.java`: entry point; computes the sensor positions, waits 2.5 s, shows the markers, presses Space and starts the timer.
- `src/dinosaurgameplayer/ObstacleDetector.java`: the timer action; reads the sensor pixel with `java.awt.Robot` and presses Space.

## Running

From the repo root: `./run.ps1 DinoGame` (or `./run.ps1 DinoGame -Build` to build a jar).

It sends real Space key presses via `java.awt.Robot` to whatever window has focus, starting about 2.5 s after launch.

## Known quirks

- Sensor positions are fixed fractions of the full screen size, so they only line up if the game sits at the matching spot on screen. The original jar name suggests it was set up for a 1920x1080 screen.
- Only the blue channel is checked, so any color with blue 83 triggers a jump.
- The 65 ms key hold runs on the Swing event thread, so the pixel is not checked during a jump.
