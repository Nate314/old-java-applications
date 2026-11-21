# Minesweeper

A Minesweeper game with three boards, a clock, best times and MIDI sound. Revealing a safe tile plays a note that climbs and falls in pitch; hitting a mine reveals all mines and plays a chord; clearing the board flags every mine and plays a rising run of notes.

## Look

The tiles draw themselves (`MineTile.paintComponent`) instead of being plain colored buttons with a letter on them:

- Hidden tiles are raised squares of grass in two checkered greens, with a light top edge and a shadow under them. The one under the mouse lights up, and it sinks while the button is held.
- Revealed tiles are flat sand, also checkered, so an opened area reads as one patch of ground. Numbers are drawn in the classic colors: 1 blue, 2 green, 3 red, 4 purple, then orange, teal, gray and silver.
- A flag is a red pennant on a pole, planted with a small bounce; a question mark is white on the grass.
- Mines are black with eight spikes and a shine. The one you hit sits on red.
- Openings ripple: each tile pops off (shrinks and fades) 22 ms after the ring of tiles inside it, so a big opening spreads out from the click. A lost game bursts the mines in outward from the one hit, keeps the flags that were right and crosses out the wrong ones; a won game plants a flag on every mine, outward from the last click.
- The keyboard cursor is a blue ring on the tile.

The original colors (bright green hidden tiles, blue revealed ones with a white number, red `b` for a mine, cyan `f` for a flag) can still be played on the site's "Then" page, the game as first restored (see Then and now in `web/README.md`).

## No guessing, and hints that say why

Classic Minesweeper often ends on a coin flip: two tiles, one mine, nothing on the board to tell them apart. Here every board is dealt so that it never comes to that, and the game shows the reasoning when you are stuck.

- **No guess** (on by default; the button in the top row, or **G**): when you make the first click, boards are dealt at random until one turns up that `Logic` clears from that click without a single guess, so every tile can be worked out from the numbers. That takes a few milliseconds (the self-check deals 50 Expert boards in a fraction of a second). If none turned up within 1.5 s a random board is played. Turning it off deals plain random boards, as before. Either way, the choice starts a new game and is remembered.
- **Hint** (the button, or **H**): rings the number (or the two numbers) in gold that show the next step, makes the tiles it settles glow (safe: open them) or turns them red (mines: flag them), and says what and why in the two lines under the status line, such as "Safe: open the glowing tiles." and "The left 1 puts 1 mine by the right 1: all it needs." The marks go when you make your next move. A hint works from the numbers alone and does not trust your flags, so a flag on a tile it finds safe is pointed out ("Safe: a flag there is wrong."). On a random board it can say "Nothing is certain here."
- `Logic` knows three rules, which are all a no-guess board needs: one number whose mines are all found (its other tiles are safe) or whose hidden tiles are exactly as many as its missing mines (all mines); two numbers sharing tiles, where one must put as many mines in the shared tiles as the other needs, so the other's own tiles are safe (or the other can only take some of them, so the rest go in the first one's own tiles); and the mine count, once every mine is found or the hidden tiles left are as many as the mines left.
- **Hit a mine on a no-guess board** and the lines under the status line show what could have been opened instead: "No need to guess: the glowing tiles were safe.", the number that shows it ringed and the reason.
- A game won with hints sets no best time ("You win in 0:42.1 with hints."; "Cleared with 2 hints."). Asking again before your next move does not count as another hint. A win with none says "Cleared by logic alone, no guesses."
- Before the first click they say how to start ("Click any tile to start: the first is always safe.") and what kind of board it is. Between hints they say what kind of board it is and where the help is ("No guess: logic alone clears every board." and "Stuck? Hint (H) shows the next step and why.").

## A first game that teaches

Someone who has never played does not know what the numbers mean or how to flag, and the lines under the board used to start on No guess and Hint. Now a first game teaches the two moves on the board as it stands (`Coach`):

