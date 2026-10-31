# Java Applications

Sources recovered from the jars published at https://nathangawith.com/applications/java, decompiled with Vineflower 1.10.1.

CircleSpawner, DinoGame, StockChecker, SynthesizerKeyboard, Windows and WindowsE are no longer on the site. They were recovered from the git history of https://github.com/Nate314/site (`assets/javaApplications/`).

BibleDownloader, GroupCreator, MusicTheoryExpert, NumberGuess, SimpleAlgorithms and TankGame came from the author's own files. MusicTheoryExpert, NumberGuess, SimpleAlgorithms and TankGame are the original Eclipse source (only `src/` is kept). BibleDownloader (`BibleDownloader1.3.1.jar`) and GroupCreator were decompiled. `AudioBibleDownloader1.1.jar` was left out: BibleDownloader 1.3.1 is a later version of the same app that keeps its MP3 download and adds chapter text.

Each project has its sources in `src/` (decompiled ones with their original `META-INF/MANIFEST.MF`) and its own README.

| App | Description |
|---|---|
| [Clock](Clock/README.md) | Analog/text clock |
| [FlappyBird](FlappyBird/README.md) | Flappy Bird style game |
| [Minesweeper](Minesweeper/README.md) | Minesweeper game |
| [NathaNOS](NathaNOS/README.md) | Desktop-style app launcher |
| [RubiksCube2D](RubiksCube2D/README.md) | 2D Rubik's Cube |
| [ScreenSaver](ScreenSaver/README.md) | Screen saver that closes when the mouse moves |
| [SpriteFloatyStars](SpriteFloatyStars/README.md) | Animated stars with several modes |
| [UltimateTicTacToe](UltimateTicTacToe/README.md) | Ultimate Tic-Tac-Toe |
| [CircleSpawner](CircleSpawner/README.md) | Draws a filled circle of a chosen diameter out of overlapping borderless windows |
| [DinoGame](DinoGame/README.md) | Bot that plays the Chrome dino game by watching a screen pixel |
| [StockChecker](StockChecker/README.md) | Editable ticker list (seeded from `myStocks.txt`) that fetched quotes from the retired Yahoo Finance CSV API |
| [SynthesizerKeyboard](SynthesizerKeyboard/README.md) | One-octave MIDI piano keyboard played with the computer keyboard |
| [Windows](Windows/README.md) | Presses the Windows key once |
| [WindowsE](WindowsE/README.md) | Opens N File Explorer windows with Win+E |
| [BibleDownloader](BibleDownloader/README.md) | Downloads NIV Bible chapters from bible.com as MP3 audio or as text saved to HTML |
| [GroupCreator](GroupCreator/README.md) | Shuffles a comma-separated list of names into random groups, shows them and writes them to `output.txt` |
| [MusicTheoryExpert](MusicTheoryExpert/README.md) | Unfinished music theory tool that builds scales and triads and plays a I-IV-V-I progression |
| [NumberGuess](NumberGuess/README.md) | Guess-the-number game |
| [SimpleAlgorithms](SimpleAlgorithms/README.md) | Console program printing hand-written estimates of square root, sine, cosine and more |
| [TankGame](TankGame/README.md) | Unfinished two-player tank game driven with WASD and the arrow keys |

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
  - NathaNOS: run from `NathaNOS/` so `assets/` resolves. `assets/Clock.jar` is an older Clock build that shipped in the zip, kept as a binary asset.

## Build and run

Requires a JDK (`java` and `javac` on PATH). From the repo root in PowerShell:

```powershell
./run.ps1                      # prompts for an app, then compiles and runs it
./run.ps1 Minesweeper          # compiles and runs Minesweeper
./run.ps1 Minesweeper -Build   # builds out/Minesweeper.jar instead of running
```

Run mode launches each app from the folder it expects (for example `FlappyBird/src` with argument `120`), with any non-Java files in `src/` on the classpath. Build mode prints the matching `java -jar` command. Build output goes to `out/`, which is git-ignored.
