# NumberGuess

A guess-the-number game, grown from the original one-label version (type a guess from 1 to 100, read "too low" or "too high") into five ranges with par, stars and a number line, a duel against the computer, a mode where the computer guesses your number, and a card trick that reads your mind. Original Eclipse source from the author's own files (only `src/` was kept).

## How it plays

Tabs along the top pick the range: **1-10**, **1-100**, **1-1,000**, **1-10,000** and **1-1,000,000**, then **Duel**, **You pick** and **Magic**.

- Each range has a par: the most guesses a search that always guesses the middle of what is left can need (4, 7, 10, 14 and 20). You get par + 3 guesses; run out and the number is given away.
- Finding it within par is three stars, within par + 2 two stars, and in the last guesses one star. Two stars opens the next range (a locked tab shows a padlock and says what opens it). Each tab shows its best stars.
- Two number lines show what you know. The top one is the whole range, with what is still possible lit in green and a marker for each guess (blue too low, red too high, green right; the latest is labelled). The lit part slides as it shrinks and opens out into the lower line, which shows only what is still possible: one cell per number once few enough are left. Clicking the lower line guesses the number under the pointer (shown in a bubble), so even 1-1,000,000 can be played with the mouse alone.
- A guess outside the range, or one your earlier answers already rule out, is not counted; the line under the verdict says why.
- Dots under the verdict count the guesses: grey up to par, orange for the three spare ones.
- The guesses so far sit at the bottom as chips with an up or down arrow. When the number is found the stars show beside them, with "New best!" when it took fewer guesses than before.
- Each guess is read against guessing the middle, in the line under the verdict and as a bar under its chip: **Halved it** (teal) for a middle guess; **Lucky** (gold) for a guess off the middle that left no more numbers than the middle would have; **Costly** (orange) for one that left more.
- When the game ends (found or given away), the number lines give way to **hindsight**: a chart of how many numbers were still possible after each of your guesses, on a log scale so every halving is the same step down, beside the path of always guessing the middle for the same number. Each of your points is coloured by its read, the line above counts them ("5 halved, 1 lucky, 2 costly"), and the headline compares the two: "Luck saved you 2 guesses", "Even with halving" or "Halving finds 549 in 10 guesses: 1 fewer than you". A click on it, or Enter, plays again.

