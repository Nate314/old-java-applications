package synthesizer;

/**
 * Hears which song free play is the start of. The last notes played are compared with the start of each song by the
 * steps between them, folded into one octave (the keyboard has only one), so a song is found in any key. A song
 * counts once at least five of its first notes are played, holding at least three different notes (so a key pressed
 * over and over is not Jingle Bells), and while at least one of its notes is still to come.
 */
public final class SongFinder {
   static final int MIN_NOTES = 5;
   private static final int MIN_PITCHES = 3;
   private final Song[] songs;
   private final int[] played = new int[64];
   private int count;
   private Song song;
   private int notes;
   private boolean inKey;

   public SongFinder(Song[] songs) {
      this.songs = songs;
   }

   /** Counts key {@code key} (0 to 12) played, and finds the song the notes so far end in the start of. */
   public void add(int key) {
      if (this.count == this.played.length) {
         System.arraycopy(this.played, 1, this.played, 0, this.count - 1);
         this.count--;
      }
      this.played[this.count++] = key;
      this.song = null;
      this.notes = 0;
      for (Song candidate : this.songs) {
         // Longest match wins; on a tie the earlier song.
         for (int k = Math.min(this.count, candidate.length() - 1); k >= MIN_NOTES && k > this.notes; k--) {
            if (this.startsWith(candidate, k)) {
               this.song = candidate;
               this.notes = k;
               this.inKey = Math.floorMod(this.played[this.count - k] - candidate.key(0), 12) == 0;
               break;
            }
         }
      }
   }

   public void clear() {
      this.count = 0;
      this.song = null;
      this.notes = 0;
   }

   /** The song found, or null. */
   public Song song() {
      return this.song;
   }

   /** How many of its first notes were played. */
   public int notes() {
      return this.notes;
   }

   /** Whether they were played in the song's own key, so a lesson can carry on from them. */
   public boolean inKey() {
      return this.song != null && this.inKey;
   }

   /** Whether the last {@code k} notes played are song's first {@code k}, in some key, with enough different notes. */
   private boolean startsWith(Song song, int k) {
      int first = this.count - k;
      int shift = this.played[first] - song.key(0);
      int pitches = 0;
      for (int i = 0; i < k; i++) {
         if (Math.floorMod(this.played[first + i] - song.key(i) - shift, 12) != 0) {
            return false;
         }
         pitches |= 1 << Math.floorMod(song.key(i), 12);
      }
      return Integer.bitCount(pitches) >= MIN_PITCHES;
   }
}
