package test;

import java.awt.Component;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import javax.swing.Timer;

/**
 * Hover to press: with it on, resting the pointer on a target for {@link #MS} presses it, so the key works for
 * someone who can point but not click (a head mouse, an eye tracker, a hand that cannot hold a button down). A
 * target presses once; the pointer must leave it and come back to press it again, and a real click there calls the
 * wait off.
 */
final class Dwell {
   static final int MS = 1000;
   private final Component owner;
   private final BooleanSupplier on;
   private final IntConsumer press;
   private final Timer timer;
   /** The target under the pointer (-1 for none), when the pointer came onto it, and whether it pressed already. */
   private int target = -1;
   private long since;
   private boolean spent;

   Dwell(Component owner, BooleanSupplier on, IntConsumer press) {
      this.owner = owner;
      this.on = on;
      this.press = press;
      this.timer = new Timer(40, e -> this.tick(System.currentTimeMillis()));
   }

   /** The pointer is over that target now (-1: over none). */
   void at(int target, long now) {
      if (target == this.target) {
         return;
      }
      this.target = target;
      this.since = now;
      this.spent = false;
      if (target >= 0 && this.on.getAsBoolean()) {
         this.timer.start();
      } else {
         this.timer.stop();
      }
      this.owner.repaint();
   }

   void at(int target) {
      this.at(target, System.currentTimeMillis());
   }

   /** A click on the target: it pressed, so the wait is off until the pointer comes back. */
   void clicked() {
      this.spent = true;
      this.timer.stop();
      this.owner.repaint();
   }

   void tick(long now) {
      if (this.target < 0 || this.spent || !this.on.getAsBoolean()) {
         this.timer.stop();
         return;
      }
      if (now - this.since >= MS) {
         this.spent = true;
         this.timer.stop();
         this.press.accept(this.target);
      }
      this.owner.repaint();
   }

   /** How far that target's wait has got, 0 to 1 (0 when it is not waiting). */
   double progress(int target, long now) {
      if (target != this.target || this.spent || !this.on.getAsBoolean()) {
         return 0;
      }
      return Math.max(0, Math.min(1, (now - this.since) / (double) MS));
   }

   double progress(int target) {
      return this.progress(target, System.currentTimeMillis());
   }
}