**Challenge a friend**: after you find a number in one of the five ranges, the **Challenge a friend** button beside SUBMIT puts a link to that game on the clipboard ("Link copied: your friend gets the same number and 6 guesses to beat."). The link carries the range and every guess you made, in a short code (`?challenge=2oi-l-a0-aw`: the range's digit, 0 for 1-10 up to 4 for 1-1,000,000, then each guess in base 36, scrambled so the number is not plain to see, but not secret either). Your friend's page opens on the same range with the same number: "A friend found a number in 4 guesses", with a gold ring on the guess dot they found it on. The range plays even if it is still locked for them, though a win there keeps no record. At the end the headline says who won ("You beat your friend: 3 guesses to 4", a tie, or "Your friend wins: 4 guesses to your 6"), and the hindsight chart adds the friend's path in gold beside yours and halving's. Out of guesses, the line under it says how many the friend took. Enter plays a new number of your own (in your own next range, if the challenge was in one not yet open for you). The desktop app has no page, so it copies the code alone, and takes a code or link as its argument (see Running).

**Duel** is a race against the computer. You hide a number from 1 to 100 (click it on the line or type it) and the computer hides one too. Then each round you guess its number and it guesses yours, with every answer given straight away. Whoever finds the other's number in fewer guesses wins, and the same number of guesses is a draw: when you find its number the computer still gets that round's guess to tie, so going first is no advantage. The board shows two lanes. Yours, on top, is the whole range with what is left of its number lit, and below it a line of just that part to click a guess on. The computer's lane, below, shows your number with a gold diamond, what is left of it lit in purple, a marker for each of its guesses, and what it is thinking ("I guessed 51: too high. So yours is from 1 to 50."). The computer guesses a random number from the middle half of what is left: close to the best search, but with no hiding place it always finds last, so halving every time wins more duels than it loses, though not all of them. Wins, losses and draws are saved.

**You pick** turns the game around: think of a number from 1 to 100, and the computer guesses it, always in the middle of what is left, so it never needs more than 7 guesses. Answer with **Lower**, **That's it!** or **Higher**. If your answers leave no number possible, it says so ("You said higher than 41 and lower than 42. Did your number change?").

**Magic** is the binary card trick. Think of a number from 1 to 63; six cards of 32 numbers each come one at a time, and you only say whether your number is on the card (**Yes, it's there** or **No**). The six cards wait face down on the right and turn face up with your answer as you go. After the sixth, the last card pauses ("Let me see..."), turns over and shows your number in a burst of sparks. **How it works** then lists the cards from the biggest key down: each card's smallest number is its key (1, 2, 4, 8, 16, 32), the cards you said yes to add up to your number ("32 + 4 + 1 = 37"), and the yes/no column read as 1s and 0s is the number in binary. Every round shuffles the order of the cards and the numbers on them, so the pattern is hard to spot and you can perform it for someone else once you know the secret. Saying no to every card gets "Not on any card?", since every number from 1 to 63 is on at least one. Nothing is saved.

The fewest guesses for each range is saved to `.numberguess-record.txt` in the user's home folder, one `LEVEL=stars,guesses` line per range won (for example `HUNDRED=3,6`), and the duels as one `duel=won,lost,drawn` line (for example `duel=4,2,1`).

## Controls / usage

- **Text field** and **SUBMIT** or **Enter**: guess a whole number (commas are allowed, as in `250,000`). After the number is found or given away, Enter starts a new one (in the next range, if that win just opened it). In Duel the first number hides yours, the rest are guesses, and after the duel Enter starts a rematch.
- **Mouse**: click a tab to switch range; click the lower number line to guess the number under the pointer, or after the game to start a new one. In Duel, click the line to hide your number, then your lane's lower line to guess; after the duel, a click on the board starts a rematch.
- **Page Up / Page Down**: previous / next tab (passing over locked ranges).
- **You pick**: **Up** or **H** for higher, **Down** or **L** for lower, **Enter**, **Space**, **C** or **Y** for "that's it". After the round, any of these starts again.
- **Magic**: **Y** or **Up** for yes, **N** or **Down** for no. After the reveal, **S** shows or hides How it works and **Enter** or **Space** deals again (or the **How it works** and **Again** buttons).
- The window is about 656x425 px (sized to fit the board). Title "Number Guess". The window opens on the first open range without three stars.
- Arguments, both optional: a challenge code or link to play first (see Challenge a friend), and the address of the page the game runs on, which the browser build passes so the links it copies point back at it.

## Code layout

- `src/numberguess/Main.java`: entry point and window; the board, the original text field and SUBMIT button, and the You pick buttons and keys.
- `src/numberguess/Board.java`: draws the tabs, verdict, guess dots, both number lines and the chips, and handles the mouse.
- `src/numberguess/Game.java`: picks the secret number and narrows what is still possible with each guess.
- `src/numberguess/Hindsight.java`: reads each guess (halved, lucky or costly), counts what was left after each one, and plays the middle search for the same number, for the hindsight chart.
- `src/numberguess/Challenge.java`: Challenge a friend; the code for a won game, reading it back (only a game won within the range's guesses counts), and the link.
- `src/numberguess/Level.java`: the five ranges, their par, guess limit and stars.
- `src/numberguess/Duel.java`: Duel; both hidden numbers, the turns, the computer's guesses and who won.
- `src/numberguess/DuelView.java`: draws the line to hide your number on, then both lanes, the computer's thoughts and the duels won.
- `src/numberguess/MindReader.java`: You pick; guesses the middle and notices answers that contradict each other.
- `src/numberguess/MagicCards.java`: Magic; deals the six shuffled cards and adds up the key numbers of the cards you said yes to.
- `src/numberguess/MagicView.java`: draws the card being asked, the six cards, the flip and sparks, and How it works.
- `src/numberguess/Records.java`: reads and writes the fewest guesses per range, which ranges are open, and the duels won, lost and drawn.

## Running

From the repo root: `./run.ps1 old NumberGuess` (or `./run.ps1 old NumberGuess -Build` to build a jar). To play a friend's challenge on the desktop, pass its code or link to the jar: `java -jar NumberGuess.jar 2oi-l-a0-aw`.

## Self-check

`test/numberguess/GameCheck.java` checks each range's par and stars, that guessing the middle always wins within par (and finds 1 and the top number), that out-of-range and ruled-out guesses are not counted, running out of guesses, that hindsight reads both middles as halved and off-middle guesses as lucky or costly by what they left, counts what is left after each guess, and that the middle search's path finds every number within par and is never costly, that You pick finds every number from 1 to 100 within 7 and catches contradicting answers, that Magic's cards each hold 32 numbers with their key as the smallest and that honest answers read every number from 0 to 63, that a duel takes turns, ignores guesses out of range or ruled out, gives the computer its guess in the round you find its number, keeps the computer's guesses in the middle half, and that the computer finds every number while halving wins more duels than it loses (but not all), and that the saved records keep the fewest guesses, skip bad lines, open ranges in order and keep the duel tally beside them, and that a won game in every range survives its challenge code (the code not showing the number), while codes that are broken, too long or carry a guess after the number was found are refused. From `NumberGuess/`:

```powershell
javac -d ../out/check/NumberGuess src/numberguess/*.java test/numberguess/*.java
java -ea -cp ../out/check/NumberGuess numberguess.GameCheck
```
