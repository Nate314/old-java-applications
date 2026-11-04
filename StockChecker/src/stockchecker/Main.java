package stockchecker;

import javax.swing.SwingUtilities;

public class Main {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         Window window = new Window();
         window.setVisible(true);
      });
   }
}
