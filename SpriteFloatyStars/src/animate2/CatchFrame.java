package animate2;

import java.io.File;
import javax.swing.JFrame;

/** The Star Catcher window. It stands in for the settings window, which comes back when it is closed. */
@SuppressWarnings("serial")
public final class CatchFrame extends JFrame {
   private final CatchPanel panel;
   private final JFrame settings;

   public CatchFrame(JFrame settings) {
      this.settings = settings;
      this.setTitle("Star Catcher");
      this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      this.setResizable(false);
      // Read after the window exists, so the browser build has put the saved file back.
      NightRecords records = NightRecords.load(new File(System.getProperty("user.home"), ".spritefloatystars-nights.txt"));
      this.panel = new CatchPanel(records);
      this.add(this.panel);
      this.addKeyListener(this.panel.keys());
      this.panel.addKeyListener(this.panel.keys());
      this.pack();
      this.setLocationRelativeTo(settings);
   }

   /** Closing the window stops the game's timer and brings the settings window back. */
   @Override
   public void dispose() {
      this.panel.stop();
      super.dispose();
      this.settings.setVisible(true);
   }
}
