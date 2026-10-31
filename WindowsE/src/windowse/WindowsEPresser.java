package windowse;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import javax.swing.Timer;

/** Holds the Windows key and taps E the given number of times, 100 ms apart, on a Swing timer. */
public final class WindowsEPresser {
   private final Robot robot;
   private final Runnable onDone;
   private int remaining;
   private Timer timer;

   public WindowsEPresser(int count, Runnable onDone) throws AWTException {
      this.robot = new Robot();
      this.remaining = count;
      this.onDone = onDone;
   }

   public void start() {
      this.robot.keyPress(KeyEvent.VK_WINDOWS);
      this.timer = new Timer(100, event -> this.tap());
      this.tap();
      this.timer.start();
   }

   private void tap() {
      if (this.remaining <= 0) {
         this.timer.stop();
         this.robot.keyRelease(KeyEvent.VK_WINDOWS);
         this.onDone.run();
         return;
      }
      this.robot.keyPress(KeyEvent.VK_E);
      this.robot.keyRelease(KeyEvent.VK_E);
      this.remaining--;
   }
}
