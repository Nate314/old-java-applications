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

**Send song** (N, the blue button in the Songwriter's header) sends the whole song to a friend:

- It copies a link to the clipboard ("Link copied: send it to a friend"). In the browser the link is the app's page with the song in it, for example `?song=1M1451.100.00` for the original demo; the desktop app has no page, so it copies the code alone. A song needs a chord to be sent.
- The code holds everything the song file does: the key's place on the circle (`0` to `b`, C to F) and `M` for major or `m` for minor, the chords as digits, the tempo, the style and voicing, and then the melody, four characters a note (its eighth, its pitch as if in C in two characters, its length), separated by dots.
- Opening a link (or starting the desktop app with a code or a link, see Running) opens the Songwriter on the friend's song and plays it. The header says **From a friend** with two buttons: **Keep it** (K) makes it your song and saves it over the one you had; **Mine** (Y) goes back to your own saved song. Until then the friend's song can be played and changed, and sent on, but nothing is saved, so your own song is safe. In melody mode K and Y play notes, so use the buttons there.
- A code the app did not write (a wrong character, too many chords, a tempo outside 60 to 180, a note off the end of the keyboard) is ignored, and the app opens on your own song.

**Sheet music** (I, or the button under the circle) writes the song out as a lead sheet, on a paper-colored page over the circle and the buttons:

- A treble staff with the key's signature and 4/4, the chord names over their bars, the tempo as a quarter note "= 100", and the key. The title is the famous progression's name while the song is one of them and has no melody, "A friend's song" for a song a link brought, and "Your song" otherwise.
- The melody is written as notes and rests: eighths, quarters, halves and wholes, dotted ones, and tied notes for lengths no single note has and for notes held over a bar line. Eighth notes in the same half bar are beamed together, so beats 1 and 2 and beats 3 and 4 read as groups (a lone eighth keeps its flag), with the stems of a group all one way and the beam sloping with the tune, at most a staff space. Notes that start together share a stem; a note still sounding when the next one starts is written up to it. Sharps, flats and naturals are written where the note differs from the key signature, or from an earlier sign on that line in the same bar. With no melody yet, each bar has a slash a beat, as a rhythm part is written.
- Four bars a line (two while a song of up to four bars has a melody, so its notes have room), the last bar ending in a repeat sign, since the song loops. The music is drawn as large as the page allows, up to 1.3 times.
- While the song plays, its bar is shaded and the chord name and the notes sounding turn gold. Everything still works over the sheet: Space plays, 1 to 7 add chords, R and the keys write a melody, and the sheet follows at once. I, Esc or **Back to the circle** go back.

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
- The keyboard on all three tabs is drawn as a piano: keys at rest have a shadow under the rail and a light front lip (white keys) or a sheen (black keys), and a lit key is drawn pressed down, flat in its color, with its note name on it: the question's notes once answered on Ear training (with sharps), the chord and bass sounding and the melody's notes on the Songwriter (spelled for the key, F# in G major, Bb in F major), and the held notes on the Chord finder (spelled as the chord's name spells them, Ab C Eb). A black key too thin for its name (the Ear training keyboard on the desktop) has it written on the white keys just under it.
- The window is 780x600 px and uses the system look and feel. Title "Music Theory Expert".
- On a screen narrower than 600 px (a phone held upright, in the browser) the window takes the whole screen instead of being shrunk to about half size, and each tab stacks its parts in one column at full size. Ear training: the six drills become a row of numbered chips with their stars over the round, with the drill's name and best score over its question, and the answers sit in rows of three. Songwriter: the circle of fifths in the top corner with Play chords, Voicing and Sheet music beside it, then the chords, your progression, the controls and the famous progressions across the width; the header keeps only its buttons (the circle's middle names the key), and the sheet music sets two bars a line. Chord finder: Add sits at the end of the Your chords heading with the eight chords under it, the best keys get a row of their own, and Build lays the roots out in two rows of six and the types in three rows of five. On all three tabs the keyboard is two rows, C5 to C7 over C3 to B4 with each C over the C two octaves down, so its keys are twice as wide (about 24 px on a 400 px phone instead of 12). A desktop window made narrower than 600 px gets the same layout.
- Arguments, all optional: a song a friend sent (its code, or a whole link with `?song=`), and the address of the page the app runs on, which the browser build passes so the links it copies point back at it.

## Code layout

- `src/musictheoryexpert/Main.java`: entry point and window; reads a friend's song from the arguments, owns the synthesizer and the three tabs, passes key presses to the tab showing, and opens the Songwriter when the Chord finder writes into it.
- `src/musictheoryexpert/EarTrainer.java`: the Ear training tab; draws the drill list, round, answer buttons and keyboard, and handles the mouse and keys.
- `src/musictheoryexpert/Drill.java`: the six drills, their answers and how each makes a question (using `Notes.getScale` and `Notes.getTriadByRomanNumeral`).
- `src/musictheoryexpert/Round.java`: ten questions, score, streak and stars.
- `src/musictheoryexpert/DrillRecords.java`: reads and writes the best score per drill, and which drills are open.
- `src/musictheoryexpert/Songwriter.java`: the Songwriter tab; draws the circle of fifths, chord buttons, progression, controls and keyboard, and handles the mouse and keys; sends the song, and holds a friend's song unsaved until it is kept.
- `src/musictheoryexpert/Progression.java`: the song: key, chords as scale degrees, the melody, chord names and numerals, key signatures, the usual next chords, smooth voicing, the famous progressions, saving, and the song's link code (`code`, `fromCode`).
- `src/musictheoryexpert/LeadSheet.java`: the Songwriter's sheet music: the melody as written notes, rests and ties, the key signature and accidentals, and the drawing.
- `src/musictheoryexpert/ProgressionPlayer.java`: loops the progression, one eighth note per tick of a one-shot Swing timer set by the wall clock, in the three styles, with the melody; plays melody notes by hand and says which eighth a note played now belongs to.
- `src/musictheoryexpert/ChordFinder.java`: the Chord finder tab; draws the name card, staff, keys, your chords, the build buttons and the keyboard, and handles the mouse and keys.
- `src/musictheoryexpert/ChordNamer.java`: names held notes (chord types, inversions, intervals), spells them by letter, finds the keys a chord belongs to and the keys a list of chords fits, and builds a chord from a root and a type.
- `src/musictheoryexpert/Guitar.java`: the guitar in standard tuning: finds the easiest shape for a chord, or the way to finger exact notes, with its fingers and barre.
- `src/musictheoryexpert/Look.java`: the colors, font fitting, keyboard drawing and keyboard hit test the tabs share, and the treble and bass clefs.
- `src/musictheoryexpert/StepPlayer.java`: plays a question's notes on MIDI channel 2, step by step on a Swing timer.
- `src/musictheoryexpert/Notes.java`: sharp and flat note names, scale step patterns (major, natural, harmonic and melodic minor), scale and triad building, and the demo playback.
- `src/synthesizer/MySynthesizer.java`: opens the MIDI synthesizer and plays timed notes and chords on a channel.
- `src/synthesizer/Notes.java`: constants for note names across octaves (for example `A4`), a name-to-number parser and `NOT_A_NOTE`.
- `src/synthesizer/Instruments.java`: map of MIDI instrument names to program numbers. Not used.

## Running

From the repo root: `./run.ps1 old MusicTheoryExpert` (or `./run.ps1 old MusicTheoryExpert -Build` to build a jar). To open a song a friend sent: `./run.ps1 old MusicTheoryExpert 1M1451.100.00` (a code or a whole link).

## Self-check

`test/musictheoryexpert/NotesCheck.java` checks note name lookup (sharp and flat), scales and triads. `test/musictheoryexpert/DrillCheck.java` checks that every drill asks each of its answers, that what plays matches the answer (triad shape, interval size, scale steps, chord degree) and stays on the keyboard, the round's score, streak and star rules, and that the saved records keep the best score, skip bad lines and open drills in order. `test/musictheoryexpert/ChordNamerCheck.java` checks the Chord finder's names (triads, sevenths, sixths, sus, add9, ninths, power chords, no-fifth chords, inversions and slash names, chords with no name), letter spelling, intervals, that every chord type on every root names itself back and builds in range, the keys of a chord (including harmonic minor's), and fitting a list of chords to a key. `test/musictheoryexpert/ProgressionCheck.java` checks the Songwriter's chord names, numerals and qualities in all 24 keys (and that the circle's neighbors hold each key's chords), key signatures, that smooth voicing keeps each chord's notes in range and never moves more than root position would, the presets, the melody (notes replacing one another, lengths capped at a bar, moving with the key and staying put for the relative minor), saving and loading, melody included, and the song's link code: the demo's code, a song with a melody arriving the same, and broken or overlong codes refused. `test/musictheoryexpert/GuitarCheck.java` checks the guitar shapes of the usual open and barre chords, barres and finger counts, that 15 chord types on all 12 roots each get a playable shape that rings just their notes with the root in the bass and names back to the same chord, and placing exact notes (or none, when a hand cannot hold them). `test/musictheoryexpert/KeyboardCheck.java` checks that the keyboard's hit test matches its drawing, in one row and in the two rows of a narrow tab: all 49 keys show and every inner pixel of a key answers that key; and that a lit key's name is written on that key only, or under a black key too thin for it. `test/musictheoryexpert/LeadSheetCheck.java` checks the sheet music: whole rests for empty bars, dotted and tied lengths, ties over the bar line, notes cut where the next starts and chords sharing a stem, that 500 random melodies fill every bar exactly in written lengths without crossing a bar line, spelling on the staff, key signatures, which eighth notes are beamed together (by half bar, never over a rest, a quarter or the middle of the bar), and the sharps, flats and naturals (kept for the bar, one per octave, none on a tied note). From `MusicTheoryExpert/`:

```
javac -d ../out/check/MusicTheoryExpert src/musictheoryexpert/*.java src/synthesizer/*.java test/musictheoryexpert/*.java
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.NotesCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.DrillCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.ProgressionCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.ChordNamerCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.GuitarCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.LeadSheetCheck
java -ea -cp ../out/check/MusicTheoryExpert musictheoryexpert.KeyboardCheck
```

## Known quirks

- The `NoteNames` enum, the `transpose` method and the `Instruments` class are not used.
- The Songwriter spells notes with the sharp or flat names of `Notes`, so the two six-sharp keys write E# as F (F# major's vii° shows as Fdim, and the sheet music writes E# as F with a natural sign).
- The sheet music spaces notes by time, an eighth note to a column.
- Clicking **Original demo** while the demo is still playing starts a second copy on the same channels, so the two copies overlap.
