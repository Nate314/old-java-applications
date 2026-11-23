# GroupCreator

Shuffles a comma-separated list of names into random groups with a size between a minimum and a maximum, shows the groups in the window and writes them to `output.txt`. Decompiled with Vineflower from the author's own `GroupCreator.jar` (jar file date 2016-09-15). Try-with-resources blocks the decompiler could not rebuild were rewritten by hand, and catch variables were renamed.

It has since become a tool for a class or team that is split into groups again and again: it remembers every round it made, and mixes each new round so people get partners they have not had yet. The groups are dealt onto colored cards, names can be swapped between groups by hand, and a partner chart shows who has been grouped with whom. For the moments in between, a name wheel picks whose turn it is, and gets round everyone before it picks anyone twice. A roll call marks who is away today, so they sit out the groups and the wheel without being deleted from the class, and keep-apart rules make sure two names never share a group when the sizes allow it.

Each group also gets its jobs: the cards give every member a role (Leader, Scribe, Timer, Reporter, Checker), and the roles rotate from round to round so people take on roles they have not had yet.

And once the groups are dealt, the work starts: a work-time countdown runs in the corner of the board, along every card and on each group's Timer tag, and chimes when the time is up.

## Controls / usage

- **Names field**: names separated by commas, for example `Alice,Bob,Carol,Dave`. Spaces around names and empty entries are ignored. It starts with twelve example names.
- **min / max**: group size limits, default `2` and `3` (the original's were `1` and `3`). Every group, including the last, has between min and max names.
- **Make groups** (or Enter in any field, or **N** / Space on the board): makes a new round and deals it onto one card per group, name by name.
- Before round 1, the class waits on the board: a name card for everyone here, laid down a little off the grid, with what Make groups will deal ("Make groups deals 4 to 6 groups of 2 or 3.") and a **Deal round 1** button under them. The cards and the line follow the names, min and max as they are typed, and the header counts who is here (and who is away). When the sizes cannot work, the line says why in red and the button is greyed out. Clicking the button or a name card deals the round, and each name flies from its card into its group.
- Under the waiting class, size chips name the group sizes that work for the names here: **Pairs**, **Threes**, **Fours**, **Fives**, **Sixes**, each with how many groups it makes ("4 groups"), as long as it makes at least two. A size that does not divide the class lets some groups have one more (13 in threes is groups of 3 or 4), and a size that cannot split the class is left out. Clicking a chip, or pressing its digit (2 to 6) on the board, puts that size in min and max without dealing; the chip matching min and max is filled in. When min and max cannot work, the red line points to the chips.
- Every name wears a badge: a circle with the person's initials ("A" for Alice, "MJ" for Mary Jane), in a color of their own. The badge follows them onto the waiting class, the dealt cards, the name held in a drag, the roll call (greyed while away) and the wheel's card when it picks them, so a person is easy to find on a projector from round to round. The colors go by place in the names field, stepped round the color wheel so names next to each other never look alike; a badge is left off a name too small to hold one.
- With reduced motion on, the names are in their groups at once instead of flying, and the wheel lands on its name at once.
- The dealt cards grow to fill the board: the columns are chosen to make the names tallest, and when there is room the names, badges, role tags and the cards' colored bands are drawn up to nearly twice their size (a name's row from 22 up to 40 px), so twelve names in five groups sit in two rows of large cards instead of a thin strip. A class too big for full-size cards still gets shorter rows, as before.
- If the names cannot be split that way (for example 5 names in groups of 3 to 4), or min/max are not whole numbers, the board says so instead and nothing is saved.

### Fresh partners

