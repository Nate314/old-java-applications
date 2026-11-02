package dinosaurgameplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The dinosaur game itself, without drawing: a dinosaur runs right, jumps cacti and low birds and ducks the rest,
 * a little faster every step. Modeled on the Chrome game the bot was written for: a jump that goes higher while the
 * key is held, gaps that widen with speed, birds once it is fast. One step is 1/60 s, so a seeded game always plays
 * out the same way.
 */
final class Runner {
   static final int WIDTH = 720;
   /** The ground line's y; heights below are measured up from it. */
   static final int GROUND = 168;
   static final int DINO_X = 40;
   static final int DINO_W = 40;
   static final int DINO_H = 40;
   static final int DUCK_W = 52;
   static final int DUCK_H = 22;
   static final double STEP_MS = 1000.0 / 60.0;
   static final double START_SPEED = 6.0;
   static final double MAX_SPEED = 13.0;
   static final double ACCELERATION = 0.001;
   static final double BIRD_SPEED = 8.5;
   private static final double GRAVITY = 0.6;
   private static final double JUMP_RISE = 10.0;
   /** Letting go of the jump key cuts the rise to this, once the dinosaur is MIN_JUMP up. */
   private static final double DROP_RISE = 5.0;
   private static final double MIN_JUMP = 30.0;
   /** Extra fall per step while Down is held in the air. */
   private static final double FAST_FALL = 1.2;

   enum State { READY, RUNNING, CRASHED }

   enum Kind { SMALL_CACTUS, LARGE_CACTUS, BIRD }

   /** Bird heights: the bottom of the bird above the ground. Low ones are jumped, middle ones ducked, high ones run under. */
   static final int[] BIRD_HEIGHTS = { 10, 30, 56 };

   static final class Obstacle {
      final Kind kind;
      final int size;
      final int w;
      final int h;
      /** Height of the obstacle's bottom above the ground (0 for cacti). */
      final int lift;
      final int gap;
      double x;
      /** Set by the bot when it jumps while this is the next obstacle. */
      boolean botJumped;

      Obstacle(Kind kind, int size, int lift, double x, int gap) {
         this.kind = kind;
         this.size = size;
         this.lift = lift;
         this.x = x;
         this.gap = gap;
         this.w = kind == Kind.BIRD ? 42 : (kind == Kind.SMALL_CACTUS ? 17 : 25) * size;
         this.h = kind == Kind.BIRD ? 28 : kind == Kind.SMALL_CACTUS ? 35 : 50;
      }

      int top() {
         return GROUND - this.lift - this.h;
      }
   }

   private final Random random;
   private final List<Obstacle> obstacles = new ArrayList<>();
   private final double[] clouds = { 120, 380, 610 };
   private State state = State.READY;
   private double speed = START_SPEED;
   private double distance;
   private long steps;
   private long idleSteps;
   private double height;
   private double rise;
   private boolean jumping;
   private boolean reachedMin;
   private boolean released;
   private boolean downHeld;
   private Obstacle crashedInto;

   Runner(long seed) {
      this.random = new Random(seed);
   }

   // ---- input ----

   /** The jump key goes down: starts a run from the ready screen, or jumps. */
   void press() {
      if (this.state == State.READY) {
         this.state = State.RUNNING;
         this.idleSteps = 0;
      }
      if (this.state == State.RUNNING && !this.jumping) {
         this.jumping = true;
         this.rise = JUMP_RISE;
         this.reachedMin = false;
         this.released = false;
      }
   }

   /** The jump key comes up: the jump stops rising early (a short hop), but never below MIN_JUMP. */
   void release() {
      this.released = true;
   }

   void duck(boolean held) {
      this.downHeld = held;
   }

   /** Back to the start of a new run, running at once. */
   void restart() {
      this.obstacles.clear();
      this.speed = START_SPEED;
      this.distance = 0;
      this.steps = 0;
      this.idleSteps = 0;
      this.height = 0;
      this.rise = 0;
      this.jumping = false;
      this.crashedInto = null;
      this.state = State.RUNNING;
   }

   // ---- one step ----

