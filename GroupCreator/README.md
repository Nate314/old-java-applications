# GroupCreator

Shuffles a comma-separated list of names into random groups with a size between a minimum and a maximum, and writes the groups to `output.txt`. Decompiled with Vineflower from the author's own `GroupCreator.jar` (jar file date 2016-09-15). Try-with-resources blocks the decompiler could not rebuild were rewritten by hand, and catch variables were renamed.

## Controls / usage

- **Names field**: names separated by commas, for example `Steve Jobs,Bill Gates,Stephen Hawking,Morgan Freeman`.
- **min / max**: group size limits, default `1` and `3`.
- **OK**: shuffles the names, splits them into groups and overwrites `output.txt`. The result is also printed to stdout. Nothing is shown in the window.
- Output format: one line per group, `Group0 --- name, name, `, numbered from 0. The file starts with a blank line.
- The window is 375x130 px and not resizable. Title "Group Creator".
- No command-line arguments.

## Code layout

- `src/groupcreator/Main.java`: entry point and window; holds the shuffle and the grouping logic.
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 GroupCreator` (or `./run.ps1 GroupCreator -Build` to build a jar).

`output.txt` is written by relative path into the working directory (`GroupCreator/`).

## Known quirks

- Each group size is `(int)(random * (max - min)) + min`, so sizes run from min to max - 1. A group of size max only happens when min equals max.
- The last group gets whatever names are left, so it can be smaller than min. When the earlier groups use up every name exactly, the last group is empty.
- Names are not trimmed, so a space after a comma stays part of the next name.
- The shuffle swaps each position with a random position anywhere in the array, which is not a uniform shuffle.
- Non-numeric min or max throws an uncaught `NumberFormatException` and nothing is written.
- Each click prints the window size to stdout.
