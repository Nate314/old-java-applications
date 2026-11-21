package synthesizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/** Assert-based self-check of the songs, the lesson, the saved records, recorded tunes and chord keys. Run with {@code java -ea}. */
public final class LessonCheck {
   public static void main(String[] args) throws IOException {
      boolean enabled = false;
      assert enabled = true;
      if (!enabled) {
         throw new IllegalStateException("Run with -ea");
      }
      checkSongs();
      checkFall();
      checkLesson();
      checkRecords();
      checkTunes();
      checkChords();
      checkBacking();
      checkSoundButton();
      System.out.println("LessonCheck: all checks passed");
   }

   private static void checkBacking() {
      for (Song song : Song.ALL) {
         int[] chords = Backing.chords(song);
         assert chords.length == song.length() && chords[0] >= 0 : song.id() + " starts on a chord";
         for (int note = 0; note < song.length(); note++) {
            int chord = Backing.at(chords, note);
            assert chord >= 0 && chord < 12 && chord != 11 : song.id() + " note " + note + " has chord " + chord;
            assert note == 0 || chords[note] != Backing.at(chords, note - 1) : song.id() + " starts the chord it has already";
         }
         assert Backing.at(chords, song.length() - 1) == (song.id().equals("JINGLE") ? 7 : 0) : song.id() + " ends on C (Jingle Bells on G)";
      }
      // Mary: C under E D C D, G under D D D, C under E G G, and G under D D E D before the last C.
      int[] mary = Backing.chords(Song.ALL[0]);
      assert mary[0] == 0 && mary[7] == 7 && mary[10] == 0 && mary[21] == 7 && mary[25] == 0;
      assert Backing.at(mary, 23) == 7 : "the E in D D E D keeps the G";
      // Twinkle: C C G G | F F C | F F C C | G G C, its first line's chords.
      int[] twinkle = Backing.chords(Song.ALL[1]);
      String line = "";
      for (int note = 0; note < 14; note++) {
         line += twinkle[note] < 0 ? "" : Chord.name(twinkle[note]) + " ";
      }
      assert line.equals("C F C F C G C ") : line;
      // A friend's tune on black keys gets their major chords; Bdim is never picked.
      Song sharp = Tune.decode("g2k2b2g2", Tune.FRIENDS);
      assert Backing.chords(sharp)[0] == 6 : "F# A# C# F# is F#, got " + Backing.chords(sharp)[0];
      int[] notes = Backing.notes(7, 60);
      assert java.util.Arrays.equals(notes, new int[]{43, 55, 59, 62}) : "G: a low G, then G B D an octave below the keys";
   }

   private static void checkSoundButton() {
      assert LoopView.sound(0).equals("Piano") && LoopView.sound(33).equals("Bass 2") && LoopView.sound(120).equals("Effects")
         : "sounds read as their family, numbered after the first";
      LessonBar bar = new LessonBar(SongRecords.load(new File("no-such-records.txt")));
      bar.setSize(900, LessonBar.HEIGHT);
      // Wide: the sound button sits left of Chords (100) and Sing along (132), 124 wide, its left arrow on its first 26 px.
      int left = 900 - 10 - 132 - 6 - 100 - 6 - 124;
      assert bar.soundAt(left + 5, 20) == -1 && bar.soundAt(left + 60, 20) == 1 && bar.soundAt(left + 120, 20) == 1;
      assert bar.soundAt(left - 2, 20) == 0 && bar.soundAt(left + 126, 20) == 0 && bar.soundAt(left + 60, 40) == 0 : "off the button";
      assert !bar.chordsAt(left + 120, 20) && bar.chordsAt(left + 130, 20) : "the sound button ends where Chords begins";
      // Narrow: a 34 px note that steps forward.
      bar.setSize(400, LessonBar.HEIGHT);
      int narrow = 400 - 10 - 34 - 6 - 34 - 6 - 34;
      assert bar.soundAt(narrow + 2, 20) == 1 && bar.soundAt(narrow + 33, 20) == 1 && bar.soundAt(narrow + 35, 20) == 0;
      // A headline too long for the room left of the buttons is cut short to fit.
      java.awt.FontMetrics metrics = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB)
         .getGraphics().getFontMetrics(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
      String cut = LessonBar.fit(metrics, "Listening: Twinkle Twinkle Little Star", 150);
      assert cut.endsWith("..") && metrics.stringWidth(cut) <= 150 : cut;
      assert LessonBar.fit(metrics, "Free play", 150).equals("Free play");
   }