- **Flag a mine**: after the first click the smallest number whose hidden tiles are exactly as many as its count is ringed in gold and those tiles turn red: "The gold 1 touches 1 mine and 1 hidden tile." and "So flag the red tile: right click or long press." After every move it finds the next one (zoomed, the board moves to show it); with none on the board the lines give the rule in words ("Each number counts the mines touching it.").
- **Open around a number**: once a flag is planted, a number whose flags add up (and are all right) is ringed in gold and the tiles it would open glow: "The gold 2 has all 2 mines flagged." and "Click or tap it to open the glowing tiles at once." Until there is one, it points at the next mine to flag instead ("Flag the red tile too, then click a number" and "whose mines are all flagged to open the rest.").
- Each lesson ends when the player does it (a flag; a click on a number that opens tiles), or with a win, and is saved (`lessons=0`, `1` or `2` in the best times file), so after both the lines go back to the tips above. A best times file from before the lessons with a win in it counts as done. The lessons are not hints: they do not count against a best time. They are off while Odds show or the computer plays (W).
- While the computer plays (W) the same lines and marks narrate each of its moves: the numbers it read, the reason, and "Computer: safe, so it opens it." or "Computer: a mine, so it flags it." (or "Computer: nothing is certain, so it guesses." on a random board).

## Odds

A hint says what is certain. **Odds** (the button at the right of the top row, or **O**) shows what is likely: every hidden tile gets its chance of being a mine, in percent, and the board turns into a map of risk.

- A tile that is certainly safe glows light with a green **0%**; the likelier a mine, the redder the tile, up to **100%**. On tiles too small to read (under 22 px) only the color shows. Flagged and question-marked tiles keep their mark and get the color under it, so a flag on a glowing tile is a wrong flag.
- The two lines under the status line say what the odds show: "Odds: the 0% tiles are safe to open." or, when nothing is certain, "Odds: no tile is safe for certain." and "The safest is 9% likely to be a mine."
- The odds are exact for what a player can see: `Odds` counts every way of placing the mines that fits all the numbers on the board (and the mine count), and a tile's chance is the share of those ways with a mine on it. Tiles touching no number share what is left of the mines. Like a hint, it works from the numbers alone and does not trust your flags. A percent between 0 and 1 shows as 1%, and one between 99 and 100 as 99%, so 0% and 100% always mean certain.
- The odds update after every move (and while the computer plays, W). They show from the first click on: before it, the first click is safe anyway. Screen readers hear them in each hidden tile's name ("hidden, 6% chance of a mine, row 1, column 1").
- Odds stay on for the next game until turned off (they are not saved between runs). A game played with the odds showing counts as one hint, so a win sets no best time ("Cleared with the odds showing.").
- The count walks the tiles along the numbers and keeps only how many mines each number it is part way through has so far, so every placement that agrees on those is counted at once; a walk from each end, met at a tile, gives the ways with a mine there. Before, it counted placements one by one and gave up after a million steps: on Expert boards opened in many scattered places (a player clicking around, or a random board) that happened about 1 move in 30, the line said "Too many ways to count the odds here.", and the slowest moves took about half a second, even with Odds off, since every move is checked for a guess. Now those same boards are always counted, and the slowest of their moves takes 11 ms instead of 459 ms (on the desktop JVM). Over 4,161 moves on 200 such boards where the old count finished, both gave the same odds. The line "Too many ways to count the odds here." is still there in case a board ever needs more than 20 million steps of the new count; the most any of those boards needed was about 1.6 million.

## Your guesses

When a game ends, every tile you guessed on is ringed in purple with a tag giving its chance of being a mine at the moment you opened it, so the board shows where the game turned on luck and how much.

- A move is a guess when the numbers on the board did not show the tile to be safe. Its chance is the one Odds (above) gives for that tile just before the click, from the numbers alone: like a hint, it does not trust your flags. The first click is never a guess. Opening around a number counts as one move, tagged on its riskiest tile.
- **After a win** the lines under the status line say how many guesses it took and the odds of them all coming off: "Cleared, with 2 guesses." and "Odds your 2 guesses all came off: 91%. Each is tagged in purple." Each guess's chance was read from the board as it stood then, so these multiply. A win with none says "Cleared without a single guess." (on a No guess board, "Cleared by logic alone, no guesses.").
- If Odds ever gives up on a board (see above; none of the boards tried so far needs it to), a move made then cannot be counted either way. A win with such moves only speaks of the moves counted: "Cleared, no guesses in the moves counted." and "3 moves were on a board too open to count." (or "Cleared, with 2 guesses in the moves counted." with the odds line).
- **After a mine** that was a guess: "That one was a guess: 29% likely a mine." and "Guess 3. Odds the 2 guesses before it came off: 76%." A tile the numbers already showed to be a mine is tagged 100% ("The numbers showed that one was a mine."). When the board could have told you instead, the hint lines show that, as before, and the tags are still drawn.
- Screen readers hear "your guess, 29% chance of a mine" after a guessed tile's name. Moves the computer makes (W) are not tagged. Tiles under 22 px get the ring without the tag.