   void step() {
      if (this.state != State.RUNNING) {
         this.idleSteps++;
         return;
      }
      this.steps++;
      if (this.jumping) {
         this.height += this.rise;
         this.rise -= GRAVITY + (this.downHeld ? FAST_FALL : 0);
         if (this.height >= MIN_JUMP) {
            this.reachedMin = true;
         }
         if (this.released && this.reachedMin && this.rise > DROP_RISE) {
            this.rise = DROP_RISE;
         }
         if (this.height <= 0) {
            this.height = 0;
            this.rise = 0;
            this.jumping = false;
         }
      }

      this.distance += this.speed;
      for (Obstacle o : this.obstacles) {
         o.x -= this.speed;
      }
      this.obstacles.removeIf(o -> o.x + o.w < 0);
      for (int i = 0; i < this.clouds.length; i++) {
         this.clouds[i] -= this.speed * 0.2;
         if (this.clouds[i] < -60) {
            this.clouds[i] += WIDTH + 120;
         }
      }
      Obstacle last = this.obstacles.isEmpty() ? null : this.obstacles.get(this.obstacles.size() - 1);
      if (last == null ? this.steps > 50 : last.x + last.w + last.gap < WIDTH) {
         this.obstacles.add(this.spawn());
      }
      this.speed = Math.min(MAX_SPEED, this.speed + ACCELERATION);

      for (Obstacle o : this.obstacles) {
         if (this.hits(o)) {
            this.state = State.CRASHED;
            this.crashedInto = o;
            this.idleSteps = 0;
            break;
         }
      }
   }

   private Obstacle spawn() {
      int kinds = this.speed >= BIRD_SPEED ? 3 : 2;
      Kind kind = Kind.values()[this.random.nextInt(kinds)];
      int size = 1;
      int lift = 0;
      if (kind == Kind.BIRD) {
         lift = BIRD_HEIGHTS[this.random.nextInt(BIRD_HEIGHTS.length)];
      } else if (kind == Kind.SMALL_CACTUS || this.speed >= 7.0) {
         size = 1 + this.random.nextInt(3);
      }
      Obstacle probe = new Obstacle(kind, size, lift, WIDTH, 0);
      // Room to land after the obstacle at this speed, plus some, up to half again as much.
      int minGap = (int) Math.round(probe.w * this.speed + (kind == Kind.BIRD ? 150 : 120) * 0.6);
      int gap = minGap + this.random.nextInt((int) Math.round(minGap * 0.5) + 1);
      return new Obstacle(kind, size, lift, WIDTH, gap);
   }

   /** Hit boxes a little inside the drawings, so a pixel of overlap is not a crash. */
   private boolean hits(Obstacle o) {
      int ox = (int) o.x + 3;
      int ow = o.w - 6;
      int oy = o.top() + (o.kind == Kind.BIRD ? 6 : 3);
      int oh = o.h - (o.kind == Kind.BIRD ? 12 : 3);
      int top = this.dinoTop();
      if (this.isDucking()) {
         return overlaps(DINO_X + 2, top + 3, DUCK_W - 6, DUCK_H - 3, ox, oy, ow, oh);
      }
      // Head and body.
      return overlaps(DINO_X + 20, top + 1, 19, 14, ox, oy, ow, oh)
         || overlaps(DINO_X + 4, top + 14, 26, 20, ox, oy, ow, oh)
         || overlaps(DINO_X + 10, top + 34, 12, 6, ox, oy, ow, oh);
   }

   private static boolean overlaps(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
      return ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah;
   }

   // ---- what is on screen ----

   State state() {
      return this.state;
   }

   int score() {
      return (int) (this.distance * 0.025);
   }

   double speed() {
      return this.speed;
   }

   long steps() {
      return this.steps;
   }

   /** Steps since the run crashed (or since the ready screen showed). */
   long idleSteps() {
      return this.idleSteps;
   }

   boolean isJumping() {
      return this.jumping;
   }

   boolean isRising() {
      return this.jumping && this.rise > 0;
   }

   boolean isDucking() {
      return this.downHeld && !this.jumping && this.state == State.RUNNING;
   }

   int dinoTop() {
      return GROUND - (int) Math.round(this.height) - (this.isDucking() ? DUCK_H : DINO_H);
   }

   double distance() {
      return this.distance;
   }

   List<Obstacle> obstacles() {
      return this.obstacles;
   }

   double[] clouds() {
      return this.clouds;
   }

   Obstacle crashedInto() {
      return this.crashedInto;
   }

   /** The obstacle across column x, or else the first one ahead of it, or null. */
   Obstacle at(int x) {
      for (Obstacle o : this.obstacles) {
         if (o.x + o.w > x) {
            return o;
         }
      }
      return null;
   }
}
