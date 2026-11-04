# SpriteFloatyStars

A background program written in Java that animates colored dots ("stars") on a blue field. A small settings window picks the mode, window size, star count and star size, then opens the animation. The stars in that window now answer the mouse: hold to pull them all in, let go to fling them out (see Playing with the stars). A sixth mode, **Orbits**, turns the field into a small galaxy: the stars fall round suns you place and drag. It also has **Star Catcher**, a game played over those same animations.

## Playing with the stars

The original animation only played to itself. Now the mouse (or a finger, in the browser) reaches into it:

- **Hold** the button anywhere on the field: every star is pulled toward the pointer. The stars keep their own drift, so they swing through the pointer and swarm round it, under a glowing ring that grows while you hold. Drag to lead the swarm round the window.
- **Let go**: the stars near the pointer burst away from it, harder the longer you held (full strength after 1.5 seconds), and a ring spreads out from the spot. Then each star glides back to its own drift.
- **Click** (press and let go at once): a small burst.
- **Rest** the pointer on the field: the stars near it slide out of its way.
- In the bouncing modes (2 to 4) a flung star bounces off the edges; in the drifting modes (1 and 5) it wraps round to the other side.
- **1** to **6** (or the number pad): start the stars over in that mode, with the same number of stars, sized to the window as it is now. **N**: the next mode. The mode's name shows in the top left corner for a moment ("Mode 3: Compass", by the names Star Catcher gives its nights).
- **Space**: a burst from the middle of the field.
- **T**: trails on or off, in any mode. With trails on, each frame fades the last one instead of clearing it, so every star draws the path it took, fading in about a second.
- **H**: show or hide the lines of help along the bottom. They show when the window opens (and when Orbits starts) and fade after 9 seconds.

## Orbits (Mode 6)

The five original modes move each star in a straight line. Orbits gives them gravity. It opens on a dark night sky with one sun in the middle and every star on a circle round it, going round at the speed that keeps it on that circle: the inner stars fast and white hot, the outer ones slower, through blue-white, gold and orange to rose and violet. Trails come on with it, so the circles draw themselves.

- **Click** anywhere: a new sun grows in there. Every star feels every sun, so the neat circles bend into loops and tangles, and stars pass from one sun to another. Up to four suns: a fifth takes the place of the oldest.
- **Drag a sun** to move it; the stars round it follow, and the ones it passes are pulled along. Let go of it at (or past) the edge of the window to take it away (it turns red when letting go would do that). On the desktop a right click on a sun takes it away too.
- **Hold** on empty sky: the pointer pulls as in the other modes, but there are no brakes in space, so the stars keep what they gain and swing round in new orbits. Letting go after a hold, and **Space**, burst at about a third of their strength in the other modes.
- A star that falls into a sun, or is flung far off, starts again on a circle round one of the suns, so the number of stars stays the same. With no suns left the stars drift and wrap round the edges until a click places one.
- Stars are drawn a third of the Size setting across in this mode (at least 2 px), so a thousand of them still show their paths. An orbit takes about as long in any window size (gravity grows with the window).

## Star Catcher

**Play Star Catcher** at the top of the settings window opens the game in its own window (640 x 480) and hides the settings window until the game window is closed.

Click the gold stars before the clock runs out, and stay off the black holes. Each night starts with 30 seconds and asks for a number of gold stars; a few are on the field at a time, and each one caught is replaced somewhere else. Every night flies over one of the original five animation modes, drawn small and dim as its sky:

- **1. Drift** (Mode 1 sky): catch 6, no black holes.
- **2. Burst** (Mode 2): catch 8, 1 black hole. Black holes cost 3 seconds.
- **3. Compass** (Mode 3): catch 10, 2 black holes, faster stars.
- **4. Confetti** (Mode 4): catch 12, 3 black holes. Stars twinkle out after 5 seconds and appear elsewhere.
- **5. Flock** (Mode 5): catch 12, 3 black holes, two stars at a time, twinkling out after 4 seconds.
- **6. Supernova** (Mode 2): catch 15, 4 black holes that pull the stars in and swallow them.

- A click within 8 px of a gold star catches it, even if it also touches a black hole. A click on a black hole takes 3 seconds off the clock.
- Catches each within 1.5 seconds of the last make a combo: from the third catch in a row on, each one adds half a second to the clock. A miss or a black hole ends it.
- Catching a night with 10 or more seconds left is three stars, with 5 or more two stars, and in time at all one star. Catching a night opens the next one.
- The menu shows the six nights as cards with their stars and best time left (locked nights show a padlock). The clock bar in the game is marked at 10 and 5 seconds, and turns orange and then red as it passes them.
- After a night, a card shows the stars won (with "New best!" when more time was left than before), and Again, Next night and Nights.

The most time left for each night caught is saved to `.spritefloatystars-nights.txt` in the user's home folder, one `NIGHT=stars,tenths` line per night (for example `DRIFT=3,142` for three stars and 14.2 seconds left).

## Controls / usage

Settings window:

