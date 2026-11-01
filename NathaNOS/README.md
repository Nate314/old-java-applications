# NathaNOS

A full-screen fake desktop. It draws a background image with a custom cursor, a grid of app icons, a start menu that lists every app, and a taskbar along the bottom with the MENU button, a button for each open app window and a clock. The apps are the `.jar` files in `assets/`. An app opens inside the desktop as a closable internal window if it supports it (Clock), otherwise in its own windows on top of the desktop.

## Controls / usage

- **Desktop icons**: one per `assets/*.jar`, in alphabetical order, in columns from the top-left corner. Click selects an icon, double-click opens the app. Clicking the empty desktop clears the selection and closes the start menu.
- **MENU button** (left end of the taskbar): show / hide the start menu.
- **Start menu**: the NathaNOS logo, one row per app (click to open it) and **Shut Down** (exit). On a short screen the rows wrap into more columns.
- **Taskbar buttons**: one per open app window, in the order they opened, with the app's name and icon. The button of the active window is lit. Clicking a button brings that window to the front. Borderless windows (ScreenSaver, CircleSpawner's circle slices) get no button.
- **Clock**: the local time and date at the right end of the taskbar.
- **Icons**: `assets/<name>.png` (matched ignoring case) on a light tile, or for an app without one, a colored tile with its initials (FlappyBird is "FB").
- Opening an app loads the jar's `Main-Class`, then:
  - If it has a public `(Boolean)` constructor (Clock), calls it with `false`, moves that frame's content pane into a centered internal window and disposes the hidden frame. Internal app windows can be closed but not resized or maximized.
  - Otherwise runs its `main(String[])` with no arguments on a thread of its own, as `java` would, and the app opens its own windows. A window that opens at the top-left corner of the screen (where most apps put theirs) is moved to the middle of the desktop, so it does not cover the icons.
  - Each open starts another copy. Opening an app whose jar failed to load prints an error; the other apps still work.
- Closing an app's window closes only that app. Apps use `EXIT_ON_CLOSE` when run alone, so NathaNOS switches every window opened after it starts to `DISPOSE_ON_CLOSE`. An app that calls `System.exit` itself still ends NathaNOS (ScreenSaver and Minesweeper's Esc close their window instead).
- The desktop window never takes focus, so clicking the taskbar or background does not raise it over open apps. That also means an internal window (Clock) stays behind app windows that cover it, even after its taskbar button brings it to the front of the desktop.
- Closing an app's window does not stop timers or threads it started unless the app stops them itself, so they can keep running until NathaNOS exits.
- No keyboard controls or command-line arguments. The window is undecorated and fills the screen.
- The shipped `assets/` has one jar, `Clock.jar`, an older Clock build. `./run.ps1 NathaNOS` adds the other apps (see Running).

The original NathaNOS had only the taskbar: one button per jar, sharing its width, and a start menu with the logo and Shut Down. The icons, the app list in the start menu, the window buttons, the clock and the window placement were added later.

## Code layout

- `src/main/Main.java`: the full-screen `JFrame` and `JDesktopPane` with the background image; creates the launcher, taskbar, start menu and icons.
- `src/main/Launcher.java`: the apps in `assets/`, their icons, and opening one (internal window or `main`).
- `src/main/DesktopShortcuts.java`: the desktop icons, on a layer below every window.
- `src/main/LauncherWindow.java`: the taskbar, with the MENU button, the window buttons and the clock.
- `src/main/TaskList.java`: tracks the open app windows for the taskbar, and centers new windows that open in the top-left corner.
- `src/main/StartMenu.java`: the start menu panel with the logo, the app list and Shut Down.
- `src/main/FlatButton.java`: the flat icon-and-label button used by the taskbar and the start menu.
- `src/main/AppIcon.java`: draws an app's icon, or its initials tile.
- `src/main/AppLauncher.java`: runs an app's `main` on its own thread, and keeps NathaNOS open when an app's window closes.
- `src/main/FileGatherer.java`: scans `assets/` for jars and loads their main classes with `URLClassLoader`.
- `src/main/GenericAppFrame.java`: the `JInternalFrame` wrapper an app's content is shown in.
- `assets/`: background, cursor, logo and app icon images, plus `Clock.jar`. The original icons were renamed to the app names they now match (for example `minesqeeper.png` to `Minesweeper.png`, `piano.png` to `SynthesizerKeyboard.png`).

## Running

From the repo root: `./run.ps1 NathaNOS` (or `./run.ps1 NathaNOS -Build` to build a jar).

`run.ps1` stages NathaNOS in `out/NathaNOS/desktop/` and runs it from there: a copy of `assets/`, a jar of each app listed under `Apps` in `run.ps1`, and the files those apps read from their working folder (FlappyBird's images, StockChecker's ticker list and csv). That is every app except NathaNOS itself and the three that drive the real desktop with `Robot` (DinoGame, Windows, WindowsE). Clock is the shipped `assets/Clock.jar`. Files the apps write (StockChecker's `myStocks.txt`, GroupCreator's `output.txt`) land in the staging folder, not in the repo.

To run NathaNOS by hand, use a folder laid out the same way as the working directory, so `assets/` resolves.
