# SynthesizerKeyboard

A one-octave piano (C to C, 8 white and 5 black keys) that plays through the Java MIDI synthesizer, teaches five songs, and records tunes of your own to send to a friend. Play it with the computer keyboard, by clicking the keys, or by singing, whistling or playing a real instrument to the microphone. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Songs

A strip above the keys lists five songs, each with the best stars won on it: Mary Had a Little Lamb, Twinkle Twinkle Little Star, Ode to Joy, Row, Row, Row Your Boat and Jingle Bells (the chorus). Picking one starts a lesson:

- The key to press next turns gold, and the strip shows the notes coming up (the computer key in large type, the note name under it, wider for longer notes), with the ones already played in green.
- The lesson waits for you: the right key moves on, any other key is counted as a miss (it still sounds).
- After the last note a short fanfare plays and the strip shows the stars: three with no misses, two with up to one miss per ten notes (at least two), one otherwise. "New best!" shows when the run has fewer misses than the best one so far.
- **Listen** plays the song to you at tempo, lighting each key. Press any piano key to stop it and play yourself; the lesson carries on where you were.

The best result for each song is saved to `.synthesizer-songs.txt` in the user's home folder, one `ID=stars,misses` line per finished song (for example `ODE=3,0`).

## Send a tune

Record a tune and send it: your friend hears it, then learns to play it, and can record one back.

