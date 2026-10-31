package windowse;

import java.awt.AWTException;
import java.awt.Robot;

public final class b {
   public b(int var1) {
      try {
         Robot var2;
         (var2 = new Robot()).keyPress(524);

         for (int var3 = 0; var3 < var1; var3++) {
            var2.keyPress(69);
            var2.keyRelease(69);

            try {
               Thread.sleep(100L);
            } catch (InterruptedException var5) {
               var5.printStackTrace();
            }
         }

         var2.keyRelease(524);
      } catch (AWTException var6) {
         var6.printStackTrace();
      }
   }
}
