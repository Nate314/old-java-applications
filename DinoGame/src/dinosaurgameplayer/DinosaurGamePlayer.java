package dinosaurgameplayer;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class DinosaurGamePlayer {
   private DinosaurGamePlayer(Robot robot) {
      int sensorX = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().width / 2.44);
      int lowerSensorY = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().height / 4.5);
      int upperSensorY = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().height / 5.35);

      try {
         Thread.sleep(2500L);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      JFrame markerFrame;
      (markerFrame = new JFrame("Player1")).setDefaultCloseOperation(3);
      markerFrame.setAlwaysOnTop(true);
      markerFrame.setUndecorated(true);
      markerFrame.setVisible(true);
      markerFrame.setBounds(sensorX, lowerSensorY, 10, 10);
      JPanel lowerMarkerPanel;
      (lowerMarkerPanel = new JPanel()).setBackground(new Color(255, 0, 0));
      markerFrame.add(lowerMarkerPanel);
      (markerFrame = new JFrame("Player2")).setDefaultCloseOperation(3);
      markerFrame.setAlwaysOnTop(true);
      markerFrame.setUndecorated(true);
      markerFrame.setVisible(true);
      markerFrame.setBounds(sensorX, upperSensorY, 10, 10);
      JPanel upperMarkerPanel;
      (upperMarkerPanel = new JPanel()).setBackground(new Color(0, 255, 0));
      markerFrame.add(upperMarkerPanel);
      robot.keyPress(32);
      pause(70);
      robot.keyRelease(32);
      Timer timer;
      (timer = new Timer(1, new ObstacleDetector(this, robot, sensorX, lowerSensorY))).setRepeats(true);
      timer.start();
   }

   static void pause(int millis) {
      try {
         Thread.sleep((long)millis);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   public static void main(String[] args) {
      try {
         new DinosaurGamePlayer(new Robot());
      } catch (AWTException e) {
         e.printStackTrace();
      }
   }
}
