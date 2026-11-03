package windowse;

import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;
import javax.swing.Timer;

/**
 * Presses a shortcut the given number of times, 100 ms apart, on a Swing timer: holds the Windows key and taps the
 * shortcut's key (Win+E ten times holds Windows once and taps E ten times, as the original did), or taps the Windows
 * key alone. {@link #stop()} ends it early and lets go of the Windows key.
 */
final class WindowsEPresser {
   private final Keys keys;
   private final Shortcut shortcut;
   private final int count;
   private final IntConsumer onTap;
   private final Runnable onDone;
   private final Timer timer;
   private int done;
   private boolean holding;
   private boolean finished;

   /** onTap gets how many presses are done so far; onDone runs once, after the last press or when stopped. */
   WindowsEPresser(Keys keys, Shortcut shortcut, int count, IntConsumer onTap, Runnable onDone) {
      this.keys = keys;
      this.shortcut = shortcut;
      this.count = count;
      this.onTap = onTap;
      this.onDone = onDone;
      this.timer = new Timer(100, event -> this.tap());
   }

   void start() {
      if (this.shortcut.keyCode != 0) {
         this.keys.press(KeyEvent.VK_WINDOWS);
         this.holding = true;
      }
      this.tap();
      if (!this.finished) {
         this.timer.start();
      }
   }

   void stop() {
      this.finish();
   }

   private void tap() {
      if (this.finished) {
         return;
      }
      if (this.done >= this.count) {
         this.finish();
         return;
      }
      int key = this.shortcut.keyCode == 0 ? KeyEvent.VK_WINDOWS : this.shortcut.keyCode;
      this.keys.press(key);
      this.keys.release(key);
      this.done++;
      this.onTap.accept(this.done);
   }

   private void finish() {
      if (this.finished) {
         return;
      }
      this.finished = true;
      this.timer.stop();
      if (this.holding) {
         this.keys.release(KeyEvent.VK_WINDOWS);
         this.holding = false;
      }
      this.onDone.run();
   }
}
