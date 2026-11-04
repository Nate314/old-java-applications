# GroupCreator

Shuffles a comma-separated list of names into random groups with a size between a minimum and a maximum, shows the groups in the window and writes them to `output.txt`. Decompiled with Vineflower from the author's own `GroupCreator.jar` (jar file date 2016-09-15). Try-with-resources blocks the decompiler could not rebuild were rewritten by hand, and catch variables were renamed.

It has since become a tool for a class or team that is split into groups again and again: it remembers every round it made, and mixes each new round so people get partners they have not had yet. The groups are dealt onto colored cards, names can be swapped between groups by hand, and a partner chart shows who has been grouped with whom. For the moments in between, a name wheel picks whose turn it is, and gets round everyone before it picks anyone twice. A roll call marks who is away today, so they sit out the groups and the wheel without being deleted from the class, and keep-apart rules make sure two names never share a group when the sizes allow it.

## Controls / usage

- **Names field**: names separated by commas, for example `Alice,Bob,Carol,Dave`. Spaces around names and empty entries are ignored. It starts with twelve example names.
- **min / max**: group size limits, default `2` and `3` (the original's were `1` and `3`). Every group, including the last, has between min and max names.
- **Make groups** (or Enter in any field, or **N** / Space on the board): makes a new round and deals it onto one card per group, name by name.
- If the names cannot be split that way (for example 5 names in groups of 3 to 4), or min/max are not whole numbers, the board says so instead and nothing is saved.

### Fresh partners

- The first round is a plain shuffle. Every later round keeps the plain shuffle's group sizes, then moves names between groups to avoid pairs that were grouped before: from 24 random starts, it swaps two names of different groups while that lowers the cost, and keeps the cheapest result. A pair costs the square of the rounds it has already shared, so a pair that met twice is avoided before two pairs that met once.
- The header says how the round did: "Everyone has new partners" when no two people in a group were together before, or how many pairs repeat, when the sizes leave no other way. Names that meet someone again are tinted amber with a dot. Pointing at a name lists its new partners and whom it meets again, with the round numbers.
- It is a search, not a guarantee: for the twelve example names in pairs, 75 of 100 trial runs reached all eleven rounds without anyone repeating a partner, and the other 25 had their first repeat in round 10.
- **Reshuffle** (R): mixes the newest round again, in its place, against the rounds before it.
- **Swap by hand**: click a name, then a name in another group; the two trade places and the newest round is changed (Esc or a second click on the same name cancels).
- **Partner chart** (C, again for Groups): a cell for every pair of names in the class, darker the more rounds put them together, with "met n/m" for each name. Pointing at a cell names the pair and the rounds they shared.
- **Clear history** (X, then X or the button again to confirm): forgets every round (not the wheel's picks).

### Roll call and keep apart

- **Roll call** (A, again for Groups) shows a tile for every name in the names field, with a green strip for here and a struck-out name for away. Click a tile (or move with the arrow keys and press Enter or Space) to mark that name away today; again marks them here. **Everyone here** (E) clears the marks.
- Away names sit out new rounds and the wheel. Their earlier rounds, partners and wheel turns stay, so they rejoin with their history when they are marked here again. The header counts who is here.
- **Keep apart** (K) turns on linking: click a name, then each name that should never share a group with it (a second click on a linked name removes the link, Esc or a click on the first name is done). Linked names show a red badge with how many names they are kept apart from, and pointing at a tile draws red threads to them. The card on the right lists every rule; click a row to remove it.
- The mix treats a kept-apart pair as far worse than any repeated partner, so every round (the first one too) keeps them apart unless the group sizes leave no other way. **Make groups** (N) works from the roll call too.
- In Groups, two kept-apart names in one group (from a swap by hand, or sizes that left no way round it) are ringed in red, and the header names them.

### Pick someone

- **Pick someone** (P) turns the board into a wheel with a slice for every name in the names field (as typed now, each name once), and **Groups** (P again) goes back.
- **Spin** (S, Space, or a click on the wheel): the wheel spins for about four seconds and slows to a stop under the pointer, with a click as each peg passes and a short chime when it lands. The name is chosen at random when the spin starts, from the names that have not had a turn yet; the wheel only shows those, so everyone gets one turn before anyone gets a second. A click while it spins stops it where it was heading.
- The card beside the wheel shows who is up, which group they are in in the newest round, how many of the class have had a turn, the turns so far (newest first) and who is still to go. After the last person, the next spin puts the whole class back on the wheel, except whoever went last, so no one is called twice in a row.
- Names marked away in the roll call are not on the wheel; the header lists them.
- **Put back** (B): the newest pick goes back on the wheel, for someone who is away. **Start over** (O): forgets this time around's picks.

### Files

- The class (the names as typed, min, max, every round and the wheel's picks) is kept in `~/.groupcreator-class.txt`: lines `names=...`, `min=...`, `max=...`, `picked=...` (the names picked this time around, oldest first, once the wheel has picked someone), `away=...` (the names marked away, when there are some), one `apart=a,b` line per keep-apart rule and one `round=` line per round, oldest first, with its groups separated by `|` (so names should not contain `|`). A spin also saves the names field as typed.
- Each new, reshuffled or edited round is also written to `output.txt` in the working directory, one line per group, `Group 1 --- name, name`, as before.
- The window is 760 px wide and not resizable. Title "Group Creator". No command-line arguments.

## Code layout

- `src/groupcreator/Main.java`: entry point and window: the input fields, and the round actions (make, reshuffle, swap, clear) that save the class and `output.txt`.
- `src/groupcreator/GroupBoard.java`: the board: the dealt cards, the swap clicks, the partner chart, the Pick someone view, the Roll call view and the button bar.
- `src/groupcreator/ClassRules.java`: the roll call (who is away today) and the keep-apart rules, and the rules a round breaks.
- `src/groupcreator/PickWheel.java`: the name wheel: its slices, pegs and pointer, and the spin that slows to a stop on the chosen name.
- `src/groupcreator/PickBag.java`: who the wheel has picked this time around, and the pick from those still to go.
- `src/groupcreator/WheelSound.java`: the peg clicks and the landing chime, on the MIDI synthesizer.
- `src/groupcreator/FreshMix.java`: the mix that avoids pairs from earlier rounds and keeps kept-apart pairs out of one group.
- `src/groupcreator/PartnerHistory.java`: the rounds made so far, and how often each pair shared a group.
- `src/groupcreator/Roster.java`: reads and writes `~/.groupcreator-class.txt`.
- `src/groupcreator/GroupMaker.java`: the original shuffle and the grouping logic.
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 GroupCreator` (or `./run.ps1 GroupCreator -Build` to build a jar).

`output.txt` is written by relative path into the working directory (`GroupCreator/`).

## Self-check

`test/groupcreator/GroupMakerCheck.java` checks group sizes, that every trimmed name lands in exactly one group, the refusal of impossible splits, and that the shuffle is even. `test/groupcreator/FreshMixCheck.java` checks that the fresh mix keeps legal sizes and every name, plays a four-person round robin in pairs with no repeated pair, never repeats a pair in a second round of twelve in threes, usually finds four repeat-free rounds of nine in threes, repeats fewer pairs than a plain shuffle after six rounds, and that the partner history and the class file round-trip. `test/groupcreator/PickBagCheck.java` checks that the wheel picks all twelve example names once each before a new time around, that the new time around never starts with the last pick, that each name is equally likely to come first, put back and start over, that names not in the class and duplicates are handled, and that the picks round-trip through the class file in order. `test/groupcreator/ClassRulesCheck.java` checks marking away and here, that rules work either way round and ignore names not in the class, that 300 trials of four rounds of twelve in threes with eight keep-apart rules never break one, that away names are not dealt, that a pair the sizes force together is reported, and that away names and rules round-trip through the class file (skipping bad lines). From `GroupCreator/`:

```
javac -d ../out/check/GroupCreator src/groupcreator/*.java test/groupcreator/*.java
java -ea -cp ../out/check/GroupCreator groupcreator.GroupMakerCheck
java -ea -cp ../out/check/GroupCreator groupcreator.FreshMixCheck
java -ea -cp ../out/check/GroupCreator groupcreator.PickBagCheck
java -ea -cp ../out/check/GroupCreator groupcreator.ClassRulesCheck
```
