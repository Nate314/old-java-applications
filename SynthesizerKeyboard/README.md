# SynthesizerKeyboard

A one-octave piano (C to C, 8 white and 5 black keys) that plays through the Java MIDI synthesizer, teaches five songs, records tunes of your own to send to a friend, and loops two bars of your playing into a band of up to four layers. Play it with the computer keyboard, by clicking the keys, or by singing, whistling or playing a real instrument to the microphone. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Songs

A strip above the keys lists five songs, each with the best stars won on it: Mary Had a Little Lamb, Twinkle Twinkle Little Star, Ode to Joy, Row, Row, Row Your Boat and Jingle Bells (the chorus). Picking one starts a lesson:

- The key to press next turns gold, and the strip shows the notes coming up (the computer key in large type, the note name under it, wider for longer notes), with the ones already played in green.
- The lesson waits for you: the right key moves on, any other key is counted as a miss (it still sounds).
- After the last note a short fanfare plays and the strip shows the stars: three with no misses, two with up to one miss per ten notes (at least two), one otherwise. "New best!" shows when the run has fewer misses than the best one so far.
- **Listen** plays the song to you at tempo, lighting each key. Press any piano key to stop it and play yourself; the lesson carries on where you were.

The keyboard also knows the songs when you play them yourself. In free play, once the notes you play are the start of one of the five (at least five notes, with at least three different ones), the strip's headline names it ("That's Ode to Joy!") and the song's chip turns gold. It listens by the steps between the notes, folded into the one octave, so a song is found in any key, and a sung or whistled tune counts too. Played in the song's own key, the chip says "play on": picking it (its number, or a click) starts the lesson after the notes already played, with them counted right, the falling notes on the next one and the backing joining in at once. In another key it says "learn it" and the lesson starts from the top. A wrong note, or a song played to its end, puts the strip back to free play. `SongFinder` does the listening.

While a song is picked, falling notes fill the 120 px space over the keys, showing the song: each note a bar over its own key, as tall as it is long (four and a half beats fit above the keys), with the computer key on it when there is room. The next note is gold and sits on the keys, the ones after it are blue, and a right press moves the song down a note, so the played one sinks into the key in green. A wrong key flashes its lane red. During Listen the notes fall at tempo and each one sinks into its key while it sounds; when Listen stops, the song moves back to where the lesson was. Clicks on the falling notes play nothing.

Outside a lesson the same space shows what you play: each note rises out of its key as a bar, growing while the key is held and then floating up at the falling notes' pace, so a tune leaves its shape over the keys for about two seconds. The notes are blue, and turn gold (with a gold line under them) once the keyboard knows the song you are playing. Before the first note the space says "Play a key, or slide across them: your notes rise here" ("Play or slide across the keys" when that does not fit). Chord keys raise a bar for each key a chord lights. With reduced motion on, a note's bar stays on its key for a second and a half and then goes, without moving. In the looper the lanes take this space, and the keys fill the panel.

A backing plays under every lesson: the chords of the song, a low root and the chord an octave below the keys, so even Mary Had a Little Lamb sounds like a song and not a row of single notes. Since the lesson waits for you, so does the band: each chord starts with the right note it goes under and holds until the next one, and Listen plays them at tempo. The chord names ride down with the falling notes, each on a faint line where its chord starts, and a pill at the top right of the falling notes names the chord sounding ("Backing G"). **A** (or a click on the pill) turns the backing off or on; turned on mid-song it joins in at once. While the microphone listens the backing rests ("Backing rests (mic)"), so it does not drown out the notes you sing.

The chords are worked out from the notes, so a friend's tune gets a backing too: the song is cut into stretches of two beats, and each stretch takes the chord that holds most of its notes' beats, from the chords of C major (C, F, G, Am, Dm, Em, the first three preferred on a tie) and the major chords on the black keys, keeping the chord before when it holds as many. Mary gets C, G, C, G, C, and Twinkle's first line C, F, C, F, C, G, C. The backing plays on MIDI channel 5 in the piano's sound.

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

## Looper