- **Play Star Catcher**: open the game (see above).
- **Mode**: Mode 1 to Mode 6.
  - Mode 1: white stars at random positions drift left and down, wrapping at the edges.
  - Mode 2: stars burst from the center and bounce off the edges, colored by position.
  - Mode 3: same burst and bounce, colored by direction (red, green, magenta or cyan).
  - Mode 4: stars start at random positions and bounce off the edges, each in its own random color.
  - Mode 5: like Mode 1, but overlapping stars average their velocities so they drift together.
  - Mode 6: Orbits (see above), new.
- **X/Y**: animation window width and height (default 800 x 800).
- **Number**: star count (default 1000, any size).
- **Size**: star diameter in px (default 20).
- **OK**: open a new animation window with those settings. Each click opens another window. If a field is not a positive whole number, the settings title says so and no window opens.

Animation window:

- **Up / Down arrows**: add or remove one star.
- **Mouse**: hold to pull, let go to fling, click to burst (in Orbits: click to place a sun, drag a sun to move it); **1** to **6**, **N**, **T**, **Space** and **H** as in Playing with the stars.

Star Catcher window:

- **Mouse**: click a night's card to play it; click gold stars; click the buttons on the result card.
- **Arrow keys**: pick a night on the menu; in a night, move a net (a ring with crosshairs) around the field. **Space** or **Enter** catches under the net.
- **1** to **6**: play that night, if it is open. **Enter**, **Space** or **N** on the menu plays the chosen night; after a night they play the next one (or the same one again if the next is still locked).
- **R**: play the night again. **Esc** or **M**: back to the menu.

Closing the settings window or an animation window exits the program; closing the Star Catcher window brings the settings window back. No command-line arguments.

## Code layout

- `src/animate2/Animate2Main.java`: entry point; shows the `SettingsFrame`.
- `src/animate2/SettingsFrame.java`: the settings window; parses the fields and opens an `AnimationFrame`, or a `CatchFrame`.
- `src/animate2/AnimationFrame.java`: the animation window holding a `StarField`.
- `src/animate2/StarField.java`: star positions, velocities and colors; moves them on a 15 ms Swing timer (scaled by the time since the last tick, so the speed does not depend on the machine) and draws them for each mode. Each star also has a kick from the pointer (the pull, the wake and bursts) that dies away by itself, and the field draws the pull's glow, the burst ring, the mode name and the help. Star Catcher makes one without a timer or pointer and moves and draws it as its sky.
- `src/animate2/StarHand.java`: the mouse on an animation window's field: press starts the pull, release flings, moving sets where the pointer rests; in Orbits, a press on a sun drags it.
- `src/animate2/Orbits.java`: Mode 6: the suns, gravity on every star (in steps of at most one, with a speed cap and softened close passes), stars starting again after falling in or flying off, and the star and sun drawing.
- `src/animate2/Night.java`: the six nights (sky mode, goal, gold stars, black holes, speed, twinkle and pull) and the stars for the time left.
- `src/animate2/CatchGame.java`: one night with no drawing: moving and bouncing, catching, black holes, the clock and combos.
- `src/animate2/NightRecords.java`: reads and writes the most time left per night, and which nights are open.
- `src/animate2/CatchPanel.java`: draws the menu, the night, the effects and the result card, and handles the mouse, keys and net.
- `src/animate2/CatchFrame.java`: the Star Catcher window.

## Running

From the repo root: `./run.ps1 old SpriteFloatyStars` (or `./run.ps1 old SpriteFloatyStars -Build` to build a jar).

## Self-check

`test/animate2/CatchCheck.java` checks the stars for the time left, that every night starts with its gold stars and black holes and keeps them inside the field until the clock runs out, that stars twinkle out only on the nights that say so and Supernova's holes swallow stars, catching within reach, combos and their bonus, black holes costing time (never below zero), and that the saved records keep the most time left, skip bad lines and open nights in order. `test/animate2/StarHandCheck.java` checks, in every mode, that holding gathers the stars, letting go spreads them and they all end up back on the field, that a click bursts near stars away, a longer hold flings harder (up to a cap), the wake nudges a star, mode changes keep the stars (Burst mode starts them in the middle) and wrap round, and the help shows, hides and fades. `test/animate2/OrbitsCheck.java` checks that Orbits starts with one sun and the trails on, that a lone sun's stars keep their distance while going round, that a click places a sun but a hold or a drag does not, the four-sun limit, dragging a sun, taking one away at the edge or by right click, a sun swallowing a star that then starts again, wrapping with no suns, the T key, and that the sun and both skies are drawn. From `SpriteFloatyStars/`:

```powershell
javac -d ../out/check/SpriteFloatyStars src/animate2/*.java test/animate2/*.java
java -ea -cp ../out/check/SpriteFloatyStars animate2.CatchCheck
java -ea -cp ../out/check/SpriteFloatyStars animate2.StarHandCheck
java -ea -cp ../out/check/SpriteFloatyStars animate2.OrbitsCheck
```
