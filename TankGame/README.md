# TankGame

A tank duel, against the computer or for two players on one screen. Each player drives a square tank with a barrel over a blue field and fires shells; three hits destroy a tank, and the first to win three rounds wins the match. Original Eclipse source from the author's own files (only `src/` was kept).

The original was unfinished: tanks could drive and fire, but shells passed through tanks and there were no hits, damage or other rules, and the top of the window showed each tank's X, Y and rotation. The rules, the computer opponent, the score bar and the banners were added later.

## Controls / usage

- **Red tank**: `W` forward, `S` back, `A` rotate counterclockwise, `D` rotate clockwise, `Q` fire.
- **Green tank**: `Up` forward, `Down` back, `Left` rotate counterclockwise, `Right` rotate clockwise, `M` fire.
- **Picking the opponent**: the game opens on a title card. Press `Q` (red's fire) to play red against the computer, or `M` (green's fire) for two players. The same keys start the next match from the match-over screen.
- Each held key moves 5 px or turns 5 degrees per game tick (about 60 per second). Tanks stay inside the window and cannot drive through each other.
- Each fire key press shoots one shell from the end of the barrel; holding the key does not repeat, and a tank reloads for about 0.4 s between shots. Shells bounce off the edge of the field once, then fly out. A shell never hurts the tank that fired it.
- A hit takes one of the tank's three health squares, knocks it back a little and makes it flash; the third hit destroys it in an explosion and wins the round. If both tanks fall on the same tick, nobody scores and the round is played again.
- Each round opens with its number and "FIGHT!" while the tanks wait. The score bar at the top shows each tank's name, health and rounds won (yellow circles).
- **The computer** drives the green tank with the same speed, turning rate and reload as a player. It aims where the red tank will be when the shell arrives, keeps a middle distance, drives or backs out of the way of shells it notices, and fires when its aim is close. It looks again about six times a second, aims within about 10 degrees, waits about half a second before pulling the trigger, and notices about two shells in three, so it can be beaten.
- Matches won and lost against the computer are stored in `.tankgame-record.txt` in your home folder and shown on the title card and the match-over screen. Two-player matches are not recorded.
- The window is 500x500 px (narrower if the screen is narrower than 500 px). Title "TankGame".
- No command-line arguments.

## Code layout

- `src/tankgame/Main.java`: entry point and window; makes the two tanks with their keys, the computer and the record, and adds the game panel.
- `src/tankgame/Battle.java`: the rules, one tick at a time: picking the opponent, rounds, hits, the score, the match result and the record. It does not use Swing, so the self-check plays whole matches.
- `src/tankgame/GamePanel.java`: runs the battle on a Swing timer, and paints the field, the tanks, the shells, the sparks, the score bar and the banners.
- `src/tankgame/Tank.java`: one tank; moves, turns, fires, takes hits and draws itself, driven by its `Controls`.
- `src/tankgame/Controls.java`: what drives a tank (forward, back, left, right, fire).
- `src/tankgame/KeyControls.java`: a player's five keys.
- `src/tankgame/ComputerControls.java`: the computer opponent.
- `src/tankgame/Shell.java`: a fired shell; moves, bounces and tells whether it hit a tank.
- `src/tankgame/Spark.java`: the fading sparks of hits and explosions.
- `src/tankgame/Record.java`: reads and writes the record against the computer.
- `src/tankgame/MyKeyListener.java`: tracks whether one key is held and whether it was just pressed; one is added to the frame per key.

## Running

From the repo root: `./run.ps1 TankGame` (or `./run.ps1 TankGame -Build` to build a jar).

## Self-check

`test/tankgame/BattleCheck.java` checks the hit box of a turned tank, that shells hit only the other tank and bounce once, that tanks cannot drive through each other, the reload time, a whole two-player match (round starts, three hits per round, first to three, the match-over pause, two-player matches not recorded), a match against the computer that is saved to the record file, that the computer destroys a still target, and that six computer-against-computer matches all finish with neither side winning every one. From `TankGame/`:

```
javac -d ../out/check/TankGame src/tankgame/*.java test/tankgame/*.java
java -ea -cp ../out/check/TankGame tankgame.BattleCheck
```

## Known quirks

- The clamp keeps the tank's unrotated square inside the window, so the corners of a turned tank can poke past the edge.
