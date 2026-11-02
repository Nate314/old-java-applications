package dinosaurgameplayer;

import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Timer;

/** The original screen bot's eye: one screen pixel, checked every millisecond. PixelBot follows the same rule. */
final class ObstacleDetector implements ActionListener {
   /** The game in this window draws its dinosaur and obstacles in #535353, and 0x53 is 83. */
   static final int OBSTACLE_BLUE = 83;
   /** How long a jump holds Space. */
   static final int HOLD_MS = 65;
   /** The first press, which starts the game. */
   static final int START_HOLD_MS = 70;

   private final Robot robot;
   private final int sensorX;
   private final int sensorY;
   private boolean jumping = false;

   ObstacleDetector(Robot robot, int sensorX, int sensorY) {
      this.robot = robot;
      this.sensorX = sensorX;
      this.sensorY = sensorY;
   }

   /** The bot's whole idea: only the blue channel is checked. */
   static boolean isObstacle(int rgb) {
      return (rgb & 0xFF) == OBSTACLE_BLUE;
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
      if (!this.jumping && isObstacle(this.robot.getPixelColor(this.sensorX - 1, this.sensorY).getRGB())) {
         this.jump(HOLD_MS);
      }
   }
}
