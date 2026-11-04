package windowse;

import java.awt.AWTException;
import java.awt.Robot;

/** Presses keys on the real keyboard with {@code java.awt.Robot}. */
final class RobotKeys implements Keys {
   private final Robot robot;

   RobotKeys() throws AWTException {
      this.robot = new Robot();
   }

   @Override
   public void press(int keyCode) {
      this.robot.keyPress(keyCode);
   }

   @Override
   public void release(int keyCode) {
      this.robot.keyRelease(keyCode);
   }
}
