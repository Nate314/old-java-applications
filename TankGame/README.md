# TankGame

An unfinished two-player tank game. Each player drives a square tank with a barrel over a blue background; firing and any game rules were never written. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- **Red tank**: `W` forward, `S` back, `A` rotate counterclockwise, `D` rotate clockwise, `Q` fire.
- **Green tank**: `Up` forward, `Down` back, `Left` rotate counterclockwise, `Right` rotate clockwise, `M` fire.
- Each held key moves 5 px or turns 5 degrees per repaint. Both tanks start centered on the top-left corner (0, 0), facing right.
- Each tank draws its X, Y and rotation as text at (50, 50).
- The window is 500x500 px. Title "TankGame".
- No command-line arguments.

## Code layout

- `src/tankgame/Main.java`: entry point and window; adds the background panel and the two tanks with their keys.
- `src/tankgame/GamePanel.java`: fills the background blue and repaints itself in a loop.
- `src/tankgame/Tank.java`: one tank; reads its key states on each paint, moves or rotates, draws itself and repaints in a loop.
- `src/tankgame/MyKeyListener.java`: tracks whether one key is held; one is added to the frame per key.

## Running

From the repo root: `./run.ps1 TankGame` (or `./run.ps1 TankGame -Build` to build a jar).

## Known quirks

- Fire does nothing: `fireTank` only calls `repaint()`. It is also never reached, because the fire flag is set in `keyTyped`, where `getKeyCode()` is always `VK_UNDEFINED`.
- Speed depends on paint rate: movement happens inside `paint`, which sleeps 10 ms and calls `repaint()` on the Swing event thread, in both tanks and the background panel.
- `Tank.paint` does not clear first (the `clearRect` call is commented out), so earlier frames are not erased.
- The background panel and both tanks are all added to the frame's default `BorderLayout` center, and both tanks get the same 1000x500 bounds, so the three panels overlap.
- The status text uses `\n`, which `drawString` does not break on, so it is drawn on one line.
- Heavy stdout output: every key listener prints every key press and release (ten lines per event), and each tank prints a line per paint. That line's format string, `"d, %d @%ddegreed"`, prints the Y position after `@` and drops the rotation.
- Tanks are not kept inside the window.
