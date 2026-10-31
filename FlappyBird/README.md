# FlappyBird

A Flappy Bird clone. The bird falls under gravity and flaps upward; fly through the gaps between scrolling pipes. Hitting a pipe or the ground restarts the game. The score is drawn at the top center.

## Controls / usage

- **Space**: flap.
- **P**: pause / resume.
- **R**: reset the game.
- **D**: toggle debug mode. Draws hitboxes, shows a debug text panel, and on collision shows "COLISION" instead of resetting.
- **Up / Down**: increase / decrease the debug text font size by 5.
- **T**: rotate the loaded images one slot (background, ground, top pipe, bottom pipe, bird). Takes effect on the next reset. Prints "test".
- **Argument**: an optional integer, e.g. `java flappyBird.Main 120`. Defaults to 120 if omitted (the original `run.bat` passed 120). It is printed and stored but never used.
- The window is 3/4 of the screen size and centered. The game starts after a short delay (see quirks).

## Code layout

- `src/flappyBird/Main.java`: the `JFrame`; sizes the window, parses the argument and wires the key listener.
- `src/flappyBird/GamePanel.java`: loads images, advances and draws the game on each paint, checks collisions, handles keys.
- `src/flappyBird/RenderLoop.java`: background thread that repaints in a tight loop and sets `running` once the start condition is met.
- `src/flappyBird/Bird.java`: bird position, gravity and flap.
- `src/flappyBird/Pipes.java`: generates 1000 random pipe pairs, scrolls them and tracks the current pipe index.
- `src/flappyBird/Background.java`: two scrolling layers (sky and ground) that wrap around.
- `src/flappyBird/Position.java`: x/y plus collision x/y.
- `src/*.jpg`, `src/*.png`: the game images.

## Running

From the repo root: `./run.ps1 FlappyBird` (or `./run.ps1 FlappyBird -Build` to build a jar).

Images are loaded by relative path (`bg.jpg`, `bird.png`, ...), so the working directory must be `FlappyBird/src`. A non-integer argument throws `NumberFormatException`.

## Known quirks

- Game speed is tied to repaint speed: `RenderLoop` repaints in a loop with no sleep, and `paint()` advances the game, so speed depends on the machine.
- `RenderLoop` waits until the screen pixel at (`FRAME_WIDTH / 2`, `FRAME_HEIGHT / 2`) has a blue value of 0 before starting. Those are screen coordinates, not window coordinates.
- Only 1000 pipe pairs are generated; there is no handling for running past them.
- Scroll speeds use integer division (`FRAME_WIDTH / 1000`, `FRAME_WIDTH / 500`), and the wrap check is an exact float compare (`x + FRAME_WIDTH == 0`), so on some screen widths the background can stop or fail to wrap.
- Unused: the `launchArgument` and `frame` constructor parameters in `GamePanel`, `Pipes.initialIndices`, and the `AffineTransform` fields.
