package windowse;

import java.awt.AWTException;
import javax.swing.SwingUtilities;

public class WindowsEMain {
   public static void main(String[] args) throws AWTException {
      Keys keys = new RobotKeys();
      SwingUtilities.invokeLater(() -> new WindowsEWindow(keys).setVisible(true));
   }
}
