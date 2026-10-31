# FlappyBird

A Flappy Bird clone. The bird falls under gravity and flaps upward; fly through the gaps between scrolling pipes. Hitting a pipe or the ground ends the game. The score (pipes passed) is drawn at the top center, and the best score is saved between runs.

## Controls / usage

- **Space**: start from the title screen, flap, or (on the game over screen) go back to the title screen.
- **P**: pause / resume.
- **R**: reset the game to the title screen.
- **D**: toggle debug mode. Draws hitboxes, shows a debug text panel, and on collision shows "COLISION" instead of ending the game.
- **Up / Down**: increase / decrease the debug text font size by 5.
- **T**: rotate the loaded images one slot (background, ground, top pipe, bottom pipe, bird). Takes effect on the next reset. Prints "test".
- **Argument**: an optional integer, e.g. `java flappyBird.Main 120`: the game speed in steps per second. Defaults to 120 if omitted or not a number (the original `run.bat` passed 120; the original code printed it but never used it).
- The window is 3/4 of the screen size and centered.
- The best score is stored in `.flappybird-highscore.txt` in your home folder.

## Code layout

- `src/flappyBird/Main.java`: the `JFrame`; sizes the window, parses the argument and wires the key listener.
- `src/flappyBird/GamePanel.java`: loads images; a Swing timer runs fixed game steps based on elapsed time, then repaints. Draws the game and the title, pause and game over screens, checks collisions, handles keys and the saved best score.
- `src/flappyBird/Bird.java`: bird position, gravity and flap.
- `src/flappyBird/Pipes.java`: generates random pipe pairs endlessly as they scroll in, drops them once off screen, and counts the pairs passed.
- `src/flappyBird/Background.java`: two scrolling layers (sky and ground) that wrap around.
- `src/flappyBird/Position.java`: x/y plus collision x/y.
- `src/*.jpg`, `src/*.png`: the game images.

## Running

From the repo root: `./run.ps1 FlappyBird` (or `./run.ps1 FlappyBird -Build` to build a jar).

Images are loaded by relative path (`bg.jpg`, `bird.png`, ...), so the working directory must be `FlappyBird/src`.
