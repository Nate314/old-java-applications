package windowse;

import javax.swing.SwingUtilities;

public class WindowsEMain {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new WindowsEWindow().setVisible(true));
   }
}
