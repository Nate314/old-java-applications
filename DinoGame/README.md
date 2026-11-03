# DinoGame

A bot for a dinosaur jumping game. It watches one screen pixel and presses Space when that pixel's color matches an obstacle. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`), where the jar was named `DinoGame1920x1080.jar`.

The bot now comes with its game. `./run.ps1 DinoGame` opens Dino Game: a dinosaur runner modeled on Chrome's game (its dinosaur and birds are original pixel drawings, with a round head and back spikes), drawn in gray #535353 (red, green and blue all 83) on an off-white ground. It speeds up from 6 to 13 pixels a step, the jump goes higher while the key is held, and birds join once it passes speed 8.5. Press B and the bot plays it, using the original rule unchanged: read the pixel just left of a red marker, and when its blue value is 83 hold the jump key for 65 ms. You can try to beat its best score, or drag its marker somewhere else and see whether that makes it better or worse. The original screen bot is still there (see Original screen bot).

## Controls / usage

- **Space**, **Up** or **W**: start a run, jump. Holding the key longer jumps higher; a tap is a short hop. A click or a tap on the game also jumps, for touch screens.
- **Down** or **S**: duck (under the middle birds). In the air it falls faster.
- **B**: the bot on or off. With the bot on, a red 10 x 10 marker shows where it looks ("bot's eye") and a badge counts its jumps this run. From the ready screen it starts the run itself, holding the key for 70 ms as the original did when it launched. After a crash it waits 3 s, then runs again.
- **Drag the red marker** (bot on) to move what the bot watches. It stays ahead of the dinosaur and above the ground. **X** puts it back (36 px ahead of the dinosaur's nose, 22 px above the ground).
- After a crash, Space starts a new run (after a short pause, so a held key does not restart at once).
- **M** (or the **Sound** button in the bottom strip): the game's beeps on or off.
- **E** (or the **Evolve a bot** button at the right of the bottom strip): evolve bots (see Evolve a bot). E again, or **Back to the game**, returns to the ready screen. A run under way when you switch is dropped and counts for nobody.

Score: the distance run. Top right shows the bot's best (red, BOT), your best (HI) and this run.

- **Your best** counts runs the bot never played.
- **The bot's best** counts runs it played from start to end without your help. A run where you pressed a key while it played, or where it was switched on or off part way, is a team run and counts for neither.
- When the bot crashes, the game says why, from what happened: its pixel never saw the obstacle (most often a middle bird, which flies above the marker; it cannot duck), it jumped too late (move the marker right), or too early (move it left).

With the marker where it starts, the bot clears cacti and low birds well and usually ends at the first middle bird it meets, usually somewhere between 400 and 650 points.

Saved in `~/.dinogame.txt`: `best=` (yours), `bot=` (the bot's) and `marker=x,y`, `sound=off` while the beeps are off, and once a generation has been bred, `evolved=generations,record`, `champion=` (the record holder's six numbers, see below) and `chart=` (the best score of each of the last 60 generations).

On the web site, `?watch` on its page (and the home page's demo, see Watch the computer play in `web/README.md`) starts it with the bot playing (B), and the visitor's first press takes over.

## Day and night

A run now has a sky and a feel of its own:

- **Night falls every 700 points** (at 700, 1400, ...) and lasts 12 seconds, with a second of dusk and a second of dawn. Every color turns to its inverse: black paper, pale gray ink, light clouds. A moon comes out, drifting slowly left, a thinner or fuller one each night (seven phases in turn), with three twinkling stars.
- **The original bot is blind at night.** Its rule is one exact number: jump when the pixel's blue value is 83. At night the ink is blue 172, and the fading colors of dusk and dawn are kept off 83 too, so from the first moment of dusk it sees nothing and runs into the next obstacle. Its crash line says so: "Night fell and the ink turned pale. Its pixel only knows blue 83." With its marker where it starts it seldom gets that far, since a middle bird usually ends its run first. Evolved bots (E) are not affected: they race by day.
- **Every 100 points** the score shows the round hundred and blinks for a second, with a two-note chime.
- **Passing your best** during a run: NEW BEST blinks under the score, with a rising run of notes.
- **Dust** puffs up as the dinosaur takes off and lands, and specks kick up behind it as it runs. It stays behind the dinosaur, out of the bot's sight.
- **Beeps**: a blip for each jump (yours or the bot's), the chime, and a low buzz for a crash. M turns them off and on.
- **A crash** shakes the picture for a moment (only on screen: the frame the bot reads stays still), and once Space can start a new run a round arrow under GAME OVER says so.

## Evolve a bot

The original bot has two numbers someone once picked by hand: where its pixel sits and how long it holds the key. Press E and the game picks them by evolution instead, while you watch.

- A bot's brain is the original rule with its numbers left open: a **jump eye** (a pixel) and how long to hold the jump key, 20 to 300 ms, when an obstacle covers it, and optionally a **duck eye** and how long to hold Down when an obstacle covers that one. The original bot is the brain with its own pixel, 65 ms and no duck eye. Two small changes from the original rule: an eye does not fire while the dinosaur is in the air (so a bot does not waste its look on a jump that cannot happen), and a jump lets go of Down.
- A **generation** is 20 dinosaurs racing the same course at once, each run by its own brain. The first has the original bot and 19 random brains. Every dinosaur still running is drawn as a see-through ghost in its own color, with small squares where its eyes are (red jumps, blue ducks). The first one still running in the generation's order (the best of the last generation, or the original bot in the first) is drawn solid, with its eyes labeled, and the line under the ground says which it is.
- When the last one crashes, or the ones left reach 3,000 (the finish line), the generation's result shows for 1.5 s: the best score, whether it is a new record, and how many ducked or finished. Then the five that ran furthest go on unchanged, 13 children of the best eight join them (each number taken from one parent or the other, a third of them nudged; one child in ten gains or loses its duck eye), and two new random brains keep fresh ideas coming. Each generation runs a new course.
- The badge (top left) shows the generation, how many are still running and the speed. Top right: the record in green (REC) and how far the race has got. Under it, a bar for each generation's best, the record's bar green.
- **F** or **Speed**: 1x, 4x (where it starts) or 16x. The result between generations always shows at normal speed.
- **N** or **Start over**, pressed twice within 3 s: forgets the evolution and starts again from generation 1.
- The bots read what is drawn without drawing it: `Runner.inkAt` works out from the sprite drawings whether an obstacle covers a pixel, so 20 bots cost no more than one. They see only obstacles, not each other, the dinosaur, the score or a marker. The self-check compares this with the drawn frame pixel by pixel.
- Each generation is saved as it ends. E carries on from the saved record holder: its next generation is that brain, 14 of its children and five random brains.
- Evolution is luck as well as selection. In the self-check's seeded run the original bot ran 507 in generation 1, and after 40 generations the record was 3,000 (the finish line), held by a brain that ducks under the middle birds. Other runs can take longer to find ducking; some stay near the original's score for many generations.

Evolved bots stay in evolve mode; the bot behind B is still the original, with your marker.

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
- `src/dinosaurgameplayer/Sprites.java`: the pixel drawings (dinosaur, ducking, birds, clouds, cacti). The dinosaur and birds were redrawn as original art; they had been close copies of Chrome's.
- `src/dinosaurgameplayer/Sky.java`: day and night on a run: when night falls, how dark it is, the moon's phase, and the colors as night turns them (kept off blue 83 once dusk starts).
- `src/dinosaurgameplayer/Dust.java`: the dust at the dinosaur's feet.
- `src/dinosaurgameplayer/Sounds.java`: the beeps, on the MIDI synthesizer.
- `src/dinosaurgameplayer/Records.java`: `~/.dinogame.txt`.
- `src/dinosaurgameplayer/Brain.java`: an evolved bot's six numbers, its children and its saved form.
- `src/dinosaurgameplayer/Evolution.java`: a generation of 20 dinosaurs (`Evolution.Dino`, a brain at the keys of its own `Runner`), ranking and breeding.
- `src/dinosaurgameplayer/EvolveView.java`: draws the evolution: ghosts, eyes, badge, record and chart.
- `src/dinosaurgameplayer/DinosaurGamePlayer.java`: the original screen bot's entry; computes the sensor positions, waits 2.5 s on the main thread, then on the Swing event thread shows the markers, presses Space and starts the timer.
- `src/dinosaurgameplayer/ObstacleDetector.java`: the original timer action; reads the sensor pixel with `java.awt.Robot` and holds Space, released by a one-shot timer. The pixel is not checked while Space is held. Its rule (blue 83, 65 ms, 70 ms to start) is shared with `PixelBot`.

## Running

From the repo root: `./run.ps1 DinoGame` (or `./run.ps1 DinoGame -Build` to build a jar). `./run.ps1 DinoGame screen` for the original screen bot. It also runs in the browser (see [web/README.md](../web/README.md)).

## Self-check

`test/dinosaurgameplayer/RunnerCheck.java` checks the bot's color rule, that a tap jumps lower than a held key, ducking, that a seed replays the same game, that the bot scores over 300 on six seeds with its marker where it starts and explains each crash, that a marker far to the right does worse, that your runs count for your best while a team run counts for nobody, and the saved file (with `sound=off`). For day and night: that night falls at 700 and 1400 and dawns 12 s later, that no color the game draws past the first moment of dusk has blue 83, that a course drawn through a whole night shows the bot no obstacle while the same course by day is full of them, the night crash line, and that the dust rises on take-off and landing, stays behind the marker and settles. For the evolution: that `Runner.inkAt` agrees with the drawn frame on every pixel an eye can sit on, through a whole course with birds; that a known brain ducks and finishes a course; that a seeded 40-generation run starts with the original bot, keeps a chart of 40 bests and ends with a record over twice the original's score held by a brain that ducks; that it carries on from a saved champion; that damaged brains are refused and numbers kept in range; that in the window E drops the run under way and each generation is saved; and the saved evolution lines. From `DinoGame/`:

```
javac -encoding UTF-8 -d ../out/check/DinoGame src/dinosaurgameplayer/*.java test/dinosaurgameplayer/*.java
java -ea -Djava.awt.headless=true -cp ../out/check/DinoGame dinosaurgameplayer.RunnerCheck
```
