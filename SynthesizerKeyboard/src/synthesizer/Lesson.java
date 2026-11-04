package synthesizer;

/**
 * Playing one song along with the keyboard: the lesson waits for the next note's key, counts every other key as a
 * miss, and is finished after the last note. Stars: three with no misses, two with up to one miss per ten notes
 * (at least two), one otherwise.
 */
public final class Lesson {
   private final Song song;
   private int position;
   private int misses;

   public Lesson(Song song) {
      this.song = song;
   }

   public Song song() {
      return this.song;
   }

   /** How many notes have been played right so far, which is also the index of the next note. */
   public int position() {
      return this.position;
   }

   public int misses() {
      return this.misses;
   }

   public boolean finished() {
      return this.position >= this.song.length();
   }

   /** The key index (0 to 12) to press next, or -1 once the song is finished. */
   public int expectedKey() {
      return this.finished() ? -1 : this.song.key(this.position);
   }

   /** Counts a press of key index {@code key}; returns true when it was the next note. Ignored once finished. */
   public boolean press(int key) {
      if (this.finished()) {
         return false;
      }
      if (key == this.expectedKey()) {
         this.position++;
         return true;
      }
      this.misses++;
      return false;
   }

   public int stars() {
      if (this.misses == 0) {
         return 3;
      }
      return this.misses <= Math.max(2, this.song.length() / 10) ? 2 : 1;
   }
}