## Zoom, for phones

On a phone the whole Classic board used to come out with tiles about 19 px wide (measured on a page 400 px wide): the row of six buttons made the window wider than the screen, so the page scaled it all down, and tiles that small are hard to hit with a finger. Now:

- **Zoom** (the button at the end of the top row, or **Z**) makes the tiles big enough to tap: the board shows 8 tiles across and the rest of it is a drag away. Drag anywhere on the board (a finger, or the mouse with the button held) and it follows; a press that moves more than a few pixels drags the board and plays no tile, so a tap still reveals and a long press still flags. Thin gray bars under and right of the board show which part of it is in view.
- The keyboard cursor and the computer's moves (W) bring their tile into view with a tile to spare, so keyboard play and watching work zoomed too.
- On a narrow screen (under 640 px wide) the buttons take two rows, the levels above the game's own buttons, so the window is no wider than the board, and Zoom starts on. On that 400 px page a Classic or Expert tile is now about 40 px, and the Odds percentages, which only show on tiles of 22 px or more, can be read.
- Beginner's 9 x 9 board fits as it is, so Zoom leaves it whole. Zoom carries over to the next game but is not saved between runs; on a large screen it starts off and the board looks as before.

## Controls / usage

- **Level buttons** (top of the window): Beginner (9x9, 10 mines), Classic (15x15, 18 mines, the original game's board) and Expert (16x16, 40 mines). The current level is lit. Picking one starts a new game on it. Keys **1**, **2** and **3** do the same, and **F2** starts a new game on the current level.
- **Left click**: reveal a tile. Blank tiles also reveal their neighbors.
- **Left click on a number** (or Space on it): once as many of its neighbors are flagged as the number says, its other hidden neighbors are revealed at once ("chording"). A wrong flag means one of them is a mine, so that loses the game, as in the classic game. While the flags do not add up yet, the hidden neighbors glow briefly instead, to show what it would open. The first click is always safe: mines are placed after it, away from the clicked tile and its neighbors. Flagged tiles cannot be revealed until unflagged.
- **Right click** (in the browser on a touch screen, a long press): cycle a hidden tile through flag, question mark and back.
- The status line under the board shows how many mines are left (mines minus flags), the time since the first click and the best time on that level. The title bar shows the mines left too. Both say when you win (with your time) or hit a mine, and on a win the status line also says whether the time is a new best.
- **Any click after the game ends**: start a new game.
- **Keyboard play**: Tab into the board (in the browser), or start playing on the first tile (on the desktop, where it has the focus). The **arrow keys** move a cursor from tile to tile (the tile under it has a blue ring), **Space** or **Enter** reveals it, and **F** (or the menu key, or Shift+F10) cycles its flag like a right click. A new game started from the keyboard keeps the cursor where it was. A mouse click hides the cursor until a key is used again.
- **Screen readers** hear each tile's state and place ("hidden, row 3, column 5", "2 mines near, ...", "flag, ...", "mine, ...", and after a loss "mine, hit, ..." and "wrong flag, ..."), through Swing's accessible names (the Java Access Bridge on the desktop, `aria-label` in the browser). In the browser the title bar's changes are read out too: the mines left after a flag, a mine hit, a win.
- **Shift+arrow keys**: move the window by 30 px per press (plain arrows did this before keyboard play; they still do while the window itself, not a tile, has the focus).
- **Z** or **Zoom**: bigger tiles, with the board dragged to see the rest (see Zoom above).
- **O** or **Odds**: every hidden tile's chance of being a mine (see Odds above).
- **H** or **Hint**: the next step that can be worked out, and why; **G** or **No guess**: deal boards that need no guess, or random ones (see above).
- **W**: watch the computer play. It plays the board as it stands (or a new one) about three moves a second, with the cursor ring on the tile it plays and the reason under the status line: it plays whatever `Logic` works out (taking its own flags as mines), and when nothing is certain reveals the tile least likely to be a mine (so it can lose, on a random board). A finished game is followed by a new one after about 2.5 s. W again, a click on a tile, or Space, Enter or F on one takes over the board as it stands; 1, 2, 3 and F2 keep it playing on the new board. A game the computer played in records no best time: the status line says "The computer cleared it" (or "You and the computer cleared it") instead.
- **Esc**: close the window, like its close button. Run alone that exits; inside NathaNOS it closes only Minesweeper.
- No command-line arguments. The window is about 3/4 of the screen height tall; a new game opens where the last window was.
- The best time on each level, the level played last, whether No guess is on (`noguess=on` or `off`) and how far the first game's lessons got (`lessons=`) are stored in `.minesweeper-best.txt` in your home folder, so the next start opens on that level.

## Code layout

- `src/minesweeper/Main.java`: entry point; creates a `GameFrame`.
- `src/minesweeper/GameFrame.java`: the window; builds the level buttons, the grid of tiles and the status line, holds game state, and handles reveals, loss, win, the clock and the mine counter.
- `src/minesweeper/Level.java`: the three board sizes and mine counts.
- `src/minesweeper/Logic.java`: what can be worked out for certain from the board as a player sees it, one step at a time, with the reason in words; also the hints (which do not trust the player's flags).
- `src/minesweeper/Odds.java`: each hidden tile's chance of being a mine, by counting every placement of the mines that fits the numbers, group by group of tiles that share numbers.
- `src/minesweeper/Coach.java`: a first game's two lessons (flag a mine, open around a number), each pointing at an example on the board.
- `src/minesweeper/Luck.java`: the player's guesses in a game, each with its chance of being a mine, and the lines that sum them up.
- `src/minesweeper/FairBoard.java`: deals no-guess boards, checking each random board by playing it with `Logic` from the first click.
- `src/minesweeper/Solver.java`: the computer's next move for W: a step from `Logic`, or the least risky guess.
- `src/minesweeper/BestTimes.java`: reads and writes the best times, the last level and the No guess setting.
- `src/minesweeper/MineFieldGenerator.java`: places mines at random and computes neighbor counts; also the flood reveal, opening around a number, and the win check.
- `src/minesweeper/MineTile.java`: a tile button that paints itself and its animations; forwards left clicks and Space or Enter to the game, cycles flags on a right click or F, moves the keyboard cursor with the arrow keys, and names itself for screen readers. `GameFrame` repaints the tiles that are animating every 16 ms until they are done.
- `src/minesweeper/SoundPlayer.java`: plays notes and chords through the Java MIDI synthesizer, timed with Swing timers.
- `src/minesweeper/WindowMoveKeyListener.java`: Esc, level keys, F2 and moving the window with the arrow keys (keys a tile has used are skipped).

## Running

From the repo root: `./run.ps1 old Minesweeper` (or `./run.ps1 old Minesweeper -Build` to build a jar).

## Self-check

`test/minesweeper/MineFieldCheck.java` checks board generation on every level, the flood reveal, opening around a number (including a wrong flag), the win check, the best times file (and the No guess setting and the lessons), the first game's lessons on a small board (which number each points at, and none over a wrong flag), and the computer (W) playing 200 games on each level: a move it is sure of never hits a mine or flags a safe tile, it never plays a revealed or flagged tile, and it wins more than 70% of Beginner games (it prints its wins per level: 187, 199 and 169 of 200). It also checks each `Logic` rule and its reason on small boards, that over 300 random games with careless (often wrong) flags a hint never calls a mine safe or a safe tile a mine, and that 50 no-guess boards per level have the right mines, open on the first click and are cleared by `Logic` alone (it prints how long they took). For Odds it checks 60 small boards (5x5, 6 mines, with some random flags that must be ignored) against counting every placement of the mines one by one, and 40 Expert games played by the computer: at every move the odds add up to 40 mines, no mine is at 0% and no safe tile at 100%; it prints the slowest move (a few ms). Then 30 Expert boards opened in up to 60 scattered places each (the kind the old count gave up on): the odds are counted at every move and add up to 40 mines, and it prints the slowest. For guesses it checks that a certain move is not kept as one, that the odds of coming through them are their product, and what a win says when some moves could not be counted. For Zoom it checks that the view never leaves the board and that the tile under the cursor is brought into view without moving a view that already shows it. From `Minesweeper/`:

```
javac -d ../out/check/Minesweeper src/minesweeper/*.java test/minesweeper/*.java
java -ea -cp ../out/check/Minesweeper minesweeper.MineFieldCheck
```
