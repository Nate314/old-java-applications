package synthesizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.SwingUtilities;

/**
 * Listens through the microphone on a thread of its own and tells the window, on the Swing thread, which note it
 * hears (for the tuner) and when a note starts (for the lessons). A reading is taken every 1024 samples over the
 * last 2048 (about 46 ms of sound, a new reading about every 23 ms).
 */
public final class Microphone {
   private static final int WINDOW = 2048;
   private static final int STEP = 1024;

   /** What the microphone tells the window. All calls come on the Swing thread. */
   public interface Ears {
      /** The line is open and listening. */
      void listening();

      /** A note has started (a MIDI note number). */
      void heard(int midi);

      /** The note heard right now and how far off it is in cents, or -1 for none. */
      void hearing(int midi, int cents);

      /** The microphone could not be opened; {@code why} says why, in a few words. */
      void failed(String why);
   }

   private final Ears ears;
   /** Bumped on every start and stop, so a listening thread from before knows to end. */
   private volatile int run;

   public Microphone(Ears ears) {
      this.ears = ears;
   }

   public void start() {
      int myRun = ++this.run;
      Thread thread = new Thread(() -> this.open(myRun), "Microphone");
      thread.setDaemon(true);
      thread.start();
   }

   public void stop() {
      this.run++;
   }

   private boolean running(int myRun) {
      return this.run == myRun;
   }

   private void open(int myRun) {
      AudioFormat wanted = new AudioFormat(44100f, 16, 1, true, false);
      TargetDataLine line;
      try {
         line = AudioSystem.getTargetDataLine(wanted);
         line.open(wanted);
      } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
         String why = e.getMessage() == null || e.getMessage().isEmpty() ? "No microphone found" : e.getMessage();
         SwingUtilities.invokeLater(() -> {
            if (this.running(myRun)) {
               this.ears.failed(why);
            }
         });
         return;
      }
      try {
         line.start();
         SwingUtilities.invokeLater(() -> {
            if (this.running(myRun)) {
               this.ears.listening();
            }
         });
         this.listen(line, myRun);
      } finally {
         line.stop();
         line.close();
      }
   }

   /** Reads the line until stopped (or the line ends), taking a reading every STEP samples. */
   void listen(TargetDataLine line, int myRun) {
      float rate = line.getFormat().getSampleRate();
      boolean bigEndian = line.getFormat().isBigEndian();
      float[] window = new float[WINDOW];
      byte[] bytes = new byte[STEP * 2];
      NoteFollower follower = new NoteFollower();
      int lastShown = -2;
      int lastCents = 0;
      while (this.running(myRun)) {
         int read = 0;
         while (read < bytes.length && this.running(myRun)) {
            int n = line.read(bytes, read, bytes.length - read);
            if (n < 0) {
               return;
            }
            read += n;
         }
         if (read < bytes.length) {
            return;
         }
         System.arraycopy(window, STEP, window, 0, WINDOW - STEP);
         for (int i = 0; i < STEP; i++) {
            int high = bytes[2 * i + (bigEndian ? 0 : 1)];
            int low = bytes[2 * i + (bigEndian ? 1 : 0)] & 0xFF;
            window[WINDOW - STEP + i] = ((high << 8) | low) / 32768f;
         }
         double level = PitchDetector.level(window);
         double hz = PitchDetector.frequency(window, rate);
         int started = follower.feed(hz, level);
         int shown = hz > 0 ? PitchDetector.midiNote(hz) : -1;
         int cents = hz > 0 ? PitchDetector.cents(hz) : 0;
         if (started >= 0 || shown != lastShown || cents != lastCents) {
            lastShown = shown;
            lastCents = cents;
            SwingUtilities.invokeLater(() -> {
               if (this.running(myRun)) {
                  this.ears.hearing(shown, cents);
                  if (started >= 0) {
                     this.ears.heard(started);
                  }
               }
            });
         }
      }
   }
}
