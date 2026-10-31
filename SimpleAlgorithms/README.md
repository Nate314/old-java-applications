# SimpleAlgorithms

A console program that prints hand-written math estimates: a square root, sine and cosine from a Taylor series, a factorial, a power and a division. Also holds a Tower of Hanoi move printer that `main` does not call. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- No window and no input. Running it prints, in order:
  - `sqrt(42) ~= ...` (Newton's method)
  - `sin(pi/4) ~= ...` and `cos(pi/4) ~= ...` (4 Taylor terms)
  - `5! = 120` and `5^3 = 125.0`
  - each step of the division search, then `4/2 = ...`
- No command-line arguments.

## Code layout

- `src/simplealgorithms/Main.java`: entry point; prints the results above.
- `src/simplealgorithms/MyMath.java`: square root, sine, cosine, power, factorial, and add/subtract/multiply/divide built from simpler operations.
- `src/simplealgorithms/Hanoi.java`: prints the moves (`Move <disk> left/right`) to solve a Tower of Hanoi of any size. Its call in `main` is commented out.
- `src/simplealgorithms/Conversions.java`: empty class.

## Running

From the repo root: `./run.ps1 SimpleAlgorithms` (or `./run.ps1 SimpleAlgorithms -Build` to build a jar).

## Known quirks

- The sine estimate is wrong: it prints `sin(pi/4) ~= -1.414...`, which is -2 sin(x). `estimateSine` adds each Taylor term twice (the same term is written once with `*` and once with `multiply`), and the sign factor `2 * (i % 2) - 1` is -1 for the first term, so every sign is flipped.
- `estimateCosine` calls `estimateSine(x - pi/2)`, so it has the same error and gives 2 cos(x), about 1.414, instead of 0.707.
- The last line is labelled `4/2` but calls `divide(5, 3)`.
- `factorial` returns an `int`, so it overflows past 12!. The sine only needs up to 7!.
