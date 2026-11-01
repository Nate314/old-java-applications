# GroupCreator

Shuffles a comma-separated list of names into random groups with a size between a minimum and a maximum, shows the groups in the window and writes them to `output.txt`. Decompiled with Vineflower from the author's own `GroupCreator.jar` (jar file date 2016-09-15). Try-with-resources blocks the decompiler could not rebuild were rewritten by hand, and catch variables were renamed.

## Controls / usage

- **Names field**: names separated by commas, for example `Alice,Bob,Carol,Dave`. Spaces around names and empty entries are ignored.
- **min / max**: group size limits, default `1` and `3`. Every group, including the last, has between min and max names.
- **OK**: shuffles the names, splits them into groups, lists the groups under the inputs (the window grows to fit) and overwrites `output.txt`.
- If the names cannot be split that way (for example 5 names in groups of 3 to 4), or min/max are not whole numbers, a message is shown instead and `output.txt` is left alone.
- Output format: one line per group, `Group 1 --- name, name`, numbered from 1.
- The window is 375 px wide and not resizable. Title "Group Creator". Group lines wider than the window are cut off in the window but complete in `output.txt`.
- No command-line arguments.

## Code layout

- `src/groupcreator/Main.java`: entry point and window; writes `output.txt` and paints the group list.
- `src/groupcreator/GroupMaker.java`: the shuffle and the grouping logic.
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 GroupCreator` (or `./run.ps1 GroupCreator -Build` to build a jar).

`output.txt` is written by relative path into the working directory (`GroupCreator/`).

## Self-check

`test/groupcreator/GroupMakerCheck.java` checks group sizes, that every trimmed name lands in exactly one group, the refusal of impossible splits, and that the shuffle is even. From `GroupCreator/`:

```
javac -d ../out/check/GroupCreator src/groupcreator/*.java test/groupcreator/*.java
java -ea -cp ../out/check/GroupCreator groupcreator.GroupMakerCheck
```
