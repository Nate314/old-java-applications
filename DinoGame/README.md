# DinoGame

A bot for a dinosaur jumping game. It watches one screen pixel and presses Space when that pixel's color matches an obstacle. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`), where the jar was named `DinoGame1920x1080.jar`.

The bot now comes with its game. `./run.ps1 old DinoGame` opens Dino Game: a dinosaur runner across a warm desert (its dinosaur and birds are original pixel drawings, with a round head and back spikes; see The desert in color). The game began as a gray copy of Chrome's look; the obstacles still keep blue at exactly 83, the one number the bot knows. It speeds up from 6 to 13 pixels a step, the jump goes higher while the key is held, and birds join once it passes speed 8.5. Press B and the bot plays it, using the original rule unchanged: read the pixel just left of a red marker, and when its blue value is 83 hold the jump key for 65 ms. You can try to beat its best score, or drag its marker somewhere else and see whether that makes it better or worse. The original screen bot is still there (see Original screen bot).

## Controls / usage

- **Space**, **Up** or **W**: start a run, jump. Holding the key longer jumps higher; a tap is a short hop. A click or a tap on the game also jumps, for touch screens.
- **Down** or **S**: duck (under the middle birds). In the air it falls faster.
- **Thumb pads** (a phone held upright): see Thumb pads.
- **B**: the bot on or off. With the bot on, a red 10 x 10 marker shows where it looks ("bot's eye") and a badge counts its jumps this run. From the ready screen it starts the run itself, holding the key for 70 ms as the original did when it launched. After a crash it waits 3 s, then runs again.
- **Drag the red marker** (bot on) to move what the bot watches. It stays ahead of the dinosaur and above the ground. **X** puts it back (36 px ahead of the dinosaur's nose, 22 px above the ground).
- After a crash, Space starts a new run (after a short pause, so a held key does not restart at once).
- **M** (or the **Sound** button in the bottom strip): the game's beeps on or off.
- **G** (or the **Race your ghost** button, once you have run once): race your best run (see Race your ghost). G again, or **New courses**, goes back to a new course each run.
- **P** (or the **Two players** button): two players race on one keyboard (see Two players). P again, or **One player**, goes back.
- **L** (or the **By ear** button in the bottom strip): play by ear, then the blindfold, then off again (see Play by ear).
- **T** (or the **Whole run** button, after a crash): the run that just ended in one picture (see The whole run in one picture). T again (**Back**), or Space for a new run, goes on.
- **E** (or the **Evolve a bot** button at the right of the bottom strip): evolve bots (see Evolve a bot). E again, or **Back to the game**, returns to the ready screen. A run under way when you switch is dropped and counts for nobody.

Score: the distance run. Top right shows the bot's best (red, BOT), your best (HI) and this run.

- **Your best** counts runs the bot never played.
- **The bot's best** counts runs it played from start to end without your help. A run where you pressed a key while it played, or where it was switched on or off part way, is a team run and counts for neither.
- When you crash, the game says what would have got you past (see What would have cleared it).
- When the bot crashes, the game says why, from what happened: its pixel never saw the obstacle (most often a middle bird, which flies above the marker; it cannot duck), it jumped too late (move the marker right), or too early (move it left).

With the marker where it starts, the bot clears cacti and low birds well and usually ends at the first middle bird it meets, usually somewhere between 400 and 650 points.

Saved in `~/.dinogame.txt`: `best=` (yours), `bot=` (the bot's) and `marker=x,y`, `sound=off` while the beeps are off, `ears=cues` or `ears=blindfold` while Play by ear is on, and once a generation has been bred, `evolved=generations,record`, `champion=` (the record holder's six numbers, see below) and `chart=` (the best score of each of the last 60 generations), and `ghost=` (see Race your ghost).

On the web site, `?watch` on its page (and the home page's demo, see Watch the computer play in `web/README.md`) starts it with the bot playing (B), and the visitor's first press takes over.

## Thumb pads

When the screen is narrower than the game (a phone held upright, in the browser), the window is 190 pixels taller and two big pads sit under the bottom strip, so a phone can duck as well as jump:

- **DUCK** on the left: hold it to duck, let go to stand. **JUMP** on the right works like Space: it says **RUN** on the ready screen and **RUN AGAIN** after a crash, and holding it jumps higher.
- On your first runs (see The mark) the pad the mark asks for fills while the mark is lit, and the JUMP pad says **TAP** or **HOLD** as the mark does. A pad also fills while a finger holds it.
- In Two players the pads are **P1 JUMP** and **P2 JUMP** (as a tap on either half of the game). While bots evolve the pads give way to a line pointing at Back to the game.
- A tap on the game itself still jumps. Wider screens, and the desktop, have no pads.

## The mark (your first runs)

Until your best reaches 700 (the first night), a run shows when to act: a small triangle under the ground, just ahead of the dinosaur.

- **Hollow**, labeled "jump" (or "duck" when the next thing to get past is a middle bird): where that cactus or bird should be when you press.
- **Filled** from the moment it gets there, labeled **TAP** (a quick tap: a short hop clears it best), **HOLD** (a wide group of cacti: hold while it is lit) or **DUCK** (hold Down while it is lit, until the bird has passed). It stays lit at least a fifth of a second, so a tap's moment can be seen. High birds, which you run under, get no mark.
- The timing is Play by ear's (`Ears`): the beat to act on, seen instead of heard. In the self-check, a player who only follows the mark (a quick tap on TAP, holding on HOLD, ducking on DUCK) reaches 700 on each of its 10 courses. Timing matters less than a short press: in a trial on 30 courses, players pressing on the mark reached 700 on 29 with quick taps and on 7 with taps of a tenth of a second, which is why it says TAP.
- The ready screen says so: "Jump when a cactus reaches the mark under the ground." When you crash, the coach's "Jump when it reaches the mark" means the same kind of mark (see What would have cleared it).
- Not while the bot plays (it is drawn under the ground, out of the bot's sight anyway), nor with the blindfold, in Two players or in Evolve a bot. Once your best reaches 700 it is gone; nothing new is saved.

## What would have cleared it

When a run of your own ends, GAME OVER says what to change, and the screen shows it:

- **The line** after your score: "Jump when it reaches the mark" (you did not jump near it), "You jumped 0.15 s late" or "0.30 s early", "Right moment: hold the key longer to jump higher" (or "a quick tap lands sooner"), "Duck under that bird: hold ↓ when it reaches the mark", or for a high bird you jumped into, "stay on the ground and run under it".
- **The mark**: the obstacle you hit, drawn see-through where it was when the press that clears it should have come, with a filled triangle under the ground labeled "jump here" (or "duck here"). A hollow triangle labeled "you" shows where it was when you pressed.
- **The slow replay**: a moment later (once the crash shake is over), the last second or so plays again at half speed, and keeps playing: the dinosaur that makes the press that clears it runs solid, labeled "like this", with yours see-through beside it, labeled "you", until it crashes. The mark under the ground is hollow until the right moment and lights with TAP, HOLD or DUCK (as on your first runs) when that press comes. "SLOW REPLAY: HOW TO GET PAST IT" shows top left. When the system asks for reduced motion (in the browser), the still picture above stays instead.
- **On a phone** (with the thumb pads) the line names the pads: "tap JUMP", "hold DUCK", "hold JUMP longer".
- **It is worked out, not guessed** (`Coach`): the run is played again from its course and keys (as a ghost is) up to a moment before the crash, and then other presses are tried there, nearest to yours first: the same press sooner or later, held as you held it, held long or tapped, and a duck for a middle bird. The first that gets the dinosaur past that obstacle is the one shown. It looks back about two thirds of a second (40 steps), so a press that would have to come earlier, or a crash that needed two presses changed (a jump held so long that the next one comes too late), gets no advice, only the score.
- Only for your runs: not the bot's or team runs, two players or evolving bots. Nothing is saved.

## Race your ghost

Every run is now its own course, and the game keeps your best one: its course and each key you pressed, step by step. The game always plays the same way for the same course and the same keys, so playing those keys back runs that run again exactly, jump for jump.

- **G** races it. Every run is then the ghost's course, and the ghost runs beside you as a see-through dinosaur, labeled "ghost" when it is in the air and you are not (or the other way round). Top left shows its score.
- **If the ghost crashes first**, it stays where it fell, with its score, and slides away behind you.
- **If you crash first**, the ghost runs on over the obstacles still on the screen until it is out of sight, so you can watch how it got past the one that stopped you. The crash line says how far it went.
- **Run further and you are the ghost**: the new run replaces it at once (and the next race is against it). A run the bot played or helped with is never kept as a ghost.
- The ghost is your best run since ghosts were kept, so after an update your first run becomes the ghost, whatever your old best. G is in the bottom strip once there is a ghost.
- Runs on the ghost's course count for your best like any other. The ghost is not drawn while the bot plays (B), so the bot's pixel never sees it.

Saved in `~/.dinogame.txt` as `ghost=course,score,` then each input as `step * 4 + key` (0 jump down, 1 jump up, 2 duck down, 3 duck up), only the presses that changed something, so a held key's repeats are not kept.

## Play by ear

The bot plays by watching one pixel. L lets you play by listening instead: every obstacle you must get past sounds as it comes, so the game can be played with your eyes shut, or by someone who cannot see it.

- **Two beats per obstacle**, a quarter of a second apart, like a count-in: a soft one to warn, then a loud one to act on. A **low** pitch (a marimba G3) asks for a jump: every cactus and the low birds. A **high** one (G5) asks for a duck: the middle birds. The high birds, which you run under, are silent.
- **Press on the second beat and hold while it rings.** It rings as long as the key should be held: longer for a wide group of cacti, a short tap for a single small one, and for a duck until the bird has passed.
- The beat to act on is worked out from where the obstacle is and the speed (`Ears`), a little early, so a press that comes a step or two after the beat still clears it.
- Press L again for the **blindfold**: the beats go on, and the cacti and birds are not drawn while a run is under way. The ground, the clouds, the score and the dinosaur stay. At a crash they come back, so you can see what you ran into. The top left says "PLAYING BY EAR" or "BLINDFOLD: LISTEN", and the ready screen says what the beats mean.
- The beats play even with the game's beeps off (M), since they are what you play by. They play only for one player (not in Two players or Evolve a bot), and also while the bot plays, so you can listen along to it.
- Runs played by ear count for your best like any other, and can become your ghost. The bot is never blindfolded: the frame it reads still has every obstacle in it.
- A third press of L turns it off. The setting is saved.

## The whole run in one picture

After a crash, T (or the **Whole run** button in the bottom strip) shows the run that just ended as one picture, like a long exposure photo of it.

- The run is played again from its course and keys (as a ghost is), and every step leaves the dinosaur's outline where it was on the course: a blue ribbon that rises in an arch over each cactus and low bird, dips where you ducked, and stops at a red cross with the score where you crashed.
- The course wraps into rows like lines of text, shrunk as little as lets it fit (a 250 point run fits four rows at a fifth of its size). The number at the left of each row is the score where it starts. Cacti are drawn as blocks, birds as small ovals, and the stretches run at night are shaded.
- The top line counts the jumps and ducks.
- With your ghost raced (G), its run is drawn too, in see-through gray, with the course past your crash that only it reached, so you can see where it went on.
- It works for the bot's runs too: turn the bot off (B) after its crash, then press T. It is not offered while the bot plays, in Two players or in Evolve a bot.
- Nothing is saved; the picture is made again each time.

## Two players

P puts two dinosaurs on the screen for two people at one keyboard (or one tablet).

- **P1** is the teal dinosaur, where yours always stands: **W** or **Space** jumps, **S** ducks. **P2** is the orange one, 70 pixels ahead of it: **Up** jumps, **Down** ducks. On a touch screen (or with a mouse), a tap on the left half of the game jumps P1 and one on the right half P2.
- The first jump key starts both at once, on one new course, without jumping. A Runner plays a course the same way whatever the keys, so both meet the same cacti and birds, P2 a moment before P1.
- A dinosaur that crashes stays where it fell and slides away behind the other, which runs on alone. The screen follows the one in front, with its night and its dust.
- When both have crashed, the one that fell further along the course wins the round: "P1 wins, 764 to 260." Since P2 stands ahead it meets each obstacle first, so two dinosaurs that fall at the same obstacle are a dead heat, whatever their scores. Top right shows both scores in their colors, top left the rounds each has won (kept until P is pressed again or the game is closed, not saved).
- Two-player runs count for nobody's best and never become your ghost. The bot (B) is off while two play; G or E leaves two players.

## The desert in color

The run has its own palette rather than a browser game's gray:

- **The sky** is a warm cream with a low pale sun behind the mesas; under the ground line is sand, with brown pebbles scrolling past.
- **The dinosaur** is deep teal. Words, the score and the marks are a dark warm brown.
- **The obstacles** keep blue at exactly 83 (0x53) whatever their red and green, so the original bot's rule still sees them and nothing else: sage-green cacti (#4E8553) with a lighter rib down the trunk and each arm (#74A653, inside the cactus's own shape), and plum birds (#8E4F53). Everything else the game draws keeps blue off 83, and the self-check holds every color to that by day and through dusk and night.
- In Two players P1 is the teal dinosaur and P2 the orange one; in the whole-run picture cacti are green blocks and birds plum ovals.

## Day and night

A run now has a sky and a feel of its own:

- **Land with depth**: behind the ground, pale flat-topped mesas (some with a narrower butte on top, sunlit on the left and shaded on the right) slide past at an eighth of the run's speed, and low rolling dunes in front of them at a third, so the run reads as a journey across a desert rather than a line. The land comes from the distance alone, so a replay, a ghost's course and the crash coach show the same land. It is only pale rose and gold, never blue 83, so the bot never sees it; at night it turns to dark silhouettes with everything else.
- **Night falls every 700 points** (at 700, 1400, ...) and lasts 12 seconds, with a second of dusk and a second of dawn. Every color turns to its inverse: a navy sky over blue-gray mesas, pale blue words, lavender cacti and a salmon dinosaur. A moon comes out, drifting slowly left, a thinner or fuller one each night (seven phases in turn), with three twinkling stars.
- **The original bot is blind at night.** Its rule is one exact number: jump when the pixel's blue value is 83. At night the ink is blue 172, and the fading colors of dusk and dawn are kept off 83 too, so from the first moment of dusk it sees nothing and runs into the next obstacle. Its crash line says so: "Night fell and the ink turned pale. Its pixel only knows blue 83." With its marker where it starts it seldom gets that far, since a middle bird usually ends its run first. Evolved bots (E) are not affected: they race by day.
- **Every 100 points** the score shows the round hundred and blinks for a second, with a two-note chime.
- **Passing your best** during a run: NEW BEST blinks under the score, with a rising run of notes.
- **Dust** puffs up as the dinosaur takes off and lands, and specks kick up behind it as it runs. It stays behind the dinosaur, out of the bot's sight.
- **Beeps**: a blip for each jump (yours or the bot's), the chime, and a low buzz for a crash. M turns them off and on.
- **A crash** shakes the picture for a moment (only on screen: the frame the bot reads stays still; in the browser, not when the system asks for reduced motion), and once Space can start a new run a round arrow under GAME OVER says so.

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

`./run.ps1 old DinoGame screen` runs the original program, which plays a dinosaur game in another window (such as Chrome's) by reading the real screen:

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
- `src/dinosaurgameplayer/Land.java`: the mesas and dunes behind the run, each at its own pace.
- `src/dinosaurgameplayer/Dust.java`: the dust at the dinosaur's feet.
- `src/dinosaurgameplayer/Sounds.java`: the beeps, on the MIDI synthesizer.
- `src/dinosaurgameplayer/Records.java`: `~/.dinogame.txt`.
- `src/dinosaurgameplayer/Ears.java`: Play by ear (L): which obstacles sound, when each beat is due (and where the first runs' mark sits), how long to hold, and the saved setting. `Sounds.beat` plays them.
- `src/dinosaurgameplayer/Duel.java`: two players (P): the second dinosaur's `Runner`, starting both on one course, which one the screen follows, and the rounds won.
- `src/dinosaurgameplayer/Ghost.java`: a run kept to race: its course and inputs, played back on a `Runner` of its own. `Runner` picks each run's course and logs its inputs.
- `src/dinosaurgameplayer/Coach.java`: what would have cleared the obstacle you crashed into: plays the run again with other presses near the crash, and the line and mark it gives.
- `src/dinosaurgameplayer/RunPicture.java`: the whole run in one picture (T): plays a run again from its keys, keeps the dinosaur's outline and every obstacle by place on the course, and draws them in rows.
- `src/dinosaurgameplayer/Brain.java`: an evolved bot's six numbers, its children and its saved form.
- `src/dinosaurgameplayer/Evolution.java`: a generation of 20 dinosaurs (`Evolution.Dino`, a brain at the keys of its own `Runner`), ranking and breeding.
- `src/dinosaurgameplayer/EvolveView.java`: draws the evolution: ghosts, eyes, badge, record and chart.
- `src/dinosaurgameplayer/DinosaurGamePlayer.java`: the original screen bot's entry; computes the sensor positions, waits 2.5 s on the main thread, then on the Swing event thread shows the markers, presses Space and starts the timer.
- `src/dinosaurgameplayer/ObstacleDetector.java`: the original timer action; reads the sensor pixel with `java.awt.Robot` and holds Space, released by a one-shot timer. The pixel is not checked while Space is held. Its rule (blue 83, 65 ms, 70 ms to start) is shared with `PixelBot`.

## Running

From the repo root: `./run.ps1 old DinoGame` (or `./run.ps1 old DinoGame -Build` to build a jar). `./run.ps1 old DinoGame screen` for the original screen bot. It also runs in the browser (see [web/README.md](../web/README.md)).

## Self-check

`test/dinosaurgameplayer/RunnerCheck.java` checks the bot's color rule, that a tap jumps lower than a held key, ducking, that a seed replays the same game, that the bot scores over 300 on six seeds with its marker where it starts and explains each crash, that a marker far to the right does worse, that your runs count for your best while a team run counts for nobody, and the saved file (with `sound=off`). For day and night: that night falls at 700 and 1400 and dawns 12 s later, that no color the game draws past the first moment of dusk has blue 83, that a course drawn through a whole night shows the bot no obstacle while the same course by day is full of them, the night crash line, and that the dust rises on take-off and landing, stays behind the marker and settles. For the evolution: that `Runner.inkAt` agrees with the drawn frame on every pixel an eye can sit on, through a whole course with birds; that a known brain ducks and finishes a course; that a seeded 40-generation run starts with the original bot, keeps a chart of 40 bests and ends with a record over twice the original's score held by a brain that ducks; that it carries on from a saved champion; that damaged brains are refused and numbers kept in range; that in the window E drops the run under way and each generation is saved; and the saved evolution lines. For the ghost: that a run played by keys is kept as the ghost and its replay crashes at the same step with the same score, that the saved form reads back and damaged ones are refused, that the next run is a new course, that G drops the run under way and races on the ghost's course, that the ghost runs on after you crash and a worse run keeps it, and that G again goes back to new courses. For two players: that P turns the bot off, P2 stands clear of P1, one key starts both on one course without a jump, W jumps only P1, the further one wins the round with the right line, two that fall at the same obstacle are a dead heat, nothing is saved, a held key does not start the next round at once, the next round is a new course, and P again goes back to one player. For Play by ear: that a player who only listens (pressing on each beat to act on and holding as long as it rings, never looking) averages over 1,000 on 40 courses, and still does when a step or two late; that each cactus and low bird sounds a low warning and then a low beat 14 or 15 steps later, a middle bird high ones and a high bird none, always before the obstacle arrives; that L goes from off to beats to the blindfold and back and is saved (and off is not written); and that the blindfold leaves the obstacles off the screen during a run, even while the bot plays (which still sees them and runs on), and shows them again at the crash. For the whole run in one picture: that T after a run played by keys replays it to the same score and step count with its jumps and obstacles, that its rows fit the game and the crash lands on the last row, that the ribbon is drawn, that T again goes back and nothing shows while the bot plays, that a raced ghost's run is in it, further along, without moving the ghost on screen, and that Space closes it and starts a new run. For what would have cleared it: on 30 courses, a run with no key after the start is told to jump at the mark, and that jump, played with the keys, gets past the cactus; a press 2 steps before the crash is called late and one 38 steps before early, with a sooner or later step; a player who never ducks or jumps birds is told to duck at each middle bird that ends a run (8 of 80 courses), and the duck clears it; the line joins the score in the window and the mark is drawn under the ground; the slow replay comes after the shake, its mark hollow until the fix presses and lit after, the fix gets past the obstacle and the replay starts over; with the thumb pads the line says "tap JUMP"; the bot's runs get none; and it prints how long the coach took after a long run. For the mark: on 10 courses, a player who only follows it (a quick tap when it lights with TAP, holding while it says HOLD, ducking while it says DUCK) reaches 700 on at least 8 (it prints how many), the lit mark is drawn filled, the ready screen shows it unlit, and it never shows for the bot or once your best is 700. For the thumb pads: that the window has none unless asked, that RUN starts a run, DUCK ducks while held and lets go, JUMP jumps, the lit mark fills its pad and leaves the other paper, RUN AGAIN runs again after a crash, and in two players either pad starts both and the left one jumps only P1. For the land: that the mesas and dunes are drawn, never in a color the bot sees, and that 800 along the ground moves the mesas 100. For the colors: that by day only the cacti, their ribs and the birds have blue 83, and none of the game's colors does from the first moment of dusk. From `DinoGame/`:

```
javac -encoding UTF-8 -d ../out/check/DinoGame src/dinosaurgameplayer/*.java test/dinosaurgameplayer/*.java
java -ea -Djava.awt.headless=true -cp ../out/check/DinoGame dinosaurgameplayer.RunnerCheck
```