Build a little band out of your own playing: the looper records two bars at a time and plays them round and round, and each new layer joins the ones before it in its own sound.

- **O** (or the Looper chip at the end of the song strip) turns the looper on. The strip shows its buttons, each with its key under it, and four lanes open over the keys: a ruler of the two bars of four beats, and a lane per layer, with its notes drawn like a piano roll (higher notes higher). The step playing is lit and notes turn white while they sound. **O** again (or Songs, or Esc) stops the loop and goes back to the songs.
- **Space** (Record, or a click on the empty lane) records a layer. When the loop is stopped, a bar of four clicks counts you in and the take starts on the next downbeat; while it plays, the take starts at the next bar. A take lasts one pass of the loop (two bars, 32 sixteenth-note steps), then joins the loop at once ("Layer 1 is looping"). Space during the count or the take calls it off.
- Notes snap to the nearest sixteenth note and last at least one. A note played up to half a step before the take starts counts as its first note, and one played in the half step after it ends wraps round to its start. A note still held when the pass ends stops with it. A take keeps at most 64 notes.
- Each layer keeps the sound and octave it was recorded with, on its own MIDI channel, so pick the next layer's sound first: **Up / Down** changes the instrument (the sound button and the empty lane name it, such as "Bass 2") and **Left / Right** the octave. The keys keep playing over the loop in the sound you have now.
- **1** to **4** (or a click on a layer's lane) mute or unmute that layer. **Backspace** (Undo) removes the newest layer. **Delete** (Clear), pressed twice, deletes every layer.
- **P** plays or stops the loop. **-** and **=** make it 4 beats per minute slower or faster (60 to 180; it starts at 96) without losing your place. **K** turns the click on the beats off or on (the count-in always clicks).
- Up to four layers. The loop, its tempo, click and mutes are saved to `.synthesizer-loop.txt` in the user's home folder (`tempo=96`, `click=1`, then one `layer=program,muted,step.length.note;...` line per layer), so it is still there next time: "Your loop is back: P plays it".
- The loop runs on a quick Swing timer that reads the clock, so its beat does not drift when a timer runs late. The click is a wood block on the General MIDI drum channel; in the browser it sounds as a short tone, since the web synthesizer has no drums. While the loop's notes sound, the microphone ignores what it hears, as it does for the keys' own notes.

## Chord keys

Turn on chord keys (**Q**, or the Chords button left of Sing along) and each key plays a three-note chord built up from it, so one finger accompanies a song:

- The white keys play the chords of C major: C, Dm, Em, F, G, Am, Bdim and C again on the high C. The black keys play major chords: C#, D#, F#, G#, A#.
- The keys show their chord's name in place of the note name, a held chord lights its three keys (a note above the high C lights the key of the same name below it), and the button names the chord last played. **Q** again turns them off.
- Chords that share notes can overlap: holding C and then pressing Am keeps C and E sounding, and a shared note stops only when the last key holding it is let go.
- In the looper a chord records all three notes, so a layer of chords can back a melody played on top. Lessons and recorded tunes count the key pressed (the chord's lowest note), since a tune keeps one note at a time.

## Sounds

The keys can play any of 121 General MIDI sounds, in sixteen families of eight (Piano, Bells, Organ, Guitar, Bass, Strings, Ensemble, Brass, Reed, Pipe, Synth lead, Synth pad, Synth effects, World, Percussive, and the first of Effects). The sound button, left of Chords at the top of the strip, names the sound the keys play: the family alone for its first sound ("Organ"), then a number for the other seven ("Organ 3"). Its arrows step to the first sound of the previous or next family (a click on the name steps forward, and Effects comes round to Piano), and each pick plays C E G in the new sound, so a first visitor hears the keyboard change its voice with one click. **Up / Down** still step one sound at a time. On a window under 420 px wide the button is a note that steps forward. In the browser the shim's synthesizer gives each family a waveform rather than a recorded instrument, so the sounds within a family sound alike there and families four apart share a waveform.

## Keys

The keys are drawn with depth, so a press looks and feels like one. Each white key has a front edge in a darker shade of its face, and each black key a dark rim and a sloped front below its top; a held key's face moves down over most of that edge, takes a deeper shadow where it dips under the panel's top, and its labels move down with it. Held white keys turn a warm grey and held black keys a lighter grey. Black keys cast a soft shadow down and to the right onto the white keys, which shortens while they are held. The lit next key (gold) and the heard note (blue) get the same edges in their own colors. The shading is drawn in bands, as the browser's shim has no gradient paint.

## Controls / usage

- **Computer keyboard**: hold to sound a note, release to stop. Every key shows its computer key and note name, and goes down while held (see Keys).
  - White keys (C to C): `Z X C V B N M ,`
  - Black keys: `S D G H J`
- **Click a key**: sounds it for as long as the mouse button is held.
- **Slide across the keys** (a glissando): with the mouse button or a finger down, moving onto another key lets the old one go and plays the new one, so a sweep plays every key it crosses and leaves a staircase of rising notes. Moving off the keys keeps the last one sounding until you let go. Every key a slide plays counts like a press: in a lesson the keys crossed on the way to the gold one count as misses, and recordings, the looper and the song finder hear each one.
- **T**: next song. **1** to **5**: that song (**6**: the recorded tune, when there is one). **0**: free play (no song). Clicking a song in the strip also starts it.
- **Space**: start or stop recording a tune. **Enter**: copy the tune's link (or code) again. While recording, the song keys wait; the piano, octave and instrument keys work.
- **L**: listen to the current song (the first one in free play). **R**: restart the current song. **A**: the backing on or off (see Songs).
- **I**: turn the microphone on or off (see Sing along). Clicking the Sing along button does the same.
- **Q**: chord keys on or off (see Chord keys). Clicking the Chords button does the same.
- **O**: the looper on or off (see Looper, for its own keys). While it is on, the song keys wait.
- **Left / Right**: shift the notes down / up one octave (5 down to 4 up). Notes already held keep the pitch they started with and stop when released. Lessons follow the keys, so they work in any octave.
- **Up / Down**: next / previous MIDI instrument (program 0 to 120). The sound button steps a whole family at a time (see Sounds).
- The window is 3/4 of the screen wide and half of it tall (at least 340 px) plus 120 px for the space over the keys (falling notes in a lesson, rising notes outside one); while the looper's lanes are open it is 48 px taller again, as the lanes take that space. The title shows the current sound's name and the octave shift.
- Arguments, all optional: a tune code or link to open (see Send a tune), and the address of the page the keyboard runs on, which the browser build passes so the links it copies point back at it.

## Code layout

- `src/synthesizer/Main.java`: entry point; reads a tune code or link and the page address from the arguments, and creates a `KeyboardWindow`.
- `src/synthesizer/KeyboardWindow.java`: the window; handles key presses, mouse presses on the keys, chord keys, songs, Listen, recording, copying the tune and the fanfare.
- `src/synthesizer/PianoPanel.java`: draws the keys with their labels, the held keys and the lit next key, and above them the lesson's falling notes or the notes played rising, and finds the key under the mouse.
- `src/synthesizer/LessonBar.java`: the strip above the keys (song list with stars, Record and Looper, upcoming notes, the recording, result, the looper's buttons, the sound, Chords and Sing along buttons, and notices such as "Link copied").
- `src/synthesizer/Song.java`: the five songs, written as note names with lengths.
- `src/synthesizer/Backing.java`: the chords under a lesson, worked out from the song's notes, and the notes that play each one.
- `src/synthesizer/Chord.java`: the chord each key plays with chord keys on, the keys it lights and its name.
- `src/synthesizer/Lesson.java`: playing along with one song: next note, misses, stars.
- `src/synthesizer/SongFinder.java`: hears which song free play is the start of, in any key.
- `src/synthesizer/SongRecords.java`: reads and writes the best result per song.
- `src/synthesizer/Tune.java`: the tune code and links, the saved tune, and `Tune.Recorder`, which turns timed key presses into a song.
- `src/synthesizer/Loop.java`: the looper's loop: layers of notes on sixteenth-note steps, mutes, tempo and click, `Loop.Take` (one pass of recording, which snaps notes to steps), and the saved file.
- `src/synthesizer/Looper.java`: plays the loop on a clock-read Swing timer (count-in, click, one MIDI channel per layer) and records takes into it.
- `src/synthesizer/LoopView.java`: the looper's lanes between the strip and the keys: ruler, piano-roll notes, the step playing and the take as it records.
- `src/synthesizer/MidiPlayer.java`: opens the synthesizer; note on/off (on any channel), instrument change and timed notes (stopped by a one-shot Swing timer); knows when it last sounded, so the microphone can ignore it.
- `src/synthesizer/Microphone.java`: reads the microphone (`javax.sound.sampled.TargetDataLine`) on its own thread and tells the window, on the Swing thread, the note it hears and each note that starts.
- `src/synthesizer/PitchDetector.java`: the pitch of a reading (YIN), its loudness, and the nearest MIDI note and cents.
- `src/synthesizer/NoteFollower.java`: turns readings into notes started (held long enough, counted once, counted again after a pause or a new attack).

The keys used to be `JButton`s (an empty `KeyButton` subclass) laid out in a `JLayeredPane`; they are now drawn by `PianoPanel`, so they can carry labels and a lit next key. The old `TickCounter` (a 1 ms tick timer used to time clicked notes) and the unused `NoteNameConverter` were removed earlier.

## Running

From the repo root: `./run.ps1 old SynthesizerKeyboard` (or `./run.ps1 old SynthesizerKeyboard -Build` to build a jar). To open a tune a friend sent: `./run.ps1 old SynthesizerKeyboard e2c2a4` (a code or a whole link).

## Self-check

`test/synthesizer/LessonCheck.java` checks that a held key's face covers its front edge and is shaded at the top, that every song parses to keys on the keyboard with positive lengths and starts each note on the right beat, that the falling notes take the top of the keyboard only during a lesson (where they are not keys) and scroll to the right beat, that a lesson only moves on for the right key and counts misses, the star rules, that the saved records keep the best run and skip bad lines, and that recorded tunes get their lengths from the timing, stop at 64 notes, survive their code, link and saved file, and that broken codes are refused, and that every chord key names its chord and lights three keys of the right pitches on the keyboard, and that every song's backing starts on a chord, never repeats one, never picks Bdim, ends on C (Jingle Bells' chorus on G), gives Mary and Twinkle their usual chords, gives a tune on black keys their major chord, and voices G as a low G under G B D, and that sounds are named by family ("Piano", "Bass 2") and the sound button's arrows sit where clicks find them, wide and narrow, and that free play finds a song from its first five notes in its own key or another (Twinkle folded round the octave), not from one key pressed over and over, loses it on a wrong note, never offers a song played to its end, and that a lesson can carry on after the notes found. `test/synthesizer/MicrophoneCheck.java` needs no microphone: it checks that pitches from a low voice to a whistle (pure and rich in overtones, at 44.1 and 48 kHz) read as the right note within 3 cents, that silence, quiet and noise read as none, that a note counts once however long it is held and again after a pause or a new attack but not for an octave slip, a fading string or a slide through notes, and that a made-up line singing E D C D E E E is heard as those seven notes. `test/synthesizer/LoopCheck.java` checks that a take snaps notes to steps (an early downbeat, a short tap, a note wrapping round from just after the pass, a held note ending with the pass, a take from the second bar ringing on into the first), skips presses outside the take and stops at 64 notes, that the loop keeps at most four layers with mutes, Undo and Clear, the tempo limits, and that the saved file comes back the same and skips bad lines. From `SynthesizerKeyboard/`:

```
javac -d ../out/check/SynthesizerKeyboard src/synthesizer/*.java test/synthesizer/*.java
java -ea -cp ../out/check/SynthesizerKeyboard synthesizer.LessonCheck
java -ea -cp ../out/check/SynthesizerKeyboard synthesizer.MicrophoneCheck
java -ea -cp ../out/check/SynthesizerKeyboard synthesizer.LoopCheck
```
