# SynthesizerKeyboard

A one-octave piano (C to C, 8 white and 5 black keys) that plays through the Java MIDI synthesizer and teaches five songs. Play it with the computer keyboard or by clicking the keys. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Songs

A strip above the keys lists five songs, each with the best stars won on it: Mary Had a Little Lamb, Twinkle Twinkle Little Star, Ode to Joy, Row, Row, Row Your Boat and Jingle Bells (the chorus). Picking one starts a lesson:

- The key to press next turns gold, and the strip shows the notes coming up (the computer key in large type, the note name under it, wider for longer notes), with the ones already played in green.
- The lesson waits for you: the right key moves on, any other key is counted as a miss (it still sounds).
- After the last note a short fanfare plays and the strip shows the stars: three with no misses, two with up to one miss per ten notes (at least two), one otherwise. "New best!" shows when the run has fewer misses than the best one so far.
- **Listen** plays the song to you at tempo, lighting each key. Press any piano key to stop it and play yourself; the lesson carries on where you were.

The best result for each song is saved to `.synthesizer-songs.txt` in the user's home folder, one `ID=stars,misses` line per finished song (for example `ODE=3,0`).

## Controls / usage

- **Computer keyboard**: hold to sound a note, release to stop. Every key shows its computer key and note name, and turns grey while held.
  - White keys (C to C): `Z X C V B N M ,`
  - Black keys: `S D G H J`
- **Click a key**: sounds it for as long as the mouse button is held.
- **T**: next song. **1** to **5**: that song. **0**: free play (no song). Clicking a song in the strip also starts it.
- **L**: listen to the current song (the first one in free play). **R**: restart the current song.
- **Left / Right**: shift the notes down / up one octave (5 down to 4 up). Notes already held keep the pitch they started with and stop when released. Lessons follow the keys, so they work in any octave.
- **Up / Down**: next / previous MIDI instrument (program 0 to 120).
- The window is 3/4 of the screen wide and half of it tall (at least 340 px). The title shows the current instrument and octave shift.
- No command-line arguments.

## Code layout

- `src/synthesizer/Main.java`: entry point; creates a `KeyboardWindow`.
- `src/synthesizer/KeyboardWindow.java`: the window; handles key presses, mouse presses on the keys, songs, Listen and the fanfare.
- `src/synthesizer/PianoPanel.java`: draws the keys with their labels, the held keys and the lit next key, and finds the key under the mouse.
- `src/synthesizer/LessonBar.java`: the strip above the keys (song list with stars, upcoming notes, result).
- `src/synthesizer/Song.java`: the five songs, written as note names with lengths.
- `src/synthesizer/Lesson.java`: playing along with one song: next note, misses, stars.
- `src/synthesizer/SongRecords.java`: reads and writes the best result per song.
- `src/synthesizer/MidiPlayer.java`: opens the synthesizer; note on/off, instrument change and timed notes (stopped by a one-shot Swing timer).

The keys used to be `JButton`s (an empty `KeyButton` subclass) laid out in a `JLayeredPane`; they are now drawn by `PianoPanel`, so they can carry labels and a lit next key. The old `TickCounter` (a 1 ms tick timer used to time clicked notes) and the unused `NoteNameConverter` were removed earlier.

## Running

From the repo root: `./run.ps1 SynthesizerKeyboard` (or `./run.ps1 SynthesizerKeyboard -Build` to build a jar).

## Self-check

`test/synthesizer/LessonCheck.java` checks that every song parses to keys on the keyboard with positive lengths, that a lesson only moves on for the right key and counts misses, the star rules, and that the saved records keep the best run and skip bad lines. From `SynthesizerKeyboard/`:

```
javac -d ../out/check/SynthesizerKeyboard src/synthesizer/*.java test/synthesizer/*.java
java -ea -cp ../out/check/SynthesizerKeyboard synthesizer.LessonCheck
```
