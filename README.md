# Java Applications

Sources recovered from the jars published at https://nathangawith.com/applications/java, decompiled with Vineflower 1.10.1.

CircleSpawner, DinoGame, StockChecker, SynthesizerKeyboard, Windows and WindowsE are no longer on the site. They were recovered from the git history of https://github.com/Nate314/site (`assets/javaApplications/`).

Each project has its sources in `src/` along with the original `META-INF/MANIFEST.MF`.

## Notes

- Most jars were obfuscated (single-letter names like `a`, `b`). Those identifiers have been renamed by hand to descriptive English names; logic is unchanged. NathaNOS and ScreenSaver were not obfuscated.
- The obfuscator stripped inner-class metadata, so a few files were fixed by hand to compile:
  - `Clock/src/clock/ClockTicker.java`: added the outer `Main` field and replaced synthetic accessors with direct field reads.
  - `FlappyBird/src/flappyBird/RenderLoop.java`: added the outer `GamePanel` field and inlined the synthetic accessors (verified against the original bytecode).
  - `NathaNOS/src/main/StartMenu.java`: repaired the anonymous `JPanel` subclass.
  - `DinoGame/src/dinosaurgameplayer/a.java`: added the outer fields and replaced the synthetic accessor with a direct call to the sleep helper (verified against the original bytecode).
  - `SynthesizerKeyboard/src/synthesizer/e.java`: added the outer `d` field. `d.java`: added `(Integer)` casts the decompiler dropped.
  - `Windows/src/test/Test.java`: added `throws Exception` to `main`, since javac requires the checked exceptions the bytecode leaves undeclared.
- Windows and DinoGame send real key presses with `java.awt.Robot` as soon as they start (the Windows key, and Space to play the Chrome dino game). WindowsE presses Win+E repeatedly when its button is clicked, opening File Explorer windows.
- StockChecker downloads quotes from the retired Yahoo Finance CSV API into `stockCheck.csv`, using the tickers in `myStocks.txt`.
- FlappyBird and NathaNOS load images by relative path, so run them from the folder holding the images:
  - FlappyBird: run from `FlappyBird/src` with a numeric argument, e.g. `java flappyBird.Main 120` (the original `run.bat` used 120).
  - NathaNOS: run from `NathaNOS/` so `assets/` resolves. `assets/Clock.jar` is an older Clock build that shipped in the zip, kept as a binary asset.

## Build and run

```sh
cd Minesweeper
javac -d out $(find src -name '*.java')
java -cp out minesweeper.Main
```
