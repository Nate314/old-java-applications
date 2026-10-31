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
