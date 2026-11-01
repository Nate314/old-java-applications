package dinosaurgameplayer;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DinosaurGamePlayer implements MouseListener {
   private DinosaurGamePlayer(Robot robot) {
      int sensorX = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().width / 2.44);
      int lowerSensorY = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().height / 4.5);
      int upperSensorY = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().height / 5.35);

      this.showMarker("Player1", sensorX, lowerSensorY, new Color(255, 0, 0));
      this.showMarker("Player2", sensorX, upperSensorY, new Color(0, 255, 0));
      ObstacleDetector detector = new ObstacleDetector(robot, sensorX, lowerSensorY);
      detector.jump(70);
      Timer timer = new Timer(1, detector);
      timer.setRepeats(true);
      timer.start();
   }

   private void showMarker(String title, int x, int y, Color color) {
      JFrame markerFrame = new JFrame(title);
      markerFrame.setDefaultCloseOperation(3);
      markerFrame.setAlwaysOnTop(true);
      markerFrame.setUndecorated(true);
      markerFrame.setBounds(x, y, 10, 10);
      JPanel markerPanel = new JPanel();
      markerPanel.setBackground(color);
      // Clicking either marker stops the bot.
      markerPanel.addMouseListener(this);
      markerFrame.add(markerPanel);
      markerFrame.setVisible(true);
   }

   @Override
   public void mouseClicked(MouseEvent event) {
   }

   @Override
   public void mousePressed(MouseEvent event) {
      System.exit(0);
   }

   @Override
   public void mouseReleased(MouseEvent event) {
   }

   @Override
   public void mouseEntered(MouseEvent event) {
   }

   @Override
   public void mouseExited(MouseEvent event) {
   }

   public static void main(String[] args) throws InterruptedException {
      // Time to focus the game. This waits on the main thread, never the Swing event thread.
      Thread.sleep(2500L);
      SwingUtilities.invokeLater(() -> {
         try {
            new DinosaurGamePlayer(new Robot());
         } catch (AWTException e) {
            e.printStackTrace();
         }
      });
   }
}
