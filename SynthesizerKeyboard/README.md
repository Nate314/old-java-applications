# SynthesizerKeyboard

A one-octave piano (C to C, 8 white and 5 black keys) that plays through the Java MIDI synthesizer. Play it with the computer keyboard; the keys can also be clicked, but see the quirks. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Computer keyboard**: hold to sound a note, release to stop. The key on screen turns darker while held.
  - White keys (C to C): `Z X C V B N M ,`
  - Black keys: `S D G H J`
- **Left / Right**: shift the computer-keyboard notes down / up one octave. Clicks are not shifted.
- **Up / Down**: next / previous MIDI instrument (program 0 to 120).
- **Click a key**: starts that note (MIDI 60 to 72) and is meant to stop it after 150 timer ticks.
- The window is 3/4 of the screen wide and 1/2 tall. Title "AWT Synthesizer".
- No command-line arguments.

## Code layout

- `src/synthesizer/Main.java`: entry point; creates a `KeyboardWindow`.
- `src/synthesizer/KeyboardWindow.java`: the window; lays out the keys on each paint and handles clicks and key presses.
- `src/synthesizer/KeyButton.java`: empty `JButton` subclass used for the keys.
- `src/synthesizer/MidiPlayer.java`: opens the synthesizer; note on/off, instrument change and timed notes.
- `src/synthesizer/TickCounter.java`: 1 ms Swing timer action that increments the window's tick count.
- `src/synthesizer/NoteNameConverter.java`: converts a note name such as `C5` to a number.

## Running

From the repo root: `./run.ps1 SynthesizerKeyboard` (or `./run.ps1 SynthesizerKeyboard -Build` to build a jar).

## Known quirks

- A click waits for the tick count to advance 150 ticks, but it waits on the Swing event thread, which is also the thread the tick timer runs on. From reading the code (not confirmed by running it), the count should never advance, so the first click likely freezes the window.
- Key presses are handled by a listener on the frame. Swing sends key events to the focused component, so if a key button takes focus the computer keyboard may stop responding.
- `paint()` creates a fresh set of key buttons every time it runs and only hides and disables the old ones.
- Changing octave while a key is held sends note-off for the new octave, so the held note keeps sounding.
- `noteOn` uses velocity 1000, outside MIDI's 0 to 127 range.
- The window icon is loaded from `icon.jpg`, which is not in the repo.
- `NoteNameConverter` compares strings with `==`, so it returns 0; the constructor prints that 0. `MidiPlayer.noteNumbers` is filled but never read, and `playNote` ignores every argument except the note.
