package animate2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One night of Star Catcher, with no drawing: gold stars and black holes bounce around a field, a click on a gold
 * star catches it (another appears elsewhere), a click on a black hole costs {@link #HOLE_PENALTY} seconds, and the
 * night is caught once {@link Night#goal()} stars are, or lost when the clock runs out. Three catches in a row,
 * each within {@link #COMBO_GAP} seconds of the last, start a combo that adds {@link #COMBO_BONUS} seconds per catch.
 */
public final class CatchGame {
   public static final double GOLD_RADIUS = 16.0;
   /** A click this far outside a gold star still catches it. */
   public static final double CATCH_SLOP = 8.0;
   public static final double HOLE_RADIUS = 22.0;
   public static final double HOLE_PENALTY = 3.0;
   public static final double COMBO_GAP = 1.5;
   public static final double COMBO_BONUS = 0.5;
   /** A new gold star appears at least this far from every black hole. */
   private static final double SPAWN_CLEARANCE = 70.0;

   public enum Kind { GOLD, HOLE }

   public enum Hit { GOLD, HOLE, MISS, NONE }

   public enum State { PLAYING, CAUGHT, LOST }

   /** A gold star or a black hole. {@code age} counts seconds since it appeared. */
   public static final class Body {
      public final Kind kind;
      public double x;
      public double y;
      public double vx;
      public double vy;
      public double age;

      Body(Kind kind, double x, double y, double vx, double vy) {
         this.kind = kind;
         this.x = x;
         this.y = y;
         this.vx = vx;
         this.vy = vy;
      }

      public double radius() {
         return this.kind == Kind.GOLD ? GOLD_RADIUS : HOLE_RADIUS;
      }
   }

   private final Night night;
   private final double width;
   private final double height;
   private final Random random;
   private final List<Body> bodies = new ArrayList<>();
   private State state = State.PLAYING;
   private double secondsLeft = Night.SECONDS;
   private double sinceCatch = Double.MAX_VALUE;
   private int caught;
   private int combo;
   private int twinkledOut;
   private int swallowed;

   public CatchGame(Night night, double width, double height, Random random) {
      this.night = night;
      this.width = width;
      this.height = height;
      this.random = random;
      for (int i = 0; i < night.holes(); i++) {
         // Holes start spread out across the field, so none sits on another.
         double x = width * (i + 0.5) / night.holes();
         double y = height * (0.3 + 0.4 * random.nextDouble());
         double[] v = this.velocity(night.speed() / 3.0);
         this.bodies.add(new Body(Kind.HOLE, x, y, v[0], v[1]));
      }
      for (int i = 0; i < night.gold(); i++) {
         this.bodies.add(this.newGold());
      }
   }

   private double[] velocity(double speed) {
      double angle = this.random.nextDouble() * Math.PI * 2.0;
      return new double[]{Math.cos(angle) * speed, Math.sin(angle) * speed};
   }

   private Body newGold() {
      double r = GOLD_RADIUS;
      double x = 0;
      double y = 0;
      for (int tries = 0; tries < 50; tries++) {
         x = r + this.random.nextDouble() * (this.width - 2 * r);
         y = r + this.random.nextDouble() * (this.height - 2 * r);
         if (this.clearOfHoles(x, y)) {
            break;
         }
      }
      double[] v = this.velocity(this.night.speed() * (0.8 + 0.4 * this.random.nextDouble()));
      return new Body(Kind.GOLD, x, y, v[0], v[1]);
   }

   private boolean clearOfHoles(double x, double y) {
      for (Body body : this.bodies) {
         if (body.kind == Kind.HOLE && Math.hypot(body.x - x, body.y - y) < SPAWN_CLEARANCE) {
            return false;
         }
      }
      return true;
   }

   /** Moves everything on by {@code seconds} and runs the clock down. Does nothing once the night is over. */
   public void step(double seconds) {
      if (this.state != State.PLAYING) {
         return;
      }
      this.sinceCatch += seconds;
      this.secondsLeft = Math.max(0.0, this.secondsLeft - seconds);
      for (int i = 0; i < this.bodies.size(); i++) {
         Body body = this.bodies.get(i);
         body.age += seconds;
         if (body.kind == Kind.GOLD && this.night.pull() > 0) {
            if (this.pullIn(body, seconds)) {
               this.bodies.set(i, this.newGold());
               this.swallowed++;
               continue;
            }
         }
         if (body.kind == Kind.GOLD && this.night.life() > 0 && body.age >= this.night.life()) {
            this.bodies.set(i, this.newGold());
            this.twinkledOut++;
            continue;
         }
         body.x += body.vx * seconds;
         body.y += body.vy * seconds;
         this.bounce(body);
      }
      if (this.secondsLeft <= 0.0) {
         this.state = State.LOST;
      }
   }

   /** Pulls a gold star toward every black hole; returns true when one swallows it. */
   private boolean pullIn(Body gold, double seconds) {
      for (Body hole : this.bodies) {
         if (hole.kind != Kind.HOLE) {
            continue;
         }
         double dx = hole.x - gold.x;
         double dy = hole.y - gold.y;
         double distance = Math.hypot(dx, dy);
         if (distance < HOLE_RADIUS * 0.6) {
            return true;
         }
         // Inverse square, capped so a star that grazes a hole is not flung off at a huge speed.
         double pull = Math.min(400.0, this.night.pull() / (distance * distance)) * seconds;
         gold.vx += dx / distance * pull;
         gold.vy += dy / distance * pull;
      }
      // Keep the speed near the night's, so stars curve toward holes rather than fall in at once.
      double speed = Math.hypot(gold.vx, gold.vy);
      double most = this.night.speed() * 1.6;
      if (speed > most) {
         gold.vx *= most / speed;
         gold.vy *= most / speed;
      }
      return false;
   }

   /** Heads back inside, as the original bouncing modes do, so a body never sticks to an edge. */
   private void bounce(Body body) {
      double r = body.radius();
      if (body.x < r) {
         body.vx = Math.abs(body.vx);
      } else if (body.x > this.width - r) {
         body.vx = -Math.abs(body.vx);
      }
      if (body.y < r) {
         body.vy = Math.abs(body.vy);
      } else if (body.y > this.height - r) {
         body.vy = -Math.abs(body.vy);
      }
   }

   /**
    * A click (or the keyboard net) at {@code x, y}. The nearest gold star within reach is caught before any black
    * hole counts, so a click that touches both is a catch.
    */
   public Hit click(double x, double y) {
      if (this.state != State.PLAYING) {
         return Hit.NONE;
      }
      Body gold = null;
      double nearest = Double.MAX_VALUE;
      for (Body body : this.bodies) {
         double distance = Math.hypot(body.x - x, body.y - y);
         if (body.kind == Kind.GOLD && distance <= GOLD_RADIUS + CATCH_SLOP && distance < nearest) {
            gold = body;
            nearest = distance;
         }
      }
      if (gold != null) {
         this.combo = this.sinceCatch <= COMBO_GAP ? this.combo + 1 : 1;
         this.sinceCatch = 0.0;
         this.caught++;
         if (this.combo >= 3) {
            this.secondsLeft += COMBO_BONUS;
         }
         this.bodies.set(this.bodies.indexOf(gold), this.newGold());
         if (this.caught >= this.night.goal()) {
            this.state = State.CAUGHT;
         }
         return Hit.GOLD;
      }
      for (Body body : this.bodies) {
         if (body.kind == Kind.HOLE && Math.hypot(body.x - x, body.y - y) <= HOLE_RADIUS) {
            this.combo = 0;
            this.secondsLeft = Math.max(0.0, this.secondsLeft - HOLE_PENALTY);
            if (this.secondsLeft <= 0.0) {
               this.state = State.LOST;
            }
            return Hit.HOLE;
         }
      }
      this.combo = 0;
      return Hit.MISS;
   }

   public Night night() {
      return this.night;
   }

   public List<Body> bodies() {
      return Collections.unmodifiableList(this.bodies);
   }

   public State state() {
      return this.state;
   }

   public double secondsLeft() {
      return this.secondsLeft;
   }

   public int caught() {
      return this.caught;
   }

   /** Catches in the current run; 3 or more means each catch adds time. */
   public int combo() {
      return this.combo;
   }

   /** Gold stars that twinkled out uncaught, and that black holes swallowed (for the self-check). */
   public int twinkledOut() {
      return this.twinkledOut;
   }

   public int swallowed() {
      return this.swallowed;
   }

   /** Stars for a caught night, 0 otherwise. */
   public int stars() {
      return this.state == State.CAUGHT ? Night.starsFor(this.secondsLeft) : 0;
   }

   /** Seconds left, in whole tenths (as saved). */
   public int tenthsLeft() {
      return (int)Math.round(this.secondsLeft * 10.0);
   }
}
