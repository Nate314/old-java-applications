package dinosaurgameplayer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Puffs of dust at the dinosaur's feet: a few as it takes off, more as it lands, and a speck now and then as it
 * runs. They drift back with the ground and fade. Only drawn: nothing in the game or the bot depends on them, and
 * they stay behind the dinosaur, out of the bot's sight.
 */
final class Dust {
   private static final class Puff {
      double x;
      double y;
      double vx;
      double vy;
      int life;
      final int size;
      final int maxLife;

      Puff(double x, double y, double vx, double vy, int life, int size) {
         this.x = x;
         this.y = y;
         this.vx = vx;
         this.vy = vy;
         this.life = life;
         this.maxLife = life;
         this.size = size;
      }
   }

   private final List<Puff> puffs = new ArrayList<>();
   private final Random random = new Random(7);
   private boolean wasJumping;

   void clear() {
      this.puffs.clear();
      this.wasJumping = false;
   }

   /** One running step: kicks up dust when the dinosaur leaves or meets the ground, and moves the dust. */
   void step(Runner runner) {
      boolean jumping = runner.isJumping();
      double feet = Runner.GROUND - 2;
      if (jumping && !this.wasJumping) {
         this.burst(Runner.DINO_X + 14, feet, 5, 0.8);
      } else if (!jumping && this.wasJumping) {
         this.burst(Runner.DINO_X + 18, feet, 8, 1.3);
      } else if (!jumping && runner.state() == Runner.State.RUNNING && runner.steps() % 9 == 0) {
         this.puffs.add(new Puff(Runner.DINO_X + 8, feet, -0.6 - this.random.nextDouble(), -0.3 - this.random.nextDouble() * 0.4, 14, 2));
      }
      this.wasJumping = jumping;
      double drift = runner.speed() * 0.35;
      for (Iterator<Puff> it = this.puffs.iterator(); it.hasNext();) {
         Puff p = it.next();
         p.x += p.vx - drift;
         p.y = Math.min(feet, p.y + p.vy);
         p.vy += 0.06;
         p.vx *= 0.94;
         if (--p.life <= 0 || p.x < -4) {
            it.remove();
         }
      }
   }

   private void burst(double x, double y, int count, double force) {
      for (int i = 0; i < count; i++) {
         double vx = (this.random.nextDouble() * 2 - 1.3) * 1.6 * force;
         double vy = -(0.4 + this.random.nextDouble() * 1.1) * force;
         this.puffs.add(new Puff(x + this.random.nextInt(14) - 7, y, vx, vy, 16 + this.random.nextInt(12), 2 + this.random.nextInt(2)));
      }
   }

   /** Draws the dust, fading from color toward paper as each puff ages. */
   void paint(Graphics2D g, Color color, Color paper) {
      for (Puff p : this.puffs) {
         double age = 1 - p.life / (double) p.maxLife;
         g.setColor(Sky.mix(color, paper, age * 0.9));
         g.fillRect((int) Math.round(p.x), (int) Math.round(p.y) - p.size, p.size, p.size);
      }
   }

   int count() {
      return this.puffs.size();
   }

   /** The right edge of the furthest right puff (0 when there is none). */
   int right() {
      int r = 0;
      for (Puff p : this.puffs) {
         r = Math.max(r, (int) Math.round(p.x) + p.size);
      }
      return r;
   }
}
