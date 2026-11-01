# FlappyBird

A Flappy Bird clone. The bird falls under gravity and flaps upward; fly through the gaps between scrolling pipes. Hitting a pipe or the ground ends the game. The score (pipes passed) is drawn at the top center, and the best score is saved between runs.

There are two courses, chosen on the title screen:

- **Endless**: new random pipes every run (the original game).
- **Today's course**: the pipes are seeded by the date, so every run on the same day meets the same pipes, and a new course starts each day. Your best run of the day is kept and flies beside you as a see-through ghost labelled with its score. Passing its score shows "You passed your ghost!", and a better run replaces it.

The game over screen shows your score, best and today's best, and a medal: Bronze at 10 pipes, Silver at 20, Gold at 30 and Platinum at 40 (the thresholds of the original Flappy Bird), with how many more pipes the next medal needs. In endless mode, beating your best score shows "New best!" during the run.

## Controls / usage

- **Space**: start from the title screen, flap, or (on the game over screen) go back to the title screen.
- **C**: on the title screen, switch between endless pipes and today's course.
- **P**: pause / resume.
- **R**: reset the game to the title screen.
- **D**: toggle debug mode. Draws hitboxes, shows a debug text panel, and on collision shows "COLISION" instead of ending the game.
- **Up / Down**: increase / decrease the debug text font size by 5.
- **T**: rotate the loaded images one slot (background, ground, top pipe, bottom pipe, bird). Takes effect on the next reset. Prints "test".
- **Argument**: an optional integer, e.g. `java flappyBird.Main 120`: the game speed in steps per second. Defaults to 120 if omitted or not a number (the original `run.bat` passed 120; the original code printed it but never used it).
- The window is 3/4 of the screen size and centered.
- The best score is stored in `.flappybird-highscore.txt` in your home folder, and today's best run in `.flappybird-today.txt` (the date, the score, and the bird's height every 4 game steps in thousandths of the window height, so the ghost replays at any window size). A file from an earlier day is ignored.

## Code layout

- `src/flappyBird/Main.java`: the `JFrame`; sizes the window, parses the argument and wires the key listener.
- `src/flappyBird/GamePanel.java`: loads images; a Swing timer runs fixed game steps based on elapsed time, then repaints. Draws the game, the ghost and the title, pause and game over screens (with the medal), checks collisions, handles keys and the saved best score, and records the run for today's course.
- `src/flappyBird/DailyCourse.java`: today's course: the date's pipe seed, today's best score and ghost, and its file.
- `src/flappyBird/Bird.java`: bird position, gravity and flap.
- `src/flappyBird/Pipes.java`: generates pipe pairs endlessly from a random source as they scroll in (a seeded one for today's course), drops them once off screen, and counts the pairs passed.
- `src/flappyBird/Background.java`: two scrolling layers (sky and ground) that wrap around.
- `src/flappyBird/Position.java`: x/y plus collision x/y.
- `src/*.jpg`, `src/*.png`: the game images.

## Running

From the repo root: `./run.ps1 FlappyBird` (or `./run.ps1 FlappyBird -Build` to build a jar).

Images are loaded by relative path (`bg.jpg`, `bird.png`, ...), so the working directory must be `FlappyBird/src`.

## Self-check

`test/flappyBird/CourseCheck.java` checks that the same day gives the same pipes and the next day different ones, that only a better run replaces today's ghost, that the ghost fills in the steps between samples and ends with its run, that the course file loads back the same ghost while a file from another day or a broken one is ignored, and the medal names and lines. From `FlappyBird/`:

```
javac -d ../out/check/FlappyBird src/flappyBird/*.java test/flappyBird/*.java
java -ea -cp ../out/check/FlappyBird flappyBird.CourseCheck
```
