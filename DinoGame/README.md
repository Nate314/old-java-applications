# DinoGame

A bot for a dinosaur jumping game. It watches one screen pixel and presses Space when that pixel's color matches an obstacle. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`), where the jar was named `DinoGame1920x1080.jar`.

The bot now comes with its game. `./run.ps1 DinoGame` opens Dino Game: a dinosaur runner modeled on Chrome's, drawn in gray #535353 (red, green and blue all 83) on an off-white ground. It speeds up from 6 to 13 pixels a step, the jump goes higher while the key is held, and birds join once it passes speed 8.5. Press B and the bot plays it, using the original rule unchanged: read the pixel just left of a red marker, and when its blue value is 83 hold the jump key for 65 ms. You can try to beat its best score, or drag its marker somewhere else and see whether that makes it better or worse. The original screen bot is still there (see Original screen bot).

## Controls / usage

- **Space**, **Up** or **W**: start a run, jump. Holding the key longer jumps higher; a tap is a short hop. A click or a tap on the game also jumps, for touch screens.
- **Down** or **S**: duck (under the middle birds). In the air it falls faster.
- **B**: the bot on or off. With the bot on, a red 10 x 10 marker shows where it looks ("bot's eye") and a badge counts its jumps this run. From the ready screen it starts the run itself, holding the key for 70 ms as the original did when it launched. After a crash it waits 3 s, then runs again.
- **Drag the red marker** (bot on) to move what the bot watches. It stays ahead of the dinosaur and above the ground. **X** puts it back (36 px ahead of the dinosaur's nose, 22 px above the ground).
- After a crash, Space starts a new run (after a short pause, so a held key does not restart at once).

Score: the distance run. Top right shows the bot's best (red, BOT), your best (HI) and this run.

- **Your best** counts runs the bot never played.
- **The bot's best** counts runs it played from start to end without your help. A run where you pressed a key while it played, or where it was switched on or off part way, is a team run and counts for neither.
- When the bot crashes, the game says why, from what happened: its pixel never saw the obstacle (most often a middle bird, which flies above the marker; it cannot duck), it jumped too late (move the marker right), or too early (move it left).

With the marker where it starts, the bot clears cacti and low birds well and usually ends at the first middle bird it meets, usually somewhere between 400 and 650 points.

Saved in `~/.dinogame.txt`: `best=` (yours), `bot=` (the bot's) and `marker=x,y`.

On the web site, `?watch` on its page (and the home page's demo, see Watch the computer play in `web/README.md`) starts it with the bot playing (B), and the visitor's first press takes over.

## Original screen bot

`./run.ps1 DinoGame screen` runs the original program, which plays a dinosaur game in another window (such as Chrome's) by reading the real screen:

- Launch it with the game visible on screen. After 2.5 s it shows two 10x10 always-on-top marker squares, presses Space once to start the game, then checks the sensor pixel every 1 ms.
- **Red marker**: the sensor, at (`screenWidth / 2.44`, `screenHeight / 4.5`). The pixel it reads is one to the left of the marker, so the marker does not cover it.
- **Green marker**: at (`screenWidth / 2.44`, `screenHeight / 5.35`). It is only drawn; nothing reads that position.
- When the sensor pixel's blue value is exactly 83, it holds Space for 65 ms.
- **Click either marker** to stop the bot. The markers have no title bar. Stopping the process (for example Ctrl+C in the terminal that launched it) also works.

It sends real Space key presses via `java.awt.Robot` to whatever window has focus, starting about 2.5 s after launch. Sensor positions are fixed fractions of the full screen size, so they only line up if the game sits at the matching spot on screen. The original jar name suggests it was set up for a 1920x1080 screen. Only the blue channel is checked, so any color with blue 83 triggers a jump.

## Code layout

- `src/dinosaurgameplayer/Main.java`: entry point; opens the game window, or with the argument `screen` runs `DinosaurGamePlayer`.
- `src/dinosaurgameplayer/Runner.java`: the game without drawing: the dinosaur's jump and duck, obstacles (small and large cactus groups, birds at three heights), gaps that widen with speed, collisions. One step is 1/60 s, and a seeded game always plays the same way.
- `src/dinosaurgameplayer/RunnerPanel.java`: draws the game, takes keys and mouse, counts runs for you or the bot. With the bot on, each step is also drawn into an image, and the bot reads its pixel from that image.
- `src/dinosaurgameplayer/PixelBot.java`: the bot inside the window: the marker, the 65 ms hold, and why a run ended.
- `src/dinosaurgameplayer/Sprites.java`: the pixel drawings (dinosaur, ducking, birds, clouds, cacti).
- `src/dinosaurgameplayer/Records.java`: `~/.dinogame.txt`.
- `src/dinosaurgameplayer/DinosaurGamePlayer.java`: the original screen bot's entry; computes the sensor positions, waits 2.5 s on the main thread, then on the Swing event thread shows the markers, presses Space and starts the timer.
- `src/dinosaurgameplayer/ObstacleDetector.java`: the original timer action; reads the sensor pixel with `java.awt.Robot` and holds Space, released by a one-shot timer. The pixel is not checked while Space is held. Its rule (blue 83, 65 ms, 70 ms to start) is shared with `PixelBot`.

## Running

From the repo root: `./run.ps1 DinoGame` (or `./run.ps1 DinoGame -Build` to build a jar). `./run.ps1 DinoGame screen` for the original screen bot. It also runs in the browser (see [web/README.md](../web/README.md)).

## Self-check

`test/dinosaurgameplayer/RunnerCheck.java` checks the bot's color rule, that a tap jumps lower than a held key, ducking, that a seed replays the same game, that the bot scores over 300 on six seeds with its marker where it starts and explains each crash, that a marker far to the right does worse, that your runs count for your best while a team run counts for nobody, and the saved file. From `DinoGame/`:

```
javac -encoding UTF-8 -d ../out/check/DinoGame src/dinosaurgameplayer/*.java test/dinosaurgameplayer/*.java
java -ea -Djava.awt.headless=true -cp ../out/check/DinoGame dinosaurgameplayer.RunnerCheck
```
