package flappyBird;

import java.awt.Toolkit;
import javax.swing.JFrame;

public class Main extends JFrame {
   public static final int FRAME_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width * 3 / 4;
   public static final int FRAME_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4;
   private static int launchArgument;

   private Main(Boolean visible) {
      this.setVisible(visible);
      this.setTitle("Flappy Bird");
      this.setDefaultCloseOperation(3);
      int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
      int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
      this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
      this.setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight()) / 2);
      this.setLayout(null);
      GamePanel gamePanel;
      (gamePanel = new GamePanel(this, launchArgument)).setVisible(true);
      gamePanel.setSize(this.getWidth(), this.getHeight());
      this.add(gamePanel);
      this.addKeyListener(gamePanel);
   }

   public static void main(String[] args) {
      if (args.length > 0) {
         launchArgument = Integer.parseInt(args[0]);
      } else {
         launchArgument = 120;
      }

      System.out.println(launchArgument);
      new Main(true);
   }
}