- The first round is a plain shuffle. Every later round keeps the plain shuffle's group sizes, then moves names between groups to avoid pairs that were grouped before: from 24 random starts, it swaps two names of different groups while that lowers the cost, and keeps the cheapest result. A pair costs the square of the rounds it has already shared, so a pair that met twice is avoided before two pairs that met once.
- The header says how the round did: "Everyone has new partners" when no two people in a group were together before, or how many pairs repeat, when the sizes leave no other way. Names that meet someone again are tinted amber with a dot. Pointing at a name lists its new partners and whom it meets again, with the round numbers.
- It is a search, not a guarantee: for the twelve example names in pairs, 75 of 100 trial runs reached all eleven rounds without anyone repeating a partner, and the other 25 had their first repeat in round 10.
- **Reshuffle** (R): mixes the newest round again, in its place, against the rounds before it.
- **Swap by hand**: drag a name onto another name. While it is held, the name rides under the pointer, the name it would land on is tinted, and the header says what the drop will do ("Drop to swap Alice into Group 2 and Carol into Group 1."). On release the two trade places, sliding into each other's spot, and the newest round is changed. Dropped on someone in its own group, the two trade places in the group, and so their roles ("Drop to trade roles: Alice becomes Scribe, Bob Leader."). Dropped anywhere else, it slides back. Clicking still works too: click a name, then a name in another group (Esc or a second click on the same name cancels). With reduced motion on, the names move without sliding.
- **Partner chart** (C, again for Groups): a cell for every pair of names in the class, darker the more rounds put them together, with "met n/m" for each name. Pointing at a cell names the pair and the rounds they shared.
- **Clear history** (X, then X or the button again to confirm): forgets every round (not the wheel's picks).

### Roles

- Every card labels its members' roles, by place: the first name is the **Leader**, then the **Scribe**, the **Timer**, the **Reporter** and the **Checker**. A group of two has a Leader and a Scribe, a group of three adds a Timer, and so on; a sixth name and later ones have no role, and a group of one has none.
- Each new or reshuffled round orders every group so its members get roles they have not had in earlier rounds: from 8 random orders, it swaps two members' places while that lowers the cost, and keeps the cheapest. A role held n times before costs n squared. A team of four kept together for four rounds has each person in each role once.
- A role tag ringed in orange is one that person has had more often than another role their group has, a repeat the rotation could not avoid. In a pair, someone who was Leader once and Scribe once is not ringed for a second turn as Leader. Pointing at a name adds its role to the line in the header, for example "Scribe for the first time" or "Leader again (round 2)". Swapping two names by hand swaps their roles too, and dragging a name onto someone in its own group is how to choose who leads. On narrow cards the tag shrinks to its first letter.
- **Roles** (O) hides or shows the tags (the button is yellow while they are shown). They are shown at first; the roles still rotate while hidden, so they are fair when shown again.
- Rounds made before roles existed count as if their order had been roles.

### Work time

- **Work 10 min** (W) starts a countdown for the groups. It shows in a dark pill at the top right of the board (in every view), as a white strip along the bottom of each card's colored band that runs down, and on each Timer's tag ("Timer 7:42", or only the time on a narrow card). The button reads **Stop timer** while it runs; W or a click stops it.
- **+** (or =) and **-** add or take a minute, as do the - and + buttons in the pill. Before a start they change the length (1 to 60 minutes, kept with the class); while it runs they change the time left (never below 0:00) and the length with it, so the next work time is as long.
- In the last minute the pill turns amber and the header says "One minute left: Timers, help your group wrap up" (with roles shown and a group of three or more). At 0:00 the wheel's chime rings once, the pill blinks red with "Time's up", and the header calls on the Reporters (with roles shown and a group of four or more). It stays up until W stops it, and + gives the groups another minute (and rings again at its end).
- When the eight buttons of the Groups bar do not fit with their key letters, the letters are left off; the keys still work.

### Present

- **Present** (F, the button at the top right of Groups and Pick someone) shows the board to the class: the window fills the screen, the names field and the button bar go away, and the groups (or the wheel) are drawn as large as fit, for a projector or a shared screen. Twelve names in pairs are drawn at twice their size in the window's own space, and up to four times on a bigger screen; the cards keep their full size, so a big class is enlarged less. It picks the enlargement that draws the names largest once the cards have grown to fill the screen as in the window.
- Everything else still works while presenting: N deals a new round, R reshuffles, W runs the work time (its clock grows with the board), P switches between the groups and the wheel, Space spins, and names can still be swapped by clicking them.
- **Back** (Esc, F, or the button where Present was) puts the window back where it was, with the names field. The partner chart (C) and the roll call (A) are for the teacher's own screen, so they leave Present first.
- In the browser the window fills the page under the site's bar (on a phone too), so a browser on a projector, or in full screen, shows the groups across the whole screen.

### Look back

- Once there are two rounds, a **Rounds** timeline runs under the cards: a dot per round (numbered when there is room), the one shown drawn large. **Left** (or a click on a dot) looks back at an earlier round; **Right** steps forward again. With many rounds, the dots that fit around the one shown are drawn.
- An earlier round is shown as it was then: its groups and roles, with the names that met again marked against the rounds before it, and pointing at a name says who was new to them in that round. The header reads "Round 3 of 7". Names cannot be swapped in an earlier round.
- The bar changes while looking back: **Earlier** (<), **Later** (>), **Use again** (U), **New round** (N) and **Newest** (Esc).
- **Use again** deals that round's groups again as a new round, for a project that carries on in last week's groups: names marked away or no longer in the names field leave their group (a group left empty goes), and a name the round did not have joins the smallest group. The roles are rotated against every round so far, and the header says which round's groups came back. It is saved and written to `output.txt` like any other round, so the partner chart counts it. The class file notes which round came back (an `again=` line that older builds skip), so the header still says so after a reload.

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

- The class (the names as typed, min, max, every round and the wheel's picks) is kept in `~/.groupcreator-class.txt`: lines `names=...`, `min=...`, `max=...`, `roles=off` (when the role tags are hidden), `work=` (the work time's length in minutes, when it is not 10), `picked=...` (the names picked this time around, oldest first, once the wheel has picked someone), `away=...` (the names marked away, when there are some), one `apart=a,b` line per keep-apart rule and one `round=` line per round, oldest first, with its groups separated by `|` and each group's names in role order (so names should not contain `|`). A spin also saves the names field as typed.
- Each new, reshuffled or edited round is also written to `output.txt` in the working directory, one line per group, `Group 1 --- name, name`, as before. While the roles are shown, each name with a role is followed by it: `Group 1 --- Alice (Leader), Bob (Scribe)`.
- The window is 760 px wide and not resizable; Present (F) makes it the size of the screen until Back. Title "Group Creator". No command-line arguments.
- On a screen narrower than 600 px (a phone held upright, in the browser), the window takes the whole screen instead of being scaled down, so names stay full size: the names label sits over the field, the header's line wraps onto two lines under the Present button, the bar's buttons wrap onto rows that fill the width, Pick someone puts the wheel above its card, and Roll call puts the Keep apart card under the tiles. The window is never smaller than 340 x 560 (a smaller screen scales it down as before). The desktop window is unchanged.

## Code layout

- `src/groupcreator/Main.java`: entry point and window: the input fields, and the round actions (make, reshuffle, swap, clear) that save the class and `output.txt`.
- `src/groupcreator/GroupBoard.java`: the board: the class waiting before round 1, the dealt cards, the swap drags and clicks, the partner chart, the Pick someone view, the Roll call view, the button bar, Present (how much it enlarges the board) and Look back (the rounds timeline).
- `src/groupcreator/ClassRules.java`: the roll call (who is away today) and the keep-apart rules, and the rules a round breaks.
- `src/groupcreator/PickWheel.java`: the name wheel: its slices, pegs and pointer, and the spin that slows to a stop on the chosen name.
- `src/groupcreator/PickBag.java`: who the wheel has picked this time around, and the pick from those still to go.
- `src/groupcreator/Badge.java`: a person's badge, their initials and color.
- `src/groupcreator/WheelSound.java`: the peg clicks and the landing chime, on the MIDI synthesizer.
- `src/groupcreator/Roles.java`: the roles by place in a group, the rounds each name had a role, the repeats worth ringing, and the rotation that orders a new round's groups.
- `src/groupcreator/WorkTimer.java`: the work-time countdown: its length, the time left, the one ring when it is up, and a minute more or less.
- `src/groupcreator/FreshMix.java`: the mix that avoids pairs from earlier rounds and keeps kept-apart pairs out of one group.
- `src/groupcreator/PartnerHistory.java`: the rounds made so far, and how often each pair shared a group.
- `src/groupcreator/Roster.java`: reads and writes `~/.groupcreator-class.txt`.
- `src/groupcreator/GroupMaker.java`: the original shuffle and the grouping logic, and an earlier round's groups fitted to the class here now (Use again).
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 old GroupCreator` (or `./run.ps1 old GroupCreator -Build` to build a jar).

`output.txt` is written by relative path into the working directory (`GroupCreator/`).

## Self-check

`test/groupcreator/GroupMakerCheck.java` checks group sizes, that every trimmed name lands in exactly one group, the refusal of impossible splits, that the preview before round 1 names the right numbers of groups and sizes and refuses exactly what Make groups refuses, and that the shuffle is even. `test/groupcreator/FreshMixCheck.java` checks that the fresh mix keeps legal sizes and every name, plays a four-person round robin in pairs with no repeated pair, never repeats a pair in a second round of twelve in threes, usually finds four repeat-free rounds of nine in threes, repeats fewer pairs than a plain shuffle after six rounds, and that the partner history and the class file round-trip. `test/groupcreator/PickBagCheck.java` checks that the wheel picks all twelve example names once each before a new time around, that the new time around never starts with the last pick, that each name is equally likely to come first, put back and start over, that names not in the class and duplicates are handled, and that the picks round-trip through the class file in order. `test/groupcreator/ClassRulesCheck.java` checks marking away and here, that rules work either way round and ignore names not in the class, that 300 trials of four rounds of twelve in threes with eight keep-apart rules never break one, that away names are not dealt, that a pair the sizes force together is reported, and that away names and rules round-trip through the class file (skipping bad lines). `test/groupcreator/RolesCheck.java` checks which places have a role, the rounds a name had one, that rotation keeps every group's names, that a team of four kept together for four rounds has each person in each role once (300 trials), that twelve in threes for three rounds repeat under a third as many roles as the plain mix's order, that a role is ringed only when another role of the group was had fewer times, and that `roles=off` round-trips through the class file. `GroupMakerCheck` also checks the `output.txt` lines with roles. `test/groupcreator/WorkTimerCheck.java` checks the clock text (seconds rounded up), the countdown, that it rings once at 0:00 and again after a minute more, that a minute less stops at 0:00, the 1 to 60 minute length, and that `work=` round-trips through the class file (a bad value keeps 10 minutes, a long one is cut to an hour). `test/groupcreator/PresentCheck.java` checks how much Present enlarges the board (twelve in pairs twice in the window, more on a full HD screen, at most four times, never less than once, the wheel by its own shape), that F presents, while Esc, the Back button and the partner chart go back, and that out of Present twelve names in pairs grow to fill the board, every name on it and none overlapping. `test/groupcreator/LookBackCheck.java` checks Use again's groups (the same class gets the same groups, a name not here leaves, an emptied group goes, a newcomer joins the smallest group, a name typed twice is placed once), and that Left, Right and Esc step through the rounds, stopping at round 1 and ending at the newest, and that `again=` round-trips through the class file (a missing, bad or out-of-range number reads as a plain round). `test/groupcreator/DragCheck.java` checks that a name dragged onto a name of another group swaps the two, that one dropped in its own group trades places (and roles) with the name it lands on, the header's line on the way, that a drop on no name changes nothing and lets the name go, that a press that barely moves still picks for click-and-click swaps, that a second click lets go, that an earlier round cannot be dragged in, and that before round 1 a size chip or its digit sets min and max without dealing. `GroupMakerCheck` also checks the size chips offered for 0 to 40 names. `test/groupcreator/BadgeCheck.java` checks the badges' initials and that names next to each other in the class get hues far apart. From `GroupCreator/`:

```
javac -d ../out/check/GroupCreator src/groupcreator/*.java test/groupcreator/*.java
java -ea -cp ../out/check/GroupCreator groupcreator.GroupMakerCheck
java -ea -cp ../out/check/GroupCreator groupcreator.FreshMixCheck
java -ea -cp ../out/check/GroupCreator groupcreator.PickBagCheck
java -ea -cp ../out/check/GroupCreator groupcreator.ClassRulesCheck
java -ea -cp ../out/check/GroupCreator groupcreator.RolesCheck
java -ea -cp ../out/check/GroupCreator groupcreator.WorkTimerCheck
java -ea -cp ../out/check/GroupCreator groupcreator.PresentCheck
java -ea -cp ../out/check/GroupCreator groupcreator.LookBackCheck
java -ea -cp ../out/check/GroupCreator groupcreator.DragCheck
java -ea -cp ../out/check/GroupCreator groupcreator.BadgeCheck
```
