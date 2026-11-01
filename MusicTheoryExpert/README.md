# MusicTheoryExpert

An ear trainer built on an early, unfinished music theory tool. The original code builds major and minor scales and triads as MIDI note numbers and plays a I-IV-V-I chord progression in G major; the Ear training tab now uses those scales and triads to quiz you by ear. Original Eclipse source from the author's own files (only `src/` was kept).

## Ear training

The window opens on the **Ear training** tab: six drills on the left, the current round on the right, and a keyboard from C3 to C7 along the bottom.

- **1. Major or minor**: a triad, note by note and then together. Major or Minor.
- **2. Chord qualities**: a triad, note by note and then together. Major, Minor, Diminished or Augmented.
- **3. Five intervals**: two notes, one after the other and then together. Major 2nd, Major 3rd, Perfect 4th, Perfect 5th or Octave.
- **4. All intervals**: two notes, one after the other and then together. Minor 2nd to Octave (12 answers).
- **5. Scales**: a scale from a random key, up to the octave. Major, Natural minor, Harmonic minor or Melodic minor.
- **6. Chords in a key**: the key's home chord (I), then a second chord. I, ii, IV, V or vi.

Roots are picked at random from G3 to F#4 (scales and keys from any of the 12 notes).

- A round is ten questions. Play sounds the first one; pick the answer with the mouse or its key.
- A right answer turns green and the next question plays by itself a second later.
- A wrong answer shows the right one in green and yours in red, then plays the question again with its notes lit on the keyboard, and names the notes (for example `A#3 D4 F4`). Play (now **Next question**) moves on.
- The dots over the buttons show each answer so far; the score and the streak of right answers in a row sit beside them.
- At the end: 10 right is three stars, 8 or 9 two, 6 or 7 one. A star passes the drill and opens the next one (drills not yet open show a lock). A passed round plays a short fanfare, and "New best!" shows when the score beats the drill's best.
- The drill list shows each drill's best stars and score, and the header the total stars (out of 18). The window opens on the first open drill without three stars.

The best score for each drill is saved to `.musictheory-drills.txt` in the user's home folder, one `DRILL=stars,score` line per finished drill (for example `SCALES=2,9`).

## Controls / usage

- **Space** or **Enter**: play the question, play it again, or (after a wrong answer) go to the next question. At the end of a round, start it again.
- **1** to **9**, **0**, **-** and **=**: answers 1 to 12, in the order of the buttons (each button shows its key).
- **Up / Down**: previous / next drill (a locked drill says which drill opens it). **R**: restart the drill. **N**: next drill.
- **Notes** tab, **test** button: plays the original demo, two threads at once through the Java MIDI synthesizer, one second per step:
  - the chords I, IV, V, I of G major (starting at G4, MIDI 67, each in root position) on MIDI channel 0;
  - the notes of the I chord (G, B, D) one at a time, then G5, on MIDI channel 1 (each note name is printed to stdout).
  The demo used to play on start; it now plays only from the button, so it does not talk over the first question.
- The window is 780x600 px and uses the system look and feel. Title "Music Theory Expert".
- No command-line arguments.

## Code layout

- `src/musictheoryexpert/Main.java`: entry point and window; owns the synthesizer, the two tabs and the test button, and passes key presses to the Ear training tab while it is showing.
- `src/musictheoryexpert/EarTrainer.java`: the Ear training tab; draws the drill list, round, answer buttons and keyboard, and handles the mouse and keys.
- `src/musictheoryexpert/Drill.java`: the six drills, their answers and how each makes a question (using `Notes.getScale` and `Notes.getTriadByRomanNumeral`).
- `src/musictheoryexpert/Round.java`: ten questions, score, streak and stars.
- `src/musictheoryexpert/DrillRecords.java`: reads and writes the best score per drill, and which drills are open.
- `src/musictheoryexpert/StepPlayer.java`: plays a question's notes on MIDI channel 2, step by step on a Swing timer.
- `src/musictheoryexpert/Notes.java`: sharp and flat note names, scale step patterns (major, natural, harmonic and melodic minor), scale and triad building, and the demo playback.
- `src/synthesizer/MySynthesizer.java`: opens the MIDI synthesizer and plays timed notes and chords on a channel.
- `src/synthesizer/Notes.java`: constants for note names across octaves (for example `A4`), a name-to-number parser and `NOT_A_NOTE`.
- `src/synthesizer/Instruments.java`: map of MIDI instrument names to program numbers. Not used.

## Running

From the repo root: `./run.ps1 MusicTheoryExpert` (or `./run.ps1 MusicTheoryExpert -Build` to build a jar).

## Self-check

`test/musictheoryexpert/NotesCheck.java` checks note name lookup (sharp and flat), scales and triads. `test/musictheoryexpert/DrillCheck.java` checks that every drill asks each of its answers, that what plays matches the answer (triad shape, interval size, scale steps, chord degree) and stays on the keyboard, the round's score, streak and star rules, and that the saved records keep the best score, skip bad lines and open drills in order. From `MusicTheoryExpert/`:

```
javac -d ../out/check/MusicTheoryExpert src/musictheoryexpert/*.java src/synthesizer/*.java test/musictheoryexpert/*.java
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.NotesCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.DrillCheck
```

## Known quirks

- The `NoteNames` enum, the `transpose` method and the `Instruments` class are not used.
- Clicking **test** while the demo is still playing starts a second copy on the same channels, so the two copies overlap.
