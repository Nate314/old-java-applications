# GroupCreator

Shuffles a comma-separated list of names into random groups with a size between a minimum and a maximum, shows the groups in the window and writes them to `output.txt`. Decompiled with Vineflower from the author's own `GroupCreator.jar` (jar file date 2016-09-15). Try-with-resources blocks the decompiler could not rebuild were rewritten by hand, and catch variables were renamed.

It has since become a tool for a class or team that is split into groups again and again: it remembers every round it made, and mixes each new round so people get partners they have not had yet. The groups are dealt onto colored cards, names can be swapped between groups by hand, and a partner chart shows who has been grouped with whom.

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
- **Clear history** (X, then X or the button again to confirm): forgets every round.

### Files

- The class (the names as typed, min, max and every round) is kept in `~/.groupcreator-class.txt`: lines `names=...`, `min=...`, `max=...` and one `round=` line per round, oldest first, with its groups separated by `|` (so names should not contain `|`).
- Each new, reshuffled or edited round is also written to `output.txt` in the working directory, one line per group, `Group 1 --- name, name`, as before.
- The window is 760 px wide and not resizable. Title "Group Creator". No command-line arguments.

## Code layout

- `src/groupcreator/Main.java`: entry point and window: the input fields, and the round actions (make, reshuffle, swap, clear) that save the class and `output.txt`.
- `src/groupcreator/GroupBoard.java`: the board: the dealt cards, the swap clicks, the partner chart and the button bar.
- `src/groupcreator/FreshMix.java`: the mix that avoids pairs from earlier rounds.
- `src/groupcreator/PartnerHistory.java`: the rounds made so far, and how often each pair shared a group.
- `src/groupcreator/Roster.java`: reads and writes `~/.groupcreator-class.txt`.
- `src/groupcreator/GroupMaker.java`: the original shuffle and the grouping logic.
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 GroupCreator` (or `./run.ps1 GroupCreator -Build` to build a jar).

`output.txt` is written by relative path into the working directory (`GroupCreator/`).

## Self-check

`test/groupcreator/GroupMakerCheck.java` checks group sizes, that every trimmed name lands in exactly one group, the refusal of impossible splits, and that the shuffle is even. `test/groupcreator/FreshMixCheck.java` checks that the fresh mix keeps legal sizes and every name, plays a four-person round robin in pairs with no repeated pair, never repeats a pair in a second round of twelve in threes, usually finds four repeat-free rounds of nine in threes, repeats fewer pairs than a plain shuffle after six rounds, and that the partner history and the class file round-trip. From `GroupCreator/`:

```
javac -d ../out/check/GroupCreator src/groupcreator/*.java test/groupcreator/*.java
java -ea -cp ../out/check/GroupCreator groupcreator.GroupMakerCheck
java -ea -cp ../out/check/GroupCreator groupcreator.FreshMixCheck
```
