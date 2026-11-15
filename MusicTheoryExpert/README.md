# MusicTheoryExpert

An ear trainer, a songwriting tool and a chord finder built on an early, unfinished music theory tool. The original code builds major and minor scales and triads as MIDI note numbers and plays a I-IV-V-I chord progression in G major; the Ear training tab now uses those scales and triads to quiz you by ear, the Songwriter tab turns that one progression into any progression in any key, and the Chord finder names whatever notes you hold. Original Eclipse source from the author's own files (only `src/` was kept).

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

## Songwriter

The **Songwriter** tab is a sketchpad for chord progressions. It opens on the progression the original program played, I-IV-V-I in G major.

- **Circle of fifths** (left): the twelve major keys round the outside and their relative minors inside. Click one to change key; the progression keeps its numerals, so it moves into the new key at once, even while it plays. The key's six major and minor chords (I, IV and V outside, vi, ii and iii inside) are the three neighbors of the key in both rings, and they are shaded. The chord sounding lights up gold on the circle.
- **Chords in the key**: the key's seven triads (`Notes.getScale` and `Notes.getTriadByRomanNumeral`, natural minor for minor keys), each with its Roman numeral (upper case major, lower case minor, `°` diminished) and name, spelled with sharps or flats as the key signature is. Click one (or press 1 to 7) to hear it and add it to the progression. Gold rings mark the chords that usually follow the last one (from I: IV, V or vi; from ii: V or vii°; from iii: vi or IV; from IV: V, I or ii; from V: I or vi; from vi: ii or IV; from vii°: I).
- **Your progression**: up to eight chords, one bar of four beats each. Click a chord to select it, then pick a chord to put in its place (Esc keeps it). Backspace or Delete removes the selected chord, or the last one. Clear (C) empties it.
- **Play** (Space or Enter) loops the progression with a bass line (MIDI channel 3 piano, channel 4 acoustic bass); the playing chord is ringed and its beats light up, and the keyboard shows the chord's notes in gold and the bass note in blue. Changes are heard by the next bar. The loop stops when another tab is opened.
- **Tempo**: 60 to 180 beats a minute, in steps of 5 (`-` / `+`, or Down / Up).
- **Play chords** (S): Held (the chord for the whole bar), Beats (the chord on every beat, the bass on the root and on the fifth for beat 3) or Broken (the chord's notes one at a time in eighth notes).
- **Voicing** (V): Smooth (each chord in the inversion and octave, between E3 and C6, that moves its notes least from the chord before; ties go to the one nearer the middle) or Root position (as the original code builds them, the key's home note in the fourth octave).
- **Famous progressions** (click, or P for the next): The original demo (I-IV-V-I), Pop (I-V-vi-IV), Fifties (I-vi-IV-V), Jazz ii-V-I (ii-V-I-I), Pachelbel's Canon (I-V-vi-iii-IV-I-IV-V) and Minor loop (i-VI-III-VII, which switches to the relative minor). Picking one starts it playing.
- Left / Right move the key a fifth round the circle; M switches between the key and its relative minor.

**Melody** (R, the Melody button, or a click on the keyboard) writes a tune over the progression:

- The keyboard becomes a flute (MIDI channel 5). Click or hold its keys, or type on the computer keyboard as in the Chord finder: A is C, W is C#, S is D, up to ; for E an octave up, starting at C4; Z / X move them an octave. Keys not in the song's key are greyed, and while the loop plays the chord's notes are gold, the safest notes to land on.
- While the loop plays, the Melody button shows a red dot: every note played is written into the loop at the eighth note nearest to where it started, as long as it was held (up to a bar), and plays every time round from then on, also with melody mode off. A note on an eighth that already has the same note replaces it.
- In melody mode the famous progressions give way to the melody's piano roll: one column per eighth note across the progression's bars, higher notes higher, the bar playing shaded and its beat in gold. The roll heading counts the notes. Backspace or Delete erases the melody (Clear empties the chords and the melody). R ends melody mode, and the piano letters, S and P go back to their usual jobs.
- The melody is kept as if the song were in C, so a new key on the circle moves it with the chords (the shorter way, at most six half steps), and the relative minor, which has the same notes, keeps it where it is.

The song (key, chords, tempo, style, voicing and melody) is saved to `.musictheory-songwriter.txt` in the user's home folder after every change, as `name=value` lines (for example `key=1`, `minor=false`, `chords=1,4,5,1`, and `melody=0:67:2,4:71:1`, each note its slot (bar times 8 plus the eighth), its pitch as if in C, and its length in eighths).

## Chord finder

The **Chord finder** tab (where the Notes tab and its test button used to be) answers "what chord is this?". It opens holding G major, the home chord of the original demo.

- **Hold notes** by clicking keys on the keyboard (C3 to C7; click again to let go), or by typing on the computer keyboard as on a piano: A is C, W is C#, S is D, E is D#, D is E, F, T, G, Y, H, U, J, then K, O, L, P and ; for C to E an octave up. Typed keys start at C4; Z and X move them an octave down or up. A note sounds when it is pressed. Held notes are blue on the keyboard.
- **The name**: the most usual chord name for the held notes, with the bass after a slash (for example `C/E`), and what it is in words ("C major, first inversion", "G dominant seventh, no fifth"). It knows 23 chord types: major, minor, diminished, augmented, sus2, sus4, power chords, 6, m6, 7, maj7, m7, m(maj7), m7b5, dim7, 7sus4, aug7, add9, m(add9), 9, maj9, m9 and 6/9; a seventh or ninth chord may leave out its fifth. Notes held in more than one octave count once.
- **The notes**, lowest first, each spelled by letter for the chord (Ab major is Ab C Eb, never G# C D#; the root's name with fewer sharps or flats wins), with what each one is in the chord (`R`, `3`, `b3`, `5`, `#5`, `b7`, `bb7`, `9`...). **Also** lists up to three other names for the same notes (C6 is also Am7/C).
- **Two notes** name the interval from the lower one ("Major 3rd: C up to E"); one note gives its name. Notes that make no chord ("No usual name") say what they would be without each note ("without Db it is C").
- **The staff** on the right writes the held notes on a grand staff (treble from middle C up, bass below it), spelled as the name spells them, with sharps and flats, ledger lines, and a second set beside the note below it.
- **In these keys**: every major and minor key the chord belongs to, with its numeral, the home chord first and then V, IV, vi, ii, iii and vii (for example `I in G major`, `V in C major`, `V7 in C minor`, `iiø7 in A minor`). Minor keys use natural minor, plus harmonic minor's V, V7, vii°, vii°7 and III+.
- **Play** (Space) plays the held notes low to high, then together, lighting each one gold. **Clear** (Esc or Delete) lets them all go.
- **Build** a chord: pick a root (C to B) and a type (maj, m, 7, maj7, m7, dim, aug, sus2, sus4, m7b5, dim7, 6, m6, add9, 9); the chord appears on the keyboard in root position, from its root between F3 and E4, and plays. The root and type of the chord held are lit.
- **Your chords**: Add (Enter) puts the chord held into a row of up to eight, for example the chords of a song you know. Click one to hold and hear it again; its corner x, a right-click (a long press on touch screens) or Backspace for the last one takes it off.
- Under them, the three keys that hold the most of those chords (ties go to a key whose home chord is the first or last chord, then to major keys and fewer sharps or flats), each marked with how many it holds, and each chord shows its numeral in the best key. **Clicking a key** writes those chords that are in it into the Songwriter tab, in that key, and plays them. The Songwriter plays a key's natural chords, so in a minor key a major V (from harmonic minor) is played as a minor v there.
- **Original demo** (top right) plays the original I-IV-V-I demo, as the Notes tab's test button did (see below).

**Guitar** (I, or the Guitar button beside Original demo) swaps the keyboard for a guitar neck in standard tuning (E A D G B E), frets 0 to 12, high E on top as a player looking down sees it, and shows how to play the chord held:

- The chord held becomes the easiest shape that plays it: every note of the chord rings, nothing else, on four strings or more, with its lowest note still in the bass. Building a chord, clicking one of Your chords, or switching to the guitar picks the shape. "Easiest" is a score: low on the neck, few fingers (a barre counts as one), no barre, few strings left out (bass strings are the usual ones to leave out, a string skipped in the middle almost never), and open strings only with the first three frets. It finds the open chords (C `x32010`, G `320003`, D `xx0232`, A `x02220`, E `022100`, Am `x02210`, Em `022000`, Dm `xx0231`) and barre chords where no open shape fits (F `133211`, Bm `x24432`).
- Each fingered note is a blue dot named by its note, an open string a ring in the column left of the nut, a string not played an `x`, and the chord's root has a white ring. A barre is drawn as one bar across the strings it holds. The shape is written under the strings the way guitarists write it (`x32010`, frets past 9 in brackets).
- Small blue dots show where else on the neck the chord's notes lie. Clicking a fret holds it on that string (one note per string, as on a guitar); clicking the held fret again, or the open string twice, lets the string go. The name, notes, staff and keys follow at once, so the neck works as a chord finder for guitar too.
- Typed piano keys still hold exact notes; the neck shows how to finger them, or, when no hand can hold them all at once, says so and marks where each one is.
- The staff writes the notes an octave up, as guitar music is written. Back on the piano (I again), a chord reaching below C3 moves up an octave so it shows on the keyboard.
- It still sounds through the same piano voice.

Nothing on this tab is saved.

## Controls / usage

On the Ear training tab:

- **Space** or **Enter**: play the question, play it again, or (after a wrong answer) go to the next question. At the end of a round, start it again.
- **1** to **9**, **0**, **-** and **=**: answers 1 to 12, in the order of the buttons (each button shows its key).
- **Up / Down**: previous / next drill (a locked drill says which drill opens it). **R**: restart the drill. **N**: next drill.
- **Chord finder** tab, **Original demo** button (once the Notes tab's **test** button): plays the original demo, two threads at once through the Java MIDI synthesizer, one second per step:
  - the chords I, IV, V, I of G major (starting at G4, MIDI 67, each in root position) on MIDI channel 0;
  - the notes of the I chord (G, B, D) one at a time, then G5, on MIDI channel 1 (each note name is printed to stdout).
  The demo used to play on start; it now plays only from the button, so it does not talk over the first question.
- On the Chord finder tab: piano keys (A W S E D F T G Y H U J K O L P ;) hold or let go of notes, Z / X move them an octave, Space plays, Enter adds the chord to Your chords, Backspace takes the last one off, Esc or Delete lets go of every note, I switches between the keyboard and the guitar neck.
- The window is 780x600 px and uses the system look and feel. Title "Music Theory Expert".
- No command-line arguments.

## Code layout

- `src/musictheoryexpert/Main.java`: entry point and window; owns the synthesizer and the three tabs, passes key presses to the tab showing, and opens the Songwriter when the Chord finder writes into it.
- `src/musictheoryexpert/EarTrainer.java`: the Ear training tab; draws the drill list, round, answer buttons and keyboard, and handles the mouse and keys.
- `src/musictheoryexpert/Drill.java`: the six drills, their answers and how each makes a question (using `Notes.getScale` and `Notes.getTriadByRomanNumeral`).
- `src/musictheoryexpert/Round.java`: ten questions, score, streak and stars.
- `src/musictheoryexpert/DrillRecords.java`: reads and writes the best score per drill, and which drills are open.
- `src/musictheoryexpert/Songwriter.java`: the Songwriter tab; draws the circle of fifths, chord buttons, progression, controls and keyboard, and handles the mouse and keys.
- `src/musictheoryexpert/Progression.java`: the song: key, chords as scale degrees, the melody, chord names and numerals, key signatures, the usual next chords, smooth voicing, the famous progressions, and saving.
- `src/musictheoryexpert/ProgressionPlayer.java`: loops the progression, one eighth note per tick of a one-shot Swing timer set by the wall clock, in the three styles, with the melody; plays melody notes by hand and says which eighth a note played now belongs to.
- `src/musictheoryexpert/ChordFinder.java`: the Chord finder tab; draws the name card, staff, keys, your chords, the build buttons and the keyboard, and handles the mouse and keys.
- `src/musictheoryexpert/ChordNamer.java`: names held notes (chord types, inversions, intervals), spells them by letter, finds the keys a chord belongs to and the keys a list of chords fits, and builds a chord from a root and a type.
- `src/musictheoryexpert/Guitar.java`: the guitar in standard tuning: finds the easiest shape for a chord, or the way to finger exact notes, with its fingers and barre.
- `src/musictheoryexpert/Look.java`: the colors, font fitting, keyboard drawing and keyboard hit test the tabs share.
- `src/musictheoryexpert/StepPlayer.java`: plays a question's notes on MIDI channel 2, step by step on a Swing timer.
- `src/musictheoryexpert/Notes.java`: sharp and flat note names, scale step patterns (major, natural, harmonic and melodic minor), scale and triad building, and the demo playback.
- `src/synthesizer/MySynthesizer.java`: opens the MIDI synthesizer and plays timed notes and chords on a channel.
- `src/synthesizer/Notes.java`: constants for note names across octaves (for example `A4`), a name-to-number parser and `NOT_A_NOTE`.
- `src/synthesizer/Instruments.java`: map of MIDI instrument names to program numbers. Not used.

## Running

From the repo root: `./run.ps1 old MusicTheoryExpert` (or `./run.ps1 old MusicTheoryExpert -Build` to build a jar).

## Self-check

`test/musictheoryexpert/NotesCheck.java` checks note name lookup (sharp and flat), scales and triads. `test/musictheoryexpert/DrillCheck.java` checks that every drill asks each of its answers, that what plays matches the answer (triad shape, interval size, scale steps, chord degree) and stays on the keyboard, the round's score, streak and star rules, and that the saved records keep the best score, skip bad lines and open drills in order. `test/musictheoryexpert/ChordNamerCheck.java` checks the Chord finder's names (triads, sevenths, sixths, sus, add9, ninths, power chords, no-fifth chords, inversions and slash names, chords with no name), letter spelling, intervals, that every chord type on every root names itself back and builds in range, the keys of a chord (including harmonic minor's), and fitting a list of chords to a key. `test/musictheoryexpert/ProgressionCheck.java` checks the Songwriter's chord names, numerals and qualities in all 24 keys (and that the circle's neighbors hold each key's chords), key signatures, that smooth voicing keeps each chord's notes in range and never moves more than root position would, the presets, the melody (notes replacing one another, lengths capped at a bar, moving with the key and staying put for the relative minor), and saving and loading, melody included. `test/musictheoryexpert/GuitarCheck.java` checks the guitar shapes of the usual open and barre chords, barres and finger counts, that 15 chord types on all 12 roots each get a playable shape that rings just their notes with the root in the bass and names back to the same chord, and placing exact notes (or none, when a hand cannot hold them). From `MusicTheoryExpert/`:

```
javac -d ../out/check/MusicTheoryExpert src/musictheoryexpert/*.java src/synthesizer/*.java test/musictheoryexpert/*.java
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.NotesCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.DrillCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.ProgressionCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.ChordNamerCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.GuitarCheck
```

## Known quirks

- The `NoteNames` enum, the `transpose` method and the `Instruments` class are not used.
- The Songwriter spells notes with the sharp or flat names of `Notes`, so the two six-sharp keys write E# as F (F# major's vii° shows as Fdim).
- Clicking **Original demo** while the demo is still playing starts a second copy on the same channels, so the two copies overlap.
