package test;

import java.awt.Robot;
import java.awt.event.KeyEvent;

public class Test {
   public static void main(String[] args) throws Exception {
      Robot robot = new Robot();
      robot.keyPress(KeyEvent.VK_WINDOWS);
      robot.keyRelease(KeyEvent.VK_WINDOWS);
      Thread.sleep(100L);
      System.exit(0);
   }
}