   private static void checkChords() {
      String[] names = {"C", "C#", "Dm", "D#", "Em", "F", "F#", "G", "G#", "Am", "A#", "Bdim", "C"};
      for (int key = 0; key < PianoPanel.KEYS; key++) {
         assert Chord.name(key).equals(names[key]) : key + " is " + Chord.name(key);
         int[] keys = Chord.keys(key);
         assert keys.length == 3 && keys[0] == key : "a chord starts on its own key";
         for (int tone = 0; tone < 3; tone++) {
            assert keys[tone] >= 0 && keys[tone] < PianoPanel.KEYS : key + " lights " + keys[tone];
            assert Math.floorMod(keys[tone] - key, 12) == Chord.intervals(key)[tone] : "key " + key + " tone " + tone + " has the chord's pitch";
         }
      }
      assert java.util.Arrays.equals(Chord.keys(12), new int[]{12, 4, 7}) : "the high C's E and G light below it";
      assert java.util.Arrays.equals(Chord.keys(9), new int[]{9, 12, 4}) : "Am: A, the high C, and E below";
   }

   private static void checkSongs() {
      assert Song.ALL.length == 5;
      for (Song song : Song.ALL) {
         assert song.length() >= 20 : song.id();
         for (int note = 0; note < song.length(); note++) {
            assert song.key(note) >= 0 && song.key(note) < PianoPanel.KEYS : song.id() + " note " + note;
            assert song.beats(note) > 0 : song.id() + " note " + note;
         }
         assert song.key(song.length() - 1) == 0 || song.id().equals("JINGLE") : song.id() + " should end on C";
      }
      Song mary = Song.ALL[0];
      assert mary.key(0) == 4 && mary.key(1) == 2 && mary.key(2) == 0 : "Mary starts E D C";
      assert mary.beats(6) == 2 && mary.beats(mary.length() - 1) == 4;
      Song row = Song.ALL[3];
      assert row.beats(0) == 1.5 && row.beats(3) == 0.5 && row.key(10) == 12 : "Row Row: dotted, half beats and the high C";
      try {
         new Song("BAD", "Bad", "Bad", "C H");
         assert false : "unknown note accepted";
      } catch (IllegalArgumentException expected) {
         // as it should be
      }
      assert Song.noteName(12).equals("C") && Song.noteName(6).equals("F#");
      assert mary.start(0) == 0 && mary.start(7) == 8 && mary.start(mary.length()) == 32 : "Mary's notes start on their beats";
      assert row.start(1) == 1.5 && row.start(4) == 4.5 : "Row Row: dotted and half beats";
   }

   /** The falling notes take the top of the piano during a lesson, and the keys stay where they are drawn. */
   private static void checkFall() {
      PianoPanel piano = new PianoPanel();
      piano.setSize(802, 300);
      assert piano.keyAt(50, 10) == 0 && piano.keyAt(50, 299) == 0 : "no lesson: the keys fill the panel";
      piano.setSong(Song.ALL[0]);
      assert piano.keyAt(50, 10) == -1 && piano.keyAt(50, 119) == -1 : "the falling notes are not keys";
      assert piano.keyAt(50, 120) == 0 && piano.keyAt(50, 299) == 0 && piano.keyAt(100, 130) == 1 : "the keys moved below them";
      piano.follow(7, 0);
      assert piano.shownBeat() == 8 : "follow moves the song to the note's beat";
      piano.play(2, 0);
      assert piano.shownBeat() == 3 : "Listen moves on to the end of the note it plays";
      piano.setSong(null);
      assert piano.keyAt(50, 10) == 0 : "free play gives the keys their room back";
      // A key's front edge shows below its face, and a press pushes the face down over most of it.
      int edge = 300 - 2 - PianoPanel.lip(300) / 2;
      int upFront = brightness(paint(piano), 10, edge);
      piano.setPressed(0, true);
      java.awt.image.BufferedImage down = paint(piano);
      assert brightness(down, 10, edge) > upFront + 40 : "a pressed key's face covers its front edge";
      assert brightness(down, 10, 2) < brightness(down, 10, 40) : "a pressed key is shaded where it dips under the panel";
      piano.setPressed(0, false);
   }

   private static java.awt.image.BufferedImage paint(PianoPanel piano) {
      java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(piano.getWidth(), piano.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
      piano.paint(image.getGraphics());
      return image;
   }

   private static int brightness(java.awt.image.BufferedImage image, int x, int y) {
      int rgb = image.getRGB(x, y);
      return ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255);
   }

   private static void checkLesson() {
      Song mary = Song.ALL[0];
      Lesson lesson = new Lesson(mary);
      assert lesson.expectedKey() == 4 && !lesson.finished();
      assert !lesson.press(0) && lesson.misses() == 1 && lesson.position() == 0 : "wrong key is a miss and does not advance";
      assert lesson.press(4) && lesson.position() == 1;
      for (int note = 1; note < mary.length(); note++) {
         assert lesson.press(mary.key(note));
      }
      assert lesson.finished() && lesson.expectedKey() == -1;
      assert !lesson.press(4) && lesson.misses() == 1 : "presses after the end are ignored";
      assert lesson.stars() == 2;

      assert perfect(mary).stars() == 3;
      Lesson sloppy = new Lesson(mary);
      for (int miss = 0; miss < 3; miss++) {
         sloppy.press(11);
      }
      assert sloppy.stars() == 1 : "three misses in 26 notes is one star";
   }

