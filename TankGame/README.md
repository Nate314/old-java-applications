# TankGame

An unfinished two-player tank game. Each player drives a square tank with a barrel over a blue background and can fire shells; hits, damage and any other game rules were never written. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- **Red tank**: `W` forward, `S` back, `A` rotate counterclockwise, `D` rotate clockwise, `Q` fire.
- **Green tank**: `Up` forward, `Down` back, `Left` rotate counterclockwise, `Right` rotate clockwise, `M` fire.
- Each held key moves 5 px or turns 5 degrees per game tick (about 60 per second). Tanks stay inside the window.
- Each fire key press shoots one shell from the end of the barrel; holding the key does not repeat. Shells fly straight until they leave the window and pass through tanks.
- The red tank starts on the left facing right, the green tank on the right facing left.
- Each tank's X, Y and rotation are shown at the top, red on the left and green on the right.
- The window is 500x500 px. Title "TankGame".
- No command-line arguments.

## Code layout

- `src/tankgame/Main.java`: entry point and window; adds the game panel and the two tanks with their keys.
- `src/tankgame/GamePanel.java`: runs the game tick on a Swing timer, and paints the blue background, the tanks and the shells.
- `src/tankgame/Tank.java`: one tank; reads its key states each tick, moves, rotates or fires, and draws itself.
- `src/tankgame/Shell.java`: a fired shell.
- `src/tankgame/MyKeyListener.java`: tracks whether one key is held and whether it was just pressed; one is added to the frame per key.

## Running

From the repo root: `./run.ps1 TankGame` (or `./run.ps1 TankGame -Build` to build a jar).

## Known quirks

- The clamp keeps the tank's unrotated square inside the window, so the corners of a turned tank can poke past the edge.
