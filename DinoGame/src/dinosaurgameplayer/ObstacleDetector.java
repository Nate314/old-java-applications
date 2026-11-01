package dinosaurgameplayer;

import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Timer;

final class ObstacleDetector implements ActionListener {
   private final Robot robot;
   private final int sensorX;
   private final int sensorY;
   private boolean jumping = false;

   ObstacleDetector(Robot robot, int sensorX, int sensorY) {
      this.robot = robot;
      this.sensorX = sensorX;
      this.sensorY = sensorY;
   }

   /** Holds Space for the given time, released by a one-shot timer so the event thread never sleeps. */
   void jump(int holdMillis) {
      this.jumping = true;
      this.robot.keyPress(KeyEvent.VK_SPACE);
      Timer release = new Timer(holdMillis, event -> {
         this.robot.keyRelease(KeyEvent.VK_SPACE);
         this.jumping = false;
      });
      release.setRepeats(false);
      release.start();
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      // Like the original, the sensor is not checked while Space is held.
      if (!this.jumping && this.robot.getPixelColor(this.sensorX - 1, this.sensorY).getBlue() == 83) {
         this.jump(65);
      }
   }
}
