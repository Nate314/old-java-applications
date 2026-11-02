# Java Applications

Sources recovered from the jars published at https://nathangawith.com/applications/java, decompiled with Vineflower 1.10.1.

CircleSpawner, DinoGame, StockChecker, SynthesizerKeyboard, Windows and WindowsE are no longer on the site. They were recovered from the git history of https://github.com/Nate314/site (`assets/javaApplications/`).

BibleDownloader, GroupCreator, MusicTheoryExpert, NumberGuess, SimpleAlgorithms and TankGame came from the author's own files. MusicTheoryExpert, NumberGuess, SimpleAlgorithms and TankGame are the original Eclipse source (only `src/` is kept). BibleDownloader (`BibleDownloader1.3.1.jar`) and GroupCreator were decompiled. `AudioBibleDownloader1.1.jar` was left out: BibleDownloader 1.3.1 is a later version of the same app that keeps its MP3 download and adds chapter text.

Each project has its sources in `src/` (decompiled ones with their original `META-INF/MANIFEST.MF`) and its own README.

| App | Description |
|---|---|
| [Clock](Clock/README.md) | Analog/text clock, plus a timer set by dragging a 60-minute dial (it keeps counting while the window is closed, then rings) and a stopwatch with laps |
| [FlappyBird](FlappyBird/README.md) | Flappy Bird style game with endless pipes or today's course (race the ghost of your best run that day), and medals |
| [Minesweeper](Minesweeper/README.md) | Minesweeper game |
| [NathaNOS](NathaNOS/README.md) | Desktop-style app launcher with icons, a start menu and a taskbar |
| [RubiksCube2D](RubiksCube2D/README.md) | 2D Rubik's Cube puzzle with 20 levels (watch it get mixed, turn it back for stars), Hint, Undo and Solve |
| [ScreenSaver](ScreenSaver/README.md) | Screen saver that closes when the mouse moves |
| [SpriteFloatyStars](SpriteFloatyStars/README.md) | Animated stars with five modes, and Star Catcher: click gold stars over those modes across six nights, dodging black holes |
| [UltimateTicTacToe](UltimateTicTacToe/README.md) | Ultimate Tic-Tac-Toe against an Easy or Hard computer, or for two players |
| [CircleSpawner](CircleSpawner/README.md) | Builds a circle (the original), ring, heart, star or a typed word out of borderless windows, growing in and bursting apart when clicked |
| [DinoGame](DinoGame/README.md) | Bot that plays the Chrome dino game by watching a screen pixel |
| [StockChecker](StockChecker/README.md) | Editable ticker list (seeded from `myStocks.txt`) that fetched quotes from the retired Yahoo Finance CSV API |
| [SynthesizerKeyboard](SynthesizerKeyboard/README.md) | One-octave MIDI piano keyboard played with the computer keyboard, with five song lessons that light the next key and award stars |
| [Windows](Windows/README.md) | Presses the Windows key once |
| [WindowsE](WindowsE/README.md) | Opens N File Explorer windows with Win+E |
| [BibleDownloader](BibleDownloader/README.md) | Downloads NIV Bible chapters from bible.com as MP3 audio or as text saved to HTML |
| [GroupCreator](GroupCreator/README.md) | Deals a list of names into random groups on cards, remembers every round so the next one gives people new partners, swaps names by hand, and charts who has been grouped with whom |
| [MusicTheoryExpert](MusicTheoryExpert/README.md) | Ear trainer built on the author's unfinished music theory code: six drills (chords, intervals, scales, chords in a key) with stars, and the original I-IV-V-I demo |
| [NumberGuess](NumberGuess/README.md) | Guess-the-number game in five ranges up to 1,000,000, with par, stars and a number line you can click, or the computer guesses yours |
| [SimpleAlgorithms](SimpleAlgorithms/README.md) | Hand-written square root, sine, cosine and Tower of Hanoi algorithms, printed and then shown step by step in an Algorithm Lab window (watch or play the Hanoi solution, see the guesses converge) |
| [TankGame](TankGame/README.md) | Tank duel, first to three rounds, against the computer or a second player on the arrow keys |

## Notes

- Most jars were obfuscated (single-letter names like `a`, `b`). Those identifiers have been renamed by hand to descriptive English names. NathaNOS and ScreenSaver were not obfuscated.
- The apps have since been modernized: the bugs listed in the per-app READMEs were fixed, the UIs start with `SwingUtilities.invokeLater`, animation and timed work runs on `javax.swing.Timer` instead of sleeping or busy-waiting on the event thread, and a few small features were added (each app README lists its controls). Apps with fixed game logic have an assert-based self-check under `<App>/test/`.
- The obfuscator stripped inner-class metadata, so a few files were fixed by hand to compile:
  - `Clock/src/clock/ClockTicker.java`: added the outer `Main` field and replaced synthetic accessors with direct field reads.
  - `FlappyBird/src/flappyBird/RenderLoop.java`: added the outer `GamePanel` field and inlined the synthetic accessors (verified against the original bytecode). `RenderLoop` was later removed when the game moved to a Swing timer.
  - `NathaNOS/src/main/StartMenu.java`: repaired the anonymous `JPanel` subclass.
  - `DinoGame/src/dinosaurgameplayer/ObstacleDetector.java`: added the outer fields and replaced the synthetic accessor with a direct call to the sleep helper (verified against the original bytecode). The sleep helper was later replaced by a one-shot Swing timer.
  - `SynthesizerKeyboard/src/synthesizer/TickCounter.java`: added the outer `KeyboardWindow` field. `KeyboardWindow.java`: added `(Integer)` casts the decompiler dropped. Both were later replaced when click-to-play was fixed.
  - `Windows/src/test/Test.java`: added `throws Exception` to `main`, since javac requires the checked exceptions the bytecode leaves undeclared.
