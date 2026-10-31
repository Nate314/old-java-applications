# Java Applications

Sources recovered from the jars published at https://nathangawith.com/applications/java, decompiled with Vineflower 1.10.1.

Each project has its sources in `src/` along with the original `META-INF/MANIFEST.MF`.

## Notes

- Most jars were obfuscated, so classes and fields have single-letter names (`a`, `b`, ...). NathaNOS and ScreenSaver were not obfuscated.
- The obfuscator stripped inner-class metadata, so a few files were fixed by hand to compile:
  - `Clock/src/clock/b.java`: added the outer `Main` field and replaced synthetic accessors with direct field reads.
  - `FlappyBird/src/flappyBird/d.java`: added the outer `c` field and inlined the synthetic accessors (verified against the original bytecode).
  - `NathaNOS/src/main/StartMenu.java`: repaired the anonymous `JPanel` subclass.
- FlappyBird and NathaNOS load images by relative path, so run them from the folder holding the images:
  - FlappyBird: run from `FlappyBird/src` with a numeric argument, e.g. `java flappyBird.Main 120` (the original `run.bat` used 120).
  - NathaNOS: run from `NathaNOS/` so `assets/` resolves. `assets/Clock.jar` is an older Clock build that shipped in the zip, kept as a binary asset.

## Build and run

```sh
cd Minesweeper
javac -d out $(find src -name '*.java')
java -cp out minesweeper.Main
```
