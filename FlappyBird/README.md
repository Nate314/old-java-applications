# FlappyBird

A Flappy Bird clone. The bird falls under gravity and flaps upward; fly through the gaps between scrolling pipes. Hitting a pipe or the ground ends the game. The score (pipes passed) is drawn at the top center, and the best score is saved between runs.

It moves and sounds like the original Flappy Bird:

- The bird hovers on the title screen, tilts nose up with each flap and turns into a nose-down dive as it falls, and each flap stretches it for a moment and puffs a little air.
- The score is big white digits outlined in black, and it pops bigger with each pipe passed.
- A crash flashes the screen white, shakes it and scatters feathers; after hitting a pipe the bird drops to the ground before the game over screen rises into place. For half a second that screen ignores Space, a click or a tap, so a flap pressed at the moment of the crash does not skip it.
- Sounds on the MIDI synthesizer: a tick for each flap, a two-note ding for each pipe, a thud for a crash and a falling run of notes as the bird drops. M turns them off and on.

There are two courses, chosen on the title screen:

- **Endless**: new random pipes every run (the original game).
- **Today's course**: the pipes are seeded by the date, so every run on the same day meets the same pipes, and a new course starts each day. Your best run of the day is kept and flies beside you as a see-through ghost labelled with its score. Passing its score shows "You passed your ghost!", and a better run replaces it.

## Rewind a crash

Crashed? Press **Z** (or Backspace), or click or tap the **Rewind and fly on** button on the game over screen. The last 2.5 seconds of flight play backwards at three times speed, tinted like an old video tape winding back (scan lines, a rolling band and a blinking REWIND sign, with a quick falling run of bells). The game then waits, frozen, until you flap to fly on from there, and the same pipes come back, including the ones that had not appeared yet. While you fly on, the path you flew into the crash stays drawn as white dots, scrolling with the pipes, with a red cross where you crashed, so you can see what to fly around.

A run with a rewind in it is practice: the score shows "Practice: 1 rewind" under it, and its game over screen says "(practice, not a record)". It does not set a best score, a "New best!", or today's best run and ghost. You can rewind again after each crash; each rewind goes back as far as you have flown since the last one, up to 2.5 seconds.

## Medals and records

The game over screen shows your score, best and today's best, and a medal: Bronze at 10 pipes, Silver at 20, Gold at 30 and Platinum at 40 (the thresholds of the original Flappy Bird), with how many more pipes the next medal needs. In endless mode, beating your best score shows "New best!" during the run.

## Controls / usage

- **Space**, or a **click** or **tap** anywhere in the game: start from the title screen, flap, or (on the game over screen) go back to the title screen.
- **Z** or **Backspace**: after a crash, rewind the last 2.5 seconds and fly on from there (practice). The game over screen's Rewind button does the same.
- **M**: sound off / on.
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
- `src/flappyBird/GamePanel.java`: loads images; a Swing timer runs fixed game steps based on elapsed time, then repaints. Draws the game, the ghost and the title, pause and game over screens (with the medal), the tilted bird, its puffs and feathers, the crash's flash and shake; checks collisions, handles keys, clicks and the saved best score, records the run for today's course, and runs a rewind (tape, backwards playback, the frozen wait and the last flight's path).
- `src/flappyBird/Rewind.java`: the rewind tape: the last 2.5 seconds as one moment per game step (what the bird, pipes and background remember), given back newest first, and the path into the crash.
- `src/flappyBird/Sounds.java`: the flap, pipe, crash, fall and rewind sounds on the MIDI synthesizer, opened on a background thread.
- `src/flappyBird/DailyCourse.java`: today's course: the date's pipe seed, today's best score and ghost, and its file.
- `src/flappyBird/Bird.java`: bird position, gravity and flap, its tilt (nose up after a flap, diving as it falls) and the stretch of a flap.
- `src/flappyBird/Pipes.java`: generates pipe pairs endlessly from a random source as they scroll in (a seeded one for today's course), drops them once off screen, and counts the pairs passed. It keeps every gap it has made, so pipes put back by a rewind scroll on into the same pipes.
- `src/flappyBird/Background.java`: two scrolling layers (sky and ground) that wrap around.
- `src/flappyBird/Position.java`: x/y plus collision x/y.
- `src/*.jpg`, `src/*.png`: the game images.

## Running

From the repo root: `./run.ps1 old FlappyBird` (or `./run.ps1 old FlappyBird -Build` to build a jar).

Images are loaded by relative path (`bg.jpg`, `bird.png`, ...), so the working directory must be `FlappyBird/src`. The downloadable `FlappyBird.jar` on the site carries them and unpacks them into `~/.oldjava/FlappyBird` before it starts (see [Download .jar](../web/README.md#download-jar)).

The images are original art for this repo (a teal bird, copper pipes, hills at sunrise and a grassy ground strip), drawn as SVG in `art/` and rendered to these files by `web/art.ps1`. They replaced the original Flappy Bird game's art, which the recovered jar had used; the old files remain in the git history.

## Self-check

`test/flappyBird/CourseCheck.java` checks that the same day gives the same pipes and the next day different ones, that only a better run replaces today's ghost, that the ghost fills in the steps between samples and ends with its run, that the course file loads back the same ghost while a file from another day or a broken one is ignored, and the medal names and lines. `test/flappyBird/RewindCheck.java` checks that the rewind tape keeps only its latest moments and gives them back newest first with the path into the crash, that pipes put back from memory scroll on into exactly the same pipes (including pairs not yet made), and that a bird put back flies the same way again. From `FlappyBird/`:

```
javac -d ../out/check/FlappyBird src/flappyBird/*.java test/flappyBird/*.java
java -ea -cp ../out/check/FlappyBird flappyBird.CourseCheck
java -ea -cp ../out/check/FlappyBird flappyBird.RewindCheck
```
