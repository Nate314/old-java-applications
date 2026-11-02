package synthesizer;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;
import javax.swing.SwingUtilities;

/**
 * Assert-based self-check of the microphone: pitch finding, note following, and the whole path from sound bytes to
 * notes with a made-up line. Needs no microphone. Run with {@code java -ea}.
 */
public final class MicrophoneCheck {
   public static void main(String[] args) throws Exception {
      boolean enabled = false;
      assert enabled = true;
      if (!enabled) {
         throw new IllegalStateException("Run with -ea");
      }
      checkPitch();
      checkFollower();
      checkMicrophone();
      System.out.println("MicrophoneCheck: all checks passed");
   }

   /** One reading's worth of a tone with {@code harmonics} overtones (1 for a pure sine). */
   private static float[] tone(double hz, float rate, int harmonics, double amplitude) {
      float[] samples = new float[2048];
      for (int i = 0; i < samples.length; i++) {
         double t = i / (double)rate;
         double sum = 0;
         for (int h = 1; h <= harmonics; h++) {
            sum += Math.sin(2 * Math.PI * hz * h * t) / h;
         }
         samples[i] = (float)(amplitude * sum);
      }
      return samples;
   }

   private static void checkPitch() {
      for (float rate : new float[]{44100f, 48000f}) {
         // Middle C, A440 and a whistle's high A as sines; voice-like low notes and an E rich in overtones.
         double[][] cases = {{261.63, 1, 60}, {440, 1, 69}, {1760, 1, 93}, {130.81, 6, 48}, {329.63, 8, 64}, {98, 5, 43}};
         for (double[] c : cases) {
            double hz = PitchDetector.frequency(tone(c[0], rate, (int)c[1], 0.2), rate);
            assert PitchDetector.midiNote(hz) == (int)c[2] : c[0] + " Hz at " + rate + " read as " + hz;
            assert Math.abs(PitchDetector.cents(hz) - PitchDetector.cents(c[0])) <= 3 : c[0] + " Hz is off by " + PitchDetector.cents(hz);
         }
      }
      assert PitchDetector.frequency(new float[2048], 44100f) == -1 : "silence has no pitch";
      assert PitchDetector.frequency(tone(440, 44100f, 1, 0.002), 44100f) == -1 : "too quiet to count";
      Random random = new Random(7);
      float[] noise = new float[2048];
      for (int i = 0; i < noise.length; i++) {
         noise[i] = (float)(random.nextGaussian() * 0.2);
      }
      assert PitchDetector.frequency(noise, 44100f) == -1 : "noise has no pitch";
      assert PitchDetector.cents(446) == 23 && PitchDetector.cents(434) == -24 && PitchDetector.cents(440) == 0;
   }

   /** Feeds {@code frames} readings of one pitch (-1 for none) at one loudness; returns the notes started. */
   private static List<Integer> feed(NoteFollower follower, int frames, double hz, double level) {
      List<Integer> started = new ArrayList<>();
      for (int frame = 0; frame < frames; frame++) {
         int note = follower.feed(hz, level);
         if (note >= 0) {
            started.add(note);
         }
      }
      return started;
   }

   private static void checkFollower() {
      NoteFollower f = new NoteFollower();
      assert feed(f, NoteFollower.STEADY - 1, 329.63, 0.1).isEmpty() : "a blip shorter than STEADY is not a note";
      assert feed(f, 1, 329.63, 0.1).equals(List.of(64)) : "a steady E counts once it holds";
      assert feed(f, 40, 329.63, 0.1).isEmpty() : "holding it does not count it again";
      assert feed(f, NoteFollower.GAP, -1, 0).isEmpty();
      assert feed(f, 10, 329.63, 0.1).equals(List.of(64)) : "after a pause the same note counts again";
      assert feed(f, NoteFollower.GAP - 1, -1, 0).isEmpty();
      assert feed(f, 10, 329.63, 0.1).isEmpty() : "a pause shorter than GAP is the same note";
      assert feed(f, 3, 329.63, 0.02).isEmpty();
      assert feed(f, 10, 329.63, 0.1).equals(List.of(64)) : "a new attack after the sound faded counts again (la la la)";
      assert feed(f, 10, 293.66, 0.1).equals(List.of(62)) : "a different note counts once it holds";
      assert feed(f, 10, 587.33, 0.1).isEmpty() : "an octave slip of a held note is not a new note";

      f = new NoteFollower();
      List<Integer> fading = new ArrayList<>();
      for (double level = 0.3; level > 0.001; level *= 0.85) {
         fading.addAll(feed(f, 1, 196, level));
      }
      assert fading.equals(List.of(55)) : "a struck string fading away is one note: " + fading;

      f = new NoteFollower();
      List<Integer> glide = new ArrayList<>();
      for (int step = 0; step < 6; step++) {
         glide.addAll(feed(f, 2, 261.63 * Math.pow(2, step / 12.0), 0.1));
      }
      assert glide.isEmpty() : "sliding through notes counts none of them: " + glide;
      assert feed(f, 10, 392, 0.1).equals(List.of(67)) : "the note the slide lands on counts";
   }

   /** The microphone's whole path, from 16-bit sound bytes to notes, with a line that sings E D C D E E E. */
   private static void checkMicrophone() throws Exception {
      float rate = 44100f;
      double[] melody = {329.63, 293.66, 261.63, 293.66, 329.63, 329.63, 329.63};
      int noteSamples = (int)(rate * 0.35);
      int gapSamples = (int)(rate * 0.08);
      short[] pcm = new short[melody.length * (noteSamples + gapSamples)];
      int at = 0;
      for (double hz : melody) {
         for (int i = 0; i < noteSamples; i++, at++) {
            double envelope = Math.min(1, i / 400.0);
            double wave = Math.sin(2 * Math.PI * hz * i / rate) + 0.4 * Math.sin(4 * Math.PI * hz * i / rate);
            pcm[at] = (short)(9000 * envelope * wave);
         }
         at += gapSamples;
      }
      AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
      int[] position = {0};
      TargetDataLine line = (TargetDataLine)Proxy.newProxyInstance(MicrophoneCheck.class.getClassLoader(),
         new Class<?>[]{TargetDataLine.class}, (proxy, method, args) -> {
            if (method.getName().equals("getFormat")) {
               return format;
            }
            if (method.getName().equals("read")) {
               byte[] bytes = (byte[])args[0];
               int offset = (Integer)args[1];
               int length = Math.min((Integer)args[2], (pcm.length - position[0]) * 2) & ~1;
               if (length <= 0) {
                  return -1;
               }
               for (int i = 0; i < length / 2; i++) {
                  short sample = pcm[position[0]++];
                  bytes[offset + 2 * i] = (byte)sample;
                  bytes[offset + 2 * i + 1] = (byte)(sample >> 8);
               }
               return length;
            }
            return null;
         });
      List<Integer> heard = new ArrayList<>();
      Microphone microphone = new Microphone(new Microphone.Ears() {
         @Override
         public void listening() {
         }

         @Override
         public void heard(int midi) {
            heard.add(midi);
         }

         @Override
         public void hearing(int midi, int cents) {
         }

         @Override
         public void failed(String why) {
         }
      });
      microphone.listen(line, 0);
      SwingUtilities.invokeAndWait(() -> { });
      assert heard.equals(List.of(64, 62, 60, 62, 64, 64, 64)) : "heard " + heard;
   }
}
