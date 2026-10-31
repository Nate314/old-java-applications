package flappyBird;

import java.awt.AWTException;
import java.awt.Robot;

final class RenderLoop implements Runnable {
   private final GamePanel panel;

   RenderLoop(GamePanel panel) {
      this.panel = panel;
   }

   @Override
   public final void run() {
      this.panel.repaint();

      try {
         Thread.sleep(500L);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      try {
         while (new Robot().getPixelColor(Main.FRAME_WIDTH / 2, Main.FRAME_HEIGHT / 2).getBlue() != 0) {
            this.panel.repaint();
         }
      } catch (AWTException e) {
         e.printStackTrace();
      }

      this.panel.running = true;

      while (true) {
         this.panel.repaint();
      }
   }
}
