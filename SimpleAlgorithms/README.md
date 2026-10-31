# SimpleAlgorithms

A console program that prints hand-written math estimates: a square root, sine and cosine from a Taylor series, a factorial, a power and a division. Also holds a Tower of Hanoi move printer that `main` does not call. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- No window and no input. Running it prints, in order:
  - `sqrt(42) ~= ...` (Newton's method)
  - `sin(pi/4) ~= ...` and `cos(pi/4) ~= ...` (4 Taylor terms each, about 0.70711)
  - `5! = 120` and `5^3 = 125.0`
  - each step of the division search, then `4/2 = ...`
- No command-line arguments.

## Code layout

- `src/simplealgorithms/Main.java`: entry point; prints the results above.
- `src/simplealgorithms/MyMath.java`: square root, sine and cosine (one shared Taylor series), power, factorial, and add/subtract/multiply/divide built from simpler operations.
- `src/simplealgorithms/Hanoi.java`: prints the moves (`Move <disk> left/right`) to solve a Tower of Hanoi of any size. Its call in `main` is commented out.
- `src/simplealgorithms/Conversions.java`: empty class.

## Running

From the repo root: `./run.ps1 SimpleAlgorithms` (or `./run.ps1 SimpleAlgorithms -Build` to build a jar).

## Self-check

`test/simplealgorithms/MyMathCheck.java` checks the sine, cosine, square root, factorial, power and division results against `java.lang.Math`. From `SimpleAlgorithms/`:

```
javac -d ../out/check/SimpleAlgorithms src/simplealgorithms/*.java test/simplealgorithms/*.java
java -ea -cp ../out/check/SimpleAlgorithms simplealgorithms.MyMathCheck
```

## Known quirks

- `factorial` returns a `long`, so it overflows past 20!. Sine and cosine only need up to 7! at the default 4 terms.
