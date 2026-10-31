package test;

import java.awt.Robot;

public class Test {
   public static void main(String[] var0) throws Exception {
      new Robot();
      short var1 = 524;
      Robot var2;
      (var2 = new Robot()).keyPress(524);
      var2.keyRelease(524);
      Thread.sleep(100L);
      System.exit(0);
   }
}
