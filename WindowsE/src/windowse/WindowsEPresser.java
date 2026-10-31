package windowse;

import java.awt.AWTException;
import java.awt.Robot;

public final class WindowsEPresser {
   public WindowsEPresser(int count) {
      try {
         Robot robot;
         (robot = new Robot()).keyPress(524);

         for (int i = 0; i < count; i++) {
            robot.keyPress(69);
            robot.keyRelease(69);

            try {
               Thread.sleep(100L);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }

         robot.keyRelease(524);
      } catch (AWTException e) {
         e.printStackTrace();
      }
   }
}
