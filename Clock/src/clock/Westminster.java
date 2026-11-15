package clock;

/**
 * The Westminster quarters, as Big Ben's clock tower rings them: four bells in five changes, one change at quarter
 * past, two at half past, three at quarter to, four on the hour and then the hour struck on a low bell.
 */
final class Westminster {
   /** Steps of {@link Chime#toll}: a MIDI note, {@code REST} to let the last bell ring on, or {@code HUSH} to let it go. */
   static final int REST = -1;
   static final int HUSH = -2;
   /** One step, in milliseconds. */
   static final int STEP_MILLIS = 650;
   /** The hour bell, E3 (Big Ben itself is an E). */
   static final int HOUR_BELL = 52;
   /** G#4, F#4, E4 and B3, as the five changes ring them. */
   private static final int[][] CHANGES = {
      {68, 66, 64, 59}, {64, 68, 66, 59}, {64, 66, 68, 64}, {68, 64, 66, 59}, {59, 66, 68, 64}};
   /** Which changes ring at each quarter: on the hour, quarter past, half past, quarter to. */
   private static final int[][] QUARTERS = {{1, 2, 3, 4}, {0}, {1, 2}, {3, 4, 0}};

   private Westminster() {
   }

   /** What rings at {@code quarter} (0 to 3) of {@code hour24}: the changes, then on the hour one stroke per hour. */
   static int[] steps(int hour24, int quarter) {
      int[] changes = QUARTERS[quarter];
      int strokes = quarter == 0 ? (hour24 % 12 == 0 ? 12 : hour24 % 12) : 0;
      int[] steps = new int[changes.length * 6 + (strokes > 0 ? 2 + strokes * 4 : 0)];
      java.util.Arrays.fill(steps, REST);
      int at = 0;
      for (int change : changes) {
         System.arraycopy(CHANGES[change], 0, steps, at, 4);
         steps[at + 5] = HUSH; // the last note of a change is held, then a breath
         at += 6;
      }
      for (int i = 0; i < strokes; i++) {
         steps[at + 2 + i * 4] = HOUR_BELL;
         steps[at + 4 + i * 4] = HUSH;
      }
      return steps;
   }
}
