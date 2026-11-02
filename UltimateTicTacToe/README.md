# UltimateTicTacToe

An UltimateTicTacToe game written in Java, against the computer, for two players on one screen, or with a friend anywhere by sending links (or codes) back and forth, one move each. Nine small tic-tac-toe boards sit in a 3x3 grid; the cell you play in decides which board your opponent must play in next.

## Controls / usage

- **Mode buttons** (top of the window): Computer: Easy, Computer: Hard, Two players and Friend by link (Easy, Hard, 2 players and By link when the full labels are wider than the board). The current mode is lit; picking one starts a new game in it, except Friend by link, which goes back to the game with your friend if one is on. Against the computer you are X and move first; the computer answers as O after a short pause, and clicks wait until it has moved.
  - Easy wins the game or a small board when it can, and otherwise plays a random legal move.
  - Hard runs a Monte Carlo tree search: for about 0.7 s (and at least 300 games) it plays random games from the position, steers the search toward the moves that keep winning, and plays the move it explored most.
- **Click a cell** to place a mark. X moves first, then players alternate. The last move is shown on a white cell.
- The first move can be on any board. After that you must play on the board matching the cell your opponent just picked. The boards you may play on are highlighted yellow.
- A small board won by X turns cyan; one won by O turns pink; a drawn (full) board turns gray. Finished boards are closed. If you are sent to a finished board, you may play on any open board.
- Win three small boards in a row to win the game. The title bar and the status line under the board show whose turn it is and the result; click any cell after the game ends to start a new one.
- Against the computer, a second status line shows your record on that level (wins, losses and draws). The record, the mode played last and the game with a friend (its code, and whether it waits for the friend) are stored in `.ultimatetictactoe-record.txt` in your home folder, so the next start opens in that mode.
- **Friend by link**: play someone who is not at your screen, one move per message. You are X and move first; your move puts the game on the clipboard (Copied! in the status line) and the board waits. Send it to your friend however you like. They open it, play O, and their move is copied for them to send back; open that one and it is your move again. The friend's last move is the white cell.
  - On the web the game is a link to the game's page, such as `UltimateTicTacToe/index.html?game=e5e1`; opening it starts the page on that position, with a line on its title screen saying a friend sent a game. On the desktop it is the code alone (`e5e1`).
  - The row under the board shows the link (or code) to send, with **Copy link** (Copy code) to copy it again and **Take back** to undo your move before you send it. To open a friend's link or code, paste it into the field and press **Open** or Enter. A link or a code works in either version.
  - A code is the game's moves, two characters each: the board as a letter a-i and the cell as a digit 1-9, both in reading order, so `e5` is the very middle. Opening one replays every move by the rules, so a code with an illegal move does not open ("That is not a game link").
  - The game is kept in the record file (below), so closing the window does not lose it, and switching to another mode only sets it aside. Opening the same link again after you answered it (reloading the page) keeps your answer.
- **Keyboard**: Tab and Shift+Tab move between the cells and buttons (Swing's focus order; in the browser too), and Space presses the focused one. A screen reader hears each cell's mark, whether the next move may go there and where it is ("empty, open, top left board, middle square"; "O, last move, top board, bottom left square"), through Swing's accessible names. No other keys. One optional command-line argument: a friend's code or link, which opens it in Friend by link (`./run.ps1 UltimateTicTacToe e5e1`). The browser build also passes the page's own address, for the links. The window is about 3/4 of the screen height (narrower on a narrow screen, so it fits) and not resizable; the marks scale with the cells.

## Code layout

- `src/ultimateticktacktoe/Main.java`: entry point; creates the `GameFrame`.
- `src/ultimateticktacktoe/GameFrame.java`: the window; holds the mode buttons, the nine boards, the status lines and the game state, recolors the cells after each move and asks the computer for its moves on a Swing timer.
- `src/ultimateticktacktoe/GameState.java`: the rules: marks, small board results, the active board, whose turn it is, the last move and the overall winner; also copies a game and lists its legal moves.
- `src/ultimateticktacktoe/Mode.java`: the four modes.
- `src/ultimateticktacktoe/FriendLink.java`: the game codes of Friend by link: moves to and from codes, codes in pasted links, and the links to send.
- `src/ultimateticktacktoe/Computer.java`: the computer's moves: Easy, and Hard's Monte Carlo tree search.
- `src/ultimateticktacktoe/Record.java`: reads and writes the record against the computer, the last mode and the game with a friend.
- `src/ultimateticktacktoe/SmallBoard.java`: one 3x3 board; lays out its cells.
- `src/ultimateticktacktoe/CellButton.java`: one cell; forwards clicks to the game, shows its mark and names itself for screen readers.

## Running

From the repo root: `./run.ps1 UltimateTicTacToe` (or `./run.ps1 UltimateTicTacToe -Build` to build a jar).

## Self-check

`test/ultimateticktacktoe/GameStateCheck.java` checks small board wins and draws, the send-to-board rule and the overall winner, copies and legal moves, that both computer levels take a winning move, that over ten games every computer move is legal and Hard (with a short search) beats Easy at least seven times, and that a whole game's code replays to the same position, malformed or illegal codes do not open, and codes are read from links and pasted text. From `UltimateTicTacToe/`:

```
javac -d ../out/check/UltimateTicTacToe src/ultimateticktacktoe/*.java test/ultimateticktacktoe/*.java
java -ea -cp ../out/check/UltimateTicTacToe ultimateticktacktoe.GameStateCheck
```