- In BibleDownloader (`DownloadManager.java`) and GroupCreator (`Main.java`), the decompiler could not rebuild three try-with-resources blocks, so they were rewritten as `try (Writer writer = ...)`.
- Windows and DinoGame send real key presses with `java.awt.Robot` as soon as they start (the Windows key, and Space to play the Chrome dino game). WindowsE presses Win+E repeatedly when its button is clicked, opening File Explorer windows.
- StockChecker downloads quotes from the retired Yahoo Finance CSV API into `stockCheck.csv`, using the tickers in `myStocks.txt`, and saves list edits back to `myStocks.txt`.
- FlappyBird and NathaNOS load images by relative path, so run them from the folder holding the images:
  - FlappyBird: run from `FlappyBird/src` with a numeric argument, e.g. `java flappyBird.Main 120` (the original `run.bat` used 120).
  - NathaNOS: run from a folder holding its `assets/`. `assets/Clock.jar` is an older Clock build that shipped in the zip, kept as a binary asset. `./run.ps1 NathaNOS` stages a copy in `out/NathaNOS/desktop/` with every other app except the three `Robot` apps on its desktop.

## Build and run

Requires a JDK (`java` and `javac` on PATH). From the repo root in PowerShell:

```powershell
./run.ps1                      # prompts for an app, then compiles and runs it
./run.ps1 Minesweeper          # compiles and runs Minesweeper
./run.ps1 Minesweeper -Build   # builds out/Minesweeper.jar instead of running
```

Run mode launches each app from the folder it expects (for example `FlappyBird/src` with argument `120`), with any non-Java files in `src/` on the classpath. Build mode prints the matching `java -jar` command. Build output goes to `out/`, which is git-ignored.

## Web (TeaVM)

Every app except DinoGame, Windows and WindowsE also runs in a browser, compiled to JavaScript with TeaVM on a small AWT/Swing compatibility layer. Requires Maven 3.9. From the repo root:

```powershell
mvn -f web/pom.xml package                               # builds the static site into web/target/site
python -m http.server 8000 --directory web/target/site   # then open http://localhost:8000/
```

What the apps save is kept in the browser between visits, as it would be on a desktop disk: Minesweeper (which now has Beginner, Classic and Expert boards, a clock and best times) keeps its best times, FlappyBird (which now has today's course, with a ghost of the day's best run, and medals) its best score and today's best run, Ultimate Tic-Tac-Toe (which now has an Easy and a Hard computer opponent) and TankGame (now a finished game: health, rounds and a computer opponent) your record against the computer, SynthesizerKeyboard (which now teaches five songs, lighting the next key) its best stars per song, RubiksCube2D (now a puzzle of 20 levels, each mixed one move more while you watch, with Hint, Undo and Solve) its stars per level, MusicTheoryExpert (now an ear trainer with six drills, from major or minor chords to chords in a key) its best score per drill, NumberGuess (now five ranges from 1-10 to 1-1,000,000, with par, stars, a clickable number line and a mode where the computer guesses your number) its fewest guesses per range, SpriteFloatyStars (which now has Star Catcher, a game of six nights played over its animation modes) the most time left per night, GroupCreator (which now remembers every round it makes, so the next one gives people partners they have not had yet, with a chart of who has met whom) its class and rounds, Clock (which now has a Timer tab, set by dragging a red wedge round a 60-minute dial, and a Stopwatch tab with laps) its timer and stopwatch, still counting when the page is opened again and shown in the browser tab's title while the timer runs, and StockChecker its ticker list, and the title screen shows the visitor's record. SimpleAlgorithms, once a console program, now opens an Algorithm Lab that animates its Tower of Hanoi solution and its square root and sine estimates. The home page runs Minesweeper in a player as soon as it opens, and swaps any other app into it from the rail of thumbnails under the player (with next, previous and shuffle buttons, and a Big screen mode that fills the monitor) or the gallery below. Each app page is a stage: a toolbar band across the top with the app's name, and the app's windows centered in the space between it and the touch controller, which the app sees as its screen (scaled down when a window is still too big), so the page's own controls never cover the app. Each app page opens on a title screen (thumbnail, hint, passport stamp and a live log of the files it loads) and starts the app on Play or any key; it also has a Switch app panel, a Next link and a How to play card, and when an app is closed it suggests three others to play next. An arcade passport stamps every app the visitor plays (saved in the browser), shows a toast pointing at one not played yet, and marks New and Played apps across the site. The keyboard games (FlappyBird, TankGame, SynthesizerKeyboard, SpriteFloatyStars) get an on-screen touch controller, so they can be played on a phone. Every app page can also show the Java it runs beside the running app: Source opens a panel with the app's README and highlighted sources, with linked class names and line links. The site's own buttons and moments (swapping an app in, a new stamp, an app closing, a skin change) make short synthesized sounds in a voice that follows the skin, with a Sound button to mute them. Share (on every app page, the home page's player, the passport and the exit screen) shows the link preview card the app's page gets when posted, with a message carrying the visitor's passport count, and sends the link to the system share sheet, X, Bluesky, Reddit, email or the clipboard; every page has description, Open Graph and Twitter tags with its own 1200 x 630 card. The site can be installed as an app, and Save all for offline on the home page keeps all 16 apps (about 9 MB) in the browser so they play with no connection. See [web/README.md](web/README.md) for how it works, per-app status and limitations.
