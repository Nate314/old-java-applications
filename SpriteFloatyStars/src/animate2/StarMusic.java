package animate2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

/**
 * S: star music. The field becomes a music box: a star rings a note when it bounces off an edge (pitched by where
 * along the edge), wraps round one, swings past the line to the right of its sun (higher the closer it orbits) or
 * lands in its letter (higher further along the message), and a burst strikes a low chord. Every note is one of a
 * pentatonic scale, so any mix of them sounds right, and only a few ring at a time. Each note draws a ripple where
 * its star was, so you see which star sang.
 */
final class StarMusic {
   /** Where the notes go: the MIDI synthesizer, or (in the self-check) a list. */
   interface Voice {
      void on(int note, int velocity);

      void off(int note);
   }

   /** The major pentatonic scale from middle C, over two octaves and the top C. */
   private static final int[] SCALE = {0, 2, 4, 7, 9};
   static final int LOW = 60;
   static final int NOTES = 11;
   /** At most one note this often (field seconds), and the same note not again sooner than SAME_NOTE_GAP. */
   static final double GAP = 0.07;
   static final double SAME_NOTE_GAP = 0.25;
   private static final double HOLD_SECONDS = 0.35;
   private static final double RIPPLE_SECONDS = 0.8;
   /** General MIDI FX 3 (Crystal): a soft chime on the desktop, a triangle wave in the browser. */
   private static final int CRYSTAL = 98;
   private static final int SLOTS = 32;

   private boolean on;
   private Voice voice;
   private boolean noSynth;
   private double last = Double.NEGATIVE_INFINITY;
   private final double[] lastOf = new double[128];
   /** Notes still sounding (note, or -1) and when each is let go; ripples where each note rang. */
   private final int[] held = new int[SLOTS];
   private final double[] offAt = new double[SLOTS];
   private final double[] rippleX = new double[SLOTS];
   private final double[] rippleY = new double[SLOTS];
   private final double[] rippleAt = new double[SLOTS];
   private final int[] rippleNote = new int[SLOTS];
   private int next;

   StarMusic() {
      java.util.Arrays.fill(this.lastOf, Double.NEGATIVE_INFINITY);
      java.util.Arrays.fill(this.held, -1);
      java.util.Arrays.fill(this.rippleAt, Double.NEGATIVE_INFINITY);
   }

   boolean on() {
      return this.on;
   }

   /** S: music on or off; off lets every note go at once. */
   void toggle() {
      this.on = !this.on;
      if (!this.on) {
         this.update(Double.POSITIVE_INFINITY);
      }
   }

   /** The self-check listens here instead of the synthesizer. */
   void listen(Voice voice) {
      this.voice = voice;
   }

   /** The note for a share (0 to 1) of the way up the scale. */
   static int note(double share) {
      int step = (int)Math.round(Math.max(0.0, Math.min(1.0, share)) * (NOTES - 1));
      return LOW + 12 * (step / SCALE.length) + SCALE[step % SCALE.length];
   }

   /**
    * A star at (x, y) asks to ring the note {@code share} of the way up the scale, {@code loudness} (0 to 1) hard.
    * It rings only with the music on and when no note rang in the last GAP seconds, nor this one in SAME_NOTE_GAP.
    */
   boolean ring(double share, double loudness, double x, double y, double clock) {
      int note = note(share);
      if (!this.on || clock - this.last < GAP || clock - this.lastOf[note] < SAME_NOTE_GAP) {
         return false;
      }
      this.strike(note, 40 + (int)(60 * Math.max(0.0, Math.min(1.0, loudness))), x, y, clock);
      return true;
   }

   /** A burst at (x, y): a low chord, louder for a harder burst, whatever else is ringing. */
   void burst(double strength, double x, double y, double clock) {
      if (!this.on) {
         return;
      }
      int velocity = 50 + (int)Math.min(60.0, strength * 9.0);
      for (int note : new int[]{LOW - 24, LOW - 12, LOW - 5}) {
         this.strike(note, velocity, x, y, clock);
      }
   }

   private void strike(int note, int velocity, double x, double y, double clock) {
      Voice out = this.voice();
      int slot = this.next;
      this.next = (this.next + 1) % SLOTS;
      if (this.held[slot] >= 0 && out != null) {
         out.off(this.held[slot]);
      }
      if (out != null) {
         out.on(note, velocity);
      }
      this.held[slot] = note;
      this.offAt[slot] = clock + HOLD_SECONDS;
      this.rippleX[slot] = x;
      this.rippleY[slot] = y;
      this.rippleAt[slot] = clock;
      this.rippleNote[slot] = note;
      this.last = clock;
      this.lastOf[note] = clock;
   }

   /** Lets go of the notes that have rung long enough. */
   void update(double clock) {
      for (int slot = 0; slot < SLOTS; slot++) {
         if (this.held[slot] >= 0 && clock >= this.offAt[slot]) {
            if (this.voice != null) {
               this.voice.off(this.held[slot]);
            }
            this.held[slot] = -1;
         }
      }
   }

   /** Notes sounding now. */
   int sounding() {
      int count = 0;
      for (int note : this.held) {
         count += note >= 0 ? 1 : 0;
      }
      return count;
   }

   /** A ring spreading from where each recent note rang, colored by its pitch (low gold through high sky blue). */
   void draw(Graphics2D g2, double clock) {
      Stroke stroke = g2.getStroke();
      g2.setStroke(new BasicStroke(2f));
      for (int slot = 0; slot < SLOTS; slot++) {
         double age = (clock - this.rippleAt[slot]) / RIPPLE_SECONDS;
         if (age < 0.0 || age >= 1.0) {
            continue;
         }
         double pitch = Math.max(0.0, Math.min(1.0, (this.rippleNote[slot] - LOW) / 24.0));
         Color hue = Color.getHSBColor((float)(0.13 + 0.45 * pitch), 0.55f, 1f);
         int r = (int)(4 + 26 * Math.sqrt(age));
         g2.setColor(new Color(hue.getRed(), hue.getGreen(), hue.getBlue(), (int)(220 * (1.0 - age))));
         g2.drawOval((int)this.rippleX[slot] - r, (int)this.rippleY[slot] - r, 2 * r, 2 * r);
      }
      g2.setStroke(stroke);
   }

   /** Opens the synthesizer on the first note; with none the ripples still show. */
   private Voice voice() {
      if (this.voice == null && !this.noSynth) {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel channel = synthesizer.getChannels()[0];
            channel.programChange(CRYSTAL);
            this.voice = new Voice() {
               @Override
               public void on(int note, int velocity) {
                  channel.noteOn(note, velocity);
               }

               @Override
               public void off(int note) {
                  channel.noteOff(note);
               }
            };
         } catch (Exception e) {
            this.noSynth = true;
         }
      }
      return this.voice;
   }
}
