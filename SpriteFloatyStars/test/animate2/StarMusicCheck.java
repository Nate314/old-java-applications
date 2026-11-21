package animate2;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Self-check for star music (S): the scale, what rings in each mode, how often, and letting notes go. Run with java -ea. */
public class StarMusicCheck {
   private static final int W = 800;
   private static final int H = 600;

   /** Notes the field played: {note, 1 for on or 0 for off}, with the field's clock at each. */
   private static final class Ear implements StarMusic.Voice {
      final StarField field;
      final List<int[]> notes = new ArrayList<>();
      final List<Double> at = new ArrayList<>();

      Ear(StarField field) {
         this.field = field;
         field.music().listen(this);
      }

      @Override
      public void on(int note, int velocity) {
         check(velocity > 0 && velocity <= 127, "velocity " + velocity + " in range");
         this.notes.add(new int[]{note, 1});
         this.at.add(this.field.clock());
      }

      @Override
      public void off(int note) {
         this.notes.add(new int[]{note, 0});
         this.at.add(this.field.clock());
      }

      int ons() {
         int count = 0;
         for (int[] n : this.notes) {
            count += n[1];
         }
         return count;
      }
   }

   public static void main(String[] args) {
      // The scale: middle C up two octaves, only pentatonic notes, rising with the share.
      check(StarMusic.note(0.0) == 60 && StarMusic.note(1.0) == 84 && StarMusic.note(-3) == 60 && StarMusic.note(9) == 84, "scale ends");
      int previous = 0;
      for (int i = 0; i <= 100; i++) {
         int note = StarMusic.note(i / 100.0);
         check(note >= previous && inScale(note), "note " + note + " for share " + i / 100.0);
         previous = note;
      }

      // Off (as it opens): nothing rings and no ripples are kept.
      StarField field = field(1, 300);
      Ear ear = new Ear(field);
      run(field, 5.0);
      check(ear.notes.isEmpty() && !field.music().on(), "music starts off");

      // On, in every mode: notes ring, never two within GAP, nor the same within SAME_NOTE_GAP, all in the scale,
      // and each is let go.
      for (int mode = 0; mode < StarField.MODE_NAMES.length; mode++) {
         field = field(mode, mode == StarField.SKY_WRITING || mode == StarField.ORBITS ? 400 : 300);
         ear = new Ear(field);
         field.toggleMusic();
         check(field.music().on(), "S turns the music on");
         run(field, 6.0);
         check(ear.ons() >= 5, "mode " + mode + " rings (" + ear.ons() + " notes)");
         double lastOn = Double.NEGATIVE_INFINITY;
         double[] lastOf = new double[128];
         java.util.Arrays.fill(lastOf, Double.NEGATIVE_INFINITY);
         for (int i = 0; i < ear.notes.size(); i++) {
            int[] n = ear.notes.get(i);
            double t = ear.at.get(i);
            if (n[1] == 1) {
               check(inScale(n[0]), "mode " + mode + " note " + n[0] + " is in the scale");
               check(t - lastOn >= StarMusic.GAP - 1e-9, "mode " + mode + " notes are spaced");
               check(t - lastOf[n[0]] >= StarMusic.SAME_NOTE_GAP - 1e-9, "mode " + mode + " the same note waits");
               lastOn = t;
               lastOf[n[0]] = t;
            }
         }
         check(field.music().sounding() <= 6, "mode " + mode + " only a few notes sound at once");
         field.toggleMusic();
         check(!field.music().on() && field.music().sounding() == 0, "mode " + mode + " S again lets every note go");
         int count = ear.ons();
         run(field, 2.0);
         check(ear.ons() == count, "mode " + mode + " silent once off");
      }

      // A bounce off the top edge near its right end rings high; off the left side near the bottom, low.
      check(edgeNote(780, 3, 0.0, 1.0) >= StarMusic.note(0.9), "top right bounce is high");
      check(edgeNote(3, 580, 1.0, 0.0) <= StarMusic.note(0.1), "bottom left bounce is low");

      // A burst strikes a low chord, whatever just rang.
      field = field(0, 1);
      ear = new Ear(field);
      field.toggleMusic();
      field.burst(400, 300, StarField.BURST);
      check(ear.ons() == 3 && ear.notes.get(0)[0] < StarMusic.LOW, "a burst strikes a low chord");
      run(field, 1.0);
      // The lone star may wrap and ring a note of its own meanwhile, so look for each chord note's own off.
      for (int i = 0; i < 3; i++) {
         int note = ear.notes.get(i)[0];
         boolean letGo = false;
         for (int j = 3; j < ear.notes.size(); j++) {
            letGo |= ear.notes.get(j)[0] == note && ear.notes.get(j)[1] == 0;
         }
         check(letGo, "the chord's note " + note + " is let go");
      }

      // Star Catcher's sky has no key to turn it on, and stays silent.
      check(!new StarField(1, new Dimension(W, H), 50, 10, false).music().on(), "Star Catcher's sky is silent");

      System.out.println("StarMusicCheck passed");
   }

   /** The one note a lone star rings bouncing at (x, y) with that velocity (Compass mode). */
   private static int edgeNote(double x, double y, double velocityX, double velocityY) {
      StarField field = field(2, 1);
      Ear ear = new Ear(field);
      field.toggleMusic();
      field.setStar(0, x, y, velocityX, velocityY);
      run(field, 0.1);
      check(ear.ons() == 1, "one bounce rings once (" + ear.ons() + ")");
      return ear.notes.get(0)[0];
   }

   private static boolean inScale(int note) {
      int degree = Math.floorMod(note - StarMusic.LOW, 12);
      return note >= StarMusic.LOW - 24 && note <= StarMusic.LOW + 24
         && (degree == 0 || degree == 2 || degree == 4 || degree == 7 || degree == 9);
   }

   private static StarField field(int mode, int stars) {
      return new StarField(mode, new Dimension(W, H), stars, 10, false);
   }

   private static void run(StarField field, double seconds) {
      for (double t = 0; t < seconds; t += 0.015) {
         field.advance(0.015, W, H);
      }
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
