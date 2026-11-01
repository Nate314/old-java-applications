# NathaNOS

A full-screen fake desktop. It draws a background image with a custom cursor, a taskbar along the bottom with a start menu button, and one taskbar button per `.jar` in `assets/`. Clicking a jar's button opens that app inside the desktop as a closable internal window.

## Controls / usage

- **MENU button** (left end of the taskbar): show / hide the start menu.
- **Shut Down** (in the start menu): exit.
- **App buttons**: one per `assets/*.jar`, labeled with the file name minus `.jar`, with icon `assets/<name>.png` (matched ignoring case). Clicking loads the jar's `Main-Class`, calls its public `(Boolean)` constructor with `false`, moves that frame's content pane into a centered internal window and disposes the hidden frame. Each click opens another window. A jar that fails to load only disables its own button (the error is printed).
- Internal app windows can be closed but not resized or maximized.
- No keyboard controls or command-line arguments. The window is undecorated and fills the screen.
- The shipped `assets/` has one jar, `Clock.jar`, an older Clock build. The Clock source in this repo also has a public `Main(Boolean)` constructor, so a jar built from it (`./run.ps1 Clock -Build`, then copy `out/Clock.jar` into `assets/`) launches here too.

## Code layout

- `src/main/Main.java`: the full-screen `JFrame` and `JDesktopPane` with the background image; creates the taskbar and start menu.
- `src/main/LauncherWindow.java`: the taskbar; builds the buttons and launches apps.
- `src/main/StartMenu.java`: the start menu panel with the logo and Shut Down button.
- `src/main/FileGatherer.java`: scans `assets/` for jars and loads their main classes with `URLClassLoader`.
- `src/main/GenericAppFrame.java`: the `JInternalFrame` wrapper an app's content is shown in.
- `assets/`: background, cursor, logo and icon images, plus `Clock.jar`.

## Running

From the repo root: `./run.ps1 NathaNOS` (or `./run.ps1 NathaNOS -Build` to build a jar).

Run with `NathaNOS/` as the working directory so `assets/` resolves.
