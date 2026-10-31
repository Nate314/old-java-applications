package animate2;

import javax.swing.SwingUtilities;

public class Animate2Main {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new SettingsFrame().setVisible(true));
   }
}