- **Space** (or the Record chip at the end of the song strip) starts recording. Play your tune, then press Space again (or click the strip) to stop. The strip shows the notes as they are recorded, up to 64.
- The time from one note to the next sets each note's length, rounded to half beats (half a beat to four beats, a beat being 420 ms, the speed Listen plays at). The last note gets two beats.
- When you stop, the tune becomes song 6, "Your tune", and its link goes to the clipboard ("Link copied: send it to a friend"). **Enter** copies it again. In the browser the link is the keyboard's page with the tune in it (`?tune=e2c2a4`); the desktop app has no page, so it copies the code alone (`e2c2a4`).
- The code has two characters per note: the key by its place on the keyboard (`a` for C, `b` for C#, up to `m` for the high C) and the length in half beats (1 to 8). `e2c2a4` is E D C, the C held for two beats.
- Opening a link (or starting the desktop app with a code or link as its argument, see Running) loads the tune as song 6, "A friend's tune", plays it once, and then teaches it like the other songs: the next key lights, misses are counted and the stars shown. After the last note the strip suggests recording a reply.
- Tunes keep no records. The last tune recorded or received is kept in `.synthesizer-tune.txt` in the user's home folder (`MINE=code` or `FRIEND=code`), so song 6 is still there next time.

## Sing along

Turn on the microphone (**I**, or the Sing along button at the top right of the strip) and the keyboard listens:

- **Tuner**: the button shows the note it hears and how far off it is in cents (`A4 +12¢`, green within 10 cents), and the keys of that note turn blue (both Cs for a C).
- **Lessons**: a sung, whistled or played note counts like a key press: the right note moves the lesson on, any other is a miss. Any octave counts, so a low voice and a high whistle can both sing along. Heard notes do not sound the synthesizer.
- **Recording**: notes sung while recording (Space) are recorded like keys, so you can hum a tune and send it.
- A note counts once it has held its pitch for about a tenth of a second, and only once however long it is held. Singing the same note again counts after a pause, a consonant, or a fresh attack after the sound has faded ("la la la"), so a fading piano or guitar string counts once.
- While the keyboard itself is sounding (a key held, Listen, the fanfare) and for half a second after, the microphone is ignored, so it does not hear the keyboard and count its notes twice.
- If there is no microphone, or the browser blocks it, the button says "No mic" and the strip says why. **I** again turns it off. Closing the window lets it go.

Pitch is found with the YIN method (`PitchDetector`) on readings of 2048 samples taken every 1024 (about 23 ms apart at 44.1 kHz), from a low voice (75 Hz) to a high whistle (2500 Hz). Nothing is recorded or kept.

## Controls / usage

- **Computer keyboard**: hold to sound a note, release to stop. Every key shows its computer key and note name, and turns grey while held.
  - White keys (C to C): `Z X C V B N M ,`
  - Black keys: `S D G H J`
- **Click a key**: sounds it for as long as the mouse button is held.
- **T**: next song. **1** to **5**: that song (**6**: the recorded tune, when there is one). **0**: free play (no song). Clicking a song in the strip also starts it.
- **Space**: start or stop recording a tune. **Enter**: copy the tune's link (or code) again. While recording, the song keys wait; the piano, octave and instrument keys work.
- **L**: listen to the current song (the first one in free play). **R**: restart the current song.
- **I**: turn the microphone on or off (see Sing along). Clicking the Sing along button does the same.
- **Left / Right**: shift the notes down / up one octave (5 down to 4 up). Notes already held keep the pitch they started with and stop when released. Lessons follow the keys, so they work in any octave.
- **Up / Down**: next / previous MIDI instrument (program 0 to 120).
- The window is 3/4 of the screen wide and half of it tall (at least 340 px). The title shows the current instrument and octave shift.
- Arguments, all optional: a tune code or link to open (see Send a tune), and the address of the page the keyboard runs on, which the browser build passes so the links it copies point back at it.

## Code layout

- `src/synthesizer/Main.java`: entry point; reads a tune code or link and the page address from the arguments, and creates a `KeyboardWindow`.
- `src/synthesizer/KeyboardWindow.java`: the window; handles key presses, mouse presses on the keys, songs, Listen, recording, copying the tune and the fanfare.
- `src/synthesizer/PianoPanel.java`: draws the keys with their labels, the held keys and the lit next key, and finds the key under the mouse.
- `src/synthesizer/LessonBar.java`: the strip above the keys (song list with stars and Record, upcoming notes, the recording, result, and notices such as "Link copied").
- `src/synthesizer/Song.java`: the five songs, written as note names with lengths.
- `src/synthesizer/Lesson.java`: playing along with one song: next note, misses, stars.
- `src/synthesizer/SongRecords.java`: reads and writes the best result per song.
- `src/synthesizer/Tune.java`: the tune code and links, the saved tune, and `Tune.Recorder`, which turns timed key presses into a song.
- `src/synthesizer/MidiPlayer.java`: opens the synthesizer; note on/off, instrument change and timed notes (stopped by a one-shot Swing timer); knows when it last sounded, so the microphone can ignore it.
- `src/synthesizer/Microphone.java`: reads the microphone (`javax.sound.sampled.TargetDataLine`) on its own thread and tells the window, on the Swing thread, the note it hears and each note that starts.
- `src/synthesizer/PitchDetector.java`: the pitch of a reading (YIN), its loudness, and the nearest MIDI note and cents.
- `src/synthesizer/NoteFollower.java`: turns readings into notes started (held long enough, counted once, counted again after a pause or a new attack).

The keys used to be `JButton`s (an empty `KeyButton` subclass) laid out in a `JLayeredPane`; they are now drawn by `PianoPanel`, so they can carry labels and a lit next key. The old `TickCounter` (a 1 ms tick timer used to time clicked notes) and the unused `NoteNameConverter` were removed earlier.

## Running

From the repo root: `./run.ps1 SynthesizerKeyboard` (or `./run.ps1 SynthesizerKeyboard -Build` to build a jar). To open a tune a friend sent: `./run.ps1 SynthesizerKeyboard e2c2a4` (a code or a whole link).

## Self-check

`test/synthesizer/LessonCheck.java` checks that every song parses to keys on the keyboard with positive lengths, that a lesson only moves on for the right key and counts misses, the star rules, that the saved records keep the best run and skip bad lines, and that recorded tunes get their lengths from the timing, stop at 64 notes, survive their code, link and saved file, and that broken codes are refused. `test/synthesizer/MicrophoneCheck.java` needs no microphone: it checks that pitches from a low voice to a whistle (pure and rich in overtones, at 44.1 and 48 kHz) read as the right note within 3 cents, that silence, quiet and noise read as none, that a note counts once however long it is held and again after a pause or a new attack but not for an octave slip, a fading string or a slide through notes, and that a made-up line singing E D C D E E E is heard as those seven notes. From `SynthesizerKeyboard/`:

```
javac -d ../out/check/SynthesizerKeyboard src/synthesizer/*.java test/synthesizer/*.java
java -ea -cp ../out/check/SynthesizerKeyboard synthesizer.LessonCheck
java -ea -cp ../out/check/SynthesizerKeyboard synthesizer.MicrophoneCheck
```
