package test;

import java.awt.Robot;

public class Test {
   public static void main(String[] args) throws Exception {
      new Robot();
      short windowsKeyCode = 524;
      Robot robot;
      (robot = new Robot()).keyPress(524);
      robot.keyRelease(524);
      Thread.sleep(100L);
      System.exit(0);
   }
}
