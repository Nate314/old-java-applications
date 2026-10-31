package dinosaurgameplayer;

import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class ObstacleDetector implements ActionListener {
   private final DinosaurGamePlayer player;
   private final Robot robot;
   private final int sensorX;
   private final int sensorY;

   ObstacleDetector(DinosaurGamePlayer player, Robot robot, int sensorX, int sensorY) {
      this.player = player;
      this.robot = robot;
      this.sensorX = sensorX;
      this.sensorY = sensorY;
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      if (this.robot.getPixelColor(this.sensorX - 1, this.sensorY).getBlue() == 83) {
         this.robot.keyPress(32);
         DinosaurGamePlayer.pause(65);
         this.robot.keyRelease(32);
      }
   }
}
