# MusicTheoryExpert

An early, unfinished music theory tool. It builds major and minor scales and triads as MIDI note numbers, and on start plays a I-IV-V-I chord progression in G major while arpeggiating the G major triad. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- On start, two threads play at once through the Java MIDI synthesizer, one second per step:
  - the chords I, IV, V, I of G major (starting at G4, MIDI 67, each in root position) on MIDI channel 0;
  - the notes of the I chord (G, B, D) one at a time, then G5, on MIDI channel 1 (each note name is printed to stdout).
- **test** button (on the "Notes" tab): plays the same thing again.
- The window is 500x200 px and uses the system look and feel. Title "Music Theory Expert".
- No command-line arguments.

## Code layout

- `src/musictheoryexpert/Main.java`: entry point and window; owns the synthesizer and the test button.
- `src/musictheoryexpert/Notes.java`: sharp and flat note names, scale step patterns (major, natural, harmonic and melodic minor), scale and triad building, and the demo playback.
- `src/synthesizer/MySynthesizer.java`: opens the MIDI synthesizer and plays timed notes and chords on a channel.
- `src/synthesizer/Notes.java`: constants for note names across octaves (for example `A4`), a name-to-number parser and `NOT_A_NOTE`.
- `src/synthesizer/Instruments.java`: map of MIDI instrument names to program numbers. Not used.

## Running

From the repo root: `./run.ps1 MusicTheoryExpert` (or `./run.ps1 MusicTheoryExpert -Build` to build a jar).

## Self-check

`test/musictheoryexpert/NotesCheck.java` checks note name lookup (sharp and flat), scales and triads. From `MusicTheoryExpert/`:

```
javac -d ../out/check/MusicTheoryExpert src/musictheoryexpert/*.java src/synthesizer/*.java test/musictheoryexpert/*.java
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.NotesCheck
```

## Known quirks

- The `NoteNames` enum, the `transpose` method and the `Instruments` class are not used.
- Clicking **test** while the demo is still playing starts a second copy on the same channels, so the two copies overlap.
