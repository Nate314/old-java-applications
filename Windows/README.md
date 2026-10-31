# Windows

Presses and releases the Windows key once, then exits. It has no window. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- No controls and no command-line arguments. It acts as soon as it starts.

## Code layout

- `src/test/Test.java`: entry point; presses the Windows key via `java.awt.Robot`, waits 100 ms and calls `System.exit(0)`.

## Running

From the repo root: `./run.ps1 Windows` (or `./run.ps1 Windows -Build` to build a jar).

It presses the Windows key via `java.awt.Robot` immediately on start, then exits.
