package flappyBird;

import java.awt.AWTException;
import java.awt.Robot;

final class d implements Runnable {
   private final c a;

   d(c var1) {
      this.a = var1;
   }

   @Override
   public final void run() {
      this.a.repaint();

      try {
         Thread.sleep(500L);
      } catch (InterruptedException var2) {
         var2.printStackTrace();
      }

      try {
         while (new Robot().getPixelColor(Main.a / 2, Main.b / 2).getBlue() != 0) {
            this.a.repaint();
         }
      } catch (AWTException var3) {
         var3.printStackTrace();
      }

      this.a.b = true;

      while (true) {
         this.a.repaint();
      }
   }
}
