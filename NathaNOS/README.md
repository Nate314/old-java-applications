# NathaNOS

A full-screen fake desktop. It draws a background image with a custom cursor, a grid of app icons, a start menu that lists every app, and a taskbar along the bottom with the MENU button, a button for each open app window and a clock. The apps are the `.jar` files in `assets/`. An app opens inside the desktop as a closable internal window if it supports it (Clock), otherwise in its own windows on top of the desktop.

## Controls / usage

- **Desktop icons**: one per `assets/*.jar`, on a grid of cells from the top-left corner, in alphabetical order down the columns. Click selects an icon, double-click opens the app. Clicking the empty desktop clears the selection and closes the menus.
- **Icon cards**: a single click (or tap) on an icon also opens a card beside it with the icon, the app's name, a line on what it is ("Minesweeper where every board can be solved without a guess.") and an **Open** button, so a visitor who does not know an app by its name, or a finger that taps only once, can still start it. The card opens to the right of the icon, or to its left near the right edge, or under or over it on a screen too narrow for either. A picture from Paint gets a card that opens it in Paint, and a jar added by hand is named by its file. Clicking the desktop, another icon, MENU or the clock, dragging the icon, or a double-click closes it.
- **Moving icons**: drag an icon (press and move it at least 5 pixels) and it follows the pointer above every window, while a faint outline marks the cell it came from. Let go and it lands in the cell under its middle; an icon already there trades places with it. From then on every icon keeps its cell, so the gap a moved icon leaves stays a gap, as on a real desktop. Until the first move (and after Arrange icons by name) the icons fill the cells in alphabetical order.
- **Desktop menu**: right-click the empty desktop (or long-press it on a touch screen) for:
  - **Wallpaper**: Original photo (`assets/background.jpg`, the only background NathaNOS had), Night sky (stars, a crescent moon and hills), Sunset hills, Deep ocean (light rays and bubbles) and Aurora. Each row shows a small preview, and the current one is ticked. The painted ones are drawn in Java2D, once per screen size.
  - **Arrange icons by name**: forgets where icons were moved and lines them all up alphabetically again.
- **Paint**: NathaNOS's own app (an icon among the others, in alphabetical order, and a row in the start menu). It opens in an internal window and draws a 200 x 125 pixel picture in the 16 colors of the classic Windows Paint palette, with each pixel shown 1 to 4 screen pixels wide (as big as the desktop allows), so it is drawn sharp, never blurred.
  - **Tools** (the column at the left, or keys 1 to 8 while Paint has the keyboard): Pencil (single pixels), Brush (round, 5 pixels), Spray can (dots while held), Eraser (white, 7 pixels), Line, Rectangle and Ellipse (drag from corner to corner; the shape follows the pointer until you let go) and Fill (an area of one color, up to its edges). When pixels are 2 or more screen pixels wide, the pixels the tool would paint are outlined under the pointer.
  - **Colors**: click a palette color for the left button, right-click one for the right button. The two squares at the left show both; click them to swap.
  - **Saving**: there is no Save step. A new picture is called "Picture 1" (or the next free number), and its first stroke puts it on the desktop as an icon showing the picture; every later stroke saves it again, and the icon follows. Double-click a picture's icon to open it in Paint. Up to 12 pictures fit; past that, Paint says so until one is deleted.
  - **New**, **Undo** (also Ctrl+Z; the last 40 strokes of this window), **Wallpaper** (makes the picture the desktop's wallpaper, stretched to cover the screen with its edges cut, and the wallpaper changes with each stroke) and **Delete** (press twice; deleting the wallpaper's picture brings back the original photo). The line under the palette says what the tool does and what just happened.
