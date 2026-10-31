# SynthesizerKeyboard

A one-octave piano (C to C, 8 white and 5 black keys) that plays through the Java MIDI synthesizer. Play it with the computer keyboard or by clicking the keys. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Computer keyboard**: hold to sound a note, release to stop. The key on screen turns darker while held.
  - White keys (C to C): `Z X C V B N M ,`
  - Black keys: `S D G H J`
- **Left / Right**: shift the notes down / up one octave (5 down to 4 up). Notes already held keep the pitch they started with and stop when released.
- **Up / Down**: next / previous MIDI instrument (program 0 to 120).
- **Click a key**: plays that note (MIDI 60 to 72, plus the octave shift) for 150 ms.
- The window is 3/4 of the screen wide and 1/2 tall. The title shows the current instrument and octave shift.
- No command-line arguments.

## Code layout

- `src/synthesizer/Main.java`: entry point; creates a `KeyboardWindow`.
- `src/synthesizer/KeyboardWindow.java`: the window; creates the keys once, re-lays them out when the window size changes, and handles clicks and key presses.
- `src/synthesizer/KeyButton.java`: empty `JButton` subclass used for the keys.
- `src/synthesizer/MidiPlayer.java`: opens the synthesizer; note on/off, instrument change and timed notes (stopped by a one-shot Swing timer).

The old `TickCounter` (a 1 ms tick timer used to time clicked notes) and the unused `NoteNameConverter` were removed.

## Running

From the repo root: `./run.ps1 SynthesizerKeyboard` (or `./run.ps1 SynthesizerKeyboard -Build` to build a jar).
