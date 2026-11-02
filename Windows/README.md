# Windows

Presses and releases the Windows key once, then exits. It has no window. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- No controls and no command-line arguments. It acts as soon as it starts.

## Code layout

- `src/test/Test.java`: entry point; presses the Windows key via `java.awt.Robot`, waits 100 ms and calls `System.exit(0)`.

## Running

From the repo root: `./run.ps1 Windows` (or `./run.ps1 Windows -Build` to build a jar).

It presses the Windows key via `java.awt.Robot` immediately on start, then exits.

## In the browser

The web build runs the same code on a page with a pretend Windows (see [web/README.md](../web/README.md), Pretend Windows): its Windows key press opens a Start menu holding every app on the site. Type to narrow it, Enter plays the best match, a tile plays that app, and Esc or the power button closes it. The program has already exited by then, so its exit screen shows once the menu is closed.
