# NathaNOS

A full-screen fake desktop. It draws a background image with a custom cursor, a taskbar along the bottom with a start menu button, and one taskbar button per `.jar` in `assets/`. Clicking a jar's button opens that app: inside the desktop as a closable internal window if the app supports it (Clock), otherwise in the app's own windows on top of the desktop.

## Controls / usage

- **MENU button** (left end of the taskbar): show / hide the start menu.
- **Shut Down** (in the start menu): exit.
- **App buttons**: one per `assets/*.jar`, labeled with the file name minus `.jar`, with icon `assets/<name>.png` (matched ignoring case). Clicking loads the jar's `Main-Class`, then:
  - If it has a public `(Boolean)` constructor (Clock), calls it with `false`, moves that frame's content pane into a centered internal window and disposes the hidden frame. Internal app windows can be closed but not resized or maximized.
  - Otherwise runs its `main(String[])` with no arguments on a thread of its own, as `java` would, and the app opens its own windows.
  - Each click starts another copy. A jar that fails to load only disables its own button (the error is printed).
- Closing an app's window closes only that app. Apps use `EXIT_ON_CLOSE` when run alone, so NathaNOS switches every window opened after it starts to `DISPOSE_ON_CLOSE`. An app that calls `System.exit` itself still ends NathaNOS (ScreenSaver and Minesweeper's Esc close their window instead).
- The desktop window never takes focus, so clicking the taskbar or background does not raise it over open apps.
- Closing an app's window does not stop timers or threads it started unless the app stops them itself, so they can keep running until NathaNOS exits.
- No keyboard controls or command-line arguments. The window is undecorated and fills the screen.
- The shipped `assets/` has one jar, `Clock.jar`, an older Clock build. `./run.ps1 NathaNOS` adds the other apps (see Running).

## Code layout

- `src/main/Main.java`: the full-screen `JFrame` and `JDesktopPane` with the background image; creates the taskbar and start menu.
- `src/main/LauncherWindow.java`: the taskbar; builds the buttons and launches apps.
- `src/main/AppLauncher.java`: runs an app's `main` on its own thread, and keeps NathaNOS open when an app's window closes.
- `src/main/StartMenu.java`: the start menu panel with the logo and Shut Down button.
- `src/main/FileGatherer.java`: scans `assets/` for jars and loads their main classes with `URLClassLoader`.
- `src/main/GenericAppFrame.java`: the `JInternalFrame` wrapper an app's content is shown in.
- `assets/`: background, cursor, logo and app icon images, plus `Clock.jar`. The original icons were renamed to the app names they now match (for example `minesqeeper.png` to `Minesweeper.png`, `piano.png` to `SynthesizerKeyboard.png`).

## Running

From the repo root: `./run.ps1 NathaNOS` (or `./run.ps1 NathaNOS -Build` to build a jar).

`run.ps1` stages NathaNOS in `out/NathaNOS/desktop/` and runs it from there: a copy of `assets/`, a jar of each app listed under `Apps` in `run.ps1`, and the files those apps read from their working folder (FlappyBird's images, StockChecker's ticker list and csv). That is every app except NathaNOS itself and the three that drive the real desktop with `Robot` (DinoGame, Windows, WindowsE). Clock is the shipped `assets/Clock.jar`. Files the apps write (StockChecker's `myStocks.txt`, GroupCreator's `output.txt`) land in the staging folder, not in the repo.

To run NathaNOS by hand, use a folder laid out the same way as the working directory, so `assets/` resolves.
