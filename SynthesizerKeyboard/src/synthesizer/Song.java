package synthesizer;

/**
 * A tune that fits the one-octave keyboard: each note is a key index from 0 (C) to 12 (the high C) and a length in
 * beats. Songs are written as space-separated notes such as {@code E D C D E E E2}: a note name (C, C#, D, D#, E, F,
 * F#, G, G#, A, A#, B, or C' for the high C) and an optional length, none for one beat, / for half a beat, . for
 * one and a half, or a number of beats.
 */
public final class Song {
   private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B", "C'"};

   /** Easiest first. All five are traditional tunes or, for Ode to Joy, Beethoven's. */
   public static final Song[] ALL = {
      new Song("MARY", "Mary Had a Little Lamb", "Mary's Lamb",
         "E D C D E E E2 D D D2 E G G2 E D C D E E E E D D E D C4"),
      new Song("TWINKLE", "Twinkle Twinkle Little Star", "Twinkle",
         "C C G G A A G2 F F E E D D C2 G G F F E E D2 G G F F E E D2 C C G G A A G2 F F E E D D C2"),
      new Song("ODE", "Ode to Joy", "Ode to Joy",
         "E E F G G F E D C C D E E. D/ D2 E E F G G F E D C C D E D. C/ C2"),
      new Song("ROW", "Row, Row, Row Your Boat", "Row Your Boat",
         "C. C. C D/ E. E D/ E F/ G3 C'/ C'/ C'/ G/ G/ G/ E/ E/ E/ C/ C/ C/ G F/ E D/ C3"),
      new Song("JINGLE", "Jingle Bells", "Jingle Bells",
         "E E E2 E E E2 E G C. D/ E4 F F F. F/ F E E E/ E/ E D D E D2 G2")
   };

   private final String id;
   private final String title;
   private final String shortTitle;
   private final int[] keys;
   private final double[] beats;

   Song(String id, String title, String shortTitle, String notes) {
      this.id = id;
      this.title = title;
      this.shortTitle = shortTitle;
      String[] tokens = notes.trim().split("\\s+");
      this.keys = new int[tokens.length];
      this.beats = new double[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
         this.parse(i, tokens[i]);
      }
   }

   private void parse(int i, String token) {
      int nameLength = token.length() > 1 && (token.charAt(1) == '#' || token.charAt(1) == '\'') ? 2 : 1;
      String name = token.substring(0, nameLength);
      String length = token.substring(nameLength);
      this.keys[i] = -1;
      for (int key = 0; key < NOTE_NAMES.length; key++) {
         if (NOTE_NAMES[key].equals(name)) {
            this.keys[i] = key;
         }
      }
      if (this.keys[i] < 0) {
         throw new IllegalArgumentException("Unknown note " + token + " in " + this.id);
      }
      this.beats[i] = length.isEmpty() ? 1 : length.equals("/") ? 0.5 : length.equals(".") ? 1.5 : Integer.parseInt(length);
   }

   /** The note name of key index {@code key} (0 to 12), e.g. "F#"; the high C is plain "C". */
   public static String noteName(int key) {
      return key == 12 ? "C" : NOTE_NAMES[key];
   }

   /** The name used in the saved records. */
   public String id() {
      return this.id;
   }

   public String title() {
      return this.title;
   }

   public String shortTitle() {
      return this.shortTitle;
   }

   public int length() {
      return this.keys.length;
   }

   public int key(int note) {
      return this.keys[note];
   }

   public double beats(int note) {
      return this.beats[note];
   }
}
