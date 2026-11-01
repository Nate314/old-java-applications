package flappyBird;

import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main extends JFrame {
   public static final int FRAME_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width * 3 / 4;
   public static final int FRAME_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4;

   private Main(int stepsPerSecond) {
      this.setTitle("Flappy Bird");
      this.setDefaultCloseOperation(3);
      int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
      int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
      this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
      this.setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight()) / 2);
      this.setLayout(null);
      GamePanel gamePanel = new GamePanel(stepsPerSecond);
      gamePanel.setSize(this.getWidth(), this.getHeight());
      this.add(gamePanel);
      this.addKeyListener(gamePanel);
      this.setVisible(true);
   }

   public static void main(String[] args) {
      int stepsPerSecond = 120;
      if (args.length > 0) {
         try {
            stepsPerSecond = Math.max(1, Integer.parseInt(args[0].trim()));
         } catch (NumberFormatException e) {
            System.err.println("Expected a whole number of game steps per second, using " + stepsPerSecond);
         }
      }

      int speed = stepsPerSecond;
      SwingUtilities.invokeLater(() -> new Main(speed));
   }
}
