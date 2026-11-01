# Web (TeaVM)

Runs the Swing/AWT apps in a browser. [TeaVM](https://teavm.org) 0.16.0 compiles the unmodified app sources to JavaScript, and a small AWT/Swing/MIDI compatibility layer ("the shim") stands in for `java.awt`, `javax.swing` and `javax.sound.midi`.

## Build and serve

Requires a JDK (17 or newer) and Maven 3.9. From the repo root:

```powershell
mvn -f web/pom.xml package                          # builds web/target/site
python -m http.server 8000 --directory web/target/site
```

Then open http://localhost:8000/. The site is fully static, so any static file server works. `web/target/` is git-ignored.

To build another checkout of the app sources (for example a branch checked out elsewhere), point `repo.dir` at it and pick an output folder:

```powershell
mvn -f web/pom.xml package "-Drepo.dir=C:/path/to/other/checkout" "-Dsite.dir=C:/path/to/site"
```

## Status

| App | Status | Notes |
|---|---|---|
| Clock | Works | |
| FlappyBird | Works | Runs one game step per animation frame (60 per second on most screens), so its speed differs from the desktop, where it ran as fast as the machine could repaint. |
| Minesweeper | Works | Sound uses simple Web Audio tones. The game-over chord bends its pitch with about 800 one-millisecond sleeps, which take longer in a browser (timers have a minimum delay), so the board waits a few seconds before the next click. Esc closes the app. |
| NathaNOS | Works | The taskbar launches the old Clock build from `assets/Clock.jar` and the 14 other browser apps (compiled in ahead of time, see below). Apps open as their own windows over the desktop; closing one leaves NathaNOS and the others running. SimpleAlgorithms prints only to the browser's developer console here, since the page has no output panel (the old Clock prints the time every second). |
| RubiksCube2D | Works | The w/a/s/d buttons and Mix send key events through the page-level `Robot`. |
| ScreenSaver | Works with limitations | The "screen" is the browser viewport, and the circle size is the greatest common factor of its width and height, which is often tiny for arbitrary window sizes. Its repaint thread busy-waits, so the tab is sluggish while it runs, as the desktop app kept a CPU core busy. Moving the mouse exits. |
| SpriteFloatyStars | Works | Settings window and all modes. |
| UltimateTicTacToe | Works | |
| CircleSpawner | Works | Each slice is a borderless window on the page. |
| StockChecker | Works with limitations | The Yahoo quote API is retired, so refresh fails (a network error is logged) and the labels are filled from the bundled `stockCheck.csv`, as on the desktop. Files the app writes stay in memory until the page is reloaded. |
| SynthesizerKeyboard | Works | Play with the keyboard. On `master` sources, clicking a key starts a note that never stops and freezes the app (its documented bug: it waits for a timer that runs on the same event thread), as on the desktop; the modernized sources fix that. |
| GroupCreator | Works | `output.txt` goes to the in-memory file system, so the page also shows standard output (where the app prints the groups) in a panel at the bottom. The window itself shows no result, as on the desktop. Its long HTML label is wider than the fixed 375 px window, so it is cut off on both sides. |
| MusicTheoryExpert | Works | The demo that plays on start is silent because browsers start audio only after a click or key press; the test button plays it. |
| NumberGuess | Works | |
| SimpleAlgorithms | Works | A console program: the page shows its standard output in a panel. |
| TankGame | Works with limitations | Movement, rotation and the trails left by the uncleared paint work as in the source. The tanks are drawn over the content pane's gray, not blue: the blue background panel gets no space in the layout. Every key event and paint prints to the browser console, as on the desktop. |
| BibleDownloader | Not built | It reads chapter pages from bible.com, which sends no CORS headers, so the browser blocks the request. Its 2016 page addresses also return 404 today. |
| DinoGame, Windows, WindowsE | Not built | They drive the real operating system with `java.awt.Robot` (Windows key, Win+E, reading screen pixels of another program). A web page cannot do that. |

Both the current sources and the `modernize-apps` branch sources build and run.

## How it works

### Class substitution

The shim lives in `shim/src/main/java/web/shim/`, mirroring the JDK package names with a `T` prefix: `web.shim.java.awt.TColor` is compiled wherever bytecode refers to `java.awt.Color`. TeaVM 0.16 configures this with a `SubstitutionPolicy` service (`web.teavm.ShimSubstitutionPolicy`, registered in `META-INF/services`). The older `META-INF/teavm.properties` keys (`mapPackageHierarchy`, `stripPrefixFromPackageHierarchyClasses`) no longer exist in 0.16.

Two consequences:

- Rules are tried in classpath order, and teavm-classlib has its own small `Color`, `Dimension` and `Point`. The shim jar must come before teavm-classlib in each app module's dependencies.
- TeaVM renames references only inside substitute classes. A shim helper that mentions a `T` class must itself be a substitute (they live in `web.shim.java.awt.web`, e.g. `TDesktop`, standing in for a made-up `java.awt.web.Desktop`). Helpers in `web.rt` (`Js`, `Edt`, `Exit`, `Sleep`, `Net`, `Files`, `Keys`) never mention shim types.

Also substituted: `java.beans.PropertyVetoException`, `java.net.URLClassLoader` (looks classes up among those compiled into the page) and `java.util.jar.JarFile`/`Manifest`/`Attributes` (TeaVM's JarFile cannot read from its in-memory file system).

### Compile-time rewrites

`web.teavm.ShimPlugin` rewrites a few JDK calls in app bytecode only, so app sources stay untouched:

- `Thread.sleep` goes to `web.rt.Sleep`, which throws for negative values like the JDK. Minesweeper's sound helper only stops looping because of that exception.
- `System.exit` goes to `web.rt.Exit`, which closes the app's windows, shows a "Run again" box (with suggestions of other apps, see App pages) and unwinds the calling thread.
- `URL.openStream` and `URLConnection.getInputStream` go to `web.rt.Net`, which throws `IOException` when a request fails. TeaVM's XHR connection otherwise returns an empty response.

### Windows and widgets

Every component owns an absolutely positioned DOM element; Java keeps the real state (bounds, colors, text) because apps compare colors and strings by identity. Layout is done in Java by the shim's `FlowLayout`, `BorderLayout`, `SpringLayout` and null layout, the same way Swing does it, with preferred sizes measured using the same CSS font the DOM uses.

- `JFrame` and `JInternalFrame` are boxes with a title bar (drag to move, close button runs the default close operation). Insets are 28 px on top and 4 px elsewhere, close to Swing's decorations, since apps size their content from the window size.
- `JButton`, `JTextField`, `JComboBox` and `JLabel` are real `<button>`, `<input>`, `<select>` and `<div>` elements. A label whose text starts with `<html>` is rendered as HTML, as in Swing, and measured in a hidden element.
- `JTabbedPane` draws its tabs as DOM buttons and hides the components of the other tabs, as Swing does. `UIManager.setLookAndFeel` does nothing; the page skin is the look and feel.
- `Graphics2D.draw(Shape)` and `fill(Shape)` accept only `Rectangle`, the one shape an app uses.
- Custom painting (`paint`, `paintComponent`) goes to a `<canvas>` inside the component, created the first time `paint` draws, through a `Graphics2D` over `CanvasRenderingContext2D`. The backing store is scaled by `devicePixelRatio`. A `JFrame.paint` override draws over the window's content, as in Swing.
- Components are stacked with `z-index` so the first child is on top, as in AWT.
- The page desktop is the browser viewport, which is also what `Toolkit.getScreenSize()` returns. A window placed above or left of the page edge is drawn at the edge, so its title bar stays reachable.

### Events and threads

TeaVM runs Java threads as green threads on the JavaScript event loop. DOM event handlers cannot block, so they only queue work for the shim's event dispatch thread (`web.rt.Edt`), which runs listeners, timers and paint passes one at a time, like Swing's EDT. Listener code there may call `Thread.sleep`.

- Repaints are coalesced into one paint pass per animation frame (with a timer fallback for hidden tabs). A thread other than the EDT that calls `repaint()` waits for that pass, so loops like FlappyBird's `while (true) panel.repaint();` run once per frame instead of hanging the tab.
- `javax.swing.Timer` runs on `setTimeout`; `SwingUtilities.invokeLater` and `EventQueue.invokeLater` queue on the EDT.
- Mouse events go to the deepest component with mouse listeners; key events go through `KeyboardFocusManager` dispatchers, then to the focus owner (the first focusable widget of the active window, or the window itself). DOM key codes are mapped to AWT `VK_` codes.
- `java.awt.Robot` can only reach the page: `keyPress`/`keyRelease` queue key events for the focused component, and `getPixelColor` samples the canvas or widget color at that viewport point.

### Standard output

`boot.js` defines `$rt_putStdoutCustom` before `classes.js` loads, so TeaVM hands it `System.out` text. Every page logs it to the browser console line by line. Pages with `<body data-console>` (SimpleAlgorithms, GroupCreator) also show it in a panel docked at the bottom of the page.

### Images, files and sound

- Each app page lists the files it needs in `<body data-files>`. `boot.js` fetches them before `main()` runs; images are decoded so their sizes are known, and every file is copied into TeaVM's in-memory file system so `java.io` finds it by the original relative path. Writes stay in memory.
- Image lookups also match case-insensitively, like the Windows file system the apps were written on (NathaNOS asks for `assets/Clock.png`; the file is `clock.png`).
- `Image.getScaledInstance` is asynchronous in AWT: the first draw of a scaled image draws nothing. The shim keeps that, since FlappyBird's start check waits for that first, still-black frame.
- `javax.sound.midi` is a 16-channel synthesizer on the Web Audio API: one oscillator per note with an attack and release, the waveform picked by the General MIDI program family, and pitch bend as detune. Browsers start audio only after a click or key press.

### NathaNOS and reflection

NathaNOS loads the `Main-Class` of each jar in `assets/` with `URLClassLoader`, then calls its `(Boolean)` constructor or its `main` method by reflection. In the browser nothing can be loaded at run time, so the `nathanos` module compiles `assets/Clock.jar` and the other browser apps' sources in with NathaNOS:

- Its entry point, `web.nathanos.Boot`, registers each app's main class in `web.rt.Classes` and then starts NathaNOS. That makes TeaVM compile the classes, and lets the shim's `URLClassLoader` find them by name. TeaVM's own `Class.forName` only finds classes the program creates instances of, which leaves out most apps' `Main` (just a static `main`).
- `web.teavm.ShimReflectionPolicy` keeps the constructors and `main` method of every app class that declares a `main`.
- Each app gets a jar in `assets/` holding only a manifest that names its `Main-Class` (built with Ant's `jar` task), since NathaNOS lists jars to build its taskbar and reads only their manifests. The page preloads those jars, plus FlappyBird's images and StockChecker's files.
- NathaNOS's desktop window is not focusable, so `TDesktop` does not raise it when it is clicked, and apps stay in front. It also switches every app window to `DISPOSE_ON_CLOSE` through `Toolkit.addAWTEventListener` (`WINDOW_OPENED`), so closing an app does not run `System.exit`.

Adding an app takes its sources and a jar entry in `nathanos/pom.xml`, a line in `Boot`, and its jar and files in `data-files` in `site/NathaNOS/index.html`.

## Layout

- `pom.xml`: parent build (TeaVM version, shared plugin settings).
- `shim/`: the compatibility layer and the TeaVM compiler extensions (`web.teavm`).
- `apps/`: compiles every app except NathaNOS straight from `<App>/src`, one TeaVM run per app, and copies the static site and app files.
- `nathanos/`: NathaNOS plus `assets/Clock.jar` and the other browser apps, in one page (Clock.jar's `clock.Main` clashes with the Clock app's, so the Clock sources are left out).
- `site/`: `index.html`, one `<App>/index.html` per app, `boot.js`, `shim.css` (the Modern, Classic and Arcade skins) and `shots/` (the home page thumbnails).
- `shots.ps1`: recaptures `site/shots/` from a built site (see Home page).

## Home page

The home page opens with a player: Minesweeper is already running in it, and every card's Play button (or thumbnail) swaps that app into the player without leaving the page. Full page opens the app's own page, as do Ctrl-, Shift- and middle-clicks on Play. Selecting an app puts it in the address (`index.html#FlappyBird`), and loading that address starts the page with that app in the player. The filter buttons narrow the cards to games, music, toys or tools.

The player is built to keep a visitor playing without scrolling back to the gallery:

- A rail of thumbnails under the screen lists all 16 apps; clicking one swaps it in. The current app is outlined and the rail scrolls to keep it in view. The arrows at either end play the previous or next app (wrapping around), and Shuffle plays a random app other than the current one.
- Big screen fills the monitor with the player (bar, screen and rail) through the Fullscreen API, where the screen gets all the room the bar and the rail leave. Where the browser refuses fullscreen (inside an iframe without `allowfullscreen`, for example), the player covers the browser window instead, and the button (or Esc, while the page rather than the app has focus) leaves it.

- The player is an iframe of `<App>/index.html?embed`. In embed mode `boot.js` leaves out the toolbar and keeps the app's windows centered as a group, since most apps open their window at the top-left corner of the screen. It recenters only when a window opens, closes or changes size, or the viewport does, so dragging a window or moving it with the arrow keys (Minesweeper) works as usual.
- The iframe's viewport is at least 900 x 520 (720 tall for Rubik's Cube, whose window is 700 px tall) and is scaled down to the width of the page, so apps that size their window from the screen get a desktop-sized screen on a phone too. The player is never taller than the browser window minus room for its bar and rail.
- Each new app gets a new iframe, so the previous app's timers and sound stop. Restart reloads the current one.
- A skin change on any page reaches every other open page of the site, the player included, through the `storage` event.
- The thumbnails in `site/shots/` are screenshots of the apps in the Modern skin, taken with the system's color scheme. To retake them after a visual change, build the site, then run `./web/shots.ps1` from the repo root (`-Apps Minesweeper,Clock` for some apps only). It serves the built site on port 8101 with Python, opens each app page in headless Chrome in thumbnail mode (`?embed&shot`: square, shadowless windows on a magenta desktop, without the output panel unless it is all the app shows), crops each image to the windows, makes the remaining magenta transparent and scales it down to at most 480 px wide.

## App pages

Each app page (`<App>/index.html`) is built to lead to the next app as well as run its own:

- The toolbar in the top-right corner has All apps, Switch app, a Next link (the following app in gallery order, wrapping around; hidden on pages narrower than 560 px) and the skin picker.
- Switch app opens a panel on the right with every app's thumbnail, name and hint, the current app marked "Now playing". Esc, the close button or a click outside the panel closes it.
- A How to play card with the app's hint shows under the toolbar when the app starts (at the bottom on narrow pages) and fades after 12 seconds.
- When the app exits (for most apps, when its window is closed), the Run again box names the app, keeps Run again and All apps, and suggests three other apps with their thumbnails: apps not played yet first (see Arcade passport), and within that, apps of the same category first, then the others, each group in random order. The box ends with the passport count. In the home page's player the box has no All apps link, and picking a suggestion asks the home page (with `postMessage`) to swap that app into the player, so its title, rail and address follow.

`boot.js` reads the list of apps (name, hint, category, thumbnail, order) from the gallery in `../index.html`, so the home page stays the one place to add or describe an app. Keys typed in these panels are kept from the app, since the shim listens for keys on the whole document. Thumbnail mode (`?embed&shot`) leaves all of this out.

## Arcade passport

The site keeps a stamp for every app the visitor has played, to give them a reason to try all 16:

- An app is stamped by the first click or key press in the app itself, on its page or in the home page's player (clicks and keys in the site's own panels do not count, so the Minesweeper that the home page starts with is not stamped just by loading). SimpleAlgorithms and GroupCreator (`<body data-console>`) are stamped once they run.
- A new stamp shows a toast at the top of the page: the app's thumbnail pressed in as a stamp, "N of 16 apps played" with a bar, and a Try link to an app not played yet (same category first). In the player the toast is shown by the home page, and its Try link swaps that app into the player. The 16th stamp shows "Arcade complete!" with a short burst of confetti (left out when the system asks for reduced motion).
- The home page has an Arcade passport panel under the player: a progress ring, the 16 stamps (gray and dashed until played; click one to play it), "Play one you haven't" (a random unplayed app) and Reset (click twice). Gallery cards carry a New or Played badge, and the player's rail puts a check on played apps.
- The Switch app panel shows the count at the top and checks played apps; the exit screen's suggestions carry New badges.

The stamps are `{ "AppId": time of first play }` under `oldjava.played` in `localStorage` (only in that browser), kept by `boot.js` as `window.oldjavaPassport`. Every open page of the site hears new stamps through the `storage` event, as with the skin.

## Touch controller

The keyboard games get an on-screen controller, so they can be played on a phone or tablet. Its buttons press and release the same keys the app listens for, taken from each app's sources:

| App | Controller |
|---|---|
| FlappyBird | Flap (Space), Pause (P), Reset (R) |
| TankGame | Two players on one screen: a cross (W A S D) and Fire (Q) for the red tank, a cross (arrow keys) and Fire (M) for the green tank |
| SynthesizerKeyboard | One octave of piano keys (Z S X D C V G B H N J M ,), each held for as long as it is touched, plus Octave and Sound buttons (arrow keys) |
| SpriteFloatyStars | Add star (Up), Remove star (Down) |

- Each button shows its key, and lights up when that key is pressed on a real keyboard, so the controller doubles as a key chart.
- On the home page the controller is docked under the player's screen (and goes with it to Big screen) whenever one of these apps is playing. Its presses reach the player's iframe through `postMessage`, and the iframe reports real key presses back to light the buttons. Controls in the player bar hides or shows it.
- On an app page it floats at the bottom, and Controls in the toolbar hides or shows it. It starts shown where the main pointer is a finger (`pointer: coarse`) and hidden elsewhere. On pages narrower than 560 px the How to play card moves above it.
- The choice made with either Controls button is remembered in `localStorage` (`oldjava.pad`).
- Buttons never take keyboard focus, so the app keeps the keyboard. A press counts as playing for the arcade passport.
- The keys are synthetic `KeyboardEvent`s dispatched on the page, with `keyCode` set the way the shim reads it, so the apps and the shim are unchanged. The layouts are the `PADS` table in `boot.js`. Every skin styles it: a dark glass console in Modern, a Metal panel in Classic, neon buttons that light up magenta in Arcade.

## Skins

The picker in the top-right corner switches between three skins:

- Modern (the default): flatter, rounded, follows the system dark mode.
- Classic: close to Swing's default Metal look.
- Arcade: every app as if on a CRT arcade cabinet. Windows get neon magenta frames with a glow, cyan title text and buttons, and phosphor-green text fields and output panels, on a dark purple desk with a faint grid. Fine scanlines and a vignette cover the page, and a faint bright band rolls down it. When an app starts, its windows open from a bright line like a tube warming up, so every app swapped into the home page's player powers on too. The home page turns into a neon cabinet as well (glowing title, player and buttons).

The home page's player bar has an Arcade mode button that switches every open page of the site to Arcade and back to the skin from before. The choice is remembered in `localStorage` (`oldjava.skin`). Skins only change colors and decoration; sizes and widget fonts come from Java, so the layout is the same in all three. Colors an app sets itself (Minesweeper's green tiles, for example) are kept in every skin.

In Arcade the scanlines and the rolling band are pseudo-elements of the page's `body` that never take pointer events. Pages in the home page's player leave them out (the home page draws them once over the player), and so does thumbnail mode. The power-on animates the CSS `scale` property of `#desktop`, so it does not disturb the `transform` that centers the windows in the player. The rolling band and the power-on are left out when the system asks for reduced motion. The thumbnails stay Modern screenshots in every skin.

## Limitations

- Only the AWT/Swing API these apps use is implemented. A new app, or a change that calls something missing, fails the build with "Method ... was not found".
- Windows cannot be resized by dragging their edges, and there are no minimize or maximize buttons.
- `Robot` cannot see or control anything outside the page.
- Nothing an app saves is kept across page loads (only the skin and the passport stamps are).
