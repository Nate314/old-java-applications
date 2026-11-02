# WindowsE

A small window that presses Win+E a chosen number of times, which on Windows opens that many File Explorer windows. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Text field**: how many times to press E (default `10`).
- **"DO IT!!!"**: holds the Windows key, taps E that many times 100 ms apart (on a Swing timer, so the window stays responsive), then releases the Windows key. The button is disabled until it finishes. If the count is not a whole number, the title says so.
- The window is 350x75 and not resizable. Title "Windows+E".
- No command-line arguments.

## Code layout

- `src/windowse/WindowsEMain.java`: entry point; shows a `WindowsEWindow`.
- `src/windowse/WindowsEWindow.java`: the text field and button.
- `src/windowse/WindowsEPresser.java`: sends the key presses via `java.awt.Robot`, timed by a Swing timer.

## Running

From the repo root: `./run.ps1 WindowsE` (or `./run.ps1 WindowsE -Build` to build a jar).

Clicking the button presses Win+E the number of times in the text field.

## In the browser

The web build runs the same code on a page with a pretend Windows (see [web/README.md](../web/README.md), Pretend Windows): each Win+E opens a pretend File Explorer window on the site's app folders, cascading like the real ones. Click a folder to read what the app is, double-click it to play it; drag the windows by their title bar, close them with their x, or use Close all in the tray at the bottom left. At most 40 are open at once.
