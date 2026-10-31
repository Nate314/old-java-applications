# UltimateTicTacToe

An UltimateTicTacToe game written in Java for two players on one screen. Nine small tic-tac-toe boards sit in a 3x3 grid; the cell you play in decides which board your opponent must play in next.

## Controls / usage

- **Click a cell** to place a mark. X moves first, then players alternate.
- The first move can be on any board. After that you must play on the board matching the cell your opponent just picked. The boards you may play on are highlighted yellow.
- A small board won by X turns cyan; one won by O turns pink; a drawn (full) board turns gray. Finished boards are closed. If you are sent to a finished board, you may play on any open board.
- Win three small boards in a row to win the game. The title bar shows whose turn it is and the result; click any cell after the game ends to start a new one.
- No keyboard controls or command-line arguments. The window is about 3/4 of the screen height square and not resizable.

## Code layout

- `src/ultimateticktacktoe/Main.java`: entry point; creates the `GameFrame`.
- `src/ultimateticktacktoe/GameFrame.java`: the window; holds the nine boards and the game state, and recolors the cells after each move.
- `src/ultimateticktacktoe/GameState.java`: the rules: marks, small board results, the active board, whose turn it is and the overall winner.
- `src/ultimateticktacktoe/SmallBoard.java`: one 3x3 board; lays out its cells.
- `src/ultimateticktacktoe/CellButton.java`: one cell; forwards clicks to the game and shows its mark.

## Running

From the repo root: `./run.ps1 UltimateTicTacToe` (or `./run.ps1 UltimateTicTacToe -Build` to build a jar).

## Self-check

`test/ultimateticktacktoe/GameStateCheck.java` checks small board wins and draws, the send-to-board rule and the overall winner. From `UltimateTicTacToe/`:

```
javac -d ../out/check/UltimateTicTacToe src/ultimateticktacktoe/*.java test/ultimateticktacktoe/*.java
java -ea -cp ../out/check/UltimateTicTacToe ultimateticktacktoe.GameStateCheck
```