- On a narrow screen the icon tiles get narrower (down to 48 pixels) so every column of icons fits across it.
- **MENU button** (left end of the taskbar): show / hide the start menu.
- **Start menu**: the NathaNOS logo, one row per app (click to open it), and **Restart** and **Shut Down** side by side at the bottom. On a short screen the rows wrap into more columns.
- **Taskbar buttons**: one per open app window, in the order they opened, with the app's name and icon. The button of the active window is lit. Clicking a button brings that window to the front; clicking the lit button minimizes its window, and clicking a minimized window's button brings it back. A minimized window keeps its button until it is closed. Borderless windows (ScreenSaver, CircleSpawner's circle slices) get no button.
- **Show desktop**: the thin strip at the right end of the taskbar, past the clock, as on Windows. A click minimizes every window on the taskbar; when they are all minimized, a click brings them all back.
- **Window outlines**: a window flies between the desktop and the taskbar as a few light outlines that grow or shrink in about a fifth of a second, slowing as they land. Opening an app sends them from the icon or start menu row that opened it to the new window, minimizing sends them from the window into its button, and restoring sends them out of the button, after which the window shows. They are drawn on the desktop, so a window that is already up (an app opened in its own window) covers the part of them inside it.
- **Restart and Shut Down**: every app window closes at once (borderless ones too), its outlines flying into the middle of the screen, while the whole desktop, taskbar included, fades to black over about half a second with "Restarting" or "Shutting down" and a ring of chasing dots. Shut Down then ends NathaNOS, as it always did (in the browser, the page's exit screen follows). Restart instead shows the recovered NathaNOS logo (`assets/NathaNOS.png`, byte for byte the same file as the `NathaNOS.jpg` the original never displayed) with "Starting NathaNOS" for about two seconds, then fades back to the desktop, about four seconds in all. Icons, wallpaper and Paint pictures stay as they were. Clicks during the sequence do nothing. With reduced motion (in the browser) the fades and outlines are skipped and each step shows at once.
- **Clock**: the local time and date at the right end of the taskbar. Click it for the calendar, as on Windows: a panel opens above it with an analog clock and the time to the second, the day and the date, and a month calendar starting on Sunday, today in a blue circle and the days of the months either side dimmed. The arrows go back and forward a month, and the month's name comes back to this month. Click a day to see how far it is from today ("Friday, December 25: in 40 days", "14 days ago", tomorrow, yesterday); click it again to clear it. With no day picked, the line at the bottom says which day of the year it is ("Day 319 of 365"). Click the clock again, the desktop, an icon or MENU to close it.
- **Icons**: `assets/<name>.png` (matched ignoring case) on a light tile. Every app on the desktop has one: Circle Spawner's ring of windows, Flappy Bird's bird between two pipes, Group Creator's three people, Music Theory Expert's circle of fifths, Screen Saver's aquarium on a monitor, Simple Algorithms' bars swapping places, Sprite Floaty Stars' sun with star trails and Tank Game's red tank were added last, for the eight apps that had only their initials. An app without a picture (a jar added by hand) still gets a colored tile with its initials ("FB" for FlappyBird).
- Opening an app loads the jar's `Main-Class`, then:
  - If it has a public `(Boolean)` constructor (Clock), calls it with `false`, moves that frame's content pane into a centered internal window and disposes the hidden frame. Internal app windows can be closed but not resized or maximized.
  - Otherwise runs its `main(String[])` with no arguments on a thread of its own, as `java` would, and the app opens its own windows. A window that opens at the top-left corner of the screen (where most apps put theirs) is moved to the middle of the desktop, so it does not cover the icons.
  - Each open starts another copy. Opening an app whose jar failed to load prints an error; the other apps still work.
- Closing an app's window closes only that app. Apps use `EXIT_ON_CLOSE` when run alone, so NathaNOS switches every window opened after it starts to `DISPOSE_ON_CLOSE`. An app that calls `System.exit` itself still ends NathaNOS (ScreenSaver and Minesweeper's Esc close their window instead).
- The desktop window never takes focus, so clicking the taskbar or background does not raise it over open apps. That also means an internal window (Clock) stays behind app windows that cover it, even after its taskbar button brings it to the front of the desktop.
- Closing an app's window does not stop timers or threads it started unless the app stops them itself, so they can keep running until NathaNOS exits.
- Minimizing hides the window (the app keeps running behind it, timers and all), so it is the same on the desktop and in the browser, and the window does not go to the system's own taskbar.
- No keyboard controls (apart from Paint's) or command-line arguments. The window is undecorated and fills the screen.

## Saved state

`~/.nathanos-pictures.txt` keeps Paint's pictures, one line each: `picture=Picture 1:` and the pixels, row by row, as runs of one color: a capital letter for the color (`A` is black, `I` white) followed by the run's length in base 36 when it is more than 1 (a blank picture is `Ijag`, 25,000 white pixels). A damaged line is skipped.

`~/.nathanos.txt` (in `user.home`) keeps the wallpaper (`wallpaper=sunset`, or `wallpaper=picture:Picture 1` for a picture from Paint) and, once an icon has been moved by hand, every icon's cell (`icon=Minesweeper,2,0`: column 2, row 0, counted from 0). A missing file, an unknown wallpaper or a damaged line leaves the defaults (the original photo, icons in alphabetical order). A saved place off the grid of a smaller screen, or taken by another icon, is dropped and that icon fills a free cell instead, as does an app new to the desktop. In the browser NathaNOS keeps its own saves, so this is saved there too.
- The shipped `assets/` has one jar, `Clock.jar`, an older Clock build, kept for the Then version. `./run.ps1 old NathaNOS` puts today's build of every app in its place, Clock included (see Running), and so does the site.

The original NathaNOS had only the taskbar: one button per jar, sharing its width, and a start menu with the logo and Shut Down. The icons, the app list in the start menu, the window buttons, the clock and the window placement were added later, then moving icons, the desktop menu and the painted wallpapers, then Paint, then minimizing, Show desktop and the window outlines, then Restart and the shut down screen, then the calendar on the clock, then drawn icons for the eight apps that still showed initials, then the cards that say what each icon is.

## Code layout

- `src/main/Main.java`: the full-screen `JFrame` and `JDesktopPane` with the background image; creates the launcher, taskbar, start menu and icons.
- `src/main/Launcher.java`: the apps in `assets/`, their icons, and opening one (internal window or `main`).
- `src/main/DesktopShortcuts.java`: the desktop icons on their grid, on a layer below every window: placing, dragging (an icon carried on the drag layer) and trading places, and the right click that opens the desktop menu.
- `src/main/PaintWindow.java`: opens Paint in an internal window with its taskbar button, and draws Paint's icon.
- `src/main/PaintPanel.java`: Paint itself: the tools, palette and buttons, the drawing on the picture (lines, shapes, fill, spray), Undo, and saving after each stroke.
- `src/main/Picture.java`: a picture's pixels, drawn as runs of one color at any size, its thumbnail, and its text form.
- `src/main/Pictures.java`: the saved pictures in `~/.nathanos-pictures.txt`, telling the desktop icons and the wallpaper when they change.
- `src/main/IconTip.java`: the card a click on an icon opens beside it, with each app's line and the Open button.
- `src/main/DesktopMenu.java`: the right-click desktop menu with the wallpaper previews and Arrange icons by name.
- `src/main/Wallpaper.java`: the five wallpapers; paints the four scenes into an image the size of the screen.
- `src/main/DesktopSettings.java`: reads and writes `~/.nathanos.txt`.
- `src/main/LauncherWindow.java`: the taskbar, with the MENU button, the window buttons, the clock and Show desktop.
- `src/main/TaskList.java`: tracks the open app windows for the taskbar, minimizes and restores them, and centers new windows that open in the top-left corner.
- `src/main/Zoom.java`: the outlines that fly between a window and its icon or taskbar button.
- `src/main/CalendarFlyout.java`: the calendar that opens above the taskbar clock, and its date sums (day numbers, weekdays, the month grid).
- `src/main/StartMenu.java`: the start menu panel with the logo, the app list, Restart and Shut Down.
- `src/main/Power.java`: Restart and Shut Down: closing every window, the fade to black, the logo and the fade back.
- `src/main/FlatButton.java`: the flat icon-and-label button used by the taskbar and the start menu.
- `src/main/AppIcon.java`: draws an app's icon, or its initials tile.
- `src/main/AppLauncher.java`: runs an app's `main` on its own thread, and keeps NathaNOS open when an app's window closes.
- `src/main/FileGatherer.java`: scans `assets/` for jars and loads their main classes with `URLClassLoader`.
- `src/main/GenericAppFrame.java`: the `JInternalFrame` wrapper an app's content is shown in.
- `assets/`: background, cursor, logo and app icon images, plus `Clock.jar` (the older Clock build NathaNOS shipped with; the Then version runs it, and today's NathaNOS opens today's Clock instead). The original icons were renamed to the app names they now match (for example `minesqeeper.png` to `Minesweeper.png`, `piano.png` to `SynthesizerKeyboard.png`). The app icons and the cursor are now original art drawn as SVG in `art/` and rendered by `web/art.ps1` (`./web/art.ps1 -Only TankGame` renders one again): they replaced pictures taken from other apps (Chrome's dinosaur, a Windows Minesweeper mine, an icon set's pointing hand) and clip art of unknown origin. The unused `calculator.png`, `converter.png`, `pingPong.png` and `windowsLogo.png` were removed. The logos (`NathaNOS.png`, `NathaNOS.jpg`, `NOS.png`), the wallpaper photo and `RubiksCube2D.png` are as recovered.

## Self-check

`test/main/PictureCheck.java` checks that a picture reads back from its text and a damaged one is refused, that lines, rectangles, ellipses and the brush paint where they should, that a fill stays inside a closed ellipse, and that the pictures file keeps its pictures, names and limit. From `NathaNOS/`:

```
javac -d ../out/check/NathaNOS src/main/*.java test/main/*.java
java -ea -cp ../out/check/NathaNOS main.PictureCheck
java -ea -cp ../out/check/NathaNOS main.ZoomCheck
java -ea -cp ../out/check/NathaNOS main.PowerCheck
java -ea -cp ../out/check/NathaNOS main.CalendarCheck
java -ea -cp ../out/check/NathaNOS main.TaskListCheck
java -ea -cp ../out/check/NathaNOS main.IconTipCheck
```

`test/main/ZoomCheck.java` checks that the outlines start on the icon (or window), land exactly on the window (or button) and slow down as they land. `test/main/PowerCheck.java` checks the Restart and Shut Down timeline: the fade to black, the logo shown only on Restart and only on black, the fade back to the desktop, and the steps shown at once with reduced motion. `test/main/CalendarCheck.java` checks the calendar's date sums: day numbers read back to their dates over about 4,400 years, known weekdays, leap years (2000 and 2024 but not 2100), months rolling into the next year, the grid starting on the Sunday before the 1st, and the lines a picked day and today give.

`test/main/IconTipCheck.java` checks where an icon's card opens (right of the icon, left of it at the right edge, under or over it on a narrow screen, always above the taskbar), that every app's line fits the card's three lines with no word lost, that icon labels still stop at two lines, and that a jar added by hand is named by its file.

`test/main/TaskListCheck.java` checks that a closed window leaves the taskbar and the list Restart and Shut Down close, and that nothing in NathaNOS keeps it in memory after that (it is freed by the garbage collector), while the windows still open stay listed and are still closed by Restart.

## Running

From the repo root: `./run.ps1 old NathaNOS` (or `./run.ps1 old NathaNOS -Build` to build a jar).

`run.ps1` stages NathaNOS in `out/NathaNOS/desktop/` and runs it from there: a copy of `assets/`, a jar of each app listed under `Apps` in `run.ps1`, and the files those apps read from their working folder (FlappyBird's images, StockChecker's ticker list and csv). That is every app except NathaNOS itself and the three that drive the real desktop with `Robot` (DinoGame, Windows, WindowsE). Clock is today's Clock too: its jar replaces the copied `assets/Clock.jar`. Files the apps write (StockChecker's `myStocks.txt`, GroupCreator's `output.txt`) land in the staging folder, not in the repo.

To run NathaNOS by hand, use a folder laid out the same way as the working directory, so `assets/` resolves.
