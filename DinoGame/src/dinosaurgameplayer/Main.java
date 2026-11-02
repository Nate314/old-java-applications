package dinosaurgameplayer;

import java.io.File;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * DinoGame: the dinosaur game the original bot was written for, with the bot built in. Play it yourself, or press B
 * and the bot plays, watching one pixel exactly as it watched Chrome's game.
 *
 * With the argument "screen" this runs the original instead (DinosaurGamePlayer): it watches the real screen and
 * presses the real Space key, for playing Chrome's own game.
 */
public final class Main extends JFrame {
   private static final long serialVersionUID = 1L;

   private Main() {
      this.setTitle("Dino Game");
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      // Read after the window exists, so the browser build has put the saved file back.
      Records records = Records.load(new File(System.getProperty("user.home"), ".dinogame.txt"));
      RunnerPanel panel = new RunnerPanel(records, System.nanoTime());
      this.add(panel);
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(true);
      panel.requestFocusInWindow();
      panel.start();
   }

   public static void main(String[] args) throws InterruptedException {
      if (args.length > 0 && args[0].equalsIgnoreCase("screen")) {
         DinosaurGamePlayer.main(new String[0]);
         return;
      }
      SwingUtilities.invokeLater(Main::new);
   }
}
