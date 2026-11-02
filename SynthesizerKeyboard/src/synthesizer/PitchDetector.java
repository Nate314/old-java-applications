package synthesizer;

/**
 * Finds the pitch of one short stretch of sound (a voice, a whistle, a real instrument) with the YIN method: the
 * shift at which the sound best matches itself is its period. The samples are halved in rate first (pairs averaged),
 * which is plenty for notes up to a whistle's and keeps the work small enough for the browser build.
 */
public final class PitchDetector {
   /** The lowest and highest pitches looked for: a low male voice to a high whistle. */
   public static final double LOWEST_HZ = 75;
   public static final double HIGHEST_HZ = 2500;
   /** Below this loudness (root mean square, samples from -1 to 1) the sound is taken as silence. */
   public static final double QUIET = 0.006;
   /** How closely a shifted copy must match for that shift to count as the period (YIN's threshold). */
   private static final double MATCH = 0.15;

   private PitchDetector() {
   }

   /** The loudness of the samples: their root mean square. */
   public static double level(float[] samples) {
      double sum = 0;
      for (float sample : samples) {
         sum += sample * sample;
      }
      return Math.sqrt(sum / Math.max(1, samples.length));
   }

   /** The pitch of the samples in Hz, or -1 when they are too quiet or have no clear pitch. */
   public static double frequency(float[] samples, float sampleRate) {
      if (level(samples) < QUIET) {
         return -1;
      }
      int length = samples.length / 2;
      double rate = sampleRate / 2.0;
      double[] x = new double[length];
      for (int i = 0; i < length; i++) {
         x[i] = (samples[2 * i] + samples[2 * i + 1]) / 2.0;
      }
      int shortest = Math.max(2, (int)(rate / HIGHEST_HZ));
      int longest = Math.min(length / 2, (int)(rate / LOWEST_HZ));
      int window = length - longest;
      // The difference between the sound and itself shifted by tau, normalized by its running mean (YIN steps 2-3).
      double[] d = new double[longest + 1];
      d[0] = 1;
      double running = 0;
      for (int tau = 1; tau <= longest; tau++) {
         double sum = 0;
         for (int i = 0; i < window; i++) {
            double delta = x[i] - x[i + tau];
            sum += delta * delta;
         }
         running += sum;
         d[tau] = running == 0 ? 1 : sum * tau / running;
      }
      // The first dip under the threshold, followed down to its lowest point (YIN step 4).
      for (int tau = shortest; tau < longest; tau++) {
         if (d[tau] < MATCH) {
            while (tau + 1 < longest && d[tau + 1] < d[tau]) {
               tau++;
            }
            // A parabola through the dip and its neighbours places the period between samples (YIN step 5).
            double before = d[tau - 1];
            double after = d[tau + 1];
            double bend = before + after - 2 * d[tau];
            double period = bend > 0 ? tau + (before - after) / (2 * bend) : tau;
            return rate / period;
         }
      }
      return -1;
   }

   /** The nearest MIDI note to a pitch (60 is middle C, 69 the A at 440 Hz). */
   public static int midiNote(double hz) {
      return (int)Math.round(69 + 12 * Math.log(hz / 440) / Math.log(2));
   }

   /** How far a pitch is from its nearest note, in cents (-50 to 50; positive is sharp). */
   public static int cents(double hz) {
      double note = 69 + 12 * Math.log(hz / 440) / Math.log(2);
      return (int)Math.round(100 * (note - Math.round(note)));
   }
}
