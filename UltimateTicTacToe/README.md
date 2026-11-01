# UltimateTicTacToe

An UltimateTicTacToe game written in Java, against the computer or for two players on one screen. Nine small tic-tac-toe boards sit in a 3x3 grid; the cell you play in decides which board your opponent must play in next.

## Controls / usage

- **Mode buttons** (top of the window): Computer: Easy, Computer: Hard, and Two players. The current mode is lit; picking one starts a new game in it. Against the computer you are X and move first; the computer answers as O after a short pause, and clicks wait until it has moved.
  - Easy wins the game or a small board when it can, and otherwise plays a random legal move.
  - Hard runs a Monte Carlo tree search: for about 0.7 s (and at least 300 games) it plays random games from the position, steers the search toward the moves that keep winning, and plays the move it explored most.
- **Click a cell** to place a mark. X moves first, then players alternate. The last move is shown on a white cell.
- The first move can be on any board. After that you must play on the board matching the cell your opponent just picked. The boards you may play on are highlighted yellow.
- A small board won by X turns cyan; one won by O turns pink; a drawn (full) board turns gray. Finished boards are closed. If you are sent to a finished board, you may play on any open board.
- Win three small boards in a row to win the game. The title bar and the status line under the board show whose turn it is and the result; click any cell after the game ends to start a new one.
- Against the computer, a second status line shows your record on that level (wins, losses and draws). The record and the mode played last are stored in `.ultimatetictactoe-record.txt` in your home folder, so the next start opens in that mode.
- No keyboard controls or command-line arguments. The window is about 3/4 of the screen height (narrower on a narrow screen, so it fits) and not resizable; the marks scale with the cells.

## Code layout

- `src/ultimateticktacktoe/Main.java`: entry point; creates the `GameFrame`.
- `src/ultimateticktacktoe/GameFrame.java`: the window; holds the mode buttons, the nine boards, the status lines and the game state, recolors the cells after each move and asks the computer for its moves on a Swing timer.
- `src/ultimateticktacktoe/GameState.java`: the rules: marks, small board results, the active board, whose turn it is, the last move and the overall winner; also copies a game and lists its legal moves.
- `src/ultimateticktacktoe/Mode.java`: the three modes.
- `src/ultimateticktacktoe/Computer.java`: the computer's moves: Easy, and Hard's Monte Carlo tree search.
- `src/ultimateticktacktoe/Record.java`: reads and writes the record against the computer and the last mode.
- `src/ultimateticktacktoe/SmallBoard.java`: one 3x3 board; lays out its cells.
- `src/ultimateticktacktoe/CellButton.java`: one cell; forwards clicks to the game and shows its mark.

## Running

From the repo root: `./run.ps1 UltimateTicTacToe` (or `./run.ps1 UltimateTicTacToe -Build` to build a jar).

## Self-check

`test/ultimateticktacktoe/GameStateCheck.java` checks small board wins and draws, the send-to-board rule and the overall winner, copies and legal moves, that both computer levels take a winning move, and that over ten games every computer move is legal and Hard (with a short search) beats Easy at least seven times. From `UltimateTicTacToe/`:

```
javac -d ../out/check/UltimateTicTacToe src/ultimateticktacktoe/*.java test/ultimateticktacktoe/*.java
java -ea -cp ../out/check/UltimateTicTacToe ultimateticktacktoe.GameStateCheck
```
