# UltimateTicTacToe

An UltimateTicTacToe game written in Java for two players on one screen. Nine small tic-tac-toe boards sit in a 3x3 grid; the cell you play in decides which board your opponent must play in next.

## Controls / usage

- **Click a cell** to place a mark. X moves first, then players alternate.
- The first move can be on any board. After that you must play on the board matching the cell your opponent just picked; it is highlighted yellow.
- A small board won by X turns cyan; one won by O turns pink.
- No keyboard controls or command-line arguments. The window is about 3/4 of the screen height square and not resizable.

## Code layout

- `src/ultimateticktacktoe/Main.java`: entry point; creates the `GameFrame`.
- `src/ultimateticktacktoe/GameFrame.java`: the window; holds the nine boards, whose turn it is and the active board.
- `src/ultimateticktacktoe/SmallBoard.java`: one 3x3 board; lays out its cells and checks for a win or draw.
- `src/ultimateticktacktoe/CellButton.java`: one cell; enforces the active board, places X or O, moves the highlight and colors won boards.

## Running

From the repo root: `./run.ps1 UltimateTicTacToe` (or `./run.ps1 UltimateTicTacToe -Build` to build a jar).

## Known quirks

- There is no overall winner. `GameFrame.checkWinner()` checks only the top-left cell of each small board, and its result is ignored.
- Only the first move is free. If you are sent to a full board there is no legal move and the game is stuck.
- Won boards can still be played on.
- A small board draw is detected but not acted on.
- `SmallBoard.checkWinner()` resets its stored winner to "none" on the call after a win, so it does not remember a result.
- `backgroundColor != new Color(0, 255, 0)` in `CellButton` compares object references, so it is always true.
- Unused: `SmallBoard.unusedFlag`, and the initial `activeBoard = 2` (overridden by the free first move).
- The window title is "AWT Ultimate Tick Tack Toe" although it uses Swing.
