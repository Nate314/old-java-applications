package animate2;

/**
 * The six nights of Star Catcher. Each one is played over one of the original five animation modes, and asks for
 * more gold stars, with more black holes, faster stars, stars that twinkle out and (last) holes that pull stars in.
 */
public enum Night {
   //        name         mode goal gold holes speed life  pull
   DRIFT    ("Drift",     0,    6,   3,   0,   60,   0,    0),
   BURST    ("Burst",     1,    8,   3,   1,   90,   0,    0),
   COMPASS  ("Compass",   2,   10,   3,   2,  110,   0,    0),
   CONFETTI ("Confetti",  3,   12,   3,   3,  120,   5.0,  0),
   FLOCK    ("Flock",     4,   12,   2,   3,  140,   4.0,  0),
   SUPERNOVA("Supernova", 1,   15,   3,   4,  150,   3.5,  900000);

   /** Seconds on the clock at the start of every night. */
   public static final double SECONDS = 30.0;
   /** Seconds left at the end for three and for two stars; any catch in time is one star. */
   public static final double THREE_STARS = 10.0;
   public static final double TWO_STARS = 5.0;

   private final String label;
   private final int mode;
   private final int goal;
   private final int gold;
   private final int holes;
   private final double speed;
   private final double life;
   private final double pull;

   Night(String label, int mode, int goal, int gold, int holes, double speed, double life, double pull) {
      this.label = label;
      this.mode = mode;
      this.goal = goal;
      this.gold = gold;
      this.holes = holes;
      this.speed = speed;
      this.life = life;
      this.pull = pull;
   }

   public String label() {
      return this.label;
   }

   /** The animation mode (0 to 4, as in the settings window) drawn behind the night. */
   public int mode() {
      return this.mode;
   }

   /** Gold stars to catch. */
   public int goal() {
      return this.goal;
   }

   /** Gold stars on the field at once. */
   public int gold() {
      return this.gold;
   }

   public int holes() {
      return this.holes;
   }

   /** Gold star speed in pixels per second (holes drift at a third of it). */
   public double speed() {
      return this.speed;
   }

   /** Seconds before an uncaught gold star twinkles out and appears elsewhere, or 0 for never. */
   public double life() {
      return this.life;
   }

   /** How hard black holes pull gold stars in (pixels cubed per second squared), or 0 for not at all. */
   public double pull() {
      return this.pull;
   }

   public int number() {
      return this.ordinal() + 1;
   }

   /** One to three stars for a night caught with {@code secondsLeft} on the clock. */
   public static int starsFor(double secondsLeft) {
      return secondsLeft >= THREE_STARS ? 3 : secondsLeft >= TWO_STARS ? 2 : 1;
   }
}
