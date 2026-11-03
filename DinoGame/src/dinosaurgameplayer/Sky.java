package dinosaurgameplayer;

import java.awt.Color;

/**
 * Day and night on a run. Every 700 points night falls for 12 seconds: the paper turns black and the ink pale gray
 * (each color inverted), a moon rises with a new phase each night, and a few stars come out. Dusk and dawn take a
 * second each. The original bot only knows ink of blue 83, so from the first moment of dusk it sees nothing.
 */
final class Sky {
   static final int NIGHT_EVERY = 700;
   /** One night, dusk and dawn included: 12 s of steps. */
   static final int NIGHT_STEPS = 720;
   static final int FADE_STEPS = 60;
   /** Moon phases, one per night in turn: thin crescent, half, gibbous, full, then waning. */
   static final int PHASES = 7;

   private int nextNight = NIGHT_EVERY;
   /** Steps into the current night, or -1 by day. */
   private int nightStep = -1;
   private int nights;

   /** A new run starts by day. */
   void reset() {
      this.nextNight = NIGHT_EVERY;
      this.nightStep = -1;
      this.nights = 0;
   }

   /** One running step at this score. */
   void step(int score) {
      if (this.nightStep >= 0) {
         if (++this.nightStep >= NIGHT_STEPS) {
            this.nightStep = -1;
         }
      } else if (score >= this.nextNight) {
         this.nightStep = 0;
         this.nights++;
         this.nextNight = (score / NIGHT_EVERY + 1) * NIGHT_EVERY;
      }
   }

   /** 0 by day, 1 at full night, in between at dusk and dawn. */
   double darkness() {
      if (this.nightStep < 0) {
         return 0;
      }
      int edge = Math.min(this.nightStep + 1, NIGHT_STEPS - this.nightStep);
      double t = Math.min(1.0, edge / (double) FADE_STEPS);
      return t * t * (3 - 2 * t);
   }

   /** Nights so far this run. */
   int nights() {
      return this.nights;
   }

   /** This night's moon, 0 to PHASES - 1; 3 is full. */
   int moonPhase() {
      return Math.max(0, this.nights - 1) % PHASES;
   }

   /** Steps into this night, or -1 by day. */
   int nightStep() {
      return this.nightStep;
   }

   /**
    * Day's color turned toward night's by t. Past the first moment of dusk no color has blue 83, the one value the
    * original bot jumps at, so a fading sky never fools it into a jump (and inked obstacles never wake it).
    */
   static Color mix(Color day, Color night, double t) {
      if (t <= 0) {
         return day;
      }
      int r = lerp(day.getRed(), night.getRed(), t);
      int g = lerp(day.getGreen(), night.getGreen(), t);
      int b = lerp(day.getBlue(), night.getBlue(), t);
      if (b == ObstacleDetector.OBSTACLE_BLUE) {
         b += night.getBlue() < day.getBlue() ? -1 : 1;
      }
      return new Color(r, g, b);
   }

   /** A color's inverse, which is what night makes of it. */
   static Color inverse(Color c) {
      return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
   }

   /** A day color as it looks at this darkness: itself by day, its inverse at full night. */
   static Color tint(Color day, double darkness) {
      return mix(day, inverse(day), darkness);
   }

   private static int lerp(int a, int b, double t) {
      return (int) Math.round(a + (b - a) * Math.min(1, t));
   }
}