   private static Lesson perfect(Song song) {
      Lesson lesson = new Lesson(song);
      for (int note = 0; note < song.length(); note++) {
         lesson.press(song.key(note));
      }
      return lesson;
   }

   private static void checkRecords() throws IOException {
      File file = File.createTempFile("synthesizer-songs", ".txt");
      file.delete();
      SongRecords records = SongRecords.load(file);
      Song ode = Song.ALL[2];
      assert records.stars(ode) == 0 : "never finished";

      Lesson twoMisses = new Lesson(ode);
      twoMisses.press(11);
      twoMisses.press(11);
      for (int note = 0; note < ode.length(); note++) {
         twoMisses.press(ode.key(note));
      }
      assert records.add(twoMisses) && records.stars(ode) == 2 : "first finish is a new best";
      assert !records.add(twoMisses) : "same misses is not a new best";
      assert records.add(perfect(ode)) && records.stars(ode) == 3;
      assert !records.add(twoMisses) && records.stars(ode) == 3 : "a worse run keeps the best";

      SongRecords reloaded = SongRecords.load(file);
      assert reloaded.stars(ode) == 3 && reloaded.stars(Song.ALL[0]) == 0 : "records survive a reload";

      try (Writer writer = new FileWriter(file)) {
         writer.write("MARY=2,1\nnonsense\nODE=x,y\nTWINKLE=9,0\n");
      }
      SongRecords odd = SongRecords.load(file);
      assert odd.stars(Song.ALL[0]) == 2 && odd.stars(ode) == 0 && odd.stars(Song.ALL[1]) == 3 : "bad lines skipped, stars clamped";
      file.delete();
   }

   private static void checkTunes() throws IOException {
      Tune.Recorder recorder = new Tune.Recorder();
      assert recorder.song(Tune.MINE) == null : "nothing recorded yet";
      // E for a beat, D for half a beat, C for two beats (840 ms), then G: 420 ms is a beat.
      long[] times = {1000, 1420, 1630, 2470};
      int[] keys = {4, 2, 0, 7};
      for (int note = 0; note < keys.length; note++) {
         assert recorder.press(keys[note], times[note]);
      }
      Song tune = recorder.song(Tune.MINE);
      assert tune.length() == 4 && tune.id() == null && tune.title().equals(Tune.MINE);
      assert tune.beats(0) == 1 && tune.beats(1) == 0.5 && tune.beats(2) == 2 && tune.beats(3) == 2 : "lengths from the gaps, last note two beats";
      assert Tune.encode(tune).equals("e2c1a4h4");

      Tune.Recorder extremes = new Tune.Recorder();
      extremes.press(0, 0);
      extremes.press(12, 10);
      extremes.press(1, 60000);
      Song clamped = extremes.song(Tune.MINE);
      assert clamped.beats(0) == 0.5 && clamped.beats(1) == 4 : "lengths kept between half a beat and four beats";

      Tune.Recorder full = new Tune.Recorder();
      for (int note = 0; note < Tune.MAX_NOTES; note++) {
         assert full.press(note % PianoPanel.KEYS, note * 300L);
      }
      assert full.full() && !full.press(0, 1000000) && full.length() == Tune.MAX_NOTES : "a tune stops at the most notes a link carries";

      Song back = Tune.decode("e2c1a4h4", Tune.FRIENDS);
      assert back.length() == 4 && back.key(3) == 7 && back.beats(1) == 0.5 && back.title().equals(Tune.FRIENDS);
      assert Tune.encode(back).equals("e2c1a4h4") : "a code survives decoding";
      for (String bad : new String[]{"", "e2", "e2d", "n2a2", "e0a2", "e9a2", "E2A2", "e2a2".repeat(Tune.MAX_NOTES)}) {
         assert Tune.decode(bad, Tune.FRIENDS) == null : "not a tune: " + bad;
      }

      String page = "https://example.com/SynthesizerKeyboard/index.html";
      assert Tune.pageOf(page + "?tune=e2a2#x").equals(page) && Tune.pageOf("e2a2") == null;
      assert Tune.codeIn(page + "?embed&tune=E2a2&x=1").equals("e2a2") && Tune.codeIn(" e2 a2 ").equals("e2a2");
      assert Tune.message(page, back).equals(page + "?tune=e2c1a4h4") && Tune.message(null, back).equals("e2c1a4h4");

      File file = File.createTempFile("synthesizer-tune", ".txt");
      file.delete();
      assert Tune.load(file) == null : "no tune yet";
      Tune.save(file, back);
      Song friends = Tune.load(file);
      assert friends.title().equals(Tune.FRIENDS) && Tune.encode(friends).equals("e2c1a4h4") : "a friend's tune survives a reload";
      Tune.save(file, tune);
      assert Tune.load(file).title().equals(Tune.MINE);
      try (Writer writer = new FileWriter(file)) {
         writer.write("MINE=zz\n");
      }
      assert Tune.load(file) == null : "a broken tune is skipped";
      file.delete();
   }
}
