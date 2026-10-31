# MusicTheoryExpert

An early, unfinished music theory tool. It builds major and minor scales and triads as MIDI note numbers, and on start plays a I-IV-V-I chord progression in G major while arpeggiating the G major triad. Original Eclipse source from the author's own files (only `src/` was kept).

## Controls / usage

- On start, two threads play at once through the Java MIDI synthesizer, one second per step:
  - the chords I, IV, V, I of G major (starting at G4, MIDI 67);
  - the notes of the I chord (G, B, D) one at a time, then G5.
- **test** button (on the "Notes" tab): plays the same thing again.
- Each start or click also prints "Testing 123" and 90 lines of note IDs to stdout.
- The window is 500x200 px and uses the system look and feel. Title "Misic Theory Expert".
- No command-line arguments.

## Code layout

- `src/musictheoryexpert/Main.java`: entry point and window; owns the synthesizer and the test button.
- `src/musictheoryexpert/Notes.java`: note names, scale step patterns (major, natural, harmonic and melodic minor), scale and triad building, and the demo playback.
- `src/synthesizer/MySynthesizer.java`: opens the MIDI synthesizer and plays timed notes and chords on a channel.
- `src/synthesizer/Notes.java`: constants for note names across octaves (for example `A4`), a name-to-number parser and `NOT_A_NOTE`.
- `src/synthesizer/Instruments.java`: map of MIDI instrument names to program numbers. Not used.

## Running

From the repo root: `./run.ps1 MusicTheoryExpert` (or `./run.ps1 MusicTheoryExpert -Build` to build a jar).

## Known quirks

- `getNoteID` only searches the flat names when the sharp search already succeeded, so flat names such as `Bf` return 400. The demo only uses sharp names.
- Note names are compared with `==`. It works for the string literals used here, but not for names built at run time.
- Both demo threads use channel 0 and share G4, so one thread's note off can cut the other's note.
- The `NoteNames` enum, the `transpose` method and the `Instruments` class are not used.
