package synthesizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/** Assert-based self-check of the songs, the lesson and the saved records. Run with {@code java -ea}. */
public final class LessonCheck {
   public static void main(String[] args) throws IOException {
      boolean enabled = false;
      assert enabled = true;
      if (!enabled) {
         throw new IllegalStateException("Run with -ea");
      }
      checkSongs();
      checkLesson();
      checkRecords();
      System.out.println("LessonCheck: all checks passed");
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
}
