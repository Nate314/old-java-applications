package windowse;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.Timer;

/**
 * Presses a list of steps in order, one press every 100 ms, on a Swing timer. Each press holds the Windows key only
 * around its own key (Windows down, E down, E up, Windows up), so between presses the Windows key is up and keys
 * typed on the keyboard stay ordinary keys; the Windows key alone is tapped. {@link #stop()} ends it early.
 */
final class WindowsEPresser {
   private final Keys keys;
   private final List<Shortcut> taps = new ArrayList<>();
   private final IntConsumer onTap;
   private final Runnable onDone;
   private final Timer timer;
   private int done;
   private boolean finished;

   /** onTap gets how many presses are done so far; onDone runs once, after the last press or when stopped. */
   WindowsEPresser(Keys keys, List<Step> steps, IntConsumer onTap, Runnable onDone) {
      this.keys = keys;
      for (Step step : steps) {
         for (int i = 0; i < step.times; i++) {
            this.taps.add(step.shortcut);
         }
      }
      this.onTap = onTap;
      this.onDone = onDone;
      this.timer = new Timer(100, event -> this.tap());
   }

   void start() {
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
      if (this.done >= this.taps.size()) {
         this.finish();
         return;
      }
      int key = this.taps.get(this.done).keyCode;
      this.keys.press(KeyEvent.VK_WINDOWS);
      if (key != 0) {
         this.keys.press(key);
         this.keys.release(key);
      }
      this.keys.release(KeyEvent.VK_WINDOWS);
      this.done++;
      this.onTap.accept(this.done);
   }

   private void finish() {
      if (this.finished) {
         return;
      }
      this.finished = true;
      this.timer.stop();
      this.onDone.run();
   }
}
